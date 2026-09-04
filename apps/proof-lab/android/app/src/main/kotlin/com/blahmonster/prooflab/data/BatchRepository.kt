package com.blahmonster.prooflab.data

import android.content.Context
import com.blahmonster.prooflab.core.Batch
import com.blahmonster.prooflab.core.MixerKind
import java.io.File
import kotlinx.serialization.json.Json

/**
 * Flat JSON in the app's files directory. Batches are small and few; a database would be
 * ceremony, and a plain file is easy to back up and easy to read when something looks wrong.
 */
class BatchRepository(context: Context) {
	private val file = File(context.filesDir, FILE_NAME)
	private val json = Json {
		prettyPrint = true
		ignoreUnknownKeys = true
		encodeDefaults = true
	}

	fun load(): List<Batch> = runCatching {
		if (!file.exists()) return emptyList()
		json.decodeFromString<List<Batch>>(file.readText())
	}.getOrElse { emptyList() }

	fun save(batches: List<Batch>) {
		runCatching {
			// Write beside the real file and swap, so a crash mid-write can't lose the lot.
			val temp = File(file.parentFile, "$FILE_NAME.tmp")
			temp.writeText(json.encodeToString(batches))
			temp.renameTo(file)
		}
	}

	companion object {
		const val FILE_NAME = "batches.json"
	}
}

/** The handful of preferences that aren't part of a batch. */
class SettingsStore(context: Context) {
	private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

	var useFahrenheit: Boolean
		get() = prefs.getBoolean(KEY_FAHRENHEIT, false)
		set(value) = prefs.edit().putBoolean(KEY_FAHRENHEIT, value).apply()

	var mixerCapacityKg: Double
		get() = prefs.getFloat(KEY_CAPACITY, 20f).toDouble()
		set(value) = prefs.edit().putFloat(KEY_CAPACITY, value.toFloat()).apply()

	var mixer: MixerKind
		get() = runCatching {
			MixerKind.valueOf(prefs.getString(KEY_MIXER, MixerKind.SPIRAL.name)!!)
		}.getOrDefault(MixerKind.SPIRAL)
		set(value) = prefs.edit().putString(KEY_MIXER, value.name).apply()

	private companion object {
		const val KEY_FAHRENHEIT = "fahrenheit"
		const val KEY_CAPACITY = "mixerCapacityKg"
		const val KEY_MIXER = "mixer"
	}
}
