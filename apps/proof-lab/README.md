# ProofLab

A bread and pizza dough proofing app for iOS and Android. It keeps the fermentation schedule,
badges you when a stage is up — including overnight in the fridge — scales a formula across
mixer loads for production, and keeps a prototyping log so you can tell why batch 14 was
better than batch 13.

```
apps/proof-lab/
  SPEC.md          the shared domain model, in prose and formulas
  doughcore/       Kotlin/JVM reference implementation — compiles and tests on a plain JDK
  fixtures/        golden values generated from doughcore
  ios/             DoughKit (Swift package) + ProofLab (SwiftUI app) + Xcode project
  android/         Compose app on top of doughcore
```

## The idea

Every stage's real duration is converted into **equivalent hours at 24 °C** using a
piecewise-Q10 curve. That single number makes a 48-hour fridge retard and a 7-hour bench bulk
directly comparable, and everything else hangs off it: the yeast or levain suggestion, the
warning that you're about to over-ferment, the effect of moving a stage two degrees.

`SPEC.md` has the formulas and the reasoning, including where the model is a defensible
starting point rather than a fact — yeast dosing especially, where real practice varies more
than fivefold between traditions for the same fermentation load.

## What's in it

- **Eighteen dough styles**, twelve of them pizza — Neapolitan, New York, Detroit, Sicilian,
  Roman teglia, contemporary biga, poolish, a semola blend, half whole-grain, and four
  sourdough pizzas on different schedules — plus country sourdough, focaccia, baguette and an
  enriched sandwich loaf.
- **Thirteen fermentation types**: same-day straight, cold ball retard, cold bulk retard,
  Neapolitan direct, poolish, biga, Detroit pan, focaccia cold bulk, three sourdough
  schedules, sourdough country loaf, and a poolish baguette. Every stage's duration and
  temperature is editable, and the leaven suggestion follows.
- **Flour blends** as first-class parameters, with a library of fourteen flours carrying
  typical protein and absorption figures. Blend protein, whole-grain share (which speeds
  fermentation and lightens the dose) and an absorption guide are derived; every ingredient
  list splits flour into one row per component.
- **Scheduling both ways**: forward from a mix time, or backward from "dough on the bench at
  five" — which is the one a kitchen actually needs.
- **Cold-ferment windows**: how long past ready the dough stays usable, alerted at both edges,
  so a tray doesn't quietly go over.
- **Production scaling**: mixer capacity in, number of loads and per-load weights out, plus
  desired dough temperature by the factor method with an ice split when the tap won't do.
- **Prototyping log**: rate a bake across seven axes, clone it, change one thing, and diff the
  two runs side by side.

## Alerts and badges

Alerts are scheduled ahead of time, because a proofing app that only works while you're
looking at it is not much of a proofing app.

- **iOS** schedules a local notification per stage boundary, each carrying its own precomputed
  cumulative badge number, so the icon count stays correct through an overnight retard with
  the app never running. Notification actions mark a stage done or add fifteen minutes.
- **Android** sets exact alarms (`USE_EXACT_ALARM`, the permission meant for alarm and timer
  apps) and re-arms them after a reboot. Android has no numeric app badge, so the equivalent
  is a grouped notification carrying the count of stages waiting on you; launchers that
  display a number read it from there, and the rest show a dot.

## Building

**Android core** — no SDK needed, just a JDK:

```sh
cd doughcore
gradle test           # 93 tests
gradle writeFixtures  # regenerates ../fixtures/conformance.json
```

**Android app** — needs the Android SDK:

```sh
cd android
./gradlew :app:assembleDebug
```

`doughcore` is included as a composite build, so the app and the tests share one source of
truth without publishing anything.

**iOS** — open `ios/ProofLab.xcodeproj` in Xcode 16 or later and run. The project uses
synchronized folders, so new files under `ios/ProofLab/` are picked up without touching the
project file. `DoughKit` is referenced as a local Swift package; its tests run from the same
scheme.

## Keeping the two platforms honest

The same model is implemented twice, which is exactly the kind of thing that drifts. So
`doughcore` generates `fixtures/conformance.json` — temperature curves, yeast and levain
doses, plan fermentation loads, every style's ingredient weights, blend properties, water
temperatures and production splits — and the Swift test suite replays those golden values
through `DoughKit`. If either side changes a constant, a test goes red.

### What has and hasn't been compiled

The Kotlin core compiles and its tests pass. The Compose app and the entire iOS side have
**not** been compiled: this repository's development container has no Swift toolchain
(`download.swift.org` is blocked by network policy) and no Android SDK (`dl.google.com` is
blocked too). Expect to fix small compile errors on first build. The numbers should be right;
the syntax around them is the part that hasn't been proven.
