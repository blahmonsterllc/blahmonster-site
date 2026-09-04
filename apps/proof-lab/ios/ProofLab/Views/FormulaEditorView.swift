import DoughKit
import SwiftUI

struct FormulaEditorView: View {
	@Binding var formula: DoughFormula
	var plan: FermentationPlan

	@Environment(AppModel.self) private var model

	private var result: FormulaResult { formula.result() }

	var body: some View {
		ScrollView {
			VStack(spacing: 14) {
				yieldPanel
				flourPanel
				percentagePanel
				leavenPanel
				prefermentPanel
				totalsPanel
			}
			.padding(16)
		}
		.background(Palette.paper)
		.navigationTitle("Formula")
		.navigationBarTitleDisplayMode(.inline)
	}

	private var yieldPanel: some View {
		Panel(title: "Yield") {
			IntField(label: formula.ballCount == 1 ? "Piece" : "Pieces", value: $formula.ballCount, range: 1...5000)
			Hairline()
			NumberField(label: "Piece weight", suffix: "g", value: $formula.ballWeightGrams, range: 20...5000, decimals: 0)
			Hairline()
			NumberField(label: "Loss allowance", suffix: "%", value: $formula.lossPercent, range: 0...15, decimals: 1)
			Text("Scrap, what stays in the bowl, the ball you drop. Two per cent covers most days.")
				.font(.mono(11))
				.foregroundStyle(Palette.inkMute)
		}
	}

	private var flourPanel: some View {
		let blend = formula.blend
		return NavigationLink {
			FlourBlendEditorView(flours: $formula.flours, hydrationPercent: formula.hydrationPercent)
		} label: {
			Panel(title: "Flour", trailing: Formatting.percent(blend.proteinPercent, places: 1) + " protein") {
				HStack {
					VStack(alignment: .leading, spacing: 4) {
						Text(blend.summary).font(.monoBody).foregroundStyle(Palette.ink)
						if !formula.flourNote.isEmpty {
							Text(formula.flourNote).font(.mono(11)).foregroundStyle(Palette.inkMute)
						}
					}
					Spacer()
					Image(systemName: "chevron.right").font(.mono(12)).foregroundStyle(Palette.inkMute)
				}
			}
		}
	}

	private var percentagePanel: some View {
		Panel(title: "Baker's percentages", trailing: "of total flour") {
			NumberField(label: "Hydration", suffix: "%", value: $formula.hydrationPercent, range: 40...110, decimals: 1)
			Hairline()
			NumberField(label: "Salt", suffix: "%", value: $formula.saltPercent, range: 0...5, decimals: 2)
			Hairline()
			NumberField(label: "Oil", suffix: "%", value: $formula.oilPercent, range: 0...20, decimals: 1)
			Hairline()
			NumberField(label: "Sugar", suffix: "%", value: $formula.sugarPercent, range: 0...25, decimals: 1)
			Hairline()
			NumberField(label: "Diastatic malt", suffix: "%", value: $formula.maltPercent, range: 0...3, decimals: 2)
		}
	}

	private var leavenPanel: some View {
		Panel(title: "Leaven") {
			Picker("Leaven", selection: $formula.leaven) {
				ForEach(LeavenKind.allCases, id: \.self) { Text($0.displayName).tag($0) }
			}
			.pickerStyle(.segmented)

			if formula.leaven == .commercialYeast {
				Picker("Yeast type", selection: $formula.yeastType) {
					ForEach(YeastType.allCases, id: \.self) { Text($0.shortName).tag($0) }
				}
				.pickerStyle(.segmented)

				NumberField(
					label: "Yeast (\(formula.yeastType.shortName))",
					suffix: "%",
					value: Binding(
						get: { formula.instantYeastPercent * formula.yeastType.multiplier },
						set: { formula.instantYeastPercent = $0 / formula.yeastType.multiplier }
					),
					range: 0...5,
					decimals: 3
				)

				if formula.yeastType != .instantDry {
					Text("Stored as \(Formatting.percent(formula.instantYeastPercent, places: 3)) instant-equivalent, so switching yeast type keeps the same leavening power.")
						.font(.mono(11))
						.foregroundStyle(Palette.inkMute)
				}
			} else {
				Text("A sourdough formula carries its leavening in the levain — set its size in the preferment section below.")
					.font(.mono(11))
					.foregroundStyle(Palette.inkMute)
			}
		}
	}

	private var prefermentPanel: some View {
		Panel(title: "Preferment") {
			Picker("Preferment", selection: $formula.prefermentKind) {
				ForEach(PrefermentKind.allCases, id: \.self) { Text($0.displayName).tag($0) }
			}
			.pickerStyle(.menu)
			.onChange(of: formula.prefermentKind) { _, new in
				guard new != .none else { return }
				formula.prefermentHydrationPercent = new.defaultHydrationPercent
				if formula.prefermentedFlourPercent == 0 { formula.prefermentedFlourPercent = 20 }
			}

			if formula.prefermentKind != .none {
				NumberField(
					label: "Prefermented flour",
					suffix: "%",
					value: $formula.prefermentedFlourPercent,
					range: 0...100,
					decimals: 1
				)
				Hairline()
				NumberField(
					label: "Its hydration",
					suffix: "%",
					value: $formula.prefermentHydrationPercent,
					range: 40...200,
					decimals: 0
				)
				Hairline()

				if formula.prefermentKind.usesStarterSeed {
					NumberField(
						label: "Starter seed",
						suffix: "%",
						value: $formula.starterSeedPercent,
						range: 1...100,
						decimals: 0
					)
					Text("Ripe starter as a share of the levain's flour. Less seed means a longer, milder build.")
						.font(.mono(11))
						.foregroundStyle(Palette.inkMute)
				} else {
					NumberField(
						label: "Yeast in it",
						suffix: "%",
						value: $formula.prefermentYeastPercent,
						range: 0...2,
						decimals: 3
					)
				}

				if !result.prefermentBuild.isEmpty {
					Hairline()
					Text("Build the night before").fieldLabel()
					ForEach(result.prefermentBuild) { IngredientRow(ingredient: $0, showPercent: false) }
					Text("Total " + Formatting.grams(result.prefermentTotalGrams))
						.font(.mono(11))
						.foregroundStyle(Palette.inkMute)
				}
			}
		}
	}

	private var totalsPanel: some View {
		Panel(title: "Totals", trailing: Formatting.grams(result.totalDoughGrams)) {
			ForEach(result.overall) { IngredientRow(ingredient: $0) }
			Hairline()
			HStack {
				Text("Ferment load").fieldLabel()
				Spacer()
				Text(Formatting.hours(plan.fermentationLoadHours)).font(.mono(12))
			}
		}
	}
}
