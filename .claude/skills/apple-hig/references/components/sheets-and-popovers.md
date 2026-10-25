# Sheets, popovers, alerts, action sheets

**HIG sources:**
- https://developer.apple.com/design/human-interface-guidelines/sheets
- https://developer.apple.com/design/human-interface-guidelines/popovers
- https://developer.apple.com/design/human-interface-guidelines/alerts
- https://developer.apple.com/design/human-interface-guidelines/action-sheets

**Last verified:** 2026-04-20
**Applies to:** varies per component (see each section)

Four presentations that interrupt the main view. Picking the wrong one is the most common Claude mistake in this space. This table is the short answer:

| You want to… | Use |
|---|---|
| Scoped sub-task with its own view (attach a file, create an item) | **Sheet** |
| A few related options related to the current action (unsaved edits → Discard / Save / Keep Editing) | **Action sheet** / confirmation dialog |
| Critical, user-actionable information or confirmation of a non-recoverable action | **Alert** |
| Small contextual widget that points at its source (quick edit, color picker) | **Popover** |
| Non-blocking confirmation of a success | **None** — use a toast, `sensoryFeedback`, or inline confirmation |

## Sheets

A sheet lets people perform a **scoped, related task** and then return to the parent view. Modal by default on macOS, tvOS, visionOS, and watchOS. On iOS/iPadOS, sheets can be **modal or non-modal**.

### Rules

- **One sheet at a time.** Closing a sheet returns to the parent view. If an action inside a sheet opens another, close the first.
- **Use a non-modal sheet for supplementary items that affect the parent view.** Example: Notes' formatting sheet on iPhone/iPad stays up while you edit the note.
- **For long or complex flows, pick an alternative.** Full-screen cover on iOS/iPadOS; a new window on macOS; a Full Space on visionOS.
- **Always pair Done with Cancel.** A lone Done button implies completion is the only way out. If the flow is multi-step, "Back" can replace Cancel.
- **Don't show Cancel + Done + Back together.** Pick two.
- **Placement (iOS/iPadOS single-view sheet):** Cancel leading, Done trailing in the top toolbar.

### Sheet detents (iOS / iPadOS)

A resizable sheet has **detents** — heights at which it rests. System detents: `medium` (≈half) and `large` (full).

- **Add `medium`** for progressive disclosure (Share Sheet does this: most relevant items visible, expand for more).
- **Skip `medium`** when the sheet's content needs full height (Compose in Mail, Messages).
- **Include a grabber** on resizable sheets — it tells people they can drag and works with VoiceOver to change detent.
- **Support swipe-to-dismiss** (the default). If there are unsaved changes, confirm via an action sheet / confirmation dialog.
- **On iPad, prefer `.presentationPage` or `.presentationForm`** — fixed sizes, centered on a dimmed parent.

### macOS sheets

- **Card-like view with rounded corners** floating on the parent window; parent dimmed.
- **Sensible default size.** Most sheets don't need to be resizable — but support resizing if users benefit.
- **Other app windows remain interactable.** Don't block them.
- **Use a panel, not a sheet, for repeated-input workflows** (Find & Replace) that need persistent observation of results.

### visionOS sheets

- Float in front of parent window, dimming it.
- **Avoid sheets that emerge from the bottom** — prefer centered in the field of view.
- **Default size that keeps the parent visible** — don't cover most of the window. Allow resizing.

### watchOS sheets

- Full-screen view sliding over current content with material background (blurs the behind).
- **Use only for custom title or content** that a standard alert / action sheet can't provide.
- **Keep brief and occasional** — a sheet interrupts the current workflow.

## Popovers

Transient view anchored to its source. Small amount of content, one-shot task.

**Not supported on tvOS or watchOS.**

### Rules

- **Small amount of information or functionality.** Popovers vanish after interaction.
- **Arrow points at the trigger.** Don't cover the trigger or essential nearby content.
- **Close button only when helpful** (e.g., to disambiguate "cancel vs save"). Otherwise popovers close when the user taps outside or picks an item.
- **Save automatically** when a non-modal popover is dismissed by tapping outside. Only discard on explicit Cancel.
- **One popover at a time.** Never cascade popovers.
- **Nothing renders on top of a popover** — except an alert.
- **Size to content.** System may adjust to fit.
- **Animate size changes smoothly** — avoid the impression of a new popover replacing the old one.
- **Don't use "popover" in help copy.** Refer to the content or the trigger instead.
- **Don't warn with a popover.** Use an alert.

