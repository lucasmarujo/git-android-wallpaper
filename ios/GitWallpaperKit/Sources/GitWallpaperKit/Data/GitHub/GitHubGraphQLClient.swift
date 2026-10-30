import Foundation

public final class GitHubGraphQLClient: ContributionsSource {
    public static let defaultEndpoint = URL(string: "https://api.github.com/graphql")!

    private let session: URLSession
    private let endpoint: URL

    public init(session: URLSession, endpoint: URL = GitHubGraphQLClient.defaultEndpoint) {
        self.session = session
        self.endpoint = endpoint
    }

    public func fetchCalendar(token: String) async throws -> ContributionCalendar {
        var request = URLRequest(url: endpoint)
        request.httpMethod = GitHubHTTP.postMethod
        request.setValue("Bearer \(token)", forHTTPHeaderField: GitHubHTTP.authorizationHeader)
        request.setValue(GitHubHTTP.jsonUTF8ContentType, forHTTPHeaderField: GitHubHTTP.contentTypeHeader)
        request.httpBody = try JSONEncoder().encode(GraphQLRequest(query: Self.contributionsQuery))
        let (data, response) = try await GitHubHTTP.send(request, using: session)
        if response.statusCode == GitHubHTTP.unauthorizedStatus { throw GitHubError.unauthorized }
        guard GitHubHTTP.successStatuses.contains(response.statusCode) else { throw GitHubError.http(response.statusCode) }
        let decoded: GraphQLResponse<ViewerData>
        do {
            decoded = try JSONDecoder().decode(GraphQLResponse<ViewerData>.self, from: data)
        } catch {
            throw GitHubError.api(Self.unreadableResponseMessage)
        }
        if let error = decoded.errors?.first { throw GitHubError.api(error.message) }
        guard let viewer = decoded.data?.viewer else { throw GitHubError.api(Self.missingViewerMessage) }
        return viewer.toCalendar()
    }

    private static let missingViewerMessage = "GitHub returned no viewer data"
    private static let unreadableResponseMessage = "GitHub returned an unreadable response"
    private static let contributionsQuery = """
        query {
          viewer {
            login
            avatarUrl(size: 160)
            contributionsCollection {
              contributionCalendar {
                totalContributions
                weeks {
                  contributionDays {
                    date
                    contributionCount
                    contributionLevel
                  }
                }
              }
            }
          }
        }
        """
}
