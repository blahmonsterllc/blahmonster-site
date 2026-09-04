package com.blahmonster.prooflab.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val START = 1_757_000_000_000L

private fun batch(
	plan: FermentationPlan = PlanLibrary.coldBallRetard,
	progress: Map<String, StageProgress> = emptyMap(),
	name: String = "Friday service",
	id: String = "batch-1",
) = Batch(
	id = id,
	name = name,
	createdAt = START,
	startAt = START,
	formula = StyleLibrary.style("new-york")!!.formula,
	plan = plan,
	progress = progress,
)

class BatchTimelineTest {
	@Test
	fun `the timeline starts when the mix did`() {
		val timeline = batch().timeline
		assertEquals(START, timeline.first().start)
		timeline.zipWithNext { a, b -> assertEquals(a.end, b.start) }
	}

	@Test
	fun `finishing a stage early drags everything after it earlier`() {
		val plain = batch()
		val bulk = plain.timeline.first { it.id == "bulk" }
		val early = bulk.end - 30 * 60_000L

		val shifted = batch(progress = mapOf("bulk" to StageProgress(completedAt = early)))
		assertEquals(early, shifted.timeline.first { it.id == "bulk" }.end)
		assertEquals(
			plain.readyAt - 30 * 60_000L,
			shifted.readyAt,
			"the whole schedule should have moved with it",
		)
	}

	@Test
	fun `giving a stage another half hour pushes the bake back`() {
		val plain = batch()
		val extended = batch(progress = mapOf("temper" to StageProgress(adjustmentHours = 0.5)))
		assertEquals(plain.readyAt + 30 * 60_000L, extended.readyAt)
	}

	@Test
	fun `stage status walks from upcoming through active to overdue`() {
		val b = batch()
		val bulk = b.timeline.first { it.id == "bulk" }

		assertEquals(StageStatus.UPCOMING, b.status(bulk, bulk.start - 1))
		assertEquals(StageStatus.ACTIVE, b.status(bulk, bulk.start + 60_000))
		assertEquals(StageStatus.DUE, b.status(bulk, bulk.end + 60_000))
		assertEquals(StageStatus.OVERDUE, b.status(bulk, bulk.end + 45 * 60_000))
	}

	@Test
	fun `a completed stage reads as done no matter the clock`() {
		val b = batch(progress = mapOf("bulk" to StageProgress(completedAt = START)))
		val bulk = b.timeline.first { it.id == "bulk" }
		assertEquals(StageStatus.DONE, b.status(bulk, START + 10 * HOUR_MILLIS))
	}

	@Test
	fun `the current stage is whatever is running now`() {
		val b = batch()
		val cold = b.timeline.first { it.id == "cold" }
		assertEquals("cold", b.currentStage(cold.start + HOUR_MILLIS)?.id)
	}
}

class BadgeTest {
	@Test
	fun `nothing is due before the first timer goes off`() {
		assertEquals(0, batch().dueStages(START).size)
		assertEquals(0, AlertScheduler.currentBadge(listOf(batch()), START))
	}

	@Test
	fun `an elapsed stage becomes a badge`() {
		val b = batch()
		val bulk = b.timeline.first { it.id == "bulk" }
		assertEquals(listOf("bulk"), b.dueStages(bulk.end + 1).map { it.id })
		assertEquals(1, AlertScheduler.currentBadge(listOf(b), bulk.end + 1))
	}

	@Test
	fun `acknowledging a stage clears its badge without completing it`() {
		val b = batch()
		val bulk = b.timeline.first { it.id == "bulk" }
		val acked = batch(progress = mapOf("bulk" to StageProgress(acknowledgedAt = bulk.end)))
		assertEquals(0, acked.dueStages(bulk.end + 1).size)
	}

	@Test
	fun `stages that never alert never badge`() {
		val b = batch()
		val mix = b.timeline.first { it.id == "mix" }
		assertTrue(!mix.stage.alerts)
		assertTrue(b.dueStages(mix.end + 1).none { it.id == "mix" })
	}

	@Test
	fun `badges accumulate across several batches`() {
		val one = batch(id = "a", name = "Trial A")
		val two = batch(id = "b", name = "Trial B")
		val bulkEnd = one.timeline.first { it.id == "bulk" }.end
		assertEquals(2, AlertScheduler.currentBadge(listOf(one, two), bulkEnd + 1))
	}

	@Test
	fun `archived batches are left out of the badge`() {
		val live = batch(id = "a")
		val shelved = batch(id = "b").copy(isArchived = true)
		val bulkEnd = live.timeline.first { it.id == "bulk" }.end
		assertEquals(1, AlertScheduler.currentBadge(listOf(live, shelved), bulkEnd + 1))
	}
}

