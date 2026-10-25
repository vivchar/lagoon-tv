# Loading

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/loading
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

The best loading experience finishes before the user notices it. Short of that: show something immediately, give users something to do while they wait, and be honest about progress.

## Rules

- **Show something immediately.** A blank screen reads as broken. Skeleton text, cached content, placeholder graphics, or a loading indicator are all better than nothing.
- **Don't block interactions you don't have to.** Load in the background. Let people navigate, read cached content, or start the next step while async work finishes.
- **Communicate the *kind* of wait.**
  - **Determinate** (you know how long): progress bar with a percentage or completion count.
  - **Indeterminate** (you don't): spinner / activity indicator.
- **Match the wait to the right indicator.**
  - < 1s → ideally don't show anything; the work finished before an indicator would have rendered.
  - 1–3s → indeterminate spinner or skeleton.
  - 3s+ → determinate progress with a message.
  - Very long → download in the background and let users keep using the app.
- **If loading is unavoidably long, give people something worth reading.** Game tips, feature highlights, what's new. Gauge the remaining time so content doesn't repeat or get cut off.
- **Download large assets in the background.** Use `BackgroundAssets` to fetch on install, on update, or at idle times.

## Kinds of indicators

| Kind | Use for |
|---|---|
| **Skeleton UI** | List rows and cards loading — shows shape before content. |
| **Placeholder content** | Cached or stale data visible while a refresh happens. |
| **Activity indicator (spinner)** | Quick (<5s) indeterminate operations. |
| **Progress bar** | Known-duration work; upload/download/export. |
| **Progress with step label** | Multi-step work ("Indexing photos 3 of 10"). |
| **In-button spinner** | The button itself is loading after tap — better than a separate spinner somewhere else. |
| **Pull-to-refresh** | User-initiated list refresh; clear gesture + determinate bar. |

## Show content, not spinners

- **Prefer a skeleton or cached content over a full-screen spinner.** Cached lists let people start scanning immediately; skeletons communicate the shape of the incoming content.
- **Don't hide already-loaded content behind a new spinner.** If you're re-syncing the inbox, show the existing list with an inline indicator, not a full-screen overlay.
- **Optimistic UI for user-triggered actions.** Insert the item locally, mark it as pending; reconcile when the server responds. Much snappier than waiting.

## Writing while loading

- **Describe the work**, not the wait. "Signing you in…" > "Loading…"
- **Tell me why it's slow** if it's going to be slow. "Syncing 12,000 photos (about 3 min)".
- **Never lie about progress.** Don't creep a progress bar to 95% and sit there — users remember and don't trust you.

## Games

- Consider a custom loading view. The system progress indicators can feel out of place in a game's visual style.
- Use the loading time for flavor — tips, lore, character introductions — not a blank screen.
- Maintain consistency with the game's style; don't jam Apple chrome into a bespoke UI.

## Platform notes

### iOS / iPadOS / macOS / tvOS / visionOS
No additional platform considerations beyond the rules above.

### watchOS

- **Avoid indeterminate progress indicators.** They make users think they need to keep watching the screen — bad UX on a device designed for glances.
- **Show content immediately.** A loading indicator is OK for 1–2s; anything longer, reassure the user that a notification will fire when the work finishes, and let them move on.
- **Complications and Smart Stack** should never show spinners — they live in short-attention moments.

## Pull-to-refresh

- **iOS, iPadOS, macOS (list-style), visionOS** support pull-to-refresh in lists.
- **Immediately after the gesture, show a determinate indicator.** When the work finishes, dismiss it; don't leave it looping.
- **Not for background sync.** Background-initiated refresh shouldn't trigger pull-to-refresh UI — put a subtle inline indicator elsewhere.

## Accessibility

- **VoiceOver reads loading states.** Use `.accessibilityLabel("Loading messages")` on loading views; update the label when state changes.
- **Reduce Motion** should still respect spinner animation preferences — the system spinners already do.
- **Don't rely on motion alone.** If a spinner is the only indicator, add a label.

## Common mistakes

- A full-screen blank-to-white flash when re-entering a list that had content.
- Indeterminate spinners on watchOS.
- Progress bars that freeze at 95% because the code doesn't model the tail.
- A generic "Loading..." label that says nothing.
- Hiding cached content behind a new spinner on refresh.
- Optimistic UI that silently fails server-side (item appears then vanishes without explanation).
- Pull-to-refresh that spins forever if the network drops.
- Full-screen spinner during a short (<300ms) operation — flashes and vanishes, feels janky.
- Game loading screen with no entertainment value and a determinate bar you can't trust.

## SwiftUI

```swift
// Skeleton list while loading.
struct InboxList: View {
    @State private var state: LoadState<[Message]> = .loading

    var body: some View {
        List {
            switch state {
            case .loading:
                ForEach(0..<6) { _ in SkeletonRow() }
            case .loaded(let items):
                ForEach(items) { MessageRow(message: $0) }
            case .failed(let error):
                ErrorRow(error: error) { Task { await load() } }
            }
        }
        .task { await load() }
        .refreshable { await load() }
        .navigationTitle("Inbox")
    }

    private func load() async {
        state = .loading
        do {
            let items = try await api.fetch()
            state = .loaded(items)
        } catch {
            state = .failed(error)
        }
    }
}

struct SkeletonRow: View {
    var body: some View {
        HStack(spacing: 12) {
            Circle().fill(.tertiary).frame(width: 40, height: 40)
            VStack(alignment: .leading, spacing: 8) {
                RoundedRectangle(cornerRadius: 4).fill(.tertiary).frame(height: 12)
                RoundedRectangle(cornerRadius: 4).fill(.tertiary).frame(height: 10).padding(.trailing, 48)
            }
        }
        .redacted(reason: .placeholder)
        .accessibilityLabel("Loading message")
    }
}

// In-button spinner.
Button(action: save) {
    HStack(spacing: 6) {
        if isSaving { ProgressView() }
        Text(isSaving ? "Saving…" : "Save")
    }
}
.buttonStyle(.borderedProminent)
.disabled(isSaving)

// Determinate progress for a known operation.
VStack(alignment: .leading) {
    Text("Exporting library")
    ProgressView(value: Double(processed), total: Double(total))
    Text("\(processed) of \(total) files")
        .font(.caption)
        .foregroundStyle(.secondary)
}

// Optimistic insert with rollback on failure.
items.insert(newItem, at: 0)
Task {
    do { try await api.create(newItem) }
    catch {
        items.removeAll { $0.id == newItem.id }
        showError(error)
    }
}
```

## UIKit

```swift
// UIActivityIndicatorView inside a table cell during refresh.
let refresh = UIRefreshControl()
refresh.addTarget(self, action: #selector(refresh), for: .valueChanged)
tableView.refreshControl = refresh

// UIProgressView with observed progress.
let progress = UIProgressView(progressViewStyle: .default)
let observed = Progress(totalUnitCount: Int64(total))
progress.observedProgress = observed
// Update observed.completedUnitCount as work advances.
```

## AppKit

```swift
let progress = NSProgressIndicator()
progress.style = .bar
progress.isIndeterminate = false
progress.minValue = 0
progress.maxValue = Double(total)
progress.startAnimation(nil)
// progress.doubleValue = processed
```

## See also

- [`./feedback.md`](./feedback.md) — communicating what happened after loading.
- [`./onboarding.md`](./onboarding.md) — don't gate onboarding on downloads.
- [`../components/progress-and-activity.md`](../components/progress-and-activity.md) (pending) — specific progress components.
- [`../foundations/accessibility.md`](../foundations/accessibility.md) — labeling loading states for VoiceOver.
