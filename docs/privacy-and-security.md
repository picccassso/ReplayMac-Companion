# Privacy and security

ReplayCap Companion keeps its data on your phone and talks to ReplayCap on your Mac over your own network. This page covers what's stored on the phone, what crosses the wire, how the Mac decides to trust a phone, and the permissions each platform asks for.

## What stays on the phone

Both apps save the same small set of settings locally. Android keeps them in one `SharedPreferences` file called `replaymac_companion_prefs`. iOS keeps them in `UserDefaults.standard`, apart from the device ID, which lives in the Keychain. The key names are identical on both platforms.

| Key | What it holds |
| --- | --- |
| `device_id` | A random UUID created on first launch. The phone sends it in every `hello` so the Mac can recognise it. iOS keeps it in the Keychain (service `com.replaycap.companion.device`), so it doesn't travel to another phone in a backup. |
| `remembered_macs_list` | Every Mac you've connected to, newest first, with its server ID, name, last network address, USB port and USB flag. |
| `last_host`, `last_port` | The last address you connected to by hand, used to pre-fill Enter Manually. |
| `keep_awake` | Deck mode on or off. |
| `has_seen_onboarding` | Whether the quick guide has been dismissed. |

Older builds stored a single Mac under the `remembered_*` keys (`remembered_server_id`, `remembered_name`, `remembered_network_host`, `remembered_network_port`, `remembered_usb_port`, `remembered_last_used_usb`). The first read migrates that record into the list, and the keys keep mirroring the most recent Mac afterwards.

The app reads these values itself and shares only the device ID, which goes to the Mac you connect to as part of `hello`. Everything else stays in the app's own settings file on the phone.

### Pairing secrets

Each Mac you approve hands the phone a random 32-byte secret, stored against that Mac's server ID:

- **iOS:** in the Keychain under service `com.replaycap.companion.pairing`, marked "this device only, after first unlock". It is never included in backups and never syncs.
- **Android:** in a separate private preferences file, `replaymac_companion_secrets`.

The secret never leaves the phone after the moment of pairing. Forgetting a Mac in the app deletes its secret, and so does the Mac revoking the phone.

### Backups

Android turns off app backup (`allowBackup="false"`) and also excludes both preferences files from cloud backup and device-to-device transfer. A restored copy of the device ID without its secret would only fight the original phone, so the new phone simply pairs as a new device.

### The iOS privacy manifest

