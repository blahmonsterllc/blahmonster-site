package com.blahmonster.prooflab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blahmonster.prooflab.AppViewModel
import com.blahmonster.prooflab.core.MixerKind
import com.blahmonster.prooflab.ui.BadgePill
import com.blahmonster.prooflab.ui.BlockButton
import com.blahmonster.prooflab.ui.Hairline
import com.blahmonster.prooflab.ui.LocalPalette
import com.blahmonster.prooflab.ui.NumberField
import com.blahmonster.prooflab.ui.Panel
import com.blahmonster.prooflab.ui.ProofType

@Composable
fun SettingsScreen(model: AppViewModel, onRequestNotifications: () -> Unit) {
	val palette = LocalPalette.current
	val allowed by model.notificationsAllowed.collectAsStateWithLifecycle()

	// Settings live in SharedPreferences; mirror them so the switches move when tapped.
	var fahrenheit by remember { mutableStateOf(model.useFahrenheit) }
	var capacity by remember { mutableStateOf(model.mixerCapacityKg) }
	var mixer by remember { mutableStateOf(model.mixer) }

	LazyColumn(
		contentPadding = PaddingValues(16.dp),
		verticalArrangement = Arrangement.spacedBy(14.dp),
	) {
		item {
			Panel(title = "Alerts") {
				if (allowed) {
					Row(verticalAlignment = Alignment.CenterVertically) {
						Text(
							"Notifications are on",
							style = ProofType.body,
							color = palette.ink,
							modifier = Modifier.weight(1f),
						)
						BadgePill("On", palette.go)
					}
					Text(
						"Stage alerts are set as exact alarms ahead of time, so they arrive with the app " +
							"closed — including overnight in the fridge. Android has no numeric app badge, " +
							"so ProofLab keeps a grouped notification with the count of stages you haven't " +
							"ticked off; launchers that show a number read it from there.",
						style = ProofType.small,
						color = palette.inkMute,
					)
					BlockButton("Send a test alert", palette.ink, filled = false) {
						model.sendTestAlert()
					}
				} else {
					Text(
						"ProofLab can't alert you yet. Without permission the app can still keep the " +
							"schedule, but you'll have to check it yourself.",
						style = ProofType.body,
						color = palette.inkSoft,
					)
					BlockButton("Turn alerts on", palette.hot, onClick = onRequestNotifications)
				}
			}
		}

		item {
			Panel(title = "Defaults") {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Text(
						"Show temperatures in °F",
						style = ProofType.body,
						color = palette.ink,
						modifier = Modifier.weight(1f),
					)
					Switch(
						checked = fahrenheit,
						onCheckedChange = {
							fahrenheit = it
							model.useFahrenheit = it
						},
					)
				}
				Hairline()
				NumberField(
					"Mixer capacity",
					capacity,
					{
						capacity = it
						model.mixerCapacityKg = it
					},
					suffix = "kg",
					range = 0.5..500.0,
					decimals = 1,
				)
				Hairline()
				Dropdown(
					label = mixer.displayName,
					options = MixerKind.entries.map { it.name to it.displayName },
				) {
					mixer = MixerKind.valueOf(it)
					model.mixer = mixer
				}
			}
		}

		item {
			Panel(title = "How the numbers work") {
				Text(
					"Every stage's real time is converted into equivalent hours at 24 °C, so a 48-hour " +
						"fridge retard and a 7-hour bench bulk can be compared directly. The yeast and " +
						"levain suggestions come from that total.",
					style = ProofType.small,
					color = palette.inkSoft,
				)
				Hairline()
				Text(
					"The suggestion is a starting point, not a verdict — real doses vary more than " +
						"fivefold between traditions. What transfers is the ratio: double the cold time, " +
						"halve the leaven.",
					style = ProofType.small,
					color = palette.inkSoft,
				)
			}
		}

		item {
			Panel(title = "ProofLab") {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Text(
						"Blah Monster LLC",
						style = ProofType.small,
						color = palette.inkMute,
						modifier = Modifier.weight(1f),
					)
					Text("blahmonster.com", style = ProofType.small, color = palette.inkMute)
				}
			}
		}
	}
}
