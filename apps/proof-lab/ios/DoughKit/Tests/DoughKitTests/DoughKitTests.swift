import XCTest
@testable import DoughKit

private let start = Date(timeIntervalSince1970: 1_757_000_000)
private let hour: TimeInterval = 3600

private func makeBatch(
	plan: FermentationPlan = .coldBallRetard,
	progress: [String: StageProgress] = [:],
	name: String = "Friday service",
	id: UUID = UUID(uuidString: "00000000-0000-0000-0000-000000000001")!
) -> Batch {
	Batch(
		id: id,
		name: name,
		createdAt: start,
		startAt: start,
		formula: DoughStyle.style(id: "new-york")!.formula,
		plan: plan,
		progress: progress
	)
}

final class FermentationTests: XCTestCase {
	func testReferenceTemperatureHasUnitRate() {
		XCTAssertEqual(Fermentation.rateMultiplier(atC: 24), 1, accuracy: 1e-12)
	}

	func testFridgeIsRoughlySixTimesSlower() {
		let rate = Fermentation.rateMultiplier(atC: 4)
		XCTAssertTrue((0.14...0.18).contains(rate), "rate(4) was \(rate)")
	}

	func testRateRisesWithTemperature() {
		let rates = [-2.0, 0, 4, 10, 15, 20, 24, 30, 35, 45].map { Fermentation.rateMultiplier(atC: $0) }
		for (a, b) in zip(rates, rates.dropFirst()) {
			XCTAssertGreaterThan(b, a)
		}
	}

	func testSegmentQ10sShowUpAsTenDegreeRatios() {
		XCTAssertEqual(
			Fermentation.rateMultiplier(atC: 30) / Fermentation.rateMultiplier(atC: 20),
			2.0,
			accuracy: 1e-9
		)
		XCTAssertEqual(
			Fermentation.rateMultiplier(atC: 20) / Fermentation.rateMultiplier(atC: 10),
			2.5,
			accuracy: 1e-9
		)
		XCTAssertEqual(
			Fermentation.rateMultiplier(atC: 10) / Fermentation.rateMultiplier(atC: 0),
			3.0,
			accuracy: 1e-9
		)
	}

	func testEquivalentHoursRoundTrip() {
		for temp in [2.0, 4, 12, 20, 24, 30] {
			for hours in [0.5, 3, 18, 72] {
				let equivalent = Fermentation.equivalentHours(hours: hours, atC: temp)
				let back = Fermentation.hours(forEquivalentHours: equivalent, atC: temp)
				XCTAssertEqual(back, hours, accuracy: 1e-9)
			}
		}
	}

	func testNegativeDurationsDoNotFerment() {
		XCTAssertEqual(Fermentation.equivalentHours(hours: -5, atC: 24), 0, accuracy: 1e-12)
	}

	func testDoseFallsAsTheScheduleLengthens() {
		let doses = [2.0, 4, 8, 16, 32].map { Leavening.instantYeastPercent(equivalentHours: $0) }
		for (a, b) in zip(doses, doses.dropFirst()) {
			XCTAssertLessThan(b, a)
		}
	}

	func testDosesAreClamped() {
		XCTAssertEqual(
			Leavening.instantYeastPercent(equivalentHours: 0),
			Leavening.maxInstantYeastPercent,
			accuracy: 1e-12
		)
		XCTAssertEqual(
			Leavening.instantYeastPercent(equivalentHours: 10_000),
			Leavening.minInstantYeastPercent,
			accuracy: 1e-12
		)
		XCTAssertEqual(Leavening.levainPercent(equivalentHours: 0), Leavening.maxLevainPercent, accuracy: 1e-12)
	}

	func testPrefermentReducesTheFinalYeast() {
		let straight = Leavening.instantYeastPercent(equivalentHours: 8)
		let withPoolish = Leavening.instantYeastPercent(equivalentHours: 8, prefermentedFlourFraction: 0.3)
		XCTAssertEqual(withPoolish, straight * (1 - 0.8 * 0.3), accuracy: 1e-12)
	}
}

