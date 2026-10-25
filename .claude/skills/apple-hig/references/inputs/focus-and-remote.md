# Focus and Siri Remote (tvOS) + focus on iPadOS / macOS / visionOS

**HIG sources:**
- https://developer.apple.com/design/human-interface-guidelines/focus-and-selection
- https://developer.apple.com/design/human-interface-guidelines/remotes

**Last verified:** 2026-04-21
**Applies to:** iPadOS · macOS · tvOS (focus); tvOS (Siri Remote)
**Not supported:** iOS · watchOS (no focus system; watchOS uses the [Digital Crown](./digital-crown.md))

Focus is how keyboard, remote, and game-controller navigation works across Apple platforms. tvOS makes focus the **only** way to navigate; iPadOS and macOS layer it over pointer and touch. On visionOS, focus works with a connected input device while **eyes + pinch** remain the dominant model (see [`./spatial-input.md`](./spatial-input.md)).

## Focus — core model

- **Focus identifies what your next action targets.** Move focus via directional input (remote, game controller arrow keys, Tab).
- **Focus effects** are platform-specific: iPadOS/macOS use a ring or background highlight; tvOS uses scale + lift + shadow; visionOS uses the hover effect when eyes target an item.
- **Focusing often implies selecting.** Exception: when auto-select would cause a distracting context shift (opening a view). tvOS cells, for example, require a **separate press** to activate after focus lands.
- **Don't move focus without user action.** Users lose their place. Exception: when a focused item disappears and moving to a neighbor is the only sensible option.
- **System focus effects first.** Custom effects are last resort. System effects are tuned for feel and accessibility.

## iPadOS focus

iPadOS has a focus system for keyboard + Full Keyboard Access. It supports **focus groups** — sidebar, grid, list — with two keyboard interactions:

- **Tab** → move between focus groups.
- **Arrow keys** → move within the current focus group (tvOS-like).

### Rules

- **Focus for content elements** — list items, text fields, search fields. The system's Full Keyboard Access handles controls (buttons, segmented controls, switches).
- **Two focus indicators:**
  - **Halo (ring)** — outline around custom views and opaque cell content (images).
  - **Highlighted appearance** — text in the accent color on a collection view cell you've configured.
- **Customize the halo** for unusual shapes (circles, Bézier paths). Adjust position when a parent clips it or a badge needs to sit above it.
- **Logical reading order.** Tab moves focus in reading order (leading → trailing, top → bottom). Override for custom layouts where the default order is wrong (a stack column that should focus top-to-bottom before moving trailing).
- **Primary item priority.** Mark the most important item in a group so it receives focus when the group does.

## macOS focus

- **Focus ring** on every actionable item the keyboard can reach. Don't suppress it.
- **System provides the ring** — don't replace with a custom highlight unless you have a specific reason.
- **Full Keyboard Access** — every control reachable. Tab moves forward; Shift-Tab moves backward.
- **Focus persists** across view reloads where possible (the user was focused on row 5; they still are).

## visionOS focus

visionOS supports **the same focus system as iPadOS and tvOS** for connected input devices (keyboard, game controller). But focus is **not** the hover effect that happens when the user looks at something — those are separate.

- **Hover effect** = eyes target a UI element (see [`./spatial-input.md`](./spatial-input.md)).
- **Focus** = a focusable item selected via keyboard/controller.

When both are active, focus takes precedence for input routing.

## tvOS focus engine

In tvOS, **everything** is focus. There's no pointer, no cursor. Every actionable item must be focusable.

### Focus states

| State | Description |
|---|---|
| **Unfocused** | Normal, quieter than focused. |
| **Focused** | Foreground, scaled, with illumination and parallax. |
| **Selected** | Just chosen — brief visual feedback (inverted colors / bounce) before state changes. |
| **Checked** (deselected inverse) | Persistent selected state (e.g., a heart-shaped favorite button). |
| **Disabled** | Unavailable; can't take focus. |

Supply assets sized for the **focused (scaled) state** so they look sharp. Ensure surrounding layout has padding so the scaled focus view doesn't crop or overlap neighbors.

