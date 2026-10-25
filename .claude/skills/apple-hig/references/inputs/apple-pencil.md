# Apple Pencil

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/apple-pencil-and-scribble
**Last verified:** 2026-04-21
**Applies to:** iPadOS (iPad with compatible Pencil)
**Not supported:** iOS · macOS · tvOS · visionOS · watchOS

Apple Pencil turns iPad into a marking instrument. It's precision drawing, marking up PDFs, natural handwriting via Scribble, and a first-class pointer. Design for how people use a **real** pencil — they'll bring those intuitions with them.

## Pencil hardware capabilities (per model)

Features vary by Pencil model:

| Capability | Pencil (1st gen) | Pencil (2nd gen) | Pencil USB-C | Pencil Pro |
|---|---|---|---|---|
| Pressure (force) | ✓ | ✓ | — | ✓ |
| Tilt (altitude) | ✓ | ✓ | ✓ | ✓ |
| Azimuth (orientation) | ✓ | ✓ | ✓ | ✓ |
| Hover | — | — | ✓ (M2 iPad Pro+) | ✓ |
| Double tap | — | ✓ | — | ✓ |
| Squeeze | — | — | — | ✓ |
| Barrel roll | — | — | — | ✓ |
| Haptic feedback | — | — | — | ✓ |
| Find My | — | — | — | ✓ |

Detect what's attached and degrade gracefully — don't require Pencil Pro features.

## Core rules

- **Let Pencil write the moment it touches the screen.** No "tap to enter drawing mode" button. Real pencils don't work that way.
- **Let people choose when to use Pencil vs finger.** Controls respond to both. Scribble is Pencil-only; everything else dual-supports.
- **Respond to pressure, tilt, orientation, and barrel roll** where meaningful. Vary stroke thickness and intensity. Opacity and brush size map naturally to pressure.
- **Immediate visual feedback.** Pencil appears to directly manipulate the content it touches. No disconnected actions in other areas.
- **Design for both hands.** Don't put controls where a right or left hand will cover them. If you can't avoid that, let users reposition controls.

## Hover (Pencil Pro / USB-C on M2 iPad Pro+)

Pencil hovers above the screen and the app can respond.

- **Preview the mark** the current tool would make — size, color, opacity — at a neutral middle value. Don't vary the preview with hover height (distracting, rarely accurate).
- **Don't use hover to perform actions.** Especially destructive ones. Hover is imprecise; unintended activation is easy.
- **Reserve contextual reveal for gesture + modifier** (e.g., double-tap or a keyboard modifier). Revealing a tool menu near the Pencil tip is a great use of this.
- **Prefer hover preview for Pencil, not for pointing devices.** Mixing them confuses users.

## Double tap (Pencil 2 / Pro)

By default, a system setting controls what double-tap does: eraser toggle, previous tool, color picker, or nothing.

- **Respect user settings** whenever they make sense in your app.
- **Custom double-tap is OK** if the system settings don't make sense in your app — e.g., toggling raise/lower modes on a 3D mesh tool.
- **Provide a way to pick the mode** when you ship custom behavior. Users need to know what mode they're in.
- **Never use double tap for destructive actions.** Accidental taps happen.

## Squeeze (Pencil Pro only)

A quick squeeze gesture, usable while the Pencil is away from the screen.

- **Single, discrete action** — not a continuous or a repeat-hold gesture. Squeezes are fatiguing.
- **Contextual UI reveal near the Pencil tip** when squeeze opens a menu (keeps the connection between gesture and outcome).
- **Nondestructive actions only.** Squeezes can happen unintentionally.
- **Might be bound to a Shortcut** by the user — your app's squeeze handler isn't always what runs.

## Barrel roll (Pencil Pro only)

Rotate the Pencil along its axis. Changes the mark being made.

- **Only for marking behavior** — rotate the brush, change pen angle for highlighter strokes.
- **Not for UI navigation** or revealing menus. Not intuitively related to those actions.

## Scribble — handwriting as input

Scribble converts handwriting to text. Works in **any standard text component** (text fields, text views, search fields, web content) by default. Password fields don't accept Scribble.

### Rules

- **Make it feel fluid.** Don't require a tap to focus a field before writing.
- **Make Scribble available where text entry is natural.** Blank space below a list might accept "write here to add an item" — see Reminders.
- **Don't distract while writing.**
  - Hide placeholder text when the user starts writing.
  - Don't autocomplete aggressively during a stroke.
  - Don't animate fields while the user is writing (moving search field → they lose their place).
  - Don't autoscroll while the user is actively writing or selecting.
- **Give fields enough space.** Small fields feel cramped for writing. Expand the field before or between strokes — never while writing.

### Custom text views

Custom text surfaces need Scribble integration (`UIScribbleInteraction` / `UIIndirectScribbleInteraction`). Without it, Pencil writing won't work.

## Drawing with PencilKit

`PencilKit` gives you low-latency drawing with the same feel as built-in iOS. Includes a ready-to-use tool picker with ink, pencil, marker, highlighter, and eraser tools.

### Rules

