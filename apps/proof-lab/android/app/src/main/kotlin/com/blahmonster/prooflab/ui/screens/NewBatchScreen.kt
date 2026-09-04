package com.blahmonster.prooflab.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import com.blahmonster.prooflab.AppViewModel
import com.blahmonster.prooflab.core.Batch
import com.blahmonster.prooflab.core.DoughFormula
import com.blahmonster.prooflab.core.FermentationPlan
import com.blahmonster.prooflab.core.FlourBlend
import com.blahmonster.prooflab.core.FlourLibrary
import com.blahmonster.prooflab.core.Formatting
import com.blahmonster.prooflab.core.HOUR_MILLIS
import com.blahmonster.prooflab.core.LeavenKind
import com.blahmonster.prooflab.core.Leavening
import com.blahmonster.prooflab.core.PlanFamily
import com.blahmonster.prooflab.core.PlanLibrary
import com.blahmonster.prooflab.core.PrefermentKind
import com.blahmonster.prooflab.core.ScheduleAnchor
import com.blahmonster.prooflab.core.Scheduler
import com.blahmonster.prooflab.core.StyleLibrary
import com.blahmonster.prooflab.core.YeastType
import com.blahmonster.prooflab.ui.BlockButton
import com.blahmonster.prooflab.ui.Clock
import com.blahmonster.prooflab.ui.FieldLabel
import com.blahmonster.prooflab.ui.Hairline
import com.blahmonster.prooflab.ui.IngredientRow
import com.blahmonster.prooflab.ui.IntStepper
import com.blahmonster.prooflab.ui.LocalPalette
import com.blahmonster.prooflab.ui.NumberField
import com.blahmonster.prooflab.ui.Panel
import com.blahmonster.prooflab.ui.ProofType
import com.blahmonster.prooflab.ui.SmallButton
import com.blahmonster.prooflab.ui.StatTile
import java.util.UUID

private enum class Editor { NONE, FORMULA, FLOUR, PLAN }

/**
 * Style, then timing, then the leaven readout — which recalculates as you move stages around,
 * so you can watch a 24 hour retard become 72 and see the yeast fall to match.
 *
 * The draft lives in this composable rather than in navigation, so the sub-editors can't get
 * out of step with it.
 */
