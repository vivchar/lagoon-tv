# Managing notifications

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/managing-notifications
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

Notifications are a powerful — and fragile — permission. Misuse them once and users revoke them forever. The rules: ask **after** the user sees value, match the notification's urgency to reality, respect **Focus** and delivery scheduling, and **never** send marketing via Time Sensitive.

## The permission is earned

- **Don't ask at launch.** Users decline blind permission requests. Show them what your app does, then ask when the notification will be contextually useful.
- **Explain before asking.** A pre-prompt screen that says "Get back-in-stock alerts for items you save" primes consent. Then trigger the system prompt.
- **One ask.** If denied, respect the decision. Deep-link to Settings in context if they change their mind later.

## Communication vs noncommunication

The system distinguishes:

- **Communication notifications** — phone calls, messages from a specific person. Support **SiriKit intents** (call, send message, start workout). People can customize delivery per contact.
- **Noncommunication notifications** — everything else. You assign an **interruption level** to each.

## The four interruption levels (noncommunication)

| Level | Behavior | Overrides scheduled delivery | Breaks Focus | Overrides Ring/Silent |
|---|---|---|---|---|
| **Passive** | Leisure info (restaurant recommendation) | No | No | No |
| **Active** (default) | Useful info the user appreciates (score update) | No | No | No |
| **Time Sensitive** | Requires immediate attention (delivery, security, imminent event) | Yes | Yes | No |
| **Critical** | Urgent health/safety (entitlement required) | Yes | Yes | Yes |

### Rules

- **Accurately represent urgency.** Misusing Time Sensitive costs trust faster than almost any other misstep.
- **Time Sensitive = imminent.** Happening now or within ~an hour. The system describes to the user what Time Sensitive means the first time they see one from your app — and gives them the option to turn them off. Apple re-prompts the user periodically. If your urgency claims are bad, users shut you down.
- **Active is the default.** Most notifications should be Active.
- **Passive for low-stakes info.** Keeps the lights on without pinging the user.
- **Critical requires Apple entitlement.** Reserved for government alerts, health/safety apps, home security. Don't apply speculatively.

## Focus

Users configure Focus modes (Work, Personal, Sleep, Driving, custom). Each Focus filters notifications.

- **Time Sensitive breaks through Focus** by default.
- **Users choose which apps/contacts** break through a specific Focus.
- **Even when a Focus delays the notification alert, the notification itself is available as soon as it arrives** — the user just doesn't see the light/sound until the Focus ends.
- **Focus filters** (iOS 16+): your app can filter its own content based on the user's active Focus. A news app could show work-related stories during Work Focus and casual stories during Personal Focus.

## Delivery scheduling

Users can choose between:

- **Immediate** — your notification fires when you send it.
- **Notification Summary** — bundled into a daily digest at user-chosen times.

You don't choose; the user does. Your notifications must work in both modes.

- **Good title + short body.** Summary view shows them side-by-side. Cryptic titles look terrible in Summary.

## Marketing notifications

- **Never Time Sensitive.** Marketing is never time-urgent.
- **Only with explicit permission.** The standard notification permission is for transactional / informational alerts. Marketing requires a **separate in-app opt-in** — your own UI explaining what you'll send and letting the user opt in or out.
- **In-app settings for changing marketing permission.** Users must be able to turn marketing off in-app, not just via system Settings.
- **No marketing in notifications for transactional content** — don't sneak a promo into a shipping update.

## Rich notifications

- **Custom notification UI via notification service extensions and notification content extensions.**
- **Thumbnail + custom view** — media preview for a message, map for a delivery update, live view for a sports score.
- **Actionable** — inline Reply, Snooze, Mark as Read without opening the app.

## Writing notifications

- **Title: who / what / where** in 4–6 words.
- **Body: the one thing the user needs to know.** No preamble.
- **No marketing language in transactional notifications.** "Your package is here" not "🎉 Your package is here — shop again!"
- **Sentence case. No trailing exclamation marks.** Except for celebrations that earn them.
- **Localize.** Every notification string.
- **Avoid app name in title.** The system shows your app name already.

