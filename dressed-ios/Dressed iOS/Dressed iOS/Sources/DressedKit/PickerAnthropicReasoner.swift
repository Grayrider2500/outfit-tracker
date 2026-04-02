import Foundation

/// Phase 3: optional Claude (Anthropic) explanations after rule-based picker results.
/// Set `ANTHROPIC_API_KEY` in the scheme environment, or `AnthropicAPIKey` in the generated Info.plist (Xcode build setting).
enum PickerAnthropicReasoner {
    private static let endpoint = URL(string: "https://api.anthropic.com/v1/messages")!
    private static let dayMs: Int64 = 86_400_000
    private static let model = "claude-3-5-haiku-20241022"

    static func resolvedApiKey() -> String {
        if let e = ProcessInfo.processInfo.environment["ANTHROPIC_API_KEY"], !e.isEmpty { return e }
        if let s = Bundle.main.object(forInfoDictionaryKey: "AnthropicAPIKey") as? String, !s.isEmpty { return s }
        return ""
    }

    static func enrichIfPossible(
        suggestions: [WardrobePickerEngine.PickerSuggestion],
        occasionId: String,
        weatherIds: Set<String>,
        moodIds: Set<String>,
        nowEpochMs: Int64,
    ) async -> [WardrobePickerEngine.PickerSuggestion] {
        let key = resolvedApiKey()
        guard !key.isEmpty, !suggestions.isEmpty else { return suggestions }

        let occasionLabel = WardrobePickerEngine.occasions.first { $0.id == occasionId }?.label ?? "Outfit"
        let weatherLabels = weatherIds.compactMap { id in
            WardrobePickerEngine.weatherTags.first { $0.id == id }?.label
        }
        let moodLabels = moodIds.compactMap { id in
            WardrobePickerEngine.moodTags.first { $0.id == id }?.label
        }

        guard let body = makeRequestJSON(
            suggestions: suggestions,
            occasionLabel: occasionLabel,
            weatherLabels: weatherLabels,
            moodLabels: moodLabels,
            nowEpochMs: nowEpochMs,
        ) else { return suggestions }

        do {
            var req = URLRequest(url: endpoint)
            req.httpMethod = "POST"
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
            req.setValue(key, forHTTPHeaderField: "x-api-key")
            req.setValue("2023-06-01", forHTTPHeaderField: "anthropic-version")
            req.httpBody = body

            let (data, resp) = try await URLSession.shared.data(for: req)
            guard let http = resp as? HTTPURLResponse, (200 ... 299).contains(http.statusCode) else { return suggestions }
            guard let combined = extractAnthropicText(from: data),
                  let reasons = parseReasons(from: combined, count: suggestions.count) else { return suggestions }

            return zip(suggestions, reasons).map { s, r in
                let t = r.trimmingCharacters(in: .whitespacesAndNewlines)
                if t.isEmpty { return s }
                return WardrobePickerEngine.PickerSuggestion(title: s.title, items: s.items, score: s.score, reason: t)
            }
        } catch {
            return suggestions
        }
    }

    private static func makeRequestJSON(
        suggestions: [WardrobePickerEngine.PickerSuggestion],
        occasionLabel: String,
        weatherLabels: [String],
        moodLabels: [String],
        nowEpochMs: Int64,
    ) -> Data? {
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
        guard let outfitsData = try? JSONSerialization.data(withJSONObject: outfits),
              let outfitsStr = String(data: outfitsData, encoding: .utf8) else { return nil }

        let w = weatherLabels.isEmpty ? "none" : weatherLabels.joined(separator: ", ")
        let m = moodLabels.isEmpty ? "none" : moodLabels.joined(separator: ", ")
        let userText = """
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
        \(outfitsStr)
        """

        let payload: [String: Any] = [
            "model": model,
            "max_tokens": 1024,
            "messages": [
                ["role": "user", "content": userText],
            ],
        ]
        return try? JSONSerialization.data(withJSONObject: payload)
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

    private static func parseReasons(from modelText: String, count: Int) -> [String]? {
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
