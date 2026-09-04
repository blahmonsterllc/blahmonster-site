package com.blahmonster.prooflab

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.blahmonster.prooflab.core.Batch
import com.blahmonster.prooflab.core.BatchReview
import com.blahmonster.prooflab.core.MixerKind
import com.blahmonster.prooflab.core.StageProgress
import com.blahmonster.prooflab.data.BatchRepository
import com.blahmonster.prooflab.data.SettingsStore
import com.blahmonster.prooflab.notify.Notifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppViewModel(application: Application) : AndroidViewModel(application) {
	private val repository = BatchRepository(application)
	private val notifier = Notifier(application)
	private val settings = SettingsStore(application)

	private val _batches = MutableStateFlow(repository.load())
	val batches: StateFlow<List<Batch>> = _batches.asStateFlow()

	private val _notificationsAllowed = MutableStateFlow(notifier.hasPermission())
	val notificationsAllowed: StateFlow<Boolean> = _notificationsAllowed.asStateFlow()

	var useFahrenheit: Boolean
		get() = settings.useFahrenheit
		set(value) {
			settings.useFahrenheit = value
			_settingsRevision.value += 1
		}

	var mixerCapacityKg: Double
		get() = settings.mixerCapacityKg
		set(value) {
			settings.mixerCapacityKg = value
			_settingsRevision.value += 1
		}

	var mixer: MixerKind
		get() = settings.mixer
		set(value) {
			settings.mixer = value
			_settingsRevision.value += 1
		}

	/** Bumped on any settings change so Compose recomposes what reads them. */
	private val _settingsRevision = MutableStateFlow(0)
	val settingsRevision: StateFlow<Int> = _settingsRevision.asStateFlow()

	init {
		notifier.ensureChannels()
		syncAlerts()
	}

	fun onPermissionResult(granted: Boolean) {
		_notificationsAllowed.value = granted
		if (granted) syncAlerts()
	}

	/** Stages elapse while the app is away, so the count is only right after a refresh. */
	fun refresh() {
		_notificationsAllowed.value = notifier.hasPermission()
		notifier.refreshSummary(_batches.value)
	}

	fun sendTestAlert() = notifier.sendTestAlert()

	// MARK: batches

	fun batch(id: String): Batch? = _batches.value.firstOrNull { it.id == id }

	fun activeBatches(): List<Batch> =
		_batches.value.filter { !it.isArchived && !it.isFinished }.sortedBy { it.readyAt }

	fun loggedBatches(): List<Batch> =
		_batches.value.filter { it.isArchived || it.isFinished }.sortedByDescending { it.createdAt }

	fun add(batch: Batch) {
		_batches.value = _batches.value + batch
		persist()
	}

	fun delete(batch: Batch) {
		_batches.value = _batches.value.filterNot { it.id == batch.id }
		persist()
	}

	fun setArchived(batch: Batch, archived: Boolean) = mutate(batch.id) { it.copy(isArchived = archived) }

	fun setNotes(id: String, notes: String) = mutate(id) { it.copy(notes = notes) }

	fun setReview(id: String, review: BatchReview) = mutate(id) { it.copy(review = review) }

	/**
	 * Start again from an existing run, keeping the formula and plan. This is the prototyping
	 * loop: clone, change one thing, bake, compare.
	 */
	fun clone(batch: Batch, startAt: Long): Batch {
		val copy = batch.copy(
			id = java.util.UUID.randomUUID().toString(),
			name = nextName(batch.name),
			createdAt = System.currentTimeMillis(),
			startAt = startAt,
			progress = emptyMap(),
			review = null,
			notes = "",
			isArchived = false,
		)
		add(copy)
		return copy
	}

	// MARK: stage control

	fun complete(batchId: String, stageId: String, at: Long = System.currentTimeMillis()) =
		mutateStage(batchId, stageId) { it.copy(completedAt = at, acknowledgedAt = at) }

	fun reopen(batchId: String, stageId: String) =
		mutateStage(batchId, stageId) { it.copy(completedAt = null) }

	fun acknowledge(batchId: String, stageId: String, at: Long = System.currentTimeMillis()) =
		mutateStage(batchId, stageId) { it.copy(acknowledgedAt = at) }

	/** Adds (or removes) time at the bench. Dough doesn't read clocks. */
	fun adjust(batchId: String, stageId: String, hours: Double) =
		mutateStage(batchId, stageId) {
			it.copy(adjustmentHours = it.adjustmentHours + hours, acknowledgedAt = null)
		}

	private fun mutateStage(batchId: String, stageId: String, change: (StageProgress) -> StageProgress) {
		mutate(batchId) { batch ->
			val current = batch.progress[stageId] ?: StageProgress()
			batch.copy(progress = batch.progress + (stageId to change(current)))
		}
	}

	private fun mutate(id: String, change: (Batch) -> Batch) {
		_batches.value = _batches.value.map { if (it.id == id) change(it) else it }
		persist()
	}

	private fun persist() {
		repository.save(_batches.value)
		syncAlerts()
	}

	private fun syncAlerts() {
		if (_notificationsAllowed.value) {
			notifier.reschedule(_batches.value)
		} else {
			notifier.refreshSummary(_batches.value)
		}
	}

	companion object {
		/** "Friday service" → "Friday service 2" → "Friday service 3" */
		fun nextName(name: String): String {
			val parts = name.trim().split(" ")
			val last = parts.lastOrNull()?.toIntOrNull()
			return if (last != null && parts.size > 1) {
				parts.dropLast(1).joinToString(" ") + " " + (last + 1)
			} else {
				"$name 2"
			}
		}
	}
}
