import SwiftUI

/// Settings sheet for BYOK picker reasoning (Anthropic, OpenAI, Grok placeholder).
struct AISettingsSheet: View {
    @Environment(\.dismiss) private var dismiss

    @State private var reasoningEnabled = AIReasoningPreferences.isReasoningEnabled
    @State private var selectedProvider = AIReasoningPreferences.selectedProvider
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
        if selectedProvider == .grok { return nil }
        if !trimmedDraft.isEmpty { return trimmedDraft }
        return APIKeyStore.getKey(for: selectedProvider)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                headerSection

                billingWarningSection

                providerSection

                Toggle("Use AI explanations for suggestions", isOn: $reasoningEnabled)
                    .tint(navPurple)
                    .onChange(of: reasoningEnabled) { _, new in
                        AIReasoningPreferences.isReasoningEnabled = new
                    }

                keyStatusSection

                if selectedProvider == .grok {
                    grokPlaceholderSection
                } else {
                    keyInputSection
                }

                VStack(spacing: 12) {
                    if selectedProvider != .grok {
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
                }

                howToSection
            }
            .padding(20)
        }
        .onAppear {
            selectedProvider = AIReasoningPreferences.selectedProvider
            refreshKeyStatus()
            reasoningEnabled = AIReasoningPreferences.isReasoningEnabled
        }
        .onChange(of: selectedProvider) { _, new in
            AIReasoningPreferences.selectedProvider = new
            keyDraft = ""
            refreshKeyStatus()
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
            Text("Optional: add your own API key so a model can write short, friendly explanations for each suggested outfit. The app still picks outfits on-device; the network is only used for text when you enable this and choose a supported provider.")
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
            Text("Every API call uses your own developer account with the selected provider and may incur charges. Monitor usage, rate limits, and billing in that provider’s console. Turn off AI above or remove your key anytime.")
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

    private var providerSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Provider")
                .font(.subheadline.weight(.semibold))
            Picker("Provider", selection: $selectedProvider) {
                ForEach(PickerAIProvider.allCases) { provider in
                    Text(provider.displayName).tag(provider)
                }
            }
            .pickerStyle(.segmented)
            Text(providerFootnote)
                .font(.caption)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var providerFootnote: String {
        switch selectedProvider {
        case .anthropic:
            return "Uses Claude via Anthropic’s Messages API."
        case .openai:
            return "Uses OpenAI Chat Completions (JSON output)."
        case .grok:
            return "Grok / xAI is not wired up yet — picker keeps rule-based hints."
        }
    }

    private var keyStatusSection: some View {
        HStack(spacing: 10) {
            Image(systemName: keyStatusIcon)
                .foregroundStyle(keyStatusIconColor)
                .font(.title3)
            VStack(alignment: .leading, spacing: 2) {
                Text(keyStatusTitle)
                    .font(.subheadline.weight(.medium))
                Text(keyStatusDetail)
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

    private var keyStatusIcon: String {
        if selectedProvider == .grok { return "key.slash" }
        return hasKeyInKeychain ? "key.fill" : "key.slash"
    }

    private var keyStatusIconColor: Color {
        if selectedProvider == .grok { return .secondary }
        return hasKeyInKeychain ? navPurple : .secondary
    }

    private var keyStatusTitle: String {
        if selectedProvider == .grok { return "No cloud key needed" }
        return hasKeyInKeychain ? "Key saved on this device" : "No key saved"
    }

    private var keyStatusDetail: String {
        if selectedProvider == .grok {
            return "Switch to Anthropic or OpenAI to store a BYOK key."
        }
        return hasKeyInKeychain
            ? "The full key is not shown for your safety."
            : "Paste a key below, then Save or Test Connection."
    }

    private var grokPlaceholderSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Grok / xAI")
                .font(.subheadline.weight(.semibold))
            Text("Support for Grok is planned. Pick Anthropic or OpenAI to enable cloud-written explanations; otherwise the picker keeps its built-in rule-based hints.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
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
            Text(keyFieldTitle)
                .font(.subheadline.weight(.semibold))

            HStack(spacing: 8) {
                Group {
                    if showKey {
                        TextField(keyPlaceholder, text: $keyDraft)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                    } else {
                        SecureField(keyPlaceholder, text: $keyDraft)
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

            Text("Keys are stored in the iOS Keychain (this device only). They are sent only to \(selectedProvider.displayName)’s API — not to Dressed’s servers.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var keyFieldTitle: String {
        switch selectedProvider {
        case .anthropic: return "Anthropic API Key"
        case .openai: return "OpenAI API Key"
        case .grok: return "API Key"
        }
    }

    private var keyPlaceholder: String {
        switch selectedProvider {
        case .anthropic: return "sk-ant-api03-…"
        case .openai: return "sk-…"
        case .grok: return "…"
        }
    }

    private var howToSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("How to get an API key")
                .font(.subheadline.weight(.semibold))
            Group {
                switch selectedProvider {
                case .anthropic:
                    VStack(alignment: .leading, spacing: 6) {
                        stepRow(number: "1", text: "Visit console.anthropic.com")
                        stepRow(number: "2", text: "Create an account or sign in")
                        stepRow(number: "3", text: "API Keys → create a key")
                        stepRow(number: "4", text: "Paste it here, then Test Connection")
                    }
                case .openai:
                    VStack(alignment: .leading, spacing: 6) {
                        stepRow(number: "1", text: "Visit platform.openai.com")
                        stepRow(number: "2", text: "Sign in and open API keys")
                        stepRow(number: "3", text: "Create a secret key")
                        stepRow(number: "4", text: "Paste it here, then Test Connection")
                    }
                case .grok:
                    Text("When Grok support ships, we’ll link xAI’s console here.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            if selectedProvider != .grok {
                Text("Typical cost is small per suggestion, but always check your own usage.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
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

    private func refreshKeyStatus() {
        hasKeyInKeychain = APIKeyStore.hasKey(for: selectedProvider)
    }

    private func saveKey() {
        guard selectedProvider != .grok, !trimmedDraft.isEmpty else { return }
        APIKeyStore.setKey(trimmedDraft, for: selectedProvider)
        keyDraft = ""
        refreshKeyStatus()
        withAnimation { toastMessage = "API key saved securely" }
    }

    private func removeKey() {
        guard selectedProvider != .grok else { return }
        APIKeyStore.setKey(nil, for: selectedProvider)
        keyDraft = ""
        refreshKeyStatus()
        withAnimation { toastMessage = "API key removed" }
    }

    private func testConnection() {
        guard selectedProvider != .grok else { return }
        guard let key = keyForTesting, !key.isEmpty else {
            withAnimation { toastMessage = "Save a key or type one to test." }
            return
        }
        isTestingConnection = true
        Task {
            let err = await PickerAIReasoner.validateApiKey(selectedProvider, key)
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
