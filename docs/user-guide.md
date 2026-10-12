# User guide

This guide walks through setting up ReplayMac Companion, connecting it to your Mac and using every control. The app is called **ReplayCap** on iPhone and iPad, matching the Mac App Store name, and **ReplayMac** on Android. Both work the same way and with either edition of the Mac app. Where they differ, it's called out.

## On your Mac

The companion talks to the Companion server built into ReplayMac. To check it's running:

1. Click ReplayMac in the Dock to open its main window.
2. Choose **Companion** in the sidebar.
3. Make sure the switch under **Server Status** is on. It reads "Companion Server Active" when it's running.

The server is off until you turn it on, and then listens on port **7375**. If it can't start, for example because another app already uses the port, the reason shows under the switch. The Companion page also shows:

- **Server Name**: the name your phone shows for this Mac. Click **Edit** to change it, or **Reset** to go back to the Mac's own name.
- **Local IP Address**: the addresses you can type into the phone if discovery doesn't work. The eye button hides them, which is handy when you're streaming or sharing your screen.
- **Port**: change it if 7375 clashes with something else (any port from 1024 to 65535).
- **Android over USB**: the `adb reverse` command for Android phones on a cable.
- **Paired Devices**: every phone you've allowed, with a green **Active** badge when it's connected. You can **Rename** or **Revoke** each one, or **Revoke All Devices…** at once.

## Connecting your phone

Open the app and you'll see **Connect to your Mac**, with up to four ways in.

### On This Network (Wi-Fi)

When your phone and Mac are on the same Wi-Fi, the Mac appears here within a few seconds, listed by its server name and address. Tap **Connect**.

If the list says "No Macs found", tap the refresh button at the top of the screen to search again.

On iOS, the first launch asks for permission to find devices on your local network. The app can't see your Mac without it. If you tapped **Don't Allow** by mistake, the app tells you Local Network access is off and gives you an **Open Settings** button. You can also find it in the iOS Settings app under Privacy & Security > Local Network.

### USB (Android)

1. Plug your phone into your Mac and make sure USB debugging is on.
2. Run this on your Mac:

   ```bash
   adb reverse tcp:7375 tcp:7375
   ```

   The app shows the same command under **USB**, with a button to copy it.
3. Tap **USB Cable** in the app. It connects to `127.0.0.1:7375`, which `adb` forwards to your Mac.

If you changed the server's port on the Mac, use that port in the command instead (the Mac's Companion page shows the exact command).

### iPhone and iPad

The iOS app connects over Wi-Fi only. iOS gives apps no way to reach a Mac through a plain USB cable, so there's no USB option like Android's. If your Mac doesn't show up under **On This Network**, use **Enter Manually** below.

### Enter Manually

Type your Mac's IP address (or host name) and port, then tap **Connect**. You'll find the Mac's addresses on ReplayMac's Companion page. The port defaults to 7375 when left blank or out of range. The fields remember the last address you connected to.

### Pairing

The first time a phone connects, the app shows **Waiting for approval** with a 4-digit code, and ReplayMac asks on your Mac whether to allow the device, showing a code too. Check the two codes match, then click **Allow** and the phone moves straight to the Home screen. If they don't match, the request isn't from your phone, so click **Deny**. Allow has to be clicked with the mouse; pressing Return does nothing, so you can't approve a device by accident while you're typing or playing. Nobody answering within a minute counts as no.

You only pair once. After that, ReplayMac recognises the phone and lets it straight in, until you revoke it on the Mac.

## Previously Connected

Every Mac you've paired with is listed under **Previously Connected**, most recent first. A green dot and its current address mean the Mac is reachable right now; a grey dot means "Offline". Tap **Connect** to go back to it.

To remove a Mac, tap the bin icon and confirm **Forget**. This removes that record from the phone's saved list only. ReplayMac keeps the device authorised on the Mac until you revoke it in **Paired Devices** on the Mac, so connecting again later lets the phone straight in.

### Reconnecting automatically

When you open the app, or the connection drops while you're using it, the app goes looking for the last Mac you used and shows **Reconnecting** while it searches. It tries wherever the Mac shows up on the current network first, then the address it used last time, then USB. It keeps trying in the background of that screen until it finds the Mac, so you can move between networks and it'll pick the Mac up again.

The search only runs while the app is open on screen. Tap **Cancel** to stop it and choose a Mac yourself. If the Mac denies or revokes the phone, the app stops searching and forgets that Mac.

[Connection and reconnection](connection.md) has the full details.

## The Home screen

Once connected, the top of the screen shows your Mac's name and "Connected • *your phone's name*", using whatever names you've set in ReplayMac. Three buttons sit next to it:

- **Bulb**: turns Deck mode on or off (see below).
- **Question mark**: opens the quick guide.
- **Power**: disconnects from the Mac. On Android, the system Back gesture disconnects too.

### Status card

The status card shows what ReplayMac is doing right now:

