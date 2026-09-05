# Dough Science — the bench rig

A lab instrument for one bench, not a product. Its only job is to answer a question the app
currently has to guess at: **how far along is this dough, really?**

The app models fermentation as *equivalent hours at 24 °C* and doses leaven from that. The
model is calibrated on published tables, and those tables disagree with each other by more than
fivefold depending on which tradition wrote them. The rig replaces that argument with your own
dough.

---

## Design stance

**Don't lay out a PCB yet.** You don't know what you're measuring for. A custom board freezes
the sensor set at exactly the moment you know least about which sensors matter. Use a dev board
and solderless breakouts, get a season of runs, *then* decide what's worth spinning.

**Over-instrument on purpose.** Storage is free and a run takes eighteen hours. Log every
channel you can, at a fine interval, and throw data away later in analysis. The run you didn't
instrument is the one you'll want back.

**No smoothing, no thresholds, no inference on the device.** The firmware's job is to write
honest numbers with honest timestamps. Every judgement happens offline where you can change your
mind and re-run it against old data. A device that decides "ready" is a device you can't
disagree with.

---

## Bill of materials

Roughly $90 for the full set, less if you stage it.

| Part | Why | Rough cost |
| --- | --- | --- |
| ESP32-S3 dev board (Qwiic/STEMMA QT) | Wi-Fi + BLE, plenty of RAM, I²C without soldering | $15 |
| **VL53L1X** ToF rangefinder | Dough height to the millimetre — the aliquot jar, automated | $6 |
| **SHT4x** temperature + humidity | Ambient conditions; the humidity channel you asked about | $5 |
| **DS18B20** in a stainless probe | Dough *core* temperature — the highest-value sensor here | $4 |
| **SCD41** NDIR CO₂ | The most direct read on yeast activity | $30 |
| Load cell (1 kg) + **HX711** | Mass loss as gas escapes — an independent check on CO₂ | $10 |
| microSD breakout | Logging that survives a dropped connection | $6 |
| DS3231 RTC | Timestamps that survive a power cut | $5 |
| Straight-sided container | Height converts to volume only if the walls are parallel | — |

### Staging it

You do not need all of this on day one. In value order:

1. **DS18B20 + microSD.** Dough temperature alone fixes the model's biggest assumption. This is
   maybe $15 and answers a real question in one bake.
2. **Add the VL53L1X.** Now you have rise, and rise is the signal bakers actually judge by.
3. **Add SHT4x.** Cheap, rides along on the same I²C bus, tells you about skin formation.
4. **Add SCD41 and the load cell** once the first two are boring — these are the research
   channels, and they need a controlled container to mean much.

---

## Wiring

Everything except the probe and the load cell is I²C on one chain:

```
ESP32-S3 ── Qwiic ── VL53L1X (0x29) ── SHT4x (0x44) ── SCD41 (0x62) ── DS3231 (0x68)
        └── GPIO + 4.7 kΩ pull-up ── DS18B20 (1-Wire, stainless probe in the dough)
        └── SPI ── microSD
        └── HX711 (2-wire) ── 1 kg load cell under the container
```

No address collisions across those four I²C parts, so they daisy-chain without a mux.

---

## The two things that ruin fridge rigs

**A fridge is a Faraday cage.** BLE and Wi-Fi through a walk-in door range from unreliable to
dead. Decide this before you design anything else. Options, best first:

1. **Log to SD, sync on the bench.** Simplest, most robust, and fine for a research rig where
   you analyse after the bake rather than during it. Start here.
2. Thin ribbon out through the door gasket. Works, mildly annoying, gasket still seals.
3. A repeater inside. More parts, more to go wrong.

**Condensation.** A cold board pulled out into a warm kitchen will sweat. Conformal-coat
everything, vent the enclosure with a GoreTex patch so it can equalise without breathing in
water, and use a food-safe stainless sheath on the probe.

---

## Data contract

The firmware writes CSV. Nothing else. It's readable by eye when a run looks wrong, opens in
anything, and survives a firmware rewrite:

