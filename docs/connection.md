# Connection and reconnection

This page covers how the apps find your Mac, remember it, and get back to it when the connection drops. Android and iOS share the same logic, the same storage keys and the same timings, and the reconnection code is tested against a fake network on both platforms.

## Ways to connect

| Route | Address | Where it comes from |
| --- | --- | --- |
| On This Network | The address Bonjour resolves for the Mac | Discovery of `_replaymac._tcp` |
| Previously Connected | Where discovery sees the Mac now, or else its last network address, or else its USB port | The remembered Macs list |
| USB Cable (Android) / USB Cable / Localhost (iOS) | `127.0.0.1:7375` | Fixed. On Android, `adb reverse` forwards it to the Mac |
| Enter Manually | Whatever you type. The port falls back to 7375 if it's blank or outside 1 to 65535 | The form, pre-filled with the last non-USB address |

Any connection to `127.0.0.1` counts as USB.

## Remembered Macs

Every successful connection saves the Mac. Each record has:

| Field | Meaning |
| --- | --- |
| `serverId` | The stable ID ReplayMac advertises. Missing for older builds of ReplayMac |
| `name` | The Mac's server name |
| `networkEndpoint` | The last network address and port |
| `usbPort` | The port last used over USB |
| `lastUsedUsb` | Whether the most recent connection was over USB |

Two records are the same Mac when both have a server ID and the IDs match. If either side has no server ID, the app compares names instead. A Mac is identified by its stable server ID (falling back to its name), since network addresses change.

When you connect:

- **Over the network** (discovery, Previously Connected or a typed address), the app stores the network address, clears `lastUsedUsb`, and preserves the existing USB port.
- **Over USB**, the app stores the USB port, sets `lastUsedUsb`, and preserves the network address so it can still find the Mac over Wi-Fi later.
- **To a different Mac**, the app creates a fresh record.

The list is ordered by when you last used each Mac, with the newest at the top, and it grows as needed. The Mac you used most recently is the one the app reconnects to.

The list lives under `remembered_macs_list`. Earlier versions stored a single Mac under the `remembered_*` keys. The first time the app reads the list, it migrates that Mac across, and it keeps those keys in step with the newest Mac afterwards.

### Online or offline

The Previously Connected list compares each remembered Mac against what discovery can see right now. A Mac that's visible gets a green dot, its current address and an enabled **Connect** button. One that isn't shows a grey dot and **Offline**, and its Connect button is disabled. iOS writes the address as "Available • 192.168.1.20:7375", while Android shows the address on its own.

A Mac that's already in Previously Connected isn't repeated under On This Network.

Tapping **Connect** on a remembered Mac uses the address discovery sees now, then its last network address, then `127.0.0.1` on its saved USB port.

### Forgetting a Mac

The bin icon on a Previously Connected row asks for confirmation and then removes that record from the phone. That only changes the phone's own list. ReplayMac still trusts the phone, so if you connect again later, it lets the phone straight in. To make ReplayMac ask again, revoke the phone in **Paired Devices** on the Mac.

## Automatic reconnection

The reconnector starts when:

- the app comes to the foreground and isn't connected, including when you open it;
- a working connection drops.

It stops when:

- the Mac accepts the phone, or ReplayMac starts showing the Allow prompt;
- you tap **Cancel** on the Reconnecting screen;
- the app goes to the background;
- the Mac denies or revokes the phone.

While it runs, the connection screen shows **Reconnecting** with "Looking for “*Mac name*” on this network and over USB."

### The order it tries

Each round builds a list of addresses for the most recent Mac, removes duplicates, and tries them in this order:

1. Every address where discovery can see the Mac right now.
2. The route that worked last time: the network address, or USB if `lastUsedUsb` is set.
3. The other route.

So if your Mac's IP address changes, or you move to another network, the app tries the new address first. A USB-only record still gets tried on `127.0.0.1`.

### Timings

| | |
| --- | --- |
| Socket connect timeout | 5 seconds |
| Time allowed per address (connect plus the `hello` round trip) | 8 seconds |
| Wait between rounds | 1, 2, 4 and 8 seconds, then every 15 seconds |

