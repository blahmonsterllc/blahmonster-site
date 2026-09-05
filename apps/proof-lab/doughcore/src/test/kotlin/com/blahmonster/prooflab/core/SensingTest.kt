package com.blahmonster.prooflab.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val START = 1_757_000_000_000L

/** A run held at one temperature, sampled every [everyMinutes]. */
private fun steady(
	tempC: Double,
	hours: Double,
	everyMinutes: Long = 1,
	heightMm: ((Double) -> Double)? = null,
): SensorSeries {
	val step = everyMinutes * 60_000
	val count = ((hours * HOUR_MILLIS) / step).toInt()
	return SensorSeries(
		(0..count).map { index ->
			val elapsedHours = (index * step).toDouble() / HOUR_MILLIS
			SensorReading(
				epochMillis = START + index * step,
				doughTempC = tempC,
				doughHeightMm = heightMm?.invoke(elapsedHours),
			)
		},
	)
}

class MeasuredFermentationTest {
	@Test
	fun `a steady run at the reference temperature banks one hour per hour`() {
		val series = steady(24.0, hours = 5.0)
		assertEquals(5.0, series.measuredEquivalentHours(), 1e-9)
		assertEquals(5.0, series.elapsedHours, 1e-9)
	}

	@Test
	fun `a steady run matches the model's own figure for that temperature`() {
		for (temp in listOf(4.0, 12.0, 20.0, 28.0)) {
			val series = steady(temp, hours = 6.0)
			assertEquals(
				Fermentation.equivalentHours(6.0, temp),
				series.measuredEquivalentHours(),
				1e-9,
				"measured run at $temp °C drifted from the model",
			)
		}
	}

	@Test
	fun `a fridge that actually runs warm banks more than the plan assumed`() {
		val planned = Fermentation.equivalentHours(24.0, 4.0)
		val measured = steady(5.8, hours = 24.0).measuredEquivalentHours()
		assertTrue(measured > planned, "expected $measured > $planned")
		// Not a rounding error — nearly a fifth more fermentation than planned.
		assertTrue(measured / planned > 1.15)
	}

	@Test
	fun `warming up mid-run is integrated, not averaged`() {
		// An hour at 30 °C then an hour at 10 °C, sampled sparsely at the two ends.
		val series = SensorSeries(
			listOf(
				SensorReading(START, doughTempC = 30.0),
				SensorReading(START + HOUR_MILLIS, doughTempC = 30.0),
				SensorReading(START + HOUR_MILLIS + 1000, doughTempC = 10.0),
				SensorReading(START + 2 * HOUR_MILLIS, doughTempC = 10.0),
			),
		)
		val measured = series.measuredEquivalentHours()
		val flatTwenty = Fermentation.equivalentHours(2.0, 20.0)
		assertTrue(
			measured > flatTwenty,
			"swinging around 20 °C should out-ferment holding at it: $measured vs $flatTwenty",
		)
	}

	@Test
	fun `ambient temperature stands in when there is no dough probe`() {
		val series = SensorSeries(
			(0..60).map {
				SensorReading(START + it * 60_000, ambientTempC = 24.0)
			},
		)
		assertEquals(1.0, series.measuredEquivalentHours(), 1e-9)
	}

	@Test
	fun `a dough probe wins over ambient when both are present`() {
		val series = SensorSeries(
			(0..60).map {
				SensorReading(START + it * 60_000, doughTempC = 24.0, ambientTempC = 4.0)
			},
		)
		assertEquals(1.0, series.measuredEquivalentHours(), 1e-9)
	}

	@Test
	fun `too few samples produce zero rather than a guess`() {
		assertEquals(0.0, SensorSeries(emptyList()).measuredEquivalentHours(), 1e-12)
		assertEquals(
			0.0,
			SensorSeries(listOf(SensorReading(START, doughTempC = 24.0))).measuredEquivalentHours(),
			1e-12,
		)
	}

	@Test
	fun `samples arriving out of order are sorted before integrating`() {
		val forward = steady(24.0, hours = 2.0, everyMinutes = 30)
		val shuffled = SensorSeries(forward.readings.reversed())
		assertEquals(
			forward.measuredEquivalentHours(),
			shuffled.measuredEquivalentHours(),
			1e-9,
		)
	}

	@Test
	fun `logging gaps are reported so the number can be labelled an estimate`() {
		val series = SensorSeries(
			listOf(
				SensorReading(START, doughTempC = 24.0),
				SensorReading(START + 90 * 60_000, doughTempC = 24.0),
			),
		)
		assertEquals(90.0, series.longestTemperatureGapMinutes(), 1e-9)
		// The dough still fermented across the gap; we just interpolated it.
		assertEquals(1.5, series.measuredEquivalentHours(), 1e-9)
	}

