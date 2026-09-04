import DoughKit
import SwiftUI

/// The home screen: what's fermenting, what's due, what's next.
struct ProofingView: View {
	@Environment(AppModel.self) private var model
	@Binding var showingNewBatch: Bool

	var body: some View {
		ScrollView {
			// One ticking clock for the whole board rather than a timer per card.
			TimelineView(.periodic(from: .now, by: 1)) { context in
				LazyVStack(spacing: 14) {
					if !model.notificationsAuthorized {
						NotificationPromptCard()
					}

					if model.activeBatches.isEmpty {
						EmptyStateView(
							title: "Nothing proofing",
							message: "Start a batch and ProofLab will badge you when each stage is up — including overnight in the fridge."
						)
					}

					ForEach(model.activeBatches) { batch in
						NavigationLink(value: batch.id) {
							BatchCard(batch: batch, now: context.date)
						}
						.buttonStyle(.plain)
					}
				}
				.padding(16)
			}
		}
		.background(Palette.paper)
		.navigationTitle("Proofing")
		.navigationDestination(for: UUID.self) { id in
			if let batch = model.batch(id: id) {
				BatchDetailView(batchID: batch.id)
			}
		}
		.toolbar {
			ToolbarItem(placement: .primaryAction) {
				Button {
					showingNewBatch = true
				} label: {
					Label("New batch", systemImage: "plus")
				}
			}
		}
	}
}

private struct NotificationPromptCard: View {
	@Environment(AppModel.self) private var model

	var body: some View {
		Panel(title: "Alerts are off") {
			Text("Without notification permission ProofLab can't tell you when a stage is up — which is most of the point of it.")
				.font(.monoBody)
				.foregroundStyle(Palette.inkSoft)
			BlockButton(title: "Turn alerts on", tint: Palette.hot) {
				Task { await model.requestNotificationPermission() }
			}
		}
	}
}

struct BatchCard: View {
	@Environment(AppModel.self) private var model
	var batch: Batch
	var now: Date

	private var current: ScheduledStage? { batch.currentStage(at: now) }
	private var status: StageStatus { current.map { batch.status(of: $0, at: now) } ?? .done }
	private var due: [ScheduledStage] { batch.dueStages(at: now) }

	var body: some View {
		Panel {
			HStack(alignment: .firstTextBaseline) {
				Text(batch.name).font(.monoTitle).foregroundStyle(Palette.ink)
				Spacer(minLength: 8)
				if !due.isEmpty {
					BadgePill(text: due.count == 1 ? "1 due" : "\(due.count) due")
				}
			}

			Text("\(batch.plan.name) · \(batch.formula.ballCount) × \(Formatting.grams(batch.formula.ballWeightGrams))")
				.font(.mono(12))
				.foregroundStyle(Palette.inkMute)

			Hairline()

			if let current {
				HStack(alignment: .top, spacing: 14) {
					VStack(alignment: .leading, spacing: 4) {
						HStack(spacing: 6) {
							Image(systemName: current.stage.kind.symbolName)
								.font(.mono(12))
								.foregroundStyle(current.stage.kind.tint)
							Text(current.stage.title).font(.mono(15, .semibold))
						}
						Text(statusLine(for: current))
							.font(.mono(11))
							.foregroundStyle(status.tint)
					}
					Spacer(minLength: 8)
					VStack(alignment: .trailing, spacing: 2) {
						Text(Formatting.countdown(current.end.timeIntervalSince(now)))
							.font(.monoClock)
							.foregroundStyle(status.tint)
							.monospacedDigit()
						Text(status == .upcoming ? "until it starts" : "remaining")
							.font(.mono(10))
							.foregroundStyle(Palette.inkMute)
					}
				}

				ProgressBar(fraction: progress(of: current))
			} else {
				Text("All stages done — log it when you've tasted it.")
					.font(.monoBody)
					.foregroundStyle(Palette.inkMute)
			}

			Hairline()

			HStack {
				Label(readyLine, systemImage: "checkmark.circle")
					.font(.mono(11))
					.foregroundStyle(Palette.inkMute)
				Spacer()
				Text(Formatting.hours(batch.fermentationLoadHours) + " load")
					.font(.mono(11))
					.foregroundStyle(Palette.inkMute)
			}
		}
	}

	private var readyLine: String {
		"Ready " + batch.readyAt.formatted(.dateTime.weekday(.abbreviated).hour().minute())
	}

	private func statusLine(for stage: ScheduledStage) -> String {
		switch batch.status(of: stage, at: now) {
		case .upcoming: "Starts " + stage.start.formatted(.dateTime.hour().minute())
		case .active: "At " + Formatting.temperature(stage.stage.temperatureC, fahrenheit: model.useFahrenheit)
		case .due: "Timer's up"
		case .overdue: "Overdue — check it"
		case .done: "Done"
		}
	}

	private func progress(of stage: ScheduledStage) -> Double {
		let total = stage.end.timeIntervalSince(stage.start)
		guard total > 0 else { return 1 }
		return min(max(now.timeIntervalSince(stage.start) / total, 0), 1)
	}
}

struct ProgressBar: View {
	var fraction: Double
	var tint: Color = Palette.ink

	var body: some View {
		GeometryReader { geometry in
			ZStack(alignment: .leading) {
				Rectangle().fill(Palette.hairline)
				Rectangle().fill(tint).frame(width: geometry.size.width * min(max(fraction, 0), 1))
			}
		}
		.frame(height: 3)
	}
}
