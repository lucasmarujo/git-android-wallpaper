import Foundation
import GitWallpaperKit
import XCTest

final class UpdateStatusRepositoryTests: XCTestCase {
    private let stores = TestStores()
    private lazy var repository = UpdateStatusRepository(defaults: stores.defaults)

    override func tearDown() {
        stores.tearDown()
        super.tearDown()
    }

    func testNothingIsRecordedInitially() {
        XCTAssertEqual(UpdateStatus(), repository.current())
    }

    func testApplyingRecordsTheFingerprintAndBothTimestamps() {
        let date = Date(timeIntervalSince1970: 1_790_000_000)

        repository.markApplied(fingerprint: "abc", at: date)

        XCTAssertEqual(UpdateStatus(fingerprint: "abc", lastAppliedAt: date, lastCheckedAt: date), repository.current())
    }

    func testCheckingOnlyMovesTheCheckTimestamp() {
        let applied = Date(timeIntervalSince1970: 1_790_000_000)
        let checked = applied.addingTimeInterval(3600)
        repository.markApplied(fingerprint: "abc", at: applied)

        repository.markChecked(at: checked)

        XCTAssertEqual(UpdateStatus(fingerprint: "abc", lastAppliedAt: applied, lastCheckedAt: checked), repository.current())
    }

    func testClearForgetsEverything() {
        repository.markApplied(fingerprint: "abc", at: Date())

        repository.clear()

        XCTAssertEqual(UpdateStatus(), repository.current())
    }
}