final class FormulaTests: XCTestCase {
	private func grams(_ rows: [Ingredient], _ id: String) -> Double {
		rows.first { $0.id == id }?.grams ?? 0
	}

	func testIngredientsAddUpToTheDoughYouAskedFor() {
		let formula = DoughFormula(
			ballCount: 20,
			ballWeightGrams: 280,
			lossPercent: 2,
			hydrationPercent: 65,
			saltPercent: 2.5,
			oilPercent: 2,
			sugarPercent: 1,
			instantYeastPercent: 0.2
		)
		let result = formula.result()
		XCTAssertEqual(result.totalDoughGrams, 20 * 280 * 1.02, accuracy: 1e-9)
		XCTAssertEqual(result.overall.reduce(0) { $0 + $1.grams }, result.totalDoughGrams, accuracy: 1e-6)
	}

	func testPrefermentMovesFlourRatherThanAddingIt() {
		let formula = DoughFormula(
			hydrationPercent: 70,
			prefermentKind: .poolish,
			prefermentedFlourPercent: 30,
			prefermentHydrationPercent: 100,
			prefermentYeastPercent: 0.15
		)
		let result = formula.result()
		XCTAssertEqual(result.prefermentFlourGrams, result.totalFlourGrams * 0.3, accuracy: 1e-9)
		XCTAssertEqual(
			grams(result.finalMix, "flour") + result.prefermentFlourGrams,
			result.totalFlourGrams,
			accuracy: 1e-9
		)
	}

	func testLevainBuildEqualsTheRipeLevain() {
		let result = DoughStyle.style(id: "country-loaf")!.formula.result()
		XCTAssertEqual(
			result.prefermentBuild.reduce(0) { $0 + $1.grams },
			result.prefermentTotalGrams,
			accuracy: 1e-9
		)
		XCTAssertNil(result.overall.first { $0.id == "yeast" })
	}

	func testFreshYeastWeighsThreeTimesInstant() {
		var formula = DoughFormula(instantYeastPercent: 0.2)
		XCTAssertEqual(formula.scoopedYeastPercent, 0.2, accuracy: 1e-12)
		formula.yeastType = .freshCake
		XCTAssertEqual(formula.scoopedYeastPercent, 0.6, accuracy: 1e-12)
	}

	func testEveryShippedStyleIsSelfConsistent() {
		for style in DoughStyle.library {
			let result = style.formula.result()
			XCTAssertEqual(
				result.overall.reduce(0) { $0 + $1.grams },
				result.totalDoughGrams,
				accuracy: 1e-6,
				"overall drifted for \(style.id)"
			)
			XCTAssertEqual(
				result.finalMix.reduce(0) { $0 + $1.grams },
				result.totalDoughGrams,
				accuracy: 1e-6,
				"final mix drifted for \(style.id)"
			)
		}
	}

	func testZeroBallCountDegradesQuietly() {
		let result = DoughFormula(ballCount: 0).result()
		XCTAssertEqual(result.totalDoughGrams, 0, accuracy: 1e-12)
		XCTAssertTrue(result.overall.allSatisfy { $0.grams.isFinite })
	}

	func testBallsSpreadEvenlyAcrossMixes() {
		let result = DoughFormula(ballCount: 7, ballWeightGrams: 1000, lossPercent: 0).result()
		let plan = result.productionPlan(mixerCapacityKg: 2)
		XCTAssertEqual(plan.mixes.map(\.ballCount), [2, 2, 2, 1])
		XCTAssertEqual(plan.mixes.reduce(0) { $0 + $1.doughGrams }, result.totalDoughGrams, accuracy: 1e-6)
	}

	func testNonsenseCapacityDoesNotHang() {
		let plan = DoughFormula(ballCount: 5, ballWeightGrams: 250).result()
			.productionPlan(mixerCapacityKg: 0)
		XCTAssertGreaterThanOrEqual(plan.mixCount, 1)
		XCTAssertEqual(plan.mixes.reduce(0) { $0 + $1.ballCount }, 5)
	}
}

