# Widgets and Live Activities

**HIG sources:**
- https://developer.apple.com/design/human-interface-guidelines/widgets
- https://developer.apple.com/design/human-interface-guidelines/live-activities
- https://developer.apple.com/design/human-interface-guidelines/always-on

**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · watchOS · visionOS (widgets); iOS · iPadOS · Mac menu bar · watchOS · CarPlay (Live Activities)
**Not supported:** tvOS

Widgets are **persistent glanceable surfaces** outside your app (Home Screen, Lock Screen, desktop, Smart Stack, Vision Pro surfaces). Live Activities are **short-lived real-time tracking** for ongoing tasks (delivery, ride, game, workout). Both are your highest-leverage non-app surfaces — many users will engage with your app primarily through these.

## Widgets

### Anatomy

Two families:

- **System family** — Small, Medium, Large, Extra Large (iPad/Mac/Vision Pro), Extra Large Portrait (Vision Pro only).
- **Accessory** — Circular, Corner, Inline, Rectangular. For Lock Screen widgets (iPhone/iPad), watch complications, and Smart Stack entries.

### Size support per device

| Size | iPhone | iPad | Mac | Vision Pro | Apple Watch |
|---|---|---|---|---|---|
| System small | Home, Today View, StandBy, CarPlay | Home, Today View, Lock Screen | Desktop, Notif. Center | Surfaces | — |
| System medium | Home, Today View | Home, Today View | Desktop, Notif. Center | Surfaces | — |
| System large | Home, Today View | Home, Today View | Desktop, Notif. Center | Surfaces | — |
| System extra large | — | Home, Today View | Desktop, Notif. Center | Surfaces | — |
| Accessory circular | Lock Screen | Lock Screen | — | — | Complications, Smart Stack |
| Accessory rectangular | Lock Screen | Lock Screen | — | — | Complications, Smart Stack |
| Accessory inline | Lock Screen | Lock Screen | — | — | Complications |

### Rendering modes

- **Full-color** — Home Screen, Today view, desktop, Vision Pro surfaces.
- **Accented** — iPhone/iPad Home Screen + Today view tinted variants; Apple Watch complications. System splits your views into accent + primary groups; tints them separately.
- **Vibrant** — Lock Screen widgets, StandBy in low light. The system desaturates and applies the Lock Screen's vibrancy to the widget.

Use opaque grayscale values (not opacity) in vibrant mode — brightness determines vibrancy strength.

### Rules

- **Pick a simple idea** that relates to your app's main purpose.
- **Dynamic content.** A widget that never changes gets removed.
- **Glanceable.** Sparse layouts risk feeling useless; overly dense risks being unreadable. Balance.
- **Direct content, not app-icon duplication.** A widget that launches your Home Screen adds no value.
- **Multiple sizes only when they add value.** Don't stretch a small widget's content to fill a large one.
- **Deep-link to the right place.** Tapping a widget opens the app at the widget's context, not the generic root.
- **Interactive widgets (iOS 17+)** — buttons and toggles. Limit complexity; keep tap targets confident; reserve app-like layouts for the app itself.
- **Respect Dynamic Type** (Large → AX5). Use text styles, not fixed sizes.
- **Minimum text ~11pt.**
- **16pt standard margins** (11pt for tighter groupings).
- **Coordinate corner radius** with the widget shape (`ContainerRelativeShape` in SwiftUI).
- **Avoid rasterized text** — breaks scaling and VoiceOver.
- **Placeholder content** via redacted views.
- **Widget description** starts with a verb ("See the current weather and forecast"). Under 200 chars.

### Don't

