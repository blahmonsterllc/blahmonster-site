import Foundation

/// The fermentation types the app ships with. Durations and temperatures are starting
/// points — every stage is editable per batch, and the yeast dose follows whatever you
/// change them to.
public extension FermentationPlan {
	static let library: [FermentationPlan] = [
		sameDayStraight,
		coldBallRetard,
		coldBulkRetard,
		neapolitanDirect,
		poolishPizza,
		bigaPizza,
		detroitPan,
		focacciaColdBulk,
		sourdoughPizzaCold,
		sourdoughCountryLoaf,
		poolishBaguette
	]

	static func plan(id: String) -> FermentationPlan? {
		library.first { $0.id == id }
	}

	// MARK: - Commercial yeast, pizza

	static let sameDayStraight = FermentationPlan(
		id: "same-day-straight",
		name: "Same day — straight",
		family: .pizza,
		leaven: .commercialYeast,
		summary: "Mix in the morning, bake for dinner. No fridge, no preferment.",
		stages: [
			PlanStage(id: "mix", kind: .mix, title: "Mix", detail: "Mix to a smooth, moderately developed dough.", hours: 0.25, temperatureC: 22, alerts: false),
			PlanStage(id: "bulk", kind: .bulk, title: "Bulk ferment", detail: "Covered, at room temperature.", hours: 3, temperatureC: 24, foldIntervalMinutes: 45),
			PlanStage(id: "divide", kind: .divide, title: "Divide & ball", hours: 0.25, temperatureC: 22, alerts: false),
			PlanStage(id: "proof", kind: .finalProof, title: "Final proof", detail: "Balls at room temperature until puffy and slack.", hours: 2, temperatureC: 24),
			PlanStage(id: "bake", kind: .bake, title: "Bake", hours: 0.25, temperatureC: 24, alerts: false)
		]
	)

	static let coldBallRetard = FermentationPlan(
		id: "cold-ball-retard",
		name: "Cold ball retard",
		family: .pizza,
		leaven: .commercialYeast,
		summary: "The pizzeria standard. Ball early, retard in trays, temper to order.",
		stages: [
			PlanStage(id: "mix", kind: .mix, title: "Mix", hours: 0.25, temperatureC: 22, alerts: false),
			PlanStage(id: "bulk", kind: .bulk, title: "Puntata", detail: "Short bulk before dividing.", hours: 1.5, temperatureC: 22),
			PlanStage(id: "divide", kind: .divide, title: "Divide & ball", detail: "Into oiled trays, lidded.", hours: 0.3, temperatureC: 22, alerts: false),
			PlanStage(id: "cold", kind: .coldRetard, title: "Cold ferment", detail: "Walk-in at 4 °C.", hours: 48, temperatureC: 4, usableWindowHours: 24),
			PlanStage(id: "temper", kind: .temper, title: "Temper", detail: "Out of the fridge until the balls relax.", hours: 3, temperatureC: 20),
			PlanStage(id: "bake", kind: .bake, title: "Bake", hours: 0.25, temperatureC: 22, alerts: false)
		]
	)

	static let coldBulkRetard = FermentationPlan(
		id: "cold-bulk-retard",
		name: "Cold bulk retard",
		family: .pizza,
		leaven: .commercialYeast,
		summary: "Retard the mass, divide the day of. Saves fridge space over balling early.",
		stages: [
			PlanStage(id: "mix", kind: .mix, title: "Mix", hours: 0.25, temperatureC: 22, alerts: false),
			PlanStage(id: "bulk", kind: .bulk, title: "Bulk ferment", hours: 1, temperatureC: 22),
			PlanStage(id: "cold", kind: .coldRetard, title: "Cold bulk", detail: "Whole mass, covered.", hours: 24, temperatureC: 4, usableWindowHours: 24),
			PlanStage(id: "divide", kind: .divide, title: "Divide & ball", hours: 0.3, temperatureC: 20, alerts: false),
			PlanStage(id: "proof", kind: .finalProof, title: "Warm up & proof", hours: 4, temperatureC: 20),
			PlanStage(id: "bake", kind: .bake, title: "Bake", hours: 0.25, temperatureC: 22, alerts: false)
		]
	)

