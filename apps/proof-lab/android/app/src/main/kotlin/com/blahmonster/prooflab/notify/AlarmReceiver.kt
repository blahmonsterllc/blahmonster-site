package com.blahmonster.prooflab.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.blahmonster.prooflab.core.DoughAlert

/** Fires when a stage's timer is up and turns the alarm into a notification. */
class AlarmReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		val title = intent.getStringExtra(Notifier.EXTRA_TITLE) ?: return
		val body = intent.getStringExtra(Notifier.EXTRA_BODY).orEmpty()
		val badge = intent.getIntExtra(Notifier.EXTRA_BADGE, 0)
		val kind = intent.getStringExtra(Notifier.EXTRA_KIND)
		val tag = intent.action ?: title

		Notifier(context.applicationContext).show(
			title = title,
			body = body,
			badge = badge,
			isFold = kind == DoughAlert.Kind.FOLD.name,
			tag = tag,
		)
	}
}

/** Alarms don't survive a reboot or an update, so put them back. */
class BootReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		when (intent.action) {
			Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED ->
				Notifier(context.applicationContext).rescheduleFromDisk()
		}
	}
}
