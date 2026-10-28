import SwiftUI

struct ProjectsView: View {
    @ObservedObject var viewModel: ProjectsViewModel
    @State private var showCreate = false

    var body: some View {
        NavigationStack {
            List(viewModel.projects) { project in
                NavigationLink(destination: ARRoomScanView(project: project)) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(project.title).font(.headline)
                        Text(project.description.isEmpty ? "No description" : project.description)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .navigationTitle("Projects")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Refresh") {
                        Task { await viewModel.loadProjects() }
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Create") { showCreate = true }
                }
            }
            .sheet(isPresented: $showCreate) {
                CreateProjectView { title, description in
                    Task {
                        await viewModel.createProject(title: title, description: description)
                        showCreate = false
                    }
                }
            }
            .task {
                if viewModel.projects.isEmpty {
                    await viewModel.loadProjects()
                }
            }
        }
    }
}
