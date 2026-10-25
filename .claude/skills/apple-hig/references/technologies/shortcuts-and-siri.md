# Shortcuts and Siri

**HIG sources:**
- https://developer.apple.com/design/human-interface-guidelines/siri

**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · watchOS · visionOS · tvOS · HomePod · AirPods

Shortcuts and Siri turn your app's tasks into things users can invoke by voice, from automation, from the Action button, from a Control, from a Home Screen icon, or from inside other apps. If your app has a task people perform regularly, expose it as an **App Intent** — that makes it available everywhere at once.

## The central primitive: App Intents

App Intents (modern Swift) replaces the older SiriKit Intents. An App Intent represents a task your app can do. Once you ship it, the system can surface it in:

- **Shortcuts app** — users compose multi-step shortcuts from your intents.
- **Siri** — voice invocation.
- **Spotlight search** — appear as a suggestion.
- **Action button** (iPhone 15 Pro+) — user-configurable hardware shortcut.
- **Lock Screen / Control Center Controls** — iOS 18+.
- **Home Screen icon** — users can pin specific app-intent shortcuts to the Home Screen.
- **Focus filters** — your intent can change behavior based on Focus mode.
- **AppShortcuts** — auto-generated phrases your users can say.
- **Accessibility (Voice Control)** — every intent gets a voice target for free.

If you build one App Intent per meaningful action, you've unlocked every invocation surface.

## Types of intents

- **System-defined intents (domains)** — Siri knows the natural-language around these. Your app just provides the data.
  - Calling, Messaging, Notes, Reminders, Payments, Media playback (play, add, like, dislike, search), Workouts, CarKey.
- **Custom App Intents** — your app defines what the task is, what parameters it takes, and how it executes.

**Always prefer a system-defined intent** when one fits. Siri's language model handles "send a message to Amy about dinner tonight" for you.

## Phases of handling an intent

Any intent goes through three phases. SwiftUI / App Intents handles most of this automatically:

1. **Resolve** — collect and disambiguate parameters. "Send a message to Amy" → which Amy? Prompt or pick automatically.
2. **Confirm** — optional confirmation before executing. Reserved for actions with financial or destructive impact.
3. **Handle** — execute the action. Return a result that Siri can speak and display.

## Rules

- **Represent real user tasks.** Not every feature in your app is a good intent. Pick the 3–10 things users actually do.
- **Design for voice-only.** Siri on AirPods / HomePod / while driving has no screen. Every intent's result must be speakable.
- **Design for hands-free.** CarPlay, Apple Watch, AirPods → reduce input.
- **Short, direct conversations.** Don't trap users in a long dialogue.
- **Support background operation.** Intents that run without bringing the app to the front feel fast and unobtrusive. Prefer it.
- **Ensure intents work across devices.** The same intent might run on Mac, iPhone, or Apple Watch. Don't hardcode screen assumptions.
- **Intents can have Siri Suggestions.** Provide metadata so the system can suggest your shortcut at the right time (location, time of day, routine).
- **Handle failure gracefully.** If something can't complete (closed coffee shop, no network), say why in the confirm/handle phase.
- **Never ask for Siri permission** if you only have custom intents (no system intents).

## Designing for Siri voice experience

### System intents

You **don't write Siri's dialogue** for system intents — Siri knows how to ask "Who do you want to send it to?" on its own. Your job:

- **Provide vocabulary.** Teach Siri about custom terms your app uses — account names, contact names, playlist names. Siri uses these to resolve the user's request.
- **Alternative app names** — if users call your app "Unicorn Chat" but the bundle is "UnicornChat", list it.
- **Example phrases** — what people can say. Shown in the Siri Help interface.

### Custom intents

You **do write custom dialogue** — follow-up prompts and responses. Rules:

- **Match your app's tone.** If your app is playful, dialogue is playful. If it's utilitarian, dialogue is direct.
- **Short.** "What size?" is a good prompt. "What size would you like for your custom drink order that will be made today?" is not.
- **Open-ended for big-space tasks** ("What do you want to add?") vs **closed for narrow tasks** ("Small, medium, or large?").
- **Confirmation includes context.** "Your large latte is $5.50. Ready to order?" beats "Ready?"
- **Use placeholders in response templates** so your dialogue can include live data.

### Custom intent categories

Pick the category that matches your action. This affects Siri's default dialogue and UI controls.

