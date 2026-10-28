import Foundation
import FirebaseAuth
import FirebaseFirestore

final class ProjectsService {
    private let db = Firestore.firestore()

    func createProject(title: String, description: String) async throws -> String {
        guard let uid = Auth.auth().currentUser?.uid else {
            throw NSError(domain: "ProjectsService", code: 401, userInfo: [
                NSLocalizedDescriptionKey: "User must be authenticated."
            ])
        }

        let ref = db.collection("projects").document()
        try await ref.setData([
            "ownerId": uid,
            "title": title.trimmingCharacters(in: .whitespacesAndNewlines),
            "description": description.trimmingCharacters(in: .whitespacesAndNewlines),
            "propertyType": "apartment",
            "status": "draft"
        ])
        return ref.documentID
    }

    func loadProjects() async throws -> [Project] {
        guard let uid = Auth.auth().currentUser?.uid else {
            return []
        }
        let query = db.collection("projects").whereField("ownerId", isEqualTo: uid)
        let snapshot = try await query.getDocuments()
        return snapshot.documents.map { doc in
            let data = doc.data()
            return Project(
                id: doc.documentID,
                ownerId: data["ownerId"] as? String ?? "",
                title: data["title"] as? String ?? "",
                description: data["description"] as? String ?? "",
                propertyType: data["propertyType"] as? String ?? "apartment",
                status: data["status"] as? String ?? "draft"
            )
        }
    }
}
