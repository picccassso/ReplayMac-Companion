# ReplayCap Companion Privacy Policy

**Last updated: 17 October 2026**

This policy covers the ReplayCap Companion app for iPhone and iPad (shown on the Home Screen as **ReplayCap**) and the ReplayMac companion app for Android. Both are remote controls for ReplayCap (also distributed as ReplayMac), the screen recorder that runs on your Mac.

**The companion app does not collect, sell or share any personal data.** It has no accounts, analytics, advertising, crash reporting, tracking or third-party SDKs, and it never contacts the developer or any internet server.

## What the app talks to

The app only talks to ReplayCap running on your own Mac, over your local network (and, on Android, an optional USB cable). Nothing goes to the developer or anyone else.

- **Finding your Mac.** The app looks for ReplayCap on your local network with Bonjour. On iPhone and iPad, iOS asks for Local Network permission first. You can turn that off at any time in Settings → Privacy & Security → Local Network.
- **Pairing.** The first time you connect, the app sends your Mac this phone's name and a random ID the app made for it. ReplayCap asks you to approve the phone on your Mac. Once you click Allow, the Mac gives the phone a random pairing key so it can sign in later without asking again.
- **While connected.** Your Mac sends the phone its recording status, timers, audio levels and settings, plus the names, sizes and thumbnails of clips saved this session. The phone sends back the commands you tap, such as saving a replay, starting or stopping recording, or moving a clip to the Trash.

This traffic stays between your phone and your Mac. It is not encrypted, so use a network you trust.

## What is stored on your phone

- A random ID for this phone.
- The pairing key for each Mac you've approved. On iPhone and iPad it lives in the Keychain, set to "this device only", so it is never backed up or synced.
- The Macs you've connected to (their name and last network address), the last address you typed, and simple preferences such as Deck mode and whether you've seen the quick guide.

Nothing else is stored. Clip thumbnails are only kept in memory while you're connected.

Forgetting a Mac in the app deletes its pairing key. Deleting the app deletes everything it stored. On your Mac, you can remove a phone at any time in ReplayCap's Companion settings.

## Children

The app collects no personal data from anyone, including children.

## Changes

If this policy changes, the new version will be posted here with a new date.

## Contact

Questions or problems: open an issue at [github.com/picccassso/ReplayMac-Companion/issues](https://github.com/picccassso/ReplayMac-Companion/issues).

ReplayCap's own privacy policy, for the Mac app: [github.com/picccassso/ReplayMac/blob/main/PRIVACY.md](https://github.com/picccassso/ReplayMac/blob/main/PRIVACY.md).
