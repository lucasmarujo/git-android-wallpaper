import Foundation

public enum GitHubError: Error, Equatable {
    case unauthorized
    case network(URLError)
    case http(Int)
    case api(String)
}

public protocol ContributionsSource: Sendable {
    func fetchCalendar(token: String) async throws -> ContributionCalendar
}

enum GitHubHTTP {
    static let acceptHeader = "Accept"
    static let authorizationHeader = "Authorization"
    static let contentTypeHeader = "Content-Type"
    static let jsonContentType = "application/json"
    static let jsonUTF8ContentType = "application/json; charset=utf-8"
    static let formContentType = "application/x-www-form-urlencoded"
    static let postMethod = "POST"
    static let unauthorizedStatus = 401
    static let successStatuses = 200..<300

    static func send(_ request: URLRequest, using session: URLSession) async throws -> (Data, HTTPURLResponse) {
        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch let error as URLError {
            throw GitHubError.network(error)
        } catch is CancellationError {
            throw CancellationError()
        } catch {
            throw GitHubError.network(URLError(.unknown))
        }
        guard let httpResponse = response as? HTTPURLResponse else {
            throw GitHubError.network(URLError(.badServerResponse))
        }
        return (data, httpResponse)
    }
}
