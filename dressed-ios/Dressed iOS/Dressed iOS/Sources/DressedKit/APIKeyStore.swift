import Foundation
import Security

/// Securely stores and retrieves the user's Anthropic API key in the iOS Keychain.
enum APIKeyStore {
    private static let service = "com.dressed.app"
    private static let account = "anthropic-api-key"

    /// Read the stored API key, or nil if not set.
    static func getKey() -> String? {
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

    /// Save or update the API key. Pass nil or empty string to delete.
    static func setKey(_ key: String?) {
        // Always delete first to avoid duplicates
        let deleteQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
        SecItemDelete(deleteQuery as CFDictionary)

        guard let key, !key.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              let data = key.data(using: .utf8) else { return }

        let addQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
        ]
        SecItemAdd(addQuery as CFDictionary, nil)
    }

    /// Whether a key is currently stored.
    static var hasKey: Bool {
        getKey() != nil
    }
}
