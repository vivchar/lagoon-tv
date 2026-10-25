# Touch and gestures

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/gestures
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS (trackpad gestures) · tvOS (clickpad) · visionOS (direct + indirect) · watchOS

Gestures are **physical motions a person uses to directly affect an object**. The standard set (tap, swipe, drag, touch-and-hold, double tap, zoom, rotate) works the same way across platforms with platform-specific extensions. The single biggest gesture mistake is using a familiar gesture for something unique to your app, or inventing a new gesture for a standard action.

## Core rules

- **Provide more than one way to do anything.** Not everyone can use a specific gesture — voice, keyboard, Switch Control, AssistiveTouch all need a path.
- **Familiar gestures mean what the system says they mean.** Tap activates. Swipe from the leading edge goes back. Pinch zooms. Don't redefine these.
- **Respond immediately.** Gestures are direct manipulation. Visual feedback lands as the finger moves, not after.
- **Show when a gesture isn't available.** Nothing is more frustrating than a gesture that silently doesn't work — the user thinks the app is frozen.
- **Don't conflict with system gestures** (edge swipes on iOS, wrist-flip on watchOS, palm-look on visionOS).
- **Hit target minimum** = [`../foundations/accessibility.md`](../foundations/accessibility.md) sizes (44pt iOS/iPadOS/watchOS, 28pt macOS, 60pt visionOS, always-focusable tvOS).

## Standard gestures (cross-platform)

| Gesture | iOS/iPadOS | macOS | tvOS | visionOS | watchOS | Common action |
|---|---|---|---|---|---|---|
| Tap | ✓ | ✓ (click) | ✓ (click) | ✓ | ✓ | Activate / select |
| Swipe | ✓ | ✓ | ✓ | ✓ | ✓ | Reveal / dismiss / scroll |
| Drag | ✓ | ✓ | ✓ | ✓ | ✓ | Move UI element |
| Touch-and-hold | ✓ | — | ✓ | ✓ | ✓ | Reveal controls or context |
| Double tap | ✓ | ✓ | ✓ | ✓ | ✓ (primary action) | Zoom or primary action |
| Pinch (zoom) | ✓ | ✓ | ✓ | ✓ (two-hand) | — | Zoom |
| Rotate | ✓ | ✓ | ✓ | ✓ (two-hand) | — | Rotate selected item |

## iOS / iPadOS extras

| Gesture | Common action |
|---|---|
| Three-finger swipe left | Undo |
| Three-finger swipe right | Redo |
| Three-finger pinch in | Copy selected text |
| Three-finger pinch out | Paste copied text |
| Four-finger swipe (iPad) | Switch between apps |
| Shake | Undo / redo |

- **Simultaneous gestures are rare** in productivity apps; common in games (joystick + fire button). Enable only when they enhance gameplay.
- **Edge swipe from the leading edge** = navigate back. Do not override.
- **Pencil + touch on iPad** — let people choose which one does what (see [`./apple-pencil.md`](./apple-pencil.md)).

## macOS

Most gestures happen on a **Magic Trackpad** or a **Magic Mouse**. A **mouse** supports fewer gestures (click, scroll, primary/secondary click).

- **Respect systemwide gestures** (Mission Control, Exposé, Notification Center, Launchpad, Show Desktop, Swipe Between Pages, Swipe Between Full-Screen Apps).
- **Don't redefine trackpad gestures** — users customize them system-wide.
- **Consistent modifier-key semantics.** Option-drag duplicates. Shift-drag constrains. Same with touch or pointer.

