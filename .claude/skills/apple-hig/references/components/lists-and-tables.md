# Lists and tables

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/lists-and-tables
**Last verified:** 2026-04-20
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

Lists and tables present rows of data. Lists are for navigating hierarchy and picking one of many options (Settings, Inbox). Multi-column tables are for productivity data (spreadsheets, logs).

## When to use what

- **List** — single-column or simple row content; hierarchy; picking; editing; Settings-style. iOS/iPadOS inside a `NavigationStack` or `Form`; macOS in a sidebar or list-style split column.
- **Table** — multi-column, sortable, resizable, for data that has multiple attributes per row (spreadsheets, file browsers, log viewers).
- **Outline view (macOS)** — hierarchical data with disclosure triangles. Use instead of nesting tables.
- **Collection / grid** — use when items vary widely in size or you have many images. See [`./collections-and-scrolling.md`](./collections-and-scrolling.md) (pending).

## Rules

- **Prefer text** in a list. Row-based layout is for scanning text fast. Photo-heavy content belongs in a collection.
- **Keep row text succinct** to minimize wrap/truncation. If rows need long content, show a title in the row and reveal detail on tap.
- **Support edit** (reorder at minimum). iOS/iPadOS enters edit mode first; macOS uses drag-drop and commit actions.
- **Feedback on selection:**
  - Navigational list (drill in) → **persistent** highlight on the current row.
  - Option list → brief highlight, then a checkmark (or similar) to show it's selected.
- **Column headings** use nouns or short noun phrases in title case, no ending punctuation. If a single-column table has no header, label the context with a surrounding header/footer.
- **Preserve recognizability when truncation happens.** Sometimes a middle ellipsis works better than a trailing one (keeps both ends visible).
- **Info button ≠ drill-in.** The circled `ⓘ` is "more about this row"; hierarchical drill uses the chevron disclosure accessory.

## List styles

Choose the style that matches the content and platform.

| Style | When | SwiftUI |
|---|---|---|
| `.automatic` | Default, adapts per context | `.listStyle(.automatic)` |
| `.plain` | Inboxes, feeds, non-grouped content | `.listStyle(.plain)` |
| `.inset` | Inset edges on iOS and iPadOS | `.listStyle(.inset)` |
| `.insetGrouped` | iOS Settings-style; grouped sections on inset cards | `.listStyle(.insetGrouped)` |
| `.grouped` | Classic grouped list, rarely preferred over insetGrouped | `.listStyle(.grouped)` |
| `.sidebar` | macOS sidebar, iPadOS sidebar column | `.listStyle(.sidebar)` |
| `.bordered` | macOS multi-column tables with alternating rows | `.listStyle(.bordered)` |
| `.elliptical` | watchOS scroll-surface feel | (watchOS only) |

## iOS and iPadOS

- **Enter edit mode** to select multiple rows, reorder, or delete. `EditButton()` in SwiftUI.
- **Swipe actions** for common row actions (archive, delete, flag). Trailing edge for destructive and frequent; leading edge for positive/secondary.
- **Swipe deletes full-swipe** when you allow it — but only if the action is non-destructive or clearly recoverable.
- **Row accessories:** chevron for drill-in (`.disclosureIndicator`), info button (`ⓘ`) for contextual detail, checkmark for selection.
- **Don't put an info button beside a chevron** in the same row unless both have distinct destinations.
- **Don't mix an index** (alphabet rail on the trailing side) with trailing-edge accessories — people can't activate one without hitting the other.

## macOS

- **Click column headings to sort.** Clicking a sorted column flips direction.
- **Resizable columns** — users adjust column width; persist their choice.
- **Alternating row backgrounds** help scanning in wide multi-column tables (`NSColor.alternatingContentBackgroundColors`).
- **Outline view for hierarchy** — `NSOutlineView` / SwiftUI `List { OutlineGroup(...) }`. Use disclosure triangles, not indentation alone.
- **Double-click to open** detail views; Enter/Return to open highlighted.
- **Context menus** (right-click) for row actions. Every action has a menu bar equivalent.
- **Drag-drop to reorder, move, or copy** between views and apps.
- **Keyboard nav first-class** — arrow keys traverse, spacebar toggles, cmd+a selects all, cmd+delete deletes.

## tvOS

- **Focused row scales and rounds.** Leave padding around rows for the scale animation; don't let focus overlap neighboring images.
- **Don't pre-mask** image corners — the focus engine rounds focused rows, and your mask fights it.
- **Keep rows visually distinct** — focus affordance needs to register without ambiguity.

## watchOS

- **Short lists are better** — scrolling fatigue is real. Cap rows where it makes sense and provide "See all" for long lists.
- **Elliptical style** (`.listStyle(.elliptical)`) gives the "rolling off a rounded surface" feel when rows scroll.
- **Vertical page-based navigation** only works when detail views are short (non-scrolling). If your detail scrolls, people can't swipe between rows.

## visionOS

Follows iOS list rules. Info buttons: same rule — they reveal detail, not navigate. Swipe actions work via pinch-drag.

## Common mistakes

