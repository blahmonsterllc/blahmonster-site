import Foundation

public enum StageKind: String, CaseIterable, Codable, Sendable {
	case preferment
	case autolyse
	case mix
	case bulk
	case divide
	case bench
	case ball
	case shape
	case coldRetard
	case temper
	case finalProof
	case bake

	public var displayName: String {
		switch self {
		case .preferment: "Preferment"
		case .autolyse: "Autolyse"
		case .mix: "Mix"
		case .bulk: "Bulk ferment"
		case .divide: "Divide"
		case .bench: "Bench rest"
		case .ball: "Ball up"
		case .shape: "Shape"
		case .coldRetard: "Cold ferment"
		case .temper: "Temper"
		case .finalProof: "Final proof"
		case .bake: "Bake"
		}
	}

	/// SF Symbol used by the iOS app; the Android app maps the same cases to its own icons.
	public var symbolName: String {
		switch self {
		case .preferment: "flask"
		case .autolyse: "drop"
		case .mix: "tornado"
		case .bulk: "arrow.up.circle"
		case .divide: "scissors"
		case .bench: "pause.circle"
		case .ball: "circle.circle"
		case .shape: "hands.sparkles"
		case .coldRetard: "snowflake"
		case .temper: "thermometer.sun"
		case .finalProof: "timer"
		case .bake: "flame"
		}
	}

	/// Whether this stage's hours count toward the dough's fermentation load. A preferment
	/// ferments on its own schedule and is dosed separately, so it's excluded here.
	public var countsTowardFermentation: Bool {
		switch self {
		case .bulk, .bench, .ball, .coldRetard, .temper, .finalProof: true
		case .preferment, .autolyse, .mix, .divide, .shape, .bake: false
		}
	}

	public var isCold: Bool { self == .coldRetard }
}

public struct PlanStage: Codable, Sendable, Equatable, Identifiable {
	public var id: String
	public var kind: StageKind
	public var title: String
	public var detail: String
	public var hours: Double
	public var temperatureC: Double
	/// Fire a notification when this stage ends.
	public var alerts: Bool
	/// Repeating reminder inside the stage — stretch-and-folds, coil folds, dimpling.
	public var foldIntervalMinutes: Int?
	/// Cold stages: how long past "ready" the dough stays usable. This is the number a
	/// production kitchen actually schedules around.
	public var usableWindowHours: Double?

	public init(
		id: String,
		kind: StageKind,
		title: String,
		detail: String = "",
		hours: Double,
		temperatureC: Double,
		alerts: Bool = true,
		foldIntervalMinutes: Int? = nil,
		usableWindowHours: Double? = nil
	) {
		self.id = id
		self.kind = kind
		self.title = title
		self.detail = detail
		self.hours = hours
		self.temperatureC = temperatureC
		self.alerts = alerts
		self.foldIntervalMinutes = foldIntervalMinutes
		self.usableWindowHours = usableWindowHours
	}

	public var equivalentHours: Double {
		guard kind.countsTowardFermentation else { return 0 }
		return Fermentation.equivalentHours(hours: hours, atC: temperatureC)
	}
}

public enum PlanFamily: String, CaseIterable, Codable, Sendable {
	case pizza
	case bread

	public var displayName: String {
		switch self {
		case .pizza: "Pizza"
		case .bread: "Bread"
		}
	}
}

public struct FermentationPlan: Codable, Sendable, Equatable, Identifiable {
	public var id: String
	public var name: String
	public var family: PlanFamily
	public var leaven: LeavenKind
	public var summary: String
	public var stages: [PlanStage]
	/// Suggested preferment sizing when the plan opens with a preferment stage.
	public var prefermentKind: PrefermentKind
	public var prefermentedFlourPercent: Double
	public var prefermentHydrationPercent: Double

	public init(
		id: String,
		name: String,
		family: PlanFamily,
		leaven: LeavenKind,
		summary: String,
		prefermentKind: PrefermentKind = .none,
		prefermentedFlourPercent: Double = 0,
		prefermentHydrationPercent: Double = 100,
		stages: [PlanStage]
	) {
		self.id = id
		self.name = name
		self.family = family
		self.leaven = leaven
		self.summary = summary
		self.stages = stages
		self.prefermentKind = prefermentKind
		self.prefermentedFlourPercent = prefermentedFlourPercent
		self.prefermentHydrationPercent = prefermentHydrationPercent
	}

	/// Total fermentation expressed in hours at 24 °C.
	public var fermentationLoadHours: Double {
		stages.reduce(0) { $0 + $1.equivalentHours }
	}

	/// Wall-clock hours from mix to bake, preferment build included.
	public var totalHours: Double {
		stages.reduce(0) { $0 + max(0, $1.hours) }
	}

	public var prefermentStage: PlanStage? {
		stages.first { $0.kind == .preferment }
	}

	/// The dough is "ready" when the last stage before the bake ends.
	public var hoursToReady: Double {
		guard let bakeIndex = stages.firstIndex(where: { $0.kind == .bake }) else { return totalHours }
		return stages[..<bakeIndex].reduce(0) { $0 + max(0, $1.hours) }
	}

	public var coldStage: PlanStage? {
		stages.first { $0.kind.isCold }
	}
}
