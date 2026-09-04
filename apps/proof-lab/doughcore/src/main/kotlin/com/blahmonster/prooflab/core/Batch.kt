package com.blahmonster.prooflab.core

import kotlin.math.max
import kotlinx.serialization.Serializable

@Serializable
data class StageProgress(
	/** Hours added or removed at the bench — "it's not ready, give it another 30". */
	val adjustmentHours: Double = 0.0,
	val completedAt: Long? = null,
	/** Set when you've seen the alert, which is what clears the badge. */
	val acknowledgedAt: Long? = null,
)

/**
 * Tasting notes for a prototype run. The point of the log is being able to tell why batch 14
 * was better than batch 13.
 */
@Serializable
data class BatchReview(
	val handling: Int = 3,
	val extensibility: Int = 3,
	val ovenSpring: Int = 3,
	val crumb: Int = 3,
	val flavor: Int = 3,
	val crust: Int = 3,
	val overall: Int = 3,
	val wouldRepeat: Boolean = true,
	val notes: String = "",
) {
	data class Axis(val id: String, val name: String, val score: Int)

	val axes: List<Axis>
		get() = listOf(
			Axis("handling", "Handling", handling),
			Axis("extensibility", "Extensibility", extensibility),
			Axis("ovenSpring", "Oven spring", ovenSpring),
			Axis("crumb", "Crumb", crumb),
			Axis("flavor", "Flavour", flavor),
			Axis("crust", "Crust", crust),
			Axis("overall", "Overall", overall),
		)
}

enum class StageStatus { UPCOMING, ACTIVE, DUE, OVERDUE, DONE }

