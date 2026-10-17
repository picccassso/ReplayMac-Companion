# App Store listing and review

Copy-ready text for App Store Connect, the answers to its questionnaires, and the notes for App Review. Character limits are Apple's; every field here fits them.

The one thing to get across everywhere: **this app is a remote for ReplayCap on a Mac and does nothing on its own.** Apple's guidelines (2.1 App Completeness, 2.3 Accurate Metadata) expect apps that depend on other software to say so up front, and reviewers reject companion apps they can't test. The description, the review notes and the app itself all state the requirement and link to ReplayCap on the Mac App Store. For reviewers without a Mac, the Connect screen's **Try without a Mac** button opens a demo mode: a pretend Mac with sample data, clearly labelled on every screen, so every control can be tried.

## App information

| Field | Value |
| --- | --- |
| Name (30) | `ReplayCap Companion` |
| Subtitle (30) | `Remote for ReplayCap on Mac` |
| Bundle ID | `com.replaycap.companion` |
| Primary category | Utilities |
| Secondary category | Photo & Video |
| Content rights | Does not contain, show or access third-party content |
| Age rating | Answer **None/No** to every question. Result: 4+ |
| Price | Free |
| Copyright | `2026 <your legal name>` |

App Store names must be unique, and the Mac app already uses **ReplayCap**, so the listing is called **ReplayCap Companion**. The Home Screen name stays **ReplayCap** (`CFBundleDisplayName`), which Apple allows because it's a shortened form of the store name.

## URLs

| Field | URL |
| --- | --- |
| Privacy Policy URL (required) | https://github.com/picccassso/ReplayMac-Companion/blob/main/PRIVACY.md |
| Support URL (required) | https://github.com/picccassso/ReplayMac-Companion/issues |
| Marketing URL (optional) | https://github.com/picccassso/ReplayMac-Companion |

The privacy policy is also linked inside the app: at the bottom of the Connect screen and in the quick guide (the ? button), so it's reachable whether or not a Mac is connected. This is what guideline 5.1.1(i) asks for.

## Promotional text (170)

```
Save the last 30 seconds of your game from your phone. ReplayCap Companion is a remote for ReplayCap on your Mac: replays, recording, audio and clips, one tap away.
```

## Description (4000)

```
ReplayCap Companion turns your iPhone or iPad into a remote for ReplayCap, the replay buffer and screen recorder for Mac.

REQUIRES REPLAYCAP FOR MAC
This app controls ReplayCap running on your Mac and does nothing on its own. Get ReplayCap from the Mac App Store, turn on Companion Server in its settings, and keep your phone on the same Wi-Fi network. Not set up yet? Tap "Try without a Mac" to explore every control with sample data first.

SAVE THE MOMENT
• Tap once to save the last 30 seconds from ReplayCap's replay buffer, or save a longer replay.
• Start and stop the replay buffer or a full session recording.
• See buffer progress, memory use and session time at a glance.

CONTROL AUDIO AND CAPTURE
• Mute or adjust the microphone and system audio.
• Mute individual apps.
• Switch the captured display and apply capture profiles you've set up in ReplayCap.

YOUR CLIPS
• Browse the clips saved this session, with thumbnails.
• Reveal a clip in Finder, trim it, copy it, or move it to the Trash, right from your phone.

DECK MODE
Keep the screen on while your phone sits next to your keyboard, ready to save.

PRIVATE AND LOCAL
Your phone talks straight to your Mac over your own network. No accounts, no analytics, no tracking, and nothing is sent to the developer. Each phone has to be approved on the Mac before it can connect, with a matching code on both screens.
```

## Keywords (100)

```
replay,clip,screen recorder,remote,capture,buffer,gaming,game clips,highlights,record,mac,deck
```

Keep competitor names and trademarks out of the keywords (guideline 2.3.7). Words already in the name and subtitle don't need repeating.

## App Privacy questionnaire

Choose **"No, we do not collect data from this app."** The label then reads **Data Not Collected**.

That's accurate because "collect" in Apple's sense means data sent off the device to the developer or third parties. The phone's name and random ID only go to the user's own Mac, which the developer has no access to. The bundled `PrivacyInfo.xcprivacy` agrees: no tracking, no collected data types, `UserDefaults` used with reason `CA92.1`.

## Export compliance

Already answered in `Info.plist` with `ITSAppUsesNonExemptEncryption = NO`, so App Store Connect won't ask. The app only uses HMAC (CryptoKit) to sign in to the Mac, which is authentication and exempt.

## App Review Information

