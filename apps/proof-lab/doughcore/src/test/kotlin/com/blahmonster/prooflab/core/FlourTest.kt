package com.blahmonster.prooflab.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlourBlendTest {
	private val blend = FlourBlend(
		listOf(
			FlourLibrary.at("bread", 80.0),
			FlourLibrary.at("whole-wheat", 20.0),
		),
	)

	@Test
	fun `protein is the weighted average of the blend`() {
		// 0.8 × 12.7 + 0.2 × 13.5
		assertEquals(12.86, blend.proteinPercent, 1e-9)
	}

	@Test
	fun `whole grain share counts only the whole grain flours`() {
		assertEquals(0.2, blend.wholeGrainFraction, 1e-9)
		assertEquals(0.0, FlourBlend(FlourLibrary.defaultBlend).wholeGrainFraction, 1e-9)
	}

	@Test
	fun `shares that do not add to a hundred are rescaled, not trusted`() {
		val sloppy = FlourBlend(
			listOf(FlourLibrary.at("bread", 40.0), FlourLibrary.at("semola", 10.0)),
		)
		assertEquals(100.0, sloppy.normalized.sumOf { it.percent }, 1e-9)
		assertEquals(80.0, sloppy.normalized.first().percent, 1e-9)
	}

	@Test
	fun `an empty or zeroed blend still produces something usable`() {
		assertEquals(1, FlourBlend(emptyList()).normalized.size)
		val zeroed = FlourBlend(
			listOf(FlourLibrary.at("bread", 0.0), FlourLibrary.at("rye", 0.0)),
		)
		assertEquals(100.0, zeroed.normalized.sumOf { it.percent }, 1e-9)
		assertEquals(50.0, zeroed.normalized.first().percent, 1e-9)
	}

	@Test
	fun `whole grain raises the absorption guide`() {
		val white = FlourBlend(listOf(FlourLibrary.at("bread", 100.0)))
		assertTrue(blend.absorptionGuidePercent > white.absorptionGuidePercent)
	}

	@Test
	fun `the summary reads like a baker would say it`() {
		assertEquals("80 % Bread flour · 20 % Whole wheat", blend.summary)
	}

	@Test
	fun `flour library ids are unique and protein figures are plausible`() {
		val ids = FlourLibrary.all.map { it.id }
		assertEquals(ids.size, ids.toSet().size)
		assertTrue(FlourLibrary.all.all { it.proteinPercent in 8.0..16.0 })
	}
}

class BlendFormulaTest {
	private val formula = DoughFormula(
		ballCount = 10,
		ballWeightGrams = 300.0,
		lossPercent = 0.0,
		hydrationPercent = 70.0,
		flours = listOf(
			FlourLibrary.at("bread", 70.0),
			FlourLibrary.at("whole-wheat", 20.0),
			FlourLibrary.at("rye", 10.0),
		),
	)

	private fun flourRows(rows: List<Ingredient>, prefix: String = "flour") =
		rows.filter { it.id == prefix || it.id.startsWith("$prefix-") }

	@Test
	fun `each flour gets its own row and they add up to the total flour`() {
		val result = formula.result()
		val rows = flourRows(result.overall)
		assertEquals(3, rows.size)
		assertEquals(result.totalFlourGrams, rows.sumOf { it.grams }, 1e-9)
	}

	@Test
	fun `a single flour stays a single row`() {
		val rows = flourRows(DoughFormula().result().overall)
		assertEquals(1, rows.size)
		assertEquals("flour", rows.single().id)
	}

	@Test
	fun `rows carry the blend's share as their baker's percentage`() {
		val rows = flourRows(formula.result().overall).associateBy { it.id }
		assertEquals(70.0, rows.getValue("flour-bread").bakersPercent, 1e-9)
		assertEquals(20.0, rows.getValue("flour-whole-wheat").bakersPercent, 1e-9)
		assertEquals(10.0, rows.getValue("flour-rye").bakersPercent, 1e-9)
	}

	@Test
	fun `a blend still adds up to exactly the dough you asked for`() {
		val result = formula.result()
		assertEquals(3000.0, result.totalDoughGrams, 1e-9)
		assertEquals(result.totalDoughGrams, result.overall.sumOf { it.grams }, 1e-6)
		assertEquals(result.totalDoughGrams, result.finalMix.sumOf { it.grams }, 1e-6)
	}

	@Test
	fun `a preferment takes the same blend, scaled down`() {
		val withPoolish = formula.copy(
			prefermentKind = PrefermentKind.POOLISH,
			prefermentedFlourPercent = 30.0,
			prefermentHydrationPercent = 100.0,
		)
		val result = withPoolish.result()
		val buildFlour = flourRows(result.prefermentBuild, "pfFlour")
		assertEquals(3, buildFlour.size)
		assertEquals(result.prefermentFlourGrams, buildFlour.sumOf { it.grams }, 1e-9)
		// Same proportions as the dough as a whole.
		assertEquals(0.7, buildFlour.first().grams / result.prefermentFlourGrams, 1e-9)
	}

	@Test
	fun `whole grain doughs are dosed with less leaven`() {
		val white = Leavening.instantYeastPercent(equivalentHours = 8.0)
		val wholemeal = Leavening.instantYeastPercent(equivalentHours = 8.0, wholeGrainFraction = 1.0)
		assertTrue(wholemeal < white)
		assertEquals(white * (1 - Leavening.WHOLE_GRAIN_SPEEDUP), wholemeal, 1e-12)

		val levainWhite = Leavening.levainPercent(equivalentHours = 8.0)
		val levainWhole = Leavening.levainPercent(equivalentHours = 8.0, wholeGrainFraction = 0.5)
		assertTrue(levainWhole < levainWhite)
	}

	@Test
	fun `every shipped style has a blend that adds to a hundred`() {
		for (style in StyleLibrary.all) {
			val total = style.formula.flours.sumOf { it.percent }
			assertEquals(100.0, total, 1e-9, "${style.id} blend adds to $total")
			assertTrue(style.formula.flours.isNotEmpty(), "${style.id} has no flour")
		}
	}

	@Test
	fun `the sourdough shelf covers more than one pizza style`() {
		val sourdoughPizza = StyleLibrary.sourdough.filter { it.family == PlanFamily.PIZZA }
		assertTrue(sourdoughPizza.size >= 4, "only found ${sourdoughPizza.map { it.id }}")
		// And they don't all run the same schedule.
		assertTrue(sourdoughPizza.map { it.planId }.toSet().size >= 3)
	}

	@Test
	fun `styles with multiple flours actually exist`() {
		val blended = StyleLibrary.all.filter { it.formula.flours.size > 1 }
		assertTrue(blended.size >= 5, "only found ${blended.map { it.id }}")
	}
}
