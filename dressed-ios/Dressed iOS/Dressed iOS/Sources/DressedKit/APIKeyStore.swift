import Foundation
import Security

/// Securely stores per-provider API keys for BYOK picker reasoning (Keychain).
enum APIKeyStore {
    private static let service = "com.dressed.app"
    private static let legacyAnthropicAccount = "anthropic-api-key"

    private static func account(for provider: PickerAIProvider) -> String {
        "picker-key-\(provider.rawValue)"
    }

    private static func read(account: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess, let data = result as? Data,
              let key = String(data: data, encoding: .utf8), !key.isEmpty else {
            return nil
        }
        return key
    }

    private static func delete(account: String) {
        let deleteQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
        SecItemDelete(deleteQuery as CFDictionary)
    }

    /// Read the stored key for a provider, or `nil`. Migrates legacy Anthropic account when needed.
    static func getKey(for provider: PickerAIProvider) -> String? {
        if let v = read(account: account(for: provider)), !v.isEmpty { return v }
        if provider == .anthropic {
            if let v = read(account: legacyAnthropicAccount), !v.isEmpty { return v }
        }
        return nil
    }

    /// Save or update the key for a provider. Pass `nil` or empty to delete.
    static func setKey(_ key: String?, for provider: PickerAIProvider) {
        delete(account: account(for: provider))

        guard let key, !key.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              let data = key.data(using: .utf8) else { return }

        let addQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account(for: provider),
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
        ]
        SecItemAdd(addQuery as CFDictionary, nil)
    }

    static func hasKey(for provider: PickerAIProvider) -> Bool {
        getKey(for: provider) != nil
    }
}

/// User preference: master switch and selected cloud provider for picker AI reasoning.
enum AIReasoningPreferences {
    private static let enabledKey = "com.dressed.aiReasoningEnabled"
    private static let providerKey = "com.dressed.aiPickerProvider"

    /// When never set, default `true` so existing installs keep prior behavior after upgrade.
    static var isReasoningEnabled: Bool {
        get {
            if UserDefaults.standard.object(forKey: enabledKey) == nil { return true }
            return UserDefaults.standard.bool(forKey: enabledKey)
        }
        set { UserDefaults.standard.set(newValue, forKey: enabledKey) }
    }

    /// Stores which BYOK provider the user selected (Anthropic, OpenAI, or Grok placeholder).
    static var selectedProvider: PickerAIProvider {
        get {
            let raw = UserDefaults.standard.string(forKey: providerKey) ?? PickerAIProvider.anthropic.rawValue
            return PickerAIProvider(rawValue: raw) ?? .anthropic
        }
        set { UserDefaults.standard.set(newValue.rawValue, forKey: providerKey) }
    }
}
