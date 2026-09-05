package com.blahmonster.prooflab.core

import kotlin.math.abs
import kotlin.math.max
import kotlinx.serialization.Serializable

/**
 * One sample from a bench rig.
 *
 * Every field but the timestamp is optional, because a rig gets built up a sensor at a time and
 * a half-instrumented run is still worth logging. Heights are the dough's own height, not the
 * sensor's distance reading — a lid-mounted rangefinder counts *down* as the dough rises, so
 * that conversion belongs in the firmware where the container geometry is known.
 */
@Serializable
data class SensorReading(
	val epochMillis: Long,
	/** Dough core temperature — what actually drives fermentation. */
	val doughTempC: Double? = null,
	/** Air around the dough. Used as a fallback when there's no probe in the dough. */
	val ambientTempC: Double? = null,
	val relativeHumidity: Double? = null,
	val doughHeightMm: Double? = null,
	val co2Ppm: Double? = null,
	/** Total mass. Falls slowly as CO₂ and water leave — an independent check on activity. */
	val massGrams: Double? = null,
) {
	/** The dough's own temperature if we have it, otherwise the air's. */
	val effectiveTempC: Double? get() = doughTempC ?: ambientTempC
}

/**
 * A run's worth of samples.
 *
 * This is deliberately descriptive rather than predictive. It measures what happened and
 * projects a trend forward; it does not claim to know when the dough is ready. That threshold
 * is what the rig exists to discover, and hard-coding a guess for it now would defeat the point.
 */
@Serializable
data class SensorSeries(val readings: List<SensorReading>) {
	/** Samples in time order, which is the only order any of this makes sense in. */
	val ordered: List<SensorReading> by lazy { readings.sortedBy { it.epochMillis } }

	val isEmpty: Boolean get() = readings.isEmpty()

	val startMillis: Long? get() = ordered.firstOrNull()?.epochMillis
	val endMillis: Long? get() = ordered.lastOrNull()?.epochMillis

	val elapsedHours: Double
		get() {
			val start = startMillis ?: return 0.0
			val end = endMillis ?: return 0.0
			return (end - start).toDouble() / HOUR_MILLIS
		}

	/**
	 * Fermentation actually accumulated, from measured temperature rather than an assumed one.
	 *
	 * This is the whole reason to put a probe in the dough: the schedule assumes the walk-in is
	 * 4 °C, and if it's really 5.8 °C every equivalent-hour figure downstream is wrong. Rates
	 * are integrated by trapezoid between samples, which at any sane logging interval is far
	 * more accurate than the temperature model's own uncertainty.
	 */
	fun measuredEquivalentHours(): Double {
		val points = ordered.mapNotNull { reading ->
			reading.effectiveTempC?.let { reading.epochMillis to it }
		}
		if (points.size < 2) return 0.0

		var total = 0.0
		for (index in 0 until points.size - 1) {
			val (t1, temp1) = points[index]
			val (t2, temp2) = points[index + 1]
			val hours = (t2 - t1).toDouble() / HOUR_MILLIS
			if (hours <= 0) continue
			val rate1 = Fermentation.rateMultiplier(temp1)
			val rate2 = Fermentation.rateMultiplier(temp2)
			total += hours * (rate1 + rate2) / 2
		}
		return total
	}

	/**
	 * The longest stretch with no usable temperature sample. Gaps are integrated straight
	 * across on the assumption temperature moved linearly — the dough kept fermenting either
	 * way — but a long one means the figure is an estimate, and the caller should say so.
	 */
	fun longestTemperatureGapMinutes(): Double {
		val stamps = ordered.filter { it.effectiveTempC != null }.map { it.epochMillis }
		if (stamps.size < 2) return 0.0
		var longest = 0L
		for (index in 0 until stamps.size - 1) {
			longest = max(longest, stamps[index + 1] - stamps[index])
		}
		return longest.toDouble() / 60_000
	}

	fun averageTemperatureC(): Double? {
		val temps = ordered.mapNotNull { it.effectiveTempC }
		if (temps.isEmpty()) return null
		return temps.sum() / temps.size
	}

