import SwiftUI

/// Settings sheet for connecting the user's Anthropic API key for AI-powered outfit reasoning.
struct AISettingsSheet: View {
    @Environment(\.dismiss) private var dismiss

    @State private var apiKey: String = ""
    @State private var isConnected = false
    @State private var showKey = false
    @State private var toastMessage: String?

    private let navPurple = Color(red: 0.42, green: 0.29, blue: 0.68)

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Header
                VStack(alignment: .leading, spacing: 8) {
                    HStack(spacing: 10) {
                        Image(systemName: "brain")
                            .font(.title2)
                            .foregroundStyle(navPurple)
                        Text("AI Outfit Reasoning")
                            .font(.title3.weight(.semibold))
                    }
                    Text("Connect your Anthropic account to get AI-powered explanations for outfit suggestions. The app uses Claude to explain why each outfit works for your occasion, weather, and mood.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }

                // Status
                HStack(spacing: 10) {
                    Image(systemName: isConnected ? "checkmark.circle.fill" : "circle.dashed")
                        .foregroundStyle(isConnected ? .green : .secondary)
                        .font(.title3)
                    Text(isConnected ? "Connected" : "Not connected")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(isConnected ? .primary : .secondary)
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(isConnected ? Color.green.opacity(0.1) : Color(.secondarySystemGroupedBackground))
                )

                // API key input
                VStack(alignment: .leading, spacing: 8) {
                    Text("Anthropic API Key")
                        .font(.subheadline.weight(.semibold))

                    HStack {
                        Group {
                            if showKey {
                                TextField("sk-ant-...", text: $apiKey)
                                    .textInputAutocapitalization(.never)
                                    .autocorrectionDisabled()
                            } else {
                                SecureField("sk-ant-...", text: $apiKey)
                                    .textInputAutocapitalization(.never)
                                    .autocorrectionDisabled()
                            }
                        }
                        .textFieldStyle(.roundedBorder)
                        .font(.system(.body, design: .monospaced))

                        Button {
                            showKey.toggle()
                        } label: {
                            Image(systemName: showKey ? "eye.slash" : "eye")
                                .foregroundStyle(.secondary)
                        }
                    }

                    Text("Your key is stored securely in the device Keychain and never leaves your device except to call the Anthropic API directly.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                // Action buttons
                VStack(spacing: 12) {
                    Button {
                        saveKey()
                    } label: {
                        Text("Save Key")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(navPurple)
                    .disabled(apiKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                    if isConnected {
                        Button(role: .destructive) {
                            removeKey()
                        } label: {
                            Text("Disconnect")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                    }
                }

                // How to get a key
                VStack(alignment: .leading, spacing: 8) {
                    Text("How to get an API key")
                        .font(.subheadline.weight(.semibold))
                    VStack(alignment: .leading, spacing: 6) {
                        stepRow(number: "1", text: "Visit console.anthropic.com")
                        stepRow(number: "2", text: "Create an account or sign in")
                        stepRow(number: "3", text: "Go to API Keys and create a new key")
                        stepRow(number: "4", text: "Copy and paste it above")
                    }
                    Text("Usage is billed to your Anthropic account. Each suggestion costs roughly $0.001 or less.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .padding(.top, 8)
            }
            .padding(20)
        }
        .onAppear { loadExistingKey() }
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
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(.thinMaterial, in: Capsule())
                    .padding(.bottom, 28)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.2) {
                            withAnimation { toastMessage = nil }
                        }
                    }
            }
        }
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

    private func loadExistingKey() {
        if let existing = APIKeyStore.getKey() {
            apiKey = existing
            isConnected = true
        }
    }

    private func saveKey() {
        let trimmed = apiKey.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        APIKeyStore.setKey(trimmed)
        isConnected = true
        withAnimation { toastMessage = "API key saved" }
    }

    private func removeKey() {
        APIKeyStore.setKey(nil)
        apiKey = ""
        isConnected = false
        withAnimation { toastMessage = "API key removed" }
    }
}

#Preview {
    NavigationStack {
        AISettingsSheet()
    }
}
