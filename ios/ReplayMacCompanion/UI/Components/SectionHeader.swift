import SwiftUI

public struct SectionHeader<Trailing: View>: View {
    public var title: String
    public let trailing: () -> Trailing

    public init(
        title: String,
        @ViewBuilder trailing: @escaping () -> Trailing = { EmptyView() }
    ) {
        self.title = title
        self.trailing = trailing
    }

    public var body: some View {
        HStack(spacing: 8) {
            Text(title)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(AppColors.textPrimary)

            Spacer()

            trailing()
        }
        .padding(.horizontal, 4)
    }
}

public struct RowSeparator: View {
    public var startIndent: CGFloat

    public init(startIndent: CGFloat = 16) {
        self.startIndent = startIndent
    }

    public var body: some View {
        Divider()
            .overlay(AppColors.separator)
            .padding(.leading, startIndent)
    }
}
