package com.blahmonster.prooflab.core

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlinx.serialization.Serializable

enum class PrefermentKind(val displayName: String, val defaultHydrationPercent: Double) {
	NONE("None (straight dough)", 0.0),
	POOLISH("Poolish", 100.0),
	BIGA("Biga", 50.0),
	LEVAIN("Levain", 100.0),
	OLD_DOUGH("Old dough (pâte fermentée)", 62.0),
	;

	/** Levain is fed from a starter; the others are seeded with commercial yeast. */
	val usesStarterSeed: Boolean get() = this == LEVAIN
}

/**
 * A dough formula in baker's percentages. Percentages are of **total flour**, which includes
 * any flour locked up in a preferment.
 */
@Serializable
data class DoughFormula(
	val name: String = "Untitled dough",
	val ballCount: Int = 20,
	val ballWeightGrams: Double = 280.0,
	/** Scrap / trim allowance so the last ball isn't short. */
	val lossPercent: Double = 2.0,
	val hydrationPercent: Double = 62.0,
	val saltPercent: Double = 2.5,
	val oilPercent: Double = 0.0,
	val sugarPercent: Double = 0.0,
	val maltPercent: Double = 0.0,
	val leaven: LeavenKind = LeavenKind.COMMERCIAL_YEAST,
	val yeastType: YeastType = YeastType.INSTANT_DRY,
	/** Always stored on an instant-dry basis; the scooped weight applies [YeastType.multiplier]. */
	val instantYeastPercent: Double = 0.2,
	val prefermentKind: PrefermentKind = PrefermentKind.NONE,
	/** Share of the *total flour* that gets prefermented. */
	val prefermentedFlourPercent: Double = 0.0,
	val prefermentHydrationPercent: Double = 100.0,
	/** Instant-dry yeast in the preferment, as a percent of the preferment's flour. */
	val prefermentYeastPercent: Double = 0.1,
	/** Mature starter carried into a levain build, as a percent of the levain's flour. */
	val starterSeedPercent: Double = 20.0,
	val flourNote: String = "",
) {
	val prefermentedFlourFraction: Double
		get() = if (prefermentKind == PrefermentKind.NONE) 0.0
		else min(max(prefermentedFlourPercent / 100.0, 0.0), 1.0)

	/** The percentage you actually weigh out, once the yeast form is accounted for. */
	val scoopedYeastPercent: Double
		get() = if (leaven == LeavenKind.SOURDOUGH) 0.0 else instantYeastPercent * yeastType.multiplier

	fun result(): FormulaResult {
		val safeBalls = max(0, ballCount)
		val targetDough = safeBalls * max(0.0, ballWeightGrams) * (1 + max(0.0, lossPercent) / 100.0)

		val yeastPercent = scoopedYeastPercent
		val prefFlourFraction = prefermentedFlourFraction
		val prefermentYeastOfTotalFlour =
			if (prefermentKind.usesStarterSeed || prefermentKind == PrefermentKind.NONE) 0.0
			else prefFlourFraction * prefermentYeastPercent * yeastType.multiplier

		val sumPercent = 100.0 +
			hydrationPercent +
			saltPercent +
			oilPercent +
			sugarPercent +
			maltPercent +
			yeastPercent +
			prefermentYeastOfTotalFlour

		val totalFlour = if (sumPercent > 0) targetDough / (sumPercent / 100.0) else 0.0
		fun weight(percent: Double) = totalFlour * percent / 100.0

		val totalWater = weight(hydrationPercent)
		val salt = weight(saltPercent)
		val oil = weight(oilPercent)
		val sugar = weight(sugarPercent)
		val malt = weight(maltPercent)
		val yeast = weight(yeastPercent)

		val prefermentFlour = totalFlour * prefFlourFraction
		val prefermentWater = prefermentFlour * prefermentHydrationPercent / 100.0
		val prefermentYeast =
			if (prefermentKind.usesStarterSeed) 0.0
			else prefermentFlour * prefermentYeastPercent * yeastType.multiplier / 100.0
		val prefermentTotal = prefermentFlour + prefermentWater + prefermentYeast

		val overall = buildList {
			add(Ingredient("flour", "Flour (total)", totalFlour, 100.0))
			add(Ingredient("water", "Water (total)", totalWater, hydrationPercent))
			add(Ingredient("salt", "Salt", salt, saltPercent))
			if (leaven == LeavenKind.COMMERCIAL_YEAST && yeast > 0) {
				add(
					Ingredient(
						"yeast",
						"Yeast — ${yeastType.shortName}",
						yeast + prefermentYeast,
						yeastPercent + prefermentYeastOfTotalFlour,
					),
				)
			}
			if (oil > 0) add(Ingredient("oil", "Oil", oil, oilPercent))
			if (sugar > 0) add(Ingredient("sugar", "Sugar", sugar, sugarPercent))
			if (malt > 0) add(Ingredient("malt", "Diastatic malt", malt, maltPercent))
		}

		val build = if (prefermentKind != PrefermentKind.NONE && prefermentFlour > 0) {
			if (prefermentKind.usesStarterSeed) {
				val seedFlour = prefermentFlour * max(0.0, starterSeedPercent) / 100.0
				val seed = seedFlour * (1 + prefermentHydrationPercent / 100.0)
				val feedFlour = prefermentFlour - seedFlour
				val feedWater = feedFlour * prefermentHydrationPercent / 100.0
				listOf(
					Ingredient("seed", "Ripe starter", seed, 0.0),
					Ingredient("pfFlour", "Flour", feedFlour, 0.0),
					Ingredient("pfWater", "Water", feedWater, 0.0),
				)
			} else {
				listOf(
					Ingredient("pfFlour", "Flour", prefermentFlour, 0.0),
					Ingredient("pfWater", "Water", prefermentWater, 0.0),
					Ingredient("pfYeast", "Yeast — ${yeastType.shortName}", prefermentYeast, 0.0),
				)
			}
		} else {
			emptyList()
		}

		val final = buildList {
			add(
				Ingredient(
					"flour",
					"Flour",
					totalFlour - prefermentFlour,
					100.0 - prefermentedFlourPercent,
				),
			)
			add(
				Ingredient(
					"water",
					"Water",
					totalWater - prefermentWater,
					hydrationPercent - (prefermentWater / max(totalFlour, 1.0)) * 100.0,
				),
			)
			if (prefermentTotal > 0) {
				add(
					Ingredient(
						"preferment",
						"${prefermentKind.displayName} (ripe)",
						prefermentTotal,
						prefermentTotal / max(totalFlour, 1.0) * 100.0,
					),
				)
			}
			add(Ingredient("salt", "Salt", salt, saltPercent))
			if (leaven == LeavenKind.COMMERCIAL_YEAST && yeast > 0) {
				add(Ingredient("yeast", "Yeast — ${yeastType.shortName}", yeast, yeastPercent))
			}
			if (oil > 0) add(Ingredient("oil", "Oil", oil, oilPercent))
			if (sugar > 0) add(Ingredient("sugar", "Sugar", sugar, sugarPercent))
			if (malt > 0) add(Ingredient("malt", "Diastatic malt", malt, maltPercent))
		}

		return FormulaResult(
			totalDoughGrams = targetDough,
			totalFlourGrams = totalFlour,
			totalWaterGrams = totalWater,
			ballCount = safeBalls,
			ballWeightGrams = ballWeightGrams,
			trueHydrationPercent = hydrationPercent,
			overall = overall,
			prefermentBuild = build,
			finalMix = final,
			prefermentFlourGrams = prefermentFlour,
			prefermentTotalGrams = prefermentTotal,
		)
	}
}

