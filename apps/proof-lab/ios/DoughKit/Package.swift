// swift-tools-version: 6.0
import PackageDescription

let package = Package(
	name: "DoughKit",
	platforms: [.iOS(.v17), .macOS(.v14), .watchOS(.v10)],
	products: [
		.library(name: "DoughKit", targets: ["DoughKit"])
	],
	targets: [
		.target(name: "DoughKit"),
		.testTarget(
			name: "DoughKitTests",
			dependencies: ["DoughKit"],
			resources: [.copy("conformance.json")]
		)
	]
)