@Serializable
data class Batch(
	val id: String,
	val name: String,
	val createdAt: Long,
	/** When the mix actually started. Everything downstream is derived from this. */
	val startAt: Long,
	val formula: DoughFormula,
	val plan: FermentationPlan,
	val mixerCapacityKg: Double = 20.0,
	val progress: Map<String, StageProgress> = emptyMap(),
	val review: BatchReview? = null,
	val notes: String = "",
	val tags: List<String> = emptyList(),
	val isArchived: Boolean = false,
) {
	/**
	 * Absolute stage times, honouring adjustments and manual completions. Completing a stage
	 * early or late drags everything after it along.
	 */
	val timeline: List<ScheduledStage>
		get() {
			val stages = mutableListOf<ScheduledStage>()
			var cursor = startAt
			for (stage in plan.stages) {
				val state = progress[stage.id]
				val planned = hoursToMillis(stage.hours + (state?.adjustmentHours ?: 0.0))
				val end = state?.completedAt ?: (cursor + planned)
				val windowEnd = stage.usableWindowHours?.let { end + hoursToMillis(it) }
				stages.add(
					ScheduledStage(
						id = stage.id,
						stage = stage,
						start = cursor,
						end = end,
						windowEnd = windowEnd,
						foldTimes = Scheduler.foldTimes(stage, cursor, end),
					),
				)
				cursor = end
			}
			return stages
		}

	val readyAt: Long
		get() {
			val stages = timeline
			val bakeIndex = stages.indexOfFirst { it.stage.kind == StageKind.BAKE }
			if (bakeIndex < 0) return stages.lastOrNull()?.end ?: startAt
			return if (bakeIndex > 0) stages[bakeIndex - 1].end else startAt
		}

	val fermentationLoadHours: Double get() = plan.fermentationLoadHours

	/** Yeast the current schedule asks for, on an instant-dry basis. */
	val suggestedInstantYeastPercent: Double
		get() = Leavening.instantYeastPercent(
			equivalentHours = fermentationLoadHours,
			saltPercent = formula.saltPercent,
			sugarPercent = formula.sugarPercent,
			prefermentedFlourFraction = formula.prefermentedFlourFraction,
		)

	val suggestedLevainPercent: Double
		get() = Leavening.levainPercent(fermentationLoadHours, formula.saltPercent)

	fun status(stage: ScheduledStage, now: Long): StageStatus = when {
		progress[stage.id]?.completedAt != null -> StageStatus.DONE
		now < stage.start -> StageStatus.UPCOMING
		now < stage.end -> StageStatus.ACTIVE
		now >= stage.end + 30 * 60_000L -> StageStatus.OVERDUE
		else -> StageStatus.DUE
	}

	fun currentStage(now: Long): ScheduledStage? {
		val stages = timeline
		return stages.firstOrNull { status(it, now) == StageStatus.ACTIVE }
			?: stages.firstOrNull { status(it, now) != StageStatus.DONE }
	}

	/** Stages whose timer has elapsed without being acknowledged. This is the badge count. */
	fun dueStages(now: Long): List<ScheduledStage> = timeline.filter { stage ->
		if (!stage.stage.alerts) return@filter false
		val state = progress[stage.id]
		state?.completedAt == null && state?.acknowledgedAt == null && now >= stage.end
	}

	val isFinished: Boolean
		get() {
			val last = plan.stages.lastOrNull() ?: return true
			return progress[last.id]?.completedAt != null
		}

	/** Every alert this batch still owes you, in fire order. */
	fun upcomingAlerts(now: Long): List<DoughAlert> {
		val alerts = mutableListOf<DoughAlert>()

		for (stage in timeline) {
			val state = progress[stage.id]
			if (state?.completedAt != null) continue

			stage.foldTimes.forEachIndexed { index, fold ->
				if (fold > now) {
					alerts.add(
						DoughAlert(
							id = "$id|${stage.id}|fold|$index",
							batchId = id,
							batchName = name,
							stageId = stage.id,
							kind = DoughAlert.Kind.FOLD,
							fireAt = fold,
							title = "Fold ${index + 1} — $name",
							body = "${stage.stage.title}: time for a set of folds.",
						),
					)
				}
			}

			if (!stage.stage.alerts || stage.end <= now) continue

			val kind = when {
				stage.stage.kind == StageKind.PREFERMENT -> DoughAlert.Kind.PREFERMENT_READY
				stage.stage.kind.isCold -> DoughAlert.Kind.WINDOW_OPEN
				else -> DoughAlert.Kind.STAGE_END
			}

			alerts.add(
				DoughAlert(
					id = "$id|${stage.id}|end",
					batchId = id,
					batchName = name,
					stageId = stage.id,
					kind = kind,
					fireAt = stage.end,
					title = "${stage.stage.title} done — $name",
					body = bodyFor(stage, kind),
				),
			)

			val windowEnd = stage.windowEnd
			if (windowEnd != null && windowEnd > now) {
				alerts.add(
					DoughAlert(
						id = "$id|${stage.id}|window",
						batchId = id,
						batchName = name,
						stageId = stage.id,
						kind = DoughAlert.Kind.WINDOW_CLOSING,
						fireAt = windowEnd,
						title = "Window closing — $name",
						body = "$name is at the end of its usable cold window. Use it or bin it.",
					),
				)
			}
		}

		return alerts.sortedBy { it.fireAt }
	}

	private fun bodyFor(stage: ScheduledStage, kind: DoughAlert.Kind): String = when (kind) {
		DoughAlert.Kind.PREFERMENT_READY ->
			"Preferment is ripe — domed and just starting to fall. Mix now."
		DoughAlert.Kind.WINDOW_OPEN ->
			"Cold ferment is ready. Usable for another " +
				"${Formatting.hours(stage.stage.usableWindowHours ?: 0.0)}."
		else ->
			if (stage.stage.detail.isEmpty()) "Timer's up on ${stage.stage.title.lowercase()}."
			else stage.stage.detail
	}
}

data class DoughAlert(
	val id: String,
	val batchId: String,
	val batchName: String,
	val stageId: String,
	val kind: Kind,
	val fireAt: Long,
	val title: String,
	val body: String,
) {
	enum class Kind { STAGE_END, FOLD, WINDOW_OPEN, WINDOW_CLOSING, PREFERMENT_READY }
}

/**
 * Assigns cumulative badge numbers across every batch, so the launcher badge is correct even
 * if the app never runs between alerts.
 */
object AlertScheduler {
	const val PENDING_LIMIT = 60

	fun badgedAlerts(
		batches: List<Batch>,
		now: Long,
		startingBadge: Int = 0,
		limit: Int = PENDING_LIMIT,
	): List<Pair<DoughAlert, Int>> {
		val all = batches
			.filterNot { it.isArchived }
			.flatMap { it.upcomingAlerts(now) }
			.sortedBy { it.fireAt }
			.take(limit)

		var badge = max(0, startingBadge)
		return all.map { alert ->
			// Fold reminders are nudges, not outstanding work; they don't raise the badge.
			if (alert.kind != DoughAlert.Kind.FOLD) badge += 1
			alert to badge
		}
	}

	/** Badge to show right now, counting only alerts that have already fired unacknowledged. */
	fun currentBadge(batches: List<Batch>, now: Long): Int =
		batches.filterNot { it.isArchived }.sumOf { it.dueStages(now).size }
}
