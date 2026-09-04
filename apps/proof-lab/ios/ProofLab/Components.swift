import DoughKit
import SwiftUI

/// A hairline-ruled panel. Everything in the app sits in one of these.
struct Panel<Content: View>: View {
	var title: String?
	var trailing: String?
	@ViewBuilder var content: Content

	var body: some View {
		VStack(alignment: .leading, spacing: 12) {
			if title != nil || trailing != nil {
				HStack(alignment: .firstTextBaseline) {
					if let title { Text(title).fieldLabel() }
					Spacer(minLength: 8)
					if let trailing {
						Text(trailing).font(.monoLabel).foregroundStyle(Palette.inkMute)
					}
				}
			}
			content
		}
		.padding(16)
		.frame(maxWidth: .infinity, alignment: .leading)
		.background(Palette.card)
		.overlay(Rectangle().strokeBorder(Palette.hairline, lineWidth: 1))
	}
}

/// The count-of-things chip: due stages, overdue, mixes.
struct BadgePill: View {
	var text: String
	var tint: Color = Palette.hot
	var filled = true

	var body: some View {
		Text(text)
			.font(.mono(11, .semibold))
			.tracking(0.4)
			.textCase(.uppercase)
			.padding(.horizontal, 7)
			.padding(.vertical, 3)
			.foregroundStyle(filled ? Palette.paper : tint)
			.background(filled ? tint : Color.clear)
			.overlay(Rectangle().strokeBorder(tint, lineWidth: filled ? 0 : 1))
	}
}

struct StatTile: View {
	var label: String
	var value: String
	var caption: String?
	var tint: Color = Palette.ink

	var body: some View {
		VStack(alignment: .leading, spacing: 4) {
			Text(label).fieldLabel()
			Text(value)
				.font(.mono(17, .semibold))
				.foregroundStyle(tint)
				.minimumScaleFactor(0.7)
				.lineLimit(1)
			if let caption {
				Text(caption).font(.mono(11)).foregroundStyle(Palette.inkMute).lineLimit(2)
			}
		}
		.frame(maxWidth: .infinity, alignment: .leading)
	}
}

struct Hairline: View {
	var body: some View {
		Rectangle().fill(Palette.hairline).frame(height: 1)
	}
}

/// Numeric entry that keeps its own text so a half-typed "6." doesn't get rewritten
/// under the cursor, and commits on every valid keystroke.
struct NumberField: View {
	var label: String
	var suffix: String?
	@Binding var value: Double
	var range: ClosedRange<Double> = 0...10_000
	var decimals: Int = 2

	@State private var text: String = ""
	@FocusState private var focused: Bool

	var body: some View {
		HStack(alignment: .firstTextBaseline, spacing: 8) {
			Text(label).fieldLabel().frame(width: 132, alignment: .leading)
			TextField("", text: $text)
				.font(.monoBody)
				.keyboardType(.decimalPad)
				.multilineTextAlignment(.trailing)
				.focused($focused)
				.onChange(of: text) { _, new in
					guard let parsed = Double(new.replacingOccurrences(of: ",", with: ".")) else { return }
					value = min(max(parsed, range.lowerBound), range.upperBound)
				}
				.onChange(of: focused) { _, isFocused in
					if !isFocused { text = Formatting.rounded(value, places: decimals) }
				}
			if let suffix {
				Text(suffix).font(.mono(12)).foregroundStyle(Palette.inkMute).frame(width: 26, alignment: .leading)
			}
		}
		.onAppear { text = Formatting.rounded(value, places: decimals) }
		.onChange(of: value) { _, new in
			// Keep in step when something else edits the value, but never fight the keyboard.
			guard !focused else { return }
			text = Formatting.rounded(new, places: decimals)
		}
	}
}

struct IntField: View {
	var label: String
	@Binding var value: Int
	var range: ClosedRange<Int> = 0...100_000

	var body: some View {
		HStack(alignment: .firstTextBaseline, spacing: 8) {
			Text(label).fieldLabel().frame(width: 132, alignment: .leading)
			Stepper(
				value: Binding(
					get: { value },
					set: { value = min(max($0, range.lowerBound), range.upperBound) }
				),
				in: range
			) {
				Text("\(value)").font(.monoBody).frame(maxWidth: .infinity, alignment: .trailing)
			}
		}
	}
}

/// A weight row on the mix sheet.
struct IngredientRow: View {
	var ingredient: Ingredient
	var showPercent = true

	var body: some View {
		HStack(alignment: .firstTextBaseline) {
			Text(ingredient.name).font(.monoBody).foregroundStyle(Palette.inkSoft)
			Spacer(minLength: 8)
			if showPercent, ingredient.bakersPercent > 0 {
				Text(Formatting.percent(ingredient.bakersPercent, places: 2))
					.font(.mono(11))
					.foregroundStyle(Palette.inkMute)
			}
			Text(Formatting.grams(ingredient.grams))
				.font(.mono(14, .semibold))
				.frame(minWidth: 78, alignment: .trailing)
		}
	}
}

/// Primary action button, flat and boxy to match the site.
struct BlockButton: View {
	var title: String
	var tint: Color = Palette.ink
	var filled = true
	var action: () -> Void

	var body: some View {
		Button(action: action) {
			Text(title)
				.font(.mono(13, .semibold))
				.tracking(0.4)
				.textCase(.uppercase)
				.frame(maxWidth: .infinity)
				.padding(.vertical, 12)
				.foregroundStyle(filled ? Palette.paper : tint)
				.background(filled ? tint : Color.clear)
				.overlay(Rectangle().strokeBorder(tint, lineWidth: filled ? 0 : 1))
		}
		.buttonStyle(.plain)
	}
}

struct EmptyStateView: View {
	var title: String
	var message: String

	var body: some View {
		VStack(spacing: 8) {
			Text(title).font(.monoTitle)
			Text(message)
				.font(.monoBody)
				.foregroundStyle(Palette.inkMute)
				.multilineTextAlignment(.center)
		}
		.frame(maxWidth: .infinity)
		.padding(.vertical, 48)
	}
}
