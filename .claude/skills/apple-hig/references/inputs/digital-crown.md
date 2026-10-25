# Digital Crown

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/digital-crown
**Last verified:** 2026-04-21
**Applies to:** Apple Watch · Apple Vision Pro
**Not supported:** iOS · iPadOS · macOS · tvOS

The Digital Crown is a **system hardware input** on Apple Watch and Apple Vision Pro. It's the most important analog control Apple ships — precise, haptic, and universally at-hand. On watchOS it's central to navigation; on visionOS it controls immersion and volume (apps don't read its value directly).

## Apple Vision Pro

On Vision Pro, the Crown is **system-only**. Apps do **not** receive turn data directly.

System uses:

- **Adjust volume**
- **Adjust immersion** in a portal, Environment, or an app/game running in a Full Space (how much of the real world shows through)
- **Recenter content** — pull windows and immersive content back in front of the user
- **Open Accessibility settings**
- **Exit an app** and return to the Home View

### Design implications

- **Don't try to override** Crown behavior in visionOS. Your app can't.
- **Design your app to be usable at any immersion level** your user picks. Progressive immersion is controlled by the user via the Crown.
- **Test your app with immersion dialed from 0% to 100%.** Content, text contrast, and motion behavior all change as the environment becomes more visible.
- **Recentering is important.** Users recenter via the Crown when your windows drift too far away or behind them. Don't require specific positioning that makes recentering awkward.

## Apple Watch

On watchOS, the Crown is **the primary input** for navigation plus a general-purpose value tuner. Starting with watchOS 10, turning the Crown is **the main way users move through your app**.

### System uses

- **Watch face → Smart Stack** — users turn the Crown to reveal widget cards.
- **Home Screen navigation** — vertical travel through the app grid.
- **Within apps** — scrolling lists, switching vertical tabs, scrubbing through content.
- **Crown presses are system-reserved** (Home Screen, Siri, etc.). Your app doesn't handle Crown presses.

### Core rules

- **Anchor your app's navigation to the Crown.** List, tab, and scroll views should be vertical. Users expect the Crown to scroll the current view.
- **Always back Crown interactions with touch.** Every Crown-driven action has a tap or swipe alternative.
- **Provide visual feedback.** A Crown turn with no UI change reads as "nothing's happening." Picker values update live. Scroll positions move. Sliders slide.
- **Match update speed to Crown velocity.** Fast turns scroll fast; slow turns scroll precisely. Don't auto-throttle in ways that feel disconnected from the user's motion.
- **Use default haptic detents.** The system provides satisfying clicks at meaningful intervals. Turn them off only if they clash with your animation.

### Haptic feedback

Most Apple Watch models provide haptic detents as the Crown turns.

- **Linear detents (default)** fire at fixed rotational distances.
- **Row-based detents in tables** fire as each new row scrolls into view. Use this for uniform-row lists.
- **Disable detents** when they conflict with animation (smooth value transitions, continuous zoom).
- **Switch tables to linear detents** when rows have significantly different heights (row-based detents feel inconsistent when rows aren't uniform).

### Beyond navigation — data inspection

When navigation isn't the point, the Crown is a **precise value tuner**. Examples:

- **World Clock** — advance the time at a selected location to compare against the current time.
- **Workouts** — scrub through splits.
- **Timers / alarms** — tune hours and minutes.
- **Pickers** — pick from lists.
- **Sliders** — continuous value changes.

Use the Crown wherever "give me a precise number" is the user's goal.

## Platform specifics

| | Apple Watch | Apple Vision Pro |
|---|---|---|
| Apps receive Crown rotation | ✓ | ✗ (system only) |
| Press handled | System only | System only |
| Haptic detents | ✓ | Volume feel |
| Primary use | Navigation + value tuning | Immersion + volume + recentering |

## Common mistakes

- **Not supporting Crown** for scrolling in a watchOS list (users reach for it by reflex).
- **Crown-only navigation** without a touch fallback.
- **Over-customizing detents** so the haptic feel doesn't match the user's expectation.
- **Throttling responses** so the Crown feels laggy or disconnected.
- **Trying to read Crown input on visionOS** — you can't.
- **Requiring specific window placement** that makes the user's recenter gesture break your layout.
- **No visual feedback** on a Crown turn.
- **Designing only for full immersion** on visionOS — users dial back mid-session.

## SwiftUI — watchOS

```swift
// Basic Crown-driven value tuning.
@State private var value: Double = 0
@FocusState private var crownFocus: Bool

var body: some View {
    Text("\(value, format: .number.precision(.fractionLength(1)))")
        .font(.system(size: 64, weight: .semibold, design: .rounded))
        .monospacedDigit()
        .focusable()
        .focused($crownFocus)
        .digitalCrownRotation($value,
                              from: 0,
                              through: 100,
                              by: 0.5,
                              sensitivity: .medium,
                              isContinuous: false,
                              isHapticFeedbackEnabled: true)
        .onAppear { crownFocus = true }
}

// List scroll — Crown scrolls automatically, but focus gets it into
// the right view in multi-view layouts.
NavigationStack {
    List(items) { ItemRow(item: $0) }
        .navigationTitle("Today")
}

// Vertical page TabView — Crown pages between full-screen views.
TabView {
    TodayView()
    GoalsView()
    HistoryView()
}
.tabViewStyle(.verticalPage)

// Picker — Crown drives discrete selection with haptics.
Picker("Interval", selection: $interval) {
    ForEach([5, 10, 15, 30, 60], id: \.self) { Text("\($0) min").tag($0) }
}
.pickerStyle(.wheel)                           // Crown-native on watchOS

// Continuous Crown input without detents for smooth scrub (no haptic).
@State private var scrubPosition: Double = 0
WaveformView(position: scrubPosition)
    .focusable()
    .digitalCrownRotation($scrubPosition,
                          from: 0,
                          through: 1,
                          by: 0.001,
                          sensitivity: .high,
                          isContinuous: false,
                          isHapticFeedbackEnabled: false)
```

## WatchKit (legacy UIKit-style)

```swift
// WKInterfaceController with crown delegate.
class TimerController: WKInterfaceController, WKCrownDelegate {
    @IBOutlet weak var picker: WKInterfacePicker!

    override func willActivate() {
        super.willActivate()
        crownSequencer.delegate = self
        crownSequencer.focus()
    }

    func crownDidRotate(_ sequencer: WKCrownSequencer?,
                        rotationalDelta: Double) {
        accumulate(rotationalDelta)
        updateDisplay()
    }
}
```

## See also

- [`./touch-and-gestures.md`](./touch-and-gestures.md) — double-tap gesture alongside the Crown on watchOS.
- [`../platforms/watchos.md`](../platforms/watchos.md) — glanceable design + Crown navigation.
- [`../platforms/visionos.md`](../platforms/visionos.md) — immersion styles controlled by the Crown.
- [`../components/pickers-and-menus.md`](../components/pickers-and-menus.md) — Crown-driven pickers on watchOS.
- [`../foundations/motion.md`](../foundations/motion.md) — immersion changes affect the motion budget.