	static let neapolitanDirect = FermentationPlan(
		id: "neapolitan-direct",
		name: "Neapolitan — direct",
		family: .pizza,
		leaven: .commercialYeast,
		summary: "Room-temperature puntata and appretto, the way the disciplinare describes it.",
		stages: [
			PlanStage(id: "mix", kind: .mix, title: "Impasto", detail: "Slow mix, finish at 23 °C.", hours: 0.35, temperatureC: 20, alerts: false),
			PlanStage(id: "puntata", kind: .bulk, title: "Puntata", detail: "Mass rest, covered.", hours: 2, temperatureC: 20),
			PlanStage(id: "staglio", kind: .divide, title: "Staglio", detail: "Divide and ball, into wooden boxes.", hours: 0.3, temperatureC: 20, alerts: false),
			PlanStage(id: "appretto", kind: .finalProof, title: "Appretto", detail: "Balls proof until nearly doubled.", hours: 6, temperatureC: 20),
			PlanStage(id: "bake", kind: .bake, title: "Bake", detail: "60–90 seconds at 430–480 °C.", hours: 0.2, temperatureC: 22, alerts: false)
		]
	)

	static let poolishPizza = FermentationPlan(
		id: "poolish-pizza",
		name: "Poolish",
		family: .pizza,
		leaven: .commercialYeast,
		summary: "Wet preferment overnight for extensibility and a sweeter, milkier crumb.",
		prefermentKind: .poolish,
		prefermentedFlourPercent: 30,
		prefermentHydrationPercent: 100,
		stages: [
			PlanStage(id: "poolish", kind: .preferment, title: "Poolish", detail: "Equal flour and water, a pinch of yeast. Ready when domed and just starting to fall.", hours: 14, temperatureC: 20),
			PlanStage(id: "mix", kind: .mix, title: "Mix", hours: 0.3, temperatureC: 22, alerts: false),
			PlanStage(id: "bulk", kind: .bulk, title: "Bulk ferment", hours: 1, temperatureC: 22),
			PlanStage(id: "divide", kind: .divide, title: "Divide & ball", hours: 0.3, temperatureC: 22, alerts: false),
			PlanStage(id: "cold", kind: .coldRetard, title: "Cold ferment", hours: 24, temperatureC: 4, usableWindowHours: 18),
			PlanStage(id: "temper", kind: .temper, title: "Temper", hours: 3, temperatureC: 20),
			PlanStage(id: "bake", kind: .bake, title: "Bake", hours: 0.25, temperatureC: 22, alerts: false)
		]
	)

	static let bigaPizza = FermentationPlan(
		id: "biga-pizza",
		name: "Biga",
		family: .pizza,
		leaven: .commercialYeast,
		summary: "Stiff preferment, contemporary Italian style. Big open crumb, strong flavour.",
		prefermentKind: .biga,
		prefermentedFlourPercent: 60,
		prefermentHydrationPercent: 50,
		stages: [
			PlanStage(id: "biga", kind: .preferment, title: "Biga", detail: "Shaggy, barely mixed. Cool room.", hours: 18, temperatureC: 18),
			PlanStage(id: "mix", kind: .mix, title: "Mix", detail: "Break the biga down, add remaining water in stages.", hours: 0.4, temperatureC: 22, alerts: false),
			PlanStage(id: "bulk", kind: .bulk, title: "Rest", hours: 1, temperatureC: 22),
			PlanStage(id: "divide", kind: .divide, title: "Divide & ball", hours: 0.3, temperatureC: 22, alerts: false),
			PlanStage(id: "cold", kind: .coldRetard, title: "Cold ferment", hours: 18, temperatureC: 4, usableWindowHours: 18),
			PlanStage(id: "temper", kind: .temper, title: "Temper", hours: 4, temperatureC: 20),
			PlanStage(id: "bake", kind: .bake, title: "Bake", hours: 0.25, temperatureC: 22, alerts: false)
		]
	)

