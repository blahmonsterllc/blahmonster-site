package com.blahmonster.prooflab.core

import kotlin.math.max
import kotlinx.serialization.Serializable

enum class StageKind(val displayName: String, val countsTowardFermentation: Boolean) {
	PREFERMENT("Preferment", false),
	AUTOLYSE("Autolyse", false),
	MIX("Mix", false),
	BULK("Bulk ferment", true),
	DIVIDE("Divide", false),
	BENCH("Bench rest", true),
	BALL("Ball up", true),
	SHAPE("Shape", false),
	COLD_RETARD("Cold ferment", true),
	TEMPER("Temper", true),
	FINAL_PROOF("Final proof", true),
	BAKE("Bake", false),
	;

	val isCold: Boolean get() = this == COLD_RETARD
}

@Serializable
data class PlanStage(
	val id: String,
	val kind: StageKind,
	val title: String,
	val detail: String = "",
	val hours: Double,
	val temperatureC: Double,
	/** Fire a notification when this stage ends. */
	val alerts: Boolean = true,
	/** Repeating reminder inside the stage — stretch-and-folds, coil folds, dimpling. */
	val foldIntervalMinutes: Int? = null,
	/**
	 * Cold stages: how long past "ready" the dough stays usable. This is the number a
	 * production kitchen actually schedules around.
	 */
	val usableWindowHours: Double? = null,
) {
	val equivalentHours: Double
		get() = if (!kind.countsTowardFermentation) 0.0
		else Fermentation.equivalentHours(hours, temperatureC)
}

enum class PlanFamily(val displayName: String) {
	PIZZA("Pizza"),
	BREAD("Bread"),
}

@Serializable
data class FermentationPlan(
	val id: String,
	val name: String,
	val family: PlanFamily,
	val leaven: LeavenKind,
	val summary: String,
	val prefermentKind: PrefermentKind = PrefermentKind.NONE,
	val prefermentedFlourPercent: Double = 0.0,
	val prefermentHydrationPercent: Double = 100.0,
	val stages: List<PlanStage>,
) {
	/** Total fermentation expressed in hours at 24 °C. */
	val fermentationLoadHours: Double get() = stages.sumOf { it.equivalentHours }

	/** Wall-clock hours from mix to bake, preferment build included. */
	val totalHours: Double get() = stages.sumOf { max(0.0, it.hours) }

	val prefermentStage: PlanStage? get() = stages.firstOrNull { it.kind == StageKind.PREFERMENT }

	/** The dough is "ready" when the last stage before the bake ends. */
	val hoursToReady: Double
		get() {
			val bakeIndex = stages.indexOfFirst { it.kind == StageKind.BAKE }
			if (bakeIndex < 0) return totalHours
			return stages.take(bakeIndex).sumOf { max(0.0, it.hours) }
		}

	val coldStage: PlanStage? get() = stages.firstOrNull { it.kind.isCold }
}