final class WaterTemperatureTests: XCTestCase {
	func testThreeFactorMethod() {
		let water = DoughTemperature.waterTemperature(
			desiredDoughTempC: 24,
			flourTempC: 21,
			roomTempC: 22,
			prefermentTempC: nil,
			frictionC: MixerKind.spiral.frictionC
		)
		XCTAssertEqual(water, 25, accuracy: 1e-9)
	}

	func testIceSplitSatisfiesTheHeatBalance() {
		let split = DoughTemperature.iceSplit(waterGrams: 10_000, tapTempC: 20, targetC: 4)
		XCTAssertEqual(split.ice + split.water, 10_000, accuracy: 1e-9)
		XCTAssertEqual(split.ice * (80 + 4), split.water * (20 - 4), accuracy: 1e-6)
	}

	func testImpossibleTemperatureIsFlagged() {
		let result = DoughTemperature.solve(
			desiredDoughTempC: 22,
			flourTempC: 32,
			roomTempC: 34,
			prefermentTempC: nil,
			mixer: .planetary,
			totalWaterGrams: 5_000,
			tapWaterTempC: 24
		)
		XCTAssertLessThan(result.waterTemperatureC, 0)
		XCTAssertNotNil(result.warning)
		XCTAssertEqual(result.iceGrams + result.waterGrams, 5_000, accuracy: 1e-9)
	}
}

final class ScheduleTests: XCTestCase {
	func testReadyByLandsExactlyWhenAsked() {
		for plan in FermentationPlan.library {
			let schedule = Scheduler.build(plan: plan, anchor: .readyBy(start))
			XCTAssertEqual(
				schedule.readyAt.timeIntervalSince1970,
				start.timeIntervalSince1970,
				accuracy: 0.001,
				"plan \(plan.id) missed its ready time"
			)
		}
	}

	func testStagesAreLaidEndToEnd() {
		let schedule = Scheduler.build(plan: .sourdoughCountryLoaf, anchor: .startAt(start))
		for (a, b) in zip(schedule.stages, schedule.stages.dropFirst()) {
			XCTAssertEqual(a.end, b.start)
		}
	}

	func testColdStageCarriesItsWindow() {
		let schedule = Scheduler.build(plan: .coldBallRetard, anchor: .startAt(start))
		let cold = schedule.stages.first { $0.stage.kind == .coldRetard }!
		XCTAssertEqual(cold.windowEnd, cold.end.addingTimeInterval(24 * hour))
	}

	func testFoldRemindersSitInTheFirstPartOfTheBulk() {
		let schedule = Scheduler.build(plan: .sourdoughCountryLoaf, anchor: .startAt(start))
		let bulk = schedule.stages.first { $0.id == "bulk" }!
		XCTAssertEqual(bulk.foldTimes.count, 5)
		XCTAssertTrue(bulk.foldTimes.allSatisfy { $0 > bulk.start && $0 < bulk.end })
		for (a, b) in zip(bulk.foldTimes, bulk.foldTimes.dropFirst()) {
			XCTAssertEqual(b.timeIntervalSince(a), 30 * 60, accuracy: 0.001)
		}
	}

	func testReadyTimeExcludesTheBake() {
		let schedule = Scheduler.build(plan: .sourdoughCountryLoaf, anchor: .startAt(start))
		XCTAssertEqual(schedule.stages.last!.stage.kind, .bake)
		XCTAssertEqual(schedule.readyAt, schedule.stages.last!.start)
		XCTAssertGreaterThan(schedule.finishAt, schedule.readyAt)
	}

	func testPlanAndStageIDsAreUnique() {
		let planIDs = FermentationPlan.library.map(\.id)
		XCTAssertEqual(planIDs.count, Set(planIDs).count)
		for plan in FermentationPlan.library {
			let stageIDs = plan.stages.map(\.id)
			XCTAssertEqual(stageIDs.count, Set(stageIDs).count, "duplicate stage ids in \(plan.id)")
		}
	}

