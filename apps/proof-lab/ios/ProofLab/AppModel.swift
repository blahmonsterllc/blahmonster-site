import DoughKit
import Foundation
import Observation
import SwiftUI

@MainActor
@Observable
final class AppModel {
	private(set) var batches: [Batch] = []
	var notificationsAuthorized = false

	// Preferences are written straight through to UserDefaults; there are only three of them
	// and they need to survive a relaunch.
	var useFahrenheit: Bool {
		didSet { defaults.set(useFahrenheit, forKey: Keys.fahrenheit) }
	}

	var defaultMixerCapacityKg: Double {
		didSet { defaults.set(defaultMixerCapacityKg, forKey: Keys.mixerCapacity) }
	}

	var defaultMixer: MixerKind {
		didSet { defaults.set(defaultMixer.rawValue, forKey: Keys.mixer) }
	}

	private enum Keys {
		static let fahrenheit = "useFahrenheit"
		static let mixerCapacity = "defaultMixerCapacityKg"
		static let mixer = "defaultMixer"
	}

	private let store: BatchStore
	private let notifications: NotificationService
	private let defaults: UserDefaults

	init(
		store: BatchStore? = nil,
		notifications: NotificationService = .shared,
		defaults: UserDefaults = .standard
	) {
		self.store = store ?? Self.makeStore()
		self.notifications = notifications
		self.defaults = defaults
		self.useFahrenheit = defaults.bool(forKey: Keys.fahrenheit)
		// `double(forKey:)` returns 0 for a missing key, which would be a nonsense mixer.
		let storedCapacity = defaults.double(forKey: Keys.mixerCapacity)
		self.defaultMixerCapacityKg = storedCapacity > 0 ? storedCapacity : 20
		self.defaultMixer = defaults.string(forKey: Keys.mixer)
			.flatMap(MixerKind.init(rawValue:)) ?? .spiral
		self.batches = self.store.load()
	}

	private static func makeStore() -> BatchStore {
		if let url = try? BatchStore.defaultURL() { return BatchStore(url: url) }
		// Falling back to a temporary file beats crashing on launch; the user still gets a
		// working app for the session and we surface nothing they can't act on.
		return BatchStore(url: FileManager.default.temporaryDirectory.appendingPathComponent("batches.json"))
	}

	// MARK: - Lifecycle

	func bootstrap() async {
		notifications.bootstrap()
		notifications.onAction = { [weak self] action, alertID in
			self?.handleNotificationAction(action, alertID: alertID)
		}
		notificationsAuthorized = await notifications.authorizationStatus() == .authorized
		await syncAlerts()
	}

	func requestNotificationPermission() async {
		notificationsAuthorized = await notifications.requestAuthorization()
		await syncAlerts()
	}

	func sendTestAlert() async {
		await notifications.sendTestAlert()
	}

	/// Called whenever the app comes forward — the badge is only ever as good as the last
	/// reconciliation, and stages may have elapsed while we were away.
	func refresh() async {
		await syncAlerts()
	}

	// MARK: - Batches

	var activeBatches: [Batch] {
		batches
			.filter { !$0.isArchived && !$0.isFinished }
			.sorted { $0.readyAt < $1.readyAt }
	}

	var loggedBatches: [Batch] {
		batches
			.filter { $0.isArchived || $0.isFinished }
			.sorted { $0.createdAt > $1.createdAt }
	}

	func batch(id: UUID) -> Batch? {
		batches.first { $0.id == id }
	}

	func add(_ batch: Batch) {
		batches.append(batch)
		persist()
	}

	func update(_ batch: Batch) {
		guard let index = batches.firstIndex(where: { $0.id == batch.id }) else { return }
		batches[index] = batch
		persist()
	}

	func delete(_ batch: Batch) {
		batches.removeAll { $0.id == batch.id }
		persist()
	}

	func archive(_ batch: Batch) {
		mutate(batch.id) { $0.isArchived = true }
	}

	func unarchive(_ batch: Batch) {
		mutate(batch.id) { $0.isArchived = false }
	}

	/// Start again from an existing run, keeping the formula and plan. This is the prototyping
	/// loop: clone, change one thing, bake, compare.
	func clone(_ batch: Batch, startingAt date: Date) -> Batch {
		let copy = Batch(
			name: Self.nextName(after: batch.name),
			startAt: date,
			formula: batch.formula,
			plan: batch.plan,
			mixerCapacityKg: batch.mixerCapacityKg,
			notes: "",
			tags: batch.tags
		)
		add(copy)
		return copy
	}

	static func nextName(after name: String) -> String {
		// "Friday service" → "Friday service 2" → "Friday service 3"
		let parts = name.split(separator: " ")
		if let last = parts.last, let number = Int(last), parts.count > 1 {
			return parts.dropLast().joined(separator: " ") + " \(number + 1)"
		}
		return name + " 2"
	}

	// MARK: - Stage control

	func complete(stageID: String, in batchID: UUID, at date: Date = Date()) {
		mutate(batchID) { batch in
			var state = batch.progress[stageID] ?? StageProgress()
			state.completedAt = date
			state.acknowledgedAt = date
			batch.progress[stageID] = state
		}
	}

	func reopen(stageID: String, in batchID: UUID) {
		mutate(batchID) { batch in
			var state = batch.progress[stageID] ?? StageProgress()
			state.completedAt = nil
			batch.progress[stageID] = state
		}
	}

	func acknowledge(stageID: String, in batchID: UUID, at date: Date = Date()) {
		mutate(batchID) { batch in
			var state = batch.progress[stageID] ?? StageProgress()
			state.acknowledgedAt = date
			batch.progress[stageID] = state
		}
	}

	/// Adds (or removes) time at the bench. Dough doesn't read clocks.
	func adjust(stageID: String, in batchID: UUID, byHours delta: Double) {
		mutate(batchID) { batch in
			var state = batch.progress[stageID] ?? StageProgress()
			state.adjustmentHours += delta
			// Reopening it means the new time is what we're now waiting on.
			state.acknowledgedAt = nil
			batch.progress[stageID] = state
		}
	}

	func setStart(_ date: Date, for batchID: UUID) {
		mutate(batchID) { $0.startAt = date }
	}

	func setReview(_ review: BatchReview, for batchID: UUID) {
		mutate(batchID) { $0.review = review }
	}

	func setNotes(_ notes: String, for batchID: UUID) {
		mutate(batchID) { $0.notes = notes }
	}

	// MARK: - Internals

	private func mutate(_ id: UUID, _ change: (inout Batch) -> Void) {
		guard let index = batches.firstIndex(where: { $0.id == id }) else { return }
		change(&batches[index])
		persist()
	}

	private func persist() {
		try? store.save(batches)
		Task { await syncAlerts() }
	}

	private func syncAlerts() async {
		guard notificationsAuthorized else {
			await notifications.setBadge(AlertScheduler.currentBadge(batches: batches))
			return
		}
		await notifications.reschedule(batches: batches)
	}

	private func handleNotificationAction(_ action: String, alertID: String) {
		// Alert identifiers are "<batchUUID>|<stageID>|<kind>[|index]".
		let parts = alertID.split(separator: "|", omittingEmptySubsequences: false)
		guard parts.count >= 2,
			  let batchID = UUID(uuidString: String(parts[0])) else { return }
		let stageID = String(parts[1])

		switch action {
		case NotificationService.markDoneAction:
			complete(stageID: stageID, in: batchID)
		case NotificationService.snoozeAction:
			adjust(stageID: stageID, in: batchID, byHours: 0.25)
		default:
			acknowledge(stageID: stageID, in: batchID)
		}
	}
}
