package com.blahmonster.prooflab.core

import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Golden values shared by both platforms.
 *
 * This module is the reference implementation — it's the one that gets compiled and tested on
 * every push. Running `gradle writeFixtures` regenerates `fixtures/conformance.json`, and the
 * Swift test suite asserts against the same file, so iOS and Android can't drift apart
 * without a test going red.
 */
@Serializable
data class ConformanceFixtures(
	val version: Int = 1,
	val rateMultipliers: List<RatePoint>,
	val equivalentHours: List<EquivalentPoint>,
	val instantYeast: List<YeastPoint>,
	val levain: List<LevainPoint>,
	val plans: List<PlanPoint>,
	val formulas: List<FormulaPoint>,
	val waterTemperatures: List<WaterPoint>,
	val production: List<ProductionPoint>,
	val blends: List<BlendPoint>,
	val sensing: List<SensingCase>,
)

@Serializable
data class SensingPoint(
	val offsetMillis: Long,
	val doughC: Double? = null,
	val heightMm: Double? = null,
)

/**
 * Inputs and outputs together, so the Swift side rebuilds the same series rather than keeping
 * its own copy of it — a duplicated fixture input is a drift waiting to happen.
 */
@Serializable
data class SensingCase(
	val id: String,
	val points: List<SensingPoint>,
	val measuredEquivalentHours: Double,
	val effectiveConstantTemperatureC: Double?,
	val longestGapMinutes: Double,
	val elapsedHours: Double,
	val expansionPercent: Double? = null,
	val riseRatePercentPerHour: Double? = null,
)

@Serializable
data class RatePoint(val temperatureC: Double, val value: Double)

@Serializable
data class EquivalentPoint(val hours: Double, val temperatureC: Double, val value: Double)

@Serializable
data class YeastPoint(
	val equivalentHours: Double,
	val saltPercent: Double,
	val sugarPercent: Double,
	val prefermentedFlourFraction: Double,
	val wholeGrainFraction: Double,
	val value: Double,
)

@Serializable
data class LevainPoint(
	val equivalentHours: Double,
	val saltPercent: Double,
	val wholeGrainFraction: Double,
	val value: Double,
)

@Serializable
data class BlendPoint(
	val styleId: String,
	val proteinPercent: Double,
	val wholeGrainFraction: Double,
	val absorptionGuidePercent: Double,
	val summary: String,
)

@Serializable
data class PlanPoint(
	val id: String,
	val fermentationLoadHours: Double,
	val totalHours: Double,
	val hoursToReady: Double,
	val stageCount: Int,
)

@Serializable
data class IngredientPoint(val id: String, val grams: Double, val bakersPercent: Double)

@Serializable
data class FormulaPoint(
	val styleId: String,
	val totalDoughGrams: Double,
	val totalFlourGrams: Double,
	val totalWaterGrams: Double,
	val prefermentFlourGrams: Double,
	val prefermentTotalGrams: Double,
	val prefermentBuild: List<IngredientPoint>,
	val finalMix: List<IngredientPoint>,
)

@Serializable
data class WaterPoint(
	val desiredDoughTempC: Double,
	val flourTempC: Double,
	val roomTempC: Double,
	val prefermentTempC: Double?,
	val mixer: String,
	val totalWaterGrams: Double,
	val tapWaterTempC: Double,
	val waterTemperatureC: Double,
	val iceGrams: Double,
	val waterGrams: Double,
	val hasWarning: Boolean,
)

@Serializable
data class ProductionPoint(
	val styleId: String,
	val mixerCapacityKg: Double,
	val mixCount: Int,
	val ballsPerMix: List<Int>,
	val doughPerMixGrams: List<Double>,
)

object Conformance {
	private val temperatures = listOf(-2.0, 0.0, 2.0, 4.0, 6.0, 10.0, 15.0, 18.0, 20.0, 22.0, 24.0, 26.0, 30.0, 35.0)

