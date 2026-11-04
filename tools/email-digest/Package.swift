// swift-tools-version: 6.2
import PackageDescription

let package = Package(
    name: "EmailDigest",
    platforms: [.macOS(.v26)],
    targets: [
        .executableTarget(
            name: "email-digest",
            path: "Sources/EmailDigest",
            swiftSettings: [.swiftLanguageMode(.v5)]
        )
    ]
)
