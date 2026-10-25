# Pickers, menus, pull-down and pop-up buttons

**HIG sources:**
- https://developer.apple.com/design/human-interface-guidelines/menus
- https://developer.apple.com/design/human-interface-guidelines/pickers
- https://developer.apple.com/design/human-interface-guidelines/pull-down-buttons
- https://developer.apple.com/design/human-interface-guidelines/pop-up-buttons
- Date pickers: https://developer.apple.com/design/human-interface-guidelines/pickers (section)

**Last verified:** 2026-04-20
**Applies to:** varies per component

Four controls that all present choices — picking the right one is the whole job.

| Control | Purpose | Use when |
|---|---|---|
| **Menu** | Reveal commands / options on demand | Many items, can include submenus, keyboard shortcuts, destructive items |
| **Pull-down button** | A button that reveals a menu of **commands** related to the button | "Add…" that lets people pick what to add; "More…" overflow |
| **Pop-up button** | A button that reveals a menu of **mutually exclusive options**; button label updates to the selection | Picking paper size, sort field, theme |
| **Picker** | Scrollable list(s) of values — medium-to-long | Country, time zone, date/time |
| **Date picker** | Specialized picker for dates, times, or both | Anything calendrical |
| **List / table** | Very large sets, with grouping, search, or sections | ≥25–40 items, or hierarchy |

## Menus

A menu reveals items when the user opens it. Items are commands, options, or state indicators.

### Labels

- **Describe what the item does.** Verb or verb phrase: "View", "Close", "Select All".
- **Title case** (all caps except articles, short prepositions, short coordinating conjunctions — plus the last word). This matches platform convention even if your app's body copy is sentence case.
- **Drop articles** (a, an, the) to save space and improve scanability.
- **Unavailable items stay visible but dimmed.** Never hide them based on state. If the whole menu is unavailable, keep the menu itself available so people can see what's in it.
- **Append `…`** when the item needs more input (opens a dialog, sheet, or other view). Examples: "Rename…", "Print…", "Find…".

### Icons (SF Symbols)

- **Use familiar SF Symbols** for standard actions (Copy, Share, Delete) across the whole app — consistency matters more than cleverness.
- **Don't ship an icon you can't justify.** No ornamental icons.
- **One icon per group, not per item.** The first item in a group carries the symbol; the rest rely on text.

### Organization

- **Important / frequent items first.** People scan from top.
- **Group related items** with separators (visual divider or gap). Keep "Copy / Cut / Paste" together even when Paste and Match Style is less-used.
- **Keep menu length manageable.** If long, split into multiple menus, or push niche items into a submenu.
- **User-generated or dynamic content** (bookmarks, recents) may be arbitrarily long — people expect that. Scrolling is fine.

### Submenus

- **Use sparingly.** Each layer hides items and adds interaction cost.
- **Single level deep** is the rule. Multi-level submenus are hard to land on.
- **≤~5 items** in a submenu — beyond that, make a new menu.
- **Prefer a submenu to indented items.** Indentation is off-platform.
- **Trigger on "Sort by", "New", "Convert"**-style labels that clearly hint at what's inside.

### Toggled items

- **Changeable label** when the meaning is clear: "Show Map" ↔ "Hide Map".
- **Verb prefix** when the state isn't obvious: "Turn HDR On" rather than "HDR On".
- **Checkmark** for mutually-inclusive states (fonts, formatting) — scanning for checks is easy.
- **Both items at once** when showing both states helps: "Take Account Online" / "Take Account Offline" — only the applicable one is enabled.

### iOS / iPadOS layouts

iOS menus have three layout styles:

- **Small** — 4 symbol-only items on top, list below. Use only for tightly related actions (Bold / Italic / Underline / Strikethrough).
- **Medium** — 3 labeled items with symbols on top, list below. Use when three actions are heavy-use (Notes: Scan / Lock / Pin).
- **Large (default)** — straight list.

### macOS / iPadOS specifics

