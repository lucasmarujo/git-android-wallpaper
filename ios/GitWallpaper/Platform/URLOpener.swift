import UIKit

@MainActor
protocol URLOpening {
    func open(_ url: URL)
}

struct SystemURLOpener: URLOpening {
    func open(_ url: URL) {
        UIApplication.shared.open(url)
    }
}
