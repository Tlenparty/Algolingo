import SwiftUI
import UserNotifications

@main
struct iOSApp: App {
    init() {
        schedulePracticeReminder()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

/// Daily "time to practice" reminder, fired 24h after the last app launch and then every day.
private func schedulePracticeReminder() {
    let center = UNUserNotificationCenter.current()
    center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
        guard granted else { return }
        let content = UNMutableNotificationContent()
        content.title = "Пора потренироваться 🧠"
        content.body = "Пара задач — 5 минут, без клавиатуры. Стрик сам себя не продлит!"
        content.sound = .default
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 24 * 60 * 60, repeats: true)
        // Same identifier replaces the previous pending reminder, so each launch resets the 24h timer.
        center.add(UNNotificationRequest(identifier: "practice", content: content, trigger: trigger))
    }
}
