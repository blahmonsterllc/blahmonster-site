import DoughKit
import SwiftUI

/// Style → formula → plan → when. Everything recalculates as you go, so you can see what a
/// change to the schedule does to the yeast before you commit.
struct NewBatchView: View {
	@Environment(AppModel.self) private var model
	@Environment(\.dismiss) private var dismiss

	@State private var name = ""
	@State private var styleID = DoughStyle.library.first!.id
	@State private var formula = DoughStyle.library.first!.formula
	@State private var plan = DoughStyle.library.first!.plan
	@State private var anchorMode = AnchorMode.readyBy
	@State private var anchorDate = Calendar.current.date(byAdding: .hour, value: 12, to: Date()) ?? Date()

	enum AnchorMode: String, CaseIterable, Identifiable {
		case startNow = "Start now"
		case readyBy = "Ready by"
		var id: String { rawValue }
	}

	private var schedule: Schedule {
		Scheduler.build(plan: plan, anchor: anchor)
	}

	private var anchor: ScheduleAnchor {
		switch anchorMode {
		case .startNow: .startAt(Date())
		case .readyBy: .readyBy(anchorDate)
		}
	}

	var body: some View {
		ScrollView {
			VStack(spacing: 14) {
				stylePanel
				timingPanel
				leavenPanel

				NavigationLink {
					FormulaEditorView(formula: $formula, plan: plan)
				} label: {
					summaryRow(
						title: "Formula",
						detail: "\(formula.ballCount) × \(Formatting.grams(formula.ballWeightGrams)) · \(Formatting.percent(formula.hydrationPercent, places: 1)) hydration"
					)
				}

				NavigationLink {
					PlanEditorView(plan: $plan, formula: formula)
				} label: {
					summaryRow(
						title: "Fermentation",
						detail: "\(plan.name) · \(Formatting.hours(plan.totalHours)) · \(plan.stages.count) stages"
					)
				}

				BlockButton(title: "Start this batch", tint: Palette.hot) { start() }
					.padding(.top, 4)
			}
			.padding(16)
		}
		.background(Palette.paper)
		.navigationTitle("New batch")
		.navigationBarTitleDisplayMode(.inline)
		.toolbar {
			ToolbarItem(placement: .cancellationAction) {
				Button("Cancel") { dismiss() }
			}
		}
	}

	// MARK: - Panels

	private var stylePanel: some View {
		Panel(title: "Style") {
			Picker("Style", selection: $styleID) {
				ForEach(DoughStyle.library) { style in
					Text(style.name).tag(style.id)
				}
			}
			.pickerStyle(.menu)
			.onChange(of: styleID) { _, new in
				guard let style = DoughStyle.style(id: new) else { return }
				formula = style.formula
				plan = style.plan
			}

			if let style = DoughStyle.style(id: styleID) {
				Text(style.blurb).font(.mono(12)).foregroundStyle(Palette.inkMute)
				if !style.formula.flourNote.isEmpty {
					Text("Flour: " + style.formula.flourNote)
						.font(.mono(11))
						.foregroundStyle(Palette.inkMute)
				}
			}

			Hairline()

			HStack(alignment: .firstTextBaseline, spacing: 8) {
				Text("Name").fieldLabel().frame(width: 132, alignment: .leading)
				TextField(defaultName, text: $name)
					.font(.monoBody)
					.multilineTextAlignment(.trailing)
			}
		}
	}

	private var timingPanel: some View {
		Panel(title: "Timing") {
			Picker("When", selection: $anchorMode) {
				ForEach(AnchorMode.allCases) { Text($0.rawValue).tag($0) }
			}
			.pickerStyle(.segmented)

			if anchorMode == .readyBy {
				DatePicker("Dough on the bench", selection: $anchorDate)
					.font(.monoBody)
					.datePickerStyle(.compact)
			}

			Hairline()

			HStack(alignment: .top, spacing: 12) {
				StatTile(
					label: anchorMode == .readyBy ? "Start mixing" : "Mix now",
					value: schedule.start.formatted(.dateTime.weekday(.abbreviated).hour().minute()),
					caption: schedule.start < Date() && anchorMode == .readyBy
						? "That's in the past — pick a later time"
						: nil,
					tint: schedule.start < Date() && anchorMode == .readyBy ? Palette.hot : Palette.ink
				)
				StatTile(
					label: "Ready",
					value: schedule.readyAt.formatted(.dateTime.weekday(.abbreviated).hour().minute()),
					caption: Formatting.hours(plan.hoursToReady) + " start to finish"
				)
			}
		}
	}

	private var leavenPanel: some View {
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
		let current = formula.leaven == .sourdough
			? formula.prefermentedFlourPercent
			: formula.scoopedYeastPercent

		return Panel(title: "Leaven", trailing: Formatting.hours(load) + " load") {
			HStack(alignment: .top, spacing: 12) {
				StatTile(
					label: formula.leaven == .sourdough ? "Prefermented flour" : "Yeast — \(formula.yeastType.shortName)",
					value: Formatting.percent(current, places: 3),
					caption: "what this batch uses"
				)
				StatTile(
					label: "Suggested",
					value: Formatting.percent(suggested, places: 3),
					caption: "for this schedule",
					tint: Palette.inkMute
				)
			}

			if abs(suggested - current) > max(0.02, current * 0.35) {
				Text(current < suggested
					? "Less leaven than the model expects — a slower rise, more flavour, and less margin if your room runs cool."
					: "More leaven than the model expects — watch it, it'll be ready early if the room is warm.")
					.font(.mono(11))
					.foregroundStyle(Palette.inkMute)
			}

			BlockButton(title: "Use the suggestion", tint: Palette.ink, filled: false) {
				if formula.leaven == .sourdough {
					formula.prefermentedFlourPercent = suggested
				} else {
					formula.instantYeastPercent = suggested / formula.yeastType.multiplier
				}
			}
		}
	}

	private func summaryRow(title: String, detail: String) -> some View {
		Panel {
			HStack {
				VStack(alignment: .leading, spacing: 4) {
					Text(title).fieldLabel()
					Text(detail).font(.monoBody).foregroundStyle(Palette.ink)
				}
				Spacer()
				Image(systemName: "chevron.right").font(.mono(12)).foregroundStyle(Palette.inkMute)
			}
		}
	}

	private var defaultName: String {
		DoughStyle.style(id: styleID)?.name ?? "Batch"
	}

	private func start() {
		let batch = Batch(
			name: name.trimmingCharacters(in: .whitespaces).isEmpty ? defaultName : name,
			startAt: schedule.start,
			formula: formula,
			plan: plan,
			mixerCapacityKg: model.defaultMixerCapacityKg
		)
		model.add(batch)
		dismiss()
	}
}
