# Labels and badges

**HIG sources:**
- https://developer.apple.com/design/human-interface-guidelines/labels
- https://developer.apple.com/design/human-interface-guidelines/tab-bars (badge guidance)
- https://developer.apple.com/design/human-interface-guidelines/managing-notifications (app icon badges)

**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

Labels display uneditable text. Badges display "critical new" signal — typically as a red oval on a tab, list row, or app icon. Both are small; neither is where you dump the interesting information.

## Labels

### When to use

- **Inside buttons, menu items, views** — naming what the element does or contains.
- **Describing rows in a list** (often with a leading symbol).
- **Contextualizing a control** when the surrounding UI doesn't already make the meaning clear.
- **Error messages, diagnostic text, location strings, IP addresses** — make them selectable for copy.

### Rules

- **Use a label for uneditable text.** For editable short text → `TextField`. For editable long text → `TextView`.
- **Prefer system fonts.** SwiftUI `Text` and UIKit/AppKit labels use the system font by default and pick up Dynamic Type. Don't apply a custom font without a reason.
- **Use semantic label colors to communicate relative importance:**

| System color | Usage |
|---|---|
| `.label` | Primary content |
| `.secondaryLabel` | Subheading or supplemental text |
| `.tertiaryLabel` | Unavailable item or behavior |
| `.quaternaryLabel` | Watermark — very low emphasis |

- **Make useful text selectable.** An error message with a failure code, a URL, an IP address, a session ID — let the user copy it. `.textSelection(.enabled)` in SwiftUI; `UILabel.isSelectable = true` (iOS 17+); `NSTextField.isSelectable = true`.
- **Dynamic Type is automatic for text styles.** If you use `.font(.body)` / `UIFont.preferredFont(forTextStyle: .body)`, scaling works. See [`../foundations/typography.md`](../foundations/typography.md).

### Platform specifics