	static let detroitPan = FermentationPlan(
		id: "detroit-pan",
		name: "Detroit / pan",
		family: .pizza,
		leaven: .commercialYeast,
		summary: "Press into blued steel, retard in the pan, proof to the rim before saucing.",
		stages: [
			PlanStage(id: "mix", kind: .mix, title: "Mix", hours: 0.25, temperatureC: 22, alerts: false),
			PlanStage(id: "bulk", kind: .bulk, title: "Bulk ferment", hours: 2, temperatureC: 24, foldIntervalMinutes: 40),
			PlanStage(id: "pan", kind: .shape, title: "Press into pans", detail: "Oil the pan well; don't fight the dough.", hours: 0.3, temperatureC: 22, alerts: false),
			PlanStage(id: "cold", kind: .coldRetard, title: "Cold ferment", hours: 18, temperatureC: 4, usableWindowHours: 24),
			PlanStage(id: "proof", kind: .finalProof, title: "Proof in pan", detail: "Until it fills the corners and jiggles.", hours: 3, temperatureC: 21),
			PlanStage(id: "bake", kind: .bake, title: "Bake", hours: 0.3, temperatureC: 22, alerts: false)
		]
	)

	static let focacciaColdBulk = FermentationPlan(
		id: "focaccia-cold-bulk",
		name: "Focaccia — cold bulk",
		family: .bread,
		leaven: .commercialYeast,
		summary: "High hydration, long cold bulk, dimpled straight from the fridge.",
		stages: [
			PlanStage(id: "mix", kind: .mix, title: "Mix", hours: 0.25, temperatureC: 22, alerts: false),
			PlanStage(id: "bulk", kind: .bulk, title: "Bulk ferment", hours: 1.5, temperatureC: 22, foldIntervalMinutes: 30),
			PlanStage(id: "cold", kind: .coldRetard, title: "Cold bulk", hours: 20, temperatureC: 4, usableWindowHours: 28),
			PlanStage(id: "pan", kind: .shape, title: "Into the pan", hours: 0.2, temperatureC: 22, alerts: false),
			PlanStage(id: "proof", kind: .finalProof, title: "Proof & dimple", hours: 3, temperatureC: 22),
			PlanStage(id: "bake", kind: .bake, title: "Bake", hours: 0.4, temperatureC: 22, alerts: false)
		]
	)

	// MARK: - Sourdough

	static let sourdoughPizzaCold = FermentationPlan(
		id: "sourdough-pizza-cold",
		name: "Sourdough pizza — cold ball",
		family: .pizza,
		leaven: .sourdough,
		summary: "Levain built the night before, bulk warm, then a cold ball retard.",
		prefermentKind: .levain,
		prefermentedFlourPercent: 10,
		prefermentHydrationPercent: 100,
		stages: [
			PlanStage(id: "levain", kind: .preferment, title: "Build levain", detail: "Ready when domed and aromatic, before it collapses.", hours: 8, temperatureC: 24),
			PlanStage(id: "autolyse", kind: .autolyse, title: "Autolyse", detail: "Flour and water only.", hours: 1, temperatureC: 24, alerts: false),
			PlanStage(id: "mix", kind: .mix, title: "Mix", detail: "Add levain and salt.", hours: 0.3, temperatureC: 24, alerts: false),
			PlanStage(id: "bulk", kind: .bulk, title: "Bulk ferment", detail: "Coil folds through the first half.", hours: 3.5, temperatureC: 25, foldIntervalMinutes: 45),
			PlanStage(id: "divide", kind: .divide, title: "Divide & ball", hours: 0.3, temperatureC: 22, alerts: false),
			PlanStage(id: "cold", kind: .coldRetard, title: "Cold ferment", hours: 24, temperatureC: 4, usableWindowHours: 18),
			PlanStage(id: "temper", kind: .temper, title: "Temper", hours: 3, temperatureC: 20),
			PlanStage(id: "bake", kind: .bake, title: "Bake", hours: 0.25, temperatureC: 22, alerts: false)
		]
	)