## Privacy on the Lock Screen

- **Redact sensitive previews.** Notifications on a locked device are public. Respect the user's "Show preview when unlocked" setting.
- **Don't embed sensitive data** (2FA codes, PII, balances) in the visible portion. Reveal only when the user unlocks.

## Platform specifics

### iOS / iPadOS / macOS / tvOS / visionOS
Follow the rules above.

### watchOS

- **Default inherits iPhone settings.** Manage per-app in the Apple Watch app on iPhone.
- **Per-notification controls on the watch** — swipe left when a notification arrives for options: Mute 1 Hour, Mute Today, Turn Off Time Sensitive, Delete.
- **Short titles and bodies** — the watch clips them fast.
- **Haptics matter more than sound** on the watch. Tune the notification sound profile to be unobtrusive.

## Common mistakes

- Asking for permission at launch.
- Sending marketing via Time Sensitive.
- Time Sensitive used for events that aren't time-sensitive.
- Critical level without entitlement or real safety justification.
- Push notifications that duplicate Live Activity updates.
- Transactional notification wrapping a marketing pitch.
- No in-app toggle for marketing opt-in.
- Unlocalized notifications.
- Notification that leaks sensitive info to the Lock Screen.
- Silent failures (user taps notification, nothing happens — deep link missing).
- Badge count out of sync with actual unread count.
- Notification without a deep-link to the relevant content.

## SwiftUI / UserNotifications

```swift
import UserNotifications

// Request permission — after the user has seen value.
func requestNotificationPermission() async {
    let center = UNUserNotificationCenter.current()
    do {
        let granted = try await center.requestAuthorization(
            options: [.alert, .sound, .badge]
        )
        // Handle granted == false gracefully — offer to deep-link to Settings later.
    } catch {
        // Log, but don't nag the user.
    }
}

// Schedule a Time Sensitive notification for an imminent event.
func scheduleDeliveryNotification(at eta: Date) {
    let content = UNMutableNotificationContent()
    content.title = "Your order is almost here"
    content.body = "Arriving in about 10 minutes."
    content.sound = .default
    content.interruptionLevel = .timeSensitive
    content.threadIdentifier = "delivery-\(orderID)"

    let trigger = UNCalendarNotificationTrigger(
        dateMatching: Calendar.current.dateComponents(
            [.year, .month, .day, .hour, .minute], from: eta.addingTimeInterval(-10 * 60)),
        repeats: false
    )

    let request = UNNotificationRequest(
        identifier: "delivery-\(orderID)",
        content: content,
        trigger: trigger
    )
    UNUserNotificationCenter.current().add(request)
}

// Passive — for information the user can view at leisure.
let passiveContent = UNMutableNotificationContent()
passiveContent.interruptionLevel = .passive

// Active (default).
let activeContent = UNMutableNotificationContent()
activeContent.interruptionLevel = .active

// Marketing opt-in UI in-app.
struct MarketingOptInView: View {
    @AppStorage("marketingNotificationsEnabled") private var marketing = false

    var body: some View {
        Form {
            Section {
                Toggle("Get deals and new feature alerts", isOn: $marketing)
            } footer: {
                Text("We'll send no more than one promotional message per week. " +
                     "You can turn this off any time.")
            }
        }
        .navigationTitle("Marketing")
    }
}

// Deep-link to Settings if notifications are denied.
Button("Enable in Settings") {
    if let url = URL(string: UIApplication.openSettingsURLString) {
        UIApplication.shared.open(url)
    }
}
```

## See also

- [`./feedback.md`](./feedback.md) — when to use a notification vs inline banner.
- [`./onboarding.md`](./onboarding.md) — don't request permission during onboarding.
- [`../technologies/widgets-and-live-activities.md`](../technologies/widgets-and-live-activities.md) — Live Activities often beat repeated push notifications.
- [`../technologies/lock-screen.md`](../technologies/lock-screen.md) — Lock Screen privacy of notifications.
- [`../foundations/writing.md`](../foundations/writing.md) (pending) — notification copy tone.