@Composable
fun NewBatchScreen(model: AppViewModel, onDone: () -> Unit) {
	val palette = LocalPalette.current
	val first = StyleLibrary.all.first()

	var styleId by remember { mutableStateOf(first.id) }
	var name by remember { mutableStateOf("") }
	var formula by remember { mutableStateOf(first.formula) }
	var plan by remember { mutableStateOf(first.plan) }
	var readyBy by remember { mutableStateOf(true) }
	var anchorMillis by remember { mutableStateOf(System.currentTimeMillis() + 12 * HOUR_MILLIS) }
	var editor by remember { mutableStateOf(Editor.NONE) }

	// The sub-editors are swapped in place rather than pushed onto the back stack, so system
	// back has to be taught to step out of them instead of abandoning the whole draft.
	BackHandler(enabled = editor != Editor.NONE) {
		editor = if (editor == Editor.FLOUR) Editor.FORMULA else Editor.NONE
	}

	when (editor) {
		Editor.FORMULA -> {
			FormulaEditor(
				formula = formula,
				onChange = { formula = it },
				onOpenFlour = { editor = Editor.FLOUR },
				onBack = { editor = Editor.NONE },
			)
			return
		}
		Editor.FLOUR -> {
			FlourBlendEditor(
				flours = formula.flours,
				hydrationPercent = formula.hydrationPercent,
				onChange = { formula = formula.copy(flours = it) },
				onBack = { editor = Editor.FORMULA },
			)
			return
		}
		Editor.PLAN -> {
			PlanEditor(
				plan = plan,
				formula = formula,
				fahrenheit = model.useFahrenheit,
				onChange = { plan = it },
				onBack = { editor = Editor.NONE },
			)
			return
		}
		Editor.NONE -> Unit
	}

	val schedule = Scheduler.build(
		plan,
		if (readyBy) ScheduleAnchor.ReadyBy(anchorMillis) else ScheduleAnchor.StartAt(System.currentTimeMillis()),
	)
	val load = plan.fermentationLoadHours
	val suggested = if (formula.leaven == LeavenKind.SOURDOUGH) {
		Leavening.levainPercent(load, formula.saltPercent, formula.blend.wholeGrainFraction) /
			(1 + formula.prefermentHydrationPercent / 100)
	} else {
		Leavening.instantYeastPercent(
			load,
			formula.saltPercent,
			formula.sugarPercent,
			formula.prefermentedFlourFraction,
			formula.blend.wholeGrainFraction,
		) * formula.yeastType.multiplier
	}
	val current = if (formula.leaven == LeavenKind.SOURDOUGH) {
		formula.prefermentedFlourPercent
	} else {
		formula.scoopedYeastPercent
	}

	LazyColumn(
		contentPadding = PaddingValues(16.dp),
		verticalArrangement = Arrangement.spacedBy(14.dp),
	) {
		item {
			Panel(title = "Style") {
				Dropdown(
					label = StyleLibrary.style(styleId)?.name ?: "Pick a style",
					options = StyleLibrary.all.map { it.id to it.name },
				) { picked ->
					styleId = picked
					StyleLibrary.style(picked)?.let {
						formula = it.formula
						plan = it.plan
					}
				}
				StyleLibrary.style(styleId)?.let { style ->
					Text(style.blurb, style = ProofType.small, color = palette.inkMute)
					if (style.formula.flourNote.isNotEmpty()) {
						Text(style.formula.flourNote, style = ProofType.small, color = palette.inkMute)
					}
				}
				Hairline()
				OutlinedTextField(
					value = name,
					onValueChange = { name = it },
					modifier = Modifier.fillMaxWidth(),
					textStyle = ProofType.body,
					label = { Text("Name", style = ProofType.label) },
					placeholder = {
						Text(StyleLibrary.style(styleId)?.name ?: "Batch", style = ProofType.body)
					},
					singleLine = true,
				)
			}
		}

		item {
			Panel(title = "Timing") {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Text(
						if (readyBy) "Ready by a set time" else "Start mixing now",
						style = ProofType.body,
						color = palette.ink,
						modifier = Modifier.weight(1f),
					)
					Switch(checked = readyBy, onCheckedChange = { readyBy = it })
				}

				if (readyBy) {
					Text(
						"Dough on the bench: ${Clock.full(anchorMillis)}",
						style = ProofType.small,
						color = palette.inkMute,
					)
					Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
						SmallButton("−6h") { anchorMillis -= 6 * HOUR_MILLIS }
						SmallButton("−1h") { anchorMillis -= HOUR_MILLIS }
						SmallButton("+1h") { anchorMillis += HOUR_MILLIS }
						SmallButton("+6h") { anchorMillis += 6 * HOUR_MILLIS }
						SmallButton("+1d") { anchorMillis += 24 * HOUR_MILLIS }
					}
				}

				Hairline()

				Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
					val late = readyBy && schedule.start < System.currentTimeMillis()
					StatTile(
						label = if (readyBy) "Start mixing" else "Mix now",
						value = Clock.dayAndTime(schedule.start),
						caption = if (late) "That's in the past — pick a later time" else null,
						tint = if (late) palette.hot else palette.ink,
					)
					StatTile(
						label = "Ready",
						value = Clock.dayAndTime(schedule.readyAt),
						caption = "${Formatting.hours(plan.hoursToReady)} start to finish",
					)
				}
			}
		}

		item {
			Panel(title = "Leaven", trailing = "${Formatting.hours(load)} load") {
				Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
					StatTile(
						label = if (formula.leaven == LeavenKind.SOURDOUGH) {
							"Prefermented flour"
						} else {
							"Yeast — ${formula.yeastType.shortName}"
						},
						value = Formatting.percent(current, 3),
						caption = "what this batch uses",
					)
					StatTile(
						label = "Suggested",
						value = Formatting.percent(suggested, 3),
						caption = "for this schedule",
						tint = palette.inkMute,
					)
				}

				if (kotlin.math.abs(suggested - current) > maxOf(0.02, current * 0.35)) {
					Text(
						if (current < suggested) {
							"Less leaven than the model expects — a slower rise, more flavour, and less margin if your room runs cool."
						} else {
							"More leaven than the model expects — watch it, it'll be ready early if the room is warm."
						},
						style = ProofType.small,
						color = palette.inkMute,
					)
				}

				BlockButton("Use the suggestion", palette.ink, filled = false) {
					formula = if (formula.leaven == LeavenKind.SOURDOUGH) {
						formula.copy(prefermentedFlourPercent = suggested)
					} else {
						formula.copy(instantYeastPercent = suggested / formula.yeastType.multiplier)
					}
				}
			}
		}

		item {
			SummaryRow(
				title = "Formula",
				detail = "${formula.ballCount} × ${Formatting.grams(formula.ballWeightGrams)} · " +
					"${Formatting.percent(formula.hydrationPercent, 1)} hydration · ${formula.blend.summary}",
			) { editor = Editor.FORMULA }
		}

		item {
			SummaryRow(
				title = "Fermentation",
				detail = "${plan.name} · ${Formatting.hours(plan.totalHours)} · ${plan.stages.size} stages",
			) { editor = Editor.PLAN }
		}

		item {
			BlockButton("Start this batch", palette.hot) {
				model.add(
					Batch(
						id = UUID.randomUUID().toString(),
						name = name.ifBlank { StyleLibrary.style(styleId)?.name ?: "Batch" },
						createdAt = System.currentTimeMillis(),
						startAt = schedule.start,
						formula = formula,
						plan = plan,
						mixerCapacityKg = model.mixerCapacityKg,
					),
				)
				onDone()
			}
		}
	}
}

