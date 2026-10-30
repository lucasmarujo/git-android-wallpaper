import GitWallpaperKit
import WidgetKit

struct WidgetCenterWidgetUpdater: WidgetUpdater {
    func updateAll() async {
        WidgetCenter.shared.reloadAllTimelines()
    }
}
