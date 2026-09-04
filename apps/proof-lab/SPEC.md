# ProofLab — shared domain spec

Both the iOS (Swift) and Android (Kotlin) apps implement the same model. This file is
the source of truth; `fixtures/conformance.json` holds golden values that both test
suites assert against, so the two platforms can't silently drift apart.

All temperatures are °C, all weights grams, all durations hours unless stated.

---

## 1. Temperature model (piecewise Q10)

Fermentation rate roughly follows a Q10 curve, but a single Q10 badly underestimates how
much a fridge slows a dough. We use three segments:

| Temperature range | Q10  |
| ----------------- | ---- |
| ≤ 10 °C           | 3.0  |
| 10–20 °C          | 2.5  |
| > 20 °C           | 2.0  |

Define a log-activity function, integrating `ln(Q10)/10` over temperature from 0 °C:

```
logActivity(T ≤ 0)      = T · ln(3.0)/10
logActivity(0 < T ≤ 10) = T · ln(3.0)/10
logActivity(10 < T ≤ 20)= 10·ln(3.0)/10 + (T-10)·ln(2.5)/10
logActivity(T > 20)     = 10·ln(3.0)/10 + 10·ln(2.5)/10 + (T-20)·ln(2.0)/10
```

Input temperature is clamped to −5 … 45 °C.

**Rate multiplier** relative to the 24 °C reference:

```
rate(T) = exp(logActivity(T) − logActivity(24))
```

**Equivalent hours at 24 °C** — the common currency of the whole app. Every stage's real
duration is converted to "how much fermentation happened", so a 48 h fridge retard and a
7.5 h room-temperature bulk are directly comparable:

```
equivalentHours(h, T) = h · rate(T)
hoursFor(equivalent e, T) = e / rate(T)
```

Sanity anchors (see fixtures): `rate(4) ≈ 0.157` (a fridge is ~6.4× slower than 24 °C),
`rate(20) ≈ 0.758`, `rate(30) ≈ 1.516`.

## 2. Leavening from fermentation load

Total fermentation load `EH` is the sum of `equivalentHours` over stages that actually
ferment (bulk, folds, bench, balling, cold retard, temper, final proof). Preferment
stages are excluded — a preferment carries its own yeast, computed the same way from its
own duration and temperature.

**Instant dry yeast**, as a percentage of total flour:

```
idy% = 1.6 / EH
     × (1 + 0.14 · max(0, salt% − 2))      // salt slows yeast
     × (1 + 0.03 · max(0, sugar% − 5))     // osmotic drag in enriched doughs
     × (1 − 0.8 · prefermentedFlourFraction)
     × (1 − 0.3 · wholeGrainFraction)      // bran ferments faster, see §3
clamped to 0.02 … 1.5
```

The constant is calibrated against the cold-fermentation tables American pizzerias work
from — measured on cold time alone, `EH` of 3.8 / 7.5 / 11.3 (24 / 48 / 72 h at 4 °C)
gives 0.42 / 0.21 / 0.14 %, which is the published range.

**This is a starting point, not a verdict.** Real doses vary by more than 5× between
traditions for the same fermentation load: an AVPN Neapolitan runs ~0.06 % IDY over 8 h at
20 °C, while a US same-day dough of comparable load runs ~0.4 %. No single curve fits both,
because "done" means a different amount of rise in each. So the app:

- seeds every style preset with that tradition's real-world dose,
- shows the suggestion next to yours rather than overwriting it,
- recomputes the suggestion live as you change stage times and temperatures, which is the
  part that genuinely transfers between traditions — if you double the cold time, halve the
  yeast, whatever your baseline was.

Other yeast forms are weight multiples of IDY: active dry × 1.25, fresh/cake × 3.0.

**Sourdough levain**, as a percentage of total flour (levain weight, not its flour):

```
levain% = 90 / EH
        × (1 + 0.14 · max(0, salt% − 2))
        × (1 − 0.3 · wholeGrainFraction)     // see §3
clamped to 3 … 40
```

≈ 20 % levain for a 4.5 h bulk, dropping as the schedule lengthens.

## 3. Flour blends