	/**
	 * The temperature a constant-temperature run would have needed to ferment this much in this
	 * time. Not the arithmetic mean — a dough that spent an hour at 30 °C and an hour at 10 °C
	 * is further along than one held at 20 °C the whole time.
	 */
	fun effectiveConstantTemperatureC(): Double? {
		val hours = elapsedHours
		if (hours <= 0) return null
		val equivalent = measuredEquivalentHours()
		if (equivalent <= 0) return null
		val targetRate = equivalent / hours
		// The rate curve is monotonic in temperature, so bisect it.
		var low = Fermentation.MIN_TEMPERATURE_C
		var high = Fermentation.MAX_TEMPERATURE_C
		repeat(60) {
			val mid = (low + high) / 2
			if (Fermentation.rateMultiplier(mid) < targetRate) low = mid else high = mid
		}
		return (low + high) / 2
	}

	// MARK: - Rise

	val firstHeightMm: Double? get() = ordered.firstNotNullOfOrNull { it.doughHeightMm }
	val latestHeightMm: Double? get() = ordered.lastOrNull { it.doughHeightMm != null }?.doughHeightMm

	/** Current height as a multiple of where it started. 1.75 means "risen 75 %". */
	fun expansionRatio(baselineMm: Double? = null): Double? {
		val baseline = baselineMm ?: firstHeightMm ?: return null
		val latest = latestHeightMm ?: return null
		if (baseline <= 0) return null
		return latest / baseline
	}

	fun expansionPercent(baselineMm: Double? = null): Double? =
		expansionRatio(baselineMm)?.let { (it - 1) * 100 }

	/**
	 * How fast it's rising right now, in percent of the baseline height per hour, measured over
	 * the last [windowMinutes]. Null when there aren't two height samples in the window.
	 */
	fun riseRatePercentPerHour(windowMinutes: Double = 45.0, baselineMm: Double? = null): Double? {
		val baseline = baselineMm ?: firstHeightMm ?: return null
		if (baseline <= 0) return null
		val end = endMillis ?: return null
		val cutoff = end - (windowMinutes * 60_000).toLong()
		val window = ordered.filter { it.epochMillis >= cutoff && it.doughHeightMm != null }
		if (window.size < 2) return null

		val first = window.first()
		val last = window.last()
		val hours = (last.epochMillis - first.epochMillis).toDouble() / HOUR_MILLIS
		if (hours <= 0) return null
		val delta = (last.doughHeightMm!! - first.doughHeightMm!!) / baseline * 100
		return delta / hours
	}

	/**
	 * Straight-line extrapolation to a target expansion, in hours from the last sample.
	 *
	 * Rise is not linear — it accelerates, then flattens as the gluten gives out — so this is a
	 * rough steer, not a prediction, and it deliberately refuses to answer when the dough isn't
	 * currently rising.
	 */
	fun projectedHoursTo(
		targetRatio: Double,
		windowMinutes: Double = 45.0,
		baselineMm: Double? = null,
	): Double? {
		val current = expansionRatio(baselineMm) ?: return null
		if (current >= targetRatio) return 0.0
		val rate = riseRatePercentPerHour(windowMinutes, baselineMm) ?: return null
		if (rate <= 0) return null
		return (targetRatio - current) * 100 / rate
	}

	// MARK: - Other channels

	fun latestCo2Ppm(): Double? = ordered.lastOrNull { it.co2Ppm != null }?.co2Ppm

	/** CO₂ slope over the window — the most direct read on how hard the yeast is working. */
	fun co2SlopePpmPerHour(windowMinutes: Double = 45.0): Double? =
		slopePerHour(windowMinutes) { it.co2Ppm }

	/**
	 * Mass loss per hour. CO₂ and water leaving a covered-but-not-sealed container show up here,
	 * which makes it an independent check on the gas reading — and it needs no headspace model.
	 */
	fun massLossGramsPerHour(windowMinutes: Double = 60.0): Double? =
		slopePerHour(windowMinutes) { it.massGrams }?.let { -it }

	fun latestRelativeHumidity(): Double? =
		ordered.lastOrNull { it.relativeHumidity != null }?.relativeHumidity

