# Companion protocol

The companion apps talk to ReplayMac's Companion server (`Sources/CompanionServer/` in the [ReplayMac](https://github.com/picccassso/ReplayMac) repo) using JSON text frames over a WebSocket.

## Discovery

ReplayMac advertises itself over Bonjour:

| | |
| --- | --- |
| Service type | `_replaymac._tcp` |
| Service name | The Mac's server name (custom name, else the Mac's own name) |
| TXT record | `id` = the Mac's stable server ID, a UUID stored in ReplayMac's `companionServerId` default |
| Port | 7375 by default, configurable from 1024 to 65535 |

Android discovers it with `NsdManager` and iOS with `NWBrowser`, resolving each service within 5 seconds. Phones match a remembered Mac by its server ID when both sides have one, and by name otherwise.

## Transport

- Plain WebSocket at `ws://<host>:<port>` with no path. IPv6 hosts are bracketed and their zone suffix is stripped.
- No TLS. See [Privacy and security](privacy-and-security.md).
- The phone sends `{"type":"ping"}` every 20 seconds and ReplayMac answers with `{"type":"pong"}`. Android's OkHttp client also sends WebSocket ping frames every 15 seconds, which ReplayMac answers automatically.
- The phone gives up on a connection attempt after 5 seconds.

## Pairing and sign-in

The phone identifies itself with a device ID, a random UUID created on first launch. A device ID alone proves nothing, because it travels in plain text. What proves a phone is paired is a **pairing secret**: 32 random bytes the Mac hands over once, at the moment the user clicks **Allow**.

**Pairing a new phone**

1. The phone connects and sends `hello` with its device ID, its name and `protocolVersion: 2`.
2. ReplayMac doesn't know the device, so it picks a random 4-digit code, replies `auth_response` with `status: "pending"` and `pairingCode`, and shows an Allow prompt on the Mac with the same code. The phone shows the code too, so the user can check the prompt belongs to their phone.
3. **Allow** sends `approved` with `pairingSecret` (base64), then a `state_snapshot`. The phone stores the secret against the Mac's `serverId` before it shows the Mac as connected: iOS in the Keychain, Android in a private preferences file that is never backed up.
4. **Deny** sends `denied` with `reason: "user_denied"` and closes the connection.

**Signing in later**

1. The phone sends `hello` as before.
2. ReplayMac has a secret for that device, so it replies `auth_challenge` with a fresh random `nonce` and its `serverId`.
3. The phone answers `auth_proof` with `proof` = base64 of HMAC-SHA256, keyed with the secret, over the UTF-8 text `replaycap-companion-auth-v1\n<serverId>\n<deviceId>\n<nonce>`. The nonce is used exactly as received.
4. A correct proof gets `approved` and a `state_snapshot`. A wrong one gets `denied` with `reason: "invalid_proof"`, and the socket closes. It does not unpair the real phone or disconnect it.
5. A phone that has lost its secret (reinstalled, or restored onto new hardware) sends `auth_proof` without `proof`. ReplayMac then runs the pairing steps again, prompt and code included, and issues a new secret.

Test vector, checked by all three codebases: secret = 32 bytes of `0x01`, serverId `S`, deviceId `D`, nonce `N` gives `uWE2MkK60y47fmnf7H64CPSeu+v47Xrw4QeYLwmpKcs=`.

**Rules the Mac enforces**

- Until a connection signs in, ReplayMac only answers `hello`, `auth_proof` and `ping`. Anything else gets `{"type":"error","message":"Device not authorized","code":"not_authorized"}`.
- A connection has 10 seconds to send `hello` and answer the challenge.
- Only one Allow prompt is shown at a time. The prompt times out after 60 seconds and closes by itself if the phone disconnects.
- After a Deny or a timeout, the same phone has to wait 30 seconds before asking again.
- A phone only replaces its own older connection once it has signed in, so knowing a device ID isn't enough to knock a phone offline.
- At most 16 connections, and messages from phones are capped at 64 KB.

When ReplayMac turns a connection away it sends `error` with a `code` and closes the socket. The phone shows the `message` as the reason.

| `code` | Meaning |
| --- | --- |
| `busy` | Another device's Allow prompt is on screen |
| `cooldown` | This phone was denied, or ignored, in the last 30 seconds |
| `timeout` | The connection didn't sign in in time, or nobody answered the prompt |
| `unsupported_client` | `protocolVersion` is missing or too old, or the device ID is malformed |
| `too_many_connections` | Reserved; today extra connections are simply refused |

ReplayMac keeps trusted devices in its `pairedCompanionDevices` default and their secrets in `pairedCompanionDeviceSecrets`. Revoking a device on the Mac deletes its secret and sends a connected phone `denied` with `reason: "revoked"`. On any `denied`, the phone deletes its secret for that Mac and forgets it.

## Phone to Mac

