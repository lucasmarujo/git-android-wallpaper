// swift-tools-version: 5.10
import PackageDescription

let package = Package(
    name: "GitWallpaperKit",
    defaultLocalization: "en",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "GitWallpaperKit", targets: ["GitWallpaperKit"]),
    ],
    targets: [
        .target(
            name: "GitWallpaperKit",
            resources: [.process("Resources")]
        ),
        .testTarget(
            name: "GitWallpaperKitTests",
            dependencies: ["GitWallpaperKit"]
        ),
    ]
)
