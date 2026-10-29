import GitWallpaperKit
import XCTest

final class GitHubGraphQLClientTests: XCTestCase {
    private let server = StubServer()
    private lazy var client = GitHubGraphQLClient(session: server.makeSession(), endpoint: server.url("/graphql"))

    func testFetchesTheViewerCalendarWithABearerToken() async throws {
        server.enqueue(.json(Self.calendarResponse))

        let calendar = try await client.fetchCalendar(token: "secret-token")

        let request = try XCTUnwrap(server.requests.first)
        XCTAssertEqual("POST", request.method)
        XCTAssertEqual("/graphql", request.url.path)
        XCTAssertEqual("Bearer secret-token", request.headers["Authorization"])
        XCTAssertTrue(request.body.contains("contributionCalendar"))
        XCTAssertTrue(request.body.contains("avatarUrl"))
        XCTAssertEqual("octocat", calendar.login)
        XCTAssertEqual(7, calendar.totalContributions)
        XCTAssertEqual(3, calendar.days.count)
        XCTAssertEqual("https://avatars.githubusercontent.com/u/583231?s=160&v=4", calendar.avatarUrl)
    }

    func testDaysAreFlattenedAcrossWeeksAndSortedByDate() async throws {
        server.enqueue(.json(Self.calendarResponse))

        let calendar = try await client.fetchCalendar(token: "token")

        XCTAssertEqual(
            [LocalDate(year: 2026, month: 10, day: 17), LocalDate(year: 2026, month: 10, day: 18), LocalDate(year: 2026, month: 10, day: 19)],
            calendar.days.map(\.date)
        )
        XCTAssertEqual([5, 0, 2], calendar.days.map(\.count))
        XCTAssertEqual(.fourthQuartile, calendar.days.first?.level)
    }

    func testUnknownContributionLevelsFallBackToNone() async throws {
        server.enqueue(.json(Self.calendarResponse))

        let calendar = try await client.fetchCalendar(token: "token")

        XCTAssertEqual(.empty, calendar.days.last?.level)
    }

    func testHttp401IsReportedAsUnauthorized() async {
        server.enqueue(.json(#"{"message":"Bad credentials"}"#, statusCode: 401))

        await XCTAssertThrowsErrorAsync(try await client.fetchCalendar(token: "expired")) { error in
            XCTAssertEqual(.unauthorized, error as? GitHubError)
        }
    }

    func testServerErrorsAreReportedWithTheirStatusCode() async {
        server.enqueue(.json("", statusCode: 502))

        await XCTAssertThrowsErrorAsync(try await client.fetchCalendar(token: "token")) { error in
            XCTAssertEqual(.http(502), error as? GitHubError)
        }
    }

    func testGraphQLErrorsAreReportedAsApiErrors() async {
        server.enqueue(.json(#"{"data":null,"errors":[{"message":"Something broke"}]}"#))

        await XCTAssertThrowsErrorAsync(try await client.fetchCalendar(token: "token")) { error in
            XCTAssertEqual(.api("Something broke"), error as? GitHubError)
        }
    }

    func testUnreadableResponsesAreReportedAsApiErrors() async {
        server.enqueue(.json("<html>"))

        await XCTAssertThrowsErrorAsync(try await client.fetchCalendar(token: "token")) { error in
            guard case .api = error as? GitHubError else { return XCTFail("Unexpected \(error)") }
        }
    }

    func testDroppedConnectionsAreReportedAsNetworkErrors() async {
        server.enqueue(.networkFailure())

        await XCTAssertThrowsErrorAsync(try await client.fetchCalendar(token: "token")) { error in
            guard case .network = error as? GitHubError else { return XCTFail("Unexpected \(error)") }
        }
    }

    private static let calendarResponse = """
        {
          "data": {
            "viewer": {
              "login": "octocat",
              "avatarUrl": "https://avatars.githubusercontent.com/u/583231?s=160&v=4",
              "contributionsCollection": {
                "contributionCalendar": {
                  "totalContributions": 7,
                  "weeks": [
                    {
                      "contributionDays": [
                        {"date": "2026-10-18", "contributionCount": 0, "contributionLevel": "NONE"},
                        {"date": "2026-10-19", "contributionCount": 2, "contributionLevel": "BRAND_NEW_LEVEL"}
                      ]
                    },
                    {
                      "contributionDays": [
                        {"date": "2026-10-17", "contributionCount": 5, "contributionLevel": "FOURTH_QUARTILE", "color": "#216e39"}
                      ]
                    }
                  ]
                }
              }
            }
          }
        }
        """
}
