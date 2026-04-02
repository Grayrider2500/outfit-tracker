import SwiftData
import SwiftUI

/// Occasion + tag chips, rule-based suggestions, swipeable collage cards (Phase 1 picker).
struct PickerView: View {
    var onNavigateHome: () -> Void

    @Query(sort: \WardrobeItem.addedAtEpochMs, order: .reverse) private var allItems: [WardrobeItem]

    @Environment(\.modelContext) private var modelContext

    @State private var occasionId: String = WardrobePickerEngine.occasions[0].id
    @State private var weatherIds: Set<String> = []
    @State private var moodIds: Set<String> = []
    @State private var suggestions: [WardrobePickerEngine.PickerSuggestion] = []
    @State private var busy = false
    @State private var pageIndex = 0
    @State private var toastMessage: String?
    @State private var showAISettings = false
    @State private var aiBannerState: PickerAIReasoner.BannerState = .needsKey
    @State private var showInsufficientVarietyHint = false
    @State private var suggestionForDetail: WardrobePickerEngine.PickerSuggestion?

    private let navPurple = Color(red: 0.42, green: 0.29, blue: 0.68)

    private var occasionLabel: String {
        WardrobePickerEngine.occasions.first { $0.id == occasionId }?.label ?? "Casual"
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                occasionSection
                tagSection(title: "Weather (optional)", pairs: WardrobePickerEngine.weatherTags, selection: $weatherIds)
                tagSection(title: "Mood (optional)", pairs: WardrobePickerEngine.moodTags, selection: $moodIds)

                aiStatusBanner

                if busy {
                    Text("Checking your wardrobe and building suggestions...")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                }

                Button {
                    generate()
                } label: {
                    HStack(spacing: 10) {
                        Spacer(minLength: 0)
                        if busy {
                            ProgressView()
                                .tint(.white)
                            Text("Building outfits...")
                                .font(.headline)
                        } else {
                            Text("Surprise me")
                                .font(.headline)
                        }
                        Spacer(minLength: 0)
                    }
                    .padding(.vertical, 16)
                    .background(navPurple, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .foregroundStyle(.white)
                }
                .buttonStyle(.plain)
                .disabled(allItems.count < 3)
                .allowsHitTesting(!busy && allItems.count >= 3)

                if busy && suggestions.isEmpty {
                    VStack(spacing: 12) {
                        ProgressView()
                            .scaleEffect(1.1)
                        Text("Checking your wardrobe and building suggestions...")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 24)
                } else if suggestions.isEmpty && !busy {
                    if showInsufficientVarietyHint {
                        Text("Not enough varied items for a full outfit – try adding more pieces.")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    } else {
                        Text("Tap Surprise me for 1–3 full outfits (three or more pieces each). Add at least three in-season pieces, with shoes, outerwear, or accessories to complete a look.")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                } else if !suggestions.isEmpty {
                    resultsPager
                    if showInsufficientVarietyHint, suggestions.count < 3 {
                        Text("Not enough varied items for a full outfit – try adding more pieces.")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .padding(.top, 4)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .sheet(item: $suggestionForDetail) { sug in
            NavigationStack {
                SuggestionDetailSheet(suggestion: sug)
            }
        }
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("← Home") { onNavigateHome() }
                    .foregroundStyle(.white)
            }
            ToolbarItem(placement: .principal) {
                VStack(spacing: 2) {
                    Text("Suggest outfits")
                        .font(.headline)
                        .foregroundStyle(.white)
                    Text("Smart picks from your closet")
                        .font(.caption2)
                        .foregroundStyle(.white.opacity(0.75))
                }
            }
        }
        .toolbarBackground(navPurple, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .onAppear { syncAIBanner() }
        .sheet(isPresented: $showAISettings, onDismiss: { syncAIBanner() }) {
            NavigationStack {
                AISettingsSheet()
            }
        }
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

    private var aiStatusBanner: some View {
        Button {
            showAISettings = true
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "brain")
                    .font(.subheadline)
                VStack(alignment: .leading, spacing: 2) {
                    Text(aiBannerTitle)
                        .font(.caption.weight(.semibold))
                    Text(aiBannerSubtitle)
                        .font(.caption2)
                        .opacity(0.85)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                Image(systemName: "chevron.right")
                    .font(.caption2)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(aiBannerBackgroundColor.opacity(0.14))
            )
            .foregroundStyle(aiBannerForegroundColor)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(aiBannerTitle). \(aiBannerSubtitle)")
    }

    private var aiBannerTitle: String {
        let p = AIReasoningPreferences.selectedProvider
        switch aiBannerState {
        case .needsKey:
            if p == .grok { return "AI explanations (Grok)" }
            return "AI outfit explanations"
        case .keySavedReasoningOff:
            return "AI reasoning is off"
        case .ready:
            return "AI reasoning on · \(p.shortBannerLabel)"
        }
    }

    private var aiBannerSubtitle: String {
        let p = AIReasoningPreferences.selectedProvider
        switch aiBannerState {
        case .needsKey:
            if p == .grok {
                return "Grok isn’t available yet — pick Anthropic or OpenAI, or use on-device hints."
            }
            return "Add your \(p.shortBannerLabel) key to enable (optional)."
        case .keySavedReasoningOff:
            return "\(p.shortBannerLabel) key saved — tap to turn explanations back on."
        case .ready:
            return "\(p.shortBannerLabel) adds short reasons to each suggestion."
        }
    }

    private var aiBannerForegroundColor: Color {
        switch aiBannerState {
        case .needsKey:
            return navPurple
        case .keySavedReasoningOff:
            return .orange
        case .ready:
            return .green
        }
    }

    private var aiBannerBackgroundColor: Color {
        switch aiBannerState {
        case .needsKey:
            return navPurple
        case .keySavedReasoningOff:
            return .orange
        case .ready:
            return .green
        }
    }

    private func syncAIBanner() {
        aiBannerState = PickerAIReasoner.bannerState
    }

    private var occasionSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Occasion")
                .font(.subheadline.weight(.semibold))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(WardrobePickerEngine.occasions) { occ in
                        chip(
                            occ.label,
                            selected: occasionId == occ.id,
                        ) {
                            occasionId = occ.id
                        }
                    }
                }
            }
        }
    }

