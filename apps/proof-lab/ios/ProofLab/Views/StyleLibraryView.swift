import DoughKit
import SwiftUI

/// Reference shelf: what each style is, what it's usually fermented on, and what the model
/// thinks it wants. Tapping one starts a batch from it.
struct StyleLibraryView: View {
	@Environment(AppModel.self) private var model
	@State private var family: PlanFamily = .pizza

	var body: some View {
		ScrollView {
			VStack(spacing: 14) {
				Picker("Family", selection: $family) {
					ForEach(PlanFamily.allCases, id: \.self) { Text($0.displayName).tag($0) }
				}
				.pickerStyle(.segmented)

				ForEach(DoughStyle.library.filter { $0.family == family }) { style in
					NavigationLink {
						StyleDetailView(style: style)
					} label: {
						StyleCard(style: style)
					}
					.buttonStyle(.plain)
				}
			}
			.padding(16)
		}
		.background(Palette.paper)
		.navigationTitle("Styles")
	}
}

private struct StyleCard: View {
	var style: DoughStyle

	var body: some View {
		Panel {
			HStack(alignment: .firstTextBaseline) {
				Text(style.name).font(.mono(17, .semibold))
				Spacer()
				Text(Formatting.percent(style.formula.hydrationPercent, places: 0))
					.font(.mono(12))
					.foregroundStyle(Palette.inkMute)
			}
			Text(style.blurb).font(.mono(12)).foregroundStyle(Palette.inkMute)
			Hairline()
			HStack {
				Text(style.plan.name).font(.mono(11)).foregroundStyle(Palette.inkSoft)
				Spacer()
				Text(Formatting.hours(style.plan.totalHours))
					.font(.mono(11))
					.foregroundStyle(Palette.inkMute)
			}
		}
	}
}

struct StyleDetailView: View {
	@Environment(AppModel.self) private var model
	@Environment(\.dismiss) private var dismiss
	var style: DoughStyle

	private var result: FormulaResult { style.formula.result() }

	var body: some View {
		ScrollView {
			VStack(spacing: 14) {
				Panel(title: "About") {
					Text(style.blurb).font(.monoBody).foregroundStyle(Palette.inkSoft)
					if !style.formula.flourNote.isEmpty {
						Hairline()
						HStack {
							Text("Flour").fieldLabel()
							Spacer()
							Text(style.formula.flourNote).font(.mono(12)).foregroundStyle(Palette.inkSoft)
						}
					}
				}

				Panel(title: "Formula", trailing: "\(style.formula.ballCount) × \(Formatting.grams(style.formula.ballWeightGrams))") {
					ForEach(result.overall) { IngredientRow(ingredient: $0) }
				}

				Panel(title: "Fermentation", trailing: Formatting.hours(style.plan.fermentationLoadHours) + " load") {
					Text(style.plan.summary).font(.mono(12)).foregroundStyle(Palette.inkMute)
					Hairline()
					ForEach(style.plan.stages) { stage in
						HStack(alignment: .firstTextBaseline, spacing: 8) {
							Image(systemName: stage.kind.symbolName)
								.font(.mono(11))
								.foregroundStyle(stage.kind.tint)
								.frame(width: 16)
							Text(stage.title).font(.mono(13))
							Spacer(minLength: 8)
							Text(Formatting.temperature(stage.temperatureC, fahrenheit: model.useFahrenheit))
								.font(.mono(11))
								.foregroundStyle(Palette.inkMute)
							Text(Formatting.hours(stage.hours))
								.font(.mono(12, .medium))
								.frame(minWidth: 56, alignment: .trailing)
						}
						.padding(.vertical, 3)
					}
				}

				BlockButton(title: "Start a batch from this", tint: Palette.hot) {
					let batch = Batch(
						name: style.name,
						startAt: Date(),
						formula: style.formula,
						plan: style.plan,
						mixerCapacityKg: model.defaultMixerCapacityKg
					)
					model.add(batch)
					dismiss()
				}
			}
			.padding(16)
		}
		.background(Palette.paper)
		.navigationTitle(style.name)
		.navigationBarTitleDisplayMode(.inline)
	}
}
