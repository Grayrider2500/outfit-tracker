import Foundation
import SwiftUI
import UIKit

/// Category keys aligned with Android `WardrobeCategories`.
enum WardrobeCatalog {
    static let allKey = "all"
    static let tops = "tops"
    static let bottoms = "bottoms"
    static let dresses = "dresses"
    static let shoes = "shoes"
    static let outerwear = "outerwear"
    static let accessories = "accessories"

    static let filters: [(key: String, label: String)] = [
        (allKey, "All"),
        (tops, "Tops"),
        (bottoms, "Bottoms"),
        (dresses, "Dresses"),
        (shoes, "Shoes"),
        (outerwear, "Outerwear"),
        (accessories, "Accessories"),
    ]

    static let addPicker: [(key: String, label: String)] = filters.filter { $0.key != allKey }

    static let seasons: [(key: String, label: String)] = [
        ("spring", "Spring"),
        ("summer", "Summer"),
        ("fall", "Fall"),
        ("winter", "Winter"),
    ]

    static func label(forCategoryKey key: String) -> String {
        filters.first { $0.key == key }?.label ?? key
    }

    static func emoji(forCategoryKey key: String) -> String {
        switch key {
        case tops: return "👕"
        case bottoms: return "👖"
        case dresses: return "👗"
        case shoes: return "👠"
        case outerwear: return "🧥"
        case accessories: return "👜"
        default: return "🧷"
        }
    }

    static func sizeSuggestions(for categoryKey: String) -> [String] {
        let letterRun = ["XS", "S", "M", "L", "XL", "XXL", "2XL", "3XL"]
        let womenNumeric = ["0", "2", "4", "6", "8", "10", "12", "14", "16", "18", "20"]
        let waistInches = ["24", "26", "28", "30", "32", "34", "36", "38", "40", "42"]
        let shoeUsWomens = [
            "5", "5.5", "6", "6.5", "7", "7.5", "8", "8.5", "9", "9.5",
            "10", "10.5", "11", "11.5", "12", "12.5", "13",
        ]
        switch categoryKey {
        case tops, outerwear: return letterRun
        case bottoms: return letterRun + waistInches
        case dresses: return letterRun + womenNumeric
        case shoes: return shoeUsWomens
        case accessories:
            return ["One size", "OS", "XS", "S", "M", "L", "XL", "Adjustable"]
        default: return []
        }
    }

    /// Matches Android `COLOR_NAME_SUGGESTIONS` for add-item field.
    static let colorNameSuggestions: [String] = [
        "Black", "Dark Gray", "Gray", "Silver", "Light Gray", "White",
        "Ivory", "Cream", "Beige", "Champagne", "Tan", "Camel",
        "Gold", "Yellow", "Olive", "Brown", "Rust", "Orange",
        "Coral", "Red", "Burgundy", "Blush", "Pink", "Hot Pink",
        "Rose", "Mauve", "Lavender", "Violet", "Purple", "Plum",
        "Indigo", "Navy", "Blue", "Sky Blue", "Cyan", "Teal",
        "Mint", "Sage", "Green", "Slate",
    ]
}

enum WardrobeColorMath {
    /// Extract a hex string like `#8B62D4` from a SwiftUI `Color`.
    static func hexFromColor(_ color: Color) -> String {
        let ui = UIColor(color)
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        ui.getRed(&r, green: &g, blue: &b, alpha: &a)
        return String(
            format: "#%02X%02X%02X",
            Int(round(r * 255)),
            Int(round(g * 255)),
            Int(round(b * 255)),
        )
    }

    /// Port of Android `labelForPickedColor`.
    static func labelForPickedColor(hex: String) -> String {
        let trimmed = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.hasPrefix("#"), trimmed.count >= 7 else { return "Custom" }
        let start = trimmed.index(trimmed.startIndex, offsetBy: 1)
        let hexBody = String(trimmed[start...].prefix(6))
        guard hexBody.count == 6, let value = UInt32(hexBody, radix: 16) else { return "Custom" }
        let r = Int((value >> 16) & 0xFF)
        let g = Int((value >> 8) & 0xFF)
        let b = Int(value & 0xFF)

        struct NC { let name: String; let r: Int; let g: Int; let b: Int }

        let palette: [NC] = [
            NC(name: "Black", r: 0, g: 0, b: 0), NC(name: "Dark Gray", r: 64, g: 64, b: 64),
            NC(name: "Gray", r: 128, g: 128, b: 128), NC(name: "Silver", r: 192, g: 192, b: 192),
            NC(name: "Light Gray", r: 211, g: 211, b: 211), NC(name: "White", r: 255, g: 255, b: 255),
            NC(name: "Ivory", r: 255, g: 255, b: 240), NC(name: "Cream", r: 255, g: 253, b: 208),
            NC(name: "Beige", r: 245, g: 245, b: 220), NC(name: "Champagne", r: 247, g: 231, b: 206),
            NC(name: "Tan", r: 210, g: 180, b: 140), NC(name: "Camel", r: 193, g: 154, b: 107),
            NC(name: "Gold", r: 212, g: 175, b: 55), NC(name: "Yellow", r: 255, g: 215, b: 0),
            NC(name: "Olive", r: 107, g: 142, b: 35), NC(name: "Brown", r: 139, g: 69, b: 19),
            NC(name: "Rust", r: 183, g: 65, b: 14), NC(name: "Orange", r: 255, g: 140, b: 0),
            NC(name: "Coral", r: 255, g: 127, b: 80), NC(name: "Red", r: 220, g: 20, b: 60),
            NC(name: "Burgundy", r: 128, g: 0, b: 32), NC(name: "Blush", r: 255, g: 182, b: 193),
            NC(name: "Pink", r: 255, g: 105, b: 180), NC(name: "Hot Pink", r: 255, g: 20, b: 147),
            NC(name: "Rose", r: 255, g: 0, b: 127), NC(name: "Mauve", r: 153, g: 102, b: 153),
            NC(name: "Lavender", r: 230, g: 230, b: 250), NC(name: "Violet", r: 138, g: 43, b: 226),
            NC(name: "Purple", r: 128, g: 0, b: 128), NC(name: "Plum", r: 142, g: 69, b: 133),
            NC(name: "Indigo", r: 75, g: 0, b: 130), NC(name: "Navy", r: 0, g: 0, b: 128),
            NC(name: "Blue", r: 30, g: 100, b: 200), NC(name: "Sky Blue", r: 135, g: 206, b: 235),
            NC(name: "Cyan", r: 0, g: 188, b: 212), NC(name: "Teal", r: 0, g: 128, b: 128),
            NC(name: "Mint", r: 152, g: 255, b: 152), NC(name: "Sage", r: 143, g: 188, b: 143),
            NC(name: "Green", r: 34, g: 139, b: 34), NC(name: "Slate", r: 112, g: 128, b: 144),
        ]

        return palette.min { lhs, rhs in
            let dl = distSq(lhs.r, lhs.g, lhs.b, r, g, b)
            let dr = distSq(rhs.r, rhs.g, rhs.b, r, g, b)
            return dl < dr
        }?.name ?? "Custom"
    }

    private static func distSq(_ r1: Int, _ g1: Int, _ b1: Int, _ r2: Int, _ g2: Int, _ b2: Int) -> Int {
        let dr = r1 - r2, dg = g1 - g2, db = b1 - b2
        return dr * dr + dg * dg + db * db
    }
}