data class Ingredient(
	val id: String,
	val name: String,
	val grams: Double,
	/** Percent of total flour. Zero for rows where a percentage is meaningless. */
	val bakersPercent: Double,
)

data class FormulaResult(
	val totalDoughGrams: Double,
	val totalFlourGrams: Double,
	val totalWaterGrams: Double,
	val ballCount: Int,
	val ballWeightGrams: Double,
	/** Water only, ignoring oil and sugar — what actually governs handling. */
	val trueHydrationPercent: Double,
	/** The whole formula, preferment folded in. */
	val overall: List<Ingredient>,
	/** What to build ahead of time. Empty for a straight dough. */
	val prefermentBuild: List<Ingredient>,
	/** What goes in the mixer on the day. */
	val finalMix: List<Ingredient>,
	val prefermentFlourGrams: Double,
	val prefermentTotalGrams: Double,
) {
	/**
	 * Splits a production run across mixer loads. Dough is divided evenly; leftover balls are
	 * spread one per mix so no load is short by more than a single ball.
	 */
	fun productionPlan(mixerCapacityKg: Double): ProductionPlan {
		val capacity = max(0.1, mixerCapacityKg) * 1000.0
		val mixCount = max(1, ceil(totalDoughGrams / capacity).toInt())
		val baseBalls = ballCount / mixCount
		val remainder = ballCount % mixCount

		val mixes = (0 until mixCount).map { index ->
			val balls = baseBalls + if (index < remainder) 1 else 0
			val share =
				if (ballCount > 0) balls.toDouble() / ballCount.toDouble() else 1.0 / mixCount.toDouble()
			ProductionPlan.Mix(
				id = index + 1,
				ballCount = balls,
				doughGrams = totalDoughGrams * share,
				ingredients = finalMix.map { it.copy(grams = it.grams * share) },
			)
		}

		return ProductionPlan(mixerCapacityKg, mixes, totalDoughGrams)
	}
}

data class ProductionPlan(
	val mixerCapacityKg: Double,
	val mixes: List<Mix>,
	val totalDoughGrams: Double,
) {
	data class Mix(
		val id: Int,
		val ballCount: Int,
		val doughGrams: Double,
		val ingredients: List<Ingredient>,
	)

	val mixCount: Int get() = mixes.size
}
