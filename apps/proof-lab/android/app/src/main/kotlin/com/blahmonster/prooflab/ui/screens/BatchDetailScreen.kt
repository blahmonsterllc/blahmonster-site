package com.blahmonster.prooflab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blahmonster.prooflab.AppViewModel
import com.blahmonster.prooflab.core.Batch
import com.blahmonster.prooflab.core.Formatting
import com.blahmonster.prooflab.core.LeavenKind
import com.blahmonster.prooflab.core.ScheduledStage
import com.blahmonster.prooflab.core.StageStatus
import com.blahmonster.prooflab.ui.BadgePill
import com.blahmonster.prooflab.ui.BlockButton
import com.blahmonster.prooflab.ui.Clock
import com.blahmonster.prooflab.ui.EmptyState
import com.blahmonster.prooflab.ui.Hairline
import com.blahmonster.prooflab.ui.LocalPalette
import com.blahmonster.prooflab.ui.Panel
import com.blahmonster.prooflab.ui.ProofType
import com.blahmonster.prooflab.ui.SmallButton
import com.blahmonster.prooflab.ui.StatTile
import com.blahmonster.prooflab.ui.tint

@Composable
fun BatchDetailScreen(
	model: AppViewModel,
	batchId: String,
	onOpenMixSheet: (String) -> Unit,
	onOpenReview: (String) -> Unit,
	onGone: () -> Unit,
) {
	val batches by model.batches.collectAsStateWithLifecycle()
	val batch = batches.firstOrNull { it.id == batchId }
	val now = rememberTicker()

	if (batch == null) {
		EmptyState(title = "Gone", message = "This batch has been deleted.")
		return
	}

	LazyColumn(
		contentPadding = PaddingValues(16.dp),
		verticalArrangement = Arrangement.spacedBy(14.dp),
	) {
		item { Summary(batch, now, model.useFahrenheit) }

		item {
			Panel(title = "Schedule") {
				batch.timeline.forEachIndexed { index, stage ->
					if (index > 0) Hairline()
					StageRow(
						batch = batch,
						stage = stage,
						now = now,
						fahrenheit = model.useFahrenheit,
						onComplete = { model.complete(batch.id, stage.id) },
						onReopen = { model.reopen(batch.id, stage.id) },
						onExtend = { model.adjust(batch.id, stage.id, 0.25) },
						onShorten = { model.adjust(batch.id, stage.id, -0.25) },
					)
				}
			}
		}

		item {
			Panel(title = "Notes") {
				OutlinedTextField(
					value = batch.notes,
					onValueChange = { model.setNotes(batch.id, it) },
					modifier = Modifier
						.fillMaxWidth()
						.height(120.dp),
					textStyle = ProofType.body,
					placeholder = { Text("What did you change this time?", style = ProofType.body) },
				)
			}
		}

		item {
			Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
				BlockButton("Mix sheet", LocalPalette.current.ink, filled = false) {
					onOpenMixSheet(batch.id)
				}
				BlockButton("Rate this bake", LocalPalette.current.ink, filled = false) {
					onOpenReview(batch.id)
				}
				BlockButton("Clone & tweak", LocalPalette.current.ink, filled = false) {
					model.clone(batch, System.currentTimeMillis())
				}
				BlockButton("Archive", LocalPalette.current.inkMute, filled = false) {
					model.setArchived(batch, true)
				}
				BlockButton("Delete", LocalPalette.current.hot, filled = false) {
					model.delete(batch)
					onGone()
				}
			}
		}
	}
}

