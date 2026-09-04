package com.blahmonster.prooflab.core

/**
 * The fermentation types the app ships with. Durations and temperatures are starting points —
 * every stage is editable per batch, and the yeast dose follows whatever you change them to.
 *
 * Kept byte-for-byte in step with the Swift `FermentationPlan.library`; the conformance
 * fixtures assert that both sides compute the same fermentation loads.
 */
object PlanLibrary {
	val sameDayStraight = FermentationPlan(
		id = "same-day-straight",
		name = "Same day — straight",
		family = PlanFamily.PIZZA,
		leaven = LeavenKind.COMMERCIAL_YEAST,
		summary = "Mix in the morning, bake for dinner. No fridge, no preferment.",
		stages = listOf(
			PlanStage("mix", StageKind.MIX, "Mix", "Mix to a smooth, moderately developed dough.", 0.25, 22.0, alerts = false),
			PlanStage("bulk", StageKind.BULK, "Bulk ferment", "Covered, at room temperature.", 3.0, 24.0, foldIntervalMinutes = 45),
			PlanStage("divide", StageKind.DIVIDE, "Divide & ball", hours = 0.25, temperatureC = 22.0, alerts = false),
			PlanStage("proof", StageKind.FINAL_PROOF, "Final proof", "Balls at room temperature until puffy and slack.", 2.0, 24.0),
			PlanStage("bake", StageKind.BAKE, "Bake", hours = 0.25, temperatureC = 24.0, alerts = false),
		),
	)

	val coldBallRetard = FermentationPlan(
		id = "cold-ball-retard",
		name = "Cold ball retard",
		family = PlanFamily.PIZZA,
		leaven = LeavenKind.COMMERCIAL_YEAST,
		summary = "The pizzeria standard. Ball early, retard in trays, temper to order.",
		stages = listOf(
			PlanStage("mix", StageKind.MIX, "Mix", hours = 0.25, temperatureC = 22.0, alerts = false),
			PlanStage("bulk", StageKind.BULK, "Puntata", "Short bulk before dividing.", 1.5, 22.0),
			PlanStage("divide", StageKind.DIVIDE, "Divide & ball", "Into oiled trays, lidded.", 0.3, 22.0, alerts = false),
			PlanStage("cold", StageKind.COLD_RETARD, "Cold ferment", "Walk-in at 4 °C.", 48.0, 4.0, usableWindowHours = 24.0),
			PlanStage("temper", StageKind.TEMPER, "Temper", "Out of the fridge until the balls relax.", 3.0, 20.0),
			PlanStage("bake", StageKind.BAKE, "Bake", hours = 0.25, temperatureC = 22.0, alerts = false),
		),
	)

	val coldBulkRetard = FermentationPlan(
		id = "cold-bulk-retard",
		name = "Cold bulk retard",
		family = PlanFamily.PIZZA,
		leaven = LeavenKind.COMMERCIAL_YEAST,
		summary = "Retard the mass, divide the day of. Saves fridge space over balling early.",
		stages = listOf(
			PlanStage("mix", StageKind.MIX, "Mix", hours = 0.25, temperatureC = 22.0, alerts = false),
			PlanStage("bulk", StageKind.BULK, "Bulk ferment", hours = 1.0, temperatureC = 22.0),
			PlanStage("cold", StageKind.COLD_RETARD, "Cold bulk", "Whole mass, covered.", 24.0, 4.0, usableWindowHours = 24.0),
			PlanStage("divide", StageKind.DIVIDE, "Divide & ball", hours = 0.3, temperatureC = 20.0, alerts = false),
			PlanStage("proof", StageKind.FINAL_PROOF, "Warm up & proof", hours = 4.0, temperatureC = 20.0),
			PlanStage("bake", StageKind.BAKE, "Bake", hours = 0.25, temperatureC = 22.0, alerts = false),
		),
	)

	val neapolitanDirect = FermentationPlan(
		id = "neapolitan-direct",
		name = "Neapolitan — direct",
		family = PlanFamily.PIZZA,
		leaven = LeavenKind.COMMERCIAL_YEAST,
		summary = "Room-temperature puntata and appretto, the way the disciplinare describes it.",
		stages = listOf(
			PlanStage("mix", StageKind.MIX, "Impasto", "Slow mix, finish at 23 °C.", 0.35, 20.0, alerts = false),
			PlanStage("puntata", StageKind.BULK, "Puntata", "Mass rest, covered.", 2.0, 20.0),
			PlanStage("staglio", StageKind.DIVIDE, "Staglio", "Divide and ball, into wooden boxes.", 0.3, 20.0, alerts = false),
			PlanStage("appretto", StageKind.FINAL_PROOF, "Appretto", "Balls proof until nearly doubled.", 6.0, 20.0),
			PlanStage("bake", StageKind.BAKE, "Bake", "60–90 seconds at 430–480 °C.", 0.2, 22.0, alerts = false),
		),
	)

