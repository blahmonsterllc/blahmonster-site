import DoughKit
import Foundation
import UserNotifications

/// Local notifications and the app icon badge.
///
/// A proofing app is only useful if it can reach you with the app closed, so nothing here
/// relies on the app being awake. Every stage boundary is scheduled ahead of time, and each
/// notification carries its own precomputed badge number — the icon count stays right through
/// an overnight retard even if ProofLab never runs.
@MainActor
final class NotificationService: NSObject {
	static let shared = NotificationService()

	static let categoryIdentifier = "STAGE_ALERT"
	static let markDoneAction = "MARK_DONE"
	static let snoozeAction = "SNOOZE_15"

	/// Set by the app so notification buttons can act on the batch they refer to.
	var onAction: ((_ actionIdentifier: String, _ alertIdentifier: String) -> Void)?

	private let center = UNUserNotificationCenter.current()

	func bootstrap() {
		center.delegate = self
		registerCategories()
	}

	private func registerCategories() {
		let done = UNNotificationAction(
			identifier: Self.markDoneAction,
			title: "Mark done",
			options: [.authenticationRequired]
		)
		let snooze = UNNotificationAction(
			identifier: Self.snoozeAction,
			title: "Give it 15 more",
			options: []
		)
		let category = UNNotificationCategory(
			identifier: Self.categoryIdentifier,
			actions: [done, snooze],
			intentIdentifiers: [],
			options: []
		)
		center.setNotificationCategories([category])
	}

	func authorizationStatus() async -> UNAuthorizationStatus {
		await center.notificationSettings().authorizationStatus
	}

	@discardableResult
	func requestAuthorization() async -> Bool {
		(try? await center.requestAuthorization(options: [.alert, .sound, .badge])) ?? false
	}

	/// Replaces every pending alert. Cheap enough to call on any change — the alternative is
	/// diffing identifiers, which goes wrong the first time a stage shifts.
	func reschedule(batches: [Batch], now: Date = Date()) async {
		center.removeAllPendingNotificationRequests()

		let outstanding = AlertScheduler.currentBadge(batches: batches, at: now)
		let planned = AlertScheduler.badgedAlerts(batches: batches, from: now, startingBadge: outstanding)

		for (alert, badge) in planned {
			let interval = alert.fireAt.timeIntervalSince(now)
			guard interval > 0 else { continue }

			let content = UNMutableNotificationContent()
			content.title = alert.title
			content.body = alert.body
			content.sound = .default
			content.badge = NSNumber(value: badge)
			content.categoryIdentifier = Self.categoryIdentifier
			content.threadIdentifier = alert.batchID.uuidString
			content.userInfo = [
				"alertID": alert.id,
				"batchID": alert.batchID.uuidString,
				"stageID": alert.stageID,
				"kind": alert.kind.rawValue
			]
			// Time-sensitive gets through a Focus; folds are a nudge and shouldn't.
			content.interruptionLevel = alert.kind == .fold ? .active : .timeSensitive

			let request = UNNotificationRequest(
				identifier: alert.id,
				content: content,
				trigger: UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)
			)
			try? await center.add(request)
		}

		await setBadge(outstanding)
	}

	func setBadge(_ count: Int) async {
		try? await center.setBadgeCount(max(0, count))
	}

	func cancelAll() {
		center.removeAllPendingNotificationRequests()
		center.removeAllDeliveredNotifications()
	}

	/// A quick "does this actually work on my phone" check, since a silent notification
	/// permission is the single most common reason a proofing alert never arrives.
	func sendTestAlert() async {
		let content = UNMutableNotificationContent()
		content.title = "ProofLab alerts are working"
		content.body = "This is what a stage alert will look like."
		content.sound = .default
		let request = UNNotificationRequest(
			identifier: "test-alert",
			content: content,
			trigger: UNTimeIntervalNotificationTrigger(timeInterval: 5, repeats: false)
		)
		try? await center.add(request)
	}
}

extension NotificationService: UNUserNotificationCenterDelegate {
	nonisolated func userNotificationCenter(
		_ center: UNUserNotificationCenter,
		willPresent notification: UNNotification
	) async -> UNNotificationPresentationOptions {
		[.banner, .list, .sound, .badge]
	}

	nonisolated func userNotificationCenter(
		_ center: UNUserNotificationCenter,
		didReceive response: UNNotificationResponse
	) async {
		let info = response.notification.request.content.userInfo
		guard let alertID = info["alertID"] as? String else { return }
		let action = response.actionIdentifier
		await MainActor.run {
			NotificationService.shared.onAction?(action, alertID)
		}
	}
}
