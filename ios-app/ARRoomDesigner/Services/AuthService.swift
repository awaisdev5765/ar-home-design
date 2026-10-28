import Foundation
import FirebaseAuth

final class AuthService {
    func login(email: String, password: String) async throws {
        _ = try await Auth.auth().signIn(withEmail: email.trimmingCharacters(in: .whitespacesAndNewlines), password: password)
    }

    func register(email: String, password: String) async throws {
        _ = try await Auth.auth().createUser(withEmail: email.trimmingCharacters(in: .whitespacesAndNewlines), password: password)
    }

    func isLoggedIn() -> Bool {
        Auth.auth().currentUser != nil
    }
}