See [`./pointer-and-keyboard.md`](./pointer-and-keyboard.md#macos) for the full table.

## tvOS

The Siri Remote's clickpad supports **swipe** and **press**:

- **Swipe → move focus.** The focus engine is the navigation model.
- **Press → activate** the focused item.
- **Positional taps** (top / bottom / left / right of the clickpad) when it makes sense — discoverable and intuitive only.
- **Avoid responding to inadvertent taps.** A user resting a thumb on the remote shouldn't trigger anything. During live video playback, ignore taps — wait for presses.

Full remote spec: [`./focus-and-remote.md`](./focus-and-remote.md).

## visionOS

Two categories: **indirect** (the default) and **direct**.

### Indirect (default, ergonomically dominant)

Look at an object → make a small hand gesture (pinch) in your lap or at your side → activate. Hands don't need to reach the content.

| Indirect gesture | Action |
|---|---|
| Pinch (tap) | Activate focused item |
| Pinch and hold | Open contextual menu / start drag |
| Pinch and drag | Move or scroll |
| Two-hand pinch + spread/together | Zoom |
| Two-hand pinch + circular | Rotate |

### Direct

The user physically touches content. Works only when content is **within reach** (arm distance).

| Direct gesture | Action |
|---|---|
| Touch | Activate |
| Touch and hold | Context menu |
| Touch and drag | Move |
| Double touch | Preview / word select |
| Swipe | Scroll / reveal / dismiss |
| Two-hand pinch + drag | Zoom |
| Two-hand pinch + circular | Rotate |

### Rules

- **Prefer indirect for UI** (buttons, menus, standard chrome).
- **Reserve direct for close-up interaction** in games or 3D content inspection.
- **Support standard gestures everywhere you can.** Tap is the first thing people try.
- **Don't require specific body movements or positions** — support alternatives.
- **Palm overlay zone is reserved for the system.** Don't anchor critical UI where the palm-look gesture reveals the Home indicator / Control Center.

### Custom visionOS gestures

Allowed only when your app runs in a Full Space and users grant permission to access hand data.

- **Comfort first.** Don't require arms-raised for long periods.
- **Don't force specific-hand gestures.** Left vs right requirements are exclusionary.
- **Simpler is better.** Multi-finger / two-hand complex gestures fatigue.
- **Rolling motion of hand/wrist/forearm is reserved** by the system.

## watchOS

- **Tap, swipe, drag.** Digital Crown does scrolling / value tuning (see [`./digital-crown.md`](./digital-crown.md)).
- **Double tap** (Series 9+, Ultra 2+) performs the **primary action** of the current view. You don't handle it directly; the system picks based on SwiftUI semantics — mark your primary button clearly.
- **Leading edge swipe** = back.
- **Side button, Digital Crown presses, wrist flip** = system-reserved.

## Custom gestures — the test

Add a custom gesture only when it:

- **Isn't covered by existing gestures.**
- **Is discoverable** (users find it without reading a manual).
- **Is easy to perform.**
- **Is distinct** from other gestures.
- **Isn't the only way to perform an important action.**

If you can't describe the gesture in one short sentence, users won't learn it.

## Accessibility

- **Voice Control** — every gesture has a labeled alternative that voice can reach.
- **Switch Control** — all actions reachable in < 5 scanning steps.
- **AssistiveTouch** — custom gestures can be recorded; your app must work with them.
- **Simpler gestures work for more people.** Complex multi-finger patterns exclude people with limited dexterity.

## Common mistakes

- Redefining tap or swipe to do something app-specific.
- Inventing a custom gesture for a standard action (back, activate, zoom).
- Silent gesture failures — UI gives no feedback.
- Swipe-to-delete as the only way to delete (no tappable alternative).
- Overriding the iOS leading-edge back swipe.
- visionOS gestures that require the hand to be close to the screen for long periods.
- tvOS app that responds to resting-thumb taps.
- watchOS app with a primary action attached inside a scrolling view (conflicts with double-tap default).
- Complex multi-finger gestures that exclude one-handed users.

## SwiftUI

```swift
// Standard tap + long press.
Circle()
    .fill(.accent)
    .frame(width: 80, height: 80)
    .onTapGesture { activate() }
    .onLongPressGesture { showContextMenu() }

// Drag.
@State private var offset: CGSize = .zero

Rectangle()
    .offset(offset)
    .gesture(
        DragGesture()
            .onChanged { offset = $0.translation }
            .onEnded { _ in withAnimation { offset = .zero } }
    )

// Magnify + rotate on the same view.
@State private var scale: CGFloat = 1
@State private var angle: Angle = .zero

image
    .scaleEffect(scale)
    .rotationEffect(angle)
    .gesture(
        SimultaneousGesture(
            MagnificationGesture().onChanged { scale = $0 },
            RotationGesture().onChanged   { angle = $0 }
        )
    )

// List row swipe actions.
List(items) { item in
    Row(item: item)
        .swipeActions(edge: .trailing) {
            Button(role: .destructive) { delete(item) } label: {
                Label("Delete", systemImage: "trash")
            }
        }
}

// watchOS — mark primary action so double-tap fires it.
Button("Start Workout") { start() }
    .handGestureShortcut(.primaryAction)
```

## UIKit

```swift
// Tap + long press.
let tap = UITapGestureRecognizer(target: self, action: #selector(activate))
cardView.addGestureRecognizer(tap)

let longPress = UILongPressGestureRecognizer(target: self, action: #selector(showMenu(_:)))
longPress.minimumPressDuration = 0.4
cardView.addGestureRecognizer(longPress)

// Pinch + rotate simultaneously on the same view.
let pinch = UIPinchGestureRecognizer(target: self, action: #selector(pinched(_:)))
let rotate = UIRotationGestureRecognizer(target: self, action: #selector(rotated(_:)))
pinch.delegate = self
rotate.delegate = self
imageView.addGestureRecognizer(pinch)
imageView.addGestureRecognizer(rotate)

extension ViewController: UIGestureRecognizerDelegate {
    func gestureRecognizer(_ g: UIGestureRecognizer,
                           shouldRecognizeSimultaneouslyWith other: UIGestureRecognizer) -> Bool {
        true
    }
}
```

## See also

- [`../foundations/accessibility.md`](../foundations/accessibility.md) — hit targets, Voice Control, Switch Control.
- [`./pointer-and-keyboard.md`](./pointer-and-keyboard.md) — macOS trackpad gestures in detail.
- [`./apple-pencil.md`](./apple-pencil.md) — Pencil + touch coexistence on iPad.
- [`./focus-and-remote.md`](./focus-and-remote.md) — tvOS clickpad.
- [`./digital-crown.md`](./digital-crown.md) — watchOS navigation input.
- [`./spatial-input.md`](./spatial-input.md) — visionOS eyes + hands in depth.
- [`../patterns/drag-and-drop.md`](../patterns/drag-and-drop.md) (pending) — drag-and-drop patterns.
