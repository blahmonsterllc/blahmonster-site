import DoughKit
import SwiftUI

struct BatchDetailView: View {
	@Environment(AppModel.self) private var model
	let batchID: UUID

	@State private var showingMixSheet = false
	@State private var showingReview = false

	private var batch: Batch? { model.batch(id: batchID) }

	var body: some View {
		ScrollView {
			if let batch {
				TimelineView(.periodic(from: .now, by: 1)) { context in
					VStack(spacing: 14) {
						summary(batch, now: context.date)
						stages(batch, now: context.date)
						actions(batch)
					}
					.padding(16)
				}
			} else {
				EmptyStateView(title: "Gone", message: "This batch has been deleted.")
			}
		}
		.background(Palette.paper)
		.navigationTitle(batch?.name ?? "Batch")
		.navigationBarTitleDisplayMode(.inline)
		.toolbar {
			ToolbarItem(placement: .primaryAction) {
				Menu {
					Button("Mix sheet", systemImage: "list.clipboard") { showingMixSheet = true }
					Button("Rate this bake", systemImage: "star") { showingReview = true }
					if let batch {
						Button("Clone & tweak", systemImage: "doc.on.doc") {
							_ = model.clone(batch, startingAt: Date())
						}
						Divider()
						Button("Archive", systemImage: "archivebox") { model.archive(batch) }
						Button("Delete", systemImage: "trash", role: .destructive) { model.delete(batch) }
					}
				} label: {
					Label("More", systemImage: "ellipsis.circle")
				}
			}
		}
		.sheet(isPresented: $showingMixSheet) {
			if let batch {
				NavigationStack { MixSheetView(batch: batch) }
			}
		}
		.sheet(isPresented: $showingReview) {
			if let batch {
				NavigationStack { ReviewEditorView(batch: batch) }
			}
		}
	}

	// MARK: - Sections

	@ViewBuilder
	private func summary(_ batch: Batch, now: Date) -> some View {
		let due = batch.dueStages(at: now)

		Panel(title: "This run", trailing: batch.plan.name) {
			if !due.isEmpty {
				HStack(spacing: 8) {
					BadgePill(text: due.count == 1 ? "1 stage due" : "\(due.count) stages due")
					Spacer()
				}
			}

			HStack(alignment: .top, spacing: 12) {
				StatTile(
					label: "Ready",
					value: batch.readyAt.formatted(.dateTime.weekday(.abbreviated).hour().minute()),
					caption: "mixed " + batch.startAt.formatted(.dateTime.weekday(.abbreviated).hour().minute())
				)
				StatTile(
					label: "Ferment load",
					value: Formatting.hours(batch.fermentationLoadHours),
					caption: "equivalent hours at 24 °C"
				)
			}

			Hairline()

			HStack(alignment: .top, spacing: 12) {
				if batch.formula.leaven == .sourdough {
					StatTile(
						label: "Prefermented flour",
						value: Formatting.percent(batch.formula.prefermentedFlourPercent, places: 1),
						caption: "suggested " + Formatting.percent(suggestedPrefermentedFlour(batch), places: 1)
					)
				} else {
					StatTile(
						label: "Yeast (\(batch.formula.yeastType.shortName))",
						value: Formatting.percent(batch.formula.scoopedYeastPercent, places: 3),
						caption: "suggested " + Formatting.percent(
							batch.suggestedInstantYeastPercent * batch.formula.yeastType.multiplier,
							places: 3
						)
					)
				}
				StatTile(
					label: "Dough",
					value: "\(batch.formula.ballCount) × \(Formatting.grams(batch.formula.ballWeightGrams))",
					caption: Formatting.grams(batch.formula.result().totalDoughGrams) + " total"
				)
			}
		}
	}

	/// The model suggests a levain *weight* as a share of flour; the formula is expressed in
	/// prefermented flour. At 100 % hydration a levain is half flour, so convert rather than
	/// showing two numbers that look contradictory.
	private func suggestedPrefermentedFlour(_ batch: Batch) -> Double {
		batch.suggestedLevainPercent / (1 + batch.formula.prefermentHydrationPercent / 100)
	}