- Using a list for a grid of photos — use a collection.
- Rows with giant text blocks — show a title and route to detail.
- Info button used for drill-in (use chevron).
- iOS index + trailing accessories on the same row.
- macOS table without click-to-sort or column resizing.
- macOS hierarchy rendered as nested lists instead of an outline view.
- No swipe actions on iOS inbox-style content.
- tvOS rows without enough padding for focus scaling.
- Persistent highlighting on a "pick one" option list (use checkmark instead).
- Column headers with trailing punctuation.
- Bail-out text ("No items") without explanation when the section is empty.

## SwiftUI

```swift
// iOS / iPadOS — inbox with swipe actions, edit mode, and drill-in.
struct InboxList: View {
    @State private var store = InboxStore()
    @State private var selection = Set<Message.ID>()

    var body: some View {
        NavigationStack {
            List(selection: $selection) {
                ForEach(store.messages) { message in
                    NavigationLink(value: message.id) {
                        MessageRow(message: message)
                    }
                    .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                        Button(role: .destructive) { store.archive(message.id) } label: {
                            Label("Archive", systemImage: "archivebox")
                        }
                    }
                    .swipeActions(edge: .leading) {
                        Button { store.markRead(message.id) } label: {
                            Label("Read", systemImage: "envelope.open")
                        }
                        .tint(.blue)
                    }
                }
                .onMove(perform: store.move)
            }
            .listStyle(.plain)
            .navigationTitle("Inbox")
            .navigationDestination(for: Message.ID.self) { MessageDetail(id: $0) }
            .toolbar { EditButton() }
        }
    }
}

// iOS Settings-style grouped list with sections.
List {
    Section("General") {
        NavigationLink("Appearance") { AppearanceSettings() }
        NavigationLink("Notifications") { NotificationSettings() }
    }
    Section("Account") {
        NavigationLink("Profile") { ProfileSettings() }
        Button("Sign Out", role: .destructive) { signOut() }
    }
}
.listStyle(.insetGrouped)

// macOS — sortable multi-column Table with resizable columns and a
// context menu.
Table(of: Book.self, selection: $selection, sortOrder: $sortOrder) {
    TableColumn("Title", value: \.title) { b in Text(b.title) }
    TableColumn("Author", value: \.author)
    TableColumn("Date", value: \.date) { b in Text(b.date, format: .dateTime) }
        .width(min: 100, ideal: 140)
} rows: {
    ForEach(books) { book in
        TableRow(book)
            .contextMenu {
                Button("Open") { open(book) }
                Divider()
                Button("Delete", role: .destructive) { delete(book) }
            }
    }
}
.onChange(of: sortOrder) { _, new in books.sort(using: new) }

// macOS — hierarchical outline.
List {
    OutlineGroup(categories, children: \.subcategories) { cat in
        Label(cat.name, systemImage: cat.systemImage)
    }
}

// watchOS
List(items) { ItemRow(item: $0) }
    .listStyle(.elliptical)
```

## UIKit

```swift
// UICollectionView with list configuration — preferred over UITableView today.
let config = UICollectionLayoutListConfiguration(appearance: .insetGrouped)
let layout = UICollectionViewCompositionalLayout.list(using: config)
let collection = UICollectionView(frame: .zero, collectionViewLayout: layout)

// Cell registration uses UIListContentConfiguration for built-in styles.
let reg = UICollectionView.CellRegistration<UICollectionViewListCell, Message> { cell, _, message in
    var cfg = cell.defaultContentConfiguration()
    cfg.text = message.sender
    cfg.secondaryText = message.preview
    cfg.image = UIImage(systemName: message.isRead ? "envelope.open" : "envelope.fill")
    cell.contentConfiguration = cfg
    cell.accessories = [.disclosureIndicator()]
}

// Swipe actions via UICollectionLayoutListConfiguration.
var c = config
c.trailingSwipeActionsConfigurationProvider = { indexPath in
    let archive = UIContextualAction(style: .destructive, title: "Archive") { _, _, done in
        self.archive(at: indexPath); done(true)
    }
    archive.image = UIImage(systemName: "archivebox")
    return UISwipeActionsConfiguration(actions: [archive])
}
```

## AppKit

```swift
// NSTableView with sorting, resizable columns, alternating backgrounds.
tableView.usesAlternatingRowBackgroundColors = true
tableView.allowsColumnResizing = true
tableView.sortDescriptors = [NSSortDescriptor(key: "title", ascending: true)]

// NSOutlineView for hierarchy.
let outline = NSOutlineView()
outline.indentationPerLevel = 16
outline.autoresizesOutlineColumn = true
outline.doubleAction = #selector(openItem(_:))
```

## See also

- [`./bars.md`](./bars.md) — toolbar + nav bar for list views.
- [`./split-views.md`](./split-views.md) (pending) — sidebar + list + detail.
- [`./sheets-and-popovers.md`](./sheets-and-popovers.md) — compose sheets, inspector popovers.
- [`../patterns/search.md`](../patterns/search.md) (pending) — `.searchable` on lists.
- [`../foundations/accessibility.md`](../foundations/accessibility.md) — VoiceOver rotor, custom actions.
