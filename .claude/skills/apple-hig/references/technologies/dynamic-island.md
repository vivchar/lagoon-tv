# Dynamic Island

**HIG source (composite — no standalone Dynamic Island page in HIG):**
- https://developer.apple.com/design/human-interface-guidelines/live-activities (Compact / Minimal / Expanded)
- https://developer.apple.com/design/human-interface-guidelines/designing-for-ios

**Last verified:** 2026-04-21
**Applies to:** iPhone 14 Pro and later (with the TrueDepth camera island)

The Dynamic Island is the region around the TrueDepth camera on supported iPhones. It hosts system indicators (Silent Mode, Face ID, charging) and, for your app, **Live Activities** in three presentations: compact, minimal, and expanded. Your app doesn't have a Dynamic Island "view" — you build a Live Activity and the system places it in the Island when appropriate.

## The three presentations

### Compact

When **one** Live Activity is active and the system chooses the Dynamic Island, your activity appears as two separate elements flanking the TrueDepth camera.

- **Leading element** — a small glyph or label on the left.
- **Trailing element** — a small glyph or label on the right.
- **Designed as one idea** across the two elements. They read together.
- **Tap opens the app** at the related deep link.
- **Long press** expands to the Expanded presentation.

Rules:

- **Snug to the camera.** No padding between your content and the TrueDepth camera.
- **Balanced widths.** Use shortened units or less precision to keep leading and trailing elements similar in size.
- **No ads or promotions** in the compact.
- **Don't obscure the status bar.** Keep element heights modest.

### Minimal

When **multiple** Live Activities are active, the system uses the minimal presentation to fit two of them in the Island. One attaches to the Island; the other detaches and floats as a circle or oval.

- **Extremely limited content.** A glyph, a number, a short progress arc.
- **Still recognizable.** Prefer live information (remaining time, score) over a static icon. Timer shows the remaining time; Ride-share shows an icon + ETA digit.
- **Tap opens the app.**
- **Long press expands.**

### Expanded

When the user **touches and holds** a compact or minimal presentation, the system displays the expanded presentation. Also shown on alerts (lighting up the screen for a major update) and briefly on iPhones without the Dynamic Island (as a banner).

Layout regions:

- **Leading** — mirrors the compact leading; more room for content.
- **Trailing** — mirrors the compact trailing.
- **Center** — between leading/trailing at the top.
- **Bottom** — progress bar, controls, secondary content.

Rules:

- **Wrap content tightly around the TrueDepth camera.** Don't leave empty padding around it — the camera is part of the composition.
- **Maintain relative placement** from compact/minimal to expanded. Don't rearrange; enlarge.
- **Single interactive element preferred** (pause button, stop button).
- **Max 2-second animations.**
- **Don't replicate notification banner layouts** — that's for the Lock Screen presentation instead.

## Hardware context

- **Liquid (motion-aware) animation.** When the Island expands, collapses, or morphs between states, the system provides fluid transitions around the camera housing.
- **Status bar coexistence.** Your Live Activity coexists with the status bar; keep content out of the battery/time/cellular zones.
- **Dimmed on Always On.** The Island dims; animations pause. Design so your content still reads and doesn't require motion to be understood.

## Non-supported iPhones

- On iPhones **without** a Dynamic Island, an update with alert fires a **Lock Screen-style banner** instead.
- Design the Lock Screen presentation carefully — it's the fallback.

## Color

- **Background is black** (opaque) in compact / minimal / expanded. You cannot override.
- **Bold content colors.** Bright glyphs and digits pop; muted colors disappear.
- **Key line color** — set via `.keylineTint(_)` in SwiftUI. Appears around the Island in Dark Mode to distinguish it from surrounding content.

## Alerts

When your Live Activity fires an alert, the Island expands + the screen lights + the notification sound plays. Reserve for critical updates (food delivered, ride arriving). Overuse fatigues and users turn Live Activities off entirely.

## Common mistakes

- **Padding between compact elements and the TrueDepth camera.** The system expects your content snug to the camera.
- **Unbalanced compact leading/trailing widths.** Looks visually lopsided.
- **Ads or marketing** in the Island.
- **Content overlapping status bar.**
- **Custom background color in compact/minimal/expanded.** Can't be done and shouldn't be tried.
- **Alert for every minor update.** Users mute or turn it off.
- **No minimal design** — your multi-activity experience collapses.
- **Rearranging layout between presentations.** Maintain relative positions; expand content.
- **Replicating notification banners in the Expanded view.** That's the Lock Screen presentation.
- **Ignoring the Always On dim state** — motion that doesn't degrade gracefully.
- **Not shipping a Live Activity at all** for long-running trackable tasks — you lose a prime engagement surface.

## SwiftUI

```swift
import ActivityKit
import SwiftUI
import WidgetKit

struct RideAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        var eta: Date
        var driverName: String
        var progress: Double
        var phase: Phase
        enum Phase: String, Codable { case matching, enRoute, arrived }
    }
    var car: String            // "Honda Civic - 7RK L42"
}

struct RideActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: RideAttributes.self) { context in
            // Lock Screen presentation.
            LockScreenRideView(context: context)
        } dynamicIsland: { context in
            DynamicIsland {
                // Expanded regions (long-press).
                DynamicIslandExpandedRegion(.leading) {
                    Label {
                        Text(context.state.driverName).font(.caption)
                    } icon: {
                        Image(systemName: "car.fill")
                    }
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text(context.state.eta, style: .timer)
                        .font(.caption.monospacedDigit())
                }
                DynamicIslandExpandedRegion(.bottom) {
                    VStack(spacing: 4) {
                        ProgressView(value: context.state.progress).tint(.yellow)
                        Text(context.state.phase.label)
                            .font(.caption2).foregroundStyle(.secondary)
                    }
                }
            } compactLeading: {
                Image(systemName: "car.fill").foregroundStyle(.yellow)
            } compactTrailing: {
                Text(context.state.eta, style: .relative).monospacedDigit()
            } minimal: {
                Image(systemName: "car.fill").foregroundStyle(.yellow)
            }
            .keylineTint(.yellow)
            .widgetURL(URL(string: "myride://trip/current")!)
        }
    }
}

extension RideAttributes.ContentState.Phase {
    var label: String {
        switch self {
        case .matching: "Matching a driver"
        case .enRoute:  "Driver en route"
        case .arrived:  "Driver has arrived"
        }
    }
}
```

## See also

- [`./widgets-and-live-activities.md`](./widgets-and-live-activities.md) — the overarching Live Activity design rules.
- [`./lock-screen.md`](./lock-screen.md) — Lock Screen banner is the fallback on non-DI iPhones.
- [`../platforms/ios.md`](../platforms/ios.md) — Dynamic Island's role in iOS.
- [`../foundations/motion.md`](../foundations/motion.md) — 2-second animation budget.
- [`../foundations/accessibility.md`](../foundations/accessibility.md) — VoiceOver labels on Island elements.
