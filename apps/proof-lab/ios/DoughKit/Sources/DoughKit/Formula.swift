import Foundation

public enum PrefermentKind: String, CaseIterable, Codable, Sendable {
	case none
	case poolish
	case biga
	case levain
	case oldDough

	public var displayName: String {
		switch self {
		case .none: "None (straight dough)"
		case .poolish: "Poolish"
		case .biga: "Biga"
		case .levain: "Levain"
		case .oldDough: "Old dough (pâte fermentée)"
		}
	}

	public var defaultHydrationPercent: Double {
		switch self {
		case .none: 0
		case .poolish: 100
		case .biga: 50
		case .levain: 100
		case .oldDough: 62
		}
	}

	/// Levain is fed from a starter; the others are seeded with commercial yeast.
	public var usesStarterSeed: Bool { self == .levain }
}

/// A dough formula in baker's percentages. Percentages are of **total flour**, which
/// includes any flour locked up in a preferment.
public struct DoughFormula: Codable, Sendable, Equatable {
	public var name: String
	public var ballCount: Int
	public var ballWeightGrams: Double
	/// Scrap / trim allowance so the last ball isn't short.
	public var lossPercent: Double

	public var hydrationPercent: Double
	public var saltPercent: Double
	public var oilPercent: Double
	public var sugarPercent: Double
	public var maltPercent: Double

	public var leaven: LeavenKind
	public var yeastType: YeastType
	/// Always stored on an instant-dry basis; the scooped weight applies `yeastType.multiplier`.
	public var instantYeastPercent: Double

	public var prefermentKind: PrefermentKind
	/// Share of the *total flour* that gets prefermented.
	public var prefermentedFlourPercent: Double
	public var prefermentHydrationPercent: Double
	/// Instant-dry yeast in the preferment, as a percent of the preferment's flour.
	public var prefermentYeastPercent: Double
	/// Mature starter carried into a levain build, as a percent of the levain's flour.
	public var starterSeedPercent: Double
	/// The flour blend. A single entry is the ordinary case; more is where prototyping starts.
	public var flours: [FlourComponent]

	public var flourNote: String

	public init(
		name: String = "Untitled dough",
		ballCount: Int = 20,
		ballWeightGrams: Double = 280,
		lossPercent: Double = 2,
		hydrationPercent: Double = 62,
		saltPercent: Double = 2.5,
		oilPercent: Double = 0,
		sugarPercent: Double = 0,
		maltPercent: Double = 0,
		leaven: LeavenKind = .commercialYeast,
		yeastType: YeastType = .instantDry,
		instantYeastPercent: Double = 0.2,
		prefermentKind: PrefermentKind = .none,
		prefermentedFlourPercent: Double = 0,
		prefermentHydrationPercent: Double = 100,
		prefermentYeastPercent: Double = 0.1,
		starterSeedPercent: Double = 20,
		flours: [FlourComponent] = FlourLibrary.defaultBlend,
		flourNote: String = ""
	) {
		self.name = name
		self.ballCount = ballCount
		self.ballWeightGrams = ballWeightGrams
		self.lossPercent = lossPercent
		self.hydrationPercent = hydrationPercent
		self.saltPercent = saltPercent
		self.oilPercent = oilPercent
		self.sugarPercent = sugarPercent
		self.maltPercent = maltPercent
		self.leaven = leaven
		self.yeastType = yeastType
		self.instantYeastPercent = instantYeastPercent
		self.prefermentKind = prefermentKind
		self.prefermentedFlourPercent = prefermentedFlourPercent
		self.prefermentHydrationPercent = prefermentHydrationPercent
		self.prefermentYeastPercent = prefermentYeastPercent
		self.starterSeedPercent = starterSeedPercent
		self.flours = flours
		self.flourNote = flourNote
	}

	public var blend: FlourBlend { FlourBlend(flours) }
}

public struct Ingredient: Sendable, Equatable, Identifiable {
	public let id: String
	public let name: String
	public let grams: Double
	/// Percent of total flour. Zero for rows where a percentage is meaningless.
	public let bakersPercent: Double

	public init(id: String, name: String, grams: Double, bakersPercent: Double) {
		self.id = id
		self.name = name
		self.grams = grams
		self.bakersPercent = bakersPercent
	}
}

public struct FormulaResult: Sendable, Equatable {
	public var totalDoughGrams: Double
	public var totalFlourGrams: Double
	public var totalWaterGrams: Double
	public var ballCount: Int
	public var ballWeightGrams: Double
	/// Water only, ignoring oil and sugar — what actually governs handling.
	public var trueHydrationPercent: Double

