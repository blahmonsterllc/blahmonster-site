package com.blahmonster.prooflab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blahmonster.prooflab.AppViewModel
import com.blahmonster.prooflab.core.Batch
import com.blahmonster.prooflab.core.BatchComparison
import com.blahmonster.prooflab.core.BatchReview
import com.blahmonster.prooflab.core.Formatting
import com.blahmonster.prooflab.ui.BadgePill
import com.blahmonster.prooflab.ui.BlockButton
import com.blahmonster.prooflab.ui.Clock
import com.blahmonster.prooflab.ui.EmptyState
import com.blahmonster.prooflab.ui.FieldLabel
import com.blahmonster.prooflab.ui.Hairline
import com.blahmonster.prooflab.ui.LocalPalette
import com.blahmonster.prooflab.ui.Panel
import com.blahmonster.prooflab.ui.ProofType
import com.blahmonster.prooflab.ui.SmallButton

/**
 * The prototyping record. Bakes you've finished, what you scored them, and a diff between any
 * two so you can see what actually changed.
 */
@Composable
fun LogScreen(model: AppViewModel, onOpenBatch: (String) -> Unit) {
	val palette = LocalPalette.current
	val batches by model.batches.collectAsStateWithLifecycle()
	val logged = batches.filter { it.isArchived || it.isFinished }.sortedByDescending { it.createdAt }
	var comparing by remember { mutableStateOf(listOf<String>()) }

	val selected = logged.filter { comparing.contains(it.id) }
	val comparison = if (selected.size == 2) BatchComparison(selected[0], selected[1]) else null

	LazyColumn(
		contentPadding = PaddingValues(16.dp),
		verticalArrangement = Arrangement.spacedBy(14.dp),
	) {
		if (logged.isEmpty()) {
			item {
				EmptyState(
					title = "No bakes logged",
					message = "Finish a batch and rate it, and it'll show up here to compare against the next one.",
				)
			}
		}

		if (comparison != null) {
			item { ComparisonPanel(comparison) }
		} else if (comparing.size == 1) {
			item {
				Panel {
					Text(
						"Pick a second bake to compare against.",
						style = ProofType.small,
						color = palette.inkMute,
					)
				}
			}
		}

		items(logged, key = { it.id }) { batch ->
			LogRow(
				batch = batch,
				selected = comparing.contains(batch.id),
				onOpen = { onOpenBatch(batch.id) },
				onToggle = {
					comparing = when {
						comparing.contains(batch.id) -> comparing - batch.id
						// Only two at a time; a third selection replaces the oldest.
						comparing.size >= 2 -> comparing.drop(1) + batch.id
						else -> comparing + batch.id
					}
				},
				onClone = { model.clone(batch, System.currentTimeMillis()) },
				onUnarchive = { model.setArchived(batch, false) },
			)
		}
	}
}

@Composable
private fun LogRow(
	batch: Batch,
	selected: Boolean,
	onOpen: () -> Unit,
	onToggle: () -> Unit,
	onClone: () -> Unit,
	onUnarchive: () -> Unit,
) {
	val palette = LocalPalette.current

	Panel {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Column(
				Modifier
					.weight(1f)
					.clickable(onClick = onOpen),
			) {
				Text(batch.name, style = ProofType.heading, color = palette.ink)
				Text(Clock.full(batch.createdAt), style = ProofType.small, color = palette.inkMute)
			}
			batch.review?.let { BadgePill("${it.overall}/5", palette.ink, filled = false) }
			SmallButton(
				title = if (selected) "Comparing" else "Compare",
				tint = if (selected) palette.hot else palette.inkMute,
				onClick = onToggle,
			)
		}

		Hairline()

		Text(
			"${batch.plan.name} · ${batch.formula.blend.summary}",
			style = ProofType.small,
			color = palette.inkMute,
		)

		Row(verticalAlignment = Alignment.CenterVertically) {
			Text(
				"${Formatting.hours(batch.fermentationLoadHours)} load",
				style = ProofType.small,
				color = palette.inkMute,
				modifier = Modifier.weight(1f),
			)
			Text(
				"${Formatting.percent(batch.formula.hydrationPercent, 1)} hydration",
				style = ProofType.small,
				color = palette.inkMute,
			)
		}

		if (batch.notes.isNotEmpty()) {
			Text(batch.notes, style = ProofType.small, color = palette.inkSoft, maxLines = 3)
		}

		Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
			SmallButton("Clone & tweak", onClick = onClone)
			if (batch.isArchived) SmallButton("Unarchive", onClick = onUnarchive)
		}
	}
}