- Full-color images in tinted/clear appearance (looks off-platform). Reserve for media art.
- Display sensitive info on the Lock Screen.
- Duplicate the widget appearance in-app (confuses users).
- Mirror Activity ring visuals in your widget — see [`../components/progress-and-activity.md`](../components/progress-and-activity.md#activity-rings).

### SwiftUI

```swift
struct TodayWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "today", provider: TodayProvider()) { entry in
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Image(systemName: "sun.max.fill").foregroundStyle(.yellow)
                    Text("\(entry.tempF)°")
                        .font(.system(.title, design: .rounded, weight: .semibold))
                }
                Text(entry.location).font(.caption).foregroundStyle(.secondary)
                Spacer(minLength: 0)
                Text("H \(entry.highF)° · L \(entry.lowF)°")
                    .font(.caption2).foregroundStyle(.tertiary)
            }
            .containerBackground(.fill.tertiary, for: .widget)   // renders correctly on all appearances
            .widgetAccentable()                                   // opt into tinted rendering
        }
        .supportedFamilies([.systemSmall, .systemMedium,
                            .accessoryCircular, .accessoryRectangular, .accessoryInline])
        .configurationDisplayName("Today")
        .description("See current conditions and today's high and low.")
    }
}
```

## Live Activities

A Live Activity tracks an **ongoing task** (delivery, ride, score, workout, timer) with **frequent updates over a few hours**. Not for general notifications.

### Locations

| Platform | Where it appears |
|---|---|
| iPhone | Lock Screen, Home Screen banner, Dynamic Island, StandBy |
| iPad | Lock Screen, Home Screen banner |
| Mac | Menu bar |
| Apple Watch | Smart Stack |
| CarPlay | Dashboard |

### Four presentations

- **Compact** — leading + trailing elements beside the Dynamic Island. Single active activity.
- **Minimal** — tiny circular/oval; multiple activities competing.
- **Expanded** — long-press the compact/minimal; wraps around the TrueDepth camera.
- **Lock Screen** — banner at the bottom of the Lock Screen; also used for Home Screen overlays.

All four must be designed; the system picks.

See [`./dynamic-island.md`](./dynamic-island.md) for Dynamic Island specifics.

### Rules

- **Defined beginning and end.** 8 hours maximum.
- **No ads or promotions.** Users expect relevant information.
- **No sensitive info visible.** Lock Screen is public; StandBy on a desk; Always On display. Redact or gate behind a tap into the app.
- **Tight wrapping around the TrueDepth camera.** Don't leave empty space, don't poke into camera territory.
- **Concentric margins.** Rounded elements mirror the outer corner radius of the Live Activity.
- **Dynamic height on Lock Screen / Expanded** — grow as content grows; shrink when there's less.
- **Custom background color only on Lock Screen presentation.** Compact/minimal/expanded use the system's black.
- **Key line color** — match text/icon color for cohesion.
- **Alerts light up the screen** and play a sound. Reserve for essential updates.
- **End immediately when the task ends.** Use a custom dismissal time (15–30 min typically) for summary visibility; system default is 4 hours on non-DI surfaces.
- **Don't duplicate push notifications** for the same updates.

### Animations

- **Max 2 second duration.**
- **System does extend/contract transitions.**
- **Preserve existing elements during layout changes** — animate position, not remove-and-replace.
- **Use numeric content transitions** for score changes.
- **No animations on Always On reduced luminance.**

### Interactivity

- **Tap opens app at the right deep link.**
- **Buttons/toggles for direct, simple actions** (pause music, stop workout, contact driver).
- **Prefer a single interactive element** to avoid mis-taps.

### SwiftUI

```swift
struct DeliveryActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: DeliveryAttributes.self) { context in
            // Lock Screen / Home Screen banner
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Image(systemName: "bag.fill")
                    Text(context.attributes.restaurant).font(.headline)
                    Spacer()
                    Text(context.state.eta, style: .relative)
                        .font(.callout.monospacedDigit())
                }
                ProgressView(value: context.state.progress)
                    .tint(.orange)
            }
            .padding()
            .activityBackgroundTint(.black)
            .activitySystemActionForegroundColor(.white)
        } dynamicIsland: { context in
            // Dynamic Island
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Image(systemName: "bag.fill")
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text(context.state.eta, style: .relative)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    ProgressView(value: context.state.progress).tint(.orange)
                }
            } compactLeading: {
                Image(systemName: "bag.fill")
            } compactTrailing: {
                Text(context.state.eta, style: .relative).monospacedDigit()
            } minimal: {
                Image(systemName: "bag.fill")
            }
            .keylineTint(.orange)
        }
    }
}
```

## Always On

On iPhone 14 Pro+ and Apple Watch, the display stays active at low luminance when the user puts the device down / drops the wrist. Your Live Activity / watchOS app view persists.

### Rules

- **Redact sensitive info.** Bank balance, health details, notifications.
- **Keep glanceable content visible** when it's useful (pace, heart rate, flight arrival).
- **Dim nonessential content.** Remove heavy images, use dim colors.
- **Consistent layout.** Don't reshuffle when Always On begins/ends.
- **Smooth motion transitions to rest.** Don't stop animations instantly.
- **Infrequent updates** during Always On — score changes yes, play-by-play no.

## Common mistakes

- Widget that just opens the app, no real content.
- Dense widget with unreadable small text.
- Full-color imagery ruining tinted Lock Screen aesthetic.
- Widget with no placeholder content (blank for 2 seconds).
- Duplicate of widget appearance in-app — confusing.
- Live Activity that lasts > 8 hours.
- Live Activity with ads or upsells.
- Sensitive info visible on Lock Screen or StandBy.
- Live Activity not ending when the task ends.
- Push notification duplicating a Live Activity update.
- Animations > 2s in a Live Activity.
- Always On view with abrupt layout changes.

## See also

- [`./dynamic-island.md`](./dynamic-island.md) — compact / minimal / expanded specifics.
- [`./lock-screen.md`](./lock-screen.md) — accessory widget rules in Lock Screen context.
- [`../platforms/ios.md`](../platforms/ios.md) — Dynamic Island integration.
- [`../platforms/watchos.md`](../platforms/watchos.md) — Smart Stack, complications.
- [`../components/progress-and-activity.md`](../components/progress-and-activity.md#activity-rings) — Activity Rings rules (don't confuse with widgets).
- [`../foundations/typography.md`](../foundations/typography.md) — Dynamic Type in widgets.
- [`../foundations/color.md`](../foundations/color.md) — semantic colors in widget appearances.