	@Test
	fun `a tight run reports no meaningful gap`() {
		assertTrue(steady(24.0, hours = 3.0).longestTemperatureGapMinutes() <= 1.0)
	}

	@Test
	fun `the effective constant temperature reproduces a steady run`() {
		for (temp in listOf(4.0, 18.0, 24.0, 30.0)) {
			val effective = steady(temp, hours = 4.0).effectiveConstantTemperatureC()
			assertNotNull(effective)
			assertTrue(abs(effective - temp) < 0.01, "got $effective for a steady $temp °C run")
		}
	}

	@Test
	fun `the effective temperature of a swinging run beats its arithmetic mean`() {
		val series = SensorSeries(
			listOf(
				SensorReading(START, doughTempC = 30.0),
				SensorReading(START + HOUR_MILLIS, doughTempC = 30.0),
				SensorReading(START + HOUR_MILLIS + 1000, doughTempC = 10.0),
				SensorReading(START + 2 * HOUR_MILLIS, doughTempC = 10.0),
			),
		)
		val effective = series.effectiveConstantTemperatureC()
		val mean = series.averageTemperatureC()
		assertNotNull(effective)
		assertNotNull(mean)
		assertEquals(20.0, mean, 0.01)
		assertTrue(effective > mean, "effective $effective should exceed the mean $mean")
	}
}

class RiseTrackingTest {
	/** 100 mm of dough rising a steady 20 % of baseline per hour. */
	private val rising = steady(24.0, hours = 4.0, everyMinutes = 5) { hours -> 100.0 * (1 + 0.2 * hours) }

	@Test
	fun `expansion is measured against where it started`() {
		assertEquals(100.0, rising.firstHeightMm)
		assertEquals(180.0, rising.latestHeightMm!!, 1e-9)
		assertEquals(1.8, rising.expansionRatio()!!, 1e-9)
		assertEquals(80.0, rising.expansionPercent()!!, 1e-9)
	}

	@Test
	fun `an explicit baseline overrides the first sample`() {
		// Useful when the rig starts logging before the dough goes in the container.
		assertEquals(0.9, rising.expansionRatio(baselineMm = 200.0)!!, 1e-9)
	}

	@Test
	fun `rise rate recovers the slope it was given`() {
		assertEquals(20.0, rising.riseRatePercentPerHour()!!, 1e-6)
	}

	@Test
	fun `projection to a target is consistent with the rate`() {
		// At 180 % and climbing 20 points an hour, 200 % is an hour out.
		assertEquals(1.0, rising.projectedHoursTo(2.0)!!, 1e-6)
	}

	@Test
	fun `a target already passed projects to zero`() {
		assertEquals(0.0, rising.projectedHoursTo(1.5)!!, 1e-12)
	}

	@Test
	fun `a dough that has stopped rising gets no projection rather than a fantasy`() {
		val flat = steady(24.0, hours = 3.0, everyMinutes = 5) { 150.0 }
		assertEquals(0.0, flat.riseRatePercentPerHour()!!, 1e-9)
		assertNull(flat.projectedHoursTo(2.0))
	}

	@Test
	fun `a collapsing dough gets no projection either`() {
		val collapsing = steady(24.0, hours = 2.0, everyMinutes = 5) { hours -> 200.0 - 10 * hours }
		assertTrue(collapsing.riseRatePercentPerHour()!! < 0)
		assertNull(collapsing.projectedHoursTo(2.0))
	}

	@Test
	fun `height readings are optional everywhere`() {
		val noHeights = steady(24.0, hours = 2.0)
		assertNull(noHeights.expansionRatio())
		assertNull(noHeights.riseRatePercentPerHour())
		assertNull(noHeights.projectedHoursTo(1.8))
	}
}

class OtherChannelsTest {
	private val series = SensorSeries(
		(0..60).map { minute ->
			SensorReading(
				epochMillis = START + minute * 60_000,
				doughTempC = 24.0,
				relativeHumidity = 72.0,
				co2Ppm = 800.0 + 20.0 * minute,
				massGrams = 1000.0 - 0.05 * minute,
			)
		},
	)

	@Test
	fun `co2 slope is reported per hour`() {
		// 20 ppm a minute over the last 45 minutes.
		assertEquals(1200.0, series.co2SlopePpmPerHour()!!, 1e-6)
		assertEquals(2000.0, series.latestCo2Ppm()!!, 1e-9)
	}

