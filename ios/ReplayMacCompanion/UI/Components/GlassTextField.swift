import SwiftUI

public struct GlassTextField: View {
    @Binding public var value: String
    public var label: String
    public var placeholder: String
    public var keyboardType: UIKeyboardType
    public var onSubmit: (() -> Void)?

    @FocusState private var isFocused: Bool

    public init(
        value: Binding<String>,
        label: String,
        placeholder: String = "",
        keyboardType: UIKeyboardType = .default,
        onSubmit: (() -> Void)? = nil
    ) {
        self._value = value
        self.label = label
        self.placeholder = placeholder
        self.keyboardType = keyboardType
        self.onSubmit = onSubmit
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(AppColors.textSecondary)
                .padding(.leading, 4)

            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: AppRadii.small, style: .continuous)
                    .fill(AppColors.backgroundPrimary.opacity(0.55))

                RoundedRectangle(cornerRadius: AppRadii.small, style: .continuous)
                    .strokeBorder(isFocused ? AppColors.accent : AppColors.glassBorder, lineWidth: 1)

                if value.isEmpty && !placeholder.isEmpty {
                    Text(placeholder)
                        .font(.system(size: 16))
                        .foregroundColor(AppColors.textTertiary)
                        .padding(.horizontal, 14)
                }

                TextField("", text: $value)
                    .font(.system(size: 16))
                    .foregroundColor(AppColors.textPrimary)
                    .keyboardType(keyboardType)
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.never)
                    .focused($isFocused)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 13)
                    .onSubmit {
                        onSubmit?()
                    }
            }
            .frame(height: 48)
        }
    }
}
