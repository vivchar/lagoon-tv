# Motion

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/motion
**Last verified:** 2026-04-20
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

System components animate themselves. You're only authoring motion when the system doesn't — and when you do, it's brief, purposeful, and respects Reduce Motion.

## Rules

- **Add motion purposefully.** Every animation communicates something (status change, feedback, spatial relationship). Gratuitous motion is noise.
- **Make motion optional.** Never use motion as the only way to communicate critical information. Pair visual motion with haptics or audio alternatives.
- **Respect Reduce Motion.** When on: tighten springs, swap transitions for cross-fades, avoid z-axis animation, don't animate into/out of blurs, pause auto-play. (See [`accessibility.md`](./accessibility.md#reduce-motion).)
- **Brief and precise over prominent.** Short, tied-to-action animations communicate faster than showy ones.
- **Avoid motion on frequent interactions.** Standard controls already animate subtly; piling on custom motion for routine interactions creates fatigue.
- **Let people cancel animations.** Don't force users to wait through a long animation, especially if it plays repeatedly.

## Providing feedback

- **Match the gesture.** If someone drags a view down, they expect to dismiss by dragging down — not by tapping, and not by sliding sideways. Feedback motion follows the user's physical intent.
- **Symbol animations are efficient feedback.** SF Symbols 5+ ship with Bounce, Scale, Pulse, Replace, Variable Color, Wiggle, Breathe, Rotate, Draw On/Off. See [`sf-symbols.md`](./sf-symbols.md#animations).

## Durations and easing (defaults)

HIG doesn't prescribe explicit millisecond values for generic motion. Defaults that work with SwiftUI / UIKit / AppKit out of the box:

- Transition between screens: system standard (ignore unless you have a reason).
- Button press feedback: ~150–200ms.
- Modal presentation: ~300–350ms.
- Haptic + visual "success" flash: ~400ms total.
- Continuous animations (pulsing recording indicator): 1–2s period.

Use the system spring (`.spring`, `.snappy`, `.bouncy`, `.smooth` in SwiftUI) rather than custom curves — they feel consistent with the platform.

## Games

- **30–60 fps** for smooth visuals; keep a stable frame rate per platform.
- Pick defaults that look great on every supported device so players don't have to change settings to enjoy the game.
- Let people switch to a lower-fidelity mode to save battery or when external power is absent.

## Platform notes

### iOS / iPadOS / macOS / tvOS
No additional considerations beyond the rules above.

### visionOS (extra care)

Motion in visionOS is extra-weighted because the user's whole visual field is involved. Getting this wrong causes discomfort.

- **Avoid motion in peripheral vision.** It's distracting and can trigger the sense that the user is moving. If you must, make brightness match the rest of the visible content.
- **Large objects moving.** If a virtual object fills enough of the field of view that people perceive it as part of their surroundings, either (a) increase its translucency so they see through it, or (b) lower its contrast so the motion is less noticeable. Even self-initiated movement of a big object can be uncomfortable — keep the window small when possible.
- **Relocating objects?** Fade out, move, fade in — don't translate across the view.
- **Never rotate the virtual world.** Even subtle self-controlled rotation unsettles people. Use instantaneous directional changes during a quick fade-out instead.
- **Give a stationary frame of reference.** If the surrounding area moves (e.g., a game that auto-travels the player through space), people feel ill.
- **Avoid sustained oscillations around 0.2 Hz.** People are especially sensitive at this frequency.

### watchOS

SwiftUI provides all the motion primitives you need. Layout and appearance animations have built-in easing at start and end — you can't turn it off or customize.

## Common mistakes

- Animating every interaction, every time.
- Running a fullscreen transition animation on every navigation in a frequent flow.
- Animations that ignore Reduce Motion.
- Relying on motion alone to signal state change.
- Slow transitions (>500ms) in navigation paths people traverse constantly.
- In visionOS: large objects moving quickly across the field of view; content oscillating at ~0.2Hz; virtual world rotation.
- Ignoring auto-play restrictions (Dim Flashing Lights, etc.).

## SwiftUI

```swift
@Environment(\.accessibilityReduceMotion) private var reduceMotion

// System spring, disabled under Reduce Motion.
withAnimation(reduceMotion ? nil : .snappy) {
    isExpanded.toggle()
}

// A brief success flash.
Image(systemName: didSave ? "checkmark.circle.fill" : "tray.and.arrow.down.fill")
    .symbolEffect(.bounce, value: didSave)                 // auto-disabled by system under Reduce Motion
    .contentTransition(.symbolEffect(.replace))

// Replace a cross-fade for a slide when Reduce Motion is on.
content
    .transition(reduceMotion ? .opacity : .move(edge: .bottom).combined(with: .opacity))

// Cancellable animation — drive from interactive state.
.offset(y: dragAmount)
.gesture(
    DragGesture()
        .onChanged { dragAmount = $0.translation.height }
        .onEnded { _ in
            withAnimation(reduceMotion ? nil : .spring) { dragAmount = 0 }
        }
)
```

## UIKit

```swift
let reduced = UIAccessibility.isReduceMotionEnabled

if reduced {
    UIView.performWithoutAnimation {
        container.layoutIfNeeded()
    }
} else {
    UIView.animate(withDuration: 0.3,
                   delay: 0,
                   usingSpringWithDamping: 0.8,
                   initialSpringVelocity: 0,
                   options: [.allowUserInteraction, .beginFromCurrentState]) {
        container.layoutIfNeeded()
    }
}
```

## AppKit

```swift
let reduced = NSWorkspace.shared.accessibilityDisplayShouldReduceMotion

NSAnimationContext.runAnimationGroup { ctx in
    ctx.duration = reduced ? 0 : 0.25
    ctx.timingFunction = CAMediaTimingFunction(name: .easeOut)
    panel.animator().alphaValue = 1
}
```

## See also

- [`accessibility.md`](./accessibility.md) — Reduce Motion detailed alternatives.
- [`sf-symbols.md`](./sf-symbols.md) — symbol animations are usually the right answer.
- [`../platforms/visionos.md`](../platforms/visionos.md) — comfort-first motion rules.
- [`../patterns/feedback.md`](../patterns/feedback.md) — pairing motion with haptics/audio.