### Platform specifics

- **iOS / iPadOS:** Don't display popovers in compact-width views. SwiftUI auto-adapts (popover becomes a sheet on iPhone); check the size class and pick accordingly.
- **macOS:** Popovers can be **detachable** — drag to turn into a free-floating panel. Makes them persistent while the user works elsewhere. Keep the detached appearance close to the original to preserve context.

## Alerts

Blocking modal for **critical, user-actionable** information.

### Rules

- **Use sparingly.** Every alert is an interruption.
- **Never informational-only.** If there's nothing to do, find another surface (inline banner, toast, status indicator).
- **No alert for common undoable destructive actions** (every email delete doesn't need one — user intended to delete, and can undo).
- **Do alert on uncommon, non-undoable destructive action.**
- **Don't show on app start.** If you need to signal a startup problem, use non-blocking chrome.

### Anatomy

Title + optional informative message + **up to three buttons**. Platform extras:

- iOS / iPadOS / macOS / visionOS alerts can include a text field.
- macOS / visionOS alerts can include an icon and an accessory view.
- macOS alerts can include a "Don't show again" checkbox and a Help button.

### Writing alerts

- **Title describes the situation** clearly and succinctly. Specific: "Couldn't save to iCloud — your network appears offline." Not "Error" or "Error 329347." Don't exceed two lines.
- **Title capitalization:** complete sentence → sentence case + ending punctuation. Fragment → title case, no ending punctuation.
- **Informative message only if it adds value.** Keep short. Complete sentences, sentence case, appropriate punctuation.
- **Tone:** neutral, direct, approachable. Don't blame the user.
- **Don't explain buttons** in the body copy.
- **Button labels are verbs or verb phrases** describing the result ("Delete", "Archive", "Try Again"). "OK" only in purely informational alerts.
- **Always use "Cancel"** for the cancel button.

### Buttons

- **Default button** is on the trailing side of a row or top of a stack. Pressing Return activates it.
- **Cancel is on the leading side of a row / bottom of a stack.**
- **Destructive style** for buttons that perform destructive actions the user didn't deliberately choose. If the destructive action is what the user explicitly asked for (Empty Trash), don't apply destructive style to the confirmation.
- **If there's a destructive action, always include Cancel.**
- **Don't make the Cancel button the default.** If you want people to read the alert, don't make anything the default.
- **Single-button alerts use Done**, not Cancel.

### Dismissal

Provide ways besides the Cancel button:

- iOS / iPadOS: Home Screen exit.
- iOS / iPadOS / macOS / visionOS: Escape or Command-Period.
- tvOS: Menu button on the remote.

### Platform specifics

- **iOS / iPadOS:** Prefer an action sheet for choices related to an intentional action; alerts are unexpected.
- **macOS:** Icon is your app's by default. Custom caution symbol only when truly exceptional. Suppression checkbox + Help button available.
- **visionOS:** Alerts appear in front of the app window, slightly forward on the z-axis. Anchored to the window; moves with it. In a Full Space, centered in view. Accessory view max height **154pt**, 16pt corner radius.

## Action sheets (confirmation dialogs)

In SwiftUI, the cross-platform API is **`.confirmationDialog`** — it renders as an action sheet on iOS, a menu on macOS, etc.

### When to use

- **Clarifying choices after an intentional action.** "You tapped Cancel — keep your edits, discard, or keep editing?"
- **Alternative confirmation** for destructive actions that need options beyond Cancel / Confirm.

### Rules

- **Use sparingly** — each one interrupts.
- **Titles fit on one line** where possible.
- **Message only when necessary.**
- **Include Cancel** for any action sheet that could destroy data.
- **Destructive actions at the top** with destructive style; Cancel at the bottom (upper-left on watchOS).

### Platform specifics

- **Not supported on visionOS.**
- **iOS / iPadOS:** Distinct from a menu. Action sheets appear in response to an intentional action; menus appear when the user chooses to open them.
- **Avoid letting action sheets scroll.** If they scroll, you have too many options — split or redesign.
- **watchOS:** Max 4 buttons including Cancel.

## Common mistakes

- Sheet for drill-in navigation.
- Full-screen cover for a simple sub-task (use a sheet).
- Alert for confirming a common undoable action.
- Alert on launch as a "welcome" surface.
- "OK" buttons on alerts that need a real verb.
- No Cancel button on a destructive alert.
- Cancel as the default button on an alert.
- Popover in a compact-width iOS view (becomes a partial-coverage weird modal).
- Warning a user via a popover (use an alert).
- Sheet on iOS without a grabber when it's resizable.
- Sheet on iOS with only `medium` detent when the content needs `large`.
- Sheet + Sheet (cascaded) without closing the first.
- macOS sheet that blocks all app windows.
- visionOS sheet emerging from the bottom of the window.
- watchOS sheet for simple yes/no where an alert works.

## SwiftUI

```swift
// Sheet with detents + grabber on iOS.
@State private var showCompose = false

Button("Compose") { showCompose = true }
    .sheet(isPresented: $showCompose) {
        ComposeView()
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
    }

// Alert for a non-recoverable destructive action.
@State private var showDelete = false

Button("Delete Account", role: .destructive) { showDelete = true }
    .alert("Delete this account?",
           isPresented: $showDelete) {
        Button("Delete Account", role: .destructive) { deleteAccount() }
        Button("Cancel", role: .cancel) { }
    } message: {
        Text("This permanently removes all your data. It cannot be undone.")
    }

// Confirmation dialog — renders as action sheet on iOS, menu on macOS.
@State private var askDiscard = false

Button("Cancel", role: .cancel) {
    if hasUnsavedChanges { askDiscard = true } else { dismiss() }
}
.confirmationDialog("Discard your changes?",
                    isPresented: $askDiscard,
                    titleVisibility: .visible) {
    Button("Discard Changes", role: .destructive) { discardAndDismiss() }
    Button("Keep Editing", role: .cancel) { }
}

// Popover (auto-adapts to sheet on compact width iOS).
@State private var showFilters = false

Button { showFilters = true } label: {
    Image(systemName: "line.3.horizontal.decrease.circle")
}
.popover(isPresented: $showFilters) {
    FilterView()
        .frame(idealWidth: 360, idealHeight: 480)
        .presentationCompactAdaptation(.sheet)  // explicit compact behavior
}
```

## UIKit

```swift
// Alert with destructive + cancel.
let alert = UIAlertController(
    title: "Delete this account?",
    message: "This permanently removes all your data. It cannot be undone.",
    preferredStyle: .alert
)
alert.addAction(UIAlertAction(title: "Delete Account", style: .destructive) { _ in
    self.deleteAccount()
})
alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
present(alert, animated: true)

// Action sheet for choice-after-action.
let sheet = UIAlertController(
    title: "Discard your changes?",
    message: nil,
    preferredStyle: .actionSheet
)
sheet.addAction(UIAlertAction(title: "Discard Changes", style: .destructive) { _ in
    self.discardAndDismiss()
})
sheet.addAction(UIAlertAction(title: "Keep Editing", style: .cancel))
// iPad: anchor to the source view.
sheet.popoverPresentationController?.sourceView = cancelButton
present(sheet, animated: true)

// Sheet with detents.
let vc = ComposeViewController()
if let sheetCtl = vc.sheetPresentationController {
    sheetCtl.detents = [.medium(), .large()]
    sheetCtl.prefersGrabberVisible = true
}
present(vc, animated: true)
```

## AppKit

```swift
// Sheet on a window.
let sheetWindow = NSWindow(contentViewController: ComposeViewController())
sheetWindow.setContentSize(NSSize(width: 520, height: 360))
window.beginSheet(sheetWindow) { response in /* handle response */ }

// Alert.
let alert = NSAlert()
alert.messageText = "Delete this account?"
alert.informativeText = "This permanently removes all your data. It cannot be undone."
alert.alertStyle = .warning
alert.addButton(withTitle: "Delete Account")       // default, first
alert.addButton(withTitle: "Cancel")
if #available(macOS 11, *) {
    alert.buttons[0].hasDestructiveAction = true
}
alert.beginSheetModal(for: window) { response in
    if response == .alertFirstButtonReturn { deleteAccount() }
}
```

## See also

- [`./buttons.md`](./buttons.md) — primary / cancel / destructive roles on alert buttons.
- [`./bars.md`](./bars.md) — sheet toolbar placement of Cancel and Done.
- [`../patterns/modality.md`](../patterns/modality.md) (pending) — when going modal is the wrong call.
- [`../patterns/feedback.md`](../patterns/feedback.md) (pending) — non-modal toasts vs. alerts.