	val poolishPizza = FermentationPlan(
		id = "poolish-pizza",
		name = "Poolish",
		family = PlanFamily.PIZZA,
		leaven = LeavenKind.COMMERCIAL_YEAST,
		summary = "Wet preferment overnight for extensibility and a sweeter, milkier crumb.",
		prefermentKind = PrefermentKind.POOLISH,
		prefermentedFlourPercent = 30.0,
		prefermentHydrationPercent = 100.0,
		stages = listOf(
			PlanStage("poolish", StageKind.PREFERMENT, "Poolish", "Equal flour and water, a pinch of yeast. Ready when domed and just starting to fall.", 14.0, 20.0),
			PlanStage("mix", StageKind.MIX, "Mix", hours = 0.3, temperatureC = 22.0, alerts = false),
			PlanStage("bulk", StageKind.BULK, "Bulk ferment", hours = 1.0, temperatureC = 22.0),
			PlanStage("divide", StageKind.DIVIDE, "Divide & ball", hours = 0.3, temperatureC = 22.0, alerts = false),
			PlanStage("cold", StageKind.COLD_RETARD, "Cold ferment", hours = 24.0, temperatureC = 4.0, usableWindowHours = 18.0),
			PlanStage("temper", StageKind.TEMPER, "Temper", hours = 3.0, temperatureC = 20.0),
			PlanStage("bake", StageKind.BAKE, "Bake", hours = 0.25, temperatureC = 22.0, alerts = false),
		),
	)

	val bigaPizza = FermentationPlan(
		id = "biga-pizza",
		name = "Biga",
		family = PlanFamily.PIZZA,
		leaven = LeavenKind.COMMERCIAL_YEAST,
		summary = "Stiff preferment, contemporary Italian style. Big open crumb, strong flavour.",
		prefermentKind = PrefermentKind.BIGA,
		prefermentedFlourPercent = 60.0,
		prefermentHydrationPercent = 50.0,
		stages = listOf(
			PlanStage("biga", StageKind.PREFERMENT, "Biga", "Shaggy, barely mixed. Cool room.", 18.0, 18.0),
			PlanStage("mix", StageKind.MIX, "Mix", "Break the biga down, add remaining water in stages.", 0.4, 22.0, alerts = false),
			PlanStage("bulk", StageKind.BULK, "Rest", hours = 1.0, temperatureC = 22.0),
			PlanStage("divide", StageKind.DIVIDE, "Divide & ball", hours = 0.3, temperatureC = 22.0, alerts = false),
			PlanStage("cold", StageKind.COLD_RETARD, "Cold ferment", hours = 18.0, temperatureC = 4.0, usableWindowHours = 18.0),
			PlanStage("temper", StageKind.TEMPER, "Temper", hours = 4.0, temperatureC = 20.0),
			PlanStage("bake", StageKind.BAKE, "Bake", hours = 0.25, temperatureC = 22.0, alerts = false),
		),
	)

	val detroitPan = FermentationPlan(
		id = "detroit-pan",
		name = "Detroit / pan",
		family = PlanFamily.PIZZA,
		leaven = LeavenKind.COMMERCIAL_YEAST,
		summary = "Press into blued steel, retard in the pan, proof to the rim before saucing.",
		stages = listOf(
			PlanStage("mix", StageKind.MIX, "Mix", hours = 0.25, temperatureC = 22.0, alerts = false),
			PlanStage("bulk", StageKind.BULK, "Bulk ferment", hours = 2.0, temperatureC = 24.0, foldIntervalMinutes = 40),
			PlanStage("pan", StageKind.SHAPE, "Press into pans", "Oil the pan well; don't fight the dough.", 0.3, 22.0, alerts = false),
			PlanStage("cold", StageKind.COLD_RETARD, "Cold ferment", hours = 18.0, temperatureC = 4.0, usableWindowHours = 24.0),
			PlanStage("proof", StageKind.FINAL_PROOF, "Proof in pan", "Until it fills the corners and jiggles.", 3.0, 21.0),
			PlanStage("bake", StageKind.BAKE, "Bake", hours = 0.3, temperatureC = 22.0, alerts = false),
		),
	)