While it waits on an address that isn't answering, the reconnector keeps watching discovery. If the Mac appears at an address it hasn't tried yet, it switches straight away and doesn't wait for the timeout. The same happens during the wait between rounds.

### Pairing during reconnection

If the Mac answers with `pending`, the reconnector treats the job as done and the app shows **Waiting for approval**. If it answers with `denied`, the reconnector stops and the app clears every remembered Mac.

## Lost connections

Both apps note whether a failure happened on a connection that was working. That's what separates "Connection lost", which starts reconnecting, from "Couldn't connect", which shows the error and waits for you.

Keepalives:

- The phone sends `{"type":"ping"}` every 20 seconds and ReplayMac answers with `pong`.
- Android's OkHttp client also sends WebSocket ping frames every 15 seconds. iOS doesn't send ping frames.
- Both apps keep a quiet connection open, since neither sets a read timeout.

## Error messages

The app checks these situations in order and displays the first matching message.

| Situation | Android | iOS | Message |
| --- | --- | --- | --- |
| The connection was working and then failed | any error | any error | The connection to your Mac was interrupted. |
| The Mac closed the connection | `EOFException`, or a clean close after connecting | `ECONNRESET` before connecting | ReplayCap closed the connection. |
| Nothing answered in time | `SocketTimeoutException` | `NSURLErrorTimedOut`, `ETIMEDOUT` | Your Mac didn't respond. Check it's awake and on the same network. |
| Something answered but refused | `ConnectException` | `NSURLErrorCannotConnectToHost`, `ECONNREFUSED` | Nothing is listening there. Check Companion Server is on in ReplayCap. |
| No route to the address | `NoRouteToHostException` | `NSURLErrorNotConnectedToInternet`, `NSURLErrorNetworkConnectionLost`, `EHOSTUNREACH`, `ENETUNREACH` | Your Mac isn't reachable from this network. |
| The name didn't resolve | `UnknownHostException` | `NSURLErrorCannotFindHost`, `NSURLErrorDNSLookupFailed` | That address couldn't be found. |
| The Mac said no | `auth_response` with `denied` | `auth_response` with `denied` | Pairing denied by *Mac name* |
| Anything else | | | The error's own message, or "Connection failed" |

Android shows "ReplayCap closed the connection." when the Mac cleanly closes a working connection. iOS shows "The connection to your Mac was interrupted." in the same situation, because it checks for a dropped connection first.

## Discovery

- Service type `_replaymac._tcp`, with the Mac's server ID in the TXT record under `id`.
- Each service gets 5 seconds to resolve.
- Android strips the IPv6 zone (`%wlan0`) during discovery. iOS prefers an IPv4 address, then IPv6, then the host name. Both socket clients strip any zone and put IPv6 addresses in brackets before building the URL.
- When a Mac stops advertising, it's removed from the list and its Previously Connected row goes offline.
- The refresh button ("Search again") clears the list and starts discovery again.
- Discovery starts when the app launches and keeps running while it's in the background.

## Renames

ReplayMac sends a new `approved` `auth_response` when you rename the Mac or the phone in its Companion settings. The phone updates the names on screen and in the remembered Mac straight away.

A remembered Mac with no server ID is matched by name, so renaming it creates a second entry. Current builds of ReplayMac always send a server ID.

## Where the code is

| | Android | iOS |
| --- | --- | --- |
| Remembered Mac model | `data/model/RememberedMac.kt` | `Models/RememberedMac.swift` |
| Storage | `data/repository/RememberedMacStore.kt` | `Repository/RememberedMacStore.swift` |
| Address order and backoff | `data/connection/ReconnectPlan.kt` | `Connection/ReconnectPlan.swift` |
| Reconnect loop | `data/connection/AutoReconnector.kt` | `Connection/AutoReconnector.swift` |
| Wiring and lifecycle | `data/repository/CompanionRepository.kt` | `Repository/CompanionRepository.swift` |
| Tests | `ReconnectPlanTest`, `AutoReconnectorTest`, `RememberedMacTest`, `RememberedMacStoreTest` | `ReconnectPlanTests`, `AutoReconnectorTests`, `RememberedMacTests` |
