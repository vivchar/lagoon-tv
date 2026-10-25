# Ratings and reviews

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/ratings-and-reviews
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS (user-facing); tvOS · watchOS · visionOS (indirect — App Store ratings apply)

People decide whether to download based on ratings. You can ask for ratings — carefully. Ask at the wrong moment and users leave 1-star reviews. Ask at the right moment and you get the 5-star community you deserve.

## Rules

- **Ask after demonstrated engagement.** User has used the app for a week. User completed a game level. User booked a trip, finished a workout, made something they're proud of. Not on launch. Not during onboarding.
- **Look for natural stopping points.** Between actions, after a success, when the user pauses — not mid-task.
- **Don't pester.** If they dismissed, give it at least a week or two. Apple limits you to **three prompts per 365-day period** per app regardless.
- **Use the system prompt.** `SKStoreReviewController.requestReview(in:)` — now wrapped as SwiftUI's `@Environment(\.requestReview)`. Consistent, non-intrusive, users already know it.
- **Never build your own rating dialog.** App Review enforces the system one for ratings that count toward your App Store rating.
- **Don't gate features behind a rating.** Never "rate us for 5 coins" or "rate us to unlock X."
- **Don't show the rating UI unless the user is a good bet.** If analytics says they've had friction, skip — a grudge rating hurts.

## When to ask

Good moments:

- **User completed a meaningful task** — shipped a project, finished a workout, beat a level, published a post.
- **User returned to the app after several sessions** — repeat users are your biggest fans.
- **User shared something from the app** — social proof moment.
- **User used a premium feature** — signals commitment.

Bad moments:

- **First launch.**
- **During onboarding.**
- **After an error.**
- **Mid-task.**
- **After a crash.**
- **During a payment flow.**
- **Immediately after prompting for a different permission.**

## Reset summary rating on new version?

Apple lets you **reset your summary rating** when you ship a new version.

### When to reset

- **Significant redesign** that likely changes user perception.
- **Major new capability** that users were asking for.

### When NOT to reset

- **Minor feature release.**
- **Bug fix release.**
- Your existing rating is strong — resetting loses social proof.

**Reset trade-off:** your new average is more accurate, but you have fewer ratings and that drives down discoverability. Weigh carefully.

## Soft rating / feedback within the app

If you want user feedback beyond stars — for product development, not App Store ratings — build your own **in-app feedback** separately.

### Rules

- **Don't confuse it with the App Store rating** — different purpose, different user expectation.
- **Label clearly.** "Send feedback to the team" is not "rate the app."
- **Non-blocking.** Feedback is voluntary; don't interrupt a flow to ask.
- **Actionable responses** when possible. "Thanks — we've logged your feedback. You'll hear back in 1-2 business days."

## Platform specifics

No additional considerations. The system prompt works on iOS, iPadOS, macOS. tvOS / watchOS / visionOS defer to the App Store rating of the iOS companion or the system apps they install through.

## Common mistakes

- Prompting on first launch.
- Prompting during onboarding.
- Prompting after an error.
- Prompting mid-task.
- Prompting multiple times in quick succession (system will silently ignore but users notice you trying).
- Custom rating UI that bypasses `SKStoreReviewController`.
- Gating content / features behind a rating.
- Resetting summary on every minor release.
- Pushing a prompt from a notification.
- Confusing App Store rating with internal feedback channel.

## SwiftUI

```swift
import StoreKit
import SwiftUI

struct PostWorkoutSummary: View {
    @Environment(\.requestReview) private var requestReview
    let workout: Workout

    var body: some View {
        VStack(spacing: 16) {
            Text("Great job!").font(.largeTitle.bold())
            // Metrics, Activity Rings, etc.
            SummaryView(workout: workout)

            Button("Done") {
                // Only prompt after a meaningful milestone and if the user
                // is a repeat user.
                if workout.user.completedWorkoutsThisMonth >= 5 {
                    requestReview()
                }
                dismiss()
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }
}
```

## UIKit

```swift
import StoreKit

func maybeRequestReview(after milestone: Milestone) {
    guard milestone == .completedProject,
          user.completedProjects > 2,
          let scene = UIApplication.shared.connectedScenes
              .first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene else { return }

    SKStoreReviewController.requestReview(in: scene)
}
```

## AppKit

```swift
import StoreKit

func maybeRequestReview() {
    guard user.engagementScore >= threshold else { return }
    SKStoreReviewController.requestReview()
}
```

## See also

- [`./feedback.md`](./feedback.md) — in-app feedback (different from App Store ratings).
- [`./onboarding.md`](./onboarding.md) — don't ask for ratings during onboarding.
- [`./managing-notifications.md`](./managing-notifications.md) — don't use notifications to solicit ratings.
- [`./in-app-purchase.md`](./in-app-purchase.md) — don't combine rating requests with payment flows.