- **Default PencilKit canvas adapts to Dark Mode** — colors flip for light/dark backgrounds. When drawing over existing content (PDF, photo), turn this off so markup stays sharp and visible.
- **Tool picker already has undo/redo in regular size class.** In compact size class, ship a toolbar with your own undo/redo + support the standard 3-finger undo/redo gesture.
- **Pencil-only mode vs dual-mode** — let the user choose whether finger input draws too.
- **Low latency matters.** Use PencilKit (or Metal directly) — custom drawing with UIKit is too slow.

## Platform considerations

Apple Pencil is iPad-only. Not available on iPhone, Mac, Apple Watch, or Vision Pro.

## Common mistakes

- A "drawing mode" button instead of letting Pencil work immediately.
- Controls that don't respond to Pencil input (feels broken).
- Ignoring Pencil pressure and tilt — flat, lifeless strokes.
- Custom text view without Scribble support.
- Placeholder text showing while the user writes over it.
- Search field jumps while the user is writing in it.
- Autocomplete suggestions appearing mid-stroke.
- Double-tap mapped to a destructive action.
- Squeeze used for a hold-style continuous gesture.
- Barrel roll used to open menus (not related to marking).
- Hover used to activate destructive actions.
- Tool picker duplicated in a view that already uses the system picker.
- Dark-mode dynamic color on markup drawn over a photo (markup becomes invisible when the user switches mode).

## SwiftUI — Pencil + PencilKit

```swift
import SwiftUI
import PencilKit

struct SketchView: View {
    @State private var canvas = PKCanvasView()
    @State private var toolPicker = PKToolPicker()

    var body: some View {
        PencilKitCanvas(canvas: canvas, toolPicker: toolPicker)
            .ignoresSafeArea()
            .toolbar {
                ToolbarItemGroup(placement: .primaryAction) {
                    Button { canvas.undoManager?.undo() } label: {
                        Label("Undo", systemImage: "arrow.uturn.backward")
                    }
                    Button { canvas.undoManager?.redo() } label: {
                        Label("Redo", systemImage: "arrow.uturn.forward")
                    }
                }
            }
    }
}

struct PencilKitCanvas: UIViewRepresentable {
    let canvas: PKCanvasView
    let toolPicker: PKToolPicker

    func makeUIView(context: Context) -> PKCanvasView {
        canvas.drawingPolicy = .pencilOnly            // finger scrolls; Pencil draws
        canvas.backgroundColor = .secondarySystemBackground
        canvas.becomeFirstResponder()
        toolPicker.setVisible(true, forFirstResponder: canvas)
        toolPicker.addObserver(canvas)
        return canvas
    }

    func updateUIView(_ view: PKCanvasView, context: Context) {}
}
```

```swift
// Scribble enablement on a custom text-like view.
struct CustomTextSurface: UIViewRepresentable {
    @Binding var text: String

    func makeUIView(context: Context) -> UITextView {
        let tv = UITextView()
        tv.font = .preferredFont(forTextStyle: .body)
        tv.adjustsFontForContentSizeCategory = true
        tv.textContainer.lineBreakMode = .byWordWrapping
        // UITextView natively supports Scribble — nothing special needed.
        return tv
    }
    func updateUIView(_ view: UITextView, context: Context) { view.text = text }
}

// For a fully custom (non-UITextView) drawing surface that should accept
// Scribble input anywhere on its bounds, use UIIndirectScribbleInteraction in UIKit.
```

## UIKit — hover + double tap

```swift
let hover = UIHoverGestureRecognizer(target: self, action: #selector(hoverChanged(_:)))
canvasView.addGestureRecognizer(hover)

@objc func hoverChanged(_ g: UIHoverGestureRecognizer) {
    switch g.state {
    case .began, .changed:
        let loc = g.location(in: canvasView)
        showBrushPreview(at: loc)
    case .ended, .cancelled:
        hideBrushPreview()
    default: break
    }
}

// Double-tap on Pencil 2 / Pro — respect the user's system setting.
UIPencilInteraction.prefersPencilOnlyDrawing = true

let interaction = UIPencilInteraction()
interaction.delegate = self
view.addInteraction(interaction)

extension ViewController: UIPencilInteractionDelegate {
    func pencilInteractionDidTap(_ i: UIPencilInteraction) {
        switch UIPencilInteraction.preferredTapAction {
        case .switchEraser:       selectTool(.eraser)
        case .switchPrevious:     selectPreviousTool()
        case .showColorPalette:   showColors()
        case .ignore:             break
        case .runSystemShortcut:  break
        @unknown default:         break
        }
    }
}
```

## See also

- [`./touch-and-gestures.md`](./touch-and-gestures.md) — touch coexists with Pencil.
- [`./pointer-and-keyboard.md`](./pointer-and-keyboard.md) — Pencil is also a pointer on iPadOS.
- [`../platforms/ipados.md`](../platforms/ipados.md#apple-pencil-and-scribble) — where Pencil fits in iPadOS.
- [`../components/text-inputs.md`](../components/text-inputs.md) — Scribble behavior on text fields.
- [`../patterns/undo-and-redo.md`](../patterns/undo-and-redo.md) (pending) — three-finger undo/redo support for compact size class drawing.
