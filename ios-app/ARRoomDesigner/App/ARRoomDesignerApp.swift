import SwiftUI

@main
struct ARRoomDesignerApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            AppRootView()
        }
    }
}
