import DoughKit
import SwiftUI

/// Pick a fermentation type, then push its stages around. The leaven readout at the top
/// updates as you do, which is the whole point — you can see a 24 hour retard turn into a
/// 72 hour one and watch the yeast fall to match.
struct PlanEditorView: View {
	@Binding var plan: FermentationPlan
	var formula: DoughFormula

	@Environment(AppModel.self) private var model

	var body: some View {
		ScrollView {
			VStack(spacing: 14) {
				typePanel
				loadPanel
				ForEach(Array(plan.stages.enumerated()), id: \.element.id) { index, _ in
					stagePanel(index: index)
				}
			}
			.padding(16)
		}
		.background(Palette.paper)
		.navigationTitle("Fermentation")
		.navigationBarTitleDisplayMode(.inline)
	}

	private var typePanel: some View {
		Panel(title: "Fermentation type") {
			Picker("Type", selection: Binding(
				get: { plan.id },
				set: { newID in
					guard let replacement = FermentationPlan.plan(id: newID) else { return }
					plan = replacement
				}
			)) {
				ForEach(PlanFamily.allCases, id: \.self) { family in
					Section(family.displayName) {
						ForEach(FermentationPlan.library.filter { $0.family == family }) { option in
							Text(option.name).tag(option.id)
						}
					}
				}
			}
			.pickerStyle(.menu)

			Text(plan.summary).font(.mono(12)).foregroundStyle(Palette.inkMute)
		}
	}

	private var loadPanel: some View {
		let load = plan.fermentationLoadHours
		let suggested = formula.leaven == .sourdough
			? Leavening.levainPercent(equivalentHours: load, saltPercent: formula.saltPercent)
				/ (1 + formula.prefermentHydrationPercent / 100)
			: Leavening.instantYeastPercent(
				equivalentHours: load,
				saltPercent: formula.saltPercent,
				sugarPercent: formula.sugarPercent,
				prefermentedFlourFraction: formula.prefermentedFlourFraction
			) * formula.yeastType.multiplier

		return Panel(title: "What this schedule asks for") {
			HStack(alignment: .top, spacing: 12) {
				StatTile(
					label: "Ferment load",
					value: Formatting.hours(load),
					caption: "equivalent hours at 24 °C"
				)
				StatTile(
					label: formula.leaven == .sourdough ? "Levain flour" : "Yeast — \(formula.yeastType.shortName)",
					value: Formatting.percent(suggested, places: 3),
					caption: "suggested dose"
				)
			}
			Hairline()
			HStack(alignment: .top, spacing: 12) {
				StatTile(label: "Mix to bake", value: Formatting.hours(plan.totalHours), caption: nil)
				StatTile(
					label: "Cold time",
					value: plan.coldStage.map { Formatting.hours($0.hours) } ?? "None",
					caption: plan.coldStage.map { Formatting.temperature($0.temperatureC, fahrenheit: model.useFahrenheit) },
					tint: plan.coldStage == nil ? Palette.inkMute : Palette.cold
				)
			}
		}
	}

	private func stagePanel(index: Int) -> some View {
		let stage = plan.stages[index]

		return Panel(
			title: stage.title,
			trailing: stage.kind.countsTowardFermentation
				? Formatting.hours(stage.equivalentHours) + " eq"
				: nil
		) {
			if !stage.detail.isEmpty {
				Text(stage.detail).font(.mono(11)).foregroundStyle(Palette.inkMute)
			}

			NumberField(
				label: "Duration",
				suffix: "h",
				value: Binding(
					get: { plan.stages[index].hours },
					set: { plan.stages[index].hours = $0 }
				),
				range: 0...168,
				decimals: 2
			)

			Hairline()

			NumberField(
				label: "Temperature",
				suffix: "°C",
				value: Binding(
					get: { plan.stages[index].temperatureC },
					set: { plan.stages[index].temperatureC = $0 }
				),
				range: -2...45,
				decimals: 1
			)

			if stage.kind.isCold {
				Hairline()
				NumberField(
					label: "Usable window",
					suffix: "h",
					value: Binding(
						get: { plan.stages[index].usableWindowHours ?? 0 },
						set: { plan.stages[index].usableWindowHours = $0 > 0 ? $0 : nil }
					),
					range: 0...96,
					decimals: 1
				)
				Text("How long the dough stays good past ready. ProofLab alerts you at both edges so a tray doesn't quietly go over.")
					.font(.mono(11))
					.foregroundStyle(Palette.inkMute)
			}

			if stage.kind.countsTowardFermentation, stage.hours > 0 {
				Hairline()
				Toggle(isOn: Binding(
					get: { plan.stages[index].alerts },
					set: { plan.stages[index].alerts = $0 }
				)) {
					Text("Alert when it's up").font(.mono(12))
				}

				// Kept visible once turned off, otherwise the toggle disappears with its own state.
				if stage.kind == .bulk || stage.foldIntervalMinutes != nil {
					Toggle(isOn: Binding(
						get: { plan.stages[index].foldIntervalMinutes != nil },
						set: { plan.stages[index].foldIntervalMinutes = $0 ? 30 : nil }
					)) {
						Text("Fold reminders").font(.mono(12))
					}
				}
			}
		}
	}
}
