# Submitting Drillbit to the Kotlin Multiplatform Contest 2027

Deadline: **Monday 7 December 2026, 23:59 CET** (17:59 EST / 16:59 CST, Hattiesburg time).
Aim to submit by 30 November so there is slack for surprises.

Form: <https://forms.gle/vh24TG4hvBhrtMja8> (the rules page links <https://forms.gle/B4QsY1ZgiotK9ZqX8>;
both redirect to the same Google Form, "Kotlin Multiplatform Contest Submission Form").

## What the form asks, pre-written

The form has three pages. Field names below were copied from the live form on 2026-10-27.
A second reviewer could only fetch page 1 (Email, Terms checkbox) and confirmed it matches.
**Pages 2 and 3 must be re-checked live** when you open the form: Google Forms can change, so
compare each field name and required flag with the tables below before pasting.

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

Paste the essay below. The rules (Step 3) ask for "a 300-word essay" covering educational and
professional background, coding experience, hobbies "if relevant to the topic of the submission",
and the idea and technologies. They do not say whether 300 is a cap, so treat it as one.

The draft is 279 words without the `[[...]]` line. **Replace that line with your own sentence of
21 words or fewer** (work or internship experience, and a hobby only if it relates to the
project; if you have none, say so briefly), then recount. Read every sentence and change
anything that is not exactly true for you.

```
I'm Abdelrahman Teima, a computer science sophomore at the University of Southern Mississippi. Most of my programming has been competitive programming in C++ and Python; this year my team won the ICPC South USA regional. [[One line: any work or internship experience, and a hobby if it relates to the project. Or say you have none yet.]] Kotlin was new to me when I started this project.

Drillbit is a formation designer for marching bands. You type a word or pick a shape, and the app decides which of the 64 performers walks to which spot. It uses the Hungarian algorithm to minimise the sum of squared distances. A robotics result, CAPT (Turpin, Michael and Kumar), proves this keeps straight, synchronised paths at least one step apart whenever spots start and end at least 1.41 steps apart, which is how a band moves between sets. Drillbit also checks every pair of paths exactly. Moving a block into "KOTLIN", drawing order gives 56 collisions, minimising plain distance still gives 21, and squared distance gives none.

The second half is for performers. Each member gets their own dot book: yard-line position, step size in band language ("6 to 5") and direction, set by set. The drill writer works on a laptop and performers need a phone on the field, so the app shows a QR code carrying the whole show, packed with my own binary codec and drawn by a QR encoder I wrote from the spec. Scanning it opens the web version with no install and no account.

Everything is Kotlin Multiplatform with Compose Multiplatform. The model, planner, codec, QR encoder and the whole UI are common code, so one planner and one codec run on Android, desktop and the browser through Kotlin/Wasm. Platform code is only storage, sound and the clipboard.
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

- `desktop-*.png`, `phone-*.png`: rendered by the desktop screenshot tests (`DRILLBIT_SCREENSHOTS=<dir> ./gradlew :composeApp:desktopTest`; without the variable they go to `composeApp/build/screenshots`).
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
2. **Publish: repo, web app and APK in one command** (2 min of your time, about 15 min of waiting).
   In PowerShell:
   ```
   & "C:\Dev\AI Automation\hackathons\kmp-contest\publish.ps1"
   ```
   It creates the public repo `UnknownHacker1/drillbit` and pushes `main`, turns on GitHub Pages
   with GitHub Actions as the source, runs the "Deploy web app" workflow and waits for it (tests,
   Wasm build, publish to `https://unknownhacker1.github.io/drillbit/`), builds the debug APK and
   attaches it to release v1.0.0, then checks the page answers. Every step is skipped when already
   done, so re-running is safe. `-NoApk` skips the release, `-Redeploy` reruns the workflow,
   `-DryRun` only checks. If GitHub refuses the workflow file (missing `workflow` scope), it
   prints the two `gh auth` commands to fix that.
   If you use a different repo name or account, change `PUBLIC_WEB_APP_URL` in
   `composeApp/src/commonMain/kotlin/dev/teima/drillbit/platform/Platform.kt` (desktop and
   Android share links point there) and the links in README.md, then commit.
3. **Check the web app** on your phone and a laptop (needs a browser with WebAssembly GC: current
   Chrome, Edge, Firefox, Safari 18.2+).
4. (folded into step 2)
5. **Record the screencast** (45-60 min, optional). Script: SUBMISSION/SCREENCAST.md.
6. **Submit the form** (10 min). Copy the fields above. Screenshot the confirmation page.
7. **Join the contest Slack channel** for announcements:
   <https://slack-chats.kotlinlang.org/c/kotlin-multiplatform-contest>.

## After submitting

- Keep committing small improvements if you want, but the judges may look at any point after
  the deadline; never leave `main` broken.
- Winners are announced around 21 December 2026 on KotlinConf's X account and the Slack channel.
