import Foundation

/// One flour in a blend.
///
/// Blends are the whole point of prototyping: 00 with a little semola handles differently from
/// straight 00, and 15 % whole wheat ferments faster than the protein alone would suggest.
/// Percentages are shares of the *total flour*, so they should add to 100 — `FlourBlend`
/// normalises them rather than trusting the arithmetic on screen.
public struct FlourComponent: Codable, Sendable, Equatable, Identifiable {
	public var id: String
	public var name: String
	public var percent: Double
	public var proteinPercent: Double
	/// Hydration points this flour would add if the blend were made entirely of it.
	public var absorptionOffset: Double
	public var isWholeGrain: Bool

	public init(
		id: String,
		name: String,
		percent: Double,
		proteinPercent: Double,
		absorptionOffset: Double = 0,
		isWholeGrain: Bool = false
	) {
		self.id = id
		self.name = name
		self.percent = percent
		self.proteinPercent = proteinPercent
		self.absorptionOffset = absorptionOffset
		self.isWholeGrain = isWholeGrain
	}

	public var shortName: String {
		String(name.prefix(while: { $0 != "(" })).trimmingCharacters(in: .whitespaces)
	}
}

/// The shelf you pick a blend from. Protein figures are typical, not a guarantee.
public enum FlourLibrary {
	private static func entry(
		_ id: String,
		_ name: String,
		protein: Double,
		absorption: Double = 0,
		wholeGrain: Bool = false
	) -> FlourComponent {
		FlourComponent(
			id: id,
			name: name,
			percent: 0,
			proteinPercent: protein,
			absorptionOffset: absorption,
			isWholeGrain: wholeGrain
		)
	}

	public static let all: [FlourComponent] = [
		entry("00-pizzeria", "Type 00 pizzeria", protein: 12.5),
		entry("00-strong", "Type 00 strong (W 330+)", protein: 13.5, absorption: 2),
		entry("nuvola", "Type 00 Nuvola", protein: 12.5, absorption: 1),
		entry("bread", "Bread flour", protein: 12.7, absorption: 1),
		entry("high-gluten", "High-gluten", protein: 14, absorption: 2.5),
		entry("all-purpose", "All-purpose", protein: 10.5, absorption: -1),
		entry("t65", "T65", protein: 11.5),
		entry("semola", "Semola rimacinata", protein: 12.5, absorption: -2),
		entry("durum", "Whole durum", protein: 13, absorption: 4, wholeGrain: true),
		entry("whole-wheat", "Whole wheat", protein: 13.5, absorption: 8, wholeGrain: true),
		entry("spelt", "Spelt", protein: 12, absorption: 2),
		entry("rye", "Rye", protein: 10, absorption: 10, wholeGrain: true),
		entry("einkorn", "Einkorn", protein: 12, absorption: -3, wholeGrain: true),
		entry("kamut", "Kamut", protein: 13, absorption: 3, wholeGrain: true)
	]

	public static func spec(id: String) -> FlourComponent? {
		all.first { $0.id == id }
	}

	/// A named flour at a given share of the blend.
	public static func at(_ id: String, _ percent: Double) -> FlourComponent {
		var component = spec(id: id) ?? all[0]
		component.percent = percent
		return component
	}

	public static let defaultBlend: [FlourComponent] = [at("bread", 100)]
}

/// Weighted properties of a blend.
public struct FlourBlend: Sendable, Equatable {
	public var components: [FlourComponent]

	public init(_ components: [FlourComponent]) {
		self.components = components
	}

	/// Shares rescaled to add to exactly 100, so a blend that says 97 % still weighs out right.
	public var normalized: [FlourComponent] {
		guard !components.isEmpty else { return FlourLibrary.defaultBlend }
		let total = components.reduce(0) { $0 + max(0, $1.percent) }
		guard total > 0 else {
			let even = 100 / Double(components.count)
			return components.map { var copy = $0; copy.percent = even; return copy }
		}
		return components.map { component in
			var copy = component
			copy.percent = max(0, component.percent) / total * 100
			return copy
		}
	}

	public var proteinPercent: Double {
		normalized.reduce(0) { $0 + $1.proteinPercent * $1.percent / 100 }
	}

	public var wholeGrainFraction: Double {
		normalized.filter(\.isWholeGrain).reduce(0) { $0 + $1.percent } / 100
	}

	/// Roughly how much water this blend carries comfortably. It's a floor, not a target —
	/// style takes you above it, and a Roman teglia goes far above it on purpose.
	public var absorptionGuidePercent: Double {
		50
			+ (proteinPercent - 10) * 2.2
			+ normalized.reduce(0) { $0 + $1.absorptionOffset * $1.percent / 100 }
	}

	public var summary: String {
		normalized
			.filter { $0.percent >= 0.5 }
			.sorted { $0.percent > $1.percent }
			.map { "\(Formatting.rounded($0.percent, places: 0)) % \($0.shortName)" }
			.joined(separator: " · ")
	}
}
