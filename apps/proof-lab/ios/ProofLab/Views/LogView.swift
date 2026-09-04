import DoughKit
import SwiftUI

/// The prototyping record. Bakes you've finished, what you scored them, and a diff between
/// any two so you can see what actually changed.
struct LogView: View {
	@Environment(AppModel.self) private var model
	@State private var comparing: Set<UUID> = []

	private var batches: [Batch] { model.loggedBatches }

	private var comparison: BatchComparison? {
		let selected = batches.filter { comparing.contains($0.id) }
		guard selected.count == 2 else { return nil }
		return BatchComparison(left: selected[0], right: selected[1])
	}

	var body: some View {
		ScrollView {
			VStack(spacing: 14) {
				if batches.isEmpty {
					EmptyStateView(
						title: "No bakes logged",
						message: "Finish a batch and rate it, and it'll show up here to compare against the next one."
					)
				}

				if let comparison {
					ComparisonPanel(comparison: comparison)
				} else if comparing.count == 1 {
					Panel {
						Text("Pick a second bake to compare against.")
							.font(.mono(12))
							.foregroundStyle(Palette.inkMute)
					}
				}

				ForEach(batches) { batch in
					LogRow(
						batch: batch,
						selected: comparing.contains(batch.id),
						onToggle: { toggle(batch) }
					)
				}
			}
			.padding(16)
		}
		.background(Palette.paper)
		.navigationTitle("Log")
		.navigationDestination(for: UUID.self) { id in
			if model.batch(id: id) != nil {
				BatchDetailView(batchID: id)
			}
		}
	}

	private func toggle(_ batch: Batch) {
		if comparing.contains(batch.id) {
			comparing.remove(batch.id)
		} else {
			// Only ever two at a time; the third selection replaces the oldest.
			if comparing.count >= 2, let first = comparing.first { comparing.remove(first) }
			comparing.insert(batch.id)
		}
	}
}

private struct LogRow: View {
	@Environment(AppModel.self) private var model
	var batch: Batch
	var selected: Bool
	var onToggle: () -> Void

	var body: some View {
		Panel {
			HStack(alignment: .firstTextBaseline) {
				NavigationLink(value: batch.id) {
					VStack(alignment: .leading, spacing: 4) {
						Text(batch.name).font(.mono(16, .semibold)).foregroundStyle(Palette.ink)
						Text(batch.createdAt.formatted(date: .abbreviated, time: .shortened))
							.font(.mono(11))
							.foregroundStyle(Palette.inkMute)
					}
				}
				.buttonStyle(.plain)

				Spacer()

				if let review = batch.review {
					BadgePill(text: "\(review.overall)/5", tint: Palette.ink, filled: false)
				}

				Button(action: onToggle) {
					Image(systemName: selected ? "checkmark.square.fill" : "square")
						.foregroundStyle(selected ? Palette.hot : Palette.inkMute)
				}
				.buttonStyle(.plain)
			}

			Hairline()

			Text(batch.plan.name + " · " + batch.formula.blend.summary)
				.font(.mono(11))
				.foregroundStyle(Palette.inkMute)

			HStack {
				Text(Formatting.hours(batch.fermentationLoadHours) + " load")
					.font(.mono(11))
					.foregroundStyle(Palette.inkMute)
				Spacer()
				Text(Formatting.percent(batch.formula.hydrationPercent, places: 1) + " hydration")
					.font(.mono(11))
					.foregroundStyle(Palette.inkMute)
			}

			if !batch.notes.isEmpty {
				Text(batch.notes).font(.mono(12)).foregroundStyle(Palette.inkSoft).lineLimit(3)
			}

			HStack(spacing: 8) {
				Button("Clone & tweak") {
					_ = model.clone(batch, startingAt: Date())
				}
				.buttonStyle(.bordered)
				if batch.isArchived {
					Button("Unarchive") { model.unarchive(batch) }.buttonStyle(.bordered)
				}
				Spacer()
			}
			.font(.mono(12))
			.controlSize(.small)
		}
	}
}

struct ComparisonPanel: View {
	var comparison: BatchComparison

	var body: some View {
		Panel(title: "Compare", trailing: "\(comparison.changedRows.count) differences") {
			HStack {
				Text(comparison.left.name).font(.mono(12, .semibold)).frame(maxWidth: .infinity, alignment: .leading)
				Text(comparison.right.name).font(.mono(12, .semibold)).frame(maxWidth: .infinity, alignment: .trailing)
			}
			Hairline()
			ForEach(comparison.rows) { row in
				HStack(alignment: .firstTextBaseline) {
					Text(row.left)
						.font(.mono(12, row.changed ? .semibold : .regular))
						.frame(maxWidth: .infinity, alignment: .leading)
					Text(row.label)
						.font(.mono(10))
						.foregroundStyle(Palette.inkMute)
						.frame(width: 96)
					Text(row.right)
						.font(.mono(12, row.changed ? .semibold : .regular))
						.frame(maxWidth: .infinity, alignment: .trailing)
				}
				.foregroundStyle(row.changed ? Palette.ink : Palette.inkMute)
				.padding(.vertical, 2)
			}
		}
	}
}
