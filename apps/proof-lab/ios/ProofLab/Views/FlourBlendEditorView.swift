import DoughKit
import SwiftUI

/// Build a blend. Shares don't have to add to 100 while you're working — the model
/// normalises them — but the app tells you when they don't so it isn't a silent surprise.
struct FlourBlendEditorView: View {
	@Binding var flours: [FlourComponent]
	var hydrationPercent: Double

	@State private var showingPicker = false

	private var blend: FlourBlend { FlourBlend(flours) }
	private var total: Double { flours.reduce(0) { $0 + $1.percent } }

	var body: some View {
		ScrollView {
			VStack(spacing: 14) {
				summaryPanel
				componentsPanel
				addPanel
			}
			.padding(16)
		}
		.background(Palette.paper)
		.navigationTitle("Flour blend")
		.navigationBarTitleDisplayMode(.inline)
	}

	private var summaryPanel: some View {
		Panel(title: "Blend", trailing: blend.summary) {
			HStack(alignment: .top, spacing: 12) {
				StatTile(
					label: "Protein",
					value: Formatting.percent(blend.proteinPercent, places: 2),
					caption: "weighted average"
				)
				StatTile(
					label: "Whole grain",
					value: Formatting.percent(blend.wholeGrainFraction * 100, places: 0),
					caption: blend.wholeGrainFraction > 0 ? "ferments faster, dose lighter" : nil
				)
			}
			Hairline()
			StatTile(
				label: "Absorption guide",
				value: Formatting.percent(blend.absorptionGuidePercent, places: 1),
				caption: hydrationPercent > blend.absorptionGuidePercent + 8
					? "you're \(Formatting.rounded(hydrationPercent - blend.absorptionGuidePercent, places: 0)) points above it — expect a slack dough"
					: "roughly what this blend carries comfortably",
				tint: hydrationPercent > blend.absorptionGuidePercent + 8 ? Palette.hot : Palette.ink
			)
		}
	}

	private var componentsPanel: some View {
		Panel(title: "Flours", trailing: Formatting.percent(total, places: 1)) {
			if flours.isEmpty {
				Text("No flour yet — add one below.").font(.mono(12)).foregroundStyle(Palette.inkMute)
			}

			ForEach(Array(flours.enumerated()), id: \.element.id) { index, flour in
				VStack(alignment: .leading, spacing: 6) {
					if index > 0 { Hairline() }
					HStack(alignment: .firstTextBaseline) {
						VStack(alignment: .leading, spacing: 2) {
							Text(flour.name).font(.mono(13, .semibold))
							Text(
								Formatting.percent(flour.proteinPercent, places: 1) + " protein"
									+ (flour.isWholeGrain ? " · whole grain" : "")
							)
							.font(.mono(10))
							.foregroundStyle(Palette.inkMute)
						}
						Spacer()
						Button {
							flours.remove(at: index)
						} label: {
							Image(systemName: "minus.circle").foregroundStyle(Palette.hot)
						}
						.buttonStyle(.plain)
					}
					NumberField(
						label: "Share",
						suffix: "%",
						value: Binding(
							get: { index < flours.count ? flours[index].percent : 0 },
							set: { if index < flours.count { flours[index].percent = $0 } }
						),
						range: 0...100,
						decimals: 1
					)
				}
				.padding(.vertical, 4)
			}

			if abs(total - 100) > 0.05, !flours.isEmpty {
				Hairline()
				Text("Adds to \(Formatting.percent(total, places: 1)). Weights are worked out from the ratio either way.")
					.font(.mono(11))
					.foregroundStyle(Palette.inkMute)
				BlockButton(title: "Normalise to 100 %", tint: Palette.ink, filled: false) {
					flours = blend.normalized
				}
			}
		}
	}

	private var addPanel: some View {
		Panel(title: "Add a flour") {
			ForEach(FlourLibrary.all.filter { candidate in
				!flours.contains { $0.id == candidate.id }
			}) { candidate in
				Button {
					// New flours come in at whatever's missing, or 10 % if the blend is full.
					let remaining = max(0, 100 - total)
					flours.append(FlourLibrary.at(candidate.id, remaining > 1 ? remaining : 10))
				} label: {
					HStack {
						Text(candidate.name).font(.monoBody).foregroundStyle(Palette.ink)
						Spacer()
						Text(Formatting.percent(candidate.proteinPercent, places: 1))
							.font(.mono(11))
							.foregroundStyle(Palette.inkMute)
						Image(systemName: "plus.circle").foregroundStyle(Palette.inkMute)
					}
					.padding(.vertical, 4)
				}
				.buttonStyle(.plain)
			}
		}
	}
}
