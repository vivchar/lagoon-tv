# Feedback

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/feedback
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

Feedback tells users what's happening, what just happened, and what they can do next. The right feedback matches the **importance** of the information to the **level of interruption**.

## Match significance to surface

| Significance | Surface |
|---|---|
| Passive state (unread count, sync time) | Badge, inline label, toolbar status |
| Success confirmation of a non-trivial action | Toast, haptic, inline checkmark, symbol animation |
| Recoverable error | Inline validation, banner, badge |
| Critical non-recoverable event | Alert (blocking) |
| Destructive confirmation | Action sheet / confirmation dialog |
| Impossible action | Disabled state + tooltip or label explaining why |

## Multi-channel is accessibility

Every feedback surface should communicate through **more than one channel** — color, text, sound, haptics, motion. Why:
- Silent device → no audio.
- Away from screen → no visuals.
- VoiceOver user → needs text.
- Color blindness → needs shape/icon too.

Minimum pairings:
- **Color + icon + label** for state changes.
- **Haptic + visual** for confirmations on iOS/iPadOS/watchOS.
- **Audio + visual** on visionOS (no haptics).

## Rules

- **Show status where it matters.** Unread count on the mailbox toolbar reads without interruption. A sync indicator inline with a list lets users check when they care. Better than a modal "Sync complete."
- **Reserve alerts for critical, actionable information.** Overuse dilutes their meaning. See [`../components/sheets-and-popovers.md`](../components/sheets-and-popovers.md#alerts).
- **Warn only when data loss is unexpected and irreversible.** Don't warn on expected destructive actions (deleting a mail). Do warn on surprising ones.
- **Confirm successful significant actions** — Apple Pay transaction, file uploaded, order placed. Don't confirm routine successes (auto-save).
- **Explain when a command can't complete.** Don't silently fail. Maps tells you "can't route to and from the same location." Your equivalent should too.

## Haptics (iOS, iPadOS, watchOS)

Haptics are the fastest, cheapest feedback channel on Apple mobile devices. Pair with visual.

- **`.success` / `.warning` / `.error`** — short notification-style feedback.
- **`.impact` (`.light`, `.medium`, `.heavy`, `.soft`, `.rigid`)** — physical-event feedback; match intensity to the event.
- **`.selection`** — changing a value (picker wheel, segmented control).
- **Don't haptic-spam.** Every interaction with a haptic gets boring and drains the battery.
- **Respect the Silent switch** for audio, but haptics generally still fire — which is the point.

## Symbol animations

SF Symbols 5+ ship built-in animations that serve as feedback:

- **`.bounce`** — one-shot acknowledgment (item saved).
- **`.pulse`** — ongoing state (listening, recording).
- **`.replace`** — toggle between states (play ↔ pause).
- **`.wiggle`** — draw attention to a change or call to action.
- **`.variableColor`** — progress / activity (cellular bars, loading).

See [`../foundations/sf-symbols.md`](../foundations/sf-symbols.md#animations).

## Toasts and inline banners

Apple platforms don't have a universal "toast" primitive but the pattern exists:

- **Snackbar-style inline banner** at the top/bottom of a view, auto-dismisses after 2–4s.
- **Action embedded** ("Message deleted / Undo") — make undo easy for 5 seconds.
- **Don't use for errors that need acknowledgment.** Use inline validation or an alert.
- **Stack queue, not overlap.** Multiple banners fight for attention.

## Inline validation

Forms should validate at the right moment:

- **Live** for fields with requirements the user is satisfying ("8+ characters, 1 digit"). Show progress as they type.
- **On blur** for syntactic checks (email format, phone number).
- **On submit** for cross-field or server-side checks (email already in use).

Inline messages should appear **below the field**, in a destructive or neutral tone, with a clear icon. Don't use an alert for validation unless the problem is show-stopping.

## Empty and error states

- **Explain why**. "No notifications" + "We'll show you new alerts here." beats a blank canvas.
- **Recovery action when possible.** Error states get a "Try again" button; empty states get a "Create your first…" button when relevant.
- **Use `ContentUnavailableView`** in SwiftUI for platform-consistent empty/error states.

## Platform notes

### iOS / iPadOS
- **`sensoryFeedback` modifier** is the SwiftUI entry point for haptics.
- **Dynamic Island / Live Activities** are long-running status surfaces.
- **Live Activities auto-dismiss**; don't rely on them for acknowledgment.

### macOS
- **Status bar** and window proxy icon convey state (unsaved dot in title bar, sync indicator).
- **NSAlert** for critical; **NSUserNotification** / Notification Center for background events.
- **Undoable actions** set the undo action name on the `UndoManager` so the Edit menu reads correctly.

### tvOS
- **Focus changes = feedback.** Scale + motion confirm selection.
- **Audio cues** — the Siri Remote plays subtle clicks; don't disable them.
- **No haptics.**

### visionOS
- **No haptics — audio is the feedback channel.** Standard controls already provide it.
- **Gaze + subtle hover effect** is its own form of feedback.
- **Motion in peripheral view is risky** — see [`../foundations/motion.md`](../foundations/motion.md).

### watchOS
- **Avoid indeterminate progress indicators.** (See [`./loading.md`](./loading.md).)
- **Haptic + crown click** for selection changes.
- **Notifications are the dominant feedback channel** — use them for background completions.

## Writing feedback

- **Describe the event, not the implementation.** "Message sent" not "200 OK from /messages".
- **Match urgency to tone.** Success messages are cheerful but brief. Errors are direct, not accusatory.
- **Offer next step** when relevant. "Couldn't connect — try again" + button.
- **No exclamation marks** by default. Reserve for genuinely celebratory moments.

## Common mistakes

- Alerts used for success.
- Toasts used for errors that require acknowledgment.
- Haptic storms on every tap.
- Silent failures (form fails to submit, nothing happens).
- Loading spinner as the only indicator that work is being done, with no success confirmation.
- Validation errors that appear in a separate alert instead of inline.
- "Are you sure?" on every delete.
- Empty lists with no explanatory copy.
- Log-style error messages surfaced to users ("Error 329347: timeout_exceeded_eagain").
- tvOS apps disabling the remote's audio cues.
- Live Activity used as a confirmation surface.

## SwiftUI

```swift
// Success haptic + symbol bounce on save.
@State private var didSave = false

Button("Save") {
    save()
    didSave.toggle()
}
.sensoryFeedback(.success, trigger: didSave)
.overlay(alignment: .trailing) {
    if didSave {
        Image(systemName: "checkmark.circle.fill")
            .foregroundStyle(.tint)
            .symbolEffect(.bounce, value: didSave)
            .padding(.trailing, 8)
    }
}

// Inline validation error.
@State private var email = ""
@State private var emailError: String?

VStack(alignment: .leading, spacing: 4) {
    TextField("Email", text: $email)
        .textContentType(.emailAddress)
        .textInputAutocapitalization(.never)
    if let emailError {
        Label(emailError, systemImage: "exclamationmark.circle.fill")
            .foregroundStyle(.red)
            .font(.caption)
    }
}
.onChange(of: email) { _, new in
    emailError = isValid(new) ? nil : "Enter a valid email address"
}

// Empty state with a recovery action.
ContentUnavailableView {
    Label("No messages yet", systemImage: "tray")
} description: {
    Text("Your inbox is clear. Check back later or start a new conversation.")
} actions: {
    Button("Compose") { compose() }
        .buttonStyle(.borderedProminent)
}

// Status in the toolbar — passive.
.toolbar {
    ToolbarItem(placement: .status) {
        if isSyncing { Label("Syncing", systemImage: "arrow.triangle.2.circlepath") }
        else if let last = lastSync { Text("Last sync \(last, style: .relative)") }
    }
}
```

## UIKit

```swift
// Haptic for success.
UINotificationFeedbackGenerator().notificationOccurred(.success)

// Inline banner (custom view) with auto-dismiss.
func showBanner(_ message: String) {
    let banner = BannerView(message: message)
    view.addSubview(banner)
    banner.pin(to: view.safeAreaLayoutGuide.topAnchor)
    UIView.animate(withDuration: 0.3) { banner.alpha = 1 }
    DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
        UIView.animate(withDuration: 0.3) { banner.alpha = 0 } completion: { _ in
            banner.removeFromSuperview()
        }
    }
}

// UndoManager for an editable state.
undoManager?.registerUndo(withTarget: self) { vc in vc.restore(item) }
undoManager?.setActionName("Delete Item")
```

## AppKit

```swift
// Status bar / badge in the Dock.
NSApp.dockTile.badgeLabel = "\(unreadCount)"

// User notification for background event.
let content = UNMutableNotificationContent()
content.title = "Backup complete"
content.body = "1,204 files uploaded to iCloud Drive."
let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
UNUserNotificationCenter.current().add(request)

// Unsaved dot in title bar.
window.isDocumentEdited = hasUnsavedChanges
```

## See also

- [`./modality.md`](./modality.md) — when feedback demands modality (critical alerts).
- [`./loading.md`](./loading.md) — feedback during async work.
- [`../components/sheets-and-popovers.md`](../components/sheets-and-popovers.md#alerts) — alerts and confirmation dialogs.
- [`../foundations/sf-symbols.md`](../foundations/sf-symbols.md#animations) — symbol animations as feedback.
- [`../foundations/accessibility.md`](../foundations/accessibility.md) — multi-channel feedback is an accessibility requirement.