@Composable
private fun Summary(batch: Batch, now: Long, fahrenheit: Boolean) {
	val palette = LocalPalette.current
	val due = batch.dueStages(now)
	val result = batch.formula.result()

	Panel(title = "This run", trailing = batch.plan.name) {
		if (due.isNotEmpty()) {
			BadgePill(if (due.size == 1) "1 stage due" else "${due.size} stages due")
		}

		Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
			StatTile(
				label = "Ready",
				value = Clock.dayAndTime(batch.readyAt),
				caption = "mixed ${Clock.dayAndTime(batch.startAt)}",
			)
			StatTile(
				label = "Ferment load",
				value = Formatting.hours(batch.fermentationLoadHours),
				caption = "equivalent hours at 24 °C",
			)
		}

		Hairline()

		Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
			if (batch.formula.leaven == LeavenKind.SOURDOUGH) {
				// The model suggests a levain weight; the formula is expressed in prefermented
				// flour. Convert rather than showing two numbers that look contradictory.
				val suggested = batch.suggestedLevainPercent /
					(1 + batch.formula.prefermentHydrationPercent / 100)
				StatTile(
					label = "Prefermented flour",
					value = Formatting.percent(batch.formula.prefermentedFlourPercent, 1),
					caption = "suggested ${Formatting.percent(suggested, 1)}",
				)
			} else {
				StatTile(
					label = "Yeast (${batch.formula.yeastType.shortName})",
					value = Formatting.percent(batch.formula.scoopedYeastPercent, 3),
					caption = "suggested " + Formatting.percent(
						batch.suggestedInstantYeastPercent * batch.formula.yeastType.multiplier,
						3,
					),
				)
			}
			StatTile(
				label = "Dough",
				value = "${batch.formula.ballCount} × ${Formatting.grams(batch.formula.ballWeightGrams)}",
				caption = "${Formatting.grams(result.totalDoughGrams)} total",
			)
		}

		Hairline()

		Text(batch.formula.blend.summary, style = ProofType.small, color = palette.inkMute)
	}
}

@Composable
private fun StageRow(
	batch: Batch,
	stage: ScheduledStage,
	now: Long,
	fahrenheit: Boolean,
	onComplete: () -> Unit,
	onReopen: () -> Unit,
	onExtend: () -> Unit,
	onShorten: () -> Unit,
) {
	val palette = LocalPalette.current
	val status = batch.status(stage, now)

	Column(
		Modifier.padding(vertical = 8.dp),
		verticalArrangement = Arrangement.spacedBy(6.dp),
	) {
		Row(verticalAlignment = Alignment.Top) {
			Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
				Text(
					stage.stage.title,
					style = ProofType.strong,
					color = if (status == StageStatus.DONE) palette.inkMute else palette.ink,
					textDecoration = if (status == StageStatus.DONE) TextDecoration.LineThrough else null,
				)
				Text(
					buildList {
						add(Clock.dayAndTime(stage.start))
						add(Formatting.temperature(stage.stage.temperatureC, fahrenheit))
						if (stage.stage.kind.countsTowardFermentation) {
							add("${Formatting.hours(stage.stage.equivalentHours)} eq")
						}
					}.joinToString(" · "),
					style = ProofType.small,
					color = palette.inkMute,
				)
			}
			Spacer(Modifier.width(8.dp))
			Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
				Text(
					when (status) {
						StageStatus.DONE -> "—"
						StageStatus.UPCOMING -> Formatting.hours(stage.stage.hours)
						else -> Formatting.countdown(stage.end - now)
					},
					style = ProofType.strong,
					color = status.tint(),
				)
				StatusPill(status)
			}
		}

		stage.windowEnd?.let { window ->
			if (status != StageStatus.DONE) {
				Text(
					"Usable until ${Clock.dayAndTime(window)}",
					style = ProofType.small,
					color = palette.cold,
				)
			}
		}

		if (stage.foldTimes.isNotEmpty() &&
			(status == StageStatus.ACTIVE || status == StageStatus.UPCOMING)
		) {
			Text(
				"Folds: " + stage.foldTimes.joinToString(", ") { Clock.time(it) },
				style = ProofType.small,
				color = palette.inkMute,
			)
		}

		if (status != StageStatus.UPCOMING) {
			Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				if (status == StageStatus.DONE) {
					SmallButton("Reopen", onClick = onReopen)
				} else {
					SmallButton("Done", palette.hot, onClick = onComplete)
					SmallButton("−15m", onClick = onShorten)
					SmallButton("+15m", onClick = onExtend)
				}
			}
		}
	}
}
