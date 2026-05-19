import Foundation

/// Unified BYOK picker reasoning: routes to Anthropic, OpenAI, or a Grok placeholder.
enum PickerAIReasoner {
    private static let dayMs: Int64 = 86_400_000

    enum BannerState: Equatable {
        case needsKey
        case keySavedReasoningOff
        case ready
    }

    // MARK: - Credentials

    static func resolvedApiKey(for provider: PickerAIProvider) -> String {
        if let stored = APIKeyStore.getKey(for: provider), !stored.isEmpty { return stored }
        let env = ProcessInfo.processInfo.environment
        switch provider {
        case .anthropic:
            if let e = env["ANTHROPIC_API_KEY"], !e.isEmpty { return e }
            if let s = Bundle.main.object(forInfoDictionaryKey: "AnthropicAPIKey") as? String, !s.isEmpty { return s }
        case .openai:
            if let e = env["OPENAI_API_KEY"], !e.isEmpty { return e }
            if let s = Bundle.main.object(forInfoDictionaryKey: "OpenAIAPIKey") as? String, !s.isEmpty { return s }
        case .grok:
            if let e = env["XAI_API_KEY"], !e.isEmpty { return e }
            if let e = env["GROK_API_KEY"], !e.isEmpty { return e }
            if let s = Bundle.main.object(forInfoDictionaryKey: "XaiAPIKey") as? String, !s.isEmpty { return s }
        }
        return ""
    }

    /// Grok is not wired to an API in this build — never treat as “ready” for cloud calls.
    private static var effectiveProvider: PickerAIProvider {
        AIReasoningPreferences.selectedProvider
    }

    static var hasApiCredential: Bool {
        let p = effectiveProvider
        if p == .grok { return false }
        return !resolvedApiKey(for: p).isEmpty
    }

    static var bannerState: BannerState {
        guard hasApiCredential else { return .needsKey }
        return AIReasoningPreferences.isReasoningEnabled ? .ready : .keySavedReasoningOff
    }

    static var isAvailable: Bool {
        bannerState == .ready
    }

    // MARK: - Validation

    static func validateApiKey(_ provider: PickerAIProvider, _ apiKey: String) async -> String? {
        let trimmed = apiKey.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return "Enter an API key to test." }