	@Test
	fun `mass loss is positive when the dough is losing weight`() {
		assertEquals(3.0, series.massLossGramsPerHour()!!, 1e-6)
	}

	@Test
	fun `humidity is carried through even though it says nothing about progress`() {
		assertEquals(72.0, series.latestRelativeHumidity()!!, 1e-9)
	}

	@Test
	fun `slicing scores one stage out of a whole run`() {
		val stage = series.slice(START, START + 30 * 60_000)
		assertEquals(0.5, stage.elapsedHours, 1e-9)
		assertEquals(0.5, stage.measuredEquivalentHours(), 1e-9)
	}
}

class SensorCsvTest {
	private val series = SensorSeries(
		listOf(
			SensorReading(START, doughTempC = 23.5, ambientTempC = 21.0, relativeHumidity = 68.0),
			SensorReading(START + 60_000, doughTempC = 23.6, doughHeightMm = 101.2, co2Ppm = 950.0),
		),
	)

	@Test
	fun `a round trip preserves every channel`() {
		val decoded = SensorCsv.decode(SensorCsv.encode(series))
		assertEquals(series.ordered, decoded.ordered)
	}

	@Test
	fun `the header is written and not read back as data`() {
		val text = SensorCsv.encode(series)
		assertTrue(text.startsWith(SensorCsv.HEADER))
		assertEquals(2, SensorCsv.decode(text).readings.size)
	}

	@Test
	fun `blank cells mean no sensor, not zero`() {
		val decoded = SensorCsv.decode("${SensorCsv.HEADER}\n$START,,21.0,,,,")
		val reading = decoded.readings.single()
		assertNull(reading.doughTempC)
		assertEquals(21.0, reading.ambientTempC!!, 1e-9)
		assertNull(reading.co2Ppm)
	}

	@Test
	fun `a truncated last line from a power cut costs one sample, not the run`() {
		// Power went at the moment of writing: timestamp and dough temperature made it to
		// disk, the rest of the row didn't.
		val text = SensorCsv.encode(series) + "${START + 120_000},24.0"
		val decoded = SensorCsv.decode(text)
		assertEquals(3, decoded.readings.size)

		val partial = decoded.ordered.last()
		assertEquals(START + 120_000, partial.epochMillis)
		assertEquals(24.0, partial.doughTempC!!, 1e-9)
		assertNull(partial.co2Ppm)
		assertNull(partial.doughHeightMm)
	}

	@Test
	fun `junk lines and comments are skipped`() {
		val text = """
			# doughscience rig v0
			${SensorCsv.HEADER}
			$START,24.0,,,,,
			not a reading at all
			${START + 60_000},24.1,,,,,
		""".trimIndent()
		assertEquals(2, SensorCsv.decode(text).readings.size)
	}

	@Test
	fun `an empty log decodes to an empty series rather than throwing`() {
		assertTrue(SensorCsv.decode("").isEmpty)
		assertTrue(SensorCsv.decode(SensorCsv.HEADER).isEmpty)
	}
}

class RunComparisonTest {
	private val coldStage = PlanLibrary.coldBallRetard.stages.first { it.kind.isCold }

	@Test
	fun `a run that matched the plan reports no significant difference`() {
		val series = steady(coldStage.temperatureC, hours = coldStage.hours, everyMinutes = 15)
		val comparison = RunComparison.of(coldStage, series)
		assertTrue(abs(comparison.differenceHours) < 1e-6)
		assertTrue(!comparison.isSignificant)
		assertNull(comparison.caveat)
	}

	@Test
	fun `a warm walk-in shows up as extra fermentation`() {
		val series = steady(6.0, hours = coldStage.hours, everyMinutes = 15)
		val comparison = RunComparison.of(coldStage, series)
		assertTrue(comparison.differenceHours > 0)
		assertTrue(comparison.isSignificant, "1.5 °C over 48 hours should be flagged")
		assertTrue(comparison.ratio!! > 1.2)
		assertEquals(6.0, comparison.measuredTemperatureC!!, 0.01)
	}

	@Test
	fun `a gap is disclosed rather than buried`() {
		val series = SensorSeries(
			listOf(
				SensorReading(START, doughTempC = 4.0),
				SensorReading(START + 6 * HOUR_MILLIS, doughTempC = 4.0),
			),
		)
		val comparison = RunComparison.of(coldStage, series)
		assertNotNull(comparison.caveat)
		assertTrue(comparison.caveat!!.contains("gap"))
	}
}