	val focacciaColdBulk = FermentationPlan(
		id = "focaccia-cold-bulk",
		name = "Focaccia — cold bulk",
		family = PlanFamily.BREAD,
		leaven = LeavenKind.COMMERCIAL_YEAST,
		summary = "High hydration, long cold bulk, dimpled straight from the fridge.",
		stages = listOf(
			PlanStage("mix", StageKind.MIX, "Mix", hours = 0.25, temperatureC = 22.0, alerts = false),
			PlanStage("bulk", StageKind.BULK, "Bulk ferment", hours = 1.5, temperatureC = 22.0, foldIntervalMinutes = 30),
			PlanStage("cold", StageKind.COLD_RETARD, "Cold bulk", hours = 20.0, temperatureC = 4.0, usableWindowHours = 28.0),
			PlanStage("pan", StageKind.SHAPE, "Into the pan", hours = 0.2, temperatureC = 22.0, alerts = false),
			PlanStage("proof", StageKind.FINAL_PROOF, "Proof & dimple", hours = 3.0, temperatureC = 22.0),
			PlanStage("bake", StageKind.BAKE, "Bake", hours = 0.4, temperatureC = 22.0, alerts = false),
		),
	)

	val sourdoughPizzaCold = FermentationPlan(
		id = "sourdough-pizza-cold",
		name = "Sourdough pizza — cold ball",
		family = PlanFamily.PIZZA,
		leaven = LeavenKind.SOURDOUGH,
		summary = "Levain built the night before, bulk warm, then a cold ball retard.",
		prefermentKind = PrefermentKind.LEVAIN,
		prefermentedFlourPercent = 10.0,
		prefermentHydrationPercent = 100.0,
		stages = listOf(
			PlanStage("levain", StageKind.PREFERMENT, "Build levain", "Ready when domed and aromatic, before it collapses.", 8.0, 24.0),
			PlanStage("autolyse", StageKind.AUTOLYSE, "Autolyse", "Flour and water only.", 1.0, 24.0, alerts = false),
			PlanStage("mix", StageKind.MIX, "Mix", "Add levain and salt.", 0.3, 24.0, alerts = false),
			PlanStage("bulk", StageKind.BULK, "Bulk ferment", "Coil folds through the first half.", 3.5, 25.0, foldIntervalMinutes = 45),
			PlanStage("divide", StageKind.DIVIDE, "Divide & ball", hours = 0.3, temperatureC = 22.0, alerts = false),
			PlanStage("cold", StageKind.COLD_RETARD, "Cold ferment", hours = 24.0, temperatureC = 4.0, usableWindowHours = 18.0),
			PlanStage("temper", StageKind.TEMPER, "Temper", hours = 3.0, temperatureC = 20.0),
			PlanStage("bake", StageKind.BAKE, "Bake", hours = 0.25, temperatureC = 22.0, alerts = false),
		),
	)

	val sourdoughCountryLoaf = FermentationPlan(
		id = "sourdough-country-loaf",
		name = "Sourdough country loaf",
		family = PlanFamily.BREAD,
		leaven = LeavenKind.SOURDOUGH,
		summary = "Warm bulk with folds, shape, then an overnight cold proof and a Dutch-oven bake.",
		prefermentKind = PrefermentKind.LEVAIN,
		prefermentedFlourPercent = 10.0,
		prefermentHydrationPercent = 100.0,
		stages = listOf(
			PlanStage("levain", StageKind.PREFERMENT, "Build levain", hours = 10.0, temperatureC = 24.0),
			PlanStage("autolyse", StageKind.AUTOLYSE, "Autolyse", hours = 1.0, temperatureC = 24.0, alerts = false),
			PlanStage("mix", StageKind.MIX, "Mix", "Levain in, salt after 30 minutes.", 0.4, 25.0, alerts = false),
			PlanStage("bulk", StageKind.BULK, "Bulk ferment", "Four sets of folds, 30 minutes apart.", 4.5, 25.0, foldIntervalMinutes = 30),
			PlanStage("preshape", StageKind.DIVIDE, "Divide & preshape", hours = 0.2, temperatureC = 22.0, alerts = false),
			PlanStage("bench", StageKind.BENCH, "Bench rest", hours = 0.4, temperatureC = 22.0),
			PlanStage("shape", StageKind.SHAPE, "Final shape", "Into bannetons, seam up.", 0.2, 22.0, alerts = false),
			PlanStage("cold", StageKind.COLD_RETARD, "Cold proof", "Uncovered for a drier skin, if you like the scoring crisp.", 14.0, 4.0, usableWindowHours = 10.0),
			PlanStage("bake", StageKind.BAKE, "Bake", "Lidded 20 min, then open to colour.", 0.75, 22.0, alerts = false),
		),
	)