        switch provider {
        case .anthropic:
            return await AnthropicPickerClient.validateKey(trimmed)
        case .openai:
            return await OpenAIPickerClient.validateKey(trimmed)
        case .grok:
            return "Grok / xAI isn’t available in this build yet — choose Anthropic or OpenAI."
        }
    }

    // MARK: - Enrich

    static func enrichIfPossible(
        suggestions: [WardrobePickerEngine.PickerSuggestion],
        occasionId: String,
        weatherIds: Set<String>,
        moodIds: Set<String>,
        nowEpochMs: Int64,
    ) async -> [WardrobePickerEngine.PickerSuggestion] {
        guard AIReasoningPreferences.isReasoningEnabled else { return suggestions }

        let provider = effectiveProvider
        if provider == .grok { return suggestions }

        let key = resolvedApiKey(for: provider)
        guard !key.isEmpty, !suggestions.isEmpty else { return suggestions }

        let occasionLabel = WardrobePickerEngine.occasions.first { $0.id == occasionId }?.label ?? "Outfit"
        let weatherLabels = weatherIds.compactMap { id in
            WardrobePickerEngine.weatherTags.first { $0.id == id }?.label
        }
        let moodLabels = moodIds.compactMap { id in
            WardrobePickerEngine.moodTags.first { $0.id == id }?.label
        }

        let userPrompt = buildUserPrompt(
            suggestions: suggestions,
            occasionLabel: occasionLabel,
            weatherLabels: weatherLabels,
            moodLabels: moodLabels,
            nowEpochMs: nowEpochMs,
        )

        let modelText: String?
        switch provider {
        case .anthropic:
            modelText = await AnthropicPickerClient.complete(userPrompt: userPrompt, apiKey: key)
        case .openai:
            modelText = await OpenAIPickerClient.complete(userPrompt: userPrompt, apiKey: key)
        case .grok:
            modelText = nil
        }

        guard let text = modelText,
              let reasons = parseReasons(from: text, count: suggestions.count) else { return suggestions }

        return zip(suggestions, reasons).map { s, r in
            let t = r.trimmingCharacters(in: .whitespacesAndNewlines)
            if t.isEmpty { return s }
            return WardrobePickerEngine.PickerSuggestion(title: s.title, items: s.items, score: s.score, reason: t)
        }
    }

    // MARK: - Shared prompt + JSON parse

    private static func buildUserPrompt(
        suggestions: [WardrobePickerEngine.PickerSuggestion],
        occasionLabel: String,
        weatherLabels: [String],
        moodLabels: [String],
        nowEpochMs: Int64,
    ) -> String {
        var outfits: [[String: Any]] = []
        for (idx, s) in suggestions.enumerated() {
            var pieces: [[String: Any]] = []
            for it in s.items {
                var p: [String: Any] = [
                    "name": it.name,
                    "category": WardrobeCatalog.label(forCategoryKey: it.category),
                    "color": it.colorName,
                    "worn_count": it.wornCount,
                ]
                if let lw = it.lastWornAtEpochMs {
                    p["days_since_last_worn"] = Int((nowEpochMs - lw) / dayMs)
                }
                pieces.append(p)
            }
            outfits.append([
                "index": idx,
                "title": s.title,
                "rule_based_hint": s.reason,
                "pieces": pieces,
            ])
        }
        let outfitsData = (try? JSONSerialization.data(withJSONObject: outfits)).flatMap { String(data: $0, encoding: .utf8) } ?? "[]"
        let w = weatherLabels.isEmpty ? "none" : weatherLabels.joined(separator: ", ")
        let m = moodLabels.isEmpty ? "none" : moodLabels.joined(separator: ", ")

        return """
        You are a friendly wardrobe assistant inside a mobile app.
        The app already chose these outfits using deterministic rules and scoring.
        For each outfit, write ONE short explanation (1–2 sentences), warm and specific.
        Mention concrete pieces/colors when natural, and occasion/weather/mood if helpful.
        Do not contradict the outfit composition.

        Return ONLY valid JSON with this exact shape, no markdown fences:
        {"reasons":["...","..."]}
        Use exactly \(suggestions.count) strings in the same order as the outfits below.

        Occasion: \(occasionLabel)
        Weather tags: \(w)
        Mood tags: \(m)

        Outfits JSON:
        \(outfitsData)
        """
    }

    static func parseReasons(from modelText: String, count: Int) -> [String]? {
        var t = modelText.trimmingCharacters(in: .whitespacesAndNewlines)
        if t.hasPrefix("```") {
            t = t.replacingOccurrences(of: "^```json\\s*", with: "", options: .regularExpression)
            t = t.replacingOccurrences(of: "^```\\s*", with: "", options: .regularExpression)
            t = t.replacingOccurrences(of: "\\s*```$", with: "", options: .regularExpression)
        }
        guard let start = t.firstIndex(of: "{"),
              let end = t.lastIndex(of: "}"),
              start < end else { return nil }
        let slice = String(t[start ... end])
        guard let data = slice.data(using: .utf8),
              let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let arr = obj["reasons"] as? [String],
              arr.count == count else { return nil }
        return arr
    }
}

// MARK: - Anthropic

private enum AnthropicPickerClient {
    private static let endpoint = URL(string: "https://api.anthropic.com/v1/messages")!
    private static let model = "claude-3-5-haiku-20241022"

    static func validateKey(_ apiKey: String) async -> String? {
        let payload: [String: Any] = [
            "model": model,
            "max_tokens": 8,
            "messages": [
                ["role": "user", "content": "Reply with the single word ok."],
            ],
        ]
        guard let body = try? JSONSerialization.data(withJSONObject: payload) else {
            return "Could not build request."
        }
        return await postAnthropic(body: body, apiKey: apiKey)
    }

    static func complete(userPrompt: String, apiKey: String) async -> String? {
        let payload: [String: Any] = [
            "model": model,
            "max_tokens": 1024,
            "messages": [
                ["role": "user", "content": userPrompt],
            ],
        ]
        guard let body = try? JSONSerialization.data(withJSONObject: payload) else { return nil }
        guard let data = await fetchAnthropicData(body: body, apiKey: apiKey) else { return nil }
        return extractAnthropicText(from: data)
    }

