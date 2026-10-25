# Accessibility

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/accessibility
**Last verified:** 2026-04-20
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

Accessibility is not a feature you add at the end — it is the floor every screen in your app stands on. These are the rules you treat as non-negotiable. For related foundations see [`typography.md`](./typography.md), [`color.md`](./color.md), [`dark-mode.md`](./dark-mode.md), [`motion.md`](./motion.md); for VoiceOver specifics see [`../technologies/voiceover.md`](../technologies/voiceover.md) (pending).

## Core principles

An accessible interface is:

- **Intuitive.** Familiar, consistent interactions; never reinvent a gesture.
- **Perceivable.** No single method carries critical information. If you say it with color, also say it with an icon or label. If you say it with sound, also say it visually.
- **Adaptable.** Responds to system settings and user preferences without complaint.

Audit every screen with Accessibility Inspector in Xcode. Ship Accessibility Nutrition Labels in App Store Connect when the app is ready.

## Sizing and hit targets

Meet or exceed these minimums on every interactive element. Prefer the default.

| Platform | Default control size | Absolute minimum |
|---|---|---|
| iOS / iPadOS | 44×44pt | 28×28pt |
| macOS | 28×28pt | 20×20pt |
| tvOS | 66×66pt | 56×56pt |
| visionOS | 60×60pt | 28×28pt |
| watchOS | 44×44pt | 28×28pt |

Spacing matters as much as size. Put **~12pt** of padding around bezeled elements and **~24pt** around bezel-less ones. Two tap targets that each meet 44pt but sit flush against each other still mis-fire.

## Dynamic Type — sizes and defaults

Dynamic Type is available on iOS, iPadOS, tvOS, visionOS, and watchOS. macOS does not support Dynamic Type — use dynamic font variants instead (see [`typography.md`](./typography.md)).

Default and minimum sizes for body text:

| Platform | Default | Minimum |
|---|---|---|
| iOS / iPadOS | 17pt | 11pt |
| macOS | 13pt | 10pt |
| tvOS | 29pt | 23pt |
| visionOS | 17pt | 12pt |
| watchOS | 16pt | 12pt |

Support text enlargement of **at least 200%** (140% on watchOS). Test at the largest accessibility sizes (AX1–AX5) and Larger Accessibility Text. Primary content stays prioritized — not every label needs to grow.

## Color contrast

Use these WCAG AA minimums as a floor:

| Text size | Text weight | Minimum contrast |
|---|---|---|
| Up to 17pt | Any | 4.5:1 |
| 18pt+ | Any | 3:1 |
| Any | Bold | 3:1 |

Provide a higher-contrast scheme when **Increase Contrast** is on. Verify in both light and dark appearances. Prefer system-defined colors — they ship with accessible variants. Do not use color alone to convey meaning; add shape, icon, or label.

## VoiceOver

- Every non-decorative view has a meaningful accessibility label.
- Actions that are non-obvious get an accessibility hint.
- Decorative images are hidden from VoiceOver.
- Combine tightly related child elements into one when the group reads better as a single item.
- Set logical reading order with sort priority if default order is wrong.

```swift
Button(action: save) {
    Image(systemName: "tray.and.arrow.down.fill")
}
.accessibilityLabel("Save draft")
.accessibilityHint("Saves to your device without uploading")

Image("decorative-gradient")
    .accessibilityHidden(true)

HStack {
    Image(systemName: "person.circle.fill")
    VStack(alignment: .leading) {
        Text("Ada Lovelace")
        Text("Engineer")
    }
}
.accessibilityElement(children: .combine)
```

## Reduce Motion

Respect `@Environment(\.accessibilityReduceMotion)` / `UIAccessibility.isReduceMotionEnabled`. When on:

- Tighten or remove animation springs.
- Track animations directly with gestures (already true if you chain to user input).
- Avoid z-axis (depth) animation.
- Replace x/y/z transitions with cross-fades.
- Avoid animating into or out of blurs.
- Pause auto-playing content.

```swift
@Environment(\.accessibilityReduceMotion) private var reduceMotion

.animation(reduceMotion ? nil : .spring(), value: isPresented)
```

## Mobility and input

