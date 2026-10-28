import Foundation

@MainActor
final class ProjectsViewModel: ObservableObject {
    @Published var projects: [Project] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let service: ProjectsService

    init(service: ProjectsService = ProjectsService()) {
        self.service = service
    }

    func loadProjects() async {
        isLoading = true
        errorMessage = nil
        do {
            projects = try await service.loadProjects()
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func createProject(title: String, description: String) async {
        isLoading = true
        errorMessage = nil
        do {
            _ = try await service.createProject(title: title, description: description)
            projects = try await service.loadProjects()
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}
