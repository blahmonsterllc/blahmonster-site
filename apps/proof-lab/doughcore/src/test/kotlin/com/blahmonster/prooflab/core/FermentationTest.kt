package com.blahmonster.prooflab.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FermentationTest {
	@Test
	fun `reference temperature has unit rate`() {
		assertEquals(1.0, Fermentation.rateMultiplier(24.0), 1e-12)
	}

	@Test
	fun `a fridge is roughly six times slower than the bench`() {
		val rate = Fermentation.rateMultiplier(4.0)
		assertTrue(rate in 0.14..0.18, "rate(4) was $rate")
		val slowdown = 1 / rate
		assertTrue(slowdown in 5.5..7.5, "fridge slowdown was $slowdown")
	}

	@Test
	fun `rate rises monotonically with temperature`() {
		val temps = listOf(-2.0, 0.0, 4.0, 10.0, 15.0, 20.0, 24.0, 30.0, 35.0, 45.0)
		val rates = temps.map { Fermentation.rateMultiplier(it) }
		rates.zipWithNext { a, b -> assertTrue(b > a, "expected increasing rate, got $a then $b") }
	}

	@Test
	fun `temperature input is clamped at both ends`() {
		assertEquals(
			Fermentation.rateMultiplier(Fermentation.MIN_TEMPERATURE_C),
			Fermentation.rateMultiplier(-40.0),
			1e-12,
		)
		assertEquals(
			Fermentation.rateMultiplier(Fermentation.MAX_TEMPERATURE_C),
			Fermentation.rateMultiplier(90.0),
			1e-12,
		)
	}

	@Test
	fun `two days in the fridge is about seven and a half hours on the bench`() {
		val equivalent = Fermentation.equivalentHours(48.0, 4.0)
		assertTrue(equivalent in 6.8..8.6, "48h at 4C came out as $equivalent equivalent hours")
	}

	@Test
	fun `equivalent hours round trip through the inverse`() {
		for (temp in listOf(2.0, 4.0, 12.0, 20.0, 24.0, 30.0)) {
			for (hours in listOf(0.5, 3.0, 18.0, 72.0)) {
				val equivalent = Fermentation.equivalentHours(hours, temp)
				val back = Fermentation.hoursForEquivalent(equivalent, temp)
				assertTrue(abs(back - hours) < 1e-9, "round trip failed at ${temp}C / ${hours}h")
			}
		}
	}

	@Test
	fun `negative durations do not create fermentation`() {
		assertEquals(0.0, Fermentation.equivalentHours(-5.0, 24.0), 1e-12)
	}

	@Test
	fun `ten degrees colder roughly halves to thirds the rate`() {
		// Each segment's Q10 should show up as the ratio across a ten degree step.
		assertEquals(2.0, Fermentation.rateMultiplier(30.0) / Fermentation.rateMultiplier(20.0), 1e-9)
		assertEquals(2.5, Fermentation.rateMultiplier(20.0) / Fermentation.rateMultiplier(10.0), 1e-9)
		assertEquals(3.0, Fermentation.rateMultiplier(10.0) / Fermentation.rateMultiplier(0.0), 1e-9)
	}
}

class LeaveningTest {
	@Test
	fun `cold ferment doses match the published tables`() {
		// Cold time only, which is how those tables are quoted.
		val day = Fermentation.equivalentHours(24.0, 4.0)
		val twoDays = Fermentation.equivalentHours(48.0, 4.0)
		val threeDays = Fermentation.equivalentHours(72.0, 4.0)

		assertTrue(Leavening.instantYeastPercent(day) in 0.30..0.50)
		assertTrue(Leavening.instantYeastPercent(twoDays) in 0.17..0.27)
		assertTrue(Leavening.instantYeastPercent(threeDays) in 0.11..0.18)
	}

	@Test
	fun `dose falls as the schedule lengthens`() {
		val doses = listOf(2.0, 4.0, 8.0, 16.0, 32.0).map { Leavening.instantYeastPercent(it) }
		doses.zipWithNext { a, b -> assertTrue(b < a, "expected falling dose, got $a then $b") }
	}

	@Test
	fun `salt and sugar raise the dose`() {
		val base = Leavening.instantYeastPercent(8.0, saltPercent = 2.0)
		assertTrue(Leavening.instantYeastPercent(8.0, saltPercent = 3.0) > base)
		assertTrue(Leavening.instantYeastPercent(8.0, sugarPercent = 12.0) > base)
		// Below the thresholds nothing should change.
		assertEquals(base, Leavening.instantYeastPercent(8.0, saltPercent = 1.5, sugarPercent = 4.0), 1e-12)
	}

	@Test
	fun `a preferment reduces the yeast in the final mix`() {
		val straight = Leavening.instantYeastPercent(8.0)
		val withPoolish = Leavening.instantYeastPercent(8.0, prefermentedFlourFraction = 0.3)
		assertTrue(withPoolish < straight)
		assertEquals(straight * (1 - 0.8 * 0.3), withPoolish, 1e-12)
	}

	@Test
	fun `doses are clamped to something you can actually weigh`() {
		assertEquals(Leavening.MAX_INSTANT_YEAST_PERCENT, Leavening.instantYeastPercent(0.0), 1e-12)
		assertEquals(Leavening.MAX_INSTANT_YEAST_PERCENT, Leavening.instantYeastPercent(0.1), 1e-12)
		assertEquals(Leavening.MIN_INSTANT_YEAST_PERCENT, Leavening.instantYeastPercent(10_000.0), 1e-12)
	}

	@Test
	fun `levain lands near twenty percent for a normal warm bulk`() {
		val bulk = Fermentation.equivalentHours(4.5, 25.0)
		val percent = Leavening.levainPercent(bulk)
		assertTrue(percent in 14.0..24.0, "levain came out at $percent %")
	}

	@Test
	fun `levain is clamped at both ends`() {
		assertEquals(Leavening.MAX_LEVAIN_PERCENT, Leavening.levainPercent(0.0), 1e-12)
		assertEquals(Leavening.MIN_LEVAIN_PERCENT, Leavening.levainPercent(5_000.0), 1e-12)
	}

	@Test
	fun `yeast types are consistent multiples of instant`() {
		assertEquals(1.0, YeastType.INSTANT_DRY.multiplier, 1e-12)
		assertTrue(YeastType.FRESH_CAKE.multiplier > YeastType.ACTIVE_DRY.multiplier)
		assertEquals(3.0, YeastType.FRESH_CAKE.multiplier, 1e-12)
	}
}
