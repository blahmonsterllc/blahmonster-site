package com.blahmonster.prooflab.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blahmonster.prooflab.AppViewModel
import com.blahmonster.prooflab.core.Batch
import com.blahmonster.prooflab.core.Formatting
import com.blahmonster.prooflab.core.StageStatus
import com.blahmonster.prooflab.ui.BadgePill
import com.blahmonster.prooflab.ui.BlockButton
import com.blahmonster.prooflab.ui.Clock
import com.blahmonster.prooflab.ui.EmptyState
import com.blahmonster.prooflab.ui.Hairline
import com.blahmonster.prooflab.ui.LocalPalette
import com.blahmonster.prooflab.ui.Panel
import com.blahmonster.prooflab.ui.ProgressBar
import com.blahmonster.prooflab.ui.ProofType
import com.blahmonster.prooflab.ui.label
import com.blahmonster.prooflab.ui.tint
import kotlinx.coroutines.delay

/** One ticking clock for the whole screen rather than a timer per card. */
@Composable
fun rememberTicker(): Long {
	val now by produceState(initialValue = System.currentTimeMillis()) {
		while (true) {
			value = System.currentTimeMillis()
			delay(1_000)
		}
	}
	return now
}

@Composable
fun ProofingScreen(
	model: AppViewModel,
	onOpenBatch: (String) -> Unit,
	onRequestNotifications: () -> Unit,
) {
	val batches by model.batches.collectAsStateWithLifecycle()
	val allowed by model.notificationsAllowed.collectAsStateWithLifecycle()
	val now = rememberTicker()
	val active = batches.filter { !it.isArchived && !it.isFinished }.sortedBy { it.readyAt }

	LazyColumn(
		contentPadding = PaddingValues(16.dp),
		verticalArrangement = Arrangement.spacedBy(14.dp),
	) {
		if (!allowed) {
			item {
				Panel(title = "Alerts are off") {
					Text(
						"Without notification permission ProofLab can't tell you when a stage is up — which is most of the point of it.",
						style = ProofType.body,
						color = LocalPalette.current.inkSoft,
					)
					BlockButton("Turn alerts on", LocalPalette.current.hot, onClick = onRequestNotifications)
				}
			}
		}

		if (active.isEmpty()) {
			item {
				EmptyState(
					title = "Nothing proofing",
					message = "Start a batch and ProofLab will alert you when each stage is up — including overnight in the fridge.",
				)
			}
		}

		items(active, key = { it.id }) { batch ->
			BatchCard(
				batch = batch,
				now = now,
				fahrenheit = model.useFahrenheit,
				onClick = { onOpenBatch(batch.id) },
			)
		}
	}
}

@Composable
private fun BatchCard(batch: Batch, now: Long, fahrenheit: Boolean, onClick: () -> Unit) {
	val palette = LocalPalette.current
	val current = batch.currentStage(now)
	val status = current?.let { batch.status(it, now) } ?: StageStatus.DONE
	val due = batch.dueStages(now)

	Panel(modifier = Modifier.clickable(onClick = onClick)) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Text(batch.name, style = ProofType.title, color = palette.ink, modifier = Modifier.weight(1f))
			if (due.isNotEmpty()) {
				BadgePill(if (due.size == 1) "1 due" else "${due.size} due")
			}
		}

		Text(
			"${batch.plan.name} · ${batch.formula.ballCount} × ${Formatting.grams(batch.formula.ballWeightGrams)}",
			style = ProofType.small,
			color = palette.inkMute,
		)

		Hairline()

		if (current != null) {
			Row(verticalAlignment = Alignment.Top) {
				Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
					Text(current.stage.title, style = ProofType.strong, color = palette.ink)
					Text(
						when (status) {
							StageStatus.UPCOMING -> "Starts ${Clock.time(current.start)}"
							StageStatus.ACTIVE -> "At ${Formatting.temperature(current.stage.temperatureC, fahrenheit)}"
							StageStatus.DUE -> "Timer's up"
							StageStatus.OVERDUE -> "Overdue — check it"
							StageStatus.DONE -> "Done"
						},
						style = ProofType.small,
						color = status.tint(),
					)
				}
				Spacer(Modifier.width(12.dp))
				Column(horizontalAlignment = Alignment.End) {
					Text(
						Formatting.countdown(current.end - now),
						style = ProofType.clock,
						color = status.tint(),
					)
					Text(
						if (status == StageStatus.UPCOMING) "until it starts" else "remaining",
						style = ProofType.small,
						color = palette.inkMute,
					)
				}
			}

			val span = (current.end - current.start).toDouble()
			ProgressBar(if (span > 0) (now - current.start) / span else 1.0, status.tint())
		} else {
			Text(
				"All stages done — log it when you've tasted it.",
				style = ProofType.body,
				color = palette.inkMute,
			)
		}

		Hairline()

		Row(verticalAlignment = Alignment.CenterVertically) {
			Text(
				"Ready ${Clock.dayAndTime(batch.readyAt)}",
				style = ProofType.small,
				color = palette.inkMute,
				modifier = Modifier.weight(1f),
			)
			Text(
				"${Formatting.hours(batch.fermentationLoadHours)} load",
				style = ProofType.small,
				color = palette.inkMute,
				textAlign = TextAlign.End,
			)
		}
	}
}

@Composable
fun StatusPill(status: StageStatus) {
	BadgePill(
		text = status.label,
		tint = status.tint(),
		filled = status == StageStatus.DUE || status == StageStatus.OVERDUE,
	)
}
