import DoughKit
import SwiftUI

/// Editorial, monospaced, hairline rules — the same visual language as blahmonster.com.
/// Colours are defined in code rather than the asset catalog so light and dark stay in one
/// place and can't drift apart.
enum Palette {
	private static func dynamic(light: UInt32, dark: UInt32) -> Color {
		Color(uiColor: UIColor { traits in
			UIColor(hex: traits.userInterfaceStyle == .dark ? dark : light)
		})
	}

	static let paper = dynamic(light: 0xF4F4EE, dark: 0x121210)
	static let card = dynamic(light: 0xFFFFFF, dark: 0x1B1B18)
	static let ink = dynamic(light: 0x0A0A0A, dark: 0xF2F2EC)
	static let inkSoft = dynamic(light: 0x2A2A26, dark: 0xCFCFC7)
	static let inkMute = dynamic(light: 0x7A7A72, dark: 0x8B8B82)
	static let rule = dynamic(light: 0x0A0A0A, dark: 0x3A3A34)
	static let hairline = dynamic(light: 0xD8D8CE, dark: 0x2E2E29)
	static let hot = dynamic(light: 0xFF2D00, dark: 0xFF5630)
	static let cold = dynamic(light: 0x1D6FB8, dark: 0x63A8E8)
	static let go = dynamic(light: 0x1F8C2E, dark: 0x4FBF60)
}

extension UIColor {
	convenience init(hex: UInt32) {
		self.init(
			red: CGFloat((hex >> 16) & 0xFF) / 255,
			green: CGFloat((hex >> 8) & 0xFF) / 255,
			blue: CGFloat(hex & 0xFF) / 255,
			alpha: 1
		)
	}
}

extension Font {
	static func mono(_ size: CGFloat, _ weight: Font.Weight = .regular) -> Font {
		.system(size: size, weight: weight, design: .monospaced)
	}

	/// Small uppercase field labels.
	static let monoLabel = Font.mono(11, .medium)
	static let monoBody = Font.mono(14)
	static let monoTitle = Font.mono(20, .semibold)
	/// Countdowns need fixed-width digits so they don't jitter every second.
	static let monoClock = Font.system(size: 34, weight: .medium, design: .monospaced)
}

extension Text {
	/// The tiny tracked-out uppercase label used throughout the app.
	func fieldLabel() -> some View {
		self
			.font(.monoLabel)
			.tracking(0.6)
			.textCase(.uppercase)
			.foregroundStyle(Palette.inkMute)
	}
}

extension StageKind {
	var tint: Color {
		switch self {
		case .coldRetard: Palette.cold
		case .bake: Palette.hot
		case .preferment: Palette.go
		default: Palette.ink
		}
	}
}

extension StageStatus {
	var tint: Color {
		switch self {
		case .overdue, .due: Palette.hot
		case .active: Palette.go
		case .done, .upcoming: Palette.inkMute
		}
	}

	var label: String {
		switch self {
		case .overdue: "Overdue"
		case .due: "Due"
		case .active: "Running"
		case .done: "Done"
		case .upcoming: "Waiting"
		}
	}
}
