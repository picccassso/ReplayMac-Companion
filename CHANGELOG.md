# Changelog

## 1.1.0 (2026-10-23)

- Match the Mac's look. The app takes on the atmosphere, accent colour, two-colour accent gradient and surface style (Liquid Glass, Tinted Glass or Solid Matte) chosen in ReplayMac's Appearance settings, and follows changes on the Mac live with a short crossfade. It stays dark, and red, green and orange keep their meaning in every atmosphere. Needs a ReplayMac build that shares its theme; older builds keep the default look
- Add an Appearance sheet, opened with the paintbrush at the top of Home. It shows the Mac's theme and a **Match my Mac** switch for keeping the app's own blue look
- Remember each Mac's theme, so the app opens looking like the Mac it's about to reconnect to. Forgetting a Mac forgets its theme
- Pick a label colour for buttons filled with the accent by contrast, so light accents such as Catppuccin's get dark labels
- Let demo mode try every atmosphere from the Appearance sheet

## 1.0.0 (2026-10-21)

The first version of the companion, on both Android and iOS. The iOS app is on the [App Store](https://apps.apple.com/us/app/replaycap-companion/id6821022359) as ReplayCap Companion, and the Android APK is on [GitHub releases](https://github.com/picccassso/ReplayMac-Companion/releases/latest).

- Add the Android companion app: find ReplayMac on the local network over Bonjour, connect by IP address and port, or connect over a USB cable with `adb reverse tcp:7375 tcp:7375`. Pair with the Mac once by clicking Allow in ReplayMac, then control it from the phone
- Add a Home screen with six controls in the same order as ReplayMac's Home page: save the last N seconds, save the extended replay, start or stop the replay buffer, record or stop a session, and mute the microphone or system audio. Tiles show their state ("Ready", "Buffering…", "Saving…", "Saved", "Muted", "Off") and grey out when an action isn't available
- Add a status card that mirrors the desktop: whether the buffer or a session is running, elapsed time in the desktop's 00:12 style, the recorded display with a "(Fallback)" label when ReplayMac has switched to another display, and meters for the quick replay (in memory) and extended replay (on disk) that read "Filling…" and then "Ready"
- Add a This Session list: every clip saved since ReplayMac opened, newest first, with a thumbnail, its type (replay, extended replay or session), length, size and save time. Tap a clip to open its details, reveal it in Finder or delete it from the Mac after a confirmation
- Add Deck mode, which keeps the phone's screen on while it's propped up next to you. Turn it on with the bulb at the top of Home
- Add a four-page quick guide that opens on first launch and from the question mark button on both screens
- Redesign the interface around neutral macOS-style glass surfaces, desktop-style action tiles and plain section headers, with portrait and landscape layouts. Content is padded with safe-area insets so nothing runs under the status bar on Android 15, and the quick guide blurs the screen behind it on Android 12 and later
- Keep the microphone and system audio tiles tappable when they're switched off, since ReplayMac turns them on and unmutes them when toggled
- Fix a spurious "Socket closed" error, and stale state overwriting fresh state, caused by callbacks from sockets that had already been replaced
- Ask the Mac for a fresh snapshot when the app comes back to the foreground
- Reconnect to the paired Mac automatically, on any network. The app remembers the Mac by the stable ID ReplayMac advertises, along with its last network address and USB port. When the app opens or a connection drops, it looks where discovery sees the Mac now, then where it was last reached, then the other route, switching straight away if the Mac turns up somewhere new. It retries after 1, 2, 4 and 8 seconds and then every 15 seconds, only searches while the app is on screen, and stops and forgets the Mac if it denies the phone
- Show "Reconnecting" while searching, tell a lost connection apart from one that never opened, and explain failures in plain words ("Nothing is listening there. Check Companion Server is on in ReplayCap.") in place of a bare "Connection failed"
- Add the iOS companion app, written in pure Swift and SwiftUI with no third-party dependencies, with the same features, wording and tests as the Android app
- Fix labels being cut off and tiles squashing on compact screens. Small phones and short landscape windows now use a two-column grid with shorter titles such as "Save 30s" and "Stop Session"
- Lay out the clip details sheet side by side in landscape, with the preview on the left and the details and actions on the right
- Set the app icon on both platforms
- Remember more than one Mac. A Previously Connected list on the connection screen shows each Mac you've used with an online or offline indicator, lets you connect with one tap, and lets you forget a Mac after a confirmation. Existing single-Mac data is migrated automatically
- Reflect renames from ReplayMac straight away: renaming the Mac or this phone in ReplayMac's Companion settings updates the names on the phone without reconnecting
- Add a privacy manifest to the iOS app (no tracking, no data collected, UserDefaults used only for the app's own settings) and declare that it uses no non-exempt encryption
- Rename the iOS app to ReplayCap to match the Mac App Store name, with the bundle identifier `com.replaycap.companion`
- Add comprehensive documentation: user guide, building and testing guide, architecture overview, companion protocol specification, connection and reconnection mechanics, and privacy and security details
- Sign in to the Mac with the pairing secret it hands out on Allow (companion protocol v2), and show the Mac's 4-digit pairing code while waiting for approval. The secret lives in the iOS Keychain or Android private storage and is never sent over the network again
- Show the Mac's reason when it turns a connection away, and on iOS detect denied Local Network access with a shortcut to Settings
- Connect over Wi-Fi only on iOS; Android keeps its `adb reverse` USB route
- Call the Android app ReplayMac again, since Apple's rule against "Mac" in app names doesn't apply there
- Add the Audio Mixer, with microphone and system audio volume and per-app muting, a display and capture profile switcher on the status card, Trim & Export and Copy to Clipboard for clips, and haptics in Deck mode
- Add a privacy policy, and link it and ReplayCap for Mac from the Connect screen and the quick guide
- Add a demo mode, "Try without a Mac", that runs every control against a pretend Mac with sample data
- Use less battery: Android closes the connection after 30 seconds in the background and reconnects on return, Bonjour discovery only runs while it's needed, and the live status dot stops pulsing after three beats
- Reconnect to the remembered Mac on a cold launch on iOS, not only after returning from the background
