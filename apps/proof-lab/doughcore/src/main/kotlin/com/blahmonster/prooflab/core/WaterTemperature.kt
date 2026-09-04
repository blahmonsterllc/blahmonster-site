package com.blahmonster.prooflab.core

import kotlin.math.max
import kotlin.math.min

enum class MixerKind(val displayName: String, val frictionC: Double) {
	HAND("By hand", 1.0),
	FORK("Fork mixer", 2.0),
	DOUBLE_ARM("Double-arm", 2.0),
	DIVING_ARM("Diving-arm", 3.0),
	SPIRAL("Spiral", 4.0),
	PLANETARY("Planetary", 6.0),
}

data class WaterTemperatureResult(
	/** The water temperature the factor method asks for. */
	val waterTemperatureC: Double,
	/** True when a preferment was included, i.e. the four-factor method was used. */
	val usedPrefermentFactor: Boolean,
	/** Grams of ice to swap in, when tap water alone can't get cold enough. */
	val iceGrams: Double,
	/** Grams of liquid tap water to pair with that ice. */
	val waterGrams: Double,
	/** Set when the required temperature is unreachable — chill the flour instead. */
	val warning: String? = null,
)

object DoughTemperature {
	/** Classic factor method. Three factors for a straight dough, four with a preferment. */
	fun waterTemperature(
		desiredDoughTempC: Double,
		flourTempC: Double,
		roomTempC: Double,
		prefermentTempC: Double?,
		frictionC: Double,
	): Double {
		val factors = if (prefermentTempC == null) 3.0 else 4.0
		val known = flourTempC + roomTempC + (prefermentTempC ?: 0.0) + frictionC
		return desiredDoughTempC * factors - known
	}

	/**
	 * Ice needed to bring [waterGrams] of tap water down to [targetC].
	 *
	 * Heat balance with the latent heat of fusion (~80 cal/g):
	 * `(W − I)(tap − target) = I(80 + target)`.
	 */
	fun iceSplit(waterGrams: Double, tapTempC: Double, targetC: Double): Pair<Double, Double> {
		if (waterGrams <= 0 || targetC >= tapTempC) return 0.0 to max(0.0, waterGrams)
		val denominator = 80.0 + tapTempC
		if (denominator <= 0) return 0.0 to waterGrams
		val ice = waterGrams * (tapTempC - targetC) / denominator
		val clamped = min(max(ice, 0.0), waterGrams)
		return clamped to (waterGrams - clamped)
	}

	fun solve(
		desiredDoughTempC: Double,
		flourTempC: Double,
		roomTempC: Double,
		prefermentTempC: Double?,
		mixer: MixerKind,
		totalWaterGrams: Double,
		tapWaterTempC: Double,
	): WaterTemperatureResult {
		val target = waterTemperature(
			desiredDoughTempC = desiredDoughTempC,
			flourTempC = flourTempC,
			roomTempC = roomTempC,
			prefermentTempC = prefermentTempC,
			frictionC = mixer.frictionC,
		)

		val warning = when {
			target < 0 -> "Below 0 °C — ice alone won't get there. Chill the flour or cool the room."
			target > 40 -> "Above 40 °C would shock the yeast. Warm the flour instead."
			else -> null
		}

		val (ice, water) = iceSplit(totalWaterGrams, tapWaterTempC, max(0.0, target))

		return WaterTemperatureResult(
			waterTemperatureC = target,
			usedPrefermentFactor = prefermentTempC != null,
			iceGrams = ice,
			waterGrams = water,
			warning = warning,
		)
	}
}
