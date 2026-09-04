package com.blahmonster.prooflab.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Schedules routinely span two or three days, so the weekday matters more than the date.
 * "Fri 17:30" is what a baker reads off a production sheet.
 */
object Clock {
	private val dayTime: DateTimeFormatter =
		DateTimeFormatter.ofPattern("EEE HH:mm", Locale.getDefault())
	private val dateTime: DateTimeFormatter =
		DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.getDefault())
	private val timeOnly: DateTimeFormatter =
		DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

	fun dayAndTime(epochMillis: Long): String = format(epochMillis, dayTime)

	fun full(epochMillis: Long): String = format(epochMillis, dateTime)

	fun time(epochMillis: Long): String = format(epochMillis, timeOnly)

	private fun format(epochMillis: Long, formatter: DateTimeFormatter): String =
		Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(formatter)
}
