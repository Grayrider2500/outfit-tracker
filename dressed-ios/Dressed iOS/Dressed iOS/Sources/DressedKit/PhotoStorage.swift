import Foundation

enum PhotoStorage {
    static func saveJPEGData(_ data: Data) throws -> String {
        let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let name = UUID().uuidString + ".jpg"
        let url = dir.appendingPathComponent(name, isDirectory: false)
        try data.write(to: url, options: [.atomic])
        return url.path
    }

    static func readJPEGData(at path: String) -> Data? {
        let url = URL(fileURLWithPath: path)
        guard FileManager.default.fileExists(atPath: path) else { return nil }
        return try? Data(contentsOf: url)
    }
}
