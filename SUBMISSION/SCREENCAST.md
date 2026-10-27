# Screencast script (about 4 minutes)

Record the desktop app full screen with OBS (free) or the Windows Game Bar (Win+G). Have the
Android emulator or a real phone ready for the last part, and the hosted web build open in a
browser tab. Talk over it in your own words; the lines below are a guide, not a script to read.
Upload to YouTube as **Unlisted** and paste the link into the form.

| Time | Show | Say (roughly) |
|---|---|---|
| 0:00-0:20 | Desktop app, demo show paused on set 1 | "This is Drillbit. A marching band show is a series of pictures on the field. The hard part is deciding who walks where, and getting every member their own part." |
| 0:20-0:50 | Press play, let it run Block -> KOTLIN -> logo | "64 performers. Every move you see was planned by the app, and none of them collide." |
| 0:50-1:50 | Select set 1, **New set after 1**, type your name. Tap **In order**, **Nearest free spot**, **Least total walking**, then **Least squared walking (CAPT)** | "Here's why it matters. Drawing order gives 56 collisions, the red rings. Nearest free spot still collides. Even the Hungarian algorithm minimising total distance gives 21. Squaring the distances gives zero. A robotics result, CAPT, proves that when everyone starts and stops together on straight lines and the spots are at least 1.41 steps apart, which is how bands move. I also check every pair of paths exactly." Press **Add set**. |
| 1:50-2:20 | Transition card: exact collision count, tightest spacing, PROVABLY COLLISION-FREE badge and its explanation, change counts with -4 | "Each move gets a report in band language. The proof badge only appears when the spacing condition holds; otherwise it shows the exact check instead. Fewer counts means bigger steps, so it turns orange." Drag a dot onto another path to show a red ring, then **Undo**. |
| 2:20-3:00 | Press **Share QR**. Scan it with the phone camera | "The whole show is in this QR code, packed with my own binary format and drawn by a QR encoder I wrote from the spec. No server." |
| 3:00-3:40 | Phone: web app opens in Perform mode. Tap a label, scroll the dot book, show the route | "On the phone each member picks themselves and gets their own dot book: where to stand, how big the steps are, which way to go." |
| 3:40-4:10 | Quick cut: same app on Android and in the browser side by side; then the README shared-code table | "One Kotlin Multiplatform codebase: Android, desktop, and the web through Kotlin/Wasm. The UI and all the logic are common code; each platform only adds storage, sound and the clipboard." |

Tips:
- Close notifications before recording. 1080p is enough.
- If scanning on camera is awkward, copy the link and open it on the phone instead, and say so.
- Keep it under 5 minutes.