### Rules

- **Make every actionable item focusable.** Non-actionable decoration isn't.
- **Full-screen experiences** — hide focus. At full screen, the item is the interaction; gestures affect it directly.
- **Avoid pointers** — they're not the navigation model on tvOS. If your game needs a pointer (flight sim, hidden-object game), make it very visible and feel integrated.
- **Don't pre-round image corners** — the focus engine rounds them on focus; your pre-clipping fights the effect.
- **Layouts accommodate the focused scale** — 1.1x or so. Leave breathing room.

## Siri Remote (tvOS)

The primary input for Apple TV. Combines a **clickpad + touch surface** with physical buttons.

### Layout

- **Clickpad** — touch surface around a directional ring, both tap-sensitive and press-sensitive.
- **Select** (center of clickpad).
- **Back** (top-right; replaces older Menu button).
- **TV / Home**.
- **Play/Pause**.
- **Volume +/−**.
- **Siri / Mute**.
- **Power**.

### Gestures

| Gesture | Behavior |
|---|---|
| Clickpad swipe | Move focus |
| Clickpad press (center) | Activate focused item |
| Direction click (up/down/left/right) | Step focus |
| Back button | Navigate back / dismiss modal / exit to Home |
| Play/Pause | Play / pause / resume |
| Siri hold | Voice input |
| Clickpad swipe-down (while video plays) | Reveal info panel (subtitles, audio, metadata) |

### Positional taps

The remote differentiates **up / down / left / right taps** on the clickpad surface. Use only when the position is **intuitive and discoverable** — revealing the info panel via a downward tap on the clickpad surface is a good fit; inventing arbitrary actions is not.

### Button behavior contract

Your app should respond to specific buttons in standard ways:

| Button | Expected in apps | Expected in games |
|---|---|---|
| Clickpad swipe | Move focus | Directional pad |
| Clickpad press | Activate | Primary button |
| Back | Go back; top-level exits to Home | Pause/resume; in menus, back one level |
| Play/Pause | Start/pause/resume playback | Secondary button / skip intro |

### In-game exception for Back

- **Press Back during active gameplay → in-game pause menu.** Users accidentally press Back during gameplay all the time.
- **Press Back in pause menu → close menu + resume.**
- **Press-and-hold Back → exit to Apple TV Home Screen** (system-handled).

### Inadvertent tap protection

- **Tap ≠ press.** Resting a thumb on the clickpad, picking up the remote, handing it off all produce incidental taps.
- **During video playback, ignore taps.** Wait for presses. Taps are fine for navigation and revealing metadata.
- **Visual feedback for rested thumb** — show the user a hint (e.g., the info-panel reveal affordance) so they know what a downward swipe would do.

## Compatible remotes and live TV

Some Apple TV–compatible remotes include a **Guide / Browse button**, **Page Up / Page Down**, and **Channel Up / Down** — for live-TV and EPG navigation.

- **Guide button** → open your electronic program guide (EPG), if you have one.
- **Page Up / Page Down** inside an EPG → page the guide.
- **Page Up / Page Down while content plays** → change channel.
- **Fall back to the system's default guide app** if you don't provide an EPG.

## Common mistakes

