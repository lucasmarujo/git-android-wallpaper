import SwiftUI

struct MessageBanner: View {
    let text: String

    var body: some View {
        Text(verbatim: text)
            .font(.subheadline)
            .foregroundStyle(Color(.systemBackground))
            .multilineTextAlignment(.center)
            .padding(.horizontal, 18)
            .padding(.vertical, 12)
            .background(Capsule().fill(Color(.label)))
            .padding(.horizontal, 20)
            .accessibilityAddTraits(.updatesFrequently)
    }
}
