package com.blahmonster.prooflab.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormulaTest {
	private fun grams(list: List<Ingredient>, id: String) = list.firstOrNull { it.id == id }?.grams ?: 0.0

	/** A blend spreads flour across several rows; sum whatever the prefix produced. */
	private fun flourGrams(list: List<Ingredient>, prefix: String = "flour") =
		list.filter { it.id == prefix || it.id.startsWith("$prefix-") }.sumOf { it.grams }

	@Test
	fun `ingredients add up to the dough you asked for`() {
		val formula = DoughFormula(
			ballCount = 20,
			ballWeightGrams = 280.0,
			lossPercent = 2.0,
			hydrationPercent = 65.0,
			saltPercent = 2.5,
			oilPercent = 2.0,
			sugarPercent = 1.0,
			instantYeastPercent = 0.2,
		)
		val result = formula.result()

		assertEquals(20 * 280 * 1.02, result.totalDoughGrams, 1e-9)
		val sum = result.overall.sumOf { it.grams }
		assertEquals(result.totalDoughGrams, sum, 1e-6)
	}

	@Test
	fun `the final mix also adds up, preferment folded in`() {
		val formula = StyleLibrary.style("contemporary-biga")!!.formula
		val result = formula.result()
		val sum = result.finalMix.sumOf { it.grams }
		assertEquals(result.totalDoughGrams, sum, 1e-6)
	}

	@Test
	fun `baker's percentages are relative to total flour`() {
		val formula = DoughFormula(hydrationPercent = 70.0, saltPercent = 2.0, instantYeastPercent = 0.3)
		val result = formula.result()
		assertEquals(result.totalFlourGrams * 0.70, result.totalWaterGrams, 1e-9)
		assertEquals(result.totalFlourGrams * 0.02, grams(result.overall, "salt"), 1e-9)
	}

	@Test
	fun `a preferment moves flour and water rather than adding any`() {
		val formula = DoughFormula(
			hydrationPercent = 70.0,
			prefermentKind = PrefermentKind.POOLISH,
			prefermentedFlourPercent = 30.0,
			prefermentHydrationPercent = 100.0,
			prefermentYeastPercent = 0.15,
		)
		val result = formula.result()

		assertEquals(result.totalFlourGrams * 0.30, result.prefermentFlourGrams, 1e-9)
		// Flour in the mixer plus flour in the poolish is all the flour there is.
		assertEquals(
			result.totalFlourGrams,
			flourGrams(result.finalMix) + result.prefermentFlourGrams,
			1e-9,
		)
		val prefermentWater = result.prefermentFlourGrams * 1.0
		assertEquals(
			result.totalWaterGrams,
			grams(result.finalMix, "water") + prefermentWater,
			1e-9,
		)
	}

	@Test
	fun `the poolish build is exactly the ripe poolish you later add`() {
		val result = StyleLibrary.style("poolish-pizza")!!.formula.result()
		assertEquals(result.prefermentTotalGrams, result.prefermentBuild.sumOf { it.grams }, 1e-9)
		assertEquals(result.prefermentTotalGrams, grams(result.finalMix, "preferment"), 1e-9)
	}

	@Test
	fun `a levain build is seed plus its feed, at the levain's hydration`() {
		val formula = DoughFormula(
			leaven = LeavenKind.SOURDOUGH,
			instantYeastPercent = 0.0,
			prefermentKind = PrefermentKind.LEVAIN,
			prefermentedFlourPercent = 10.0,
			prefermentHydrationPercent = 100.0,
			starterSeedPercent = 20.0,
		)
		val result = formula.result()

		assertEquals(result.prefermentTotalGrams, result.prefermentBuild.sumOf { it.grams }, 1e-9)
		val seedFlour = result.prefermentFlourGrams * 0.20
		assertEquals(seedFlour * 2, grams(result.prefermentBuild, "seed"), 1e-9)
		assertEquals(result.prefermentFlourGrams - seedFlour, flourGrams(result.prefermentBuild, "pfFlour"), 1e-9)
	}

	@Test
	fun `sourdough carries no commercial yeast anywhere`() {
		val result = StyleLibrary.style("country-loaf")!!.formula.result()
		assertNull(result.overall.firstOrNull { it.id == "yeast" })
		assertNull(result.finalMix.firstOrNull { it.id == "yeast" })
		assertNull(result.prefermentBuild.firstOrNull { it.id == "pfYeast" })
	}

	@Test
	fun `fresh yeast weighs three times what instant does`() {
		val instant = DoughFormula(yeastType = YeastType.INSTANT_DRY, instantYeastPercent = 0.2)
		val fresh = instant.copy(yeastType = YeastType.FRESH_CAKE)
		assertEquals(0.2, instant.scoopedYeastPercent, 1e-12)
		assertEquals(0.6, fresh.scoopedYeastPercent, 1e-12)
		// Both still make exactly the dough you asked for.
		assertEquals(fresh.result().totalDoughGrams, fresh.result().overall.sumOf { it.grams }, 1e-6)
	}

	@Test
	fun `a zero ball count degrades quietly instead of dividing by zero`() {
		val result = DoughFormula(ballCount = 0).result()
		assertEquals(0.0, result.totalDoughGrams, 1e-12)
		assertEquals(0.0, result.totalFlourGrams, 1e-12)
		assertTrue(result.overall.all { it.grams.isFinite() })
	}

	@Test
	fun `every shipped style produces a self consistent formula`() {
		for (style in StyleLibrary.all) {
			val result = style.formula.result()
			assertEquals(
				result.totalDoughGrams,
				result.overall.sumOf { it.grams },
				1e-6,
				"overall totals drifted for ${style.id}",
			)
			assertEquals(
				result.totalDoughGrams,
				result.finalMix.sumOf { it.grams },
				1e-6,
				"final mix drifted for ${style.id}",
			)
			assertTrue(result.totalFlourGrams > 0, "${style.id} has no flour")
		}
	}
}