	private fun slopePerHour(windowMinutes: Double, value: (SensorReading) -> Double?): Double? {
		val end = endMillis ?: return null
		val cutoff = end - (windowMinutes * 60_000).toLong()
		val window = ordered.filter { it.epochMillis >= cutoff && value(it) != null }
		if (window.size < 2) return null
		val first = window.first()
		val last = window.last()
		val hours = (last.epochMillis - first.epochMillis).toDouble() / HOUR_MILLIS
		if (hours <= 0) return null
		return (value(last)!! - value(first)!!) / hours
	}

	fun appending(reading: SensorReading): SensorSeries = SensorSeries(readings + reading)

	/** Everything from [from] to [to] inclusive — used to score one stage out of a whole run. */
	fun slice(from: Long, to: Long): SensorSeries =
		SensorSeries(ordered.filter { it.epochMillis in from..to })
}

/**
 * The wire format between the rig and the app.
 *
 * Plain CSV on purpose: it opens in anything, survives a firmware rewrite, and can be read by
 * eye when a run looks wrong. Blank means "no sensor for this on that run", not zero.
 */
object SensorCsv {
	const val HEADER = "timestamp_ms,dough_c,ambient_c,rh,height_mm,co2_ppm,mass_g"

	fun encode(series: SensorSeries): String = buildString {
		appendLine(HEADER)
		for (reading in series.ordered) {
			append(reading.epochMillis).append(',')
			append(reading.doughTempC?.toString().orEmpty()).append(',')
			append(reading.ambientTempC?.toString().orEmpty()).append(',')
			append(reading.relativeHumidity?.toString().orEmpty()).append(',')
			append(reading.doughHeightMm?.toString().orEmpty()).append(',')
			append(reading.co2Ppm?.toString().orEmpty()).append(',')
			appendLine(reading.massGrams?.toString().orEmpty())
		}
	}

	/**
	 * Lenient by design. A rig that drops a field, writes a partial last line on power loss, or
	 * emits a stray comment shouldn't cost you the run.
	 */
	fun decode(text: String): SensorSeries {
		val readings = text.lineSequence()
			.map { it.trim() }
			.filter { it.isNotEmpty() && !it.startsWith("#") }
			.filterNot { it.startsWith("timestamp", ignoreCase = true) }
			.mapNotNull { line ->
				val cells = line.split(',')
				val timestamp = cells.getOrNull(0)?.trim()?.toLongOrNull() ?: return@mapNotNull null
				fun cell(index: Int) = cells.getOrNull(index)?.trim()?.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
				SensorReading(
					epochMillis = timestamp,
					doughTempC = cell(1),
					ambientTempC = cell(2),
					relativeHumidity = cell(3),
					doughHeightMm = cell(4),
					co2Ppm = cell(5),
					massGrams = cell(6),
				)
			}
			.toList()
		return SensorSeries(readings)
	}
}

/**
 * What a run looked like against what the plan expected.
 *
 * The gap between planned and measured is the finding. A cold retard that was supposed to bank
 * 7.5 equivalent hours and actually banked 9.4 is why Friday's dough was slack, and this is
 * where you'd see that.
 */
data class RunComparison(
	val plannedEquivalentHours: Double,
	val measuredEquivalentHours: Double,
	val plannedTemperatureC: Double,
	val measuredTemperatureC: Double?,
	val longestGapMinutes: Double,
) {
	val differenceHours: Double get() = measuredEquivalentHours - plannedEquivalentHours

	val ratio: Double?
		get() = if (plannedEquivalentHours > 0) measuredEquivalentHours / plannedEquivalentHours else null

	/** True when the run drifted far enough from plan to explain a different loaf. */
	val isSignificant: Boolean get() = abs(differenceHours) > max(0.5, plannedEquivalentHours * 0.15)

	/** Set when a logging gap makes the measurement an estimate rather than a record. */
	val caveat: String?
		get() = if (longestGapMinutes > 30) {
			"Logging gap of ${Formatting.hours(longestGapMinutes / 60)} — temperature was interpolated across it."
		} else {
			null
		}

	companion object {
		fun of(stage: PlanStage, series: SensorSeries): RunComparison = RunComparison(
			plannedEquivalentHours = stage.equivalentHours,
			measuredEquivalentHours = series.measuredEquivalentHours(),
			plannedTemperatureC = stage.temperatureC,
			measuredTemperatureC = series.effectiveConstantTemperatureC(),
			longestGapMinutes = series.longestTemperatureGapMinutes(),
		)
	}
}
