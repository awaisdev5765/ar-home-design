import SwiftUI

struct AuthView: View {
    @ObservedObject var viewModel: AuthViewModel

    @State private var email = ""
    @State private var password = ""

    var body: some View {
        VStack(spacing: 12) {
            Text("Welcome to AR Home Design")
                .font(.title3)

            TextField("Email", text: $email)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled(true)
                .textFieldStyle(.roundedBorder)

            SecureField("Password", text: $password)
                .textFieldStyle(.roundedBorder)

            Button("Login") {
                Task { await viewModel.login(email: email, password: password) }
            }
            .buttonStyle(.borderedProminent)
            .disabled(viewModel.isLoading)

            Button("Register") {
                Task { await viewModel.register(email: email, password: password) }
            }
            .buttonStyle(.bordered)
            .disabled(viewModel.isLoading)

            if let error = viewModel.errorMessage {
                Text(error)
                    .foregroundStyle(.red)
            }
        }
        .padding()
    }
}
