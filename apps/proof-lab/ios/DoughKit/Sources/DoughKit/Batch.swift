import Foundation

public struct StageProgress: Codable, Sendable, Equatable {
	/// Hours added or removed at the bench — "it's not ready, give it another 30".
	public var adjustmentHours: Double
	public var completedAt: Date?
	/// Set when you've seen the alert, which is what clears the badge.
	public var acknowledgedAt: Date?

	public init(adjustmentHours: Double = 0, completedAt: Date? = nil, acknowledgedAt: Date? = nil) {
		self.adjustmentHours = adjustmentHours
		self.completedAt = completedAt
		self.acknowledgedAt = acknowledgedAt
	}
}

/// Tasting notes for a prototype run. The point of the log is being able to tell why
/// batch 14 was better than batch 13.
public struct BatchReview: Codable, Sendable, Equatable {
	public var handling: Int
	public var extensibility: Int
	public var ovenSpring: Int
	public var crumb: Int
	public var flavor: Int
	public var crust: Int
	public var overall: Int
	public var wouldRepeat: Bool
	public var notes: String

	public init(
		handling: Int = 3,
		extensibility: Int = 3,
		ovenSpring: Int = 3,
		crumb: Int = 3,
		flavor: Int = 3,
		crust: Int = 3,
		overall: Int = 3,
		wouldRepeat: Bool = true,
		notes: String = ""
	) {
		self.handling = handling
		self.extensibility = extensibility
		self.ovenSpring = ovenSpring
		self.crumb = crumb
		self.flavor = flavor
		self.crust = crust
		self.overall = overall
		self.wouldRepeat = wouldRepeat
		self.notes = notes
	}

	public struct Axis: Sendable, Equatable, Identifiable {
		public let id: String
		public let name: String
		public let score: Int
	}

	public var axes: [Axis] {
		[
			Axis(id: "handling", name: "Handling", score: handling),
			Axis(id: "extensibility", name: "Extensibility", score: extensibility),
			Axis(id: "ovenSpring", name: "Oven spring", score: ovenSpring),
			Axis(id: "crumb", name: "Crumb", score: crumb),
			Axis(id: "flavor", name: "Flavour", score: flavor),
			Axis(id: "crust", name: "Crust", score: crust),
			Axis(id: "overall", name: "Overall", score: overall)
		]
	}
}

public enum StageStatus: String, Sendable {
	case upcoming
	case active
	case due
	case overdue
	case done
}

public struct Batch: Codable, Sendable, Equatable, Identifiable {
	public var id: UUID
	public var name: String
	public var createdAt: Date
	/// When the mix actually started. Everything downstream is derived from this.
	public var startAt: Date
	public var formula: DoughFormula
	public var plan: FermentationPlan
	public var mixerCapacityKg: Double
	public var progress: [String: StageProgress]
	public var review: BatchReview?
	public var notes: String
	public var tags: [String]
	public var isArchived: Bool

	public init(
		id: UUID = UUID(),
		name: String,
		createdAt: Date = Date(),
		startAt: Date,
		formula: DoughFormula,
		plan: FermentationPlan,
		mixerCapacityKg: Double = 20,
		progress: [String: StageProgress] = [:],
		review: BatchReview? = nil,
		notes: String = "",
		tags: [String] = [],
		isArchived: Bool = false
	) {
		self.id = id
		self.name = name
		self.createdAt = createdAt
		self.startAt = startAt
		self.formula = formula
		self.plan = plan
		self.mixerCapacityKg = mixerCapacityKg
		self.progress = progress
		self.review = review
		self.notes = notes
		self.tags = tags
		self.isArchived = isArchived
	}
}

public extension Batch {
	/// Absolute stage times, honouring adjustments and manual completions. Completing a
	/// stage early or late drags everything after it along.
	var timeline: [ScheduledStage] {
		var stages: [ScheduledStage] = []
		stages.reserveCapacity(plan.stages.count)
		var cursor = startAt

		for stage in plan.stages {
			let state = progress[stage.id]
			let planned = max(0, stage.hours + (state?.adjustmentHours ?? 0)) * 3600
			let end = state?.completedAt ?? cursor.addingTimeInterval(planned)
			let windowEnd = stage.usableWindowHours.map { end.addingTimeInterval($0 * 3600) }
			stages.append(
				ScheduledStage(
					id: stage.id,
					stage: stage,
					start: cursor,
					end: end,
					windowEnd: windowEnd,
					foldTimes: Scheduler.foldTimes(for: stage, start: cursor, end: end)
				)
			)
			cursor = end
		}
		return stages
	}

	var readyAt: Date {
		let stages = timeline
		guard let bakeIndex = stages.firstIndex(where: { $0.stage.kind == .bake }) else {
			return stages.last?.end ?? startAt
		}
		return bakeIndex > 0 ? stages[bakeIndex - 1].end : startAt
	}

	var fermentationLoadHours: Double { plan.fermentationLoadHours }

	/// Yeast the current schedule asks for, on an instant-dry basis.
	var suggestedInstantYeastPercent: Double {
		Leavening.instantYeastPercent(
			equivalentHours: fermentationLoadHours,
			saltPercent: formula.saltPercent,
			sugarPercent: formula.sugarPercent,
			prefermentedFlourFraction: formula.prefermentedFlourFraction
		)
	}

