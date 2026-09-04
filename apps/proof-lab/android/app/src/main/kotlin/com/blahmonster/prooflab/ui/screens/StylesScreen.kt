package com.blahmonster.prooflab.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blahmonster.prooflab.AppViewModel
import com.blahmonster.prooflab.core.Batch
import com.blahmonster.prooflab.core.DoughStyle
import com.blahmonster.prooflab.core.Formatting
import com.blahmonster.prooflab.core.PlanFamily
import com.blahmonster.prooflab.core.StyleLibrary
import com.blahmonster.prooflab.ui.BlockButton
import com.blahmonster.prooflab.ui.EmptyState
import com.blahmonster.prooflab.ui.Hairline
import com.blahmonster.prooflab.ui.IngredientRow
import com.blahmonster.prooflab.ui.LocalPalette
import com.blahmonster.prooflab.ui.Panel
import com.blahmonster.prooflab.ui.ProofType
import com.blahmonster.prooflab.ui.SmallButton
import java.util.UUID

/**
 * Reference shelf: what each style is, what it's usually fermented on, and what it weighs out
 * to. Tapping one starts a batch from it.
 */
@Composable
fun StylesScreen(onOpenStyle: (String) -> Unit) {
	val palette = LocalPalette.current
	var family by remember { mutableStateOf(PlanFamily.PIZZA) }
	val styles = StyleLibrary.styles(family)

	LazyColumn(
		contentPadding = PaddingValues(16.dp),
		verticalArrangement = Arrangement.spacedBy(14.dp),
	) {
		item {
			Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				PlanFamily.entries.forEach { option ->
					SmallButton(
						title = option.displayName,
						tint = if (option == family) palette.hot else palette.inkMute,
					) { family = option }
				}
			}
		}

		items(styles, key = { it.id }) { style ->
			Panel(modifier = Modifier.clickable { onOpenStyle(style.id) }) {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Text(style.name, style = ProofType.heading, color = palette.ink, modifier = Modifier.weight(1f))
					Text(
						Formatting.percent(style.formula.hydrationPercent, 0),
						style = ProofType.small,
						color = palette.inkMute,
					)
				}
				Text(style.blurb, style = ProofType.small, color = palette.inkMute)
				Hairline()
				Text(style.formula.blend.summary, style = ProofType.small, color = palette.inkSoft)
				Row(verticalAlignment = Alignment.CenterVertically) {
					Text(
						style.plan.name,
						style = ProofType.small,
						color = palette.inkSoft,
						modifier = Modifier.weight(1f),
					)
					Text(
						Formatting.hours(style.plan.totalHours),
						style = ProofType.small,
						color = palette.inkMute,
						textAlign = TextAlign.End,
					)
				}
			}
		}
	}
}

@Composable
fun StyleDetailScreen(model: AppViewModel, styleId: String, onStarted: (String) -> Unit) {
	val palette = LocalPalette.current
	val style: DoughStyle? = StyleLibrary.style(styleId)

	if (style == null) {
		EmptyState(title = "Not found", message = "That style isn't in the library.")
		return
	}

	val result = style.formula.result()

	LazyColumn(
		contentPadding = PaddingValues(16.dp),
		verticalArrangement = Arrangement.spacedBy(14.dp),
	) {
		item {
			Panel(title = "About") {
				Text(style.blurb, style = ProofType.body, color = palette.inkSoft)
				Hairline()
				Text(style.formula.blend.summary, style = ProofType.body, color = palette.ink)
				if (style.formula.flourNote.isNotEmpty()) {
					Text(style.formula.flourNote, style = ProofType.small, color = palette.inkMute)
				}
			}
		}

		item {
			Panel(
				title = "Formula",
				trailing = "${style.formula.ballCount} × ${Formatting.grams(style.formula.ballWeightGrams)}",
			) {
				result.overall.forEach { IngredientRow(it) }
			}
		}

		item {
			Panel(
				title = "Fermentation",
				trailing = "${Formatting.hours(style.plan.fermentationLoadHours)} load",
			) {
				Text(style.plan.summary, style = ProofType.small, color = palette.inkMute)
				Hairline()
				style.plan.stages.forEach { stage ->
					Row(
						Modifier
							.fillMaxWidth()
							.padding(vertical = 3.dp),
						verticalAlignment = Alignment.CenterVertically,
					) {
						Text(
							stage.title,
							style = ProofType.body,
							color = palette.ink,
							modifier = Modifier.weight(1f),
						)
						Text(
							Formatting.temperature(stage.temperatureC, model.useFahrenheit),
							style = ProofType.small,
							color = palette.inkMute,
						)
						Text(
							"  " + Formatting.hours(stage.hours),
							style = ProofType.strong,
							color = palette.ink,
						)
					}
				}
			}
		}

		item {
			BlockButton("Start a batch from this", palette.hot) {
				val batch = Batch(
					id = UUID.randomUUID().toString(),
					name = style.name,
					createdAt = System.currentTimeMillis(),
					startAt = System.currentTimeMillis(),
					formula = style.formula,
					plan = style.plan,
					mixerCapacityKg = model.mixerCapacityKg,
				)
				model.add(batch)
				onStarted(batch.id)
			}
		}
	}
}
