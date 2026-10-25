# Segmented controls

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/segmented-controls
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS
**Not supported:** watchOS

A segmented control is a linear set of two or more equal-width segments, each acting like a button. Use it for **mutually exclusive selection** among a small set, or (macOS only) for multiple simultaneous selection, or as a button group for actions without selection state.

## When to use

- **Switching between closely related subviews** within the same screen: "List / Grid / Map", "Day / Week / Month".
- **Single-choice selection** over a small set where all options should be visible.
- **macOS multi-select** (e.g., Bold / Italic / Underline in Keynote's font attributes).
- **macOS button group** in a toolbar or inspector (Reply / Reply All / Forward).

## When not to use

- **Top-level app sections** — use a tab bar or sidebar.
- **More than about 5–7 options** in a wide layout (or ~5 on iPhone). Too many segments become a smear.
- **Mixed selection and action semantics.** Either all segments toggle a selection state, or all segments perform actions. Never mix.

## Rules

- **All segments roughly equal width.** The control feels balanced because of this. Keep content sizes similar across segments.
- **Either text or images, not both in the same control.** Mixing reads as disconnected and uneven.
- **Nouns or noun phrases** as labels, title case. No introductory label needed when labels are self-describing.
- **Limit to 5–7 segments** (5 max on iPhone). More than that → use a `Picker`, a menu, or a split view.
- **Don't mix action and state.** A segmented control either tracks a selection or fires actions — never both.
- **Keep one type of interaction per instance.** Selection-tracking segments share one model; action-segments share another. Two segmented controls are better than one that mixes.

## Platform specifics

### iOS / iPadOS

- **Subview switching inside a screen** is the idiomatic use (Calendar "Event / Reminder" segment in the New Event sheet).
- **Never a replacement for tab bars or sidebars** — these are top-level navigation; segmented controls are in-view refinement.
- **Live updates** — the view under the segmented control reflects the new segment immediately; don't require a separate "Apply" button.

### macOS

- **Introductory text** or per-segment labels (below icons) help clarify. Tooltips are expected on icon-only controls.
- **Tab views, not segmented controls**, for top-level view switching in a window. Segmented controls belong in toolbars and inspectors.
- **Spring loading** (Magic Trackpad force click) — consider supporting it on segmented controls that are drop targets; users can drag items onto a segment and force-click to activate without dropping.

### tvOS

- **Prefer split views over segmented controls for content filtering.** Split views are easier to navigate with the Siri Remote.
- **Focus activates, not clicks.** Segments change selection as focus moves to them. Put enough space around the control that neighboring focusable elements don't get hit accidentally.

### visionOS

- **Tooltips on gaze.** If segments use icons, supply descriptive text — the system shows it as a tooltip when the user looks at the segment.

## Common mistakes

- Segmented control with 8+ options — users lose place.
- Mixing text and icons across segments.
- Segments with very different content widths (one short word, one long phrase).
- Using a segmented control for top-level nav on iOS instead of a tab bar.
- Using a segmented control where a menu or picker better fits (long lists, mutually exclusive).
- Mixing "select a view" and "perform an action" semantics in the same control.
- No live update — selecting a segment does nothing until the user taps an Apply button.

## SwiftUI

```swift
// `Picker` with `.segmented` style is the idiomatic segmented control.
enum ListStyle { case list, grid, map }
@State private var style: ListStyle = .list

Picker("Layout", selection: $style) {
    Text("List").tag(ListStyle.list)
    Text("Grid").tag(ListStyle.grid)
    Text("Map").tag(ListStyle.map)
}
.pickerStyle(.segmented)
.labelsHidden()

// Icon-based segmented control with tooltips (tooltips auto on macOS/visionOS via .help).
Picker("View", selection: $mode) {
    Image(systemName: "list.bullet").tag(Mode.list).help("List view")
    Image(systemName: "square.grid.2x2").tag(Mode.grid).help("Grid view")
    Image(systemName: "map").tag(Mode.map).help("Map view")
}
.pickerStyle(.segmented)
.labelsHidden()

// macOS-style action button group: a ToolbarItemGroup with three buttons achieves the same reading.
ToolbarItemGroup(placement: .primaryAction) {
    Button("Reply", systemImage: "arrowshape.turn.up.left") { reply() }
    Button("Reply All", systemImage: "arrowshape.turn.up.left.2") { replyAll() }
    Button("Forward", systemImage: "arrowshape.turn.up.right") { forward() }
}
```

## UIKit

```swift
let segmented = UISegmentedControl(items: ["List", "Grid", "Map"])
segmented.selectedSegmentIndex = 0
segmented.addTarget(self, action: #selector(styleChanged(_:)), for: .valueChanged)

// Icon-only with tooltips (iPadOS / Mac Catalyst).
let icons = UISegmentedControl(items: [
    UIImage(systemName: "list.bullet")!,
    UIImage(systemName: "square.grid.2x2")!,
    UIImage(systemName: "map")!
])
icons.setToolTip("List view", forSegmentAt: 0)
icons.setToolTip("Grid view", forSegmentAt: 1)
icons.setToolTip("Map view", forSegmentAt: 2)
```

## AppKit

```swift
let seg = NSSegmentedControl(labels: ["Reply", "Reply All", "Forward"],
                             trackingMode: .momentary,
                             target: self,
                             action: #selector(segmentActivated(_:)))

// Multi-select (Bold / Italic / Underline).
let styles = NSSegmentedControl(images: [
    NSImage(systemSymbolName: "bold", accessibilityDescription: "Bold")!,
    NSImage(systemSymbolName: "italic", accessibilityDescription: "Italic")!,
    NSImage(systemSymbolName: "underline", accessibilityDescription: "Underline")!
], trackingMode: .selectAny, target: self, action: #selector(stylesChanged(_:)))
```

## See also

- [`./pickers-and-menus.md`](./pickers-and-menus.md) — when a picker or menu beats a segmented control.
- [`./bars.md`](./bars.md) — tab bars are not segmented controls.
- [`./buttons.md`](./buttons.md) — action button groups on macOS.
- [`./split-views.md`](./split-views.md) — for tvOS filtering, use a split view not a segmented control.