	/// The whole formula, preferment folded in.
	public var overall: [Ingredient]
	/// What to build ahead of time. Empty for a straight dough.
	public var prefermentBuild: [Ingredient]
	/// What goes in the mixer on the day.
	public var finalMix: [Ingredient]

	public var prefermentFlourGrams: Double
	public var prefermentTotalGrams: Double
}

public extension DoughFormula {
	var prefermentedFlourFraction: Double {
		guard prefermentKind != .none else { return 0 }
		return min(max(prefermentedFlourPercent / 100, 0), 1)
	}

	/// The percentage you actually weigh out, once the yeast form is accounted for.
	var scoopedYeastPercent: Double {
		leaven == .sourdough ? 0 : instantYeastPercent * yeastType.multiplier
	}

	func result() -> FormulaResult {
		let safeBalls = max(0, ballCount)
		let targetDough = Double(safeBalls) * max(0, ballWeightGrams) * (1 + max(0, lossPercent) / 100)

		let yeastPercent = scoopedYeastPercent
		let prefFlourFraction = prefermentedFlourFraction
		let prefermentYeastOfTotalFlour = prefermentKind.usesStarterSeed || prefermentKind == .none
			? 0
			: prefFlourFraction * prefermentYeastPercent * yeastType.multiplier

		let sumPercent = 100
			+ hydrationPercent
			+ saltPercent
			+ oilPercent
			+ sugarPercent
			+ maltPercent
			+ yeastPercent
			+ prefermentYeastOfTotalFlour

		let totalFlour = sumPercent > 0 ? targetDough / (sumPercent / 100) : 0
		func weight(_ percent: Double) -> Double { totalFlour * percent / 100 }

		let totalWater = weight(hydrationPercent)
		let salt = weight(saltPercent)
		let oil = weight(oilPercent)
		let sugar = weight(sugarPercent)
		let malt = weight(maltPercent)
		let yeast = weight(yeastPercent)

		let prefermentFlour = totalFlour * prefFlourFraction
		let prefermentWater = prefermentFlour * prefermentHydrationPercent / 100
		let prefermentYeast = prefermentKind.usesStarterSeed
			? 0
			: prefermentFlour * prefermentYeastPercent * yeastType.multiplier / 100
		let prefermentTotal = prefermentFlour + prefermentWater + prefermentYeast

		let components = blend.normalized

		/// One row per flour, or a single row when the blend is just the one. A preferment is
		/// assumed to take the same blend as the dough, scaled down.
		func flourRows(scale: Double, label: String, idPrefix: String) -> [Ingredient] {
			guard components.count > 1 else {
				return [
					Ingredient(id: idPrefix, name: label, grams: totalFlour * scale, bakersPercent: 100 * scale)
				]
			}
			return components.map { component in
				Ingredient(
					id: "\(idPrefix)-\(component.id)",
					name: "\(label) — \(component.shortName)",
					grams: totalFlour * scale * component.percent / 100,
					bakersPercent: component.percent * scale
				)
			}
		}

		var overall: [Ingredient] = flourRows(scale: 1, label: "Flour (total)", idPrefix: "flour")
		overall.append(
			Ingredient(id: "water", name: "Water (total)", grams: totalWater, bakersPercent: hydrationPercent)
		)
		overall.append(Ingredient(id: "salt", name: "Salt", grams: salt, bakersPercent: saltPercent))
		if leaven == .commercialYeast, yeast > 0 {
			overall.append(
				Ingredient(
					id: "yeast",
					name: "Yeast — \(yeastType.shortName)",
					grams: yeast + prefermentYeast,
					bakersPercent: yeastPercent + prefermentYeastOfTotalFlour
				)
			)
		}
		if oil > 0 {
			overall.append(Ingredient(id: "oil", name: "Oil", grams: oil, bakersPercent: oilPercent))
		}
		if sugar > 0 {
			overall.append(Ingredient(id: "sugar", name: "Sugar", grams: sugar, bakersPercent: sugarPercent))
		}
		if malt > 0 {
			overall.append(Ingredient(id: "malt", name: "Diastatic malt", grams: malt, bakersPercent: maltPercent))
		}

		var build: [Ingredient] = []
		if prefermentKind != .none, prefermentFlour > 0 {
			if prefermentKind.usesStarterSeed {
				let seedFlour = prefermentFlour * max(0, starterSeedPercent) / 100
				let seed = seedFlour * (1 + prefermentHydrationPercent / 100)
				let feedFlour = prefermentFlour - seedFlour
				let feedWater = feedFlour * prefermentHydrationPercent / 100
				let feedScale = totalFlour > 0 ? feedFlour / totalFlour : 0
				build = [Ingredient(id: "seed", name: "Ripe starter", grams: seed, bakersPercent: 0)]
				build += flourRows(scale: feedScale, label: "Flour", idPrefix: "pfFlour").map {
					Ingredient(id: $0.id, name: $0.name, grams: $0.grams, bakersPercent: 0)
				}
				build.append(Ingredient(id: "pfWater", name: "Water", grams: feedWater, bakersPercent: 0))
			} else {
				build = flourRows(scale: prefFlourFraction, label: "Flour", idPrefix: "pfFlour").map {
					Ingredient(id: $0.id, name: $0.name, grams: $0.grams, bakersPercent: 0)
				}
				build.append(Ingredient(id: "pfWater", name: "Water", grams: prefermentWater, bakersPercent: 0))
				build.append(
					Ingredient(
						id: "pfYeast",
						name: "Yeast — \(yeastType.shortName)",
						grams: prefermentYeast,
						bakersPercent: 0
					)
				)
			}
		}

		var final: [Ingredient] = flourRows(
			scale: 1 - prefFlourFraction,
			label: "Flour",
			idPrefix: "flour"
		)
		final.append(
			Ingredient(
				id: "water",
				name: "Water",
				grams: totalWater - prefermentWater,
				bakersPercent: hydrationPercent - (prefermentWater / max(totalFlour, 1)) * 100
			)
		)
		if prefermentTotal > 0 {
			final.append(
				Ingredient(
					id: "preferment",
					name: "\(prefermentKind.displayName) (ripe)",
					grams: prefermentTotal,
					bakersPercent: prefermentTotal / max(totalFlour, 1) * 100
				)
			)
		}
		final.append(Ingredient(id: "salt", name: "Salt", grams: salt, bakersPercent: saltPercent))
		if leaven == .commercialYeast, yeast > 0 {
			final.append(
				Ingredient(id: "yeast", name: "Yeast — \(yeastType.shortName)", grams: yeast, bakersPercent: yeastPercent)
			)
		}
		if oil > 0 { final.append(Ingredient(id: "oil", name: "Oil", grams: oil, bakersPercent: oilPercent)) }
		if sugar > 0 { final.append(Ingredient(id: "sugar", name: "Sugar", grams: sugar, bakersPercent: sugarPercent)) }
		if malt > 0 {
			final.append(Ingredient(id: "malt", name: "Diastatic malt", grams: malt, bakersPercent: maltPercent))
		}

		return FormulaResult(
			totalDoughGrams: targetDough,
			totalFlourGrams: totalFlour,
			totalWaterGrams: totalWater,
			ballCount: safeBalls,
			ballWeightGrams: ballWeightGrams,
			trueHydrationPercent: hydrationPercent,
			overall: overall,
			prefermentBuild: build,
			finalMix: final,
			prefermentFlourGrams: prefermentFlour,
			prefermentTotalGrams: prefermentTotal
		)
	}
}

