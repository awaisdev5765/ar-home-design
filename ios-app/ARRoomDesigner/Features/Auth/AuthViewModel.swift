import Foundation

@MainActor
final class AuthViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var isLoggedIn: Bool
    @Published var errorMessage: String?

    private let service: AuthService

    init(service: AuthService = AuthService()) {
        self.service = service
        self.isLoggedIn = service.isLoggedIn()
    }

    func login(email: String, password: String) async {
        isLoading = true
        errorMessage = nil
        do {
            try await service.login(email: email, password: password)
            isLoggedIn = true
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func register(email: String, password: String) async {
        isLoading = true
        errorMessage = nil
        do {
            try await service.register(email: email, password: password)
            isLoggedIn = true
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}
