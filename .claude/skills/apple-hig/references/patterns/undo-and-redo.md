# Undo and redo

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/undo-and-redo
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · visionOS
**Not supported:** tvOS · watchOS

Undo is how users explore safely. Redo is how they recover from an over-correction. Both are required anywhere the user can modify state that isn't trivially recreated — text, documents, layout, selections, content. Every editable surface gets undo/redo.

## Core rules

- **Let people undo multiple times.** No arbitrary cap. Users expect undo to reach back to the last logical checkpoint (document open, save, session start).
- **Predict the result.** Users hit undo repeatedly until something changes — without prediction, they lose track of what changed. Show the result every time.
- **Name the operation.** On macOS, label the menu items: "Undo Typing", "Redo Paragraph Break". On iOS, when showing an Undo alert, describe the target ("Undo Name", "Redo Address Change").
- **Scroll to the change.** If undoing restores content that's offscreen, scroll to it. Users who don't see the result repeat undo and stack unintended reversals.
- **Consider batched undo.** If the user made ten incremental slider adjustments, let one undo reverse all ten — not each nudge individually. Same for "undo all changes since last save."
- **Revert all changes since open / save** as a separate operation when it makes sense — not a replacement for undo/redo.

## Platform invocation

### iOS / iPadOS / visionOS

- **Three-finger swipe left** → undo.
- **Three-finger swipe right** → redo.
- **Three-finger tap** → undo/redo menu.
- **Shake gesture** (iPhone only) — prompts an undo alert.
- **⌘Z / ⇧⌘Z** on a hardware keyboard.
- **Don't redefine these gestures.**
- **Dedicated toolbar buttons** only if you have a strong reason — the system gestures and shake work everywhere else.
- **Undo alert requires a succinct description.** The title auto-prepends "Undo " or "Redo " — you supply the rest: "Name" becomes "Undo Name".

### macOS

- **Edit menu** has Undo and Redo at the top. Required — users look here first.
- **⌘Z** undo. **⇧⌘Z** redo. Never redefine.
- **Menu items include the action name** — "Undo Typing", "Redo Delete Paragraph".
- **Disable when there's nothing to undo/redo.** Don't remove the items — gray them out so users know the feature exists and is currently unavailable.

## The `UndoManager`

Both UIKit and AppKit expose `UndoManager`. SwiftUI reads from `@Environment(\.undoManager)`.

- **Register each action.** When you mutate state, register the inverse with the undo manager. SwiftUI views can use `Transaction` + `undoManager.registerUndo(withTarget:handler:)`.
- **Set the action name.** `undoManager?.setActionName("Typing")` — this is what gets prefixed in the menu.
- **Group related actions** into a single undo step via `beginUndoGrouping()` / `endUndoGrouping()` or `registerUndo(withTarget:handler:)` wrapped in `undoManager?.groupsByEvent`.

## When NOT to support undo

- **Actions with no reasonable inverse** (sending an email — offer a confirmation instead).
- **System-level actions** that require specific authentication (Apple Pay confirmation — no undo).
- **Asynchronous operations after they've completed** (deleted a cloud file; the file is gone).

For destructive irreversible actions, prefer **confirmation** (action sheet / alert / confirmation dialog) over undo.

## Common mistakes

- No undo support in an editable view — the one the user loses trust in fastest.
- Cap on undo depth (users expect to unwind back to file open).
- Undo that doesn't scroll or highlight the change — feels like "nothing happened".
- Menu items without the action name ("Undo" instead of "Undo Typing").
- Redefining three-finger swipe or shake gesture.
- Redo lost on any text modification after undo (users expect to redo until they type something new).
- Custom undo button in a toolbar that has no corresponding menu item on macOS.
- Confirming every destructive action — undo is for routine reversal; confirm only what's truly not recoverable.
- Each individual slider nudge as its own undo step (should batch).

## SwiftUI

```swift
import SwiftUI

@Observable final class Document {
    var text: String = ""
    var history: [String] = []
}

struct EditorView: View {
    @Bindable var document: Document
    @Environment(\.undoManager) private var undo

    var body: some View {
        TextEditor(text: Binding(
            get: { document.text },
            set: { newValue in
                let old = document.text
                document.text = newValue
                undo?.registerUndo(withTarget: document) { doc in
                    let current = doc.text
                    doc.text = old
                    // Register the redo.
                    undo?.registerUndo(withTarget: doc) { $0.text = current }
                }
                undo?.setActionName("Typing")
            }
        ))
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    undo?.undo()
                } label: {
                    Label("Undo", systemImage: "arrow.uturn.backward")
                }
                .disabled(!(undo?.canUndo ?? false))
                .keyboardShortcut("z")
            }
            ToolbarItem(placement: .primaryAction) {
                Button {
                    undo?.redo()
                } label: {
                    Label("Redo", systemImage: "arrow.uturn.forward")
                }
                .disabled(!(undo?.canRedo ?? false))
                .keyboardShortcut("z", modifiers: [.shift, .command])
            }
        }
    }
}
```

## UIKit

```swift
// In a UIViewController, get the first responder's undo manager.
override func motionEnded(_ motion: UIEvent.EventSubtype, with event: UIEvent?) {
    guard motion == .motionShake,
          let undo = firstResponder?.undoManager ?? self.undoManager else { return }
    // The system usually handles the Undo alert automatically for text views.
}

// Register undo from custom state changes.
func setTitle(_ newTitle: String) {
    let oldTitle = document.title
    undoManager?.registerUndo(withTarget: document) { [oldTitle] doc in
        doc.title = oldTitle
    }
    undoManager?.setActionName("Title Change")
    document.title = newTitle
}
```

## AppKit

```swift
// Menu items Undo / Redo automatically hook to the first responder chain.
// Your NSDocument / NSResponder subclass exposes an NSUndoManager.

override func insertText(_ string: Any, replacementRange: NSRange) {
    let oldText = text
    super.insertText(string, replacementRange: replacementRange)
    undoManager?.registerUndo(withTarget: self) { editor in
        editor.text = oldText
    }
    undoManager?.setActionName("Typing")
}
```

## See also

- [`./entering-data.md`](./entering-data.md) — undo for typing.
- [`./drag-and-drop.md`](./drag-and-drop.md) — undo a drop.
- [`./modality.md`](./modality.md) — confirmation dialog for truly irreversible actions.
- [`../components/sheets-and-popovers.md`](../components/sheets-and-popovers.md#alerts) — destructive confirmation.
- [`../inputs/touch-and-gestures.md`](../inputs/touch-and-gestures.md) — three-finger swipe.
- [`../platforms/macos.md`](../platforms/macos.md) — Edit menu standards.