**Sign-in required:** No. The app has no accounts.

**Contact:** your name, phone number and email.

**Attachment:** upload a screen recording (about 1 minute) showing the full flow: ReplayCap on a Mac with Companion Server on, the iPhone app finding the Mac, the matching pairing code on both screens, clicking Allow, then saving a replay and the clip appearing on the phone. Reviewers can't test hardware-dependent flows they can't reproduce, and this video is what stops a 2.1 rejection when they can't set it up.

**Notes** (4000; fill in the promo code):

```
ReplayCap Companion is a remote control for ReplayCap, our screen recorder for Mac on the Mac App Store (https://apps.apple.com/app/replaycap/id6789296427). It needs ReplayCap running on a Mac on the same Wi-Fi network and does nothing without it. A demo video of the full flow is attached.

To try every control without a Mac:
1. On the Connect screen, scroll to the bottom and tap "Try without a Mac".
2. The app opens its controls connected to "Demo Mac", a pretend Mac with sample data. A "Demo mode" banner stays at the top the whole time.
3. Everything works against the sample data: "Save Last 30s" and the longer replay add a clip, "Stop Buffer" / "Start Buffer" and "Record Session" change the timers (stopping a session saves it as a clip), the audio mixer mutes apps and sets volumes, Capture Setup switches displays and profiles, and tapping a clip opens its actions: "Trim on Mac" adds a trimmed copy, and Delete removes it.
4. Tap "Exit Demo" in the banner (or the power button) to return to the Connect screen.
Demo mode never uses the network and never stores a Mac or pairing secret.

To test with a real Mac:
1. On a Mac, download ReplayCap with this Mac App Store promo code: XXXXXXXXXXXX
2. Open ReplayCap, grant Screen Recording permission, then go to Settings → Companion and switch on the server under Server Status. Allow Local Network access when macOS asks.
3. Put the iPhone or iPad on the same Wi-Fi network as the Mac and open ReplayCap Companion. Allow Local Network access when iOS asks.
4. The Mac appears under "On This Network". Tap Connect. (If it doesn't appear, type the IP address shown in ReplayCap's Companion settings under "Enter Manually"; port 7375.)
5. ReplayCap on the Mac shows an Allow prompt with a 4-digit code that matches the phone. Click Allow.
6. The phone now shows the controls. Tap "Start Buffer", wait a few seconds, then tap "Save Last 30s". The clip appears under "This Session".

The app has no accounts, in-app purchases, ads or analytics. It talks only to the user's own Mac over the local network.
```

Generate the code in App Store Connect under ReplayCap (the Mac app), as a promo code or, where Apple no longer offers those, a one-time-use offer code. Codes expire, so make a fresh one for each submission.

## Screenshots

`App Store Photos/store-ready/` holds sets at accepted sizes:

| Set | Size | Use |
| --- | --- | --- |
| iPhone 6.9 | 1320 × 2868 | Required iPhone set |
| iPhone 6.3 | 1206 × 2622 | Optional, Apple scales 6.9 down otherwise |
| iPad 13 | 2752 × 2064 | Required, because the app runs on iPad |

`review_clip_details_private_thumbnail.png` is for your own reference, not the store. Screenshots must show the app in use (2.3.3), and they do. Keep Android, other platforms and prices out of them (2.3.10).

## Guideline checklist

| Guideline | Status |
| --- | --- |
| 2.1 App completeness: testable by review | In-app demo mode ("Try without a Mac"), review notes, promo code and demo video, above |
| 2.3.1 Accurate metadata: requirement disclosed | First section of the description, and in the app |
| 2.3.7 Name, subtitle, keywords | Within limits, no trademarks |
| 2.3.10 No other platforms in metadata | Description and screenshots mention only Apple platforms |
| 4.2 Minimum functionality | Full remote: replays, recording, audio mixer, capture setup, clip actions |
| 5.1.1(i) Privacy policy | URL in App Store Connect, link in the app |
| 5.1.1(v) Account deletion | Not applicable, no accounts |
| 5.1.2 Data use and sharing | Nothing collected or shared, label says Data Not Collected |
| Local network permission | `NSLocalNetworkUsageDescription` explains why; denial is handled with a Settings shortcut |
| Privacy manifest | `PrivacyInfo.xcprivacy` bundled |
| iPad multitasking | All four orientations supported on iPad |
| Build SDK | Archive with Xcode 26 or later (iOS 26 SDK), required since April 2026 |
| Build number | Bump `CFBundleVersion` in `Info.plist` before each upload |
