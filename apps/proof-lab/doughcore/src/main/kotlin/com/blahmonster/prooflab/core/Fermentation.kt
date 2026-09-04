package com.blahmonster.prooflab.core

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * The temperature model everything else hangs off.
 *
 * A single Q10 constant badly underestimates how much a fridge slows a dough, so the curve
 * is split into three segments (see SPEC.md). Real durations get converted into *equivalent
 * hours at 24 °C* — the common currency that makes a 48 hour retard and a 7 hour room
 * temperature bulk comparable, and that drives every yeast suggestion.
 */
object Fermentation {
	const val REFERENCE_TEMPERATURE_C = 24.0
	const val MIN_TEMPERATURE_C = -5.0
	const val MAX_TEMPERATURE_C = 45.0

	private data class Segment(val upper: Double, val q10: Double)

	private val segments = listOf(
		Segment(10.0, 3.0),
		Segment(20.0, 2.5),
		Segment(Double.POSITIVE_INFINITY, 2.0),
	)

	private fun logActivity(raw: Double): Double {
		val temperature = min(max(raw, MIN_TEMPERATURE_C), MAX_TEMPERATURE_C)
		if (temperature <= 0.0) {
			// Below freezing there is no useful data; extrapolate the coldest segment.
			return temperature * ln(segments[0].q10) / 10.0
		}
		var total = 0.0
		var lower = 0.0
		for (segment in segments) {
			if (temperature <= lower) break
			total += (min(temperature, segment.upper) - lower) * ln(segment.q10) / 10.0
			lower = segment.upper
		}
		return total
	}

	/** How fast dough ferments at [temperatureC], as a multiple of the 24 °C rate. */
	fun rateMultiplier(temperatureC: Double, referenceC: Double = REFERENCE_TEMPERATURE_C): Double =
		exp(logActivity(temperatureC) - logActivity(referenceC))

	/** Converts a real duration into hours-at-24 °C. */
	fun equivalentHours(hours: Double, temperatureC: Double): Double =
		max(0.0, hours) * rateMultiplier(temperatureC)

	/** The inverse: how long you must wait at [temperatureC] for the same fermentation. */
	fun hoursForEquivalent(equivalentHours: Double, temperatureC: Double): Double {
		val rate = rateMultiplier(temperatureC)
		if (rate <= 0.0) return Double.POSITIVE_INFINITY
		return max(0.0, equivalentHours) / rate
	}
}

enum class YeastType(val displayName: String, val shortName: String, val multiplier: Double) {
	INSTANT_DRY("Instant dry (IDY)", "IDY", 1.0),
	ACTIVE_DRY("Active dry (ADY)", "ADY", 1.25),
	FRESH_CAKE("Fresh / cake", "Fresh", 3.0),
}

enum class LeavenKind(val displayName: String) {
	COMMERCIAL_YEAST("Commercial yeast"),
	SOURDOUGH("Sourdough"),
}

object Leavening {
	const val MIN_INSTANT_YEAST_PERCENT = 0.02
	const val MAX_INSTANT_YEAST_PERCENT = 1.5
	const val MIN_LEVAIN_PERCENT = 3.0
	const val MAX_LEVAIN_PERCENT = 40.0

	/**
	 * Bran carries enzymes and wild yeast, so whole-grain doughs run faster than their protein
	 * suggests. A wholly whole-grain dough wants roughly 30 % less leaven than a white one on
	 * the same schedule.
	 */
	const val WHOLE_GRAIN_SPEEDUP = 0.3

	/**
	 * Instant dry yeast as a percentage of total flour for a given fermentation load.
	 *
	 * Calibrated against the cold-fermentation tables American pizzerias work from —
	 * 0.42 / 0.21 / 0.14 % for 24 / 48 / 72 hours at 4 °C. Treat it as a starting point: real
	 * doses vary more than fivefold between traditions for the same load (an AVPN Neapolitan
	 * runs ~0.06 %), so the app shows this beside your own number instead of overwriting it.
	 * What transfers between traditions is the *ratio* — double the cold time, halve the yeast.
	 */
	fun instantYeastPercent(
		equivalentHours: Double,
		saltPercent: Double = 2.0,
		sugarPercent: Double = 0.0,
		prefermentedFlourFraction: Double = 0.0,
		wholeGrainFraction: Double = 0.0,
	): Double {
		if (equivalentHours <= 0.0) return MAX_INSTANT_YEAST_PERCENT
		var percent = 1.6 / equivalentHours
		percent *= 1 + 0.14 * max(0.0, saltPercent - 2.0)
		percent *= 1 + 0.03 * max(0.0, sugarPercent - 5.0)
		percent *= 1 - 0.8 * min(max(prefermentedFlourFraction, 0.0), 1.0)
		percent *= 1 - WHOLE_GRAIN_SPEEDUP * min(max(wholeGrainFraction, 0.0), 1.0)
		return min(max(percent, MIN_INSTANT_YEAST_PERCENT), MAX_INSTANT_YEAST_PERCENT)
	}

	/** Ripe levain as a percentage of total flour (levain weight, not the flour in it). */
	fun levainPercent(
		equivalentHours: Double,
		saltPercent: Double = 2.0,
		wholeGrainFraction: Double = 0.0,
	): Double {
		if (equivalentHours <= 0.0) return MAX_LEVAIN_PERCENT
		var percent = 90.0 / equivalentHours
		percent *= 1 + 0.14 * max(0.0, saltPercent - 2.0)
		percent *= 1 - WHOLE_GRAIN_SPEEDUP * min(max(wholeGrainFraction, 0.0), 1.0)
		return min(max(percent, MIN_LEVAIN_PERCENT), MAX_LEVAIN_PERCENT)
	}

	/** Rough inverse of [instantYeastPercent] — "at this dose, how long does it want?". */
	fun equivalentHoursForInstantYeast(percent: Double): Double =
		if (percent <= 0.0) Double.POSITIVE_INFINITY else 1.6 / percent
}
