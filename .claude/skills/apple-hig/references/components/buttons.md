# Buttons

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/buttons
**Last verified:** 2026-04-20
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

A button initiates an instantaneous action. Three attributes communicate its function: **style** (how it looks), **content** (label and/or symbol), and **role** (what it means to the system: normal, primary, cancel, destructive).

## Core rules

- **Hit target.** ≥44×44pt on iOS / iPadOS / watchOS, ≥28×28pt on macOS, ≥60×60pt on visionOS, always focusable on tvOS. Apply to the tap region, not the visible frame.
- **Press state on custom buttons.** Never ship a button that doesn't acknowledge a tap. The system's button styles do this automatically.
- **Use style to signal hierarchy, not size.** Same-size buttons in a row communicate peer choices. Different sizes read as inconsistent. Emphasis comes from `.borderedProminent` / `.bordered` / `.plain`, not from making one button bigger.
- **One or two prominent buttons per view.** More prominence = less hierarchy. A view with five "primary" buttons has no primary.
- **Tint only when it earns it.** Accent-tinted backgrounds on Liquid Glass buttons grab attention. If every button is tinted, attention is gone.
- **Meaning before decoration.** If a short label is clearer than an icon, ship a label.

## Content

A button's content is a symbol, a label, or both.

- **SF Symbols first.** Use the symbol that matches the action conventionally: `square.and.arrow.up` for share, `trash` for delete, `magnifyingglass` for search, `plus` for add, `chevron.right` for forward navigation. See [`../foundations/sf-symbols.md`](../foundations/sf-symbols.md) for the full system.
- **Text labels** start with a verb when they describe an action ("Add to Cart", "Send", "Delete Account").
- **Sentence case** for multi-word labels. "Add item" not "Add Item" (unless it's the title of a view).
- **On macOS, append `…`** to any label that opens another window, view, or app. "Save As…", "Rename…", "Find…". Convention-critical on Mac.
- **Tooltips on macOS and visionOS** — the system shows a tooltip on hover/gaze. Set it on icon-only buttons.
- **`.accessibilityLabel` is mandatory** on icon-only buttons.

## Roles

Set a `role` on every button that has semantic meaning:

| Role | What it means | Visual | SwiftUI |
|---|---|---|---|
| Normal | No semantic role | Plain / bordered | `.buttonStyle(.bordered)` |
| Primary | The default button — user's most likely action | Prominent, accent tint | `.buttonStyle(.borderedProminent)` + `.keyboardShortcut(.defaultAction)` |
| Cancel | Dismisses the action | Plain | `Button("Cancel", role: .cancel) { ... }` |
| Destructive | Action destroys data | Red tint | `Button("Delete", role: .destructive) { ... }` |

Rules:

- **Primary responds to Return/Enter** in a temporary view (sheet, alert). Assign it to enable quick confirmation.
- **Never make a destructive action the primary.** Users sometimes hit primary without reading. Keep destructive visually distinct and requiring deliberate confirmation.
- **One primary per view.** More than one primary means no primary.

## Platform-specific rules

### iOS / iPadOS

- Display an **activity indicator inside a button** for actions that don't complete instantly. Optionally swap the label ("Checkout" → "Checking out…"). Better than a separate spinner next to the button.
- **Prominent button for the primary CTA.** Keep others bordered or plain.
- **Don't ship full-width buttons flush to the edges** (iOS convention is insets that respect system margins).

### macOS — button types

macOS has more button flavors than other platforms.

- **Push button** — the standard. Text, symbol, icon, or mix. Can be tinted. Can be the default (Return key). Place in dialogs with trailing-right order: destructive/non-default on leading, primary on trailing.
- **Flexible-height push button** — for 2-line text or tall icons. Same padding/radius as a regular push button.
- **Square / gradient button** — for view-local actions (add / remove rows). Contains symbols only, never text. Goes inside a view (table footer), never in the toolbar or status bar.
- **Help button** — the circular "?" icon. Max one per window. Opens app help, ideally to the topic related to the current view. Position:
  - In a dialog with OK/Cancel: lower corner opposite the dismissal buttons, vertically aligned with them.
  - In a dialog without dismissal buttons: lower-left or lower-right.
  - In a Settings pane: lower-left or lower-right.
- **Image button** — displays an image/symbol/icon inside a view. Never in the window frame — use a toolbar item there. Leave ~10pt padding between image and button edges so clicks register.
- **Spring loading** — for apps with drag-drop. Force-clicking a button while dragging activates it without dropping the items. Support it in drop-target buttons.

### visionOS

Three shapes: **circular**, **capsule** (text-only or text+icon), **rounded rectangle**. Prefer circles and capsules — eyes fixate on corners, so very square shapes strain attention.

Sizes (pt):

| Shape | Mini 28 | Small 32 | Regular 44 | Large 52 | XL 64 |
|---|---|---|---|---|---|

- **Visible background fill.** Always. The only exception is a button already inside a toolbar, context menu, alert, or popover — the container material provides contrast.
  - On a glass window: use the **regular** material for the button background.
  - Floating in space: use an **elevated** material.
- **Do not ship a white-on-black custom button.** The system reserves that for the toggled state.
- **≥60pt between button centers.** Closer = gaze can't separate them and hover effects overlap.
- **Button type per layout:** rounded rectangle in a vertical stack; capsule in a horizontal row.
- **No custom hover effects.** The system handles focus visuals.
- **Sound is the feedback.** visionOS has no haptics. Use standard controls to get the system's audible feedback for free.

### tvOS

Standard button types. Focus-driven. Size is driven by the focus engine; focused item scales. Make sure surrounding layout has padding for the scale-up.

### watchOS

- **Full-width text buttons** for primary actions. Look better, easier to tap.
- **Same height for stacks** of one- and two-line text buttons. Visual consistency.
- **Inline buttons** use the capsule shape with a material background for contrast against content.
- **Corner toolbar buttons** stay above scrolling content and the system moves the time/title to accommodate.
- **Scrolling toolbar buttons** hide by default, revealed by scrolling up. Use for important-but-secondary actions; primary actions should be full-width.

## Common mistakes

- Custom button with no pressed state.
- Three "primary" buttons in the same view.
- Tiny tap targets (<44×44pt on iOS).
- Destructive action as the primary button of an alert.
- iOS buttons stretched edge-to-edge against the safe area.
- Inventing a custom "Back" or "Cancel" button instead of using the role.
- macOS button with no Return shortcut for primary, or no `.cancelAction` on the Cancel button.
- macOS "Save as…" without the ellipsis.
- Missing `accessibilityLabel` on an icon-only button.
- visionOS button too close to another (<60pt center-to-center).
- visionOS white-on-black custom button (conflicts with the toggled state).
- Spinner beside a button while its label is static, instead of the in-button spinner pattern.

## SwiftUI

```swift
// iOS / iPadOS — primary + secondary in a form.
VStack(spacing: 12) {
    Button("Save changes") { save() }
        .buttonStyle(.borderedProminent)
        .controlSize(.large)
        .keyboardShortcut(.defaultAction)
        .disabled(!isValid)

    Button("Cancel", role: .cancel) { dismiss() }
        .buttonStyle(.bordered)
}

// Icon-only with accessibility label.
Button(action: newItem) {
    Image(systemName: "plus")
}
.accessibilityLabel("New item")

// Progress inside a button.
Button(action: submit) {
    HStack {
        if isSubmitting { ProgressView() }
        Text(isSubmitting ? "Submitting…" : "Submit")
    }
}
.buttonStyle(.borderedProminent)
.disabled(isSubmitting)

// Destructive confirmation flow.
Button("Delete account", role: .destructive) { showingConfirm = true }
    .confirmationDialog("Delete this account?",
                        isPresented: $showingConfirm,
                        titleVisibility: .visible) {
        Button("Delete account", role: .destructive) { deleteAccount() }
        Button("Cancel", role: .cancel) { }
    } message: {
        Text("This will permanently remove all your data. This cannot be undone.")
    }
```

## UIKit

```swift
// iOS 15+ UIButton.Configuration — modern, declarative, accessible.
var cfg = UIButton.Configuration.borderedProminent()
cfg.title = "Save changes"
cfg.baseBackgroundColor = .tintColor
cfg.cornerStyle = .large
let save = UIButton(configuration: cfg, primaryAction: UIAction { _ in self.save() })
save.role = .primary            // iOS 14+
save.accessibilityLabel = "Save changes"

// Activity indicator inside the button.
save.configurationUpdateHandler = { button in
    var cfg = button.configuration
    cfg?.showsActivityIndicator = self.isSaving
    cfg?.title = self.isSaving ? "Saving…" : "Save changes"
    button.configuration = cfg
}
```

## AppKit

```swift
// Push button with default action and Cancel.
let save = NSButton(title: "Save Changes", target: self, action: #selector(save))
save.bezelStyle = .rounded
save.keyEquivalent = "\r"                  // Return key
save.setAccessibilityLabel("Save Changes")

let cancel = NSButton(title: "Cancel", target: self, action: #selector(cancelEdit))
cancel.bezelStyle = .rounded
cancel.keyEquivalent = "\u{1b}"            // Escape

// Help button.
let help = NSButton()
help.bezelStyle = .helpButton
help.target = self
help.action = #selector(showHelp)

// Destructive — red tint via button role in modern AppKit.
if #available(macOS 14, *) {
    let delete = NSButton(title: "Delete", target: self, action: #selector(delete))
    delete.hasDestructiveAction = true
}

// macOS naming convention: ellipsis on anything that opens a dialog/sheet.
let saveAs = NSButton(title: "Save As…", target: self, action: #selector(saveAs))
```

## See also

- [`../foundations/sf-symbols.md`](../foundations/sf-symbols.md) — pick the right glyph.
- [`../foundations/color.md`](../foundations/color.md) — tint accent usage.
- [`../foundations/accessibility.md`](../foundations/accessibility.md) — hit target floors.
- [`./bars.md`](./bars.md) — toolbar item buttons and primary actions.
- [`./sheets-and-popovers.md`](./sheets-and-popovers.md) — dismissing sheets via Primary/Cancel roles.
- [`./pickers-and-menus.md`](./pickers-and-menus.md) — menu buttons, pull-down/pop-up buttons.
