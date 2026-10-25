# Control Center (and custom Controls, iOS 18+)

**HIG sources (composite — HIG's Control Center content lives in the iOS / Controls guidance rather than a standalone technologies page):**
- https://developer.apple.com/design/human-interface-guidelines/designing-for-ios
- https://developer.apple.com/documentation/ControlsKit (developer)

**Last verified:** 2026-04-21
**Applies to:** iOS 18+ · iPadOS 18+ (Controls — user-installable Control Center buttons)

Since iOS 18, your app can ship **Controls** — custom buttons that users can add to Control Center, assign to the Lock Screen, the Action button, or the side button on compatible iPhones. Controls are a high-leverage surface: they're invokable without opening your app.

## What a Control is

A Control is a small, focused, action-shaped entry point into one of your app's features. Types:

- **Button** — a single-shot action ("Scan receipt", "New note", "Mute mic").
- **Toggle** — a two-state switch ("Focus mode on / off", "Dark Mode on / off").

Controls live in Control Center by default. Users can:

- **Add / remove** Controls from Control Center.
- **Assign** a Control to the **Lock Screen** (replacing the flashlight or camera slot on iPhones that support it).
- **Assign** a Control to the **Action button** (iPhone 15 Pro and later).

## Rules

- **One clear action per control.** A Control is not a multi-step form; it's a fire-and-forget button.
- **Works without opening your app** whenever possible. The best Controls don't make the user launch the app.
- **Fast.** Users tap Control Center and swipe away. If your Control takes > 1 second, design a clear in-progress state.
- **Respect authentication.** If the action requires auth, present a lightweight inline prompt — don't push the user into your full app unless necessary.
- **Don't advertise.** Controls that open your app's "What's New" page are unwelcome.
- **Sensible label and glyph.** 1–2 words + an SF Symbol. Abbreviate when needed.
- **Use SF Symbols.** Custom glyphs must match system style and render well in tinted form.
- **Localize** the display name.
- **Support Shortcuts.** A Control should be equivalent to (or built on) an App Intent so users can invoke the same action from Shortcuts, Siri, and the Action button.

## Good Control examples

- **Scan receipt** — a camera-capture app's Control opens the scanner directly.
- **New workout** — fitness app starts a workout with the user's default type.
- **Lock all notes** — privacy app locks everything with one tap.
- **Toggle Home Wi-Fi** — home automation app flips a known switch.
- **Mute microphone** — meeting app.

## Don't

- **Don't make the Control a launcher.** "Open MyApp" is not a useful Control.
- **Don't hide destructive actions behind a Control.** Accidental taps happen in Control Center.
- **Don't request repeated confirmation** — if an action needs confirming, either pick a non-destructive action or make the confirmation inline and one-tap.
- **Don't stack controls** — one action per control.

## Appearance

Controls render in Control Center in a standard tile with:

- **Glyph** (SF Symbol or custom symbol).
- **Short label** below the glyph.
- **State indicator** for toggles (on/off visual).
- **Tinted background** matching user's Control Center theme.

The system renders your Control in both full-color and monochrome appearances depending on the context (Control Center, Lock Screen, Action button).

## Placement conventions

- **Control Center grid** — user organizes; your Control gets placed via the customization UI.
- **Lock Screen** — one custom slot on the leading side, one on the trailing side (replaces flashlight / camera by default). User chooses.
- **Action button** — single hardware shortcut. User picks one assignment.

## SwiftUI

```swift
import AppIntents
import WidgetKit

// An App Intent is the foundation — makes the action scriptable
// from Shortcuts, Siri, Action button, and Control.
struct StartWorkoutIntent: AppIntent {
    static var title: LocalizedStringResource = "Start Workout"
    static var description = IntentDescription("Start your default workout.")

    func perform() async throws -> some IntentResult {
        WorkoutEngine.shared.startDefault()
        return .result()
    }
}

// A Control Widget ships the Control.
struct StartWorkoutControl: ControlWidget {
    var body: some ControlWidgetConfiguration {
        StaticControlConfiguration(kind: "startWorkout") {
            ControlWidgetButton(action: StartWorkoutIntent()) {
                Label("Start Workout", systemImage: "figure.run")
            }
        }
        .displayName("Start Workout")
        .description("Starts your default workout.")
    }
}

// A toggle Control.
struct LockNotesIntent: AppIntent {
    static var title: LocalizedStringResource = "Lock All Notes"
    @Parameter(title: "Locked") var locked: Bool
    func perform() async throws -> some IntentResult {
        NoteStore.shared.setLocked(locked)
        return .result()
    }
}

struct LockNotesControl: ControlWidget {
    var body: some ControlWidgetConfiguration {
        StaticControlConfiguration(kind: "lockNotes") {
            ControlWidgetToggle("Lock Notes",
                                isOn: NoteStore.shared.isLocked,
                                action: LockNotesIntent()) { isOn in
                Label(isOn ? "Locked" : "Unlocked",
                      systemImage: isOn ? "lock.fill" : "lock.open")
            }
        }
        .displayName("Lock Notes")
    }
}
```

## Common mistakes

- Control that opens the app with no action performed.
- Control that requires typing or multi-step input.
- Long-running action without progress feedback.
- Destructive action (delete, send, publish) as a single-tap Control.
- Custom glyph that fights SF Symbols' style.
- Non-localized label.
- No App Intent under the hood — the Control can't be used from Shortcuts/Siri/Action button.
- Multiple Controls that all do "roughly" the same thing — confuses users.
- Marketing-driven Controls.

## See also

- [`./shortcuts-and-siri.md`](./shortcuts-and-siri.md) — App Intents are the core primitive; Controls layer on top.
- [`./lock-screen.md`](./lock-screen.md) — Controls can replace the Lock Screen flashlight/camera slot.
- [`../platforms/ios.md`](../platforms/ios.md) — Action button and Control Center context.
- [`../foundations/sf-symbols.md`](../foundations/sf-symbols.md) — glyph selection rules.
