package com.blahmonster.prooflab.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val FRIDAY_5PM = 1_757_010_000_000L // arbitrary fixed instant; the maths is relative

class ScheduleTest {
	@Test
	fun `ready by anchoring lands the dough exactly when you asked`() {
		for (plan in PlanLibrary.all) {
			val schedule = Scheduler.build(plan, ScheduleAnchor.ReadyBy(FRIDAY_5PM))
			assertEquals(FRIDAY_5PM, schedule.readyAt, "plan ${plan.id} missed its ready time")
			assertTrue(schedule.start < schedule.readyAt)
		}
	}

	@Test
	fun `starting now runs forward for the plan's full length`() {
		val plan = PlanLibrary.coldBallRetard
		val schedule = Scheduler.build(plan, ScheduleAnchor.StartAt(FRIDAY_5PM))
		assertEquals(FRIDAY_5PM, schedule.start)
		val expected = FRIDAY_5PM + plan.stages.sumOf { hoursToMillis(it.hours) }
		assertEquals(expected, schedule.finishAt)
	}

	@Test
	fun `stages are laid end to end with no gaps`() {
		val schedule = Scheduler.build(PlanLibrary.sourdoughCountryLoaf, ScheduleAnchor.StartAt(FRIDAY_5PM))
		schedule.stages.zipWithNext { a, b ->
			assertEquals(a.end, b.start, "gap between ${a.id} and ${b.id}")
		}
	}

	@Test
	fun `a cold stage carries its usable window`() {
		val schedule = Scheduler.build(PlanLibrary.coldBallRetard, ScheduleAnchor.StartAt(FRIDAY_5PM))
		val cold = schedule.stages.first { it.stage.kind == StageKind.COLD_RETARD }
		assertNotNull(cold.windowEnd)
		assertEquals(cold.end + (24 * HOUR_MILLIS), cold.windowEnd)
	}

	@Test
	fun `fold reminders sit inside the first part of the bulk and are evenly spaced`() {
		val schedule = Scheduler.build(PlanLibrary.sourdoughCountryLoaf, ScheduleAnchor.StartAt(FRIDAY_5PM))
		val bulk = schedule.stages.first { it.id == "bulk" }
		assertTrue(bulk.foldTimes.isNotEmpty())
		assertTrue(bulk.foldTimes.all { it > bulk.start && it < bulk.end })
		// 4.5 hours of bulk, folds every 30 minutes, only through the first 60 %.
		assertEquals(5, bulk.foldTimes.size)
		bulk.foldTimes.zipWithNext { a, b -> assertEquals(30 * 60_000L, b - a) }
	}

	@Test
	fun `stages without a fold interval get no reminders`() {
		val schedule = Scheduler.build(PlanLibrary.neapolitanDirect, ScheduleAnchor.StartAt(FRIDAY_5PM))
		assertTrue(schedule.stages.all { it.foldTimes.isEmpty() })
	}

	@Test
	fun `ready time excludes the bake`() {
		val plan = PlanLibrary.sourdoughCountryLoaf
		val schedule = Scheduler.build(plan, ScheduleAnchor.StartAt(FRIDAY_5PM))
		val bake = schedule.stages.last()
		assertEquals(StageKind.BAKE, bake.stage.kind)
		assertEquals(bake.start, schedule.readyAt)
		assertTrue(schedule.finishAt > schedule.readyAt)
	}
}

class PlanLibraryTest {
	@Test
	fun `plan ids are unique`() {
		val ids = PlanLibrary.all.map { it.id }
		assertEquals(ids.size, ids.toSet().size, "duplicate plan ids: $ids")
	}

	@Test
	fun `stage ids are unique within a plan`() {
		for (plan in PlanLibrary.all) {
			val ids = plan.stages.map { it.id }
			assertEquals(ids.size, ids.toSet().size, "duplicate stage ids in ${plan.id}: $ids")
		}
	}

	@Test
	fun `every style points at a plan that exists`() {
		for (style in StyleLibrary.all) {
			assertNotNull(PlanLibrary.plan(style.planId), "${style.id} points at a missing plan")
		}
	}

	@Test
	fun `every plan asks for a dose you can actually weigh out`() {
		for (plan in PlanLibrary.all) {
			val load = plan.fermentationLoadHours
			assertTrue(load > 0, "${plan.id} has no fermentation at all")
			if (plan.leaven == LeavenKind.SOURDOUGH) {
				val levain = Leavening.levainPercent(load)
				assertTrue(levain in 3.0..40.0, "${plan.id} levain came out at $levain %")
			} else {
				val yeast = Leavening.instantYeastPercent(load)
				assertTrue(yeast in 0.05..1.0, "${plan.id} yeast came out at $yeast %")
			}
		}
	}

	@Test
	fun `cold plans really are dominated by their cold time`() {
		val plan = PlanLibrary.coldBallRetard
		val cold = plan.stages.first { it.kind.isCold }
		assertTrue(
			cold.equivalentHours > plan.fermentationLoadHours * 0.5,
			"cold stage should carry most of the fermentation",
		)
	}

	@Test
	fun `sourdough plans open with a levain build`() {
		val sourdough = PlanLibrary.all.filter { it.leaven == LeavenKind.SOURDOUGH }
		assertTrue(sourdough.isNotEmpty())
		for (plan in sourdough) {
			assertEquals(StageKind.PREFERMENT, plan.stages.first().kind, "${plan.id} has no levain build")
			assertEquals(PrefermentKind.LEVAIN, plan.prefermentKind)
		}
	}

	@Test
	fun `a preferment does not count toward the dough's own fermentation load`() {
		val plan = PlanLibrary.poolishPizza
		val withoutPreferment = plan.stages.filter { it.kind != StageKind.PREFERMENT }
		assertEquals(
			withoutPreferment.sumOf { it.equivalentHours },
			plan.fermentationLoadHours,
			1e-12,
		)
	}
}
