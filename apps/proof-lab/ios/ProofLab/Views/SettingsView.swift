import DoughKit
import SwiftUI

struct SettingsView: View {
	@Environment(AppModel.self) private var model

	var body: some View {
		@Bindable var model = model

		ScrollView {
			VStack(spacing: 14) {
				Panel(title: "Alerts") {
					if model.notificationsAuthorized {
						HStack {
							Text("Notifications are on").font(.monoBody)
							Spacer()
							BadgePill(text: "On", tint: Palette.go)
						}
						Text("Stage alerts are scheduled ahead of time, so they arrive with the app closed — including overnight in the fridge. The icon badge counts stages you haven't ticked off.")
							.font(.mono(11))
							.foregroundStyle(Palette.inkMute)
						BlockButton(title: "Send a test alert", tint: Palette.ink, filled: false) {
							Task { await model.sendTestAlert() }
						}
					} else {
						Text("ProofLab can't alert you yet. Without permission the app can still keep the schedule, but you'll have to check it yourself.")
							.font(.mono(12))
							.foregroundStyle(Palette.inkSoft)
						BlockButton(title: "Turn alerts on", tint: Palette.hot) {
							Task { await model.requestNotificationPermission() }
						}
					}
				}

				Panel(title: "Defaults") {
					Toggle(isOn: $model.useFahrenheit) {
						Text("Show temperatures in °F").font(.monoBody)
					}
					Hairline()
					NumberField(
						label: "Mixer capacity",
						suffix: "kg",
						value: $model.defaultMixerCapacityKg,
						range: 0.5...500,
						decimals: 1
					)
					Hairline()
					Picker("Mixer", selection: $model.defaultMixer) {
						ForEach(MixerKind.allCases, id: \.self) { Text($0.displayName).tag($0) }
					}
					.pickerStyle(.menu)
				}

				Panel(title: "How the numbers work") {
					Text("Every stage's real time is converted into equivalent hours at 24 °C, so a 48-hour fridge retard and a 7-hour bench bulk can be compared directly. The yeast and levain suggestions come from that total.")
						.font(.mono(12))
						.foregroundStyle(Palette.inkSoft)
					Hairline()
					Text("The suggestion is a starting point, not a verdict — real doses vary more than fivefold between traditions. What transfers is the ratio: double the cold time, halve the leaven.")
						.font(.mono(12))
						.foregroundStyle(Palette.inkSoft)
				}

				Panel(title: "ProofLab") {
					HStack {
						Text("Blah Monster LLC").font(.mono(12)).foregroundStyle(Palette.inkMute)
						Spacer()
						Text("blahmonster.com").font(.mono(12)).foregroundStyle(Palette.inkMute)
					}
				}
			}
			.padding(16)
		}
		.background(Palette.paper)
		.navigationTitle("Settings")
	}
}
