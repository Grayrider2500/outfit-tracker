import Foundation

/// Cloud provider for wardrobe picker “AI reasoning” explanations (BYOK).
enum PickerAIProvider: String, CaseIterable, Identifiable {
    case anthropic
    case openai
    case grok

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .anthropic: return "Anthropic (Claude)"
        case .openai: return "OpenAI"
        case .grok: return "Grok / xAI"
        }
    }

    /// Short label for the picker status banner.
    var shortBannerLabel: String {
        switch self {
        case .anthropic: return "Claude"
        case .openai: return "OpenAI"
        case .grok: return "Grok"
        }
    }
}
