# Split views

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/split-views
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS (see platform rules — compact iOS is a bad fit)

A split view shows **multiple adjacent panes of content at once** and supports navigation between them. It's the scaffolding for Mail, Messages, Files, Keynote, Notes — two or three columns where a selection in the leading pane determines what the trailing pane shows.

## When to use

- **Two or three levels of hierarchy** visible at once (sidebar → list → detail).
- **macOS and iPadOS regular size class** — the natural fit.
- **Supplementary panes around a main canvas** — Keynote uses split view panes for the slide navigator, presenter notes, and inspector.
- **Not iPhone / iOS compact.** There isn't enough horizontal space. Use a `NavigationStack` instead.

## Common configurations

| Panes | Typical use |
|---|---|
| **2 columns** (sidebar + detail) | Mail on iPhone-in-landscape, simple note apps |
| **3 columns** (sidebar + list + detail) | Mail on iPad/Mac, Messages, Notes |
| **3-pane editor canvas** (navigator + canvas + inspector) | Keynote, Pages, Xcode, visual editors |

## Rules

- **Persistently highlight the current selection** in each pane that drives the next. The highlight is how people track what they're looking at across columns.
- **Selection in column N fills column N+1.** No floating-in-from-a-sheet surprises.
- **Drag and drop between panes.** People use split views to move items between scopes — make it work.
- **Preserve selection across launches** when meaningful (the mailbox they were in, the project they last touched).
- **Empty states matter.** When the detail pane has nothing selected, use `ContentUnavailableView` or equivalent — don't ship a bare empty canvas.

## Platform specifics

### iOS

- **Prefer regular environments** (iPad, some iPhones in landscape). In a compact environment (iPhone portrait), the split view collapses to a `NavigationStack`.
- If you support both, design the stack version carefully — the detail must have a back route that matches the split-view pattern.

### iPadOS

