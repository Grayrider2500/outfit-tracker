import Foundation
import SwiftData

@Model
final class BorrowedLibrary {
    @Attribute(.unique) var id: String
    var sharerName: String
    var importedAtEpochMs: Int64

    @Relationship(deleteRule: .cascade, inverse: \BorrowedItem.library)
    var items: [BorrowedItem] = []

    init(id: String = UUID().uuidString, sharerName: String, importedAtEpochMs: Int64) {
        self.id = id
        self.sharerName = sharerName
        self.importedAtEpochMs = importedAtEpochMs
    }
}
