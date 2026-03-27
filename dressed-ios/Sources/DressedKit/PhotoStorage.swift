import Foundation

enum PhotoStorage {
    static func saveJPEGData(_ data: Data) throws -> String {
        let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let name = UUID().uuidString + ".jpg"
        let url = dir.appendingPathComponent(name, isDirectory: false)
        try data.write(to: url, options: [.atomic])
        return url.path
    }
}
