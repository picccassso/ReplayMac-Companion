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

## Pairing

1. The phone connects and sends `hello` with its device ID (a random UUID generated on first launch) and its name.
2. If ReplayMac already trusts that device ID, it replies `auth_response` with `status: "approved"`, followed by a `state_snapshot`.
3. Otherwise it replies `status: "pending"` and asks the user on the Mac whether to allow the device. **Allow** sends `approved` and a snapshot. **Deny** sends `denied` and closes the connection.
4. Until a connection is approved, ReplayMac only answers `hello` and `ping`. Anything else gets `{"type":"error","message":"Device not authorized"}`.

ReplayMac keeps trusted devices in its `pairedCompanionDevices` default. Revoking a device on the Mac sends that phone `denied`, and the phone forgets the Mac.

## Phone to Mac

| `type` | Fields | Purpose |
| --- | --- | --- |
| `hello` | `deviceId`, `deviceName`, `clientVersion` | Identify the phone. Also sent again to ask for a fresh snapshot when the app returns to the foreground. |
| `action` | `action` | Run a control (see below). |
| `get_thumbnail` | `clipId` | Ask for a clip's thumbnail. |
| `reveal_clip` | `clipId` | Show the clip in Finder. |
| `delete_clip` | `clipId` | Delete the clip from the Mac. |
| `ping` | | Keepalive. |

Actions are `saveReplay`, `saveExtendedReplay`, `toggleBuffer`, `toggleSession`, `toggleMicrophone` and `toggleSystemAudio`. ReplayMac also accepts snake_case spellings.

```json
{"type":"hello","deviceId":"6F1C2A9E-…","deviceName":"Pixel 9 Pro","clientVersion":"1.0.0"}
{"type":"action","action":"saveReplay"}
{"type":"get_thumbnail","action":"get_thumbnail","clipId":"2B7D…"}
```

For `type == "action"` commands, ReplayMac reads `message.action`; for clip operations (`get_thumbnail`, `reveal_clip`, `delete_clip`), it reads `message.type`. Both client apps send clean, matching envelopes, so `action` mirrors `type` on clip messages. Android also sends unused optional fields as `null`, while iOS leaves them out.

## Mac to phone

| `type` | When |
| --- | --- |
| `auth_response` | In reply to `hello`, and again whenever the Mac or this device is renamed or revoked. |
| `state_snapshot` | Straight after approval. |
| `state_changed` | Whenever the buffer, session, display, audio, clips or save status change. Same shape as `state_snapshot`. |
| `telemetry_tick` | Once a second with the timers and memory use. |
| `thumbnail_response` | In reply to `get_thumbnail`. |
| `pong` | In reply to `ping`. |
| `error` | When an unapproved connection sends a command. |

### `auth_response`

| Key | Type | Notes |
| --- | --- | --- |
| `status` | String | `approved`, `pending` or `denied` |
| `serverName` | String | The Mac's current server name |
| `serverVersion` | String | ReplayMac's version |
| `serverId` | String? | The Mac's stable ID |
| `deviceName` | String? | This phone's name as set on the Mac |

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

The protocol operates without a formal version field. Both sides tolerate unknown keys and missing optional fields, and the phone ignores unrecognised message types. The `clientVersion` field reports `"1.0.0"`, preserved for future compatibility.
