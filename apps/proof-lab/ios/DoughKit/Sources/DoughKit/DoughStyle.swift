import Foundation

/// A starting point: a formula plus the fermentation plan it's usually run on.
public struct DoughStyle: Sendable, Equatable, Identifiable {
	public let id: String
	public let name: String
	public let family: PlanFamily
	public let blurb: String
	public let formula: DoughFormula
	public let planID: String

	public var plan: FermentationPlan {
		FermentationPlan.plan(id: planID) ?? .coldBallRetard
	}
}

public extension DoughStyle {
	static let library: [DoughStyle] = [
		DoughStyle(
			id: "neapolitan",
			name: "Neapolitan",
			family: .pizza,
			blurb: "00 flour, no oil, no sugar. Soft, blistered, gone in four bites.",
			formula: DoughFormula(
				name: "Neapolitan",
				ballCount: 20,
				ballWeightGrams: 250,
				hydrationPercent: 62,
				saltPercent: 2.8,
				instantYeastPercent: 0.1,
				flourNote: "Type 00, W 260–300"
			),
			planID: FermentationPlan.neapolitanDirect.id
		),
		DoughStyle(
			id: "new-york",
			name: "New York",
			family: .pizza,
			blurb: "Oil and a touch of sugar for a foldable slice that browns evenly.",
			formula: DoughFormula(
				name: "New York",
				ballCount: 20,
				ballWeightGrams: 340,
				hydrationPercent: 62,
				saltPercent: 2,
				oilPercent: 2,
				sugarPercent: 1.5,
				instantYeastPercent: 0.21,
				flourNote: "High-gluten bread flour, 13–14 % protein"
			),
			planID: FermentationPlan.coldBallRetard.id
		),
		DoughStyle(
			id: "detroit",
			name: "Detroit",
			family: .pizza,
			blurb: "Wet, slack, pressed into steel. Cheese to the edges, crust to the corners.",
			formula: DoughFormula(
				name: "Detroit",
				ballCount: 6,
				ballWeightGrams: 400,
				hydrationPercent: 70,
				saltPercent: 2,
				oilPercent: 3,
				instantYeastPercent: 0.3,
				flourNote: "Bread flour"
			),
			planID: FermentationPlan.detroitPan.id
		),
		DoughStyle(
			id: "sicilian",
			name: "Sicilian / grandma",
			family: .pizza,
			blurb: "Oily pan dough, tender crumb, crisp fried bottom.",
			formula: DoughFormula(
				name: "Sicilian",
				ballCount: 4,
				ballWeightGrams: 700,
				hydrationPercent: 72,
				saltPercent: 2.2,
				oilPercent: 4,
				instantYeastPercent: 0.35,
				flourNote: "Bread flour"
			),
			planID: FermentationPlan.coldBulkRetard.id
		),
		DoughStyle(
			id: "roman-teglia",
			name: "Roman teglia",
			family: .pizza,
			blurb: "Very high hydration, cold bulk, airy honeycomb crumb.",
			formula: DoughFormula(
				name: "Roman teglia",
				ballCount: 4,
				ballWeightGrams: 750,
				hydrationPercent: 80,
				saltPercent: 2.2,
				oilPercent: 3,
				instantYeastPercent: 0.3,
				flourNote: "Strong 00 or a W 330+ blend"
			),
			planID: FermentationPlan.coldBulkRetard.id
		),
		DoughStyle(
			id: "contemporary-biga",
			name: "Contemporary — biga",
			family: .pizza,
			blurb: "Stiff biga at 60 % of the flour. Cloud-like cornicione.",
			formula: DoughFormula(
				name: "Contemporary biga",
				ballCount: 16,
				ballWeightGrams: 280,
				hydrationPercent: 72,
				saltPercent: 2.6,
				instantYeastPercent: 0.08,
				prefermentKind: .biga,
				prefermentedFlourPercent: 60,
				prefermentHydrationPercent: 50,
				prefermentYeastPercent: 0.12,
				flourNote: "Strong 00, W 320+"
			),
			planID: FermentationPlan.bigaPizza.id
		),
		DoughStyle(
			id: "poolish-pizza",
			name: "Poolish pizza",
			family: .pizza,
			blurb: "Overnight poolish for extensibility and a sweeter crumb.",
			formula: DoughFormula(
				name: "Poolish pizza",
				ballCount: 16,
				ballWeightGrams: 270,
				hydrationPercent: 68,
				saltPercent: 2.5,
				instantYeastPercent: 0.1,
				prefermentKind: .poolish,
				prefermentedFlourPercent: 30,
				prefermentHydrationPercent: 100,
				prefermentYeastPercent: 0.15,
				flourNote: "00 or bread flour"
			),
			planID: FermentationPlan.poolishPizza.id
		),
		DoughStyle(
			id: "sourdough-pizza",
			name: "Sourdough pizza",
			family: .pizza,
			blurb: "Levain-leavened, cold-retarded balls. Tang without sourness.",
			formula: DoughFormula(
				name: "Sourdough pizza",
				ballCount: 12,
				ballWeightGrams: 270,
				hydrationPercent: 66,
				saltPercent: 2.5,
				leaven: .sourdough,
				instantYeastPercent: 0,
				prefermentKind: .levain,
				prefermentedFlourPercent: 10,
				prefermentHydrationPercent: 100,
				flourNote: "Bread flour with 10 % whole wheat"
			),
			planID: FermentationPlan.sourdoughPizzaCold.id
		),
		DoughStyle(
			id: "country-loaf",
			name: "Country sourdough",
			family: .bread,
			blurb: "Bread flour with a little whole grain, overnight cold proof.",
			formula: DoughFormula(
				name: "Country sourdough",
				ballCount: 2,
				ballWeightGrams: 900,
				hydrationPercent: 76,
				saltPercent: 2,
				leaven: .sourdough,
				instantYeastPercent: 0,
				prefermentKind: .levain,
				prefermentedFlourPercent: 10,
				prefermentHydrationPercent: 100,
				flourNote: "85 % bread flour, 15 % whole wheat"
			),
			planID: FermentationPlan.sourdoughCountryLoaf.id
		),
		DoughStyle(
			id: "focaccia",
			name: "Focaccia",
			family: .bread,
			blurb: "Olive oil, sea salt, dimples all the way to the bottom of the pan.",
			formula: DoughFormula(
				name: "Focaccia",
				ballCount: 2,
				ballWeightGrams: 1000,
				hydrationPercent: 80,
				saltPercent: 2.2,
				oilPercent: 5,
				instantYeastPercent: 0.25,
				flourNote: "Bread flour"
			),
			planID: FermentationPlan.focacciaColdBulk.id
		),
		DoughStyle(
			id: "baguette",
			name: "Baguette",
			family: .bread,
			blurb: "Poolish at 40 %, restrained bulk, thin crackling crust.",
			formula: DoughFormula(
				name: "Baguette",
				ballCount: 6,
				ballWeightGrams: 350,
				hydrationPercent: 72,
				saltPercent: 2,
				instantYeastPercent: 0.12,
				prefermentKind: .poolish,
				prefermentedFlourPercent: 40,
				prefermentHydrationPercent: 100,
				prefermentYeastPercent: 0.12,
				flourNote: "T65 or unbleached AP"
			),
			planID: FermentationPlan.poolishBaguette.id
		),
		DoughStyle(
			id: "enriched",
			name: "Enriched sandwich",
			family: .bread,
			blurb: "Sugar, butter, milk powder. Soft crumb, quick same-day schedule.",
			formula: DoughFormula(
				name: "Enriched sandwich",
				ballCount: 3,
				ballWeightGrams: 800,
				hydrationPercent: 64,
				saltPercent: 2,
				oilPercent: 8,
				sugarPercent: 10,
				instantYeastPercent: 0.6,
				flourNote: "Bread flour"
			),
			planID: FermentationPlan.sameDayStraight.id
		)
	]

	static func style(id: String) -> DoughStyle? {
		library.first { $0.id == id }
	}
}