	func testEveryStylePointsAtARealPlan() {
		for style in DoughStyle.library {
			XCTAssertNotNil(FermentationPlan.plan(id: style.planID), "\(style.id) points at a missing plan")
		}
	}
}

final class BatchTests: XCTestCase {
	func testFinishingEarlyDragsEverythingEarlier() {
		let plain = makeBatch()
		let bulk = plain.timeline.first { $0.id == "bulk" }!
		let early = bulk.end.addingTimeInterval(-30 * 60)
		let shifted = makeBatch(progress: ["bulk": StageProgress(completedAt: early)])

		XCTAssertEqual(shifted.timeline.first { $0.id == "bulk" }!.end, early)
		XCTAssertEqual(
			shifted.readyAt.timeIntervalSince1970,
			plain.readyAt.timeIntervalSince1970 - 30 * 60,
			accuracy: 0.001
		)
	}

	func testExtendingAStagePushesTheBakeBack() {
		let plain = makeBatch()
		let extended = makeBatch(progress: ["temper": StageProgress(adjustmentHours: 0.5)])
		XCTAssertEqual(
			extended.readyAt.timeIntervalSince1970,
			plain.readyAt.timeIntervalSince1970 + 30 * 60,
			accuracy: 0.001
		)
	}

	func testStageStatusWalksThroughItsStates() {
		let batch = makeBatch()
		let bulk = batch.timeline.first { $0.id == "bulk" }!
		XCTAssertEqual(batch.status(of: bulk, at: bulk.start.addingTimeInterval(-1)), .upcoming)
		XCTAssertEqual(batch.status(of: bulk, at: bulk.start.addingTimeInterval(60)), .active)
		XCTAssertEqual(batch.status(of: bulk, at: bulk.end.addingTimeInterval(60)), .due)
		XCTAssertEqual(batch.status(of: bulk, at: bulk.end.addingTimeInterval(45 * 60)), .overdue)
	}

	func testElapsedStageBecomesABadge() {
		let batch = makeBatch()
		let bulk = batch.timeline.first { $0.id == "bulk" }!
		let after = bulk.end.addingTimeInterval(1)
		XCTAssertEqual(batch.dueStages(at: after).map(\.id), ["bulk"])
		XCTAssertEqual(AlertScheduler.currentBadge(batches: [batch], at: after), 1)
	}

	func testAcknowledgingClearsTheBadgeWithoutCompleting() {
		let batch = makeBatch()
		let bulk = batch.timeline.first { $0.id == "bulk" }!
		let acked = makeBatch(progress: ["bulk": StageProgress(acknowledgedAt: bulk.end)])
		XCTAssertTrue(acked.dueStages(at: bulk.end.addingTimeInterval(1)).isEmpty)
	}

	func testArchivedBatchesAreLeftOutOfTheBadge() {
		let live = makeBatch(id: UUID())
		var shelved = makeBatch(id: UUID())
		shelved.isArchived = true
		let after = live.timeline.first { $0.id == "bulk" }!.end.addingTimeInterval(1)
		XCTAssertEqual(AlertScheduler.currentBadge(batches: [live, shelved], at: after), 1)
	}

	func testColdStageFiresOnBothEdgesOfItsWindow() {
		let alerts = makeBatch().upcomingAlerts(from: start).filter { $0.stageID == "cold" }
		let kinds = Set(alerts.map(\.kind))
		XCTAssertTrue(kinds.contains(.windowOpen))
		XCTAssertTrue(kinds.contains(.windowClosing))
		let open = alerts.first { $0.kind == .windowOpen }!
		let close = alerts.first { $0.kind == .windowClosing }!
		XCTAssertEqual(close.fireAt.timeIntervalSince(open.fireAt), 24 * hour, accuracy: 0.001)
	}

