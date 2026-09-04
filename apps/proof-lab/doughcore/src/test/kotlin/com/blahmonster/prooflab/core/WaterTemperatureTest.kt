package com.blahmonster.prooflab.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WaterTemperatureTest {
	@Test
	fun `three factor method matches the worked example`() {
		// DDT 24, flour 21, room 22, spiral friction 4 → 24*3 − 47 = 25
		val water = DoughTemperature.waterTemperature(
			desiredDoughTempC = 24.0,
			flourTempC = 21.0,
			roomTempC = 22.0,
			prefermentTempC = null,
			frictionC = MixerKind.SPIRAL.frictionC,
		)
		assertEquals(25.0, water, 1e-9)
	}

	@Test
	fun `a preferment switches it to the four factor method`() {
		val three = DoughTemperature.waterTemperature(24.0, 21.0, 22.0, null, 4.0)
		val four = DoughTemperature.waterTemperature(24.0, 21.0, 22.0, 20.0, 4.0)
		// The extra factor adds one DDT and subtracts the preferment's own temperature.
		assertEquals(three + 24.0 - 20.0, four, 1e-9)
	}

	@Test
	fun `a hot kitchen pushes the water colder`() {
		val cool = DoughTemperature.waterTemperature(24.0, 20.0, 20.0, null, 4.0)
		val hot = DoughTemperature.waterTemperature(24.0, 30.0, 32.0, null, 4.0)
		assertTrue(hot < cool)
	}

	@Test
	fun `ice split satisfies the heat balance`() {
		val total = 10_000.0
		val tap = 20.0
		val target = 4.0
		val (ice, water) = DoughTemperature.iceSplit(total, tap, target)

		assertEquals(total, ice + water, 1e-9)
		// (W − I)(tap − target) should equal I(80 + target).
		val absorbed = ice * (80 + target)
		val released = water * (tap - target)
		assertTrue(abs(absorbed - released) < 1e-6, "heat balance off: $absorbed vs $released")
	}

	@Test
	fun `no ice is needed when the tap is already cold enough`() {
		val (ice, water) = DoughTemperature.iceSplit(5_000.0, 8.0, 12.0)
		assertEquals(0.0, ice, 1e-12)
		assertEquals(5_000.0, water, 1e-12)
	}

	@Test
	fun `an impossible water temperature is called out rather than faked`() {
		val result = DoughTemperature.solve(
			desiredDoughTempC = 22.0,
			flourTempC = 32.0,
			roomTempC = 34.0,
			prefermentTempC = null,
			mixer = MixerKind.PLANETARY,
			totalWaterGrams = 5_000.0,
			tapWaterTempC = 24.0,
		)
		assertTrue(result.waterTemperatureC < 0)
		assertNotNull(result.warning)
		// It still hands back a usable all-ice-you-can split rather than nonsense.
		assertEquals(5_000.0, result.iceGrams + result.waterGrams, 1e-9)
		assertTrue(result.iceGrams > 0)
	}

	@Test
	fun `a normal bakery setup needs no warning`() {
		val result = DoughTemperature.solve(
			desiredDoughTempC = 24.0,
			flourTempC = 21.0,
			roomTempC = 22.0,
			prefermentTempC = null,
			mixer = MixerKind.SPIRAL,
			totalWaterGrams = 12_000.0,
			tapWaterTempC = 18.0,
		)
		assertNull(result.warning)
		assertEquals(0.0, result.iceGrams, 1e-12) // target 25 °C is above the tap
	}

	@Test
	fun `mixer friction is ordered the way the machines actually behave`() {
		assertTrue(MixerKind.HAND.frictionC < MixerKind.SPIRAL.frictionC)
		assertTrue(MixerKind.SPIRAL.frictionC < MixerKind.PLANETARY.frictionC)
	}
}