The iOS app ships a `PrivacyInfo.xcprivacy` manifest. It declares its use of `UserDefaults` under `NSPrivacyAccessedAPICategoryUserDefaults` with reason `CA92.1` (reading and writing the app's own settings). Tracking stays off, and the collected data types list is empty.

## Local network communications

The phone talks to ReplayCap's Companion server over a WebSocket at `ws://<host>:<port>`, port 7375 by default. Every frame is plain JSON text, and the URL carries no path. The app brackets IPv6 hosts and drops any zone suffix before using the address.

Two routes reach the Mac:

- **Local network.** Over Wi-Fi, using the address Bonjour resolves for the Mac.
- **USB.** On Android, `adb reverse tcp:7375 tcp:7375` forwards `127.0.0.1:7375` on the phone to the Mac. On iOS, you can connect over USB using Personal Hotspot, Internet Sharing, or `iproxy 7375 7375`. Any connection to `127.0.0.1` counts as USB.

ReplayCap advertises itself over Bonjour as `_replaymac._tcp`, with its stable server ID in the TXT record under `id`. The phone browses that service with `NsdManager` on Android and `NWBrowser` on iOS.

The URL uses `ws://`, so frames travel unencrypted and anything on the network path can read them: clip names, timers and the commands you send. That fits the intended setup of a home or studio LAN, or a cable straight to the Mac. What an eavesdropper can't do is take control. Signing in needs the pairing secret, which is only on the wire once, when you click Allow, and every later sign-in is a one-time HMAC answer that is useless if replayed. The keepalive traffic is small: `{"type":"ping"}` every 20 seconds and a `pong` in reply.

## Pairing and trust model

Pairing uses trust on first use, confirmed with a code. The phone connects and sends `hello` with its `deviceId` and `deviceName`. When the Mac doesn't know that device, it shows a prompt on the Mac with a random 4-digit code, and the phone shows the same code:

> Allow "<device name>" to connect?

Clicking **Allow** saves the device in ReplayCap's paired list and sends the phone its pairing secret. From then on the phone signs in by answering a fresh challenge with an HMAC of that secret (details in [Companion protocol](protocol.md#pairing-and-sign-in)), with no prompt.

The prompt is built to be hard to approve by accident or by someone else:

- **Allow has to be clicked.** Return doesn't approve, so a stray key press mid-game can't pair anything. Escape denies.
- **Only one prompt at a time.** A second device asking meanwhile is told the Mac is busy. The prompt closes by itself after 60 seconds, or as soon as the phone gives up.
- **Cool-down.** After a Deny or an ignored prompt, the same phone must wait 30 seconds to ask again.
- **Safe names.** The Mac strips control characters, line breaks and text-direction tricks from the device name and shortens long names, so the prompt can't be made to read like something else.

Clicking **Deny** replies `denied` and closes the connection. Revoking a phone in ReplayCap's Paired Devices list deletes its secret, sends `denied` to it if connected, and the next connection needs fresh approval.

Until a connection signs in, the Mac answers only `hello`, `auth_proof` and `ping`. Any other message gets `{"type":"error","message":"Device not authorized"}`. A connection that doesn't sign in within 10 seconds is closed, and the Mac accepts at most 16 connections at once.

Someone who has watched your traffic and knows your phone's device ID still can't sign in, and can't kick your phone off either: a phone's older connection is only replaced after the new one has proved itself.

Forgetting a Mac from the phone's Previously Connected list deletes the phone's secret for it. The Mac still lists the phone, so the next connection shows the Allow prompt again and issues a new secret.

### What this doesn't cover

Without TLS, someone who can actively intercept traffic on your network (not just read it) could in principle tamper with a session after it has signed in. That takes control of the network itself. If you're on Wi-Fi you don't trust, use a cable instead (`adb reverse` on Android, Personal Hotspot over USB on iPhone).

## Network permissions

**iOS**

- `NSLocalNetworkUsageDescription` supplies the text for the Local Network prompt: "ReplayCap Companion discovers and connects to ReplayCap running on your Mac over the local network or USB."
- `NSBonjourServices` lists `_replaymac._tcp` so the app may browse it.
- `NSAppTransportSecurity` → `NSAllowsLocalNetworking` allows the plain `ws://` socket to LAN addresses and `.local` names.
- Grant the Local Network prompt, and the app can find your Mac on the LAN. If it's turned off, the connection screen says so and offers a button to Settings.

**Android**

- `INTERNET` and `ACCESS_NETWORK_STATE` open the socket and read the current network.
- `CHANGE_WIFI_MULTICAST_STATE` lets the app hold a Wi-Fi multicast lock while `NsdManager` browses for `_replaymac._tcp`, so discovery keeps working with the screen off.
- `usesCleartextTraffic="true"` allows the plain `ws://` socket.
- `VIBRATE` and `WAKE_LOCK` support the controls and Deck mode.

## Security advice

The Companion server is built for home and studio LANs and for wired USB connections. Run it on networks you control, or plug in over USB when you're somewhere shared.

A few habits keep it tight:

- Check that the code on the Mac's Allow prompt matches the one on your phone before you click. That prompt is the one moment that decides access.
- Revoke phones you no longer use in ReplayCap's Paired Devices list, so their secrets stop working.
- Prefer a cable on public or untrusted Wi-Fi. The frames are unencrypted, and a cable keeps them off the shared network.
- Keep ReplayCap updated so the Mac side has the latest pairing behaviour.
