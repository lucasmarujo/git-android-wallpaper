import CoreGraphics
import Foundation
import ImageIO
import os

public protocol AvatarSource: Sendable {
    func load(url: String?) async -> CGImage?
}

public actor CachedAvatarRepository: AvatarSource {
    private let session: URLSession
    private let cacheDirectory: URL
    private let isAllowedURL: @Sendable (URL) -> Bool
    private var memoryCache: (key: String, image: CGImage)?

    public init(
        session: URLSession,
        cacheDirectory: URL,
        isAllowedURL: @escaping @Sendable (URL) -> Bool = { isGitHubAvatarURL($0) }
    ) {
        self.session = session
        self.cacheDirectory = cacheDirectory
        self.isAllowedURL = isAllowedURL
    }

    public func load(url: String?) async -> CGImage? {
        guard let url = url.flatMap(URL.init(string:)), isAllowedURL(url) else { return nil }
        let key = url.absoluteString
        if let memoryCache, memoryCache.key == key { return memoryCache.image }
        let image: CGImage?
        if let cached = readCache(key: key) {
            image = cached
        } else {
            image = await download(url)
        }
        if let image { memoryCache = (key, image) }
        return image
    }

    private var imageFile: URL { cacheDirectory.appendingPathComponent(Self.imageFileName) }

    private var urlFile: URL { cacheDirectory.appendingPathComponent(Self.urlFileName) }

    private func readCache(key: String) -> CGImage? {
        guard let cachedKey = try? String(contentsOf: urlFile, encoding: .utf8), cachedKey == key,
              let data = try? Data(contentsOf: imageFile)
        else { return nil }
        return Self.decode(data)
    }

    private func download(_ url: URL) async -> CGImage? {
        do {
            let (bytes, response) = try await session.bytes(from: url)
            guard let httpResponse = response as? HTTPURLResponse,
                  GitHubHTTP.successStatuses.contains(httpResponse.statusCode),
                  httpResponse.expectedContentLength <= Self.maxAvatarBytes
            else { return nil }
            var data = Data()
            for try await byte in bytes {
                data.append(byte)
                if data.count > Self.maxAvatarBytes { return nil }
            }
            guard let image = Self.decode(data) else { return nil }
            try FileManager.default.createDirectory(at: cacheDirectory, withIntermediateDirectories: true)
            try data.write(to: imageFile, options: .atomic)
            try Data(url.absoluteString.utf8).write(to: urlFile, options: .atomic)
            return image
        } catch {
            Self.logger.warning("Could not download the GitHub avatar: \(error.localizedDescription, privacy: .public)")
            return nil
        }
    }

    private static func decode(_ data: Data) -> CGImage? {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil) else { return nil }
        return CGImageSourceCreateImageAtIndex(source, 0, nil)
    }

    private static let imageFileName = "avatar.img"
    private static let urlFileName = "avatar.url"
    private static let maxAvatarBytes: Int64 = 1_048_576
    private static let logger = Logger(subsystem: "dev.marujo.gitwallpaper", category: "AvatarRepository")
}

private let gitHubAvatarHost = "githubusercontent.com"

public func isGitHubAvatarURL(_ url: URL) -> Bool {
    guard url.scheme == "https", let host = url.host?.lowercased() else { return false }
    return host == gitHubAvatarHost || host.hasSuffix(".\(gitHubAvatarHost)")
}