- **macOS:** use `NSTextField.stringValue` for displayed text; prefer `NSTextField.isEditable = false` over `NSLabel` (which doesn't exist in AppKit).
- **watchOS:** special **date/time and countdown/timer text components** that watchOS auto-updates and auto-sizes. Don't try to roll your own live clock — use the system components. They also work beautifully in complications.
- Other platforms: no additional considerations.

### Common mistakes

- Using a label where a text field should go (breaks copy/paste expectations).
- Fixed-size point values that defeat Dynamic Type.
- `.foregroundColor(.gray)` instead of `.secondaryLabel` (breaks Dark Mode and Increase Contrast).
- Ignoring selectable text for error / diagnostic content.
- Embedding a custom font in an app just to style one label.
- Manually rolling a watchOS clock instead of using the date-text component.

### SwiftUI

```swift
// Primary vs secondary vs tertiary — use semantics, not colors.
VStack(alignment: .leading, spacing: 2) {
    Text("Weekly summary")
        .font(.headline)
        .foregroundStyle(Color(.label))
    Text("12 items synced • 2 min ago")
        .font(.subheadline)
        .foregroundStyle(.secondary)
    Text("Draft")
        .font(.caption)
        .foregroundStyle(.tertiary)
}

// Selectable diagnostic text.
Text("Request failed. Session: 3f2a-4b91-ea77")
    .font(.footnote.monospaced())
    .foregroundStyle(.secondary)
    .textSelection(.enabled)

// watchOS auto-updating date component.
#if os(watchOS)
Text(Date.now, style: .time)          // the system updates this for you
Text(Date.now, style: .relative)
#endif
```

### UIKit

```swift
let label = UILabel()
label.font = .preferredFont(forTextStyle: .headline)
label.textColor = .label
label.adjustsFontForContentSizeCategory = true
label.numberOfLines = 0

let subtitle = UILabel()
subtitle.font = .preferredFont(forTextStyle: .subheadline)
subtitle.textColor = .secondaryLabel
subtitle.adjustsFontForContentSizeCategory = true

// Selectable from iOS 17+
if #available(iOS 17, *) {
    label.isSelectable = true
}
```

### AppKit

```swift
let title = NSTextField(labelWithString: "Weekly summary")
title.font = .preferredFont(forTextStyle: .headline, options: [:])
title.textColor = .labelColor

let diagnostic = NSTextField(wrappingLabelWithString: "Session: 3f2a-4b91-ea77")
diagnostic.font = .preferredFont(forTextStyle: .footnote, options: [:])
diagnostic.textColor = .secondaryLabelColor
diagnostic.isSelectable = true
```

## Badges

A badge is a small visual indicator of **new or critical information** — traditionally a red oval with white text (number or exclamation). Apple uses badges on:

- **Tab bar items** (e.g., number of unread messages in a tab).
- **List row trailing accessories** (macOS Mail mailbox counts, iOS Reminders counts).
- **App icons** — Home Screen / Dock badge with a count or alert symbol.
- **Dynamic Island / Live Activity** for long-running status.

### Rules

- **Reserve for critical information.** Badging everything kills the signal. Unread message count = yes; "you logged in recently" = no.
- **Numeric or alert-symbol only.** Apple's tab-bar and app-icon badge is a red oval with white text — a number, an `!`, or an alert glyph. No arbitrary text inside the badge.
- **Hide the badge when the count is zero.** Leaving "0" visible is noise.
- **Badges synchronize with reality.** Clear them when the user has seen or dismissed the thing. Stale badges are worse than no badges.
- **Don't badge to show "new feature"** — use TipKit or an onboarding tip instead.
- **App icon badges require permission.** Users grant you the right to badge the icon as part of notification authorization (`UNAuthorizationOptions.badge`). Respect the user's choice.
- **Complement badges with VoiceOver** — SwiftUI's `.badge` modifier announces "n items" automatically; UIKit `UITabBarItem.badgeValue` and `UIApplication.applicationIconBadgeNumber` also integrate with VoiceOver.

### Where badges go

| Surface | What it shows |
|---|---|
| Tab bar item | Unread count in that section |
| List row trailing | Count / status for that row |
| Top-right of a toolbar icon | New-activity signal for the icon's destination |
| App icon (home screen / Dock) | System-wide unread/new count |
| Dynamic Island | Live update for a Live Activity |
| Widget corner | Not a badge — use the widget's own layout |

### Platform specifics

- **iOS / iPadOS:** `UIApplication.shared.applicationIconBadgeNumber` (pre iOS 17) or `UNUserNotificationCenter.current().setBadgeCount(_:)` (iOS 17+). Tab bar badging via `UITabBarItem.badgeValue`.
- **macOS:** `NSApp.dockTile.badgeLabel = "12"` for the Dock badge. Menu bar extras can badge themselves if you're an NSStatusItem app.
- **tvOS:** no app icon badges; badge visible focus items sparingly.
- **watchOS:** notification counts aggregate; don't maintain your own badging scheme.
- **visionOS:** tab bar badges work the same as iPadOS.

### Common mistakes

- Showing "0" badge when nothing is new.
- Badging for marketing ("New feature!") instead of real new-activity signal.
- Badging without clearing after view — users see a 37 forever.
- Using a badge for a general-purpose numeric display (use a label).
- Non-numeric text inside the badge ("NEW" / "HOT" — use different UI).
- App icon badge without `UNAuthorizationOptions.badge` requested.
- Tab bar badge that doesn't match reality after background fetch updates state.

### SwiftUI

```swift
// Tab bar badge.
TabView {
    Tab("Inbox", systemImage: "tray") { InboxView() }
        .badge(unreadCount > 0 ? "\(unreadCount)" : nil)
    Tab("Alerts", systemImage: "bell") { AlertsView() }
        .badge(hasCritical ? "!" : nil)
}

// List row badge.
List {
    NavigationLink(value: Box.inbox) {
        Label("Inbox", systemImage: "tray")
    }
    .badge(unreadCount)

    NavigationLink(value: Box.archive) {
        Label("Archive", systemImage: "archivebox")
    }
    .badge(archiveCount > 0 ? archiveCount : nil)
}

// App icon badge (iOS 17+).
import UserNotifications
try? await UNUserNotificationCenter.current().setBadgeCount(unreadTotal)
```

### UIKit

```swift
// Tab bar badge.
tabBarController?.tabBar.items?[0].badgeValue = unreadCount > 0 ? "\(unreadCount)" : nil

// App icon badge (iOS 17+).
import UserNotifications
UNUserNotificationCenter.current().setBadgeCount(unreadTotal)
```

### AppKit

```swift
// Dock tile badge on macOS.
NSApp.dockTile.badgeLabel = unreadTotal > 0 ? "\(unreadTotal)" : nil

// NSStatusItem (menu bar extras).
statusItem.button?.title = unreadTotal > 0 ? "● \(unreadTotal)" : ""
```

## See also

- [`../foundations/color.md`](../foundations/color.md) — semantic label colors.
- [`../foundations/typography.md`](../foundations/typography.md) — Dynamic Type and text styles.
- [`./bars.md`](./bars.md#tab-bars) — tab bar badge rules.
- [`../patterns/feedback.md`](../patterns/feedback.md) — when to badge vs. when to inline-indicate.
- [`../patterns/managing-notifications.md`](../patterns/managing-notifications.md) (pending) — notification and badge permissions.
