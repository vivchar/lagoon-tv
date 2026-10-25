# Pointer and keyboard

**HIG sources:**
- https://developer.apple.com/design/human-interface-guidelines/pointing-devices
- https://developer.apple.com/design/human-interface-guidelines/keyboards

**Last verified:** 2026-04-21
**Applies to:** iPadOS · macOS · visionOS (paired) · iOS (keyboard only)
**Not supported:** watchOS (no keyboard/pointer input paradigm)

Pointer and keyboard are how people on iPad and Mac — and paired Vision Pro — actually work fast. Supporting them isn't a nicety; it's how your app feels on a laptop-style iPad or on a Mac at all.

## Pointer — the rules

- **Be consistent** with systemwide gestures. Mission Control, swipe between pages, Notification Center — users rely on these and customize them.
- **Never redefine systemwide trackpad gestures**, even in games.
- **Consistent experience across inputs.** Touch, pointer, keyboard all reach the same features.
- **Hold-to-reveal fading chrome.** Safari toolbar minimizes; holding the pointer near it reveals it again. Video playback controls work the same.
- **Modifier keys behave the same with pointer and touch.** Option-drag duplicates either way. Shift-drag constrains either way.

## iPadOS pointer

iPadOS integrates pointer shape + element appearance — the pointer **lives in** the element it's targeting.

### Content effects

Three effects, each with a specific intent:

| Effect | Visual | When to use |
|---|---|---|
| **Highlight** | Translucent rounded rectangle background with parallax | Small transparent-background elements: bar buttons, tab bars, segmented controls, edit menus |
| **Lift** | Element scales up, shadow appears, specular highlight | Small elements with an opaque background: app icons, Control Center buttons |
| **Hover** | Custom scale / tint / shadow; pointer shape doesn't change | Large elements or custom hover feel |

### Pointer shapes

- **Default circle** — most contexts.
- **I-beam** — text entry areas (automatic).
- **Custom shape** for contextual precision (crosshair for drawing, etc.) — keep simple.
- **Pointer accessories** (small arrows, symbols) — indicate resizability or other state. Transition them to signal state changes (e.g., `.plus` → `.xmark` when an add becomes unavailable).

### Pointer magnetism

- **Magnetism is automatic** for elements with the **lift** and **highlight** effects. The hit region pulls the pointer in before the visible boundary.
- **Not applied to hover-effect elements** — would feel jarring since the pointer shape doesn't change.
- **Applied to text-entry areas** so small vertical drift while selecting text doesn't skip lines.

### Rules

- **Use system-provided effects for standard elements.** Custom effects only when the element is custom.
- **Standard button/text-field appearances** for the pointer — don't reinvent.
- **Hit region padding**: ~12pt around bezeled elements, ~24pt around bezel-less. Too small feels twitchy; too large feels sticky.
- **Contiguous hit regions for custom bar buttons** — avoid pointer flashes between adjacent buttons as it reverts to the default shape.
- **Specify corner radius for custom-shape lift elements** (circles, pills) so the pointer animates into the shape smoothly.
- **Custom pointer shapes** — keep simple. Purely decorative effects annoy.
- **Annotations** (e.g., X/Y values in a graph) are fine. Instructional text overlaid on the pointer is not.

### Custom hover effects

- Combine tint / scale / shadow carefully. Scaling doesn't fit a table row (overlaps neighbors). Tint alone works for dense rows.
- Don't use shadow without scale — feels broken.

## macOS pointer

Mac is pointer-first. The system provides a wide gesture vocabulary that users customize.

### Gestures (mouse + trackpad)

| Gesture | Expected behavior | Mouse | Trackpad |
|---|---|---|---|
| Primary click | Select / activate | ● | ● |
| Secondary click (right-click / Control-click) | Contextual menu | ● | ● |
| Scroll | Move content | ● | ● |
| Smart zoom (double-tap two fingers / double-tap) | Toggle zoom | ● | ● |
| Swipe between pages | Navigate forward/back | ● | ● |
| Swipe between full-screen apps | Switch spaces | ● | ● |
| Mission Control | Activate Mission Control | ● | ● |
| Lookup (force click or tap three fingers) | Lookup / data detectors | — | ● |
| Tap to click | Primary click via tap | — | ● |
| Force click | Quick Look / variable pressure | — | ● |
| Pinch to zoom | Zoom | — | ● |
| Rotate | Rotate image | — | ● |
| Notification Center (edge swipe) | Show Notification Center | — | ● |
| App Exposé (swipe down) | Show app windows | — | ● |
| Launchpad (pinch with thumb+3 fingers) | Show Launchpad | — | ● |
| Show Desktop (spread with thumb+3 fingers) | Show desktop | — | ● |

### Standard pointer shapes