    private func tagSection(
        title: String,
        pairs: [(id: String, label: String)],
        selection: Binding<Set<String>>,
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.subheadline.weight(.semibold))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(pairs, id: \.id) { pair in
                        chip(
                            pair.label,
                            selected: selection.wrappedValue.contains(pair.id),
                        ) {
                            if selection.wrappedValue.contains(pair.id) {
                                selection.wrappedValue.remove(pair.id)
                            } else {
                                selection.wrappedValue.insert(pair.id)
                            }
                        }
                    }
                }
            }
        }
    }

    private func chip(_ label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.subheadline.weight(.medium))
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(
                    selected ? navPurple.opacity(0.22) : Color(.secondarySystemGroupedBackground),
                    in: Capsule(),
                )
                .overlay(
                    Capsule().stroke(selected ? navPurple : Color.clear, lineWidth: 1.5),
                )
        }
        .buttonStyle(.plain)
    }

    private var resultsPager: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Suggestions")
                .font(.subheadline.weight(.semibold))

            TabView(selection: $pageIndex) {
                ForEach(Array(suggestions.enumerated()), id: \.offset) { idx, sug in
                    VStack(spacing: 12) {
                        SuggestionOutfitCollageCard(suggestion: sug) {
                            suggestionForDetail = sug
                        }
                        HStack(spacing: 12) {
                            Button {
                                saveAsNewOutfit(suggestion: sug)
                            } label: {
                                Label("Save as outfit", systemImage: "square.and.arrow.down")
                                    .font(.subheadline.weight(.semibold))
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                            }
                            .buttonStyle(.plain)

                            Button {
                                wearToday(suggestion: sug)
                            } label: {
                                Label("Wear today", systemImage: "checkmark.circle.fill")
                                    .font(.subheadline.weight(.semibold))
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .background(navPurple, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    .foregroundStyle(.white)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .tag(idx)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .automatic))
            .frame(height: 460)

            Text("Swipe for more looks")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    private func generate() {
        guard !busy else { return }
        busy = true
        suggestions = []
        showInsufficientVarietyHint = false
        pageIndex = 0
        let occasionSnapshot = occasionId
        let weatherSnapshot = weatherIds
        let moodSnapshot = moodIds
        let itemsSnapshot = allItems
        var seedHasher = Hasher()
        seedHasher.combine(UUID().uuidString)
        seedHasher.combine(occasionSnapshot)
        seedHasher.combine(Int64(Date().timeIntervalSince1970 * 1000))
        for w in weatherSnapshot.sorted() { seedHasher.combine(w) }
        for m in moodSnapshot.sorted() { seedHasher.combine(m) }
        let seed =
            Int64.random(in: 1 ... Int64.max)
                ^ Int64(seedHasher.finalize())
                ^ Int64(weatherSnapshot.hashValue)
                ^ Int64(moodSnapshot.hashValue)
        Task {
            let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
            let outcome = WardrobePickerEngine.suggest(
                allItems: itemsSnapshot,
                occasionId: occasionSnapshot,
                weatherTagIds: weatherSnapshot,
                moodTagIds: moodSnapshot,
                seed: seed,
                maxOutfits: 3,
                nowEpochMs: nowMs,
            )
            let insufficient = outcome.shouldShowInsufficientVarietyMessage
            var result = outcome.suggestions
            result = await PickerAIReasoner.enrichIfPossible(
                suggestions: result,
                occasionId: occasionSnapshot,
                weatherIds: weatherSnapshot,
                moodIds: moodSnapshot,
                nowEpochMs: nowMs,
            )
            await MainActor.run {
                suggestions = result
                showInsufficientVarietyHint = insufficient
                busy = false
                if result.isEmpty {
                    let msg = insufficient
                        ? "Not enough varied items for a full outfit – try adding more pieces."
                        : "Not enough matching pieces — try another occasion or tags."
                    withAnimation { toastMessage = msg }
                } else if insufficient {
                    withAnimation {
                        toastMessage = "Not enough varied items for a full outfit – try adding more pieces."
                    }
                }
            }
        }
    }

    private func saveAsNewOutfit(suggestion: WardrobePickerEngine.PickerSuggestion) {
        let df = DateFormatter()
        df.dateFormat = "yyyy-MM-dd"
        let day = df.string(from: Date())
        let outfit = Outfit(
            name: "Picker · \(occasionLabel) · \(day)",
            itemIdsJoined: Outfit.joinItemIds(suggestion.itemIds),
        )
        modelContext.insert(outfit)
        do {
            try modelContext.save()
            withAnimation { toastMessage = "Saved to Outfits" }
        } catch {
            withAnimation { toastMessage = "Save failed: \(error.localizedDescription)" }
        }
    }

    private func wearToday(suggestion: WardrobePickerEngine.PickerSuggestion) {
        let idSet = Set(suggestion.itemIds)
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        for item in allItems where idSet.contains(item.id) {
            item.wornCount += 1
            item.lastWornAtEpochMs = nowMs
        }
        do {
            try modelContext.save()
            withAnimation { toastMessage = "Wear counts updated" }
        } catch {
            withAnimation { toastMessage = "Could not update: \(error.localizedDescription)" }
        }
    }
}

// MARK: - Collage card (matches `OutfitCollageCard` / Android suggestion card)

private struct SuggestionOutfitCollageCard: View {
    let suggestion: WardrobePickerEngine.PickerSuggestion
    var onTap: () -> Void

    private var mainPiecesSummary: String {
        pickerMainPiecesSummaryLine(items: suggestion.items, maxPieces: 3)
    }

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 0) {
                collage
                    .aspectRatio(1, contentMode: .fit)
                    .clipped()

                VStack(alignment: .leading, spacing: 4) {
                    if !mainPiecesSummary.isEmpty {
                        Text(mainPiecesSummary)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(.primary)
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)
                    }
                    Text(suggestion.title)
                        .font(.subheadline.weight(.medium))
                        .lineLimit(2)
                        .padding(.top, mainPiecesSummary.isEmpty ? 0 : 2)
                    Text(suggestion.reason)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .lineLimit(4)
                        .fixedSize(horizontal: false, vertical: true)
                    HStack(spacing: 4) {
                        let n = suggestion.items.count
                        Text("\(n) piece" + (n == 1 ? "" : "s"))
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                        Text("· score \(Int(suggestion.score))")
                            .font(.caption2)
                            .foregroundStyle(Color(red: 0.42, green: 0.29, blue: 0.68))
                    }
                }
                .padding(10)
            }
            .background(Color(.systemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .shadow(color: .black.opacity(0.06), radius: 2, x: 0, y: 1)
        }
        .buttonStyle(.plain)
    }

    private var collage: some View {
        let collageItems = Array(suggestion.items.prefix(4))
        return GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height
            Group {
                if collageItems.isEmpty {
                    emptyCollageCell
                        .frame(width: w, height: h)
                } else if collageItems.count == 1 {
                    collageCell(item: collageItems[0])
                        .frame(width: w, height: h)
                } else {
                    VStack(spacing: 0) {
                        HStack(spacing: 0) {
                            collageCell(item: collageItems[0])
                                .frame(width: w / 2, height: h / 2)
                            collageCell(item: collageItems.count > 1 ? collageItems[1] : nil)
                                .frame(width: w / 2, height: h / 2)
                        }
                        HStack(spacing: 0) {
                            collageCell(item: collageItems.count > 2 ? collageItems[2] : nil)
                                .frame(width: w / 2, height: h / 2)
                            collageCell(item: collageItems.count > 3 ? collageItems[3] : nil)
                                .frame(width: w / 2, height: h / 2)
                        }
                    }
                }
            }
        }
        .background(Color(.secondarySystemGroupedBackground))
    }

    private var emptyCollageCell: some View {
        Color(.secondarySystemGroupedBackground)
            .overlay {
                Text("👗")
                    .font(.system(size: 44))
            }
    }

    @ViewBuilder
    private func collageCell(item: WardrobeItem?) -> some View {
        Group {
            if let item {
                CachedLocalPhotoImage(
                    photoPath: item.photoPath,
                    categoryKey: item.category,
                    emojiSize: 28,
                )
            } else {
                Color(.secondarySystemGroupedBackground).opacity(0.55)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
    }
}

private func pickerDisplayOrder(_ category: String) -> Int {
    switch category {
    case WardrobeCatalog.tops, WardrobeCatalog.dresses: 0
    case WardrobeCatalog.bottoms: 1
    case WardrobeCatalog.outerwear: 2
    case WardrobeCatalog.shoes: 3
    case WardrobeCatalog.accessories: 4
    default: 5
    }
}

private func pickerItemsSortedForDisplay(_ items: [WardrobeItem]) -> [WardrobeItem] {
    items.sorted {
        let o0 = pickerDisplayOrder($0.category)
        let o1 = pickerDisplayOrder($1.category)
        if o0 != o1 { return o0 < o1 }
        if $0.name != $1.name { return $0.name < $1.name }
        return $0.id < $1.id
    }
}

private func pickerMainPiecesSummaryLine(items: [WardrobeItem], maxPieces: Int = 3) -> String {
    pickerItemsSortedForDisplay(items).prefix(maxPieces).map { item in
        let c = item.colorName.trimmingCharacters(in: .whitespacesAndNewlines)
        let n = item.name.trimmingCharacters(in: .whitespacesAndNewlines)
        if !c.isEmpty, !n.isEmpty { return "\(c) \(n)" }
        if !n.isEmpty { return n }
        if !c.isEmpty { return c }
        return WardrobeCatalog.label(forCategoryKey: item.category)
    }.joined(separator: " + ")
}

private struct SuggestionDetailSheet: View {
    let suggestion: WardrobePickerEngine.PickerSuggestion

    var body: some View {
        List {
            Section {
                Text(suggestion.reason)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            } header: {
                Text("Why this look")
            }
            Section {
                ForEach(pickerItemsSortedForDisplay(suggestion.items), id: \.id) { item in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(pieceHeadline(item))
                            .font(.body.weight(.semibold))
                        Text(detailLine(item))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 4)
                }
            } header: {
                Text("All pieces (\(suggestion.items.count))")
            }
        }
        .navigationTitle(suggestion.title)
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
    }

    private func pieceHeadline(_ item: WardrobeItem) -> String {
        let c = item.colorName.trimmingCharacters(in: .whitespacesAndNewlines)
        let n = item.name.trimmingCharacters(in: .whitespacesAndNewlines)
        if !c.isEmpty, !n.isEmpty { return "\(c) \(n)" }
        if !n.isEmpty { return n }
        if !c.isEmpty { return c }
        return WardrobeCatalog.label(forCategoryKey: item.category)
    }

    private func detailLine(_ item: WardrobeItem) -> String {
        var parts: [String] = [WardrobeCatalog.label(forCategoryKey: item.category)]
        let s = item.sizeLabel.trimmingCharacters(in: .whitespacesAndNewlines)
        if !s.isEmpty { parts.append(s) }
        return parts.joined(separator: " · ")
    }
}

#Preview {
    NavigationStack {
        PickerView(onNavigateHome: {})
    }
    .modelContainer(for: [WardrobeItem.self, Outfit.self], inMemory: true)
}
