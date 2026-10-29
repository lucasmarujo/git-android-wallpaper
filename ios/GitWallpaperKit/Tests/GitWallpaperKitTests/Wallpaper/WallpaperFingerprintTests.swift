import GitWallpaperKit
import XCTest

final class WallpaperFingerprintTests: XCTestCase {
    private let calendar = TestCalendars.calendar()
    private let settings = WallpaperSettings()
    private let size = ScreenSize(width: 1179, height: 2556)
    private let languageTag = "en|en_US"
    private let today = TestCalendars.today

    func testSameInputsProduceTheSameSha256Fingerprint() {
        let first = fingerprint()
        let second = fingerprint()

        XCTAssertEqual(first, second)
        XCTAssertNotNil(first.range(of: "^[0-9a-f]{64}$", options: .regularExpression))
    }

    func testANewContributionChangesTheFingerprint() {
        var changed = calendar
        changed.days[changed.days.count - 1].count += 1

        XCTAssertNotEqual(fingerprint(), fingerprint(calendar: changed))
    }

    func testANewDayChangesTheFingerprintEvenWithoutNewContributions() {
        XCTAssertNotEqual(fingerprint(), fingerprint(today: today.plusDays(1)))
    }

    func testANewAvatarChangesTheFingerprint() {
        var withAvatar = calendar
        withAvatar.avatarUrl = "https://avatars.githubusercontent.com/u/1?v=4"

        XCTAssertNotEqual(fingerprint(), fingerprint(calendar: withAvatar))
    }

    func testAppearanceAndTargetSettingsChangeTheFingerprint() {
        let base = fingerprint()
        let changes: [(inout WallpaperSettings) -> Void] = [
            { $0.theme = .ocean },
            { $0.commitColor = ARGBColor(argb: 0xFFFF_0000) },
            { $0.target = .both },
            { $0.showStats = false },
            { $0.period = .last12Months },
            { $0.cellShape = .circle },
            { $0.framing = WallpaperFraming(zoom: 1.3) },
            { $0.framing = WallpaperFraming(offsetX: 0.1) },
            { $0.framing = WallpaperFraming(offsetY: -0.1) },
        ]

        for (index, change) in changes.enumerated() {
            var changed = settings
            change(&changed)
            XCTAssertNotEqual(base, fingerprint(settings: changed), "change \(index)")
        }
    }

    func testSchedulingSettingsDoNotChangeTheFingerprint() {
        var changed = settings
        changed.autoUpdate = false
        changed.updateInterval = .twelveHours

        XCTAssertEqual(fingerprint(), fingerprint(settings: changed))
    }

    func testScreenSizeAndLanguageChangeTheFingerprint() {
        let base = fingerprint()

        XCTAssertNotEqual(base, fingerprint(size: ScreenSize(width: 1320, height: 2868)))
        XCTAssertNotEqual(base, fingerprint(languageTag: "pt-BR|pt_BR"))
    }

    private func fingerprint(
        calendar: ContributionCalendar? = nil,
        settings: WallpaperSettings? = nil,
        size: ScreenSize? = nil,
        languageTag: String? = nil,
        today: LocalDate? = nil
    ) -> String {
        WallpaperFingerprint.of(
            calendar: calendar ?? self.calendar,
            settings: settings ?? self.settings,
            size: size ?? self.size,
            languageTag: languageTag ?? self.languageTag,
            today: today ?? self.today
        )
    }
}