- Keep gestures simple. Avoid multi-finger / multi-hand custom gestures.
- Every swipe-only action has a tappable alternative.
- Support **Voice Control** by giving every interactive element a visible text label (or matching accessibility label) so people can say "Tap Save".
- Support **Full Keyboard Access** on iOS and full keyboard navigation on iPadOS/macOS. Do not override system shortcuts.
- Support **Switch Control**.
- Support **AssistiveTouch** and **Pointer Control**.

## Hearing

- Captions, subtitles, audio descriptions, and transcripts for any app that plays media.
- Pair audio cues with haptics on iOS/iPadOS (`UIFeedbackGenerator` or SwiftUI `.sensoryFeedback`) and visual cues.
- Never rely on an alert sound alone.

```swift
Button("Save", action: save)
    .sensoryFeedback(.success, trigger: didSave)
```

## Cognitive

- Keep actions simple and intuitive.
- Avoid time-limited views that auto-dismiss. Prefer explicit dismissal.
- Let people control audio/video playback; don't auto-play without controls.
- Respect **Dim Flashing Lights**.
- Be cautious with fast-moving and blinking animations — they can trigger epileptic episodes.
- Optimize for **Assistive Access** on iOS/iPadOS: identify core flow, reduce steps, confirm destructive actions twice.

## Platform notes

### iOS, iPadOS, macOS, tvOS, watchOS
No additional considerations beyond the rules above.

### visionOS

- Prioritize comfort. The immersive context amplifies motion sickness and eye strain.
- Keep interactive elements in a comfortable field of view. Prefer horizontal layouts.
- Reduce animation speed and intensity, especially in peripheral vision.
- Don't anchor UI to the head — people can't escape it and assistive tech breaks.
- Minimize large or repetitive gestures.
- Support **Pointer Control** (head and hand) and Zoom.

## Common mistakes

- Icon-only buttons without `.accessibilityLabel`.
- Hardcoded tap target smaller than platform minimum.
- Using color alone to indicate state (error, selected, enabled).
- Animations that ignore Reduce Motion.
- Body text at a fixed point size instead of a text style.
- Critical info carried only by sound.
- Custom font without Dynamic Type + Bold Text behaviors.
- Auto-dismissing banners for critical messages.

## SwiftUI — the minimum accessibility floor

```swift
struct ActionRow: View {
    let title: String
    let hint: String
    let systemImage: String
    let action: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: systemImage)
                    .imageScale(.large)
                    .accessibilityHidden(true)
                Text(title)
                    .font(.body)
                Spacer()
            }
            .frame(minHeight: 44)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title)
        .accessibilityHint(hint)
        .accessibilityAddTraits(.isButton)
        .animation(reduceMotion ? nil : .snappy, value: title)
    }
}
```

## UIKit — the minimum accessibility floor

```swift
let button = UIButton(type: .system)
button.setImage(UIImage(systemName: "tray.and.arrow.down.fill"), for: .normal)
button.translatesAutoresizingMaskIntoConstraints = false
button.widthAnchor.constraint(greaterThanOrEqualToConstant: 44).isActive = true
button.heightAnchor.constraint(greaterThanOrEqualToConstant: 44).isActive = true
button.accessibilityLabel = "Save draft"
button.accessibilityHint = "Saves to your device without uploading"

if UIAccessibility.isReduceMotionEnabled {
    UIView.performWithoutAnimation { container.layoutIfNeeded() }
}
```

## AppKit — the minimum accessibility floor

```swift
let button = NSButton(title: "Save", target: self, action: #selector(save))
button.setAccessibilityLabel("Save draft")
button.setAccessibilityRole(.button)

if NSWorkspace.shared.accessibilityDisplayShouldReduceMotion {
    // Skip decorative animations.
}
```

## See also

- [`typography.md`](./typography.md) — Dynamic Type, SF Pro/Compact/New York.
- [`color.md`](./color.md) — semantic colors and Increase Contrast variants.
- [`dark-mode.md`](./dark-mode.md) — contrast checks in dark appearance.
- [`motion.md`](./motion.md) — Reduce Motion alternatives.
- [`../../assets/checklists/accessibility-review.md`](../../assets/checklists/accessibility-review.md) — pre-ship checklist.