A formula carries a list of flours, each a share of the total flour. One entry is the
ordinary case; several is where prototyping starts — 70 % 00 with 30 % semola behaves
nothing like straight 00.

Shares are **normalised before use**, so a blend that adds to 97 % on screen still weighs out
correctly. Derived properties are weighted by the normalised shares:

```
protein        = Σ(protein · share)
wholeGrain     = Σ(share) over flours flagged whole grain
absorptionGuide = 50 + (protein − 10)·2.2 + Σ(absorptionOffset · share)
```

The absorption guide is a **floor, not a target** — roughly what the blend carries
comfortably. Style takes you above it; a Roman teglia goes far above it on purpose.

Whole grain also feeds back into leavening. Bran carries enzymes and wild yeast, so a
whole-grain dough runs faster than its protein suggests:

```
× (1 − 0.3 · wholeGrainFraction)
```

on both the yeast and levain suggestions. Every ingredient list splits flour into one row per
component — totals, the mix sheet, and each mixer load — and a preferment is assumed to take
the same blend, scaled down.

## 4. Formula (baker's percentages)

Percentages are relative to **total flour**, which includes flour inside a preferment.

```
totalDough = ballCount · ballWeight · (1 + loss%/100)
yeastWeight% = idy% · yeastType.multiplier              (0 for sourdough)
prefYeast%   = prefFlour%/100 · prefIdy% · multiplier   (0 for levain)
sum% = 100 + hydration% + salt% + oil% + sugar% + malt% + yeastWeight% + prefYeast%
totalFlour = totalDough / (sum%/100)
ingredient = totalFlour · pct/100
```

A preferment redistributes rather than adds:

```
prefermentFlour = totalFlour · prefermentedFlour%/100
prefermentWater = prefermentFlour · prefermentHydration%/100
finalFlour      = totalFlour − prefermentFlour
finalWater      = totalWater − prefermentWater
```

Levain build (hydration `hl`, seed `r` % of the levain's flour):

```
seedFlour = prefermentFlour · r/100
seed      = seedFlour · (1 + hl/100)      // mature starter, assumed at hydration hl
feedFlour = prefermentFlour − seedFlour
feedWater = feedFlour · hl/100
```

**True hydration** is reported separately from the water percentage because eggs, milk and
oil change how a dough behaves; only water and the water inside a preferment count.

## 5. Production scaling

```
mixes = ceil(totalDough / (mixerCapacityKg · 1000))
```

Dough is split evenly across mixes; balls are distributed `floor(n/mixes)` each with the
remainder spread one-per-mix across the first mixes, so no mix is short by more than one
ball.

## 6. Dough temperature (DDT)

Classic factor method. Three factors without a preferment, four with one:

```
waterTemp = DDT · factors − (flourTemp + roomTemp [+ prefermentTemp] + friction)
```

Friction defaults: hand 1 °C, fork 2 °C, double-arm 2 °C, diving-arm 3 °C, spiral 4 °C,
planetary 6 °C.

If the required water temperature is below the tap, swap part of the water for ice
(latent heat of fusion ≈ 80 cal/g):

```
ice = water · (tapTemp − targetTemp) / (80 + tapTemp)
```

Below 0 °C required water temperature the answer is "chill the flour" — the app says so
instead of returning a nonsense number.

## 7. Scheduling

A plan is an ordered list of stages, each with a duration, a temperature and flags for
alerts, fold reminders and — on cold stages — a *usable window*: how long past "ready"
the dough stays good. That window is what turns a home timer into a production tool.

Schedules anchor two ways:

- **Start at** — forward from a mix time.
- **Ready by** — backward from when dough must hit the bench; the app reports the mix time.

At runtime a batch recomputes from its actual start: each stage's end is
`start + (hours + adjustment)`, unless it was completed manually, in which case the
completion time becomes the next stage's start and everything downstream shifts.

## 8. Alerts and badges

Every stage boundary schedules a local notification. Because notifications must fire with
the app closed, badge counts are **precomputed**: all pending alerts across all batches
are sorted by fire time and each carries its own cumulative badge number, so the icon
badge is right even if the app never wakes. On foreground the app reconciles the badge
against the stages actually due.

Cold stages emit two alerts: one when the window opens, one when it closes.
