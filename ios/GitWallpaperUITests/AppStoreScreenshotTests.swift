import CoreGraphics
import GitWallpaperKit
import XCTest

final class AppStoreScreenshotTests: XCTestCase {
    override func setUp() {
        continueAfterFailure = false
    }

    func testEnglishScreenshots() throws {
        try captureScreens(language: "en", locale: "en_US")
        try captureRenders(language: "en")
    }

    func testPortugueseScreenshots() throws {
        try captureScreens(language: "pt-BR", locale: "pt_BR")
        try captureRenders(language: "pt-BR")
    }

    private func captureScreens(language: String, locale: String) throws {
        let app = XCUIApplication()
        app.launchArguments = [Self.screenshotArgument, "-AppleLanguages", "(\(language))", "-AppleLocale", locale]
        app.launch()

        let applyButton = app.buttons[Self.applyButton]
        XCTAssertTrue(applyButton.waitForExistence(timeout: Self.timeout))
        expectation(for: NSPredicate(format: "isEnabled == true"), evaluatedWith: applyButton)
        waitForExpectations(timeout: Self.timeout)
        settle()
        attach(app.screenshot(), name: "\(language)-1-home")

        let customColor = app.buttons[Self.customColorButton]
        XCTAssertTrue(customColor.waitForExistence(timeout: Self.timeout))
        scrollToSettings(app, anchor: customColor)
        attach(app.screenshot(), name: "\(language)-2-settings")

        customColor.tap()
        let redSlider = app.sliders.element(boundBy: 0)
        XCTAssertTrue(redSlider.waitForExistence(timeout: Self.timeout))
        redSlider.adjust(toNormalizedSliderPosition: Self.purpleRed)
        app.sliders.element(boundBy: 1).adjust(toNormalizedSliderPosition: Self.purpleGreen)
        app.sliders.element(boundBy: 2).adjust(toNormalizedSliderPosition: Self.purpleBlue)
        settle()
        attach(app.screenshot(), name: "\(language)-3-commit-color")
        app.buttons[Self.commitColorCancelButton].tap()
        settle()

        applyButton.tap()
        let adjustApply = app.buttons[Self.adjustApplyButton]
        XCTAssertTrue(adjustApply.waitForExistence(timeout: Self.timeout))
        settle(seconds: Self.renderSeconds)
        attach(app.screenshot(), name: "\(language)-4-framing")
    }

    private func captureRenders(language: String) throws {
        let strings = try XCTUnwrap(WallpaperStrings(localization: language))
        let renderer = ContributionWallpaperRenderer(strings: strings)
        let today = WallClock.system.today()
        let calendar = ContributionCalendar.sample(login: Self.login, endingAt: today)
        let settings = WallpaperSettings()
        let palette = WallpaperPalette.of(settings.theme)

        let wallpaper = try renderer.render(calendar: calendar, settings: settings, size: Self.screenSize, today: today, avatar: nil)
        attach(wallpaper, name: "\(language)-render-wallpaper")

        for (name, points) in Self.widgetSizes {
            let size = ScreenSize(width: Int(points.width * Self.widgetScale), height: Int(points.height * Self.widgetScale))
            let image = try renderer.renderWidget(
                calendar: calendar,
                settings: settings,
                palette: name == Self.lockScreenWidget ? .monochrome : palette,
                size: size,
                density: Self.widgetScale,
                today: today
            )
            attach(image, name: "\(language)-render-widget-\(name)")
        }
    }

    private func scrollToSettings(_ app: XCUIApplication, anchor: XCUIElement) {
        let scrollView = app.scrollViews.firstMatch
        var attempts = 0
        while anchor.frame.minY > app.frame.height * Self.settingsAnchorRatio, attempts < Self.maxScrollAttempts {
            scrollView.swipeUp()
            settle()
            attempts += 1
        }
        XCTAssertLessThanOrEqual(anchor.frame.minY, app.frame.height * Self.settingsAnchorRatio, "The settings did not scroll into view")
    }

    private func attach(_ screenshot: XCUIScreenshot, name: String) {
        let attachment = XCTAttachment(screenshot: screenshot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func attach(_ image: CGImage, name: String) {
        guard let data = image.pngData() else { return XCTFail("Could not encode \(name)") }
        let attachment = XCTAttachment(data: data, uniformTypeIdentifier: "public.png")
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func settle(seconds: TimeInterval = AppStoreScreenshotTests.settleSeconds) {
        Thread.sleep(forTimeInterval: seconds)
    }

    private static let screenshotArgument = "-GitWallpaperScreenshots"
    private static let applyButton = "home_apply_button"
    private static let customColorButton = "home_custom_commit_color"
    private static let adjustApplyButton = "adjust_apply_button"
    private static let commitColorCancelButton = "commit_color_cancel"
    private static let login = "lucasmarujo"
    private static let timeout: TimeInterval = 30
    private static let settleSeconds: TimeInterval = 1.5
    private static let renderSeconds: TimeInterval = 3
    private static let settingsAnchorRatio: CGFloat = 0.7
    private static let maxScrollAttempts = 4
    private static let purpleRed: CGFloat = 0.54
    private static let purpleGreen: CGFloat = 0.17
    private static let purpleBlue: CGFloat = 0.89
    private static let screenSize = ScreenSize(width: 1320, height: 2868)
    private static let widgetScale: CGFloat = 3
    private static let lockScreenWidget = "lock"
    private static let widgetSizes: [(String, CGSize)] = [
        ("small", CGSize(width: 170, height: 170)),
        ("medium", CGSize(width: 364, height: 170)),
        ("large", CGSize(width: 364, height: 382)),
        ("lock", CGSize(width: 172, height: 76)),
    ]
}
