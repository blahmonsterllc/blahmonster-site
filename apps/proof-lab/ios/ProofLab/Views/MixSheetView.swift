import DoughKit
import SwiftUI

/// The sheet you'd actually tape to the mixer: what to weigh, how many loads, and what
/// temperature the water needs to be so the dough lands on target.
struct MixSheetView: View {
	@Environment(AppModel.self) private var model
	@Environment(\.dismiss) private var dismiss

	let batch: Batch

	@State private var mixerCapacityKg: Double = 20
	@State private var mixer: MixerKind = .spiral
	@State private var desiredDoughTempC: Double = 24
	@State private var flourTempC: Double = 21
	@State private var roomTempC: Double = 22
	@State private var tapWaterTempC: Double = 18

	private var result: FormulaResult { batch.formula.result() }
	private var production: ProductionPlan { result.productionPlan(mixerCapacityKg: mixerCapacityKg) }

	private var prefermentTempC: Double? {
		batch.plan.prefermentStage?.temperatureC
	}

	private var water: WaterTemperatureResult {
		DoughTemperature.solve(
			desiredDoughTempC: desiredDoughTempC,
			flourTempC: flourTempC,
			roomTempC: roomTempC,
			prefermentTempC: prefermentTempC,
			mixer: mixer,
			totalWaterGrams: result.totalWaterGrams,
			tapWaterTempC: tapWaterTempC
		)
	}

	var body: some View {
		ScrollView {
			VStack(spacing: 14) {
				if !result.prefermentBuild.isEmpty { prefermentPanel }
				waterPanel
				productionPanel
				ForEach(production.mixes) { mixPanel($0) }
			}
			.padding(16)
		}
		.background(Palette.paper)
		.navigationTitle("Mix sheet")
		.navigationBarTitleDisplayMode(.inline)
		.toolbar {
			ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() } }
		}
		.onAppear {
			mixerCapacityKg = batch.mixerCapacityKg
			mixer = model.defaultMixer
		}
	}

	private var prefermentPanel: some View {
		Panel(
			title: batch.formula.prefermentKind.displayName,
			trailing: batch.plan.prefermentStage.map { Formatting.hours($0.hours) }
		) {
			ForEach(result.prefermentBuild) { IngredientRow(ingredient: $0, showPercent: false) }
			Hairline()
			HStack {
				Text("Ripe weight").fieldLabel()
				Spacer()
				Text(Formatting.grams(result.prefermentTotalGrams)).font(.mono(13, .semibold))
			}
			if let stage = batch.plan.prefermentStage {
				Text("Hold at \(Formatting.temperature(stage.temperatureC, fahrenheit: model.useFahrenheit)) for \(Formatting.hours(stage.hours)).")
					.font(.mono(11))
					.foregroundStyle(Palette.inkMute)
			}
		}
	}

	private var waterPanel: some View {
		Panel(title: "Water temperature", trailing: prefermentTempC == nil ? "3-factor" : "4-factor") {
			NumberField(label: "Target dough", suffix: "°C", value: $desiredDoughTempC, range: 18...32, decimals: 1)
			Hairline()
			NumberField(label: "Flour", suffix: "°C", value: $flourTempC, range: 0...40, decimals: 1)
			Hairline()
			NumberField(label: "Room", suffix: "°C", value: $roomTempC, range: 0...45, decimals: 1)
			Hairline()
			NumberField(label: "Tap water", suffix: "°C", value: $tapWaterTempC, range: 0...40, decimals: 1)
			Hairline()

			Picker("Mixer", selection: $mixer) {
				ForEach(MixerKind.allCases, id: \.self) { Text($0.displayName).tag($0) }
			}
			.pickerStyle(.menu)
			Text("Friction adds \(Formatting.rounded(mixer.frictionC, places: 1)) °C over a normal mix.")
				.font(.mono(11))
				.foregroundStyle(Palette.inkMute)

			Hairline()

			HStack(alignment: .top, spacing: 12) {
				StatTile(
					label: "Use water at",
					value: Formatting.temperature(water.waterTemperatureC, fahrenheit: model.useFahrenheit),
					caption: water.warning == nil ? nil : "not reachable",
					tint: water.warning == nil ? Palette.ink : Palette.hot
				)
				if water.iceGrams > 0 {
					StatTile(
						label: "Ice / water split",
						value: Formatting.grams(water.iceGrams) + " ice",
						caption: "+ " + Formatting.grams(water.waterGrams) + " tap water",
						tint: Palette.cold
					)
				} else {
					StatTile(
						label: "Total water",
						value: Formatting.grams(result.totalWaterGrams),
						caption: "no ice needed"
					)
				}
			}

			if let warning = water.warning {
				Text(warning).font(.mono(11)).foregroundStyle(Palette.hot)
			}
		}
	}

	private var productionPanel: some View {
		Panel(title: "Production", trailing: Formatting.grams(result.totalDoughGrams)) {
			NumberField(label: "Mixer capacity", suffix: "kg", value: $mixerCapacityKg, range: 0.5...500, decimals: 1)
			Hairline()
			HStack(alignment: .top, spacing: 12) {
				StatTile(
					label: production.mixCount == 1 ? "Mix" : "Mixes",
					value: "\(production.mixCount)",
					caption: "\(result.ballCount) pieces total"
				)
				StatTile(
					label: "Per mix",
					value: Formatting.grams(production.totalDoughGrams / Double(production.mixCount)),
					caption: "of dough"
				)
			}
		}
	}

	private func mixPanel(_ mix: ProductionPlan.Mix) -> some View {
		Panel(
			title: production.mixCount == 1 ? "Mix" : "Mix \(mix.id) of \(production.mixCount)",
			trailing: "\(mix.ballCount) pieces"
		) {
			ForEach(mix.ingredients) { IngredientRow(ingredient: $0) }
			Hairline()
			HStack {
				Text("Dough").fieldLabel()
				Spacer()
				Text(Formatting.grams(mix.doughGrams)).font(.mono(13, .semibold))
			}
		}
	}
}
