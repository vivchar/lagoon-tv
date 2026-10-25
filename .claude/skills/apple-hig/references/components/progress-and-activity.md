# Progress and activity (progress indicators, gauges, activity rings)

**HIG sources:**
- https://developer.apple.com/design/human-interface-guidelines/progress-indicators
- https://developer.apple.com/design/human-interface-guidelines/gauges
- https://developer.apple.com/design/human-interface-guidelines/activity-rings

**Last verified:** 2026-04-21
**Applies to:** varies per component

Three progress-shaped components, each with a specific job. Use the right one.

| Component | Shows |
|---|---|
| **Progress indicator** (determinate bar, determinate circular, or indeterminate spinner) | In-flight work with or without known duration |
| **Gauge** | A specific numerical value within a fixed range (not progress; state) |
| **Activity ring** | Apple Fitness Move / Exercise / Stand only. Never anything else |

## Progress indicators

All progress indicators are **transient** — they appear while work is ongoing and disappear when it's done.

### Determinate vs indeterminate

- **Determinate** — you know how long (or how much). Use a progress bar or circular progress with a fillable track.
- **Indeterminate** — you don't. Use an activity indicator / spinner. macOS also has an indeterminate progress bar.

**Prefer determinate whenever possible.** An indeterminate spinner doesn't tell the user whether the work takes 2 seconds or 2 minutes — that's actionable information. Use a determinate bar so people can decide to wait, do something else, or cancel.

### Rules

- **Be accurate about progress.** Don't race to 90% and sit there. Even out the pace of advancement so the progress bar feels honest. Users remember dishonesty.
- **Keep them moving.** A frozen progress indicator reads as a frozen app. If a process stalls, tell the user why and what they can do.
- **Switch indeterminate → determinate when possible.** If you start not knowing the length and then learn it, swap from spinner to bar.
- **Don't switch bar ↔ spinner.** The different shapes confuse — it looks like a new operation.
- **Succinct description label.** "Signing you in…" > "Loading…". Avoid vague verbs (Loading, Authenticating) that don't describe the work.
- **Consistent location.** Put the progress indicator where users will look for it every time.
- **Cancel when it's safe.** Include a Cancel button when interrupting has no bad consequences.
- **Pause + Cancel when cancel loses data.** A partial download that would be lost benefits from a Pause button.
- **Warn before destructive cancel.** An alert with "Resume / Cancel" when the user cancels a costly process.

### Refresh controls (iOS / iPadOS)

`UIRefreshControl` / SwiftUI `.refreshable` — pull-down at the top of a scroll view.

- **Don't make users refresh manually all the time.** Also update content automatically.
- **Title text is optional.** If included, say something meaningful ("Updated 2 min ago"). Don't use it to explain how to refresh.

### Platform specifics

- **iOS / iPadOS / tvOS / visionOS:** nothing additional beyond the common rules.
- **macOS:** indeterminate indicators can be a bar or a spinner. Prefer spinners for background tasks or constrained spaces. Don't label a spinner — context usually makes it clear.
- **watchOS:** **avoid indeterminate indicators.** Users glance at their watch and expect to glance away; a loading spinner invites them to keep watching. Tell people they'll be notified when work finishes.

## Gauges

A gauge shows **a specific value** within a fixed range. It is not a progress indicator — it's a status display.

### Anatomy

- **Circular or linear path.**
- **Standard style:** an indicator marks the current value.
- **Capacity style:** the path fills up to the current value.
- **Accessory variant** — looks like a watchOS complication. Use it in iOS Lock Screen widgets and anywhere you want a complication aesthetic.

### Rules

