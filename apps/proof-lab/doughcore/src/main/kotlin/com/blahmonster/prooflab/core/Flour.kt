package com.blahmonster.prooflab.core

import kotlin.math.max
import kotlinx.serialization.Serializable

/**
 * One flour in a blend.
 *
 * Blends are the whole point of prototyping: 00 with a little semola handles differently from
 * straight 00, and 15 % whole wheat ferments faster than the protein alone would suggest.
 * Percentages are shares of the *total flour*, so they should add to 100 — [DoughFormula]
 * normalises them rather than trusting the arithmetic on screen.
 */
@Serializable
data class FlourComponent(
	val id: String,
	val name: String,
	val percent: Double,
	val proteinPercent: Double,
	/** Hydration points this flour would add if the blend were made entirely of it. */
	val absorptionOffset: Double = 0.0,
	val isWholeGrain: Boolean = false,
) {
	val shortName: String get() = name.substringBefore(" —").substringBefore(" (")
}

/** The shelf you pick a blend from. Protein figures are typical, not a guarantee. */
object FlourLibrary {
	private fun entry(
		id: String,
		name: String,
		protein: Double,
		absorption: Double = 0.0,
		wholeGrain: Boolean = false,
	) = FlourComponent(id, name, 0.0, protein, absorption, wholeGrain)

	val all = listOf(
		entry("00-pizzeria", "Type 00 pizzeria", 12.5),
		entry("00-strong", "Type 00 strong (W 330+)", 13.5, absorption = 2.0),
		entry("nuvola", "Type 00 Nuvola", 12.5, absorption = 1.0),
		entry("bread", "Bread flour", 12.7, absorption = 1.0),
		entry("high-gluten", "High-gluten", 14.0, absorption = 2.5),
		entry("all-purpose", "All-purpose", 10.5, absorption = -1.0),
		entry("t65", "T65", 11.5),
		entry("semola", "Semola rimacinata", 12.5, absorption = -2.0),
		entry("durum", "Whole durum", 13.0, absorption = 4.0, wholeGrain = true),
		entry("whole-wheat", "Whole wheat", 13.5, absorption = 8.0, wholeGrain = true),
		entry("spelt", "Spelt", 12.0, absorption = 2.0),
		entry("rye", "Rye", 10.0, absorption = 10.0, wholeGrain = true),
		entry("einkorn", "Einkorn", 12.0, absorption = -3.0, wholeGrain = true),
		entry("kamut", "Kamut", 13.0, absorption = 3.0, wholeGrain = true),
	)

	fun spec(id: String): FlourComponent? = all.firstOrNull { it.id == id }

	/** A named flour at a given share of the blend. */
	fun at(id: String, percent: Double): FlourComponent =
		(spec(id) ?: all.first()).copy(percent = percent)

	val defaultBlend = listOf(at("bread", 100.0))
}

/** Weighted properties of a blend. */
data class FlourBlend(val components: List<FlourComponent>) {
	/** Shares rescaled to add to exactly 100, so a blend that says 97 % still weighs out right. */
	val normalized: List<FlourComponent>
		get() {
			val total = components.sumOf { max(0.0, it.percent) }
			if (components.isEmpty()) return FlourLibrary.defaultBlend
			if (total <= 0.0) {
				val even = 100.0 / components.size
				return components.map { it.copy(percent = even) }
			}
			return components.map { it.copy(percent = max(0.0, it.percent) / total * 100.0) }
		}

	val proteinPercent: Double
		get() = normalized.sumOf { it.proteinPercent * it.percent / 100.0 }

	val wholeGrainFraction: Double
		get() = normalized.filter { it.isWholeGrain }.sumOf { it.percent } / 100.0

	/**
	 * Roughly how much water this blend carries comfortably. It's a floor, not a target —
	 * style takes you above it, and a Roman teglia goes far above it on purpose.
	 */
	val absorptionGuidePercent: Double
		get() = 50.0 +
			(proteinPercent - 10.0) * 2.2 +
			normalized.sumOf { it.absorptionOffset * it.percent / 100.0 }

	val summary: String
		get() = normalized
			.filter { it.percent >= 0.5 }
			.sortedByDescending { it.percent }
			.joinToString(" · ") { "${Formatting.rounded(it.percent, 0)} % ${it.shortName}" }
}
