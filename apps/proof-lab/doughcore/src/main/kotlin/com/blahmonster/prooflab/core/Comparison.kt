package com.blahmonster.prooflab.core

/**
 * Side-by-side diff of two runs. The prototyping loop is "change one thing, bake, compare",
 * and this is the compare half.
 */
class BatchComparison(val left: Batch, val right: Batch) {
	data class Row(
		val id: String,
		val label: String,
		val left: String,
		val right: String,
		val changed: Boolean,
	)

	val rows: List<Row> = buildRows()

	val changedRows: List<Row> get() = rows.filter { it.changed }

	private fun buildRows(): List<Row> {
		fun row(id: String, label: String, a: String, b: String) = Row(id, label, a, b, a != b)

		fun leavenLabel(batch: Batch): String =
			if (batch.formula.leaven == LeavenKind.SOURDOUGH) {
				"Levain ${Formatting.percent(batch.formula.prefermentedFlourPercent, 1)} flour"
			} else {
				"${batch.formula.yeastType.shortName} " +
					Formatting.percent(batch.formula.instantYeastPercent, 3)
			}

		val leftResult = left.formula.result()
		val rightResult = right.formula.result()

		val rows = mutableListOf(
			row("plan", "Plan", left.plan.name, right.plan.name),
			row(
				"load",
				"Ferment load",
				Formatting.hours(left.fermentationLoadHours),
				Formatting.hours(right.fermentationLoadHours),
			),
			row(
				"hydration",
				"Hydration",
				Formatting.percent(left.formula.hydrationPercent, 1),
				Formatting.percent(right.formula.hydrationPercent, 1),
			),
			row(
				"salt",
				"Salt",
				Formatting.percent(left.formula.saltPercent, 2),
				Formatting.percent(right.formula.saltPercent, 2),
			),
			row("leaven", "Leaven", leavenLabel(left), leavenLabel(right)),
			row(
				"preferment",
				"Preferment",
				left.formula.prefermentKind.displayName,
				right.formula.prefermentKind.displayName,
			),
			row(
				"ball",
				"Ball weight",
				Formatting.grams(left.formula.ballWeightGrams),
				Formatting.grams(right.formula.ballWeightGrams),
			),
			row(
				"flour",
				"Total flour",
				Formatting.grams(leftResult.totalFlourGrams),
				Formatting.grams(rightResult.totalFlourGrams),
			),
			row(
				"oil",
				"Oil",
				Formatting.percent(left.formula.oilPercent, 1),
				Formatting.percent(right.formula.oilPercent, 1),
			),
			row(
				"sugar",
				"Sugar",
				Formatting.percent(left.formula.sugarPercent, 1),
				Formatting.percent(right.formula.sugarPercent, 1),
			),
		)

		val a = left.review
		val b = right.review
		if (a != null && b != null) {
			a.axes.zip(b.axes).forEach { (axisA, axisB) ->
				rows.add(row("score-${axisA.id}", axisA.name, "${axisA.score}/5", "${axisB.score}/5"))
			}
		}

		return rows
	}
}