@Composable
private fun ComparisonPanel(comparison: BatchComparison) {
	val palette = LocalPalette.current

	Panel(title = "Compare", trailing = "${comparison.changedRows.size} differences") {
		Row {
			Text(
				comparison.left.name,
				style = ProofType.strong,
				color = palette.ink,
				modifier = Modifier.weight(1f),
			)
			Text(
				comparison.right.name,
				style = ProofType.strong,
				color = palette.ink,
				modifier = Modifier.weight(1f),
				textAlign = TextAlign.End,
			)
		}
		Hairline()
		comparison.rows.forEach { row ->
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(
					row.left,
					style = if (row.changed) ProofType.strong else ProofType.small,
					color = if (row.changed) palette.ink else palette.inkMute,
					modifier = Modifier.weight(1f),
				)
				Text(
					row.label,
					style = ProofType.label,
					color = palette.inkMute,
					modifier = Modifier.width(96.dp),
					textAlign = TextAlign.Center,
				)
				Text(
					row.right,
					style = if (row.changed) ProofType.strong else ProofType.small,
					color = if (row.changed) palette.ink else palette.inkMute,
					modifier = Modifier.weight(1f),
					textAlign = TextAlign.End,
				)
			}
		}
	}
}

@Composable
fun ReviewScreen(model: AppViewModel, batchId: String, onDone: () -> Unit) {
	val palette = LocalPalette.current
	val batches by model.batches.collectAsStateWithLifecycle()
	val batch = batches.firstOrNull { it.id == batchId }

	if (batch == null) {
		EmptyState(title = "Gone", message = "This batch has been deleted.")
		return
	}

	var review by remember { mutableStateOf(batch.review ?: BatchReview()) }

	LazyColumn(
		contentPadding = PaddingValues(16.dp),
		verticalArrangement = Arrangement.spacedBy(14.dp),
	) {
		item {
			Panel(title = "How was it?") {
				Score("Handling", review.handling) { review = review.copy(handling = it) }
				Hairline()
				Score("Extensibility", review.extensibility) { review = review.copy(extensibility = it) }
				Hairline()
				Score("Oven spring", review.ovenSpring) { review = review.copy(ovenSpring = it) }
				Hairline()
				Score("Crumb", review.crumb) { review = review.copy(crumb = it) }
				Hairline()
				Score("Flavour", review.flavor) { review = review.copy(flavor = it) }
				Hairline()
				Score("Crust", review.crust) { review = review.copy(crust = it) }
				Hairline()
				Score("Overall", review.overall) { review = review.copy(overall = it) }
			}
		}

		item {
			Panel(title = "Notes") {
				OutlinedTextField(
					value = review.notes,
					onValueChange = { review = review.copy(notes = it) },
					modifier = Modifier
						.fillMaxWidth()
						.height(140.dp),
					textStyle = ProofType.body,
				)
				Hairline()
				Row(verticalAlignment = Alignment.CenterVertically) {
					Text(
						"Worth making again",
						style = ProofType.body,
						color = palette.ink,
						modifier = Modifier.weight(1f),
					)
					Switch(
						checked = review.wouldRepeat,
						onCheckedChange = { review = review.copy(wouldRepeat = it) },
					)
				}
			}
		}

		item {
			BlockButton("Save", palette.hot) {
				model.setReview(batch.id, review)
				onDone()
			}
		}
	}
}

@Composable
private fun Score(label: String, value: Int, onChange: (Int) -> Unit) {
	val palette = LocalPalette.current
	Row(verticalAlignment = Alignment.CenterVertically) {
		FieldLabel(label, Modifier.weight(1f))
		(1..5).forEach { step ->
			Box(
				Modifier
					.padding(start = 4.dp)
					.background(if (value >= step) palette.ink else Color.Transparent)
					.border(1.dp, if (value >= step) palette.ink else palette.hairline)
					.clickable { onChange(step) }
					.padding(horizontal = 9.dp, vertical = 5.dp),
			) {
				Text(
					"$step",
					style = ProofType.label,
					color = if (value >= step) palette.paper else palette.inkMute,
				)
			}
		}
	}
}
