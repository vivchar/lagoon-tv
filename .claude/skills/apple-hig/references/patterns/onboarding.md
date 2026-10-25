# Onboarding

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/onboarding
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

Ideally people understand your app by using it. When onboarding is genuinely necessary, design a flow that's **fast, fun, and optional**. Onboarding is not a welcome mat for the system, it's not a legal agreement venue, and it's not the place to hoover up every setting the user might ever want to change.

## Rules

- **Optional whenever possible.** Let users skip on first launch. Don't show the tutorial again on subsequent launches — but keep it discoverable in a help/settings/account area.
- **Teach through interaction.** Passive "tap Next to see the next slide" flows don't stick. Let the user perform the action they're learning.
- **Context tips beat a global tour.** Tip Kit-style in-app hints at the moment a feature is relevant make the content land better than a cold onboarding wall.
- **If the tour is prerequisite, keep it short and delightful.** Users are more likely to finish it and remember it.
- **Stay on-topic.** Teach your app, not iOS. Don't teach how to tap, swipe, or unlock the phone.
- **Don't front-load setup.** Ship good defaults. If you absolutely need a user choice to function, ask for it in context when the feature is first used, not in a setup wizard.
- **Don't make onboarding a download gate.** Include enough content in the app package so users can interact immediately.
- **Permission requests belong in context** when possible. If a permission is required for the app to function at all, integrate the request into onboarding — and explain the benefit before the system prompt appears.
- **Defer rating prompts, purchase prompts, and email captures.** Let people become engaged first.
- **Don't host the EULA in onboarding.** The App Store already shows it.

## The anatomy of a minimal tour

1. **Splash / brand screen** (optional). Show briefly, communicate the product in a glance, don't linger.
2. **1–3 value screens.** Each screen = one idea + one visual + one line of text. Maximum.
3. **Interactive moment.** Let the user do one thing the app is great at (compose, search, scan, swipe).
4. **Permissions explainer** (only if required to function). Explain → system prompt → continue or explain recovery if denied.
5. **Hand off to the main UI.** Don't linger on a "You're all set!" screen.

## Alternatives to a formal tour

- **Contextual tips** (e.g., `TipKit` on iOS/iPadOS/macOS) — pop up inline when a feature is first reachable.
- **Empty states that teach.** The first time a list is empty, explain what goes there and how to fill it.
- **Coachmarks** — the classic pointer at "tap here to add your first item". Use sparingly.
- **Sample data** — instead of a long tour, show a curated example the user can poke. Then offer to clear it.
- **Video or animation in context** for complex gestures.

## Writing onboarding copy

- **One idea per screen.** Don't make dense slides.
- **Benefit over feature.** "Find anything, fast" > "Search index technology".
- **Verb-first, action-oriented.** "Send a message in one tap", "See your stats at a glance".
- **Sentence case.** No exclamation marks by default; reserve for celebratory moments.
- **First-person or second-person** (pick one per app) — both work; consistency matters.

## Platform notes

No platform-specific rules beyond the above. All Apple platforms follow the same principles. Practical specifics:

- **iOS / iPadOS / visionOS:** `TipKit` is the standard for contextual tips in 2024+.
- **macOS:** First-run onboarding typically lives in a single onboarding window that closes when finished. Avoid a multi-window tour.
- **watchOS:** Onboarding is almost always "first use of the companion iPhone app" plus an unobtrusive tip on the watch. Don't force a tour on the tiny screen.
- **tvOS:** Short, Remote-friendly — one screen of value, one Play button. No multi-step typing.

## Splash screens — if you must

- **Brief.** Long enough to absorb at a glance, short enough to feel instantaneous.
- **Static visual, concise copy.** Never animate for the sake of animation.
- **Never block.** Splash screen should never prevent the user from interacting with main content.

## Accessibility

- **Don't require gestures.** If you teach a gesture, provide a tap alternative.
- **Readable at AX5 size.** Dynamic Type applies in onboarding too.
- **VoiceOver labels on every interactive element.**
- **Dismissal is obvious.** A visible "Skip" or "Close" with a clear accessibility label — not a tiny X.

## Common mistakes

- A five-step slideshow that explains everything upfront.
- A "select up to 10 topics" interest picker before the user sees the app.
- A paywall or subscription pitch before value has been experienced.
- An email capture as the first screen.
- Permission requests with no explanatory context ("Allow location? Don't Allow / Allow").
- Hiding the skip button.
- Replaying onboarding on every update.
- A splash screen that stays up for 3+ seconds.
- Teaching the system ("Swipe up to go home") instead of the app.
- EULA scroll walls.
- Requiring a download to proceed.

## SwiftUI — minimal three-step onboarding

```swift
struct OnboardingFlow: View {
    enum Step: Int { case welcome, value, permission }

    @State private var step: Step = .welcome
    @AppStorage("hasOnboarded") private var hasOnboarded = false
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Group {
                switch step {
                case .welcome:    WelcomeStep(next: { step = .value })
                case .value:      ValueStep(next: { step = .permission })
                case .permission: PermissionStep(finish: finish)
                }
            }
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Skip") { finish() }
                }
            }
            .animation(.default, value: step)
        }
    }

    private func finish() { hasOnboarded = true; dismiss() }
}

struct ValueStep: View {
    var next: () -> Void
    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "sparkles.rectangle.stack")
                .font(.system(size: 72, weight: .semibold))
                .foregroundStyle(.tint)
                .accessibilityHidden(true)
            Text("See everything in one place")
                .font(.largeTitle.bold())
                .multilineTextAlignment(.center)
            Text("Your reading, your notes, your highlights — synced and searchable.")
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Spacer()
            Button("Continue") { next() }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .frame(maxWidth: .infinity)
        }
        .padding()
    }
}
```

## SwiftUI — contextual tips (TipKit)

```swift
import TipKit

struct ComposeTip: Tip {
    var title: Text { Text("Quick compose") }
    var message: Text? { Text("Double-tap to jump into a new message from anywhere.") }
    var image: Image? { Image(systemName: "hand.tap") }
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
                    .popoverTip(composeTip)
                }
            }
            .task { try? Tips.configure() }
    }
}
```

## See also

- [`./settings.md`](./settings.md) — defer configuration choices out of onboarding.
- [`./loading.md`](./loading.md) — don't block onboarding on downloads.
- [`../foundations/writing.md`](../foundations/writing.md) (pending) — tone and copy rules.
- [`../foundations/privacy.md`](../foundations/privacy.md) (pending) — permission-request UX.