    private static func postAnthropic(body: Data, apiKey: String) async -> String? {
        do {
            var req = URLRequest(url: endpoint)
            req.httpMethod = "POST"
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
            req.setValue(apiKey, forHTTPHeaderField: "x-api-key")
            req.setValue("2023-06-01", forHTTPHeaderField: "anthropic-version")
            req.httpBody = body

            let (data, resp) = try await URLSession.shared.data(for: req)
            guard let http = resp as? HTTPURLResponse else {
                return "Invalid response from server."
            }
            switch http.statusCode {
            case 200 ... 299:
                return nil
            case 401, 403:
                return "Key rejected (check that it is valid and active)."
            default:
                return httpErrorSnippet(http.statusCode, data)
            }
        } catch {
            return error.localizedDescription
        }
    }

    private static func fetchAnthropicData(body: Data, apiKey: String) async -> Data? {
        do {
            var req = URLRequest(url: endpoint)
            req.httpMethod = "POST"
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
            req.setValue(apiKey, forHTTPHeaderField: "x-api-key")
            req.setValue("2023-06-01", forHTTPHeaderField: "anthropic-version")
            req.httpBody = body
            let (data, resp) = try await URLSession.shared.data(for: req)
            guard let http = resp as? HTTPURLResponse, (200 ... 299).contains(http.statusCode) else { return nil }
            return data
        } catch {
            return nil
        }
    }

    private static func extractAnthropicText(from data: Data) -> String? {
        guard let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let content = obj["content"] as? [[String: Any]] else { return nil }
        var out = ""
        for block in content {
            if block["type"] as? String == "text", let t = block["text"] as? String {
                out += t
            }
        }
        return out.isEmpty ? nil : out
    }
}

// MARK: - OpenAI

private enum OpenAIPickerClient {
    private static let endpoint = URL(string: "https://api.openai.com/v1/chat/completions")!
    private static let model = "gpt-4o-mini"

    static func validateKey(_ apiKey: String) async -> String? {
        let payload: [String: Any] = [
            "model": model,
            "max_tokens": 16,
            "messages": [
                ["role": "user", "content": "Reply with the single word ok."],
            ],
        ]
        guard let body = try? JSONSerialization.data(withJSONObject: payload) else {
            return "Could not build request."
        }
        return await postOpenAIExpectingAnyJson(body: body, apiKey: apiKey)
    }

    static func complete(userPrompt: String, apiKey: String) async -> String? {
        let payload: [String: Any] = [
            "model": model,
            "max_tokens": 1024,
            "response_format": ["type": "json_object"],
            "messages": [
                ["role": "user", "content": userPrompt],
            ],
        ]
        guard let body = try? JSONSerialization.data(withJSONObject: payload) else { return nil }
        do {
            var req = URLRequest(url: endpoint)
            req.httpMethod = "POST"
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
            req.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
            req.httpBody = body
            let (data, resp) = try await URLSession.shared.data(for: req)
            guard let http = resp as? HTTPURLResponse, (200 ... 299).contains(http.statusCode) else { return nil }
            return extractOpenAIContent(from: data)
        } catch {
            return nil
        }
    }

    private static func postOpenAIExpectingAnyJson(body: Data, apiKey: String) async -> String? {
        do {
            var req = URLRequest(url: endpoint)
            req.httpMethod = "POST"
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
            req.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
            req.httpBody = body

            let (data, resp) = try await URLSession.shared.data(for: req)
            guard let http = resp as? HTTPURLResponse else {
                return "Invalid response from server."
            }
            switch http.statusCode {
            case 200 ... 299:
                return nil
            case 401, 403:
                return "Key rejected (check that it is valid and active)."
            default:
                return httpErrorSnippet(http.statusCode, data)
            }
        } catch {
            return error.localizedDescription
        }
    }

    private static func extractOpenAIContent(from data: Data) -> String? {
        guard let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let choices = obj["choices"] as? [[String: Any]],
              let first = choices.first,
              let message = first["message"] as? [String: Any],
              let content = message["content"] as? String else { return nil }
        return content
    }
}

private func httpErrorSnippet(_ code: Int, _ data: Data) -> String {
    let snippet = String(data: data, encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    let short = snippet.count > 120 ? String(snippet.prefix(120)) + "…" : snippet
    return short.isEmpty ? "Request failed (HTTP \(code))." : "HTTP \(code): \(short)"
}
