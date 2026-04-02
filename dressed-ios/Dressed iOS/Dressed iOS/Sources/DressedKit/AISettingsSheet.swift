import SwiftUI

/// Settings sheet for connecting the user's Anthropic API key for AI-powered outfit reasoning.
struct AISettingsSheet: View {
    @Environment(\.dismiss) private var dismiss

    @State private var reasoningEnabled = AIReasoningPreferences.isReasoningEnabled
    @State private var keyDraft = ""
    @State private var hasKeyInKeychain = false
    @State private var showKey = false
    @State private var toastMessage: String?
    @State private var isTestingConnection = false

    private let navPurple = Color(red: 0.42, green: 0.29, blue: 0.68)

    private var trimmedDraft: String {
        keyDraft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var keyForTesting: String? {
        if !trimmedDraft.isEmpty { return trimmedDraft }
        return APIKeyStore.getKey()
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                headerSection

                billingWarningSection

                Toggle("Use AI explanations for suggestions", isOn: $reasoningEnabled)
                    .tint(navPurple)
                    .onChange(of: reasoningEnabled) { _, new in
                        AIReasoningPreferences.isReasoningEnabled = new
                    }

                keyStatusSection

                keyInputSection

                VStack(spacing: 12) {
                    Button {
                        saveKey()
                    } label: {
                        Text("Save Key")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(navPurple)
                    .disabled(trimmedDraft.isEmpty)

                    Button {
                        testConnection()
                    } label: {
                        HStack {
                            if isTestingConnection {
                                ProgressView()
                                    .scaleEffect(0.9)
                            }
                            Text("Test Connection")
                                .frame(maxWidth: .infinity)
                        }
                    }
                    .buttonStyle(.bordered)
                    .tint(navPurple)
                    .disabled(isTestingConnection || keyForTesting == nil)

                    if hasKeyInKeychain {
                        Button(role: .destructive) {
                            removeKey()
                        } label: {
                            Text("Remove Key from This Device")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                    }
                }

                howToSection
            }
            .padding(20)
        }
        .onAppear {
            hasKeyInKeychain = APIKeyStore.hasKey
            reasoningEnabled = AIReasoningPreferences.isReasoningEnabled
        }
        .navigationTitle("AI Settings")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Done") { dismiss() }
            }
        }
        .toolbarBackground(navPurple, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .overlay(alignment: .bottom) {
            if let message = toastMessage {
                Text(message)
                    .font(.subheadline.weight(.medium))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(.thinMaterial, in: Capsule())
                    .padding(.horizontal, 16)
                    .padding(.bottom, 28)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.6) {
                            withAnimation { toastMessage = nil }
                        }
                    }
            }
        }
    }

    private var headerSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 10) {
                Image(systemName: "brain")
                    .font(.title2)
                    .foregroundStyle(navPurple)
                Text("AI Outfit Reasoning")
                    .font(.title3.weight(.semibold))
            }
            Text("Optional: Claude can add short, friendly explanations to each suggested outfit. The app still picks outfits using on-device rules; the API is only used for text if you enable this and add your own key.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
    }

    private var billingWarningSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label {
                Text("Billing & usage")
                    .font(.subheadline.weight(.semibold))
            } icon: {
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundStyle(.orange)
            }
            Text("Every API call uses your Anthropic quota and may incur charges on your account. You are responsible for monitoring usage, rate limits, and billing at console.anthropic.com. Turn off AI above or remove your key anytime.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Color.orange.opacity(0.12))
        )
    }

    private var keyStatusSection: some View {
        HStack(spacing: 10) {
            Image(systemName: hasKeyInKeychain ? "key.fill" : "key.slash")
                .foregroundStyle(hasKeyInKeychain ? navPurple : .secondary)
                .font(.title3)
            VStack(alignment: .leading, spacing: 2) {
                Text(hasKeyInKeychain ? "Key saved on this device" : "No key saved")
                    .font(.subheadline.weight(.medium))
                Text(hasKeyInKeychain ? "The full key is not shown for your safety." : "Paste a key below, then Save or Test Connection.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Color(.secondarySystemGroupedBackground))
        )
    }

    private var keyInputSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Anthropic API Key")
                .font(.subheadline.weight(.semibold))

            HStack(spacing: 8) {
                Group {
                    if showKey {
                        TextField("sk-ant-api03-…", text: $keyDraft)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                    } else {
                        SecureField("sk-ant-api03-…", text: $keyDraft)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                    }
                }
                .textFieldStyle(.roundedBorder)
                .font(.system(.body, design: .monospaced))

                Button {
                    showKey.toggle()
                } label: {
                    Image(systemName: showKey ? "eye.slash.fill" : "eye.fill")
                        .foregroundStyle(.secondary)
                        .accessibilityLabel(showKey ? "Hide key" : "Show key")
                }
            }

            Text("Keys are stored in the iOS Keychain (this device only). They are sent directly to Anthropic’s API — not to Dressed’s servers.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var howToSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("How to get an API key")
                .font(.subheadline.weight(.semibold))
            VStack(alignment: .leading, spacing: 6) {
                stepRow(number: "1", text: "Visit console.anthropic.com")
                stepRow(number: "2", text: "Create an account or sign in")
                stepRow(number: "3", text: "Go to API Keys and create a new key")
                stepRow(number: "4", text: "Paste it here, then Test Connection")
            }
            Text("Typical cost is small per suggestion, but always check your own usage.")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(.top, 8)
    }

    private func stepRow(number: String, text: String) -> some View {
        HStack(alignment: .top, spacing: 8) {
            Text(number)
                .font(.caption.weight(.bold))
                .frame(width: 20, height: 20)
                .background(navPurple.opacity(0.15), in: Circle())
                .foregroundStyle(navPurple)
            Text(text)
                .font(.subheadline)
        }
    }

    private func saveKey() {
        guard !trimmedDraft.isEmpty else { return }
        APIKeyStore.setKey(trimmedDraft)
        keyDraft = ""
        hasKeyInKeychain = true
        withAnimation { toastMessage = "API key saved securely" }
    }

    private func removeKey() {
        APIKeyStore.setKey(nil)
        keyDraft = ""
        hasKeyInKeychain = false
        withAnimation { toastMessage = "API key removed" }
    }

    private func testConnection() {
        guard let key = keyForTesting, !key.isEmpty else {
            withAnimation { toastMessage = "Save a key or type one to test." }
            return
        }
        isTestingConnection = true
        Task {
            let err = await PickerAnthropicReasoner.validateApiKey(key)
            await MainActor.run {
                isTestingConnection = false
                if let err {
                    withAnimation { toastMessage = err }
                } else {
                    withAnimation { toastMessage = "Connection OK — key works." }
                }
            }
        }
    }
}

#Preview {
    NavigationStack {
        AISettingsSheet()
    }
}
