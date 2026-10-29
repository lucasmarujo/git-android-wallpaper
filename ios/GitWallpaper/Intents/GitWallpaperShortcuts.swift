import AppIntents

struct GitWallpaperShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: GetWallpaperIntent(),
            phrases: [
                "Get \(.applicationName)",
                "Update \(.applicationName)",
            ],
            shortTitle: "Get Git Wallpaper",
            systemImageName: "square.grid.3x3.fill"
        )
    }
}