	@ViewBuilder
	private func stages(_ batch: Batch, now: Date) -> some View {
		Panel(title: "Schedule") {
			VStack(spacing: 0) {
				ForEach(Array(batch.timeline.enumerated()), id: \.element.id) { index, stage in
					if index > 0 { Hairline() }
					StageRow(
						batch: batch,
						stage: stage,
						now: now,
						fahrenheit: model.useFahrenheit,
						onComplete: { model.complete(stageID: stage.id, in: batch.id) },
						onReopen: { model.reopen(stageID: stage.id, in: batch.id) },
						onExtend: { model.adjust(stageID: stage.id, in: batch.id, byHours: 0.25) },
						onShorten: { model.adjust(stageID: stage.id, in: batch.id, byHours: -0.25) }
					)
					.padding(.vertical, 10)
				}
			}
		}
	}

	@ViewBuilder
	private func actions(_ batch: Batch) -> some View {
		Panel(title: "Notes") {
			TextEditor(text: Binding(
				get: { batch.notes },
				set: { model.setNotes($0, for: batch.id) }
			))
			.font(.monoBody)
			.frame(minHeight: 90)
			.scrollContentBackground(.hidden)
			.overlay(alignment: .topLeading) {
				if batch.notes.isEmpty {
					Text("What did you change this time?")
						.font(.monoBody)
						.foregroundStyle(Palette.inkMute)
						.padding(.top, 8)
						.padding(.leading, 5)
						.allowsHitTesting(false)
				}
			}
		}
	}
}

struct StageRow: View {
	var batch: Batch
	var stage: ScheduledStage
	var now: Date
	var fahrenheit: Bool
	var onComplete: () -> Void
	var onReopen: () -> Void
	var onExtend: () -> Void
	var onShorten: () -> Void

	private var status: StageStatus { batch.status(of: stage, at: now) }

	var body: some View {
		VStack(alignment: .leading, spacing: 8) {
			HStack(alignment: .firstTextBaseline, spacing: 8) {
				Image(systemName: stage.stage.kind.symbolName)
					.font(.mono(12))
					.foregroundStyle(stage.stage.kind.tint)
					.frame(width: 18)

				VStack(alignment: .leading, spacing: 2) {
					Text(stage.stage.title)
						.font(.mono(14, .semibold))
						.strikethrough(status == .done, color: Palette.inkMute)
						.foregroundStyle(status == .done ? Palette.inkMute : Palette.ink)
					Text(subtitle)
						.font(.mono(11))
						.foregroundStyle(Palette.inkMute)
				}

				Spacer(minLength: 8)

				VStack(alignment: .trailing, spacing: 2) {
					Text(timeText)
						.font(.mono(13, .medium))
						.monospacedDigit()
						.foregroundStyle(status.tint)
					BadgePill(text: status.label, tint: status.tint, filled: status == .due || status == .overdue)
				}
			}

			if let window = stage.windowEnd, status != .done {
				Text("Usable until " + window.formatted(.dateTime.weekday(.abbreviated).hour().minute()))
					.font(.mono(11))
					.foregroundStyle(Palette.cold)
			}

			if !stage.foldTimes.isEmpty, status == .active || status == .upcoming {
				Text("Folds: " + stage.foldTimes.map { $0.formatted(date: .omitted, time: .shortened) }.joined(separator: ", "))
					.font(.mono(11))
					.foregroundStyle(Palette.inkMute)
			}

			if status != .upcoming {
				HStack(spacing: 8) {
					if status == .done {
						Button("Reopen", action: onReopen).buttonStyle(.bordered)
					} else {
						Button("Done", action: onComplete).buttonStyle(.borderedProminent)
						Button("−15m", action: onShorten).buttonStyle(.bordered)
						Button("+15m", action: onExtend).buttonStyle(.bordered)
					}
					Spacer()
				}
				.font(.mono(12))
				.controlSize(.small)
			}
		}
	}

	private var subtitle: String {
		var parts = [
			stage.start.formatted(.dateTime.weekday(.abbreviated).hour().minute()),
			Formatting.temperature(stage.stage.temperatureC, fahrenheit: fahrenheit)
		]
		if stage.stage.kind.countsTowardFermentation {
			parts.append(Formatting.hours(stage.stage.equivalentHours) + " eq")
		}
		return parts.joined(separator: " · ")
	}

	private var timeText: String {
		switch status {
		case .done: "—"
		case .upcoming: Formatting.hours(stage.stage.hours)
		default: Formatting.countdown(stage.end.timeIntervalSince(now))
		}
	}
}
