import Foundation

/// One sample from a bench rig.
///
/// Every field but the timestamp is optional, because a rig gets built up a sensor at a time and
/// a half-instrumented run is still worth logging. Heights are the dough's own height, not the
/// sensor's distance reading — a lid-mounted rangefinder counts *down* as the dough rises, so
/// that conversion belongs in the firmware where the container geometry is known.
public struct SensorReading: Codable, Sendable, Equatable {
	public var date: Date
	/// Dough core temperature — what actually drives fermentation.
	public var doughTempC: Double?
	/// Air around the dough. Used as a fallback when there's no probe in the dough.
	public var ambientTempC: Double?
	public var relativeHumidity: Double?
	public var doughHeightMm: Double?
	public var co2Ppm: Double?
	/// Total mass. Falls slowly as CO₂ and water leave — an independent check on activity.
	public var massGrams: Double?

	public init(
		date: Date,
		doughTempC: Double? = nil,
		ambientTempC: Double? = nil,
		relativeHumidity: Double? = nil,
		doughHeightMm: Double? = nil,
		co2Ppm: Double? = nil,
		massGrams: Double? = nil
	) {
		self.date = date
		self.doughTempC = doughTempC
		self.ambientTempC = ambientTempC
		self.relativeHumidity = relativeHumidity
		self.doughHeightMm = doughHeightMm
		self.co2Ppm = co2Ppm
		self.massGrams = massGrams
	}

	/// The dough's own temperature if we have it, otherwise the air's.
	public var effectiveTempC: Double? { doughTempC ?? ambientTempC }
}

/// A run's worth of samples.
///
/// This is deliberately descriptive rather than predictive. It measures what happened and
/// projects a trend forward; it does not claim to know when the dough is ready. That threshold
/// is what the rig exists to discover, and hard-coding a guess for it now would defeat the point.
public struct SensorSeries: Codable, Sendable, Equatable {
	public var readings: [SensorReading]

	public init(_ readings: [SensorReading]) {
		self.readings = readings
	}

	/// Samples in time order, which is the only order any of this makes sense in.
	public var ordered: [SensorReading] {
		readings.sorted { $0.date < $1.date }
	}

	public var isEmpty: Bool { readings.isEmpty }

	public var start: Date? { ordered.first?.date }
	public var end: Date? { ordered.last?.date }

	public var elapsedHours: Double {
		guard let start, let end else { return 0 }
		return end.timeIntervalSince(start) / 3600
	}

	/// Fermentation actually accumulated, from measured temperature rather than an assumed one.
	///
	/// This is the whole reason to put a probe in the dough: the schedule assumes the walk-in is
	/// 4 °C, and if it's really 5.8 °C every equivalent-hour figure downstream is wrong. Rates
	/// are integrated by trapezoid between samples, which at any sane logging interval is far
	/// more accurate than the temperature model's own uncertainty.
	public func measuredEquivalentHours() -> Double {
		let points: [(Date, Double)] = ordered.compactMap { reading in
			reading.effectiveTempC.map { (reading.date, $0) }
		}
		guard points.count >= 2 else { return 0 }

		var total = 0.0
		for index in 0..<(points.count - 1) {
			let (dateA, tempA) = points[index]
			let (dateB, tempB) = points[index + 1]
			let hours = dateB.timeIntervalSince(dateA) / 3600
			guard hours > 0 else { continue }
			let rateA = Fermentation.rateMultiplier(atC: tempA)
			let rateB = Fermentation.rateMultiplier(atC: tempB)
			total += hours * (rateA + rateB) / 2
		}
		return total
	}

	/// The longest stretch with no usable temperature sample. Gaps are integrated straight
	/// across on the assumption temperature moved linearly — the dough kept fermenting either
	/// way — but a long one means the figure is an estimate, and the caller should say so.
	public func longestTemperatureGapMinutes() -> Double {
		let dates = ordered.filter { $0.effectiveTempC != nil }.map(\.date)
		guard dates.count >= 2 else { return 0 }
		var longest: TimeInterval = 0
		for index in 0..<(dates.count - 1) {
			longest = max(longest, dates[index + 1].timeIntervalSince(dates[index]))
		}
		return longest / 60
	}

	public func averageTemperatureC() -> Double? {
		let temps = ordered.compactMap(\.effectiveTempC)
		guard !temps.isEmpty else { return nil }
		return temps.reduce(0, +) / Double(temps.count)
	}

