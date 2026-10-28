import SwiftUI

struct AppRootView: View {
    @StateObject private var authViewModel = AuthViewModel()
    @StateObject private var projectsViewModel = ProjectsViewModel()

    var body: some View {
        if authViewModel.isLoggedIn {
            ProjectsView(viewModel: projectsViewModel)
        } else {
            AuthView(viewModel: authViewModel)
        }
    }
}
