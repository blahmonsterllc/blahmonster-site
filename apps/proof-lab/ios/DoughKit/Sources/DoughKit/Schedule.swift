import Foundation

public enum ScheduleAnchor: Sendable, Equatable {
	/// Forward from a mix time.
	case startAt(Date)
	/// Backward from when the dough has to be on the bench.
	case readyBy(Date)
}

public struct ScheduledStage: Sendable, Equatable, Identifiable {
	public let id: String
	public let stage: PlanStage
	public let start: Date
	public let end: Date
	/// Cold stages only: the far edge of the usable window.
	public let windowEnd: Date?
	/// Fold / dimple reminders inside the stage.
	public let foldTimes: [Date]

	public var duration: TimeInterval { end.timeIntervalSince(start) }
}

public struct Schedule: Sendable, Equatable {
	public let stages: [ScheduledStage]
	public let start: Date
	/// End of the last stage before the bake.
	public let readyAt: Date
	public let finishAt: Date
	public let fermentationLoadHours: Double
}

public enum Scheduler {
	static func foldTimes(for stage: PlanStage, start: Date, end: Date) -> [Date] {
		guard let interval = stage.foldIntervalMinutes, interval > 0 else { return [] }
		let step = Double(interval) * 60
		// Folds belong in the first half of a bulk; past that you're degassing a proofed dough.
		let cutoff = start.addingTimeInterval(max(0, end.timeIntervalSince(start)) * 0.6)
		var times: [Date] = []
		var next = start.addingTimeInterval(step)
		while next < cutoff, times.count < 8 {
			times.append(next)
			next = next.addingTimeInterval(step)
		}
		return times
	}

	/// Lays a plan out on the clock. Stage durations are taken as given; `durationOverride`
	/// lets a running batch substitute the time a stage actually took.
	public static func build(
		plan: FermentationPlan,
		anchor: ScheduleAnchor,
		durationOverride: ((PlanStage) -> TimeInterval?)? = nil
	) -> Schedule {
		let durations = plan.stages.map { stage in
			durationOverride?(stage) ?? max(0, stage.hours) * 3600
		}
		// Anchor off the same durations the timeline uses, so "ready by 5pm" is 5pm on the
		// nose rather than a rounding error either side of it.
		let bakeIndex = plan.stages.firstIndex { $0.kind == .bake }
		let secondsToReady = durations[..<(bakeIndex ?? durations.endIndex)].reduce(0, +)

		let start: Date
		switch anchor {
		case .startAt(let date):
			start = date
		case .readyBy(let date):
			start = date.addingTimeInterval(-secondsToReady)
		}

		var scheduled: [ScheduledStage] = []
		scheduled.reserveCapacity(plan.stages.count)
		var cursor = start
		var readyAt = start

		for (index, stage) in plan.stages.enumerated() {
			let end = cursor.addingTimeInterval(durations[index])
			let windowEnd = stage.usableWindowHours.map { end.addingTimeInterval($0 * 3600) }
			scheduled.append(
				ScheduledStage(
					id: stage.id,
					stage: stage,
					start: cursor,
					end: end,
					windowEnd: windowEnd,
					foldTimes: foldTimes(for: stage, start: cursor, end: end)
				)
			)
			if stage.kind != .bake { readyAt = end }
			cursor = end
		}

		return Schedule(
			stages: scheduled,
			start: start,
			readyAt: readyAt,
			finishAt: cursor,
			fermentationLoadHours: plan.fermentationLoadHours
		)
	}
}
