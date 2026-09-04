import DoughKit
import SwiftUI

struct ReviewEditorView: View {
	@Environment(AppModel.self) private var model
	@Environment(\.dismiss) private var dismiss

	let batch: Batch
	@State private var review = BatchReview()
	@State private var loaded = false

	var body: some View {
		ScrollView {
			VStack(spacing: 14) {
				Panel(title: "How was it?") {
					score("Handling", \.handling)
					Hairline()
					score("Extensibility", \.extensibility)
					Hairline()
					score("Oven spring", \.ovenSpring)
					Hairline()
					score("Crumb", \.crumb)
					Hairline()
					score("Flavour", \.flavor)
					Hairline()
					score("Crust", \.crust)
					Hairline()
					score("Overall", \.overall)
				}

				Panel(title: "Notes") {
					TextEditor(text: $review.notes)
						.font(.monoBody)
						.frame(minHeight: 120)
						.scrollContentBackground(.hidden)
					Hairline()
					Toggle(isOn: $review.wouldRepeat) {
						Text("Worth making again").font(.monoBody)
					}
				}

				BlockButton(title: "Save", tint: Palette.hot) {
					model.setReview(review, for: batch.id)
					dismiss()
				}
			}
			.padding(16)
		}
		.background(Palette.paper)
		.navigationTitle("Rate this bake")
		.navigationBarTitleDisplayMode(.inline)
		.toolbar {
			ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
		}
		.onAppear {
			guard !loaded else { return }
			review = batch.review ?? BatchReview()
			loaded = true
		}
	}

	private func score(_ label: String, _ keyPath: WritableKeyPath<BatchReview, Int>) -> some View {
		HStack {
			Text(label).fieldLabel().frame(width: 110, alignment: .leading)
			Spacer()
			ForEach(1...5, id: \.self) { value in
				Button {
					review[keyPath: keyPath] = value
				} label: {
					Rectangle()
						.fill(review[keyPath: keyPath] >= value ? Palette.ink : Palette.hairline)
						.frame(width: 26, height: 22)
						.overlay(
							Text("\(value)")
								.font(.mono(11, .semibold))
								.foregroundStyle(review[keyPath: keyPath] >= value ? Palette.paper : Palette.inkMute)
						)
				}
				.buttonStyle(.plain)
			}
		}
	}
}
