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
				flours: [FlourLibrary.at("00-pizzeria", 100)],
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
				flours: [FlourLibrary.at("high-gluten", 100)],
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
				flours: [FlourLibrary.at("bread", 100)],
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
				flours: [FlourLibrary.at("bread", 100)],
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
				flours: [FlourLibrary.at("00-strong", 80), FlourLibrary.at("semola", 20)],
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
				flours: [FlourLibrary.at("00-strong", 100)],
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
				flours: [FlourLibrary.at("00-pizzeria", 100)],
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
				flours: [FlourLibrary.at("bread", 90), FlourLibrary.at("whole-wheat", 10)],
				flourNote: "Bread flour with 10 % whole wheat"
			),
			planID: FermentationPlan.sourdoughPizzaCold.id
		),
		DoughStyle(
			id: "sourdough-neapolitan",
			name: "Sourdough Neapolitan",
			family: .pizza,
			blurb: "Naples rhythm on a levain — no fridge, a touch of semola for bite.",
			formula: DoughFormula(
				name: "Sourdough Neapolitan",
				ballCount: 16,
				ballWeightGrams: 250,
				hydrationPercent: 63,
				saltPercent: 2.8,
				leaven: .sourdough,
				instantYeastPercent: 0,
				prefermentKind: .levain,
				prefermentedFlourPercent: 8,
				prefermentHydrationPercent: 100,
				flours: [FlourLibrary.at("00-pizzeria", 90), FlourLibrary.at("semola", 10)],
				flourNote: "Keep the levain young or it'll read sour in a 60-second bake"
			),
			planID: FermentationPlan.sourdoughRoomPizza.id
		),
		DoughStyle(
			id: "sourdough-new-york",
			name: "Sourdough New York",
			family: .pizza,
			blurb: "Foldable slice with a levain tang. Oil for browning, whole wheat for depth.",
			formula: DoughFormula(
				name: "Sourdough New York",
				ballCount: 16,
				ballWeightGrams: 340,
				hydrationPercent: 65,
				saltPercent: 2.2,
				oilPercent: 2,
				leaven: .sourdough,
				instantYeastPercent: 0,
				prefermentKind: .levain,
				prefermentedFlourPercent: 10,
				prefermentHydrationPercent: 100,
				flours: [
					FlourLibrary.at("high-gluten", 85),
					FlourLibrary.at("whole-wheat", 15)
				],
				flourNote: "High-gluten carries the long cold retard"
			),
			planID: FermentationPlan.sourdoughPizzaCold.id
		),
		DoughStyle(
			id: "sourdough-pan",
			name: "Sourdough pan",
			family: .pizza,
			blurb: "Detroit-style on a levain. Slack, oily, proofed to the rim of the pan.",
			formula: DoughFormula(
				name: "Sourdough pan",
				ballCount: 4,
				ballWeightGrams: 420,
				hydrationPercent: 75,
				saltPercent: 2,
				oilPercent: 3,
				leaven: .sourdough,
				instantYeastPercent: 0,
				prefermentKind: .levain,
				prefermentedFlourPercent: 12,
				prefermentHydrationPercent: 100,
				flours: [FlourLibrary.at("bread", 100)],
				flourNote: "Bread flour; a stronger flour fights the pan"
			),
			planID: FermentationPlan.sourdoughPanCold.id
		),
		DoughStyle(
			id: "sourdough-teglia",
			name: "Sourdough teglia",
			family: .pizza,
			blurb: "Roman tray pizza on a levain. Very wet, very open, cut with a scissor.",
			formula: DoughFormula(
				name: "Sourdough teglia",
				ballCount: 3,
				ballWeightGrams: 800,
				hydrationPercent: 82,
				saltPercent: 2.2,
				oilPercent: 3,
				leaven: .sourdough,
				instantYeastPercent: 0,
				prefermentKind: .levain,
				prefermentedFlourPercent: 10,
				prefermentHydrationPercent: 100,
				flours: [
					FlourLibrary.at("00-strong", 70),
					FlourLibrary.at("semola", 20),
					FlourLibrary.at("whole-wheat", 10)
				],
				flourNote: "Needs a strong flour to hold 82 %"
			),
			planID: FermentationPlan.sourdoughPanCold.id
		),
		DoughStyle(
			id: "semola-blend",
			name: "Semola blend pizza",
			family: .pizza,
			blurb: "00 cut with semola: more bite, more colour, a slightly shorter dough.",
			formula: DoughFormula(
				name: "Semola blend",
				ballCount: 16,
				ballWeightGrams: 270,
				hydrationPercent: 65,
				saltPercent: 2.6,
				instantYeastPercent: 0.15,
				flours: [FlourLibrary.at("00-pizzeria", 70), FlourLibrary.at("semola", 30)],
				flourNote: "Semola drinks less — expect a firmer dough at the same number"
			),
			planID: FermentationPlan.coldBallRetard.id
		),
		DoughStyle(
			id: "whole-grain-pizza",
			name: "Half whole-grain pizza",
			family: .pizza,
			blurb: "Fifty per cent whole grain. Nutty, fast-fermenting, needs less leaven than it looks.",
			formula: DoughFormula(
				name: "Half whole-grain",
				ballCount: 12,
				ballWeightGrams: 280,
				hydrationPercent: 72,
				saltPercent: 2.3,
				leaven: .sourdough,
				instantYeastPercent: 0,
				prefermentKind: .levain,
				prefermentedFlourPercent: 9,
				prefermentHydrationPercent: 100,
				flours: [
					FlourLibrary.at("bread", 50),
					FlourLibrary.at("whole-wheat", 40),
					FlourLibrary.at("kamut", 10)
				],
				flourNote: "Bran cuts the gluten — handle gently and don't over-bulk"
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
				flours: [
					FlourLibrary.at("bread", 80),
					FlourLibrary.at("whole-wheat", 15),
					FlourLibrary.at("rye", 5)
				],
				flourNote: "A little rye keeps the levain lively"
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
				flours: [FlourLibrary.at("bread", 100)],
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
				flours: [FlourLibrary.at("t65", 100)],
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
				flours: [FlourLibrary.at("bread", 100)],
				flourNote: "Bread flour"
			),
			planID: FermentationPlan.sameDayStraight.id
		)
	]

	static func style(id: String) -> DoughStyle? {
		library.first { $0.id == id }
	}

	static func styles(family: PlanFamily) -> [DoughStyle] {
		library.filter { $0.family == family }
	}

	/// Every style leavened with a levain — the sourdough shelf.
	static var sourdough: [DoughStyle] {
		library.filter { $0.formula.leaven == .sourdough }
	}
}