@Composable
private fun SummaryRow(title: String, detail: String, onClick: () -> Unit) {
	Panel(modifier = Modifier.clickable(onClick = onClick)) {
		FieldLabel(title)
		Text(detail, style = ProofType.body, color = LocalPalette.current.ink)
	}
}

@Composable
fun Dropdown(
	label: String,
	options: List<Pair<String, String>>,
	onPick: (String) -> Unit,
) {
	val palette = LocalPalette.current
	var open by remember { mutableStateOf(false) }

	Column {
		Row(
			Modifier
				.fillMaxWidth()
				.clickable { open = true }
				.padding(vertical = 6.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Text(label, style = ProofType.body, color = palette.ink, modifier = Modifier.weight(1f))
			Text("▾", style = ProofType.body, color = palette.inkMute)
		}
		DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
			options.forEach { (id, title) ->
				DropdownMenuItem(
					text = { Text(title, style = ProofType.body) },
					onClick = {
						open = false
						onPick(id)
					},
				)
			}
		}
	}
}

// MARK: - Formula

@Composable
private fun FormulaEditor(
	formula: DoughFormula,
	onChange: (DoughFormula) -> Unit,
	onOpenFlour: () -> Unit,
	onBack: () -> Unit,
) {
	val palette = LocalPalette.current
	val result = formula.result()

	LazyColumn(
		contentPadding = PaddingValues(16.dp),
		verticalArrangement = Arrangement.spacedBy(14.dp),
	) {
		item { SmallButton("‹ Back", onClick = onBack) }

		item {
			Panel(title = "Yield") {
				IntStepper("Pieces", formula.ballCount) { onChange(formula.copy(ballCount = it)) }
				Hairline()
				NumberField(
					"Piece weight",
					formula.ballWeightGrams,
					{ onChange(formula.copy(ballWeightGrams = it)) },
					suffix = "g",
					range = 20.0..5000.0,
					decimals = 0,
				)
				Hairline()
				NumberField(
					"Loss allowance",
					formula.lossPercent,
					{ onChange(formula.copy(lossPercent = it)) },
					suffix = "%",
					range = 0.0..15.0,
					decimals = 1,
				)
				Text(
					"Scrap, what stays in the bowl, the ball you drop. Two per cent covers most days.",
					style = ProofType.small,
					color = palette.inkMute,
				)
			}
		}

		item {
			Panel(
				modifier = Modifier.clickable(onClick = onOpenFlour),
				title = "Flour",
				trailing = "${Formatting.percent(formula.blend.proteinPercent, 1)} protein",
			) {
				Text(formula.blend.summary, style = ProofType.body, color = palette.ink)
				if (formula.flourNote.isNotEmpty()) {
					Text(formula.flourNote, style = ProofType.small, color = palette.inkMute)
				}
			}
		}

		item {
			Panel(title = "Baker's percentages", trailing = "of total flour") {
				NumberField(
					"Hydration",
					formula.hydrationPercent,
					{ onChange(formula.copy(hydrationPercent = it)) },
					suffix = "%",
					range = 40.0..110.0,
					decimals = 1,
				)
				Hairline()
				NumberField(
					"Salt",
					formula.saltPercent,
					{ onChange(formula.copy(saltPercent = it)) },
					suffix = "%",
					range = 0.0..5.0,
				)
				Hairline()
				NumberField(
					"Oil",
					formula.oilPercent,
					{ onChange(formula.copy(oilPercent = it)) },
					suffix = "%",
					range = 0.0..20.0,
					decimals = 1,
				)
				Hairline()
				NumberField(
					"Sugar",
					formula.sugarPercent,
					{ onChange(formula.copy(sugarPercent = it)) },
					suffix = "%",
					range = 0.0..25.0,
					decimals = 1,
				)
				Hairline()
				NumberField(
					"Diastatic malt",
					formula.maltPercent,
					{ onChange(formula.copy(maltPercent = it)) },
					suffix = "%",
					range = 0.0..3.0,
				)
			}
		}

		item {
			Panel(title = "Leaven") {
				Dropdown(
					label = formula.leaven.displayName,
					options = LeavenKind.entries.map { it.name to it.displayName },
				) { onChange(formula.copy(leaven = LeavenKind.valueOf(it))) }

				if (formula.leaven == LeavenKind.COMMERCIAL_YEAST) {
					Dropdown(
						label = formula.yeastType.displayName,
						options = YeastType.entries.map { it.name to it.displayName },
					) { onChange(formula.copy(yeastType = YeastType.valueOf(it))) }

					NumberField(
						"Yeast (${formula.yeastType.shortName})",
						formula.instantYeastPercent * formula.yeastType.multiplier,
						{ onChange(formula.copy(instantYeastPercent = it / formula.yeastType.multiplier)) },
						suffix = "%",
						range = 0.0..5.0,
						decimals = 3,
					)

					if (formula.yeastType != YeastType.INSTANT_DRY) {
						Text(
							"Stored as ${Formatting.percent(formula.instantYeastPercent, 3)} instant-equivalent, " +
								"so switching yeast type keeps the same leavening power.",
							style = ProofType.small,
							color = palette.inkMute,
						)
					}
				} else {
					Text(
						"A sourdough formula carries its leavening in the levain — set its size below.",
						style = ProofType.small,
						color = palette.inkMute,
					)
				}
			}
		}

		item {
			Panel(title = "Preferment") {
				Dropdown(
					label = formula.prefermentKind.displayName,
					options = PrefermentKind.entries.map { it.name to it.displayName },
				) { picked ->
					val kind = PrefermentKind.valueOf(picked)
					onChange(
						formula.copy(
							prefermentKind = kind,
							prefermentHydrationPercent = if (kind == PrefermentKind.NONE) {
								formula.prefermentHydrationPercent
							} else {
								kind.defaultHydrationPercent
							},
							prefermentedFlourPercent = if (
								kind != PrefermentKind.NONE && formula.prefermentedFlourPercent == 0.0
							) {
								20.0
							} else {
								formula.prefermentedFlourPercent
							},
						),
					)
				}

				if (formula.prefermentKind != PrefermentKind.NONE) {
					NumberField(
						"Prefermented flour",
						formula.prefermentedFlourPercent,
						{ onChange(formula.copy(prefermentedFlourPercent = it)) },
						suffix = "%",
						range = 0.0..100.0,
						decimals = 1,
					)
					Hairline()
					NumberField(
						"Its hydration",
						formula.prefermentHydrationPercent,
						{ onChange(formula.copy(prefermentHydrationPercent = it)) },
						suffix = "%",
						range = 40.0..200.0,
						decimals = 0,
					)
					Hairline()
					if (formula.prefermentKind.usesStarterSeed) {
						NumberField(
							"Starter seed",
							formula.starterSeedPercent,
							{ onChange(formula.copy(starterSeedPercent = it)) },
							suffix = "%",
							range = 1.0..100.0,
							decimals = 0,
						)
						Text(
							"Ripe starter as a share of the levain's flour. Less seed means a longer, milder build.",
							style = ProofType.small,
							color = palette.inkMute,
						)
					} else {
						NumberField(
							"Yeast in it",
							formula.prefermentYeastPercent,
							{ onChange(formula.copy(prefermentYeastPercent = it)) },
							suffix = "%",
							range = 0.0..2.0,
							decimals = 3,
						)
					}

					if (result.prefermentBuild.isNotEmpty()) {
						Hairline()
						FieldLabel("Build the night before")
						result.prefermentBuild.forEach { IngredientRow(it, showPercent = false) }
					}
				}
			}
		}

		item {
			Panel(title = "Totals", trailing = Formatting.grams(result.totalDoughGrams)) {
				result.overall.forEach { IngredientRow(it) }
			}
		}
	}
}