- **Not recording**, **Replay buffer running** or **Session recording**.
- How long it's been running and which display is being recorded. "(Fallback)" after the display name means your preferred display wasn't available and ReplayMac is recording another one.
- When nothing is running, a reminder of how many seconds the buffer will keep once you start it.

The red button on the card starts the replay buffer, and reads **Stop Buffer** while it's running.

While the buffer runs, two meters show how much replay is ready to save:

- **Quick replay**: how much of the replay buffer has filled, with the memory it's using.
- **Extended replay**: how much of the longer on-disk buffer has filled. This one only appears when extended replay is turned on in ReplayMac.

Each meter reads "Filling…" until it's full and then "Ready".

### Controls

| Tile | What it does | What the subtitle tells you |
| --- | --- | --- |
| **Save Last 30s** | Saves the replay buffer, like ReplayMac's Save Replay hotkey. The number matches your buffer length. | "Ready" when there's something to save, "Buffering…" just after the buffer starts, "Buffer stopped" when it isn't running, then "Saving…", "Saved" or "Save failed". |
| **Save Last 5m** | Saves the extended replay. | "Extended replay" when available, "Turned off" when extended replay is off in ReplayMac. |
| **Start Buffer / Stop Buffer** | Starts or stops the replay buffer. | "Running" or "Stopped". |
| **Record Session / Stop & Save Session** | Starts a full session recording, or stops and saves it. | The session's elapsed time while it records, "Full recording" otherwise. |
| **Microphone** | Mutes or unmutes the microphone. | "On", "Muted" or "Off". |
| **System Audio** | Mutes or unmutes system audio. | "On", "Muted" or "Off". |

The audio tiles stay tappable when an input is switched off, because ReplayMac turns it on and unmutes it when toggled. Saving is blocked while another save is in progress.

On small phones and in short landscape windows the tiles use shorter titles, such as "Save 30s" and "Stop Session".

### This Session

Clips you save while ReplayMac is open appear under **This Session**, newest first. Each row shows a thumbnail, the clip's type (Replay, Extended Replay or Session), length, size and the time it was saved. The list empties when you quit ReplayMac.

Tap a clip to open its details:

- **Show in Finder** reveals the clip on your Mac.
- **Delete** moves the clip to the Trash on your Mac after you confirm. To get it back, restore it from the Trash on the Mac.

### Deck mode

Deck mode keeps your phone's screen on so the controls are always ready, which is the whole point when the phone is propped up beside your keyboard. Tap the bulb at the top of Home to turn it on or off. The app remembers your choice.

## Quick guide

The quick guide opens automatically the first time you launch the app. It covers what the app does, how to connect, the difference between the replay buffer and session recording, and Deck mode. Reopen it any time with the question mark button.

## Troubleshooting

**Your Mac isn't listed under On This Network.** Check that ReplayMac is open, the Companion server is on, and both devices are on the same Wi-Fi. Guest networks, hotel Wi-Fi and some office networks stop devices from seeing each other, so try **Enter Manually** with the Mac's IP address. On iOS, check that Local Network access is allowed for the app.

**"Nothing is listening there. Check Companion Server is on in ReplayCap."** The phone reached your Mac but nothing answered on that port. Turn the Companion server on, and check the port matches the one on ReplayMac's Companion page.

**"Your Mac didn't respond. Check it's awake and on the same network."** The Mac might be asleep, on a different network, or blocked by a firewall. If you use the macOS firewall, allow incoming connections for ReplayMac.

**"Your Mac isn't reachable from this network."** The phone has no route to that address, usually because one device has moved to a different network.

**"That address couldn't be found."** Check the host name or IP address you typed.

**"The connection to your Mac was interrupted."** The connection dropped after it was working, for example because the Mac went to sleep or the phone changed networks. The app starts reconnecting by itself.

**"Pairing denied by …"** Someone clicked **Deny** on the Mac. Connect again and click **Allow** on the Mac.

**"“…” removed this phone."** The phone was revoked in ReplayMac's Paired Devices list. Connect again to pair it.

**"“…” couldn't verify this phone."** The Mac no longer recognises this phone's pairing, for example after the Mac's settings were reset. Connect again to pair it.

**"Another device is waiting for approval on the Mac."** Someone else's Allow prompt is already up. Answer it on the Mac, then try again.

**"Pairing was declined on the Mac. Try again in a moment."** After a Deny, or a prompt nobody answered, the Mac waits 30 seconds before it will ask about the same phone again.

**"Update ReplayCap Companion on this device to connect."** The app is older than the Mac. Update it from the App Store or Google Play.

**USB on Android doesn't connect.** Run `adb devices` to check the phone shows up, then run the `adb reverse` command again. It needs to be run each time the phone is plugged back in.

**The controls don't match the Mac.** Leave the app and come back; it asks the Mac for a fresh copy of its state every time it returns to the foreground.
