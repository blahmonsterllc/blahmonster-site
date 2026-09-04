package com.blahmonster.prooflab.core

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Display helpers shared by every screen, kept next to the model so the iOS port has
 * something to match string-for-string.
 */
object Formatting {
	fun cToF(c: Double): Double = c * 1.8 + 32
	fun fToC(f: Double): Double = (f - 32) / 1.8

	fun temperature(c: Double, fahrenheit: Boolean = false): String =
		if (fahrenheit) "${cToF(c).roundToInt()}°F"
		else "${rounded(c, if (c % 1.0 == 0.0) 0 else 1)}°C"

	/** "3h 20m", "45m", "2d 4h". */
	fun hours(hours: Double): String {
		if (hours.isNaN() || hours.isInfinite()) return "—"
		val totalMinutes = (hours * 60).roundToInt()
		val days = totalMinutes / 1440
		val h = (totalMinutes - days * 1440) / 60
		val m = totalMinutes - days * 1440 - h * 60
		val parts = mutableListOf<String>()
		if (days > 0) parts.add("${days}d")
		if (h > 0) parts.add("${h}h")
		if (m > 0 || parts.isEmpty()) parts.add("${m}m")
		return parts.joinToString(" ")
	}

	/** Countdown form: "01:59:04", or "2d 03:12" past a day. Negative reads "+00:04:12". */
	fun countdown(millis: Long): String {
		val negative = millis < 0
		val total = abs(millis) / 1000
		val days = total / 86_400
		val h = (total % 86_400) / 3600
		val m = (total % 3600) / 60
		val s = total % 60
		fun pad(value: Long) = value.toString().padStart(2, '0')
		val body = if (days > 0) "${days}d ${pad(h)}:${pad(m)}" else "${pad(h)}:${pad(m)}:${pad(s)}"
		return if (negative) "+$body" else body
	}

	fun grams(grams: Double): String = when {
		grams >= 10_000 -> "${rounded(grams / 1000, 2)} kg"
		grams >= 1000 -> "${rounded(grams / 1000, 3)} kg"
		grams >= 100 -> "${rounded(grams, 0)} g"
		grams >= 10 -> "${rounded(grams, 1)} g"
		else -> "${rounded(grams, 2)} g"
	}

	fun percent(value: Double, places: Int = 2): String = "${rounded(value, places)} %"

	fun rounded(value: Double, places: Int): String {
		val formatted = String.format(Locale.US, "%.${maxOf(0, places)}f", value)
		if (!formatted.contains('.')) return formatted
		return formatted.trimEnd('0').trimEnd('.')
	}

	/** Scale weights the way a baker actually reads them off a scale. */
	fun scaleWeight(grams: Double): Double = when {
		grams >= 1000 -> (grams / 5).roundToLong() * 5.0
		grams >= 100 -> grams.roundToLong().toDouble()
		else -> (grams * 10).roundToLong() / 10.0
	}
}