// MARK: - Flour blend

@Composable
private fun FlourBlendEditor(
	flours: List<com.blahmonster.prooflab.core.FlourComponent>,
	hydrationPercent: Double,
	onChange: (List<com.blahmonster.prooflab.core.FlourComponent>) -> Unit,
	onBack: () -> Unit,
) {
	val palette = LocalPalette.current
	val blend = FlourBlend(flours)
	val total = flours.sumOf { it.percent }

	LazyColumn(
		contentPadding = PaddingValues(16.dp),
		verticalArrangement = Arrangement.spacedBy(14.dp),
	) {
		item { SmallButton("‹ Back", onClick = onBack) }

		item {
			Panel(title = "Blend", trailing = blend.summary) {
				Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
					StatTile(
						label = "Protein",
						value = Formatting.percent(blend.proteinPercent, 2),
						caption = "weighted average",
					)
					StatTile(
						label = "Whole grain",
						value = Formatting.percent(blend.wholeGrainFraction * 100, 0),
						caption = if (blend.wholeGrainFraction > 0) "ferments faster, dose lighter" else null,
					)
				}
				Hairline()
				Row {
					StatTile(
						label = "Absorption guide",
						value = Formatting.percent(blend.absorptionGuidePercent, 1),
						caption = if (hydrationPercent > blend.absorptionGuidePercent + 8) {
							"you're ${Formatting.rounded(hydrationPercent - blend.absorptionGuidePercent, 0)}" +
								" points above it — expect a slack dough"
						} else {
							"roughly what this blend carries comfortably"
						},
						tint = if (hydrationPercent > blend.absorptionGuidePercent + 8) palette.hot else palette.ink,
					)
				}
			}
		}

		item {
			Panel(title = "Flours", trailing = Formatting.percent(total, 1)) {
				if (flours.isEmpty()) {
					Text("No flour yet — add one below.", style = ProofType.small, color = palette.inkMute)
				}

				flours.forEachIndexed { index, flour ->
					if (index > 0) Hairline()
					Row(verticalAlignment = Alignment.CenterVertically) {
						Column(Modifier.weight(1f)) {
							Text(flour.name, style = ProofType.strong, color = palette.ink)
							Text(
								Formatting.percent(flour.proteinPercent, 1) + " protein" +
									if (flour.isWholeGrain) " · whole grain" else "",
								style = ProofType.small,
								color = palette.inkMute,
							)
						}
						SmallButton("Remove", palette.hot) {
							onChange(flours.filterIndexed { i, _ -> i != index })
						}
					}
					NumberField(
						"Share",
						flour.percent,
						{ value ->
							onChange(
								flours.mapIndexed { i, f -> if (i == index) f.copy(percent = value) else f },
							)
						},
						suffix = "%",
						range = 0.0..100.0,
						decimals = 1,
					)
				}

				if (flours.isNotEmpty() && kotlin.math.abs(total - 100.0) > 0.05) {
					Hairline()
					Text(
						"Adds to ${Formatting.percent(total, 1)}. Weights are worked out from the ratio either way.",
						style = ProofType.small,
						color = palette.inkMute,
					)
					BlockButton("Normalise to 100 %", palette.ink, filled = false) {
						onChange(blend.normalized)
					}
				}
			}
		}

		item {
			Panel(title = "Add a flour") {
				FlourLibrary.all
					.filterNot { candidate -> flours.any { it.id == candidate.id } }
					.forEach { candidate ->
						Row(
							Modifier
								.fillMaxWidth()
								.clickable {
									val remaining = (100.0 - total).coerceAtLeast(0.0)
									onChange(
										flours + FlourLibrary.at(
											candidate.id,
											if (remaining > 1) remaining else 10.0,
										),
									)
								}
								.padding(vertical = 5.dp),
							verticalAlignment = Alignment.CenterVertically,
						) {
							Text(
								candidate.name,
								style = ProofType.body,
								color = palette.ink,
								modifier = Modifier.weight(1f),
							)
							Text(
								Formatting.percent(candidate.proteinPercent, 1),
								style = ProofType.small,
								color = palette.inkMute,
							)
						}
					}
			}
		}
	}
}

