package com.blahmonster.prooflab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import com.blahmonster.prooflab.core.Formatting
import com.blahmonster.prooflab.core.Ingredient

/** A hairline-ruled panel. Everything in the app sits in one of these. */
@Composable
fun Panel(
	modifier: Modifier = Modifier,
	title: String? = null,
	trailing: String? = null,
	content: @Composable ColumnScope.() -> Unit,
) {
	val palette = LocalPalette.current
	Column(
		modifier = modifier
			.fillMaxWidth()
			.background(palette.card)
			.border(1.dp, palette.hairline)
			.padding(16.dp),
		verticalArrangement = Arrangement.spacedBy(10.dp),
	) {
		if (title != null || trailing != null) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				if (title != null) FieldLabel(title, Modifier.weight(1f))
				if (trailing != null) {
					Text(
						trailing,
						style = ProofType.label,
						color = palette.inkMute,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
					)
				}
			}
		}
		content()
	}
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
	Text(
		text.uppercase(),
		modifier = modifier,
		style = ProofType.label,
		color = LocalPalette.current.inkMute,
	)
}

@Composable
fun Hairline() {
	Box(
		Modifier
			.fillMaxWidth()
			.height(1.dp)
			.background(LocalPalette.current.hairline),
	)
}

/** The count-of-things chip: due stages, overdue, mixes. */
@Composable
fun BadgePill(text: String, tint: Color = LocalPalette.current.hot, filled: Boolean = true) {
	val palette = LocalPalette.current
	Box(
		Modifier
			.background(if (filled) tint else Color.Transparent)
			.border(if (filled) 0.dp else 1.dp, tint)
			.padding(horizontal = 7.dp, vertical = 3.dp),
	) {
		Text(
			text.uppercase(),
			style = ProofType.label,
			color = if (filled) palette.paper else tint,
		)
	}
}

@Composable
fun RowScope.StatTile(
	label: String,
	value: String,
	caption: String? = null,
	tint: Color = LocalPalette.current.ink,
) {
	val palette = LocalPalette.current
	Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
		FieldLabel(label)
		Text(value, style = ProofType.heading, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
		if (caption != null) {
			Text(caption, style = ProofType.small, color = palette.inkMute, maxLines = 2)
		}
	}
}

/**
 * Numeric entry that keeps its own text, so a half-typed "6." doesn't get rewritten under the
 * cursor, and commits on every valid keystroke.
 */
@Composable
fun NumberField(
	label: String,
	value: Double,
	onValueChange: (Double) -> Unit,
	suffix: String? = null,
	range: ClosedFloatingPointRange<Double> = 0.0..10_000.0,
	decimals: Int = 2,
) {
	val palette = LocalPalette.current
	var text by remember { mutableStateOf(Formatting.rounded(value, decimals)) }

	// Resync when something else edits the value — the leaven suggestion button, say — but
	// never while the two already agree, or we'd rewrite text under the cursor.
	LaunchedEffect(value) {
		val parsed = text.replace(',', '.').toDoubleOrNull()
		if (parsed == null || kotlin.math.abs(parsed - value) > 1e-9) {
			text = Formatting.rounded(value, decimals)
		}
	}

	Row(verticalAlignment = Alignment.CenterVertically) {
		FieldLabel(label, Modifier.width(132.dp))
		TextField(
			value = text,
			onValueChange = { new ->
				text = new
				new.replace(',', '.').toDoubleOrNull()?.let {
					onValueChange(it.coerceIn(range.start, range.endInclusive))
				}
			},
			modifier = Modifier.weight(1f),
			textStyle = ProofType.body.copy(textAlign = TextAlign.End),
			singleLine = true,
			keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
			colors = TextFieldDefaults.colors(
				focusedContainerColor = Color.Transparent,
				unfocusedContainerColor = Color.Transparent,
			),
		)
		if (suffix != null) {
			Spacer(Modifier.width(6.dp))
			Text(suffix, style = ProofType.small, color = palette.inkMute, modifier = Modifier.width(26.dp))
		}
	}
}