| `type` | Fields | Purpose |
| --- | --- | --- |
| `hello` | `deviceId`, `deviceName`, `clientVersion`, `protocolVersion` | Identify the phone. Sent again on a signed-in connection to ask for a fresh snapshot when the app returns to the foreground. |
| `auth_proof` | `proof` | Answer `auth_challenge`. Leave `proof` out or `null` when the phone has no secret for this Mac. |
| `action` | `action` | Run a control (see below). |
| `get_thumbnail` | `clipId` | Ask for a clip's thumbnail. |
| `reveal_clip` | `clipId` | Show the clip in Finder. |
| `delete_clip` | `clipId` | Move the clip to the Trash on the Mac. |
| `ping` | | Keepalive. |

Actions are `saveReplay`, `saveExtendedReplay`, `toggleBuffer`, `toggleSession`, `toggleMicrophone` and `toggleSystemAudio`. ReplayMac also accepts snake_case spellings.

```json
{"type":"hello","deviceId":"6F1C2A9E-…","deviceName":"Pixel 9 Pro","clientVersion":"1.0.0","protocolVersion":2}
{"type":"auth_proof","proof":"uWE2MkK60y47fmnf7H64CPSeu+v47Xrw4QeYLwmpKcs="}
{"type":"action","action":"saveReplay"}
{"type":"get_thumbnail","action":"get_thumbnail","clipId":"2B7D…"}
```

For `type == "action"` commands, ReplayMac reads `message.action`; for clip operations (`get_thumbnail`, `reveal_clip`, `delete_clip`), it reads `message.type`. Both client apps send clean, matching envelopes, so `action` mirrors `type` on clip messages. Android also sends unused optional fields as `null`, while iOS leaves them out.

## Mac to phone

| `type` | When |
| --- | --- |
| `auth_challenge` | In reply to `hello` from a paired device. Carries `nonce`, `serverId`, `serverName` and `serverVersion`. |
| `auth_response` | When pairing or sign-in settles, and again whenever the Mac or this device is renamed or revoked. |
| `state_snapshot` | Straight after approval. |
| `state_changed` | Whenever the buffer, session, display, audio, clips or save status change. Same shape as `state_snapshot`. |
| `telemetry_tick` | Once a second with the timers and memory use. |
| `thumbnail_response` | In reply to `get_thumbnail`. |
| `pong` | In reply to `ping`. |
| `error` | When an unapproved connection sends a command, or just before the Mac closes a connection it's turning away. |

### `auth_response`

| Key | Type | Notes |
| --- | --- | --- |
| `status` | String | `approved`, `pending` or `denied` |
| `serverName` | String | The Mac's current server name |
| `serverVersion` | String | ReplayMac's version |
| `serverId` | String? | The Mac's stable ID |
| `deviceName` | String? | This phone's name as set on the Mac |
| `pairingCode` | String? | With `pending`: the 4-digit code on the Mac's prompt |
| `pairingSecret` | String? | With `approved`, only right after the user clicks Allow: the base64 secret to keep |
| `reason` | String? | With `denied`: `user_denied`, `revoked` or `invalid_proof` |

The phone updates the names it shows from every `approved` response, which is how renames on the Mac appear without reconnecting.

### `state_snapshot` and `state_changed`

| Key | Type | Meaning |
| --- | --- | --- |
| `isRecording` | Bool | Replay buffer running |
| `isSessionRecording` | Bool | Session recording running |
| `saveStatus` | String | `idle`, `saving`, `saved` or `failed` |
| `recordingElapsedSeconds` | Double | How long the buffer has run |
| `extendedBufferElapsedSeconds` | Double | How long the extended replay has run |
| `sessionElapsedSeconds` | Double | Session length so far |
| `bufferedSeconds` | Double | Seconds of replay currently buffered |
| `bufferDurationSeconds` | Int | Replay buffer length setting |
| `bufferMemoryBytes` | Int | Memory the buffer uses |
| `bufferMemoryFormatted` | String | The same, formatted, such as `"50 MB"` |
| `longBufferEnabled` | Bool | Extended replay turned on |
| `longBufferDurationSeconds` | Int | Extended replay length setting |
| `capturedDisplayName` | String? | Display being recorded |
| `isUsingFallbackDisplay` | Bool | Recording a fallback display (defaults to false if missing) |
| `isMicrophoneEnabled`, `isMicrophoneMuted` | Bool | Microphone state |
| `isSystemAudioEnabled`, `isSystemAudioMuted` | Bool | System audio state |
| `recentClips` | Array | Clips saved this session (below) |

Each clip has `id`, `filename`, `kind` (`replay`, `extended replay` or `session`), `savedAt` (Unix seconds), `duration`, `fileSize` and `hasThumbnail`. The phone asks for thumbnails of clips where `hasThumbnail` is true.

### Other messages

- `telemetry_tick` carries `recordingElapsedSeconds`, `extendedBufferElapsedSeconds`, `sessionElapsedSeconds`, `bufferedSeconds`, `bufferMemoryBytes` and `bufferMemoryFormatted`. The phone merges these into the last snapshot.
- `thumbnail_response` carries `clipId` and `thumbnailBase64`, a base64-encoded image or `null`.

## Versioning

`hello` carries `protocolVersion`, currently `2`. ReplayMac turns away a `hello` without it, or with an older version, using `unsupported_client` and a message asking the user to update the app. Beyond that, both sides tolerate unknown keys and missing optional fields, and the phone ignores unrecognised message types. `clientVersion` reports the app's version and is informational only.