	func testFoldRemindersBecomeAlerts() {
		let alerts = makeBatch(plan: .sourdoughCountryLoaf).upcomingAlerts(from: start)
		let folds = alerts.filter { $0.kind == .fold }
		XCTAssertEqual(folds.count, 5)
		XCTAssertEqual(folds.first?.title, "Fold 1 — Friday service")
	}

	func testCompletedStagesStopNagging() {
		let batch = makeBatch(progress: ["bulk": StageProgress(completedAt: start.addingTimeInterval(hour))])
		XCTAssertTrue(batch.upcomingAlerts(from: start).allSatisfy { $0.stageID != "bulk" })
	}

	func testAlertIDsAreStableAndUnique() {
		let ids = makeBatch().upcomingAlerts(from: start).map(\.id)
		XCTAssertEqual(ids.count, Set(ids).count)
		XCTAssertEqual(ids, makeBatch().upcomingAlerts(from: start).map(\.id))
	}

	func testBadgesClimbForRealAlertsButNotFolds() {
		let badged = AlertScheduler.badgedAlerts(
			batches: [makeBatch(plan: .sourdoughCountryLoaf)],
			from: start
		)
		XCTAssertFalse(badged.isEmpty)
		var previous = 0
		for (alert, badge) in badged {
			if alert.kind == .fold {
				XCTAssertEqual(badge, previous, "a fold reminder should not raise the badge")
			} else {
				XCTAssertEqual(badge, previous + 1)
			}
			previous = badge
		}
	}

	func testPendingAlertsAreCappedToThePlatformLimit() {
		let many = (1...40).map { _ in makeBatch(id: UUID()) }
		let badged = AlertScheduler.badgedAlerts(batches: many, from: start)
		XCTAssertLessThanOrEqual(badged.count, AlertScheduler.pendingLimit)
	}
}

final class ComparisonTests: XCTestCase {
	func testAnUnchangedCloneShowsNoDifferences() {
		let comparison = BatchComparison(left: makeBatch(), right: makeBatch(id: UUID()))
		XCTAssertTrue(comparison.changedRows.isEmpty, "unexpected diffs: \(comparison.changedRows)")
	}

	func testChangingOneThingShowsThatOneThing() {
		let original = makeBatch()
		var tweaked = makeBatch(id: UUID())
		tweaked.formula.hydrationPercent = 68
		let changed = BatchComparison(left: original, right: tweaked).changedRows.map(\.id)
		XCTAssertTrue(changed.contains("hydration"))
		XCTAssertFalse(changed.contains("salt"))
	}

	func testScoresAppearOnlyWhenBothRunsWereTasted() {
		var tasted = makeBatch()
		tasted.review = BatchReview(crumb: 4)
		XCTAssertTrue(
			BatchComparison(left: tasted, right: makeBatch(id: UUID())).rows
				.allSatisfy { !$0.id.hasPrefix("score-") }
		)

		var other = makeBatch(id: UUID())
		other.review = BatchReview(crumb: 2)
		XCTAssertTrue(
			BatchComparison(left: tasted, right: other).changedRows.contains { $0.id == "score-crumb" }
		)
	}
}

final class FlourBlendTests: XCTestCase {
	private let blend = FlourBlend([
		FlourLibrary.at("bread", 80),
		FlourLibrary.at("whole-wheat", 20)
	])

	func testProteinIsTheWeightedAverage() {
		XCTAssertEqual(blend.proteinPercent, 12.86, accuracy: 1e-9)
	}

	func testWholeGrainShareCountsOnlyWholeGrains() {
		XCTAssertEqual(blend.wholeGrainFraction, 0.2, accuracy: 1e-9)
		XCTAssertEqual(FlourBlend(FlourLibrary.defaultBlend).wholeGrainFraction, 0, accuracy: 1e-9)
	}