	val sourdoughRoomPizza = FermentationPlan(
		id = "sourdough-room-pizza",
		name = "Sourdough pizza — room temp",
		family = PlanFamily.PIZZA,
		leaven = LeavenKind.SOURDOUGH,
		summary = "No fridge at all. Long warm bulk and a slow appretto, Neapolitan rhythm on a levain.",
		prefermentKind = PrefermentKind.LEVAIN,
		prefermentedFlourPercent = 8.0,
		prefermentHydrationPercent = 100.0,
		stages = listOf(
			PlanStage("levain", StageKind.PREFERMENT, "Build levain", "Stiff-ish and sweet-smelling, not sharp.", 8.0, 24.0),
			PlanStage("autolyse", StageKind.AUTOLYSE, "Autolyse", hours = 0.75, temperatureC = 22.0, alerts = false),
			PlanStage("mix", StageKind.MIX, "Mix", "Levain first, salt last.", 0.3, 22.0, alerts = false),
			PlanStage("bulk", StageKind.BULK, "Puntata", "Folds in the first hour only.", 4.0, 24.0, foldIntervalMinutes = 45),
			PlanStage("staglio", StageKind.DIVIDE, "Staglio", hours = 0.3, temperatureC = 22.0, alerts = false),
			PlanStage("appretto", StageKind.FINAL_PROOF, "Appretto", "Balls in boxes until slack and domed.", 5.0, 22.0),
			PlanStage("bake", StageKind.BAKE, "Bake", hours = 0.2, temperatureC = 22.0, alerts = false),
		),
	)

	val sourdoughPanCold = FermentationPlan(
		id = "sourdough-pan-cold",
		name = "Sourdough pan — cold",
		family = PlanFamily.PIZZA,
		leaven = LeavenKind.SOURDOUGH,
		summary = "Levain pan dough: warm bulk, into the pan, overnight cold, proof to the rim.",
		prefermentKind = PrefermentKind.LEVAIN,
		prefermentedFlourPercent = 12.0,
		prefermentHydrationPercent = 100.0,
		stages = listOf(
			PlanStage("levain", StageKind.PREFERMENT, "Build levain", hours = 9.0, temperatureC = 24.0),
			PlanStage("mix", StageKind.MIX, "Mix", hours = 0.3, temperatureC = 24.0, alerts = false),
			PlanStage("bulk", StageKind.BULK, "Bulk ferment", "Coil folds while it's still slack.", 3.0, 25.0, foldIntervalMinutes = 40),
			PlanStage("pan", StageKind.SHAPE, "Into the pan", "Oil generously, stretch in two goes.", 0.3, 22.0, alerts = false),
			PlanStage("cold", StageKind.COLD_RETARD, "Cold ferment", hours = 16.0, temperatureC = 4.0, usableWindowHours = 20.0),
			PlanStage("proof", StageKind.FINAL_PROOF, "Proof in pan", hours = 3.5, temperatureC = 21.0),
			PlanStage("bake", StageKind.BAKE, "Bake", hours = 0.3, temperatureC = 22.0, alerts = false),
		),
	)

	val poolishBaguette = FermentationPlan(
		id = "poolish-baguette",
		name = "Baguette — poolish",
		family = PlanFamily.BREAD,
		leaven = LeavenKind.COMMERCIAL_YEAST,
		summary = "Overnight poolish, short bulk, gentle shaping, same-day bake.",
		prefermentKind = PrefermentKind.POOLISH,
		prefermentedFlourPercent = 40.0,
		prefermentHydrationPercent = 100.0,
		stages = listOf(
			PlanStage("poolish", StageKind.PREFERMENT, "Poolish", hours = 14.0, temperatureC = 20.0),
			PlanStage("mix", StageKind.MIX, "Mix", hours = 0.3, temperatureC = 23.0, alerts = false),
			PlanStage("bulk", StageKind.BULK, "Bulk ferment", hours = 2.0, temperatureC = 24.0, foldIntervalMinutes = 45),
			PlanStage("divide", StageKind.DIVIDE, "Divide & preshape", hours = 0.2, temperatureC = 22.0, alerts = false),
			PlanStage("bench", StageKind.BENCH, "Bench rest", hours = 0.4, temperatureC = 22.0),
			PlanStage("shape", StageKind.SHAPE, "Shape", hours = 0.2, temperatureC = 22.0, alerts = false),
			PlanStage("proof", StageKind.FINAL_PROOF, "Final proof", "In couche, until it springs back slowly.", 1.25, 24.0),
			PlanStage("bake", StageKind.BAKE, "Bake", "Steam hard for the first 10 minutes.", 0.4, 22.0, alerts = false),
		),
	)

	val all = listOf(
		sameDayStraight,
		coldBallRetard,
		coldBulkRetard,
		neapolitanDirect,
		poolishPizza,
		bigaPizza,
		detroitPan,
		focacciaColdBulk,
		sourdoughPizzaCold,
		sourdoughRoomPizza,
		sourdoughPanCold,
		sourdoughCountryLoaf,
		poolishBaguette,
	)

	fun plan(id: String): FermentationPlan? = all.firstOrNull { it.id == id }
}
