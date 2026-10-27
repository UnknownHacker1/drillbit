# Design notes

## What the contest rewards

From the official rules (kotlinconf.com/contest-rules):

| Criterion | Weight |
|---|---|
| Creativity and novelty of the idea | 40% |
| Use of Kotlin Multiplatform and/or share of Kotlin code shared across platforms | 40% |
| Alignment with Kotlin coding conventions | 20% |

Ties go to creativity. The app has to run on at least two platforms with no extra setup
(no API keys to configure), has to be made for the contest, and must not be a template,
a "Hello World" or a library.

So the idea has to be one judges have not seen fifty times, the multiplatform part has to
be the point of the app rather than a checkbox, and almost all code should live in
`commonMain`.

## Five candidates

Scores are 1 to 10. Creativity = how unlikely a judge has seen it before and how memorable
it is. Feasibility = can one student ship it well in six weeks, on Android + Desktop + Web.

| # | Idea | One-line pitch | Creativity | Feasibility |
|---|---|---|---|---|
| 1 | **Drillbit** | A marching-band / dance-crew formation designer: type a word or pick a shape and 64 performers walk into it on collision-free paths (optimal assignment), then every performer gets their own step-by-step "dot book" on their phone via a QR code. | 9 | 8 |
| 2 | Loom Companion | Weaving-draft designer where the drawdown is a boolean matrix product of threading x tie-up x treadling; desktop to design, phone at the loom steps through picks. | 9 | 7 |
| 3 | Phone Pixels | Serverless crowd light show: every phone in a stadium section enters its seat and renders its pixel of a shared animation from the wall clock alone. | 8 | 5 |
| 4 | Softlock | Escape-room / puzzle-hunt designer that model-checks the puzzle dependency graph for softlocks and dead ends with state-space search. | 7 | 7 |
| 5 | Stitchwise | Photo to cross-stitch pattern (CIELAB k-means to thread colours, confetti cleanup), phone tracks stitching progress. | 5 | 8 |

Why the others lost:

- **Loom Companion** is the most surprising, but the audience is tiny and a judge cannot
  "feel" whether a drawdown is right in a 2-minute look. Screenshots read as abstract grids.
- **Phone Pixels** depends on phones' clocks agreeing to ~50 ms and can only really be
  demoed with a crowd. Judges would have to imagine it working.
- **Softlock** is clever but the visuals are a graph and a list of states. Weak screenshots.
- **Stitchwise** exists in many forms already (Pic2Pat, StitchFiddle and others).

## Pick: Drillbit

1. **Novel for this audience.** Drill-writing software exists (Pyware, EnVision) but it is
   paid, desktop-only and closed. Nobody at KotlinConf has seen 64 dots spell "KOTLIN" and
   then fold into the Kotlin logo without a single collision, computed live in a browser.
2. **Multiplatform is the product, not a checkbox.** The two halves of the app are used in
   different places on different devices:
   - The **drill writer** works best on a big screen: Desktop or the Web build.
   - The **performer** needs it on a phone, on the field, in their hand: Android, or the
     same web build opened from a QR code with nothing installed.
   - The handoff between them is a QR code / link that carries the whole show (no server,
     no account, works offline). One Kotlin codec writes it on desktop and reads it on the
     phone.
3. **Heavy logic in shared code, which is also the entrant's real edge.**
   - Optimal assignment (Hungarian algorithm, O(n^3)) of performers to target spots,
     minimising the sum of squared distances. This is exactly the CAPT setting from
     robotics (Turpin, Michael, Kumar 2014): when everyone starts and stops together and
     moves in straight lines, the squared-distance optimum is provably collision-free if
     spots are at least 2*sqrt(2)*R apart. A marching band moving on the same counts is that
     setting.
   - Exact closest-approach check for every pair of performers per transition (solves a
     quadratic per pair), so the app can prove "0 collisions" and show a naive order
     colliding.
   - Shape sampling by arc length (text through a hand-made stroke font, arcs, stars, the
     Kotlin logo), step-size maths in band language ("8 to 5"), yard-line coordinate
     readouts.
   - A compact binary show codec (delta + zig-zag varints + CRC-32, base64url) and a QR
     code encoder written from scratch (Reed-Solomon over GF(256), masking, BCH format bits).
   None of that is a competitive-programming app, but it is the kind of code an ICPC
   champion writes well.
4. **Plausible student project.** A student who knows algorithms, sees the band practise on
   campus, and notices that every member still gets their positions on paper "dot sheets".
5. **Demo-able in 30 seconds.** Open the web link, press play, watch the formations morph,
   flip the "naive vs optimal" switch and watch the collision count go from 20+ to 0.

## Scope

Must have (for submission):

- Field canvas with yard lines and hashes, dots coloured by section, drag to edit.
- Sets timeline with counts, add / delete sets, playback with a count display.
- Formation generator: text, line, arc, circle, block, wedge, star, Kotlin logo.
- Optimal (CAPT) transitions + exact collision report; naive comparison.
- Performer mode: pick yourself, see your dot book (yard-line coordinates, step size,
  direction) and your path highlighted.
- Show code + QR code, import from link (web), paste (all), saved locally (all).
- Targets: Android, Desktop (JVM), Web (Wasm).
- commonTest coverage for every algorithm. ktlint clean.

Not in scope: audio sync to music, 3D view, iOS build (needs a Mac to compile;
the shared code has no JVM-only APIs, so adding iOS later is a target, not a rewrite).

## Coordinate system

- Units are 8-to-5 steps (22.5 inches): 8 steps = 5 yards.
- x runs from the left goal line (0) to the right goal line (160). The 50 yard line is x = 80.
  Left of the 50 is "Side 1", right is "Side 2" (audience view).
- y runs from the front sideline (0, audience side) to the back sideline (85.33).
- High school hashes are 28 steps in from each sideline (front hash y = 28, back hash
  y = 57.33). College hashes are at 32 steps.
- Collision radius: 0.5 step (performers closer than 1 step centre to centre). The CAPT
  guarantee then needs spots at least 1.42 steps apart; the formation tools default to
  2-step intervals.
