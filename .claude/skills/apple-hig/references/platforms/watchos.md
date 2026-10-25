# watchOS

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/designing-for-watchos
**Last verified:** 2026-04-21
**Applies to:** Apple Watch (watchOS)

A glance. Ten seconds. One wrist. Apple Watch is not a small iPhone — it's a different device with different goals. If your user opens your watchOS app more than about a minute at a time, you probably designed the app wrong; the complications, notifications, and Smart Stack entries are likely more important than the app.

## Table of contents

- [What makes watchOS watchOS](#what-makes-watchos-watchos)
- [Device traits](#device-traits)
- [The watchOS app is one of four surfaces](#the-watchos-app-is-one-of-four-surfaces)
- [Navigation](#navigation)
- [Input](#input)
- [Layout](#layout)
- [Typography](#typography)
- [Color and materials](#color-and-materials)
- [Feedback](#feedback)
- [Always On](#always-on)
- [Notifications](#notifications)
- [Common mistakes](#common-mistakes)
- [SwiftUI — a complete watchOS app](#swiftui--a-complete-watchos-app)
- [See also](#see-also)

## What makes watchOS watchOS

- **Glanceable.** Every screen designed for a sub-minute read.
- **Minimal hierarchy.** Deep nav trees don't survive a 10-second interaction. One level deep is ideal; two is acceptable; three means you're doing something wrong.
- **Digital Crown does scrolling and value tuning.** Design content that benefits from crown interaction (lists, pickers, values).
- **Complications + notifications + Smart Stack matter more than the app itself** for most users.
- **Your app should stand alone** — but it complements, not replaces, the related surfaces.
- **Always On display** means content must be legible at half brightness and low refresh.

## Device traits

- **Display.** Small, high-res. Current sizes: 41/45mm (Series 7–9), 40/44mm (earlier), 42/46mm (Series 10/11), 49mm (Ultra). Design for the smallest supported target.
- **Ergonomics.** Wrist-raise to view; opposite-hand finger to interact. One-handed operation is possible but secondary.
- **Inputs.** Digital Crown, Action button (Ultra), side button, tap/swipe/drag, double-tap gesture (Series 9+ / Ultra 2+), voice (Siri), hardware sensors (GPS, heart rate, blood oxygen, altimeter, accelerometer, gyroscope, temperature, ECG).
- **Session length.** Typically 5–30 seconds per interaction. Longer only for workouts, meditation, audio playback.
- **System features:** Complications, notifications, Smart Stack, Focus filters, Control Center, Now Playing, Workouts, Siri, Shortcuts, HealthKit, HomeKit, Maps.

## The watchOS app is one of four surfaces

When you design for watchOS, you're designing four things together:

1. **Complications** — right on the watch face, visible on every wrist-raise. Your highest-value surface.
2. **Smart Stack widgets** — timely cards the user scrolls through from the watch face.
3. **Notifications** — short, actionable, often all the user needs.
4. **The app** — for when the glance and the notification aren't enough.

Plan the four together. If the app has 12 screens but the complication does the top job in one tap, the complication is your product.

## Navigation

One paradigm per app. Pick based on content shape.

- **Vertical page navigation (`TabView` with `.verticalPage`).** The user rotates the Digital Crown to swipe between full-screen pages. Ideal when each page is a distinct context (Today / Goals / Trends). The system shows a page indicator next to the Crown.
- **Hierarchical (`NavigationStack`).** Drill-in for a list → detail pattern. Max two levels deep.
- **Don't use a tab bar at the bottom.** That's iOS. Vertical pages are the watchOS equivalent.
- **List selection navigates** — the user scrolls, taps, drills in. `NavigationLink` does the right thing.
- **Back navigation** is automatic in a stack (swipe right from the leading edge or tap the top-left chevron).

## Input

### Digital Crown

- **Primary input for scrolling** (vertical and through tabs).
- **Secondary input for value tuning** in pickers, sliders, timers.
- **Precision** — values change smoothly or in discrete steps depending on the binding.
- **Focused scrollable region** only — crown input goes to the focused view. Use `.focusable(true)` and `.digitalCrownRotation(...)` in SwiftUI to receive it in custom views.

### Touch

- **44×44pt minimum** tap target on 44mm+; 40pt on smaller models. The bezel acts as padding, so target sizes don't look tight.
- **Standard gestures:** tap, swipe, drag. Pinch and rotate aren't standard.
- **Long press** typically reveals editing (watch face, Smart Stack).

### Side button

- **Opens Smart Stack** by default; reserved by the system.
- **Double-click** invokes Apple Pay.
- Don't try to override.

### Action button (Apple Watch Ultra)

- **User-configurable hardware shortcut.** Register a Shortcut or Siri intent to be a valid target.
- Good for fitness apps — start a workout, mark a lap, drop a waypoint.

### Double tap (Series 9+ / Ultra 2+)

- **System gesture** that invokes the primary action of the current view. Your app doesn't handle it directly; the system picks the primary action based on SwiftUI semantics. Make sure your primary action is clearly marked.

### Voice and dictation

- **Siri is first-class** on watchOS. Shortcuts integration lets voice drive app actions.
- **Dictation, Scribble, and predefined replies** beat a text field every time. Only show a text field when none of those suffice.

## Layout

- **Edge-to-edge content.** The bezel is the padding. Don't add external margins.
- **Max 2 text buttons side-by-side.** Max 3 glyph buttons. Prefer full-width buttons stacked vertically.
- **Full-width buttons** for primary actions — easier to hit, look at home on watchOS.
- **Same height for button stacks** — inconsistent heights look bad and hurt tap accuracy.
- **Vertical scrolling is the default.** Don't ship horizontal scroll unless you really need it.
- **Digital Crown page indicator** appears next to the Crown when a `TabView` is in vertical-page mode. It expands to show scroll position on tall pages.
- **Autorotation** for views meant to be shown to others — QR codes, boarding passes, images. The watch rotates the content when the wrist flips.

See [`../foundations/layout.md`](../foundations/layout.md#watchos) for dimensions.

## Typography

- **SF Compact** is the system font. SF Compact Rounded in complications. NY available.
- **Default body 16pt, minimum 12pt.**
- **Dynamic Type** supported. Test at larger sizes.
- **Don't ship thin or light weights** — they vanish at small sizes.

Detail: [`../foundations/typography.md`](../foundations/typography.md).

## Color and materials

- **Always dark.** No Dark Mode setting — watchOS is always dark.
- **No hex.** Use semantic colors; they adapt for accessibility (increased contrast, reduced transparency).
- **Tinted complications** — users can set a monochrome tint; your complication art must look good as a single user-chosen color.
- **Background color for context.** Match an Activity ring section's background to its ring color. Avoid full-screen colored backgrounds in long-running views (workouts, audio) — they feel heavy over time.
- **Material backgrounds on sheets** provide depth and orientation — don't replace them with solid fills.

Detail: [`../foundations/color.md`](../foundations/color.md#watchos), [`../foundations/materials.md`](../foundations/materials.md#watchos), [`../foundations/dark-mode.md`](../foundations/dark-mode.md).

## Feedback

- **Haptics are the feedback channel.** Pair every significant action with a haptic. watchOS has a rich haptic vocabulary: `.success`, `.failure`, `.retry`, `.start`, `.stop`, `.notification`, `.directionUp`, `.directionDown`, `.click`.
- **Audio** is sparing — watchOS typically runs silent. Use haptics first.
- **Don't show indeterminate progress indicators.** Users glance; a spinner invites them to stare. Say "we'll notify you when it's done" and let them move on.
- **Confirmations** via alerts (small modal with material background) or action sheets (max 4 buttons including Cancel).

## Always On

Apple Watch displays the main watch face and app UI dimly while the wrist is down. When in an app (workouts, timers), Always On shows a low-power rendering of your view.

- **Test at low brightness.** High-contrast, readable text.
- **Reduce visual noise** — minimize animation, dim decorative elements, keep essential numbers prominent.
- **Update at appropriate intervals.** The system throttles Always On refresh; design for infrequent redraw.

## Notifications

- **Your highest-leverage surface.** Many users never open the app; they act on notifications.
- **Short title + short body.** The watch shows maybe 3–6 lines total before tapping to expand.
- **Actionable** — include custom actions where the user can resolve without opening the app ("Mark as read", "Snooze", "Like").
- **Context-rich rich notifications** — custom views for notifications that benefit (game state, workout splits, transit arrivals).

## Common mistakes

- Bottom tab bar (use vertical page `TabView`).
- Three-level navigation hierarchy.
- Spinners for async work (users stare; fail).
- Indeterminate progress bars.
- Tiny text (<12pt) or thin weights.
- Horizontal scroll when vertical would work.
- Solid colored backgrounds in long-running views.
- No complication or Smart Stack surface at all.
- Ignoring the double-tap gesture (the primary action isn't declarative; the system can't infer it).
- Custom gesture reinventing what the Digital Crown does.
- Text fields instead of dictation / Scribble / predefined replies.
- Complications that look awful when the user picks tinted mode.
- Notifications without custom actions.

## SwiftUI — a complete watchOS app

```swift
import SwiftUI
import WatchKit

@main
struct TrailApp: App {
    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}

struct RootView: View {
    var body: some View {
        TabView {
            TodayView()
            GoalsView()
            HistoryView()
        }
        .tabViewStyle(.verticalPage)
    }
}

struct TodayView: View {
    @State private var miles: Double = 2.4
    @State private var isActive = false
    @FocusState private var crownFocus: Bool

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                Text("\(miles, format: .number.precision(.fractionLength(1))) mi")
                    .font(.system(size: 56, weight: .semibold, design: .rounded))
                    .monospacedDigit()
                    .focusable()
                    .focused($crownFocus)
                    .digitalCrownRotation($miles,
                                          from: 0, through: 26.2,
                                          by: 0.1,
                                          sensitivity: .low,
                                          isContinuous: false,
                                          isHapticFeedbackEnabled: true)

                Text("Target")
                    .font(.caption)
                    .foregroundStyle(.secondary)

                Button(isActive ? "Stop Workout" : "Start Workout") {
                    isActive.toggle()
                }
                .buttonStyle(.borderedProminent)
                .tint(isActive ? .red : .green)
                .sensoryFeedback(isActive ? .start : .stop, trigger: isActive)
            }
            .padding()
        }
        .onAppear { crownFocus = true }
        .navigationTitle("Today")
    }
}

struct GoalsView: View {
    var body: some View {
        List {
            Section("This Week") {
                Label("Runs: 3 / 5", systemImage: "figure.run")
                Label("Miles: 12 / 20", systemImage: "ruler")
                Label("Active Days: 4 / 7", systemImage: "calendar")
            }
        }
    }
}

// Complication (WidgetKit for watchOS).
struct TrailComplication: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "TrailComplication",
                            provider: TrailProvider()) { entry in
            HStack(spacing: 4) {
                Image(systemName: "figure.run")
                Text("\(entry.milesToday, format: .number.precision(.fractionLength(1)))")
            }
            .widgetAccentable()            // honors tinted complications
            .containerBackground(.fill.tertiary, for: .widget)
        }
        .supportedFamilies([.accessoryCircular, .accessoryRectangular, .accessoryInline])
        .configurationDisplayName("Today's Miles")
    }
}
```

What this demonstrates: vertical-page navigation, Digital Crown value binding with haptics, sensory feedback, a glanceable list view, and a WidgetKit complication with `widgetAccentable` for tinted mode.

## See also

- [`../foundations/accessibility.md`](../foundations/accessibility.md) — hit targets on small displays.
- [`../foundations/typography.md`](../foundations/typography.md) — SF Compact, minimums.
- [`../components/bars.md`](../components/bars.md) — toolbar buttons on watchOS.
- [`../components/lists-and-tables.md`](../components/lists-and-tables.md#watchos) — elliptical list style, paging constraints.
- [`../components/progress-and-activity.md`](../components/progress-and-activity.md#activity-rings) — Activity rings.
- [`../patterns/loading.md`](../patterns/loading.md#watchos) — no indeterminate indicators.
- [`../inputs/digital-crown.md`](../inputs/digital-crown.md) (pending) — deeper Crown design.
- [`../technologies/widgets-and-live-activities.md`](../technologies/widgets-and-live-activities.md) (pending) — complications + Smart Stack.
- [`../patterns/workouts.md`](../patterns/workouts.md) (pending) — workout session design.
