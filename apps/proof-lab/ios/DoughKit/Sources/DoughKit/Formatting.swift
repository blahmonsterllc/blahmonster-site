import Foundation

/// Display helpers shared by every view, kept next to the model so the Android port has
/// something to match string-for-string.
public enum Formatting {
	public static func cToF(_ c: Double) -> Double { c * 1.8 + 32 }
	public static func fToC(_ f: Double) -> Double { (f - 32) / 1.8 }

	public static func temperature(_ c: Double, fahrenheit: Bool = false) -> String {
		fahrenheit
			? "\(Int(round(cToF(c))))°F"
			: "\(rounded(c, places: c.truncatingRemainder(dividingBy: 1) == 0 ? 0 : 1))°C"
	}

	/// "3h 20m", "45m", "2d 4h".
	public static func hours(_ hours: Double) -> String {
		guard hours.isFinite else { return "—" }
		let totalMinutes = Int((hours * 60).rounded())
		let days = totalMinutes / 1440
		let h = (totalMinutes - days * 1440) / 60
		let m = totalMinutes - days * 1440 - h * 60
		var parts: [String] = []
		if days > 0 { parts.append("\(days)d") }
		if h > 0 { parts.append("\(h)h") }
		if m > 0 || parts.isEmpty { parts.append("\(m)m") }
		return parts.joined(separator: " ")
	}

	/// Countdown form: "01:59:04", or "2d 03:12" past a day. Negative reads "+00:04:12".
	public static func countdown(_ interval: TimeInterval) -> String {
		let negative = interval < 0
		let total = Int(abs(interval))
		let days = total / 86_400
		let h = (total % 86_400) / 3600
		let m = (total % 3600) / 60
		let s = total % 60
		func pad(_ value: Int) -> String { String(format: "%02d", value) }
		let body = days > 0 ? "\(days)d \(pad(h)):\(pad(m))" : "\(pad(h)):\(pad(m)):\(pad(s))"
		return negative ? "+\(body)" : body
	}

	public static func grams(_ grams: Double) -> String {
		if grams >= 10_000 { return "\(rounded(grams / 1000, places: 2)) kg" }
		if grams >= 1000 { return "\(rounded(grams / 1000, places: 3)) kg" }
		if grams >= 100 { return "\(rounded(grams, places: 0)) g" }
		if grams >= 10 { return "\(rounded(grams, places: 1)) g" }
		return "\(rounded(grams, places: 2)) g"
	}

	public static func percent(_ value: Double, places: Int = 2) -> String {
		"\(rounded(value, places: places)) %"
	}

	public static func rounded(_ value: Double, places: Int) -> String {
		let formatted = String(format: "%.\(max(0, places))f", value)
		guard formatted.contains(".") else { return formatted }
		var trimmed = formatted
		while trimmed.hasSuffix("0") { trimmed.removeLast() }
		if trimmed.hasSuffix(".") { trimmed.removeLast() }
		return trimmed
	}
}
