package com.blahmonster.prooflab.core

import kotlin.math.max
import kotlin.math.roundToLong

const val HOUR_MILLIS = 3_600_000L

/** Hours to milliseconds, rounded once so schedules don't accumulate truncation drift. */
fun hoursToMillis(hours: Double): Long = (max(0.0, hours) * HOUR_MILLIS).roundToLong()

sealed interface ScheduleAnchor {
	/** Forward from a mix time. */
	data class StartAt(val epochMillis: Long) : ScheduleAnchor

	/** Backward from when the dough has to be on the bench. */
	data class ReadyBy(val epochMillis: Long) : ScheduleAnchor
}

data class ScheduledStage(
	val id: String,
	val stage: PlanStage,
	val start: Long,
	val end: Long,
	/** Cold stages only: the far edge of the usable window. */
	val windowEnd: Long?,
	/** Fold / dimple reminders inside the stage. */
	val foldTimes: List<Long>,
) {
	val durationMillis: Long get() = end - start
}

data class Schedule(
	val stages: List<ScheduledStage>,
	val start: Long,
	/** End of the last stage before the bake. */
	val readyAt: Long,
	val finishAt: Long,
	val fermentationLoadHours: Double,
)

object Scheduler {
	fun foldTimes(stage: PlanStage, start: Long, end: Long): List<Long> {
		val interval = stage.foldIntervalMinutes ?: return emptyList()
		if (interval <= 0) return emptyList()
		val step = interval * 60_000L
		// Folds belong in the first half of a bulk; past that you're degassing a proofed dough.
		val cutoff = start + (max(0L, end - start) * 0.6).toLong()
		val times = mutableListOf<Long>()
		var next = start + step
		while (next < cutoff && times.size < 8) {
			times.add(next)
			next += step
		}
		return times
	}

	/**
	 * Lays a plan out on the clock. Stage durations are taken as given; [durationOverride] lets
	 * a running batch substitute the time a stage actually took.
	 */
	fun build(
		plan: FermentationPlan,
		anchor: ScheduleAnchor,
		durationOverride: ((PlanStage) -> Long?)? = null,
	): Schedule {
		val durations = plan.stages.map { stage ->
			durationOverride?.invoke(stage) ?: hoursToMillis(stage.hours)
		}
		// Anchor off the same rounded durations the timeline uses, so "ready by 5pm" is 5pm
		// on the nose rather than a few milliseconds either side of it.
		val bakeIndex = plan.stages.indexOfFirst { it.kind == StageKind.BAKE }
		val millisToReady =
			if (bakeIndex < 0) durations.sum() else durations.take(bakeIndex).sum()

		val start = when (anchor) {
			is ScheduleAnchor.StartAt -> anchor.epochMillis
			is ScheduleAnchor.ReadyBy -> anchor.epochMillis - millisToReady
		}

		val scheduled = mutableListOf<ScheduledStage>()
		var cursor = start
		var readyAt = start

		for ((index, stage) in plan.stages.withIndex()) {
			val end = cursor + durations[index]
			val windowEnd = stage.usableWindowHours?.let { end + hoursToMillis(it) }
			scheduled.add(
				ScheduledStage(
					id = stage.id,
					stage = stage,
					start = cursor,
					end = end,
					windowEnd = windowEnd,
					foldTimes = foldTimes(stage, cursor, end),
				),
			)
			if (stage.kind != StageKind.BAKE) readyAt = end
			cursor = end
		}

		return Schedule(
			stages = scheduled,
			start = start,
			readyAt = readyAt,
			finishAt = cursor,
			fermentationLoadHours = plan.fermentationLoadHours,
		)
	}
}