	static let sourdoughCountryLoaf = FermentationPlan(
		id: "sourdough-country-loaf",
		name: "Sourdough country loaf",
		family: .bread,
		leaven: .sourdough,
		summary: "Warm bulk with folds, shape, then an overnight cold proof and a Dutch-oven bake.",
		prefermentKind: .levain,
		prefermentedFlourPercent: 10,
		prefermentHydrationPercent: 100,
		stages: [
			PlanStage(id: "levain", kind: .preferment, title: "Build levain", hours: 10, temperatureC: 24),
			PlanStage(id: "autolyse", kind: .autolyse, title: "Autolyse", hours: 1, temperatureC: 24, alerts: false),
			PlanStage(id: "mix", kind: .mix, title: "Mix", detail: "Levain in, salt after 30 minutes.", hours: 0.4, temperatureC: 25, alerts: false),
			PlanStage(id: "bulk", kind: .bulk, title: "Bulk ferment", detail: "Four sets of folds, 30 minutes apart.", hours: 4.5, temperatureC: 25, foldIntervalMinutes: 30),
			PlanStage(id: "preshape", kind: .divide, title: "Divide & preshape", hours: 0.2, temperatureC: 22, alerts: false),
			PlanStage(id: "bench", kind: .bench, title: "Bench rest", hours: 0.4, temperatureC: 22),
			PlanStage(id: "shape", kind: .shape, title: "Final shape", detail: "Into bannetons, seam up.", hours: 0.2, temperatureC: 22, alerts: false),
			PlanStage(id: "cold", kind: .coldRetard, title: "Cold proof", detail: "Uncovered for a drier skin, if you like the scoring crisp.", hours: 14, temperatureC: 4, usableWindowHours: 10),
			PlanStage(id: "bake", kind: .bake, title: "Bake", detail: "Lidded 20 min, then open to colour.", hours: 0.75, temperatureC: 22, alerts: false)
		]
	)

	static let poolishBaguette = FermentationPlan(
		id: "poolish-baguette",
		name: "Baguette — poolish",
		family: .bread,
		leaven: .commercialYeast,
		summary: "Overnight poolish, short bulk, gentle shaping, same-day bake.",
		prefermentKind: .poolish,
		prefermentedFlourPercent: 40,
		prefermentHydrationPercent: 100,
		stages: [
			PlanStage(id: "poolish", kind: .preferment, title: "Poolish", hours: 14, temperatureC: 20),
			PlanStage(id: "mix", kind: .mix, title: "Mix", hours: 0.3, temperatureC: 23, alerts: false),
			PlanStage(id: "bulk", kind: .bulk, title: "Bulk ferment", hours: 2, temperatureC: 24, foldIntervalMinutes: 45),
			PlanStage(id: "divide", kind: .divide, title: "Divide & preshape", hours: 0.2, temperatureC: 22, alerts: false),
			PlanStage(id: "bench", kind: .bench, title: "Bench rest", hours: 0.4, temperatureC: 22),
			PlanStage(id: "shape", kind: .shape, title: "Shape", hours: 0.2, temperatureC: 22, alerts: false),
			PlanStage(id: "proof", kind: .finalProof, title: "Final proof", detail: "In couche, until it springs back slowly.", hours: 1.25, temperatureC: 24),
			PlanStage(id: "bake", kind: .bake, title: "Bake", detail: "Steam hard for the first 10 minutes.", hours: 0.4, temperatureC: 22, alerts: false)
		]
	)
}
