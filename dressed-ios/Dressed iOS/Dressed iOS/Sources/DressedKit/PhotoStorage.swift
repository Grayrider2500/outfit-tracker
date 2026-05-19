import Foundation
import UIKit

enum PhotoStorage {

    private static let defaultMaxLongEdge: CGFloat = 1600
    private static let pickedPhotoJPEGQuality: CGFloat = 0.87

    static func saveJPEGData(_ data: Data) throws -> String {
        let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let name = UUID().uuidString + ".jpg"
        let url = dir.appendingPathComponent(name, isDirectory: false)
        try data.write(to: url, options: [.atomic])
        return url.path
    }

    /// Camera / Photos picker: resize (long edge ≤ max pixels, aspect preserved), JPEG compress, new file only.
    static func saveOptimizedPickedPhotoJPEG(
        from data: Data,
        maxLongEdge: CGFloat = defaultMaxLongEdge,
        quality: CGFloat = pickedPhotoJPEGQuality
    ) throws -> String {
        guard let image = UIImage(data: data) else {
            throw NSError(domain: "PhotoStorage", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid image data"])
        }
        let normalized = image.normalizedUpOrientation()
        let scaled = normalized.scaledToMaxLongEdgePixels(maxLongEdge)
        guard let jpeg = scaled.jpegData(compressionQuality: quality) else {
            throw NSError(domain: "PhotoStorage", code: 2, userInfo: [NSLocalizedDescriptionKey: "JPEG encoding failed"])
        }
        return try saveJPEGData(jpeg)
    }

    static func readJPEGData(at path: String) -> Data? {
        let url = URL(fileURLWithPath: path)
        guard FileManager.default.fileExists(atPath: path) else { return nil }
        return try? Data(contentsOf: url)
    }

    static func deleteFileIfExists(at path: String?) {
        guard let path, !path.isEmpty, FileManager.default.fileExists(atPath: path) else { return }
        try? FileManager.default.removeItem(atPath: path)
    }
}

private extension UIImage {
    /// Bitmap pixel dimensions use `size.width/height × scale`.
    func scaledToMaxLongEdgePixels(_ maxLongEdge: CGFloat) -> UIImage {
        guard maxLongEdge > 0 else { return self }
        let w = size.width * scale
        let h = size.height * scale
        let longEdge = max(w, h)
        guard longEdge > maxLongEdge else { return self }
        let ratio = maxLongEdge / longEdge
        let newW = max(1, floor(w * ratio))
        let newH = max(1, floor(h * ratio))
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: newW, height: newH), format: format)
        return renderer.image { _ in
            draw(in: CGRect(origin: .zero, size: CGSize(width: newW, height: newH)))
        }
    }

    /// Draws so `imageOrientation` is `.up` (correct geometry for camera captures).
    func normalizedUpOrientation() -> UIImage {
        guard imageOrientation != .up else { return self }
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = scale
        let renderer = UIGraphicsImageRenderer(size: size, format: format)
        return renderer.image { _ in
            draw(in: CGRect(origin: .zero, size: size))
        }
    }
}