| Category | Default verb | Additional verbs |
|---|---|---|
| Generic | Do | Run, go |
| Information | View | Open |
| Order | Order | Book, buy |
| Start | Start | Navigate |
| Share | Share | Post, send |
| Create | Create | Add |
| Search | Search | Find, filter |
| Download | Download | Get |
| Other | Set | Request, toggle, check in |

### Custom UI for Siri

You can supply a custom view Siri displays alongside its response (iOS only; watchOS doesn't support custom Siri UI).

- **Max height: half the screen** — content above and below is spoken + waveform.
- **20pt margins** from all edges.
- **Align with the centered app icon** shown above your view.
- **Don't repeat your app name or icon** — Siri already shows them.
- **Still speak the same info** your UI displays — voice-only scenarios must work.

## App Shortcuts

Ship **AppShortcuts** with your app and Siri exposes phrases for them automatically — no user setup required.

Rules:

- **Declare them in code** via `AppShortcutsProvider`.
- **Localized phrases** that include your app name as the trigger anchor ("Start a run with MyApp").
- **Phrases must be self-contained** — Siri recognizes the exact phrase.
- **Provide utterance variations** — users don't all speak the same way.

## Shortcuts app

The Shortcuts app lets users stitch your App Intents into multi-step workflows, automations, and Home Screen shortcuts.

- **Name your intents for scanning.** "Start Workout" reads better than "workoutActionExtended".
- **Summary** describes what the shortcut does with its current parameters — makes multi-step shortcuts readable.
- **Parameters show up in the Shortcuts UI.** Name them for users, not for code.
- **Automations** — your intent can be triggered by time of day, location, NFC, Focus change, or app open.

## Rules summary

- **App Intents first, not SiriKit Intents.** Modern codebase.
- **Background operation where possible.**
- **System intents before custom ones.**
- **Short dialogue, hands-free friendly.**
- **Voice + visual parity.**
- **Don't block on Siri permission if you only have custom intents.**

## Common mistakes

- Defining an intent per app feature instead of per user task.
- Long dialogue loops (resolve → confirm → re-resolve → confirm → handle).
- Intent that always launches the app instead of running in the background.
- Asking Siri permission for custom-only intents.
- Custom UI for Siri that contradicts the spoken dialogue.
- Voice response that reads a URL or session ID out loud.
- Intents that don't work on watchOS (where your app ships for watchOS).
- Confirming non-destructive actions (every step requires "Are you sure?").
- AppShortcuts phrases without the app name as anchor — users can't find them.
- Hardcoding screen assumptions that break when invoked via AirPods/HomePod.

## SwiftUI / App Intents

```swift
import AppIntents

struct StartWorkoutIntent: AppIntent {
    static var title: LocalizedStringResource = "Start Workout"
    static var description = IntentDescription(
        "Starts a workout of the selected type.",
        categoryName: "Workouts"
    )

    @Parameter(title: "Workout type", default: .run)
    var type: WorkoutType

    static var parameterSummary: some ParameterSummary {
        Summary("Start a \(\.$type) workout")
    }

    func perform() async throws -> some IntentResult & ProvidesDialog {
        try await WorkoutEngine.start(type: type)
        return .result(dialog: "Starting your \(type.label) workout.")
    }
}

enum WorkoutType: String, AppEnum {
    case run, walk, cycle, yoga
    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Workout Type"
    static var caseDisplayRepresentations: [WorkoutType: DisplayRepresentation] = [
        .run: "Run",
        .walk: "Walk",
        .cycle: "Cycle",
        .yoga: "Yoga"
    ]
    var label: String { caseDisplayRepresentations[self]?.title.description ?? "Workout" }
}

struct MyAppShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: StartWorkoutIntent(),
            phrases: [
                "Start a \(\.$type) with \(.applicationName)",
                "Start a workout with \(.applicationName)",
                "Begin my \(\.$type) in \(.applicationName)"
            ],
            shortTitle: "Start Workout",
            systemImageName: "figure.run"
        )
    }
}
```

## See also

- [`./control-center.md`](./control-center.md) — Controls are built on App Intents.
- [`./app-clips.md`](./app-clips.md) — App Clips ship App Intents that start Live Activities.
- [`../platforms/ios.md`](../platforms/ios.md) — Action button + Shortcuts integration.
- [`../platforms/watchos.md`](../platforms/watchos.md) — Siri is first-class on the watch.
- [`../platforms/carplay.md`](./carplay.md) — hands-free scenarios.
- [`../components/buttons.md`](../components/buttons.md) — action semantics.