// MARK: - Plan

@Composable
private fun PlanEditor(
	plan: FermentationPlan,
	formula: DoughFormula,
	fahrenheit: Boolean,
	onChange: (FermentationPlan) -> Unit,
	onBack: () -> Unit,
) {
	val palette = LocalPalette.current
	val load = plan.fermentationLoadHours

	LazyColumn(
		contentPadding = PaddingValues(16.dp),
		verticalArrangement = Arrangement.spacedBy(14.dp),
	) {
		item { SmallButton("‹ Back", onClick = onBack) }

		item {
			Panel(title = "Fermentation type") {
				PlanFamily.entries.forEach { family ->
					FieldLabel(family.displayName)
					PlanLibrary.all.filter { it.family == family }.forEach { option ->
						Row(
							Modifier
								.fillMaxWidth()
								.clickable { onChange(option) }
								.padding(vertical = 5.dp),
							verticalAlignment = Alignment.CenterVertically,
						) {
							Text(
								option.name,
								style = if (option.id == plan.id) ProofType.strong else ProofType.body,
								color = if (option.id == plan.id) palette.hot else palette.ink,
								modifier = Modifier.weight(1f),
							)
							Text(
								Formatting.hours(option.totalHours),
								style = ProofType.small,
								color = palette.inkMute,
							)
						}
					}
				}
				Hairline()
				Text(plan.summary, style = ProofType.small, color = palette.inkMute)
			}
		}

		item {
			Panel(title = "What this schedule asks for") {
				Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
					StatTile(
						label = "Ferment load",
						value = Formatting.hours(load),
						caption = "equivalent hours at 24 °C",
					)
					StatTile(
						label = "Cold time",
						value = plan.coldStage?.let { Formatting.hours(it.hours) } ?: "None",
						caption = plan.coldStage?.let {
							Formatting.temperature(it.temperatureC, fahrenheit)
						},
						tint = if (plan.coldStage == null) palette.inkMute else palette.cold,
					)
				}
			}
		}

		itemsIndexedStages(plan) { index, stage ->
			Panel(
				title = stage.title,
				trailing = if (stage.kind.countsTowardFermentation) {
					"${Formatting.hours(stage.equivalentHours)} eq"
				} else {
					null
				},
			) {
				if (stage.detail.isNotEmpty()) {
					Text(stage.detail, style = ProofType.small, color = palette.inkMute)
				}

				NumberField(
					"Duration",
					stage.hours,
					{ value ->
						onChange(
							plan.copy(
								stages = plan.stages.mapIndexed { i, s ->
									if (i == index) s.copy(hours = value) else s
								},
							),
						)
					},
					suffix = "h",
					range = 0.0..168.0,
				)

				Hairline()

				NumberField(
					"Temperature",
					stage.temperatureC,
					{ value ->
						onChange(
							plan.copy(
								stages = plan.stages.mapIndexed { i, s ->
									if (i == index) s.copy(temperatureC = value) else s
								},
							),
						)
					},
					suffix = "°C",
					range = -2.0..45.0,
					decimals = 1,
				)

				if (stage.kind.isCold) {
					Hairline()
					NumberField(
						"Usable window",
						stage.usableWindowHours ?: 0.0,
						{ value ->
							onChange(
								plan.copy(
									stages = plan.stages.mapIndexed { i, s ->
										if (i == index) {
											s.copy(usableWindowHours = value.takeIf { it > 0 })
										} else {
											s
										}
									},
								),
							)
						},
						suffix = "h",
						range = 0.0..96.0,
						decimals = 1,
					)
					Text(
						"How long the dough stays good past ready. ProofLab alerts you at both edges " +
							"so a tray doesn't quietly go over.",
						style = ProofType.small,
						color = palette.inkMute,
					)
				}
			}
		}
	}
}

/** Small readability helper — the stage list is the only place we need an indexed item run. */
private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedStages(
	plan: FermentationPlan,
	content: @Composable (Int, com.blahmonster.prooflab.core.PlanStage) -> Unit,
) {
	plan.stages.forEachIndexed { index, stage ->
		item(key = stage.id) { content(index, stage) }
	}
}