	// equivalent hours, salt %, sugar %, prefermented flour fraction, whole grain fraction
	private val yeastCases = listOf(
		listOf(4.0, 2.0, 0.0, 0.0, 0.0),
		listOf(7.53, 2.0, 0.0, 0.0, 0.0),
		listOf(11.3, 2.0, 0.0, 0.0, 0.0),
		listOf(15.0, 2.8, 0.0, 0.0, 0.0),
		listOf(6.0, 2.0, 10.0, 0.0, 0.0),
		listOf(9.0, 2.5, 0.0, 0.3, 0.0),
		listOf(8.0, 2.0, 0.0, 0.0, 0.5),
		listOf(8.0, 2.3, 0.0, 0.1, 1.0),
		listOf(0.0, 2.0, 0.0, 0.0, 0.0),
		listOf(400.0, 2.0, 0.0, 0.0, 0.0),
	)

	private val waterCases = listOf(
		WaterCase(24.0, 21.0, 22.0, null, MixerKind.SPIRAL, 12_000.0, 18.0),
		WaterCase(23.0, 24.0, 26.0, 20.0, MixerKind.PLANETARY, 8_000.0, 24.0),
		WaterCase(25.0, 18.0, 18.0, null, MixerKind.HAND, 1_000.0, 14.0),
		WaterCase(26.0, 30.0, 32.0, 28.0, MixerKind.SPIRAL, 20_000.0, 27.0),
	)

	private data class WaterCase(
		val ddt: Double,
		val flour: Double,
		val room: Double,
		val preferment: Double?,
		val mixer: MixerKind,
		val water: Double,
		val tap: Double,
	)

	private fun steadyPoints(tempC: Double, hours: Double, everyMinutes: Long): List<SensingPoint> {
		val step = everyMinutes * 60_000
		val count = ((hours * HOUR_MILLIS) / step).toInt()
		return (0..count).map { SensingPoint(it * step, doughC = tempC) }
	}

	private fun sensingCase(id: String, points: List<SensingPoint>): SensingCase {
		val series = SensorSeries(
			points.map {
				SensorReading(
					epochMillis = it.offsetMillis,
					doughTempC = it.doughC,
					doughHeightMm = it.heightMm,
				)
			},
		)
		return SensingCase(
			id = id,
			points = points,
			measuredEquivalentHours = series.measuredEquivalentHours(),
			effectiveConstantTemperatureC = series.effectiveConstantTemperatureC(),
			longestGapMinutes = series.longestTemperatureGapMinutes(),
			elapsedHours = series.elapsedHours,
			expansionPercent = series.expansionPercent(),
			riseRatePercentPerHour = series.riseRatePercentPerHour(),
		)
	}

	private fun sensingCases(): List<SensingCase> = listOf(
		sensingCase("steady-24c-5h", steadyPoints(24.0, 5.0, 5)),
		sensingCase("warm-walk-in-24h", steadyPoints(5.8, 24.0, 15)),
		sensingCase("cold-4c-48h", steadyPoints(4.0, 48.0, 30)),
		sensingCase(
			"swing-30-then-10",
			listOf(
				SensingPoint(0, doughC = 30.0),
				SensingPoint(HOUR_MILLIS, doughC = 30.0),
				SensingPoint(HOUR_MILLIS + 1000, doughC = 10.0),
				SensingPoint(2 * HOUR_MILLIS, doughC = 10.0),
			),
		),
		sensingCase(
			"logging-gap",
			listOf(
				SensingPoint(0, doughC = 24.0),
				SensingPoint(90 * 60_000, doughC = 24.0),
			),
		),
		sensingCase(
			"rising-20-percent-per-hour",
			(0..48).map { index ->
				val offset = index * 5L * 60_000
				val hours = offset.toDouble() / HOUR_MILLIS
				SensingPoint(offset, doughC = 24.0, heightMm = 100.0 * (1 + 0.2 * hours))
			},
		),
	)

