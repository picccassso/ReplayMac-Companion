# Privacy and security

ReplayCap Companion keeps its data on your phone and talks to ReplayCap on your Mac over your own network. This page covers what's stored on the phone, what crosses the wire, how the Mac decides to trust a phone, and the permissions each platform asks for.

## What stays on the phone

Both apps save the same small set of settings locally. Android keeps them in one `SharedPreferences` file called `replaymac_companion_prefs`. iOS keeps them in `UserDefaults.standard`. The key names are identical on both platforms.

| Key | What it holds |
| --- | --- |
| `device_id` | A random UUID created on first launch. The phone sends it in every `hello` so the Mac can recognise it. |
| `remembered_macs_list` | Every Mac you've connected to, newest first, with its server ID, name, last network address, USB port and USB flag. |
| `last_host`, `last_port` | The last address you connected to by hand, used to pre-fill Enter Manually. |
| `keep_awake` | Deck mode on or off. |
| `has_seen_onboarding` | Whether the quick guide has been dismissed. |

Older builds stored a single Mac under the `remembered_*` keys (`remembered_server_id`, `remembered_name`, `remembered_network_host`, `remembered_network_port`, `remembered_usb_port`, `remembered_last_used_usb`). The first read migrates that record into the list, and the keys keep mirroring the most recent Mac afterwards.

The app reads these values itself and shares only the device ID, which goes to the Mac you connect to as part of `hello`. Everything else stays in the app's own settings file on the phone.

### The iOS privacy manifest

The iOS app ships a `PrivacyInfo.xcprivacy` manifest. It declares its use of `UserDefaults` under `NSPrivacyAccessedAPICategoryUserDefaults` with reason `CA92.1` (reading and writing the app's own settings). Tracking stays off, and the collected data types list is empty.

## Local network communications

The phone talks to ReplayCap's Companion server over a WebSocket at `ws://<host>:<port>`, port 7375 by default. Every frame is plain JSON text, and the URL carries no path. The app brackets IPv6 hosts and drops any zone suffix before using the address.

Two routes reach the Mac:

- **Local network.** Over Wi-Fi, using the address Bonjour resolves for the Mac.
- **USB.** On Android, `adb reverse tcp:7375 tcp:7375` forwards `127.0.0.1:7375` on the phone to the Mac. On iOS, you can connect over USB using Personal Hotspot, Internet Sharing, or `iproxy 7375 7375`. Any connection to `127.0.0.1` counts as USB.

ReplayCap advertises itself over Bonjour as `_replaymac._tcp`, with its stable server ID in the TXT record under `id`. The phone browses that service with `NsdManager` on Android and `NWBrowser` on iOS.

The URL uses `ws://`, so frames travel as unencrypted plain text that anything on the network path can read. That fits the intended setup of a home or studio LAN, or a USB cable straight to the Mac. The keepalive traffic is small: `{"type":"ping"}` every 20 seconds and a `pong` in reply.

## Pairing and trust model

Pairing uses trust on first use. The phone connects and sends `hello` with its `deviceId` and `deviceName`. When the Mac sees a device ID it hasn't stored yet, it shows a prompt on the Mac:

> Allow '<device name>' to connect?

Clicking **Allow** saves that `deviceId` in ReplayCap's `pairedCompanionDevices` list and replies `auth_response` with `status: "approved"`, followed by a `state_snapshot`. That phone then connects without a prompt on later launches.

Clicking **Deny** replies `status: "denied"` and closes the connection. The same happens when you revoke a phone in ReplayCap's Paired Devices list: the Mac sends `denied`, closes any open connection, and drops the device ID from `pairedCompanionDevices`. The phone forgets the Mac, and the next connection needs fresh approval.

Until a connection is approved, the Mac answers only `hello` and `ping`. Any other message gets `{"type":"error","message":"Device not authorized"}`. The paired list lives on the Mac alone, and the phone only learns whether its own ID was approved.

Forgetting a Mac from the phone's Previously Connected list is a local action. The Mac still trusts the phone, so reconnecting goes straight through. To make the Mac ask again, revoke the phone in Paired Devices on the Mac.

## Network permissions

**iOS**

- `NSLocalNetworkUsageDescription` supplies the text for the Local Network prompt: "ReplayCap Companion discovers and connects to ReplayCap running on your Mac over the local network or USB."
- `NSBonjourServices` lists `_replaymac._tcp` so the app may browse it.
- Grant the Local Network prompt, and the app can find your Mac on the LAN.

**Android**

- `INTERNET` and `ACCESS_NETWORK_STATE` open the socket and read the current network.
- `CHANGE_WIFI_MULTICAST_STATE` lets the app hold a Wi-Fi multicast lock while `NsdManager` browses for `_replaymac._tcp`, so discovery keeps working with the screen off.
- `usesCleartextTraffic="true"` allows the plain `ws://` socket.
- `VIBRATE` and `WAKE_LOCK` support the controls and Deck mode.

## Security advice

The Companion server is built for home and studio LANs and for wired USB connections. Run it on networks you control, or plug in over USB when you're somewhere shared.

A few habits keep it tight:

- Read the device name on the Mac's Allow prompt before you click. That prompt is the one moment that decides access.
- Revoke phones you no longer use in ReplayCap's Paired Devices list, so their stored IDs stop working.
- Prefer USB on public or untrusted Wi-Fi. The frames are unencrypted, and a cable keeps them off the network entirely.
- Keep ReplayCap updated so the Mac side has the latest pairing behaviour.
