import Foundation

/// The temperature model everything else hangs off.
///
/// A single Q10 constant badly underestimates how much a fridge slows a dough, so the
/// curve is split into three segments (see `SPEC.md`). Real durations get converted into
/// *equivalent hours at 24 °C* — the common currency that makes a 48 hour retard and a
/// 7 hour room-temperature bulk comparable, and that drives every yeast suggestion.
public enum Fermentation {
	/// Every rate is quoted relative to this.
	public static let referenceTemperatureC: Double = 24

	public static let minTemperatureC: Double = -5
	public static let maxTemperatureC: Double = 45

	private struct Segment: Sendable {
		let upper: Double
		let q10: Double
	}

	private static let segments: [Segment] = [
		Segment(upper: 10, q10: 3.0),
		Segment(upper: 20, q10: 2.5),
		Segment(upper: .infinity, q10: 2.0)
	]

	private static func logActivity(_ raw: Double) -> Double {
		let temperature = min(max(raw, minTemperatureC), maxTemperatureC)
		guard temperature > 0 else {
			// Below freezing there is no useful data; extrapolate the coldest segment.
			return temperature * log(segments[0].q10) / 10
		}
		var total = 0.0
		var lower = 0.0
		for segment in segments {
			guard temperature > lower else { break }
			total += (min(temperature, segment.upper) - lower) * log(segment.q10) / 10
			lower = segment.upper
		}
		return total
	}

	/// How fast dough ferments at `temperatureC`, as a multiple of the 24 °C rate.
	public static func rateMultiplier(
		atC temperatureC: Double,
		referenceC: Double = referenceTemperatureC
	) -> Double {
		exp(logActivity(temperatureC) - logActivity(referenceC))
	}

	/// Converts a real duration into hours-at-24 °C.
	public static func equivalentHours(hours: Double, atC temperatureC: Double) -> Double {
		max(0, hours) * rateMultiplier(atC: temperatureC)
	}

	/// The inverse: how long you must wait at `temperatureC` for the same fermentation.
	public static func hours(forEquivalentHours equivalent: Double, atC temperatureC: Double) -> Double {
		let rate = rateMultiplier(atC: temperatureC)
		guard rate > 0 else { return .infinity }
		return max(0, equivalent) / rate
	}
}

// MARK: - Leavening

public enum YeastType: String, CaseIterable, Codable, Sendable {
	case instantDry
	case activeDry
	case freshCake

	public var displayName: String {
		switch self {
		case .instantDry: "Instant dry (IDY)"
		case .activeDry: "Active dry (ADY)"
		case .freshCake: "Fresh / cake"
		}
	}

	public var shortName: String {
		switch self {
		case .instantDry: "IDY"
		case .activeDry: "ADY"
		case .freshCake: "Fresh"
		}
	}

	/// Weight relative to instant dry yeast for the same leavening power.
	public var multiplier: Double {
		switch self {
		case .instantDry: 1.0
		case .activeDry: 1.25
		case .freshCake: 3.0
		}
	}
}

public enum LeavenKind: String, CaseIterable, Codable, Sendable {
	case commercialYeast
	case sourdough

	public var displayName: String {
		switch self {
		case .commercialYeast: "Commercial yeast"
		case .sourdough: "Sourdough"
		}
	}
}

public enum Leavening {
	public static let minInstantYeastPercent: Double = 0.02
	public static let maxInstantYeastPercent: Double = 1.5
	public static let minLevainPercent: Double = 3
	public static let maxLevainPercent: Double = 40

	/// Bran carries enzymes and wild yeast, so whole-grain doughs run faster than their protein
	/// suggests. A wholly whole-grain dough wants roughly 30 % less leaven than a white one on
	/// the same schedule.
	public static let wholeGrainSpeedup: Double = 0.3

	/// Instant dry yeast as a percentage of total flour for a given fermentation load.
	///
	/// Calibrated against the cold-fermentation tables American pizzerias work from —
	/// 0.42 / 0.21 / 0.14 % for 24 / 48 / 72 hours at 4 °C. Treat it as a starting point:
	/// real doses vary more than fivefold between traditions for the same load (an AVPN
	/// Neapolitan runs ~0.06 %), so the app shows this beside your own number instead of
	/// overwriting it. What transfers between traditions is the *ratio* — double the cold
	/// time, halve the yeast.
	public static func instantYeastPercent(
		equivalentHours: Double,
		saltPercent: Double = 2,
		sugarPercent: Double = 0,
		prefermentedFlourFraction: Double = 0,
		wholeGrainFraction: Double = 0
	) -> Double {
		guard equivalentHours > 0 else { return maxInstantYeastPercent }
		var percent = 1.6 / equivalentHours
		percent *= 1 + 0.14 * max(0, saltPercent - 2)
		percent *= 1 + 0.03 * max(0, sugarPercent - 5)
		percent *= 1 - 0.8 * min(max(prefermentedFlourFraction, 0), 1)
		percent *= 1 - wholeGrainSpeedup * min(max(wholeGrainFraction, 0), 1)
		return min(max(percent, minInstantYeastPercent), maxInstantYeastPercent)
	}

	/// Ripe levain as a percentage of total flour (levain weight, not the flour in it).
	public static func levainPercent(
		equivalentHours: Double,
		saltPercent: Double = 2,
		wholeGrainFraction: Double = 0
	) -> Double {
		guard equivalentHours > 0 else { return maxLevainPercent }
		var percent = 90 / equivalentHours
		percent *= 1 + 0.14 * max(0, saltPercent - 2)
		percent *= 1 - wholeGrainSpeedup * min(max(wholeGrainFraction, 0), 1)
		return min(max(percent, minLevainPercent), maxLevainPercent)
	}

	/// Rough inverse of `instantYeastPercent` — "at this dose, how long does it want?".
	public static func equivalentHours(forInstantYeastPercent percent: Double) -> Double {
		guard percent > 0 else { return .infinity }
		return 1.6 / percent
	}
}