- **2-pane or 3-pane.** Mail: 2-pane. Keynote: 3-pane with the navigator, canvas, and inspector. Both are fine; pick based on the information architecture.
- **Account for narrow, compact, and intermediate widths.** iPad windows resize fluidly in Stage Manager and Split Screen. The layout must still be navigable when narrow.
- **Hide tertiary columns first** as width decreases. Keep the primary and detail visible as long as possible.
- **Edge swipe from leading** reveals a hidden sidebar. `NavigationSplitView` does this automatically.
- **Convertible sidebar ↔ tab bar.** See [`./bars.md`](./bars.md#tab-bars) and `TabView(.sidebarAdaptable)`.

### macOS

- **Vertical, horizontal, or combined arrangement.** Sidebar + list + detail is the common pattern, all vertical dividers.
- **Draggable dividers** — users expect to resize. Use the thin divider style (1pt) unless content on either side needs more visual separation.
- **Sensible min/max pane sizes.** If a pane can shrink to zero width, the divider disappears and becomes un-grabbable.
- **Collapsible panes when appropriate.** Editing-focused apps let users hide the sidebar and inspector to get more canvas.
- **Multiple ways to reveal a hidden pane.** Toolbar button + View menu item + keyboard shortcut. Users remember at least one.
- **Use `NavigationSplitView`** in SwiftUI; `NSSplitViewController` with sidebar items in AppKit.

### tvOS

- **Content filtering works well** — filter categories in the primary pane, results in the secondary.
- **Default proportion:** 1/3 primary, 2/3 secondary. A half/half layout is also possible.
- **Single title above the whole split view.** Don't title each pane — users know what's in it.
- **Title alignment follows content type:** center for collections in the secondary; leading-aligned for a single primary-content view.

### visionOS

- **Prefer a split view to a new window** for supplementary information — keeps users in context. New windows require you to manage spatial relationships.
- **For small, focused tasks**, prefer a sheet instead of opening a new window.

### watchOS

- **Split view = list view or detail view as a full-screen view.** The two don't sit side-by-side; one replaces the other.
- **Launch into the most relevant detail.** Time-, location-, or recent-action-relevant. Don't start on an empty list.
- **Multiple detail pages?** Use a vertical `TabView` — the Digital Crown scrolls between detail pages, the system shows a page indicator alongside.

## Three-column sidebar rules

When you have a three-column split (sidebar + list + detail):

- **Sidebar:** navigation — apps' top-level groupings. Max two levels of hierarchy (see [`./bars.md`](./bars.md#sidebars)).
- **Middle column:** list of items in the selected section (messages, notes, photos).
- **Detail:** the selected item.

Selection flows left → right. You can also drive the sidebar from a deep link.

## Common mistakes

- Split view on iPhone portrait.
- Non-resizable macOS panes.
- Dividers that disappear because a pane can shrink to 0.
- No highlight in the primary pane, so users lose the "I'm in Inbox" cue.
- Detail pane with no empty state — users see nothing on first launch.
- Three-column with three levels of deep hierarchy in the sidebar.
- iPad split view that doesn't hide the inspector at narrow widths.
- macOS split view that holds the toolbar hostage — toolbar actions should apply to the selected detail, not be trapped in one pane.

## SwiftUI

```swift
// Three-column split view with search on the sidebar.
@State private var selectedMailbox: Mailbox.ID?
@State private var selectedMessage: Message.ID?
@State private var searchText = ""

NavigationSplitView {
    List(selection: $selectedMailbox) {
        ForEach(store.mailboxes) { mb in
            Label(mb.name, systemImage: mb.systemImage).tag(mb.id)
                .badge(mb.unreadCount)
        }
    }
    .navigationTitle("Mailboxes")
    .navigationSplitViewColumnWidth(min: 200, ideal: 240, max: 320)
} content: {
    if let id = selectedMailbox, let box = store.mailbox(id) {
        List(box.messages, selection: $selectedMessage) { m in
            MessageRow(message: m).tag(m.id)
        }
        .searchable(text: $searchText, placement: .sidebar,
                    prompt: Text("Search mailbox"))
        .navigationTitle(box.name)
    } else {
        ContentUnavailableView("Select a mailbox", systemImage: "tray.2")
    }
} detail: {
    if let id = selectedMessage {
        MessageDetailView(id: id)
    } else {
        ContentUnavailableView("Select a message", systemImage: "envelope.open")
    }
}
.toolbar {
    ToolbarItem(placement: .primaryAction) {
        Button { compose() } label: { Label("Compose", systemImage: "square.and.pencil") }
    }
}

// Two-column layout (sidebar + detail).
NavigationSplitView(columnVisibility: $visibility) {
    SidebarView(selection: $selected)
} detail: {
    DetailView(id: selected)
}

// watchOS — pick the most relevant detail on launch.
NavigationSplitView {
    List(sections) { NavigationLink($0.name, value: $0) }
} detail: {
    RelevantDetailView(forCurrentTimeAndLocation: now)
}
```

## UIKit

```swift
// UISplitViewController — 2 or 3 column on iPadOS / macOS Catalyst.
let split = UISplitViewController(style: .tripleColumn)
split.preferredDisplayMode = .twoBesideSecondary
split.preferredSplitBehavior = .tile
split.setViewController(SidebarVC(),   for: .primary)
split.setViewController(MessageListVC(), for: .supplementary)
split.setViewController(DetailVC(),    for: .secondary)
split.presentsWithGesture = true
```

## AppKit

```swift
let split = NSSplitViewController()
split.splitView.dividerStyle = .thin
split.splitView.isVertical = true

let sidebar = NSSplitViewItem(sidebarWithViewController: SidebarVC())
sidebar.minimumThickness = 200
sidebar.maximumThickness = 320

let content = NSSplitViewItem(contentListWithViewController: MessageListVC())
content.minimumThickness = 260

let detail  = NSSplitViewItem(viewController: DetailVC())
detail.minimumThickness = 360

split.splitViewItems = [sidebar, content, detail]

// Persist sizes across launches.
split.splitView.autosaveName = "MainSplit"
```

## See also

- [`./bars.md`](./bars.md#sidebars) — sidebar rules (max 2 levels, behavior behind Liquid Glass).
- [`../patterns/navigation.md`](../patterns/navigation.md) — picking the right paradigm per platform.
- [`../platforms/macos.md`](../platforms/macos.md) — menu bar + toolbar + sidebar is the Mac spine.
- [`../platforms/ipados.md`](../platforms/ipados.md) (pending) — Stage Manager, compact-vs-regular width behavior.
- [`../components/lists-and-tables.md`](./lists-and-tables.md) — the pane contents for the middle column.