class ProductionTest {
	@Test
	fun `a run that fits in one mixer stays a single mix`() {
		val result = DoughFormula(ballCount = 10, ballWeightGrams = 280.0).result()
		val plan = result.productionPlan(mixerCapacityKg = 20.0)
		assertEquals(1, plan.mixCount)
		assertEquals(10, plan.mixes.single().ballCount)
	}

	@Test
	fun `balls spread evenly and nothing goes missing`() {
		val result = DoughFormula(ballCount = 100, ballWeightGrams = 280.0, lossPercent = 0.0).result()
		val plan = result.productionPlan(mixerCapacityKg = 8.0)

		assertEquals(4, plan.mixCount) // 28 kg over 8 kg loads
		assertEquals(100, plan.mixes.sumOf { it.ballCount })
		val counts = plan.mixes.map { it.ballCount }
		assertTrue(counts.max() - counts.min() <= 1, "loads were lopsided: $counts")
		assertEquals(result.totalDoughGrams, plan.mixes.sumOf { it.doughGrams }, 1e-6)
	}

	@Test
	fun `an odd remainder is spread one ball per mix`() {
		val result = DoughFormula(ballCount = 7, ballWeightGrams = 1000.0, lossPercent = 0.0).result()
		val plan = result.productionPlan(mixerCapacityKg = 2.0)
		assertEquals(4, plan.mixCount)
		assertEquals(listOf(2, 2, 2, 1), plan.mixes.map { it.ballCount })
	}

	@Test
	fun `per mix ingredients scale with that mix's share`() {
		val result = DoughFormula(ballCount = 9, ballWeightGrams = 300.0).result()
		val plan = result.productionPlan(mixerCapacityKg = 1.0)
		for (mix in plan.mixes) {
			val sum = mix.ingredients.sumOf { it.grams }
			assertTrue(abs(sum - mix.doughGrams) < 1e-6, "mix ${mix.id} ingredients don't match its dough")
		}
		assertEquals(result.totalDoughGrams, plan.mixes.sumOf { it.doughGrams }, 1e-6)
	}

	@Test
	fun `a nonsense capacity does not hang or divide by zero`() {
		val result = DoughFormula(ballCount = 5, ballWeightGrams = 250.0).result()
		val plan = result.productionPlan(mixerCapacityKg = 0.0)
		assertTrue(plan.mixCount >= 1)
		assertEquals(5, plan.mixes.sumOf { it.ballCount })
	}
}