- **Arrow** — default.
- **I-beam** (horizontal or vertical) — text entry.
- **Pointing hand** — link.
- **Crosshair** — precise selection.
- **Open / closed hand** — pan content.
- **Resize arrows** (up, down, left, right, up-down, left-right) — resize handles.
- **Disappearing item** — drop removes item.
- **Drag copy / drag link** — Option / Option-Command modifiers during drag.
- **Contextual menu** — shown when Control-hovering (rare).
- **Operation not allowed** — drop target rejects the drag.

## visionOS pointer

- **Attach a Bluetooth pointing device** (Magic Trackpad, Magic Mouse, Magic Keyboard with Trackpad) and it coexists with eyes + hands.
- **The user looks + the pointer follows context.** Looking at a different window shifts the pointer's context automatically.
- **Pointer hides during gestures.** Reappears when moved, in the location the user is looking at.

## Keyboard — the rules

### Cross-cutting

- **Standard shortcuts work the same everywhere.** ⌘S, ⌘O, ⌘N, ⌘Z, ⌘⇧Z, ⌘C/V/X, ⌘F, ⌘Q, ⌘,. Don't repurpose.
- **Full Keyboard Access** — every interactive element reachable via keyboard. Test with system setting on.
- **Menu bar commands register shortcuts** — SwiftUI `.keyboardShortcut(...)` / UIKit `UIKeyCommand` / AppKit menu key equivalents. Every shortcut shows up in the menu.
- **Tab cycles focus** forward; Shift-Tab cycles backward. Logical reading order.
- **Esc dismisses** sheets, popovers, alerts.
- **⌘. cancels** in-flight operations.

### Standard shortcut catalog (highlights)

| Key | Default action |
|---|---|
| ⌘N | New document |
| ⌘O | Open |
| ⌘S | Save |
| ⇧⌘S | Save As / Duplicate |
| ⌘W | Close window |
| ⇧⌘W | Close file + windows |
| ⌥⌘W | Close all windows |
| ⌘P | Print |
| ⇧⌘P | Page setup |
| ⌘Q | Quit |
| ⌘Z | Undo |
| ⇧⌘Z | Redo |
| ⌘X / ⌘C / ⌘V | Cut / Copy / Paste |
| ⇧⌘V | Paste as |
| ⌘A | Select all |
| ⇧⌘A | Deselect all |
| ⌘F | Find |
| ⌘G | Find next |
| ⇧⌘G | Find previous |
| ⌘B / ⌘I / ⌘U | Bold / Italic / Underline |
| ⌘, | Open Settings |
| ⌘? | Help menu |
| ⌘H | Hide app |
| ⌥⌘H | Hide others |
| ⌘M | Minimize window |
| ⌘T | Fonts panel |
| ⌃⌘F | Full-screen |
| ⌘` | Cycle open windows in app |

Full table: see the HIG source link above.

### Modifier keys — order and semantics

List modifiers in **Control, Option, Shift, Command** order. Symbols: ⌃ ⌥ ⇧ ⌘.

| Modifier | Recommended use |
|---|---|
| ⌘ | Primary modifier for custom shortcuts. |
| ⇧ | Secondary; complements a related shortcut. |
| ⌥ | Power-user or less-common commands. |
| ⌃ | Avoid. System uses ⌃ extensively (F-keys, accessibility). |

### Custom shortcuts — rules

- **Only for the most frequent app-specific commands.** Too many shortcuts = nothing memorable.
- **Modifier meaning stays consistent.** ⌘-drag moves selection as a group. ⇧-drag constrains to aspect ratio.
- **Don't add ⇧ to a shortcut using the upper character** of a two-character key. "⌘?" is right; "⌘⇧/" is wrong.
- **Don't stack an unrelated modifier on an existing shortcut.** ⇧⌘Z is redo because it's related to Z-for-undo, not because someone wanted a different action.
- **Let the system localize and mirror.** Keyboards in different locales generate different characters; RTL layouts mirror. Don't hardcode key codes.
- **Every toolbar action has a shortcut in the menu bar**, not the other way around. (Power users sometimes hide the toolbar.)

### iPadOS specifics

- **iPadOS text fields, text views, sidebars** support keyboard navigation out of the box.
- **Don't add keyboard navigation to individual controls** (buttons, switches). Full Keyboard Access handles it — your job is to make sure every control is reachable.
- **Attached hardware keyboard** reveals a menu bar. Wire up `Commands` (SwiftUI) or `UIMenuBuilder` (UIKit) commands.
- **Standard shortcuts** (⌘⇥ app switcher, ⌘, settings) work automatically.

### visionOS specifics

- **Hold ⌘ on the paired keyboard** to reveal an overlay of your app's keyboard shortcuts. Shown as a flat list in system categories (File, Edit, View).
- **Descriptive titles** on each shortcut — no submenu context to rely on.
- **Virtual keyboard overlay** with typing completion appears when a physical keyboard is connected.

### Games

- **Single-key bindings** are easier to reach quickly.
- **Test on an Apple keyboard.** If a binding uses Control on a PC keyboard, consider remapping to ⌘ on Apple (next to the Space bar).
- **Key proximity matters** — WASD navigation pairs well with nearby keys for related actions.
- **Let players customize key bindings.** Defaults matter, but so does personal comfort.

## Common mistakes

- Overriding systemwide trackpad gestures.
- Missing pointer hover states on iPad interactive elements.
- Custom highlight that doesn't use the system effects.
- Tiny hit regions with no padding (feel twitchy).
- Toolbar item with no corresponding menu bar command on Mac.
- Standard shortcuts repurposed (e.g., ⌘S for Submit, not Save).
- ⇧⌘Z mapped to something unrelated to undo.
- iPad app with no menu bar commands when a keyboard is attached.
- visionOS app with no ⌘-held shortcut overlay.
- Games that can't remap key bindings.
- iPad app that styles a custom pointer badly and hides the user's real cursor state.

## SwiftUI

```swift
// Pointer effect on a custom button-like view.
CustomCard()
    .hoverEffect(.highlight)                    // highlight effect