- **No focus support** (tvOS app that doesn't respond to directional input at all).
- **Custom focus effect that doesn't scale** — ignores the platform feel.
- **Pre-rounded images** that fight the focus rounding.
- **Focused elements overlap neighbors** because the layout wasn't padded for scale.
- **Color alone for focus** — fails accessibility and TV color-gamut variance.
- **No in-game pause on Back** — users lose state.
- **Responding to incidental taps** during video playback.
- **Pointer UI on tvOS** where focus would work.
- **No Tab/Shift-Tab support** on iPadOS or macOS in custom views.
- **Suppressing the macOS focus ring.**
- **iPadOS custom view that traps Tab** and prevents focus moving to the next group.
- **Not marking a primary item in an iPadOS focus group** — focus lands on whichever happens to come first.

## SwiftUI

```swift
// tvOS — a grid of focusable cards using the system card button style.
struct PosterGrid: View {
    let items: [Item]
    var body: some View {
        ScrollView {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 240), spacing: 40)],
                      spacing: 40) {
                ForEach(items) { item in
                    NavigationLink(value: item) {
                        PosterCell(item: item)        // don't pre-round corners
                    }
                    .buttonStyle(.card)               // system focus handling
                }
            }
            .padding(.horizontal, 80)                 // tvOS safe area
            .padding(.vertical, 60)
        }
    }
}

// iPadOS / macOS — focus management in a custom form.
@FocusState private var field: Field?
enum Field: Hashable { case name, email, notes, save }

Form {
    TextField("Name",  text: $name)
        .focused($field, equals: .name)
        .submitLabel(.next)
        .onSubmit { field = .email }

    TextField("Email", text: $email)
        .focused($field, equals: .email)
        .textContentType(.emailAddress)
        .submitLabel(.next)
        .onSubmit { field = .notes }

    TextEditor(text: $notes)
        .focused($field, equals: .notes)

    Button("Save") { save() }
        .buttonStyle(.borderedProminent)
        .keyboardShortcut(.defaultAction)
        .focused($field, equals: .save)
}

// tvOS — focus changes trigger haptic-like visual feedback.
.focusable(true) { isFocused in
    // React to focus transitions, e.g., swap a hero image.
    currentlyFocused = isFocused
}

// iPadOS — custom focus group ordering.
Section {
    ForEach(rows) { row in RowView(row: row) }
}
.focusSection()                                  // this section is a focus group
```

## UIKit

```swift
// tvOS — a UICollectionView that relies on the focus engine automatically.
// Cells with .selectionStyle = .none and isUserInteractionEnabled = true
// become focusable by default. To exclude a cell from focus:
func collectionView(_ cv: UICollectionView, canFocusItemAt indexPath: IndexPath) -> Bool {
    !cellIsDecoration(at: indexPath)
}

// Customize focus behavior for a cell.
class PosterCell: UICollectionViewCell {
    override func didUpdateFocus(in context: UIFocusUpdateContext,
                                 with coordinator: UIFocusAnimationCoordinator) {
        coordinator.addCoordinatedAnimations({
            if self.isFocused {
                self.transform = .init(scaleX: 1.08, y: 1.08)
                self.layer.shadowOpacity = 0.25
            } else {
                self.transform = .identity
                self.layer.shadowOpacity = 0
            }
        })
    }
}

// iPadOS — custom view with a halo focus effect.
override var canBecomeFocused: Bool { true }

override func didUpdateFocus(in context: UIFocusUpdateContext,
                             with coordinator: UIFocusAnimationCoordinator) {
    focusEffect = UIFocusHaloEffect(rect: bounds, cornerRadius: 12, curve: .continuous)
}
```

## AppKit

```swift
// Focus ring on a custom NSControl.
override var focusRingType: NSFocusRingType { .exterior }
override var acceptsFirstResponder: Bool { true }

// Full Keyboard Access is opt-in per control on macOS; default controls
// already participate. For custom views, override becomeFirstResponder
// and draw your own focus ring if the default doesn't suffice.
```

## See also

- [`./touch-and-gestures.md`](./touch-and-gestures.md) — tvOS remote's clickpad gestures.
- [`./pointer-and-keyboard.md`](./pointer-and-keyboard.md) — Tab focus on iPadOS/macOS.
- [`./digital-crown.md`](./digital-crown.md) — watchOS/visionOS equivalent input paradigm.
- [`./spatial-input.md`](./spatial-input.md) — visionOS gaze is separate from the focus system.
- [`../platforms/tvos.md`](../platforms/tvos.md) — focus engine on tvOS in context.
- [`../components/bars.md`](../components/bars.md) — tvOS tab bar focus behavior.
- [`../patterns/live-viewing-apps.md`](../patterns/live-viewing-apps.md) (pending) — EPG and channel navigation.
