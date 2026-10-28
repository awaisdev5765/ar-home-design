import Foundation

struct MaterialCatalogItem: Identifiable {
    let id: String
    let name: String
    let category: String
    let presetCode: String
    let colorHex: String
    let active: Bool
}