// or
CustomCard()
    .hoverEffect(.lift)                         // lift effect

// Custom hover effect with tint + scale.
CustomRow()
    .hoverEffect { effect, isActive, proxy in
        effect
            .animation(.spring, body: { c in
                c.scaleEffect(isActive ? 1.03 : 1)
                 .background(isActive ? .quaternary : .clear)
            })
    }

// Keyboard shortcuts wired to app commands.
.commands {
    CommandGroup(replacing: .newItem) {
        Button("New Note") { newNote() }
            .keyboardShortcut("n", modifiers: .command)
    }
    CommandMenu("Format") {
        Button("Bold")      { toggle(.bold) }    .keyboardShortcut("b")
        Button("Italic")    { toggle(.italic) }  .keyboardShortcut("i")
        Button("Underline") { toggle(.underline)}.keyboardShortcut("u")
    }
    CommandGroup(after: .sidebar) {
        Button("Find") { showFind() }.keyboardShortcut("f")
    }
}

// Focusable custom view for Full Keyboard Access.
@FocusState private var focus: Field?

TextField("Name", text: $name)
    .focused($focus, equals: .name)
    .submitLabel(.next)
    .onSubmit { focus = .email }

TextField("Email", text: $email)
    .focused($focus, equals: .email)
    .textContentType(.emailAddress)
```

## UIKit

```swift
// Pointer interaction on a custom view.
let interaction = UIPointerInteraction(delegate: self)
customView.addInteraction(interaction)

extension ViewController: UIPointerInteractionDelegate {
    func pointerInteraction(_ i: UIPointerInteraction,
                            styleFor region: UIPointerRegion) -> UIPointerStyle? {
        let effect = UIPointerEffect.lift(.init(view: customView))
        let shape = UIPointerShape.roundedRect(customView.bounds, radius: 12)
        return UIPointerStyle(effect: effect, shape: shape)
    }
}

// Key command in a UIViewController.
override var keyCommands: [UIKeyCommand]? {
    [
        UIKeyCommand(title: "New Note",
                     action: #selector(newNote),
                     input: "n", modifierFlags: .command,
                     discoverabilityTitle: "New Note"),
        UIKeyCommand(title: "Find",
                     action: #selector(showFind),
                     input: "f", modifierFlags: .command)
    ]
}
```

## AppKit

```swift
// NSCursor swap on hover.
imageView.addCursorRect(imageView.bounds, cursor: .crosshair)

// NSMenu item with key equivalent.
let save = NSMenuItem(title: "Save",
                     action: #selector(save(_:)),
                     keyEquivalent: "s")
save.keyEquivalentModifierMask = [.command]
fileMenu.addItem(save)

// Custom shortcut in the View menu.
let find = NSMenuItem(title: "Find…",
                     action: #selector(showFind(_:)),
                     keyEquivalent: "f")
find.keyEquivalentModifierMask = [.command]
editMenu.addItem(find)
```

## See also

- [`./touch-and-gestures.md`](./touch-and-gestures.md) — touch interop with pointer on iPad.
- [`./focus-and-remote.md`](./focus-and-remote.md) — focus system on iPadOS/macOS/tvOS.
- [`../foundations/accessibility.md`](../foundations/accessibility.md) — Full Keyboard Access, Voice Control.
- [`../platforms/macos.md`](../platforms/macos.md) — menu bar is authoritative.
- [`../platforms/ipados.md`](../platforms/ipados.md) — when a keyboard is attached.
- [`../components/buttons.md`](../components/buttons.md) — hover states on buttons.