class AlertTest {
	@Test
	fun `alerts come back in fire order and only from the future`() {
		val alerts = batch().upcomingAlerts(START)
		assertTrue(alerts.isNotEmpty())
		assertTrue(alerts.all { it.fireAt > START })
		alerts.zipWithNext { a, b -> assertTrue(b.fireAt >= a.fireAt) }
	}

	@Test
	fun `a cold stage fires both when the window opens and when it closes`() {
		val alerts = batch().upcomingAlerts(START).filter { it.stageId == "cold" }
		val kinds = alerts.map { it.kind }.toSet()
		assertTrue(DoughAlert.Kind.WINDOW_OPEN in kinds)
		assertTrue(DoughAlert.Kind.WINDOW_CLOSING in kinds)

		val open = alerts.first { it.kind == DoughAlert.Kind.WINDOW_OPEN }
		val close = alerts.first { it.kind == DoughAlert.Kind.WINDOW_CLOSING }
		assertEquals(24 * HOUR_MILLIS, close.fireAt - open.fireAt)
	}

	@Test
	fun `a levain build announces itself as a preferment`() {
		val alerts = batch(plan = PlanLibrary.sourdoughCountryLoaf).upcomingAlerts(START)
		val levain = alerts.first { it.stageId == "levain" }
		assertEquals(DoughAlert.Kind.PREFERMENT_READY, levain.kind)
		assertTrue(levain.body.contains("ripe"))
	}

	@Test
	fun `fold reminders are emitted for stages that ask for them`() {
		val alerts = batch(plan = PlanLibrary.sourdoughCountryLoaf).upcomingAlerts(START)
		val folds = alerts.filter { it.kind == DoughAlert.Kind.FOLD }
		assertEquals(5, folds.size)
		assertEquals("Fold 1 — Friday service", folds.first().title)
	}

	@Test
	fun `completed stages stop nagging`() {
		val b = batch(progress = mapOf("bulk" to StageProgress(completedAt = START + HOUR_MILLIS)))
		assertTrue(b.upcomingAlerts(START).none { it.stageId == "bulk" })
	}

	@Test
	fun `alert ids are stable and unique so they can be cancelled`() {
		val alerts = batch().upcomingAlerts(START)
		val ids = alerts.map { it.id }
		assertEquals(ids.size, ids.toSet().size)
		assertEquals(alerts.map { it.id }, batch().upcomingAlerts(START).map { it.id })
	}

	@Test
	fun `badge numbers climb with each real alert but not with folds`() {
		val badged = AlertScheduler.badgedAlerts(
			listOf(batch(plan = PlanLibrary.sourdoughCountryLoaf)),
			now = START,
		)
		assertTrue(badged.isNotEmpty())

		var previous = 0
		for ((alert, badge) in badged) {
			if (alert.kind == DoughAlert.Kind.FOLD) {
				assertEquals(previous, badge, "a fold reminder should not raise the badge")
			} else {
				assertEquals(previous + 1, badge)
			}
			previous = badge
		}
	}

	@Test
	fun `the pending list is capped so it fits the platform limit`() {
		val many = (1..40).map { batch(id = "batch-$it", name = "Trial $it") }
		val badged = AlertScheduler.badgedAlerts(many, now = START)
		assertTrue(badged.size <= AlertScheduler.PENDING_LIMIT)
		badged.zipWithNext { a, b -> assertTrue(b.first.fireAt >= a.first.fireAt) }
	}
}

class ComparisonTest {
	@Test
	fun `an unchanged clone shows no differences`() {
		val comparison = BatchComparison(batch(), batch(id = "batch-2"))
		assertTrue(comparison.changedRows.isEmpty(), "unexpected diffs: ${comparison.changedRows}")
	}

	@Test
	fun `changing one thing shows exactly that one thing`() {
		val original = batch()
		val tweaked = batch(id = "batch-2").copy(
			formula = original.formula.copy(hydrationPercent = 68.0),
		)
		val changed = BatchComparison(original, tweaked).changedRows.map { it.id }
		assertTrue("hydration" in changed)
		assertTrue("flour" in changed) // more water means less flour for the same dough weight
		assertTrue("salt" !in changed)
		assertTrue("plan" !in changed)
	}

	@Test
	fun `review scores are compared when both runs have been tasted`() {
		val a = batch().copy(review = BatchReview(crumb = 4, overall = 4))
		val b = batch(id = "batch-2").copy(review = BatchReview(crumb = 2, overall = 3))
		val comparison = BatchComparison(a, b)
		assertNotNull(comparison.rows.firstOrNull { it.id == "score-crumb" })
		assertTrue(comparison.changedRows.any { it.id == "score-crumb" })
	}

	@Test
	fun `scores are left out when only one run has been tasted`() {
		val comparison = BatchComparison(batch().copy(review = BatchReview()), batch(id = "batch-2"))
		assertTrue(comparison.rows.none { it.id.startsWith("score-") })
	}
}