```
timestamp_ms,dough_c,ambient_c,rh,height_mm,co2_ppm,mass_g
1757000000000,23.4,21.8,68.2,101.5,912,1004.2
1757000060000,23.4,21.9,68.0,102.1,948,1004.1
```

Rules the parser already assumes, so keep to them:

- **Blank means "no sensor", not zero.** A missing channel must not read as 0.0.
- **`height_mm` is the dough's height, not the rangefinder's distance.** A lid-mounted sensor
  counts *down* as dough rises; do that subtraction in firmware where you know the container.
- Timestamps are epoch milliseconds, from the RTC.
- Lines starting `#` are comments. Put the batch id in one at the top.
- A truncated final row from a power cut is fine — the parser keeps the fields that made it.

`SensorCsv.decode` in both `doughcore` and `DoughKit` reads exactly this, and its handling of
blanks, junk lines, comments and truncated rows is covered by tests on both platforms.

**Suggested interval:** 60 s. That's 1,080 rows for an 18-hour retard — nothing — and finer than
any real dough behaviour. Drop to 30 s for a warm bulk if you want a clean rise curve.

---

## What the software already does with this

In `Sensing.kt` / `Sensing.swift`, both compiled from the same spec and checked against shared
fixtures:

- `measuredEquivalentHours()` — integrates the Q10 curve over the *measured* temperature series
  instead of an assumed constant. This is the payoff. A walk-in you believe is 4 °C but which
  actually sits at 5.8 °C banks **4.59 equivalent hours in 24, against the 3.76 the plan
  assumed** — 22 % more fermentation than you scheduled for, which is enough to explain a slack
  Friday.
- `effectiveConstantTemperatureC()` — the single temperature that would have produced the same
  fermentation. Not the arithmetic mean: an hour at 30 °C and an hour at 10 °C is *further along*
  than two hours at 20 °C, and this reports that honestly.
- `longestTemperatureGapMinutes()` — gaps are integrated across, but reported, so a figure built
  over a two-hour logging hole gets labelled an estimate rather than passed off as a record.
- `expansionPercent()`, `riseRatePercentPerHour()`, `projectedHoursTo(ratio:)` — rise tracking,
  with the projection deliberately refusing to answer when the dough has stalled or is
  collapsing.
- `co2SlopePpmPerHour()`, `massLossGramsPerHour()` — the two activity channels.
- `RunComparison` — planned versus measured for a stage, with a significance flag.

**What it does not do, on purpose:** decide the dough is ready. There is no readiness threshold
anywhere in the code, because that number is the output of the experiment, not an input to it.

---

## The experiment

The rig is pointless without a protocol. This is the loop that turns runs into an answer:

1. Start a batch in the app. Note its id.
2. Put an aliquot of the same dough in the straight-sided container under the rangefinder —
   same dough, same temperature, so it tracks the mass without you disturbing the real batch.
3. Log the whole run. Comment the batch id at the top of the CSV.
4. Bake it. **Rate it in the app** — handling, extensibility, oven spring, crumb, flavour, crust.
5. Repeat, changing **one thing** at a time.

After a dozen runs you can ask the question the whole thing exists for: *at what expansion
percentage, or what CO₂ slope, did the bakes I scored highest go into the oven?* That threshold
is almost certainly different per style — a Neapolitan ball and a pan dough proofed to the rim
are not the same number — which is exactly why it has to be measured rather than assumed.

Once you have it, the app stops estimating and starts reporting.

### Runs worth doing first

- **Same dough, two temperatures.** Does the Q10 curve actually predict the ratio between them?
  This validates or breaks the core model in two bakes.
- **Your walk-in, instrumented, for a week.** Almost certainly not the temperature you think,
  and almost certainly not constant through a service.
- **One style, leaven halved.** The model says double the time; check it.
- **50 % whole grain against white, same schedule.** The code assumes bran speeds fermentation
  by about 30 % at full substitution. That number is the softest guess in the whole model.