	func testSharesAreRescaledRatherThanTrusted() {
		let sloppy = FlourBlend([FlourLibrary.at("bread", 40), FlourLibrary.at("semola", 10)])
		XCTAssertEqual(sloppy.normalized.reduce(0) { $0 + $1.percent }, 100, accuracy: 1e-9)
		XCTAssertEqual(sloppy.normalized[0].percent, 80, accuracy: 1e-9)
	}

	func testEmptyOrZeroedBlendsStillWork() {
		XCTAssertEqual(FlourBlend([]).normalized.count, 1)
		let zeroed = FlourBlend([FlourLibrary.at("bread", 0), FlourLibrary.at("rye", 0)])
		XCTAssertEqual(zeroed.normalized.reduce(0) { $0 + $1.percent }, 100, accuracy: 1e-9)
		XCTAssertEqual(zeroed.normalized[0].percent, 50, accuracy: 1e-9)
	}

	func testSummaryReadsLikeABakerWouldSayIt() {
		XCTAssertEqual(blend.summary, "80 % Bread flour · 20 % Whole wheat")
	}

	func testFlourLibraryIsSaneAndUnique() {
		let ids = FlourLibrary.all.map(\.id)
		XCTAssertEqual(ids.count, Set(ids).count)
		XCTAssertTrue(FlourLibrary.all.allSatisfy { (8.0...16.0).contains($0.proteinPercent) })
	}

	func testEachFlourGetsItsOwnRow() {
		let formula = DoughFormula(
			ballCount: 10,
			ballWeightGrams: 300,
			lossPercent: 0,
			hydrationPercent: 70,
			flours: [
				FlourLibrary.at("bread", 70),
				FlourLibrary.at("whole-wheat", 20),
				FlourLibrary.at("rye", 10)
			]
		)
		let result = formula.result()
		let rows = result.overall.filter { $0.id.hasPrefix("flour") }
		XCTAssertEqual(rows.count, 3)
		XCTAssertEqual(rows.reduce(0) { $0 + $1.grams }, result.totalFlourGrams, accuracy: 1e-9)
		XCTAssertEqual(result.totalDoughGrams, 3000, accuracy: 1e-9)
		XCTAssertEqual(result.finalMix.reduce(0) { $0 + $1.grams }, 3000, accuracy: 1e-6)
	}

	func testASingleFlourStaysASingleRow() {
		let rows = DoughFormula().result().overall.filter { $0.id.hasPrefix("flour") }
		XCTAssertEqual(rows.count, 1)
		XCTAssertEqual(rows[0].id, "flour")
	}

	func testWholeGrainDoughsGetLessLeaven() {
		let white = Leavening.instantYeastPercent(equivalentHours: 8)
		let wholemeal = Leavening.instantYeastPercent(equivalentHours: 8, wholeGrainFraction: 1)
		XCTAssertEqual(wholemeal, white * (1 - Leavening.wholeGrainSpeedup), accuracy: 1e-12)
	}

	func testEveryShippedStyleHasABlendThatAddsToAHundred() {
		for style in DoughStyle.library {
			XCTAssertEqual(
				style.formula.flours.reduce(0) { $0 + $1.percent },
				100,
				accuracy: 1e-9,
				"\(style.id) blend doesn't add to 100"
			)
		}
	}

	func testTheSourdoughShelfCoversSeveralPizzaStyles() {
		let pizza = DoughStyle.sourdough.filter { $0.family == .pizza }
		XCTAssertGreaterThanOrEqual(pizza.count, 4)
		XCTAssertGreaterThanOrEqual(Set(pizza.map(\.planID)).count, 3)
	}
}

private func steadySeries(
	tempC: Double,
	hours: Double,
	everyMinutes: Double = 1,
	height: ((Double) -> Double)? = nil
) -> SensorSeries {
	let step = everyMinutes * 60
	let count = Int((hours * 3600) / step)
	return SensorSeries((0...count).map { index in
		let offset = Double(index) * step
		return SensorReading(
			date: start.addingTimeInterval(offset),
			doughTempC: tempC,
			doughHeightMm: height.map { $0(offset / 3600) }
		)
	})
}

