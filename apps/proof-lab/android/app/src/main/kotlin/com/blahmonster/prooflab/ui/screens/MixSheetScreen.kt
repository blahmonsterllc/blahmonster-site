package com.blahmonster.prooflab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blahmonster.prooflab.AppViewModel
import com.blahmonster.prooflab.core.DoughTemperature
import com.blahmonster.prooflab.core.Formatting
import com.blahmonster.prooflab.core.MixerKind
import com.blahmonster.prooflab.ui.EmptyState
import com.blahmonster.prooflab.ui.Hairline
import com.blahmonster.prooflab.ui.IngredientRow
import com.blahmonster.prooflab.ui.LocalPalette
import com.blahmonster.prooflab.ui.NumberField
import com.blahmonster.prooflab.ui.Panel
import com.blahmonster.prooflab.ui.ProofType
import com.blahmonster.prooflab.ui.StatTile

/**
 * The sheet you'd tape to the mixer: what to weigh, how many loads, and what temperature the
 * water needs to be so the dough lands on target.
 */
@Composable
fun MixSheetScreen(model: AppViewModel, batchId: String) {
	val palette = LocalPalette.current
	val batches by model.batches.collectAsStateWithLifecycle()
	val batch = batches.firstOrNull { it.id == batchId }

	if (batch == null) {
		EmptyState(title = "Gone", message = "This batch has been deleted.")
		return
	}

	var capacity by remember { mutableStateOf(batch.mixerCapacityKg) }
	var mixer by remember { mutableStateOf(model.mixer) }
	var desiredDoughTempC by remember { mutableStateOf(24.0) }
	var flourTempC by remember { mutableStateOf(21.0) }
	var roomTempC by remember { mutableStateOf(22.0) }
	var tapWaterTempC by remember { mutableStateOf(18.0) }

	val result = batch.formula.result()
	val production = result.productionPlan(capacity)
	val prefermentTempC = batch.plan.prefermentStage?.temperatureC
	val water = DoughTemperature.solve(
		desiredDoughTempC = desiredDoughTempC,
		flourTempC = flourTempC,
		roomTempC = roomTempC,
		prefermentTempC = prefermentTempC,
		mixer = mixer,
		totalWaterGrams = result.totalWaterGrams,
		tapWaterTempC = tapWaterTempC,
	)

	LazyColumn(
		contentPadding = PaddingValues(16.dp),
		verticalArrangement = Arrangement.spacedBy(14.dp),
	) {
		if (result.prefermentBuild.isNotEmpty()) {
			item {
				Panel(
					title = batch.formula.prefermentKind.displayName,
					trailing = batch.plan.prefermentStage?.let { Formatting.hours(it.hours) },
				) {
					result.prefermentBuild.forEach { IngredientRow(it, showPercent = false) }
					Hairline()
					Text(
						"Ripe weight ${Formatting.grams(result.prefermentTotalGrams)}",
						style = ProofType.strong,
						color = palette.ink,
					)
					batch.plan.prefermentStage?.let { stage ->
						Text(
							"Hold at ${Formatting.temperature(stage.temperatureC, model.useFahrenheit)} " +
								"for ${Formatting.hours(stage.hours)}.",
							style = ProofType.small,
							color = palette.inkMute,
						)
					}
				}
			}
		}

		item {
			Panel(
				title = "Water temperature",
				trailing = if (prefermentTempC == null) "3-factor" else "4-factor",
			) {
				NumberField("Target dough", desiredDoughTempC, { desiredDoughTempC = it }, "°C", 18.0..32.0, 1)
				Hairline()
				NumberField("Flour", flourTempC, { flourTempC = it }, "°C", 0.0..40.0, 1)
				Hairline()
				NumberField("Room", roomTempC, { roomTempC = it }, "°C", 0.0..45.0, 1)
				Hairline()
				NumberField("Tap water", tapWaterTempC, { tapWaterTempC = it }, "°C", 0.0..40.0, 1)
				Hairline()

				Dropdown(
					label = mixer.displayName,
					options = MixerKind.entries.map { it.name to it.displayName },
				) { mixer = MixerKind.valueOf(it) }
				Text(
					"Friction adds ${Formatting.rounded(mixer.frictionC, 1)} °C over a normal mix.",
					style = ProofType.small,
					color = palette.inkMute,
				)

				Hairline()

				Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
					StatTile(
						label = "Use water at",
						value = Formatting.temperature(water.waterTemperatureC, model.useFahrenheit),
						caption = if (water.warning == null) null else "not reachable",
						tint = if (water.warning == null) palette.ink else palette.hot,
					)
					if (water.iceGrams > 0) {
						StatTile(
							label = "Ice / water split",
							value = "${Formatting.grams(water.iceGrams)} ice",
							caption = "+ ${Formatting.grams(water.waterGrams)} tap water",
							tint = palette.cold,
						)
					} else {
						StatTile(
							label = "Total water",
							value = Formatting.grams(result.totalWaterGrams),
							caption = "no ice needed",
						)
					}
				}

				water.warning?.let {
					Text(it, style = ProofType.small, color = palette.hot)
				}
			}
		}

		item {
			Panel(title = "Production", trailing = Formatting.grams(result.totalDoughGrams)) {
				NumberField("Mixer capacity", capacity, { capacity = it }, "kg", 0.5..500.0, 1)
				Hairline()
				Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
					StatTile(
						label = if (production.mixCount == 1) "Mix" else "Mixes",
						value = "${production.mixCount}",
						caption = "${result.ballCount} pieces total",
					)
					StatTile(
						label = "Per mix",
						value = Formatting.grams(production.totalDoughGrams / production.mixCount),
						caption = "of dough",
					)
				}
			}
		}

		items(production.mixes.size) { index ->
			val mix = production.mixes[index]
			Panel(
				title = if (production.mixCount == 1) "Mix" else "Mix ${mix.id} of ${production.mixCount}",
				trailing = "${mix.ballCount} pieces",
			) {
				mix.ingredients.forEach { IngredientRow(it) }
				Hairline()
				Text(
					"Dough ${Formatting.grams(mix.doughGrams)}",
					style = ProofType.strong,
					color = palette.ink,
				)
			}
		}
	}
}
