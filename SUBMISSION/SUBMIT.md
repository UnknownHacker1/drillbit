# Submitting Drillbit to the Kotlin Multiplatform Contest 2027

Deadline: **Monday 7 December 2026, 23:59 CET** (17:59 EST / 16:59 CST, Hattiesburg time).
Aim to submit by 30 November so there is slack for surprises.

Form: <https://forms.gle/vh24TG4hvBhrtMja8> (the rules page links <https://forms.gle/B4QsY1ZgiotK9ZqX8>;
both redirect to the same Google Form, "Kotlin Multiplatform Contest Submission Form").

## What the form asks, pre-written

The form has three pages. Field names below are copied from the live form (read on 2026-10-27).

### Page 1

| Field | Required | Answer |
|---|---|---|
| Email | yes | your email (the one you check; winners are contacted here) |
| Contest Terms of Service: "I have read and accept these Terms of Service" | yes | read the rules at <https://www.kotlinconf.com/contest-rules/>, then tick it yourself |

### Page 2: Your submission

**Link to your project** (required)

```
https://github.com/UnknownHacker1/drillbit
```

The form's help text says the project must be "fully functioning, be built with Kotlin 1.7.0 or
higher, and run without any additional setup needed (e.g. it must have all of the API keys
configured...) on at least two different platforms". Drillbit uses Kotlin 2.4.21, needs no
keys or accounts, and runs on Android, Desktop and Web.

**Essay about your background** (required; "Tell us about your background, the idea behind your
project, and the technologies you used to develop it." The rules ask for about 300 words.)

Paste the essay below. It is a draft in your voice: read it, change anything that is not
exactly true for you, and add one personal sentence if you have a real connection to marching
band, dance, or drone shows. The rules also say the essay should cover hobbies; add one line
about yours. Word count of the draft: 308.

```
I'm Abdelrahman Teima, a computer science sophomore at the University of Southern Mississippi. Most of my programming has been competitive programming in C++ and Python; this year my team won the ICPC South USA regional. That taught me to love algorithms, and left me wanting to build something people actually hold in their hands. Kotlin was new to me when I started this project.

Drillbit is a formation designer for marching bands and anyone who moves a group of people across a field. You type a word or pick a shape, and the app decides which of the 64 performers walks to which spot so that nobody collides. Under the hood that is the Hungarian algorithm minimising the sum of squared distances. A paper from robotics (CAPT, Turpin, Michael and Kumar) proves that this choice is collision-free when everyone starts and stops together and walks in straight lines, which is exactly how a band moves between sets. Drillbit also checks every pair exactly, so you can watch a naive plan collide dozens of times and the optimal one never.

The second half of the app is for the performers. Every member gets their own dot book: yard-line coordinates, step size in band language ("6 to 5") and direction, set by set. The drill writer works on a laptop, performers need a phone on the field, so the designer shows a QR code that carries the entire show, compressed with my own binary codec and drawn by a QR encoder I wrote from the spec. Scanning it opens the web version with nothing to install, no server and no account.

Everything is Kotlin Multiplatform with Compose Multiplatform: the model, planner, codec, QR encoder and the whole UI live in common code and run on Android, desktop and the browser through Kotlin/Wasm. Platform code is only storage, sound and clipboard.
```

**Link to short screencast (three to five minutes long)** (optional, but recommended by the rules)

Record one (script below), upload to YouTube as Unlisted, paste the link. If you skip it, leave blank.

### Page 3: Personal details

| Field | Required | Answer |
|---|---|---|
| Full name | yes | Abdelrahman Teima |
| Country | yes | United States (or the country you reside in; check eligibility: residents of Brazil, Quebec, Italy and sanctioned territories are excluded) |
| Academic institution you're affiliated with | yes | University of Southern Mississippi |

## Screenshots

`SUBMISSION/screens/` has captures from three platforms, all real renders of this build:

- `desktop-*.png`, `phone-*.png`: rendered by the desktop test (`DRILLBIT_SCREENSHOTS=<dir> ./gradlew :composeApp:desktopTest`).
- `android-*.png`: the debug APK on the Android 16 emulator (Medium Phone, API 36).
- `web-*.png`: the Kotlin/Wasm production build in a browser. The URL in `web-share-qr.png` is a
  localhost one; on GitHub Pages it becomes `https://unknownhacker1.github.io/drillbit/#show=...`.

## Eligibility check (from the rules)

- 18 or older (or age of majority where you live, if higher).
- Enrolled full-time in an accredited undergraduate or graduate program (or within 12 months before the contest start).
- One entry per person, solo. Project created for this contest.
- Repository must contain a LICENSE file with Apache 2.0, MIT, or BSD 3-clause. Drillbit is MIT.
- If you win you must sign an affidavit within 7 days of notification. Visas, meals and insurance are not covered. As an F-1 student, check travel and re-entry for Poland (Schengen visa) early if you plan to go.

## Steps you must do (about 1.5 to 3 hours total)

1. **Read the code and the essay, make them yours** (45-90 min). You will be asked about this
   project if you win. Run it on all three targets once (commands in the README). Edit the essay.
2. **Create the GitHub repo and push** (10 min).
   ```
   gh repo create UnknownHacker1/drillbit --public --source . --remote origin
   git push -u origin main
   ```
   If you use a different repo name or account, change `PUBLIC_WEB_APP_URL` in
   `composeApp/src/commonMain/kotlin/dev/teima/drillbit/platform/Platform.kt` (desktop and
   Android share links point there) and the links in README.md, then commit.
3. **Publish the web app on GitHub Pages** (10 min, recommended: judges can try it without
   installing anything).
   - Repo Settings -> Pages -> Build and deployment -> Source: **GitHub Actions**.
   - Actions tab -> "Deploy web app" -> Run workflow. It runs the tests, builds the Wasm app and
     publishes it to `https://unknownhacker1.github.io/drillbit/`.
   - Open that URL on your phone and on a laptop to check it loads (needs a browser with
     WebAssembly GC: current Chrome, Edge, Firefox, Safari 18.2+).
4. **Attach the APK to a GitHub release** (5 min, optional but nice for judges with Android phones).
   ```
   ./gradlew :androidApp:assembleDebug
   gh release create v1.0.0 androidApp/build/outputs/apk/debug/androidApp-debug.apk --title "Drillbit 1.0.0" --notes "Debug APK for Android 8.0+"
   ```
5. **Record the screencast** (45-60 min, optional). Script: SUBMISSION/SCREENCAST.md.
6. **Submit the form** (10 min). Copy the fields above. Screenshot the confirmation page.
7. **Join the contest Slack channel** for announcements:
   <https://slack-chats.kotlinlang.org/c/kotlin-multiplatform-contest>.

## After submitting

- Keep committing small improvements if you want, but the judges may look at any point after
  the deadline; never leave `main` broken.
- Winners are announced around 21 December 2026 on KotlinConf's X account and the Slack channel.