	fun build(): ConformanceFixtures = ConformanceFixtures(
		rateMultipliers = temperatures.map { RatePoint(it, Fermentation.rateMultiplier(it)) },
		equivalentHours = listOf(
			EquivalentPoint(48.0, 4.0, Fermentation.equivalentHours(48.0, 4.0)),
			EquivalentPoint(24.0, 4.0, Fermentation.equivalentHours(24.0, 4.0)),
			EquivalentPoint(3.0, 24.0, Fermentation.equivalentHours(3.0, 24.0)),
			EquivalentPoint(14.0, 20.0, Fermentation.equivalentHours(14.0, 20.0)),
			EquivalentPoint(6.0, 20.0, Fermentation.equivalentHours(6.0, 20.0)),
			EquivalentPoint(4.5, 25.0, Fermentation.equivalentHours(4.5, 25.0)),
		),
		instantYeast = yeastCases.map {
			YeastPoint(
				equivalentHours = it[0],
				saltPercent = it[1],
				sugarPercent = it[2],
				prefermentedFlourFraction = it[3],
				wholeGrainFraction = it[4],
				value = Leavening.instantYeastPercent(it[0], it[1], it[2], it[3], it[4]),
			)
		},
		levain = listOf(
			Triple(4.5, 2.0, 0.0),
			Triple(12.0, 2.0, 0.0),
			Triple(20.0, 2.5, 0.0),
			Triple(9.0, 2.0, 0.35),
			Triple(0.0, 2.0, 0.0),
			Triple(500.0, 2.0, 0.0),
		).map {
			LevainPoint(
				it.first,
				it.second,
				it.third,
				Leavening.levainPercent(it.first, it.second, it.third),
			)
		},
		plans = PlanLibrary.all.map {
			PlanPoint(it.id, it.fermentationLoadHours, it.totalHours, it.hoursToReady, it.stages.size)
		},
		formulas = StyleLibrary.all.map { style ->
			val result = style.formula.result()
			FormulaPoint(
				styleId = style.id,
				totalDoughGrams = result.totalDoughGrams,
				totalFlourGrams = result.totalFlourGrams,
				totalWaterGrams = result.totalWaterGrams,
				prefermentFlourGrams = result.prefermentFlourGrams,
				prefermentTotalGrams = result.prefermentTotalGrams,
				prefermentBuild = result.prefermentBuild.map {
					IngredientPoint(it.id, it.grams, it.bakersPercent)
				},
				finalMix = result.finalMix.map { IngredientPoint(it.id, it.grams, it.bakersPercent) },
			)
		},
		waterTemperatures = waterCases.map { case ->
			val solved = DoughTemperature.solve(
				desiredDoughTempC = case.ddt,
				flourTempC = case.flour,
				roomTempC = case.room,
				prefermentTempC = case.preferment,
				mixer = case.mixer,
				totalWaterGrams = case.water,
				tapWaterTempC = case.tap,
			)
			WaterPoint(
				desiredDoughTempC = case.ddt,
				flourTempC = case.flour,
				roomTempC = case.room,
				prefermentTempC = case.preferment,
				mixer = case.mixer.name,
				totalWaterGrams = case.water,
				tapWaterTempC = case.tap,
				waterTemperatureC = solved.waterTemperatureC,
				iceGrams = solved.iceGrams,
				waterGrams = solved.waterGrams,
				hasWarning = solved.warning != null,
			)
		},
		production = listOf("new-york" to 20.0, "neapolitan" to 8.0, "country-loaf" to 30.0, "detroit" to 2.0)
			.mapNotNull { (styleId, capacity) ->
				val style = StyleLibrary.style(styleId) ?: return@mapNotNull null
				val plan = style.formula.result().productionPlan(capacity)
				ProductionPoint(
					styleId = styleId,
					mixerCapacityKg = capacity,
					mixCount = plan.mixCount,
					ballsPerMix = plan.mixes.map { it.ballCount },
					doughPerMixGrams = plan.mixes.map { it.doughGrams },
				)
			},
		blends = StyleLibrary.all.map { style ->
			val blend = style.formula.blend
			BlendPoint(
				styleId = style.id,
				proteinPercent = blend.proteinPercent,
				wholeGrainFraction = blend.wholeGrainFraction,
				absorptionGuidePercent = blend.absorptionGuidePercent,
				summary = blend.summary,
			)
		},
		sensing = sensingCases(),
	)
}

private val json = Json { prettyPrint = true; encodeDefaults = true }

fun main(args: Array<String>) {
	val path = args.firstOrNull() ?: "../fixtures/conformance.json"
	val file = File(path)
	file.parentFile?.mkdirs()
	file.writeText(json.encodeToString(Conformance.build()) + "\n")
	println("Wrote ${file.absolutePath}")
}