// MARK: - Production scaling

public struct ProductionPlan: Sendable, Equatable {
	public struct Mix: Sendable, Equatable, Identifiable {
		public let id: Int
		public let ballCount: Int
		public let doughGrams: Double
		public let ingredients: [Ingredient]
	}

	public var mixerCapacityKg: Double
	public var mixes: [Mix]
	public var totalDoughGrams: Double

	public var mixCount: Int { mixes.count }
}

public extension FormulaResult {
	/// Splits a production run across mixer loads. Dough is divided evenly; leftover balls
	/// are spread one per mix so no load is short by more than a single ball.
	func productionPlan(mixerCapacityKg: Double) -> ProductionPlan {
		let capacity = max(0.1, mixerCapacityKg) * 1000
		let mixCount = max(1, Int(ceil(totalDoughGrams / capacity)))
		let baseBalls = ballCount / mixCount
		let remainder = ballCount % mixCount

		var mixes: [ProductionPlan.Mix] = []
		mixes.reserveCapacity(mixCount)
		for index in 0..<mixCount {
			let balls = baseBalls + (index < remainder ? 1 : 0)
			let share = ballCount > 0 ? Double(balls) / Double(ballCount) : 1 / Double(mixCount)
			let scaled = finalMix.map {
				Ingredient(id: $0.id, name: $0.name, grams: $0.grams * share, bakersPercent: $0.bakersPercent)
			}
			mixes.append(
				ProductionPlan.Mix(
					id: index + 1,
					ballCount: balls,
					doughGrams: totalDoughGrams * share,
					ingredients: scaled
				)
			)
		}

		return ProductionPlan(
			mixerCapacityKg: mixerCapacityKg,
			mixes: mixes,
			totalDoughGrams: totalDoughGrams
		)
	}
}