	/// The temperature a constant-temperature run would have needed to ferment this much in this
	/// time. Not the arithmetic mean — a dough that spent an hour at 30 °C and an hour at 10 °C
	/// is further along than one held at 20 °C the whole time.
	public func effectiveConstantTemperatureC() -> Double? {
		let hours = elapsedHours
		guard hours > 0 else { return nil }
		let equivalent = measuredEquivalentHours()
		guard equivalent > 0 else { return nil }
		let targetRate = equivalent / hours

		// The rate curve is monotonic in temperature, so bisect it.
		var low = Fermentation.minTemperatureC
		var high = Fermentation.maxTemperatureC
		for _ in 0..<60 {
			let mid = (low + high) / 2
			if Fermentation.rateMultiplier(atC: mid) < targetRate { low = mid } else { high = mid }
		}
		return (low + high) / 2
	}

	// MARK: - Rise

	public var firstHeightMm: Double? {
		ordered.first { $0.doughHeightMm != nil }?.doughHeightMm
	}

	public var latestHeightMm: Double? {
		ordered.last { $0.doughHeightMm != nil }?.doughHeightMm
	}

	/// Current height as a multiple of where it started. 1.75 means "risen 75 %".
	public func expansionRatio(baselineMm: Double? = nil) -> Double? {
		guard let baseline = baselineMm ?? firstHeightMm, baseline > 0 else { return nil }
		guard let latest = latestHeightMm else { return nil }
		return latest / baseline
	}

	public func expansionPercent(baselineMm: Double? = nil) -> Double? {
		expansionRatio(baselineMm: baselineMm).map { ($0 - 1) * 100 }
	}

	/// How fast it's rising right now, in percent of the baseline height per hour, measured over
	/// the last `windowMinutes`. Nil when there aren't two height samples in the window.
	public func riseRatePercentPerHour(
		windowMinutes: Double = 45,
		baselineMm: Double? = nil
	) -> Double? {
		guard let baseline = baselineMm ?? firstHeightMm, baseline > 0 else { return nil }
		guard let end else { return nil }
		let cutoff = end.addingTimeInterval(-windowMinutes * 60)
		let window = ordered.filter { $0.date >= cutoff && $0.doughHeightMm != nil }
		guard window.count >= 2, let first = window.first, let last = window.last else { return nil }

		let hours = last.date.timeIntervalSince(first.date) / 3600
		guard hours > 0, let a = first.doughHeightMm, let b = last.doughHeightMm else { return nil }
		return ((b - a) / baseline * 100) / hours
	}

	/// Straight-line extrapolation to a target expansion, in hours from the last sample.
	///
	/// Rise is not linear — it accelerates, then flattens as the gluten gives out — so this is a
	/// rough steer, not a prediction, and it deliberately refuses to answer when the dough isn't
	/// currently rising.
	public func projectedHoursTo(
		ratio targetRatio: Double,
		windowMinutes: Double = 45,
		baselineMm: Double? = nil
	) -> Double? {
		guard let current = expansionRatio(baselineMm: baselineMm) else { return nil }
		if current >= targetRatio { return 0 }
		guard let rate = riseRatePercentPerHour(windowMinutes: windowMinutes, baselineMm: baselineMm),
			  rate > 0 else { return nil }
		return (targetRatio - current) * 100 / rate
	}

	// MARK: - Other channels

	public func latestCo2Ppm() -> Double? {
		ordered.last { $0.co2Ppm != nil }?.co2Ppm
	}

	/// CO₂ slope over the window — the most direct read on how hard the yeast is working.
	public func co2SlopePpmPerHour(windowMinutes: Double = 45) -> Double? {
		slopePerHour(windowMinutes: windowMinutes) { $0.co2Ppm }
	}

	/// Mass loss per hour. CO₂ and water leaving a covered-but-not-sealed container show up here,
	/// which makes it an independent check on the gas reading — and it needs no headspace model.
	public func massLossGramsPerHour(windowMinutes: Double = 60) -> Double? {
		slopePerHour(windowMinutes: windowMinutes) { $0.massGrams }.map { -$0 }
	}

	public func latestRelativeHumidity() -> Double? {
		ordered.last { $0.relativeHumidity != nil }?.relativeHumidity
	}

	private func slopePerHour(
		windowMinutes: Double,
		value: (SensorReading) -> Double?
	) -> Double? {
		guard let end else { return nil }
		let cutoff = end.addingTimeInterval(-windowMinutes * 60)
		let window = ordered.filter { $0.date >= cutoff && value($0) != nil }
		guard window.count >= 2, let first = window.first, let last = window.last else { return nil }
		let hours = last.date.timeIntervalSince(first.date) / 3600
		guard hours > 0, let a = value(first), let b = value(last) else { return nil }
		return (b - a) / hours
	}

