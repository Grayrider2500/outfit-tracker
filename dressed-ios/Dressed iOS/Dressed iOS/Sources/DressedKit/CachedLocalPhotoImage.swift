import SwiftUI
import UIKit

/// In-memory cache of decoded file images (similar role to Coil’s memory cache on Android).
enum LocalPhotoImageCache {
    static let shared: NSCache<NSString, UIImage> = {
        let c = NSCache<NSString, UIImage>()
        c.countLimit = 200
        return c
    }()
}

/// Decodes `photoPath` off the main actor, caches by path, and shows category emoji until the image is ready.
struct CachedLocalPhotoImage: View {
    var photoPath: String?
    var categoryKey: String
    var emojiSize: CGFloat = 44

    @State private var image: UIImage?

    var body: some View {
        ZStack {
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            } else {
                Text(WardrobeCatalog.emoji(forCategoryKey: categoryKey))
                    .font(.system(size: emojiSize))
            }
        }
        .task(id: photoPath) {
            await load()
        }
    }

    private func load() async {
        image = nil
        let path = photoPath?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !path.isEmpty, FileManager.default.fileExists(atPath: path) else { return }

        let key = path as NSString
        if let cached = LocalPhotoImageCache.shared.object(forKey: key) {
            image = cached
            return
        }

        let pathCopy = path
        let decoded: UIImage? = await Task(priority: .utility) {
            UIImage(contentsOfFile: pathCopy)
        }.value

        guard let decoded else { return }
        LocalPhotoImageCache.shared.setObject(decoded, forKey: key)
        image = decoded
    }
}
