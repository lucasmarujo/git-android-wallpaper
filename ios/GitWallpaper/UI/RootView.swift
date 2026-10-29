import GitWallpaperKit
import SwiftUI

enum SessionState: Equatable {
    case loading
    case signedOut
    case signedIn(login: String)
}

@MainActor
final class RootViewModel: ObservableObject {
    @Published private(set) var state: SessionState = .loading

    private let sessionRepository: SessionRepository
    private let notificationCenter: NotificationCenter

    init(sessionRepository: SessionRepository, notificationCenter: NotificationCenter = .default) {
        self.sessionRepository = sessionRepository
        self.notificationCenter = notificationCenter
    }

    func observe() async {
        reload()
        for await _ in notificationCenter.notifications(named: SessionRepository.didChangeNotification) {
            reload()
        }
    }

    func reload() {
        let login = sessionRepository.currentSession()?.login
        state = login.map { .signedIn(login: $0) } ?? .signedOut
    }
}

struct RootView: View {
    let container: AppContainer
    @StateObject private var viewModel: RootViewModel
    @Environment(\.scenePhase) private var scenePhase

    init(container: AppContainer) {
        self.container = container
        _viewModel = StateObject(wrappedValue: RootViewModel(sessionRepository: container.core.sessionRepository))
    }

    var body: some View {
        Group {
            switch viewModel.state {
            case .loading:
                Color(.systemBackground).ignoresSafeArea()
            case .signedOut:
                LoginView(viewModel: container.makeLoginViewModel())
            case .signedIn(let login):
                HomeView(viewModel: container.makeHomeViewModel(login: login))
                    .id(login)
            }
        }
        .task { await viewModel.observe() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { viewModel.reload() }
        }
    }
}
