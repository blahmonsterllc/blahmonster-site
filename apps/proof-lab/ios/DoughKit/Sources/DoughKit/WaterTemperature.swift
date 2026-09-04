import Foundation

public enum MixerKind: String, CaseIterable, Codable, Sendable {
	case hand
	case fork
	case doubleArm
	case divingArm
	case spiral
	case planetary

	public var displayName: String {
		switch self {
		case .hand: "By hand"
		case .fork: "Fork mixer"
		case .doubleArm: "Double-arm"
		case .divingArm: "Diving-arm"
		case .spiral: "Spiral"
		case .planetary: "Planetary"
		}
	}

	/// Degrees the mixer adds to the dough over a normal mix.
	public var frictionC: Double {
		switch self {
		case .hand: 1
		case .fork: 2
		case .doubleArm: 2
		case .divingArm: 3
		case .spiral: 4
		case .planetary: 6
		}
	}
}

public struct WaterTemperatureResult: Sendable, Equatable {
	/// The water temperature the factor method asks for.
	public var waterTemperatureC: Double
	/// True when a preferment was included, i.e. the four-factor method was used.
	public var usedPrefermentFactor: Bool
	/// Grams of ice to swap in, when tap water alone can't get cold enough.
	public var iceGrams: Double
	/// Grams of liquid tap water to pair with that ice.
	public var waterGrams: Double
	/// Set when the required temperature is unreachable — chill the flour instead.
	public var warning: String?
}

public enum DoughTemperature {
	/// Classic factor method. Three factors for a straight dough, four with a preferment.
	public static func waterTemperature(
		desiredDoughTempC: Double,
		flourTempC: Double,
		roomTempC: Double,
		prefermentTempC: Double?,
		frictionC: Double
	) -> Double {
		let factors = prefermentTempC == nil ? 3.0 : 4.0
		let known = flourTempC + roomTempC + (prefermentTempC ?? 0) + frictionC
		return desiredDoughTempC * factors - known
	}

	/// Ice needed to bring `waterGrams` of tap water down to `targetC`.
	///
	/// Heat balance with the latent heat of fusion (~80 cal/g):
	/// `(W − I)(tap − target) = I(80 + target)`.
	public static func iceSplit(
		waterGrams: Double,
		tapTempC: Double,
		targetC: Double
	) -> (ice: Double, water: Double) {
		guard waterGrams > 0, targetC < tapTempC else { return (0, max(0, waterGrams)) }
		let denominator = 80 + tapTempC
		guard denominator > 0 else { return (0, waterGrams) }
		let ice = waterGrams * (tapTempC - targetC) / denominator
		let clampedIce = min(max(ice, 0), waterGrams)
		return (clampedIce, waterGrams - clampedIce)
	}

	public static func solve(
		desiredDoughTempC: Double,
		flourTempC: Double,
		roomTempC: Double,
		prefermentTempC: Double?,
		mixer: MixerKind,
		totalWaterGrams: Double,
		tapWaterTempC: Double
	) -> WaterTemperatureResult {
		let target = waterTemperature(
			desiredDoughTempC: desiredDoughTempC,
			flourTempC: flourTempC,
			roomTempC: roomTempC,
			prefermentTempC: prefermentTempC,
			frictionC: mixer.frictionC
		)

		var warning: String?
		if target < 0 {
			warning = "Below 0 °C — ice alone won't get there. Chill the flour or cool the room."
		} else if target > 40 {
			warning = "Above 40 °C would shock the yeast. Warm the flour instead."
		}

		let split = iceSplit(
			waterGrams: totalWaterGrams,
			tapTempC: tapWaterTempC,
			targetC: max(0, target)
		)

		return WaterTemperatureResult(
			waterTemperatureC: target,
			usedPrefermentFactor: prefermentTempC != nil,
			iceGrams: split.ice,
			waterGrams: split.water,
			warning: warning
		)
	}
}
