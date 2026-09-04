package com.blahmonster.prooflab.notify

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.blahmonster.prooflab.MainActivity
import com.blahmonster.prooflab.core.AlertScheduler
import com.blahmonster.prooflab.core.Batch
import com.blahmonster.prooflab.core.DoughAlert
import com.blahmonster.prooflab.data.BatchRepository

/**
 * Stage alerts.
 *
 * Android has no numeric app badge, so the equivalent of the iOS icon count is a grouped
 * notification carrying [NotificationCompat.Builder.setNumber] — launchers that show a count
 * read that, and the rest show a dot. Alarms are exact because a proofing timer that fires
 * "sometime in the next half hour" is no timer at all; the manifest declares USE_EXACT_ALARM,
 * which is the permission meant for alarm and timer apps.
 */
class Notifier(private val context: Context) {

	private val alarms = context.getSystemService(AlarmManager::class.java)
	private val manager = NotificationManagerCompat.from(context)

	fun ensureChannels() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

		val stages = NotificationChannel(
			CHANNEL_STAGES,
			"Stage alerts",
			NotificationManager.IMPORTANCE_HIGH,
		).apply {
			description = "When a bulk, retard, proof or preferment is up."
			enableVibration(true)
		}

		val folds = NotificationChannel(
			CHANNEL_FOLDS,
			"Fold reminders",
			NotificationManager.IMPORTANCE_DEFAULT,
		).apply {
			description = "Stretch-and-fold nudges during a bulk ferment."
		}

		context.getSystemService(NotificationManager::class.java)
			.createNotificationChannels(listOf(stages, folds))
	}

	fun hasPermission(): Boolean =
		Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
			ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
			PackageManager.PERMISSION_GRANTED

	/**
	 * Replaces every pending alarm. Cheap enough to call on any change — the alternative is
	 * diffing request codes, which goes wrong the first time a stage shifts.
	 */
	fun reschedule(batches: List<Batch>, now: Long = System.currentTimeMillis()) {
		cancelAll()

		val outstanding = AlertScheduler.currentBadge(batches, now)
		val planned = AlertScheduler.badgedAlerts(batches, now, startingBadge = outstanding)

		for ((alert, badge) in planned) {
			if (alert.fireAt <= now) continue
			val pending = pendingIntent(alert, badge)
			runCatching {
				alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alert.fireAt, pending)
			}.onFailure {
				// Exact alarms can be refused; an inexact alarm still beats none at all.
				alarms.set(AlarmManager.RTC_WAKEUP, alert.fireAt, pending)
			}
			remember(alert)
		}

		showSummary(outstanding)
	}

	fun cancelAll() {
		for (id in rememberedIds()) {
			alarms.cancel(
				PendingIntent.getBroadcast(
					context,
					id.hashCode(),
					Intent(context, AlarmReceiver::class.java).setAction(id),
					PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
				) ?: continue,
			)
		}
		forgetAll()
	}

	private fun pendingIntent(alert: DoughAlert, badge: Int): PendingIntent {
		val intent = Intent(context, AlarmReceiver::class.java)
			.setAction(alert.id)
			.putExtra(EXTRA_TITLE, alert.title)
			.putExtra(EXTRA_BODY, alert.body)
			.putExtra(EXTRA_BADGE, badge)
			.putExtra(EXTRA_BATCH_ID, alert.batchId)
			.putExtra(EXTRA_STAGE_ID, alert.stageId)
			.putExtra(EXTRA_KIND, alert.kind.name)

		return PendingIntent.getBroadcast(
			context,
			alert.id.hashCode(),
			intent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
		)
	}

	fun show(title: String, body: String, badge: Int, isFold: Boolean, tag: String) {
		if (!hasPermission()) return

		val open = PendingIntent.getActivity(
			context,
			tag.hashCode(),
			Intent(context, MainActivity::class.java)
				.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
		)

		val notification = NotificationCompat.Builder(
			context,
			if (isFold) CHANNEL_FOLDS else CHANNEL_STAGES,
		)
			.setSmallIcon(android.R.drawable.ic_popup_reminder)
			.setContentTitle(title)
			.setContentText(body)
			.setStyle(NotificationCompat.BigTextStyle().bigText(body))
			.setPriority(if (isFold) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
			.setCategory(NotificationCompat.CATEGORY_ALARM)
			.setAutoCancel(true)
			.setGroup(GROUP)
			.setNumber(badge)
			.setContentIntent(open)
			.build()

		runCatching { manager.notify(tag, NOTIFICATION_ID, notification) }
		showSummary(badge)
	}

	/** The grouped header is what carries the count a launcher can display. */
	private fun showSummary(count: Int) {
		if (!hasPermission()) return
		if (count <= 0) {
			runCatching { manager.cancel(SUMMARY_ID) }
			return
		}

		val open = PendingIntent.getActivity(
			context,
			0,
			Intent(context, MainActivity::class.java),
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
		)

		val summary = NotificationCompat.Builder(context, CHANNEL_STAGES)
			.setSmallIcon(android.R.drawable.ic_popup_reminder)
			.setContentTitle(if (count == 1) "1 stage waiting on you" else "$count stages waiting on you")
			.setGroup(GROUP)
			.setGroupSummary(true)
			.setNumber(count)
			.setOnlyAlertOnce(true)
			.setContentIntent(open)
			.build()

		runCatching { manager.notify(SUMMARY_ID, summary) }
	}

	fun refreshSummary(batches: List<Batch>, now: Long = System.currentTimeMillis()) {
		showSummary(AlertScheduler.currentBadge(batches, now))
	}

	fun sendTestAlert() {
		show(
			title = "ProofLab alerts are working",
			body = "This is what a stage alert will look like.",
			badge = 0,
			isFold = false,
			tag = "test",
		)
	}

	// Android gives no way to enumerate pending alarms, so we keep our own list of what we set.
	private val bookkeeping =
		context.getSharedPreferences("alarms", Context.MODE_PRIVATE)

	private fun remember(alert: DoughAlert) {
		val ids = rememberedIds() + alert.id
		bookkeeping.edit().putStringSet(KEY_IDS, ids).apply()
	}

	private fun rememberedIds(): Set<String> =
		bookkeeping.getStringSet(KEY_IDS, emptySet()) ?: emptySet()

	private fun forgetAll() {
		bookkeeping.edit().remove(KEY_IDS).apply()
	}

	/** Used after a reboot, when alarms are gone but batches are not. */
	fun rescheduleFromDisk() {
		ensureChannels()
		reschedule(BatchRepository(context).load())
	}

	companion object {
		const val CHANNEL_STAGES = "stages"
		const val CHANNEL_FOLDS = "folds"
		const val GROUP = "com.blahmonster.prooflab.STAGES"
		const val NOTIFICATION_ID = 1
		const val SUMMARY_ID = 2

		const val EXTRA_TITLE = "title"
		const val EXTRA_BODY = "body"
		const val EXTRA_BADGE = "badge"
		const val EXTRA_BATCH_ID = "batchId"
		const val EXTRA_STAGE_ID = "stageId"
		const val EXTRA_KIND = "kind"

		private const val KEY_IDS = "ids"
	}
}
