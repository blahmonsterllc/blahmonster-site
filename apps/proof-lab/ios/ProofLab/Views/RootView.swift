import DoughKit
import SwiftUI

struct RootView: View {
	@Environment(AppModel.self) private var model
	@State private var selection = Tab.proofing
	@State private var showingNewBatch = false

	enum Tab: Hashable {
		case proofing
		case styles
		case log
		case settings
	}

	var body: some View {
		TabView(selection: $selection) {
			NavigationStack {
				ProofingView(showingNewBatch: $showingNewBatch)
			}
			.tabItem { Label("Proofing", systemImage: "timer") }
			.badge(dueCount)
			.tag(Tab.proofing)

			NavigationStack { StyleLibraryView() }
				.tabItem { Label("Styles", systemImage: "books.vertical") }
				.tag(Tab.styles)

			NavigationStack { LogView() }
				.tabItem { Label("Log", systemImage: "chart.bar.doc.horizontal") }
				.tag(Tab.log)

			NavigationStack { SettingsView() }
				.tabItem { Label("Settings", systemImage: "gearshape") }
				.tag(Tab.settings)
		}
		.sheet(isPresented: $showingNewBatch) {
			NavigationStack { NewBatchView() }
		}
	}

	private var dueCount: Int {
		AlertScheduler.currentBadge(batches: model.batches)
	}
}