@Composable
fun IntStepper(label: String, value: Int, onValueChange: (Int) -> Unit, range: IntRange = 1..5000) {
	val palette = LocalPalette.current
	Row(verticalAlignment = Alignment.CenterVertically) {
		FieldLabel(label, Modifier.width(132.dp))
		Spacer(Modifier.weight(1f))
		StepButton("−") { onValueChange((value - 1).coerceIn(range)) }
		Text(
			"$value",
			style = ProofType.body,
			color = palette.ink,
			modifier = Modifier.width(56.dp),
			textAlign = TextAlign.Center,
		)
		StepButton("+") { onValueChange((value + 1).coerceIn(range)) }
	}
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
	val palette = LocalPalette.current
	Box(
		Modifier
			.border(1.dp, palette.hairline)
			.clickable(onClick = onClick)
			.padding(horizontal = 12.dp, vertical = 6.dp),
	) {
		Text(symbol, style = ProofType.strong, color = palette.ink)
	}
}

/** A weight row on the mix sheet. */
@Composable
fun IngredientRow(ingredient: Ingredient, showPercent: Boolean = true) {
	val palette = LocalPalette.current
	Row(verticalAlignment = Alignment.CenterVertically) {
		Text(
			ingredient.name,
			style = ProofType.body,
			color = palette.inkSoft,
			modifier = Modifier.weight(1f),
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)
		if (showPercent && ingredient.bakersPercent > 0) {
			Text(
				Formatting.percent(ingredient.bakersPercent, 2),
				style = ProofType.small,
				color = palette.inkMute,
			)
			Spacer(Modifier.width(10.dp))
		}
		Text(
			Formatting.grams(ingredient.grams),
			style = ProofType.strong,
			color = palette.ink,
			modifier = Modifier.width(84.dp),
			textAlign = TextAlign.End,
		)
	}
}

/** Primary action button, flat and boxy to match the site. */
@Composable
fun BlockButton(
	title: String,
	tint: Color = LocalPalette.current.ink,
	filled: Boolean = true,
	onClick: () -> Unit,
) {
	val palette = LocalPalette.current
	Box(
		Modifier
			.fillMaxWidth()
			.background(if (filled) tint else Color.Transparent)
			.border(if (filled) 0.dp else 1.dp, tint)
			.clickable(onClick = onClick)
			.padding(vertical = 12.dp),
		contentAlignment = Alignment.Center,
	) {
		Text(
			title.uppercase(),
			style = ProofType.strong,
			color = if (filled) palette.paper else tint,
		)
	}
}

@Composable
fun SmallButton(title: String, tint: Color = LocalPalette.current.ink, onClick: () -> Unit) {
	Box(
		Modifier
			.border(1.dp, tint)
			.clickable(onClick = onClick)
			.padding(horizontal = 10.dp, vertical = 5.dp),
	) {
		Text(title, style = ProofType.small, color = tint)
	}
}

@Composable
fun EmptyState(title: String, message: String) {
	val palette = LocalPalette.current
	Column(
		Modifier
			.fillMaxWidth()
			.padding(vertical = 48.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(8.dp),
	) {
		Text(title, style = ProofType.title, color = palette.ink)
		Text(
			message,
			style = ProofType.body,
			color = palette.inkMute,
			textAlign = TextAlign.Center,
		)
	}
}

@Composable
fun ProgressBar(fraction: Double, tint: Color = LocalPalette.current.ink) {
	val palette = LocalPalette.current
	Box(
		Modifier
			.fillMaxWidth()
			.height(3.dp)
			.background(palette.hairline),
	) {
		Box(
			Modifier
				.fillMaxWidth(fraction.coerceIn(0.0, 1.0).toFloat())
				.height(3.dp)
				.background(tint),
		)
	}
}
