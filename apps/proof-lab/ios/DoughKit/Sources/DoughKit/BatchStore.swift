import Foundation

/// Flat JSON on disk. Batches are small and few; a database would be ceremony.
public struct BatchStore: Sendable {
	public let url: URL

	public init(url: URL) {
		self.url = url
	}

	public static func defaultURL(fileManager: FileManager = .default) throws -> URL {
		let base = try fileManager.url(
			for: .applicationSupportDirectory,
			in: .userDomainMask,
			appropriateFor: nil,
			create: true
		)
		let directory = base.appendingPathComponent("ProofLab", isDirectory: true)
		try fileManager.createDirectory(at: directory, withIntermediateDirectories: true)
		return directory.appendingPathComponent("batches.json")
	}

	private static var encoder: JSONEncoder {
		let encoder = JSONEncoder()
		encoder.dateEncodingStrategy = .iso8601
		encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
		return encoder
	}

	private static var decoder: JSONDecoder {
		let decoder = JSONDecoder()
		decoder.dateDecodingStrategy = .iso8601
		return decoder
	}

	public func load() -> [Batch] {
		guard let data = try? Data(contentsOf: url) else { return [] }
		return (try? Self.decoder.decode([Batch].self, from: data)) ?? []
	}

	public func save(_ batches: [Batch]) throws {
		let data = try Self.encoder.encode(batches)
		try data.write(to: url, options: .atomic)
	}
}

// MARK: - Prototyping

/// Side-by-side diff of two runs. The prototyping loop is "change one thing, bake, compare",
/// and this is the compare half.
public struct BatchComparison: Sendable {
	public struct Row: Sendable, Equatable, Identifiable {
		public let id: String
		public let label: String
		public let left: String
		public let right: String
		public let changed: Bool
	}

	public let left: Batch
	public let right: Batch
	public let rows: [Row]

	public init(left: Batch, right: Batch) {
		self.left = left
		self.right = right

		func row(_ id: String, _ label: String, _ a: String, _ b: String) -> Row {
			Row(id: id, label: label, left: a, right: b, changed: a != b)
		}

		let leftResult = left.formula.result()
		let rightResult = right.formula.result()

		var rows: [Row] = [
			row("plan", "Plan", left.plan.name, right.plan.name),
			row(
				"load",
				"Ferment load",
				Formatting.hours(left.fermentationLoadHours),
				Formatting.hours(right.fermentationLoadHours)
			),
			row(
				"hydration",
				"Hydration",
				Formatting.percent(left.formula.hydrationPercent, places: 1),
				Formatting.percent(right.formula.hydrationPercent, places: 1)
			),
			row(
				"salt",
				"Salt",
				Formatting.percent(left.formula.saltPercent, places: 2),
				Formatting.percent(right.formula.saltPercent, places: 2)
			),
			row(
				"leaven",
				"Leaven",
				left.formula.leaven == .sourdough
					? "Levain \(Formatting.percent(left.formula.prefermentedFlourPercent, places: 1)) flour"
					: "\(left.formula.yeastType.shortName) \(Formatting.percent(left.formula.instantYeastPercent, places: 3))",
				right.formula.leaven == .sourdough
					? "Levain \(Formatting.percent(right.formula.prefermentedFlourPercent, places: 1)) flour"
					: "\(right.formula.yeastType.shortName) \(Formatting.percent(right.formula.instantYeastPercent, places: 3))"
			),
			row(
				"preferment",
				"Preferment",
				left.formula.prefermentKind.displayName,
				right.formula.prefermentKind.displayName
			),
			row(
				"ball",
				"Ball weight",
				Formatting.grams(left.formula.ballWeightGrams),
				Formatting.grams(right.formula.ballWeightGrams)
			),
			row(
				"flour",
				"Total flour",
				Formatting.grams(leftResult.totalFlourGrams),
				Formatting.grams(rightResult.totalFlourGrams)
			),
			row("oil", "Oil", Formatting.percent(left.formula.oilPercent, places: 1), Formatting.percent(right.formula.oilPercent, places: 1)),
			row("sugar", "Sugar", Formatting.percent(left.formula.sugarPercent, places: 1), Formatting.percent(right.formula.sugarPercent, places: 1))
		]

		if let a = left.review, let b = right.review {
			for (axisA, axisB) in zip(a.axes, b.axes) {
				rows.append(row("score-\(axisA.id)", axisA.name, "\(axisA.score)/5", "\(axisB.score)/5"))
			}
		}

		self.rows = rows
	}

	public var changedRows: [Row] { rows.filter(\.changed) }
}
