# Offering help

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/offering-help
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

The best help is the kind the user never needs. When they do, it should be **contextual** (relevant to what they're doing right now), **small** (a tip, a tooltip, a one-screen tutorial), and **easy to dismiss**.

## Rules

- **Match help to task complexity.** One-step task → inline hint. Multi-step task → short tutorial. Don't write a manual for a button.
- **Contextual over global.** Help that appears near the relevant feature beats help that's buried in a separate Help menu.
- **Don't explain standard system UI.** Users know how buttons work. Explain only what's unique to your app.
- **Use platform-specific language.** "Tap" on iOS, "Click" on macOS. "Press" on tvOS. Don't write "tap the menu" in a Mac app.
- **Match images to context.** Show the Siri Remote in tvOS help; show a game controller only if the user has one connected.
- **Inclusive content** — follow [`../foundations/inclusion.md`](../foundations/inclusion.md) (pending).
- **Easy to dismiss.** A visible close, skip, or dismiss action. Don't trap users in help.
- **Don't replay help on every launch.** First-time or contextual; discoverable later in a Help menu / settings / profile area.

## Tips (TipKit) — iOS / iPadOS / macOS / visionOS / watchOS

A tip is a small, transient view that briefly explains how to use a feature. Great for:

- New features a user might not discover.
- Shortcuts / fast paths to tasks.
- Easy-to-describe features.

### Rules

- **Short, actionable, engaging.** 1–2 sentences. Verb-first. "Double-tap to jump into a new message from anywhere."
- **Simple features only.** If the feature needs > 3 steps, it's too complex for a tip.
- **Eligibility rules.** Use parameters (app state) and events (user actions) to decide when a tip appears. Don't show a tip about a feature someone has already used.
- **Display frequency.** When you have multiple tips, rate-limit them — maybe once every 24 hours. Tip Kit handles this automatically.
- **No promotional content.** Tips are for the current feature; don't advertise unrelated features or subscriptions.
- **Use associated iconography.** A star tip for favorites. Prefer the filled SF Symbol variant.
- **Don't repeat the same image** in both the tip and the UI it points at.
- **Buttons for "learn more" or "jump to settings."** Don't explain in prose what a button can reveal in one tap.

### Tip types

- **Popover tip** — floats over UI, preserves content flow. Good when the UI is busy and you don't want the tip to push content.
- **Inline tip** — takes space in the layout. Good when you want the surrounding info to remain visible.
- **Annotation-style inline tip** — points to a specific UI element.
- **Hint-style inline tip** — general hint not tied to a specific element.

### SwiftUI (TipKit)

```swift
import TipKit

struct ComposeTip: Tip {
    var title: Text { Text("Quick compose") }
    var message: Text? {
        Text("Double-tap the inbox to jump into a new message.")
    }
    var image: Image? { Image(systemName: "square.and.pencil") }

    @Parameter static var hasViewedInbox: Bool = false

    var rules: [Rule] {
        // Only show after the user has seen the inbox at least 3 times.
        #Rule(Self.$hasViewedInbox) { $0 == true }
    }
}

struct InboxView: View {
    private let composeTip = ComposeTip()

    var body: some View {
        List(items) { ItemRow(item: $0) }
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button { compose() } label: {
                        Label("Compose", systemImage: "square.and.pencil")
                    }
                    .popoverTip(composeTip)            // attaches a popover tip
                }
            }
            .task { try? Tips.configure() }
    }
}

// Or inline:
TipView(composeTip) { _ in
    // dismissed
}
.padding(.horizontal)
```

## Tooltips / help tags (macOS, visionOS)

On macOS (and visionOS with a pointer or gaze), the system shows a **tooltip** on hover — a small transient label describing a control. `.help("...")` in SwiftUI.

### Rules

- **Describe the control only.** Not nearby controls. Not a larger task.
- **Start with a verb.** "Restore default settings." "Add or remove a language from the list."
- **Don't repeat the control's name.** "Save" as the tooltip of a Save button is useless. Say what the button *does*.
- **Be brief.** 60–75 characters where possible (localization inflates).
- **Sentence case.** Complete sentences optional; omit trailing punctuation unless needed.
- **Context-sensitive text** for controls with multiple states. "Lock document" vs "Unlock document" on the same button.

### visionOS specifics

- **Appear on gaze** as well as on pointer hover.
- **Short labels** — users look briefly, then pinch. Long tooltips interrupt flow.

### SwiftUI

```swift
Button {
    save()
} label: {
    Image(systemName: "tray.and.arrow.down.fill")
}
.help("Save document")
```

## Help menu (macOS)

A standard macOS convention: **Help menu** at the far right of the menu bar, containing:

- **App name Help** — opens your app's help documentation.
- **Additional help entries** for specific topics.
- **Search** — system provides an automatic search across your help content + menu items.

### Rules

- **Always provide app help** — it's a deeply ingrained macOS expectation.
- **Minimum: a single "MyApp Help" entry** that opens relevant documentation in Help Viewer or your website.
- **Help Viewer + Apple Help Indexer** for long-form docs; simple external URL is OK for short help.
- **Keyboard shortcut:** ⌘? (no custom binding).

### SwiftUI

```swift
.commands {
    CommandGroup(replacing: .help) {
        Link("MyApp Help", destination: URL(string: "https://help.example.com")!)
            .keyboardShortcut("?")
        Divider()
        Link("Keyboard Shortcuts", destination: URL(string: "https://help.example.com/shortcuts")!)
        Link("Contact Support", destination: URL(string: "https://example.com/support")!)
    }
}
```

## Longer help — tutorials, documentation

For multi-step flows, shipping a real tutorial is sometimes right:

- **Optional.** Users can skip on first launch.
- **Discoverable later.** Keep it reachable from a Help menu, Settings, or profile area — just not on every launch.
- **Interactive beats passive.** Let the user actually try the action while learning.
- **Short.** < 1 minute per section.
- **Accessible.** All text is Dynamic Type; VoiceOver labels on every interactive element; Reduce Motion skips any parallax or scene transitions.

## Common mistakes

- Tips that advertise unrelated features.
- Help that replays every launch.
- Tooltip that repeats the control's name ("Save" = "Save").
- Help explaining how buttons work.
- Multi-paragraph tips on a simple feature.
- No dismiss action on an onboarding-style tutorial.
- Mac app without a Help menu.
- iOS language ("tap") in Mac help.
- Help text not localized.
- Tip shown at a bad moment (during active typing; mid-animation).
- Help that points the user to external docs when an inline explanation would suffice.
- No TipKit on a feature users consistently miss (you'll see the pattern in analytics).

## See also

- [`./onboarding.md`](./onboarding.md) — first-launch help vs contextual tips.
- [`./feedback.md`](./feedback.md) — inline validation vs help.
- [`../foundations/writing.md`](../foundations/writing.md) (pending) — tone.
- [`../foundations/accessibility.md`](../foundations/accessibility.md) — Dynamic Type / VoiceOver on help content.
- [`../platforms/macos.md`](../platforms/macos.md) — Help menu conventions.
