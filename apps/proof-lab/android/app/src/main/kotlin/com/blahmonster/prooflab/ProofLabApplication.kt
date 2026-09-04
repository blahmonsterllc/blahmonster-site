package com.blahmonster.prooflab

import android.app.Application
import com.blahmonster.prooflab.notify.Notifier

class ProofLabApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		Notifier(this).ensureChannels()
	}
}