- **Label both endpoints of the range** — even if the style doesn't display them, VoiceOver reads them.
- **Label the current value** so the user can read it without inferring position.
- **Gradient fill when it communicates meaning.** A temperature gauge from red (hot) to blue (cold) reinforces the range. Don't gradient for decoration.
- **Use a gauge for current-state visualization, not for task progress** (that's a progress indicator).

### macOS level indicators

macOS has a distinct **level indicator** primitive that overlaps with gauges. Styles:

- **Continuous capacity** — a smooth fill (ideal for large ranges).
- **Discrete capacity** — segmented bar (good for small, countable ranges).
- **Rating** — 5 stars / hearts / flags.
- **Relevance** — shaded bar showing relative importance; rarely used outside search-result sorting.

**Change fill color at thresholds** when meaningful — very low / very high states benefit from a red/green flip. Use tiered state to show a sequence of colors within one indicator.

## Activity rings

Move / Exercise / Stand. Those three, that's it. Available on **watchOS and iOS** (via HealthKit). Not on macOS, tvOS, or visionOS.

### Rules — these are strict

- **Only for Move / Exercise / Stand.** Never display other data inside activity rings. Never modify the shape for another purpose.
- **One person only.** Never show rings for multiple people in the same element.
- **Never change the colors.** No filters, no opacity modifications.
- **Always on a black background** — it's part of the visual identity.
- **Prefer enclosing in a circle** (corner-radius approach, not a circular mask — which would clip the outer glow).
- **Black background visible around the outermost ring.** If your container background is non-black, add a thin black stroke around the outer edge.
- **No gradient, shadow, or visual effects on rings.**
- **Scale consistently.** Don't shrink the rings to the point they read as disconnected.
- **Surrounding UI adapts to the rings, not the rings to the UI.**
- **Ring margins** ≥ the distance between rings. Nothing encroaches on the rings themselves.
- **Don't duplicate Activity notifications** — the system already sends Move/Exercise/Stand notifications. Don't show rings in your notifications.
- **Not decoration.** Don't put Activity rings in your app icon, logo, marketing materials, labels, or background art.
- **Non-Activity rings in the same UI** must be clearly visually distinct — padding, lines, labels, different color or scale.

### Colors (when labeling associated data)

- **Move:** pink/red — specific RGB per HIG.
- **Exercise:** green.
- **Stand:** blue/cyan.

Match label colors to the ring if you're labeling ring-specific data.

### iOS specifics

- **With Apple Watch paired:** iOS shows all three rings.
- **Without Apple Watch:** iOS shows **only the Move ring** (approximated from step count + workouts from other HealthKit sources).
- Activity history can contain both states in sequence.

## Common mistakes

- Indeterminate spinner for a 30-second process (use a progress bar).
- Progress bar that lies (95% stuck; last 5% is actually half the work).
- Dismissing a spinner before the work finishes.
- Switching a spinner to a bar mid-flight.
- Spinner on watchOS — invites users to stare.
- Gauge used where a progress indicator belongs.
- Activity rings displaying stats other than Move/Exercise/Stand.
- Activity rings recolored for brand.
- Activity rings displayed on a non-black background without the black-stroke workaround.
- Ring decoration in a splash screen.
- Two similar ring indicators next to Activity rings with no visual separation.

## SwiftUI

```swift
// Determinate progress.
ProgressView(value: Double(processed), total: Double(total))
ProgressView("Exporting library", value: Double(processed), total: Double(total))

// Indeterminate.
ProgressView("Signing in…")          // shows spinner + label

// Circular determinate (iOS / iPadOS / visionOS / watchOS).
ProgressView(value: downloadFraction)
    .progressViewStyle(.circular)

// Gauge — current speed in a bounded range.
Gauge(value: speedKmh, in: 0...220) {
    Text("Speed")
} currentValueLabel: {
    Text("\(Int(speedKmh)) km/h")
} minimumValueLabel: {
    Text("0")
} maximumValueLabel: {
    Text("220")
}
.gaugeStyle(.accessoryCircular)

// Capacity gauge for a storage bar.
Gauge(value: Double(usedBytes), in: 0...Double(totalBytes)) {
    Text("Storage")
} currentValueLabel: {
    Text(ByteCountFormatStyle().format(usedBytes))
}
.gaugeStyle(.linearCapacity)

// Activity rings via HealthKit's SwiftUI view.
import HealthKitUI
HKActivityRingView(activitySummary: summary)   // UIViewRepresentable in SwiftUI
    .frame(width: 120, height: 120)
    .background(Color.black)                   // mandatory

// Refreshable list.
List(items) { ItemRow(item: $0) }
    .refreshable { await store.reload() }
```

## UIKit

```swift
// Determinate bar.
let bar = UIProgressView(progressViewStyle: .default)
let observed = Progress(totalUnitCount: Int64(total))
bar.observedProgress = observed
// ...increment observed.completedUnitCount as work advances

// Activity indicator.
let spinner = UIActivityIndicatorView(style: .medium)
spinner.hidesWhenStopped = true
spinner.startAnimating()

// UIRefreshControl.
let rc = UIRefreshControl()
rc.addTarget(self, action: #selector(refresh), for: .valueChanged)
tableView.refreshControl = rc
```

## AppKit

```swift
let bar = NSProgressIndicator()
bar.style = .bar
bar.isIndeterminate = false
bar.minValue = 0
bar.maxValue = Double(total)
bar.usesThreadedAnimation = true

let spinner = NSProgressIndicator()
spinner.style = .spinning
spinner.isIndeterminate = true
spinner.startAnimation(nil)

// Level indicator (macOS).
let level = NSLevelIndicator()
level.levelIndicatorStyle = .continuousCapacity
level.minValue = 0
level.maxValue = 1.0
level.doubleValue = 0.35
level.warningValue = 0.8
level.criticalValue = 0.95
level.fillColor = .systemGreen
level.warningFillColor = .systemOrange
level.criticalFillColor = .systemRed
```

## See also

- [`../patterns/loading.md`](../patterns/loading.md) — indicator vs skeleton vs cached content.
- [`../patterns/feedback.md`](../patterns/feedback.md) — confirming work completed.
- [`../foundations/sf-symbols.md`](../foundations/sf-symbols.md) — variable-color symbols for progress-like states.
- [`../technologies/widgets-and-live-activities.md`](../technologies/widgets-and-live-activities.md) (pending) — gauges in widgets.