- **Menu bar menus** — see [`../platforms/macos.md`](../platforms/macos.md#menu-bar--first-class). Every command in the app lives there, ordered predictably.
- **Keyboard shortcuts** on menu items are a contract. Standard assignments: ⌘N, ⌘O, ⌘S, ⌘W, ⌘Z, ⌘F, ⌘,.
- **Context menu** (right-click) pulls relevant commands — should reflect menu bar, not invent.

### visionOS

- **Display menu near the content it controls.** Users must look at an item to tap it — distance means misses.
- **Breakthrough effect** options: `subtle` (default, blends), `prominent`, `occluded`. Prefer subtle; use prominent only when the menu must stand out over 3D content, and occluded only when gameplay hides menus behind terrain.
- Supports small and large layouts (no medium).

## Pull-down buttons

A button whose menu contains **commands** directly related to the button. After picking, the menu closes and the action runs.

### Rules

- **Commands, not mutually-exclusive options.** For options, use a pop-up button.
- **Don't hide all the view's actions** behind a single pull-down. Primary actions must be discoverable.
- **Three-or-more items** to be worth the interaction. Fewer → use regular buttons or toggles.
- **No title unless it adds meaning** — the button content + menu items usually say enough.
- **Menu items can be destructive** (red text); confirm with an action sheet (iOS) / alert (iPadOS / macOS).
- **SF Symbol after the label** when it clarifies the item.

### Platform availability

- iOS, iPadOS, macOS, visionOS. **Not supported on tvOS or watchOS.**

## Pop-up buttons

A button whose menu presents **mutually exclusive options**. Button label updates to reflect the selection.

### Rules

- **Mutually exclusive, flat list.** No submenus, no multi-select — if you need those, use a menu.
- **Useful default** selection.
- **Introductory label or button label** so users can guess options without opening.
- **Space-efficient** — good when you don't want to take up room with radio buttons.
- **Custom option** is fine if it lets users access less-common values without cluttering.

### Platform availability

- iOS, iPadOS, macOS, visionOS. **Not supported on tvOS or watchOS.**

## Pickers (general)

Scrollable list(s) of distinct values. The system provides several picker styles per platform.

### When to use

- **Medium-to-long lists (10–50 items)** where users can predict the ordering (countries, time zones, currencies).
- **Short lists** → use a pop-up button or segmented control instead; a picker over-weights the UI.
- **Very long / sectioned / searchable lists** → use a list or table instead.

### Rules

- **Predictable, logically ordered values.** Alphabetize, or sort by a dimension users understand.
- **Don't switch views to show a picker.** Display it inline or in a popover near the field. On iPhone it appears at the bottom sheet-style or inline.

## Date pickers

### Modes

- **Date** — month / day / year.
- **Time** — hours / minutes / (AM/PM).
- **Date and time** — both.
- **Countdown timer** — hours + minutes, max 23:59. Not available in inline/compact styles.

### iOS / iPadOS styles

- **Compact** (default on small fields) — a pill button showing the value; tap opens a popover calendar.
- **Inline** — shows the calendar in place.
- **Wheels** — scroll wheels; supports keyboard input.
- **Automatic** — system picks based on context.

### Rules

- **Use compact when space is tight** — it opens to a familiar calendar modal.
- **Set an appropriate minute interval** when precision isn't every minute. `1, 5, 10, 15, 30` all divide 60 evenly.
- **Constrain with `in: ClosedRange`** when some dates are invalid (future-only, within-year, etc.) — blocks user errors up front.
- **Localize** — the system handles day/month order per locale. Don't hardcode "MM/DD/YYYY" anywhere.

### macOS styles

- **Textual** — editable text like "1/15/2026". Use when space is limited or users will type specific values.
- **Graphical** — calendar-style with clock face (if time is included). Use when browsing/ranging.

### watchOS

- Wheels style; user scrolls with the Digital Crown. Precise and engaging.

## Common mistakes

- Pull-down button containing mutually-exclusive options (should be pop-up).
- Pop-up button used for commands (should be pull-down or menu).
- Menu with 40 top-level items instead of grouping into submenus.
- Picker for a 3-item list (use segmented control or pop-up).
- List for 20 country options when a picker is better.
- Hiding unavailable menu items instead of dimming.
- Menu items without `…` that open a dialog.
- Sentence-case menu item labels on macOS (convention is title case).
- Indented menu items instead of a submenu.
- Custom date picker instead of the system one (breaks localization).
- Wheels date picker on macOS (use textual or graphical).
- Picker values in non-predictable order.

## SwiftUI

```swift
// Menu / pull-down — a button that reveals commands.
Menu {
    Button { add(.folder) } label: { Label("New Folder", systemImage: "folder") }
    Button { add(.file)   } label: { Label("New File",   systemImage: "doc") }
    Divider()
    Button("Import from File…") { showImport = true }
    Button("Paste from Clipboard", action: pasteFromClipboard)
        .keyboardShortcut("v", modifiers: [.command])
    Divider()
    Button("Delete All", role: .destructive) { askDeleteAll = true }
} label: {
    Label("Add", systemImage: "plus")
}

// Pop-up button — mutually exclusive. `Picker` + `.menu` style is the API.
@State private var sortField: SortField = .title

Picker("Sort by", selection: $sortField) {
    ForEach(SortField.allCases) { field in
        Text(field.label).tag(field)
    }
}
.pickerStyle(.menu)

// Segmented — pop-up-like but best for very short lists, always-visible.
Picker("Style", selection: $style) {
    Text("List").tag(Style.list)
    Text("Grid").tag(Style.grid)
    Text("Map").tag(Style.map)
}
.pickerStyle(.segmented)

// Picker with system styles per platform.
Picker("Country", selection: $country) {
    ForEach(Country.all) { c in Text(c.name).tag(c) }
}
.pickerStyle(.wheel)    // iOS wheel, macOS pop-up menu

// Date picker — compact on iOS, constrained, minute stride.
DatePicker("Start",
           selection: $start,
           in: Date.now...Date.now.addingTimeInterval(60*60*24*365),
           displayedComponents: [.date, .hourAndMinute])
    .datePickerStyle(.compact)

DatePicker("Date", selection: $date, displayedComponents: .date)
    .datePickerStyle(.graphical)          // macOS-friendly

// Time with 15-minute intervals.
DatePicker("At",
           selection: $time,
           displayedComponents: .hourAndMinute)
    .environment(\.minuteInterval, 15)    // custom env; alt: use Formatter

// Menu with checkmark-style toggles.
Menu("Font Style") {
    Button { toggle(.bold) } label: {
        Label("Bold", systemImage: styles.contains(.bold) ? "checkmark" : "")
    }
    Button { toggle(.italic) } label: {
        Label("Italic", systemImage: styles.contains(.italic) ? "checkmark" : "")
    }
}
```

## UIKit

```swift
// UIButton with a menu — pull-down style.
var cfg = UIButton.Configuration.bordered()
cfg.title = "Add"
cfg.image = UIImage(systemName: "plus")
let add = UIButton(configuration: cfg)

add.menu = UIMenu(children: [
    UIAction(title: "New Folder", image: UIImage(systemName: "folder")) { _ in self.add(.folder) },
    UIAction(title: "New File",   image: UIImage(systemName: "doc"))    { _ in self.add(.file) },
    UIMenu(options: .displayInline, children: [
        UIAction(title: "Import from File…") { _ in self.showImport() },
    ]),
    UIAction(title: "Delete All", attributes: .destructive) { _ in self.askDeleteAll() }
])
add.showsMenuAsPrimaryAction = true

// Pop-up (single-selection menu).
add.changesSelectionAsPrimaryAction = true          // turns it into a pop-up

// Date picker — compact.
let picker = UIDatePicker()
picker.preferredDatePickerStyle = .compact
picker.datePickerMode = .dateAndTime
picker.minimumDate = .now
picker.minuteInterval = 15
```

## AppKit

```swift
// NSMenu with shortcuts.
let sort = NSMenuItem(title: "Sort", action: nil, keyEquivalent: "")
let sub = NSMenu()
sub.addItem(withTitle: "By Title", action: #selector(sortByTitle(_:)), keyEquivalent: "1")
    .keyEquivalentModifierMask = [.command, .option]
sub.addItem(withTitle: "By Date",  action: #selector(sortByDate(_:)),  keyEquivalent: "2")
    .keyEquivalentModifierMask = [.command, .option]
sort.submenu = sub

// NSPopUpButton (pop-up behavior).
let theme = NSPopUpButton(frame: .zero, pullsDown: false)
theme.addItems(withTitles: ["Automatic", "Light", "Dark"])
theme.selectItem(withTitle: defaultTheme)

// NSPopUpButton with pullsDown: true → pull-down behavior.
let add = NSPopUpButton(frame: .zero, pullsDown: true)
add.addItems(withTitles: ["Add", "New Folder", "New File", "Import from File…"])

// NSDatePicker — textual vs graphical.
let date = NSDatePicker()
date.datePickerStyle = .textField               // or .clockAndCalendar for graphical
date.datePickerElements = [.yearMonthDayDatePickerElementFlag]
```

## See also

- [`./buttons.md`](./buttons.md) — primary and destructive roles, single actions.
- [`./bars.md`](./bars.md) — menu bar + toolbar buttons host menus.
- [`./sheets-and-popovers.md`](./sheets-and-popovers.md) — action sheets / confirmation dialogs for destructive menu picks.
- [`../foundations/sf-symbols.md`](../foundations/sf-symbols.md) — item icons.
- [`../platforms/macos.md`](../platforms/macos.md#menu-bar--first-class) — menu bar is authoritative on Mac.