	var suggestedLevainPercent: Double {
		Leavening.levainPercent(
			equivalentHours: fermentationLoadHours,
			saltPercent: formula.saltPercent
		)
	}

	func status(of stage: ScheduledStage, at now: Date = Date()) -> StageStatus {
		if progress[stage.id]?.completedAt != nil { return .done }
		if now < stage.start { return .upcoming }
		if now < stage.end { return .active }
		if now >= stage.end.addingTimeInterval(30 * 60) { return .overdue }
		return .due
	}

	func currentStage(at now: Date = Date()) -> ScheduledStage? {
		let stages = timeline
		if let active = stages.first(where: { status(of: $0, at: now) == .active }) { return active }
		return stages.first { status(of: $0, at: now) != .done }
	}

	/// Stages whose timer has elapsed without being acknowledged. This is the badge count.
	func dueStages(at now: Date = Date()) -> [ScheduledStage] {
		timeline.filter { stage in
			guard stage.stage.alerts else { return false }
			let state = progress[stage.id]
			guard state?.completedAt == nil, state?.acknowledgedAt == nil else { return false }
			return now >= stage.end
		}
	}

	var isFinished: Bool {
		guard let last = plan.stages.last else { return true }
		return progress[last.id]?.completedAt != nil
	}
}

// MARK: - Alerts

public struct DoughAlert: Sendable, Equatable, Identifiable {
	public enum Kind: String, Sendable {
		case stageEnd
		case fold
		case windowOpen
		case windowClosing
		case prefermentReady
	}

	public let id: String
	public let batchID: UUID
	public let batchName: String
	public let stageID: String
	public let kind: Kind
	public let fireAt: Date
	public let title: String
	public let body: String
}

public extension Batch {
	/// Every alert this batch still owes you, in fire order.
	func upcomingAlerts(from now: Date = Date()) -> [DoughAlert] {
		var alerts: [DoughAlert] = []

		for stage in timeline {
			let state = progress[stage.id]
			guard state?.completedAt == nil else { continue }

			for (index, fold) in stage.foldTimes.enumerated() where fold > now {
				alerts.append(
					DoughAlert(
						id: "\(id.uuidString)|\(stage.id)|fold|\(index)",
						batchID: id,
						batchName: name,
						stageID: stage.id,
						kind: .fold,
						fireAt: fold,
						title: "Fold \(index + 1) — \(name)",
						body: "\(stage.stage.title): time for a set of folds."
					)
				)
			}

			guard stage.stage.alerts, stage.end > now else { continue }

			let kind: DoughAlert.Kind = stage.stage.kind == .preferment
				? .prefermentReady
				: (stage.stage.kind.isCold ? .windowOpen : .stageEnd)

			alerts.append(
				DoughAlert(
					id: "\(id.uuidString)|\(stage.id)|end",
					batchID: id,
					batchName: name,
					stageID: stage.id,
					kind: kind,
					fireAt: stage.end,
					title: "\(stage.stage.title) done — \(name)",
					body: Self.body(for: stage, kind: kind)
				)
			)

			if let windowEnd = stage.windowEnd, windowEnd > now {
				alerts.append(
					DoughAlert(
						id: "\(id.uuidString)|\(stage.id)|window",
						batchID: id,
						batchName: name,
						stageID: stage.id,
						kind: .windowClosing,
						fireAt: windowEnd,
						title: "Window closing — \(name)",
						body: "\(name) is at the end of its usable cold window. Use it or bin it."
					)
				)
			}
		}

		return alerts.sorted { $0.fireAt < $1.fireAt }
	}

	private static func body(for stage: ScheduledStage, kind: DoughAlert.Kind) -> String {
		switch kind {
		case .prefermentReady:
			"Preferment is ripe — domed and just starting to fall. Mix now."
		case .windowOpen:
			"Cold ferment is ready. Usable for another \(Formatting.hours(stage.stage.usableWindowHours ?? 0))."
		default:
			stage.stage.detail.isEmpty ? "Timer's up on \(stage.stage.title.lowercased())." : stage.stage.detail
		}
	}
}

/// Assigns cumulative badge numbers across every batch, so the icon badge is correct even
/// if the app never runs between alerts.
public enum AlertScheduler {
	/// iOS keeps at most 64 pending local notifications per app.
	public static let pendingLimit = 60

	public static func badgedAlerts(
		batches: [Batch],
		from now: Date = Date(),
		startingBadge: Int = 0,
		limit: Int = pendingLimit
	) -> [(alert: DoughAlert, badge: Int)] {
		let all = batches
			.filter { !$0.isArchived }
			.flatMap { $0.upcomingAlerts(from: now) }
			.sorted { $0.fireAt < $1.fireAt }
			.prefix(limit)

		var badge = max(0, startingBadge)
		return all.map { alert in
			// Fold reminders are nudges, not outstanding work; they don't raise the badge.
			if alert.kind != .fold { badge += 1 }
			return (alert, badge)
		}
	}

	/// Badge to show right now, counting only alerts that have already fired unacknowledged.
	public static func currentBadge(batches: [Batch], at now: Date = Date()) -> Int {
		batches.filter { !$0.isArchived }.reduce(0) { $0 + $1.dueStages(at: now).count }
	}
}
