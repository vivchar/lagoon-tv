# Workouts

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/workouts
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · watchOS (primary)
**Not supported:** macOS · tvOS · visionOS (standalone workouts; TV apps can facilitate follow-along but the tracking device is still watch/phone)

A workout app does one job: **help the user stay engaged with their workout and track their progress**. On Apple Watch, users are in motion, looking briefly, often sweaty — design accordingly. On iPhone / iPad, users may be carrying the device; on iPad/Mac/TV, they might be following a session while the device sits stationary.

## Rules — watchOS active session

### The active session is special

During an active workout, watchOS **keeps your app frontmost even between wrist raises**. You get an exceptional amount of screen time. Use it:

- **Show the metrics the user cares about.** Elapsed time, remaining time, calories, distance, pace, heart rate, splits.
- **Update values in real time.** That's how users know the session is live.
- **Relevant controls:** lap / interval marker, pause, resume, stop.
- **No distractions.** No lists of workouts, no settings, no social features during active session. They're for before and after.

### Visual distinction of an active session

Users glancing should know "I'm in a workout" vs "I just opened the app".

- **Unique layout** for the metrics page. Large numbers, specific colors, possibly full-screen.
- **Real-time updating values** reinforce the active state.
- **Different color or background** from the rest of the app's UI.

### Controls

- **Easy to tap.** Large buttons. Users might be sweaty, jostling, wearing gloves.
- **Clear feedback on start / stop / pause.** Haptic + visual.
- **Pause / resume accessible** in one tap.
- **Stop / save** in one tap → confirmation → summary.

### Summary screen

When the user ends the session, show a summary:

- **Key metrics** — duration, calories, distance, average pace/HR.
- **Activity Rings** so users check progress toward daily goals. See [`../components/progress-and-activity.md`](../components/progress-and-activity.md#activity-rings) for the strict rules.
- **Save or discard option.**

### Sensor caveats

Some sensors don't work in some conditions. Explain when data is unavailable.

- **Water blocks heart rate.** Swimming workouts can't show HR.
- **GPS unreliable indoors.** Indoor workouts shouldn't show distance from GPS.
- **Use language similar to Apple's Workout app** for these explanations. Trust the reference.

### Discard brief sessions

A workout that ends 4 seconds after it started is noise. Either:

- **Auto-discard** brief sessions.
- **Ask the user** if they want to save it.

### Motion-legible text

Users are glancing in motion.

- **Large numbers** — the system text styles go big for a reason.
- **High contrast.**
- **Most important info prominent.** Time or remaining time at the top usually.
- **Minimize text; maximize numerals.**

## Rules — iOS / iPadOS

### During a session

If the user carries the phone during a workout (running, wheelchair pushing, walking):

- **Large text, high contrast, motion-legible** — same rule as watchOS.
- **Persistent controls** — pause / resume / stop visible.
- **Live Activity** so the user can see progress on Lock Screen without opening the app. See [`../technologies/widgets-and-live-activities.md`](../technologies/widgets-and-live-activities.md).
- **Dynamic Island** if iPhone 14 Pro+ and the activity warrants.

### Stationary device (iPad following a session)

Different design. The user isn't carrying the device — they're watching a trainer.

- **Full-screen video** of the trainer.
- **Metrics overlay** (heart rate, reps, elapsed) compact and unobtrusive.
- **Scrubber bar** if the session is recorded.
- **Pause / resume** reachable but not prominent.

## Activity Rings

Review [`../components/progress-and-activity.md`](../components/progress-and-activity.md#activity-rings). Strict Apple rules — use only for Move / Exercise / Stand; don't recolor; always on black background.

## HealthKit

Most workout apps write to and/or read from HealthKit.

- **Get permission early** in onboarding, with clear benefit ("See your heart rate during workouts, save history to Health").
- **Minimum necessary types.** Don't request types you don't use.
- **Privacy disclosure** clearly — what's written, what's read.

## Notifications

Post-workout notifications belong only when the user cares.

- **Workout complete notification** if the user backgrounded the app mid-session and it finished on its own (auto-pause detecting zero motion).
- **Daily / weekly summary** if the user opted in.
- **No promotional notifications** during an active session. Ever.

## Common mistakes

- Tab bar / settings visible during active session — invites distraction.
- Small text during active session (user can't read in motion).
- Missing pause control during active session.
- No summary screen at the end.
- Auto-saving a 3-second "workout" as a real session.
- Heart rate shown as "--" during swimming without explanation.
- Activity Rings recolored for branding (forbidden).
- No Live Activity for an iPhone-in-armband scenario.
- Requesting unnecessary HealthKit types.
- Promotional content in the summary screen.
- Complex multi-step flow before the workout can start ("pick music, pick playlist, pick intensity, pick profile" — users hate it).
- watchOS workout app that shows a list of workouts *during* the active session.

## SwiftUI — watchOS active session layout

```swift
import SwiftUI
import HealthKit

struct ActiveWorkoutView: View {
    @Environment(WorkoutSession.self) private var session

    var body: some View {
        VStack(spacing: 6) {
            Text(session.elapsed.formatted(.time(pattern: .minuteSecond)))
                .font(.system(size: 56, weight: .semibold, design: .rounded))
                .monospacedDigit()
                .foregroundStyle(.yellow)

            Grid(horizontalSpacing: 12, verticalSpacing: 6) {
                GridRow {
                    MetricLabel(value: "\(session.heartRate)", unit: "BPM", color: .red)
                    MetricLabel(value: session.distance.formatted(.number.precision(.fractionLength(2))),
                                unit: "MI", color: .green)
                }
                GridRow {
                    MetricLabel(value: "\(session.caloriesBurned)", unit: "CAL", color: .pink)
                    MetricLabel(value: session.pace.formatted(.time(pattern: .minuteSecond)),
                                unit: "/MI", color: .cyan)
                }
            }
        }
        .scenePadding()
    }
}

struct MetricLabel: View {
    let value: String
    let unit: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(value).font(.title3.weight(.semibold)).monospacedDigit()
            Text(unit).font(.caption2).foregroundStyle(color)
        }
    }
}

// Pause / resume / stop controls.
struct SessionControls: View {
    @Environment(WorkoutSession.self) private var session

    var body: some View {
        HStack(spacing: 12) {
            Button {
                session.togglePause()
            } label: {
                Image(systemName: session.isPaused ? "play.fill" : "pause.fill")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(session.isPaused ? .green : .orange)

            Button {
                session.end()
            } label: {
                Image(systemName: "stop.fill")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(.red)
        }
        .sensoryFeedback(.stop, trigger: session.isPaused)
    }
}
```

## See also

- [`../platforms/watchos.md`](../platforms/watchos.md) — watchOS glanceable design, Digital Crown.
- [`../components/progress-and-activity.md`](../components/progress-and-activity.md#activity-rings) — strict Activity Rings rules.
- [`../technologies/widgets-and-live-activities.md`](../technologies/widgets-and-live-activities.md) — Live Activity for iPhone workouts.
- [`./feedback.md`](./feedback.md) — haptics on pause/start/stop.
- [`../foundations/motion.md`](../foundations/motion.md) — minimal motion during sweaty sessions.