	public func appending(_ reading: SensorReading) -> SensorSeries {
		SensorSeries(readings + [reading])
	}

	/// Everything from `from` to `to` inclusive — used to score one stage out of a whole run.
	public func slice(from: Date, to: Date) -> SensorSeries {
		SensorSeries(ordered.filter { $0.date >= from && $0.date <= to })
	}
}

/// The wire format between the rig and the app.
///
/// Plain CSV on purpose: it opens in anything, survives a firmware rewrite, and can be read by
/// eye when a run looks wrong. Blank means "no sensor for this on that run", not zero.
public enum SensorCsv {
	public static let header = "timestamp_ms,dough_c,ambient_c,rh,height_mm,co2_ppm,mass_g"

	public static func encode(_ series: SensorSeries) -> String {
		var lines = [header]
		for reading in series.ordered {
			func cell(_ value: Double?) -> String {
				value.map { String($0) } ?? ""
			}
			let millis = Int64((reading.date.timeIntervalSince1970 * 1000).rounded())
			lines.append(
				[
					String(millis),
					cell(reading.doughTempC),
					cell(reading.ambientTempC),
					cell(reading.relativeHumidity),
					cell(reading.doughHeightMm),
					cell(reading.co2Ppm),
					cell(reading.massGrams)
				].joined(separator: ",")
			)
		}
		return lines.joined(separator: "\n") + "\n"
	}

	/// Lenient by design. A rig that drops a field, writes a partial last line on power loss, or
	/// emits a stray comment shouldn't cost you the run.
	public static func decode(_ text: String) -> SensorSeries {
		let readings: [SensorReading] = text
			.split(separator: "\n", omittingEmptySubsequences: false)
			.map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
			.filter { !$0.isEmpty && !$0.hasPrefix("#") }
			.filter { !$0.lowercased().hasPrefix("timestamp") }
			.compactMap { line in
				let cells = line.split(separator: ",", omittingEmptySubsequences: false)
					.map { $0.trimmingCharacters(in: .whitespaces) }
				guard let first = cells.first, let millis = Int64(first) else { return nil }
				func cell(_ index: Int) -> Double? {
					guard index < cells.count, !cells[index].isEmpty else { return nil }
					return Double(cells[index])
				}
				return SensorReading(
					date: Date(timeIntervalSince1970: Double(millis) / 1000),
					doughTempC: cell(1),
					ambientTempC: cell(2),
					relativeHumidity: cell(3),
					doughHeightMm: cell(4),
					co2Ppm: cell(5),
					massGrams: cell(6)
				)
			}
		return SensorSeries(readings)
	}
}

/// What a run looked like against what the plan expected.
///
/// The gap between planned and measured is the finding. A cold retard that was supposed to bank
/// 7.5 equivalent hours and actually banked 9.4 is why Friday's dough was slack, and this is
/// where you'd see that.
public struct RunComparison: Sendable, Equatable {
	public let plannedEquivalentHours: Double
	public let measuredEquivalentHours: Double
	public let plannedTemperatureC: Double
	public let measuredTemperatureC: Double?
	public let longestGapMinutes: Double

	public var differenceHours: Double { measuredEquivalentHours - plannedEquivalentHours }

	public var ratio: Double? {
		plannedEquivalentHours > 0 ? measuredEquivalentHours / plannedEquivalentHours : nil
	}

	/// True when the run drifted far enough from plan to explain a different loaf.
	public var isSignificant: Bool {
		abs(differenceHours) > max(0.5, plannedEquivalentHours * 0.15)
	}

	/// Set when a logging gap makes the measurement an estimate rather than a record.
	public var caveat: String? {
		guard longestGapMinutes > 30 else { return nil }
		return "Logging gap of \(Formatting.hours(longestGapMinutes / 60)) — temperature was interpolated across it."
	}

	public init(stage: PlanStage, series: SensorSeries) {
		self.plannedEquivalentHours = stage.equivalentHours
		self.measuredEquivalentHours = series.measuredEquivalentHours()
		self.plannedTemperatureC = stage.temperatureC
		self.measuredTemperatureC = series.effectiveConstantTemperatureC()
		self.longestGapMinutes = series.longestTemperatureGapMinutes()
	}
}
