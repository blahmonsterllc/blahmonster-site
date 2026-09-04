import SwiftUI

@main
struct ProofLabApp: App {
	@State private var model = AppModel()
	@Environment(\.scenePhase) private var scenePhase

	var body: some Scene {
		WindowGroup {
			RootView()
				.environment(model)
				.tint(Palette.hot)
				.task { await model.bootstrap() }
		}
		.onChange(of: scenePhase) { _, phase in
			// Stages elapse while the app is away; the badge is only right after a refresh.
			guard phase == .active else { return }
			Task { await model.refresh() }
		}
	}
}