final class SensingTests: XCTestCase {
	func testSteadyRunAtReferenceBanksOneHourPerHour() {
		XCTAssertEqual(steadySeries(tempC: 24, hours: 5).measuredEquivalentHours(), 5, accuracy: 1e-9)
	}

	func testSteadyRunMatchesTheModel() {
		for temp in [4.0, 12, 20, 28] {
			XCTAssertEqual(
				steadySeries(tempC: temp, hours: 6).measuredEquivalentHours(),
				Fermentation.equivalentHours(hours: 6, atC: temp),
				accuracy: 1e-9,
				"measured run at \(temp) °C drifted from the model"
			)
		}
	}

	func testAWarmWalkInBanksMoreThanPlanned() {
		let planned = Fermentation.equivalentHours(hours: 24, atC: 4)
		let measured = steadySeries(tempC: 5.8, hours: 24).measuredEquivalentHours()
		XCTAssertGreaterThan(measured / planned, 1.15)
	}

	func testAmbientStandsInWithoutADoughProbe() {
		let series = SensorSeries((0...60).map {
			SensorReading(date: start.addingTimeInterval(Double($0) * 60), ambientTempC: 24)
		})
		XCTAssertEqual(series.measuredEquivalentHours(), 1, accuracy: 1e-9)
	}

	func testDoughProbeWinsOverAmbient() {
		let series = SensorSeries((0...60).map {
			SensorReading(
				date: start.addingTimeInterval(Double($0) * 60),
				doughTempC: 24,
				ambientTempC: 4
			)
		})
		XCTAssertEqual(series.measuredEquivalentHours(), 1, accuracy: 1e-9)
	}

	func testTooFewSamplesProduceZeroRatherThanAGuess() {
		XCTAssertEqual(SensorSeries([]).measuredEquivalentHours(), 0, accuracy: 1e-12)
		XCTAssertEqual(
			SensorSeries([SensorReading(date: start, doughTempC: 24)]).measuredEquivalentHours(),
			0,
			accuracy: 1e-12
		)
	}

	func testOutOfOrderSamplesAreSorted() {
		let forward = steadySeries(tempC: 24, hours: 2, everyMinutes: 30)
		let shuffled = SensorSeries(forward.readings.reversed())
		XCTAssertEqual(
			shuffled.measuredEquivalentHours(),
			forward.measuredEquivalentHours(),
			accuracy: 1e-9
		)
	}

	func testGapsAreReportedButStillIntegrated() {
		let series = SensorSeries([
			SensorReading(date: start, doughTempC: 24),
			SensorReading(date: start.addingTimeInterval(90 * 60), doughTempC: 24)
		])
		XCTAssertEqual(series.longestTemperatureGapMinutes(), 90, accuracy: 1e-9)
		XCTAssertEqual(series.measuredEquivalentHours(), 1.5, accuracy: 1e-9)
	}

	func testEffectiveTemperatureBeatsTheArithmeticMeanOnASwingingRun() {
		let series = SensorSeries([
			SensorReading(date: start, doughTempC: 30),
			SensorReading(date: start.addingTimeInterval(3600), doughTempC: 30),
			SensorReading(date: start.addingTimeInterval(3601), doughTempC: 10),
			SensorReading(date: start.addingTimeInterval(7200), doughTempC: 10)
		])
		guard let effective = series.effectiveConstantTemperatureC(),
			  let mean = series.averageTemperatureC() else {
			return XCTFail("expected both figures")
		}
		XCTAssertEqual(mean, 20, accuracy: 0.01)
		XCTAssertGreaterThan(effective, mean)
	}

	func testRiseTracking() {
		let rising = steadySeries(tempC: 24, hours: 4, everyMinutes: 5) { 100 * (1 + 0.2 * $0) }
		XCTAssertEqual(rising.expansionRatio()!, 1.8, accuracy: 1e-9)
		XCTAssertEqual(rising.expansionPercent()!, 80, accuracy: 1e-9)
		XCTAssertEqual(rising.riseRatePercentPerHour()!, 20, accuracy: 1e-6)
		XCTAssertEqual(rising.projectedHoursTo(ratio: 2.0)!, 1, accuracy: 1e-6)
		XCTAssertEqual(rising.projectedHoursTo(ratio: 1.5)!, 0, accuracy: 1e-12)
	}

	func testAStalledOrCollapsingDoughGetsNoProjection() {
		let flat = steadySeries(tempC: 24, hours: 3, everyMinutes: 5) { _ in 150 }
		XCTAssertEqual(flat.riseRatePercentPerHour()!, 0, accuracy: 1e-9)
		XCTAssertNil(flat.projectedHoursTo(ratio: 2.0))

		let collapsing = steadySeries(tempC: 24, hours: 2, everyMinutes: 5) { 200 - 10 * $0 }
		XCTAssertLessThan(collapsing.riseRatePercentPerHour()!, 0)
		XCTAssertNil(collapsing.projectedHoursTo(ratio: 2.0))
	}

	func testHeightsAreOptionalEverywhere() {
		let none = steadySeries(tempC: 24, hours: 2)
		XCTAssertNil(none.expansionRatio())
		XCTAssertNil(none.riseRatePercentPerHour())
		XCTAssertNil(none.projectedHoursTo(ratio: 1.8))
	}

	func testCsvRoundTrip() {
		let series = SensorSeries([
			SensorReading(date: start, doughTempC: 23.5, ambientTempC: 21, relativeHumidity: 68),
			SensorReading(
				date: start.addingTimeInterval(60),
				doughTempC: 23.6,
				doughHeightMm: 101.2,
				co2Ppm: 950
			)
		])
		let decoded = SensorCsv.decode(SensorCsv.encode(series))
		XCTAssertEqual(decoded.ordered.count, 2)
		XCTAssertEqual(decoded.ordered[0].relativeHumidity!, 68, accuracy: 1e-9)
		XCTAssertEqual(decoded.ordered[1].doughHeightMm!, 101.2, accuracy: 1e-9)
		XCTAssertNil(decoded.ordered[0].co2Ppm)
	}

	func testCsvSkipsJunkAndCommentsAndKeepsPartialRows() {
		let millis = Int64(start.timeIntervalSince1970 * 1000)
		let text = """
		# doughscience rig v0
		\(SensorCsv.header)
		\(millis),24.0,,,,,
		not a reading at all
		\(millis + 60_000),24.1
		"""
		let decoded = SensorCsv.decode(text)
		XCTAssertEqual(decoded.readings.count, 2)
		XCTAssertEqual(decoded.ordered.last!.doughTempC!, 24.1, accuracy: 1e-9)
		XCTAssertNil(decoded.ordered.last!.co2Ppm)
	}

	func testAnEmptyLogDecodesToAnEmptySeries() {
		XCTAssertTrue(SensorCsv.decode("").isEmpty)
		XCTAssertTrue(SensorCsv.decode(SensorCsv.header).isEmpty)
	}

	func testRunComparisonFlagsAWarmWalkIn() {
		let cold = FermentationPlan.coldBallRetard.stages.first { $0.kind.isCold }!
		let onPlan = RunComparison(
			stage: cold,
			series: steadySeries(tempC: cold.temperatureC, hours: cold.hours, everyMinutes: 15)
		)
		XCTAssertFalse(onPlan.isSignificant)
		XCTAssertNil(onPlan.caveat)

		let warm = RunComparison(
			stage: cold,
			series: steadySeries(tempC: 6, hours: cold.hours, everyMinutes: 15)
		)
		XCTAssertTrue(warm.isSignificant)
		XCTAssertGreaterThan(warm.ratio!, 1.2)
		XCTAssertEqual(warm.measuredTemperatureC!, 6, accuracy: 0.01)
	}
}
