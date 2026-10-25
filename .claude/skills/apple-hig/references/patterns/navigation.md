# Navigation

**HIG sources (composite — HIG doesn't have a single "navigation" page):**
- https://developer.apple.com/design/human-interface-guidelines/designing-for-ios
- https://developer.apple.com/design/human-interface-guidelines/designing-for-ipados
- https://developer.apple.com/design/human-interface-guidelines/designing-for-macos
- https://developer.apple.com/design/human-interface-guidelines/tab-bars
- https://developer.apple.com/design/human-interface-guidelines/sidebars
- https://developer.apple.com/design/human-interface-guidelines/toolbars

**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

Pick the navigation paradigm that matches the platform. Don't nest two paradigms in the same view. Don't invent a third.

## Rule zero: one paradigm per platform per view

| Platform | Primary | Secondary (within a section) |
|---|---|---|
| iPhone | **Tab bar** (2–5 tabs at the bottom) | `NavigationStack` (push / pop) |
| iPad, regular width | **Sidebar** via `NavigationSplitView`, or convertible tab bar/sidebar | `NavigationStack` in the detail column |
| iPad, compact width | **Tab bar** (same app collapses to this) | `NavigationStack` |
| Mac | **Menu bar + sidebar + toolbar**; `NavigationSplitView` for sidebar apps | Multi-window for parallel work |
| Apple Watch | **Vertical page** (swipe) or **hierarchical** (push/pop) | Never tab-bar-style |
| Apple TV | **Focus-driven** — top-level collections + focused navigation | Same |
| Vision Pro | **Vertical tab bar** (leading side) | `NavigationSplitView` inside a tab for depth |

## Paradigms

### Tab bar
- **2–5 peer sections.** iOS at bottom, iPadOS at top, visionOS vertical-leading.
- Lateral: switching tabs is not a drill-in.
- Preserves nav state per tab.
- See [`../components/bars.md`](../components/bars.md#tab-bars) for the hard rules.

### Navigation stack
- Hierarchical drill-in. Back chevron at top-leading.
- Don't replace the system back. Don't ship a custom "Back" label.
- Value-based destinations: `NavigationStack { … }` with `navigationDestination(for:)`.

### Sidebar / split view
- Deep hierarchy, two or three columns.
- iPadOS regular or macOS. **Never on iPhone.**
- Max two levels of hierarchy in the sidebar itself — add a content list column for depth.
- Extend content behind the sidebar; it floats on Liquid Glass.

### Menu bar (macOS)
- **Authoritative.** Every command your app does lives here.
- Toolbar and context menus are shortcuts into the menu bar.
- Standard menus in order: App / File / Edit / View / (app-specific) / Window / Help.

### Focus engine (tvOS)
- No cursor, no tab bar paradigm alone — focused items scale; surrounding layout must have padding.
- Use focus sections to group related controls.

### Ornaments (visionOS)
- External controls anchored to a window without consuming internal space.
- Toolbars live at the bottom as an ornament; tab bars as a vertical leading ornament.

## Choosing per screen

- **Top-level sections (home / inbox / search / profile):** tab bar on iOS/iPadOS, sidebar on macOS.
- **Drill from a list to a detail:** `NavigationStack` on iOS/iPadOS/visionOS; third column in split view on iPadOS regular and macOS.
- **Choose one of many items:** list — not a modal sheet.
- **Quick sub-task that returns to the parent:** sheet (see [`../components/sheets-and-popovers.md`](../components/sheets-and-popovers.md#sheets)).
- **"Do this one extra step":** popover (wide context) or inline — not a push.

## Common mistakes

- Tab bar on macOS.
- Sidebar on iPhone.
- Custom "Back" label instead of the system Back.
- Sheet used for drill-in (the user expects a sheet to return to the exact same place).
- Modal for a primary top-level section.
- Three-level sidebar hierarchy (collapses scannability).
- Tab bar + bottom toolbar both present and fighting for attention.
- iPad app that ships only a tab bar in compact and never shows a sidebar in regular width.
- visionOS app with a horizontal toolbar instead of using ornaments.
- watchOS tab-bar emulation.

## SwiftUI

```swift
// iPhone — tab bar of top-level sections, NavigationStack inside each.
TabView {
    Tab("Inbox", systemImage: "tray") {
        NavigationStack { InboxView() }
    }
    Tab("Browse", systemImage: "square.grid.2x2") {
        NavigationStack { BrowseView() }
    }
    Tab("Profile", systemImage: "person.crop.circle") {
        NavigationStack { ProfileView() }
    }
    Tab("Search", systemImage: "magnifyingglass", role: .search) {
        NavigationStack { SearchView() }
    }
}
.tabViewStyle(.sidebarAdaptable)     // iPad promotes to sidebar automatically

// iPad regular / macOS — three-column split view.
NavigationSplitView {
    SidebarView()
        .navigationSplitViewColumnWidth(min: 200, ideal: 240, max: 320)
} content: {
    MessageListView()
} detail: {
    MessageDetailView()
}

// Value-based destinations so deep-links are a value, not a view.
NavigationStack(path: $path) {
    InboxView()
        .navigationDestination(for: Message.ID.self) { MessageDetailView(id: $0) }
        .navigationDestination(for: Thread.ID.self)  { ThreadView(id: $0) }
}

// watchOS — vertical page navigation with three top-level pages.
TabView {
    TodayView()
    GoalsView()
    TrendsView()
}
.tabViewStyle(.verticalPage)
```

## UIKit / AppKit

```swift
// UIKit — UITabBarController wrapping UINavigationControllers.
let tabs = UITabBarController()
tabs.viewControllers = [
    UINavigationController(rootViewController: InboxVC()),
    UINavigationController(rootViewController: BrowseVC()),
    UINavigationController(rootViewController: SearchVC()),
]
for (vc, (title, symbol)) in zip(tabs.viewControllers!, zip(
    ["Inbox", "Browse", "Search"],
    ["tray", "square.grid.2x2", "magnifyingglass"])) {
    vc.tabBarItem = UITabBarItem(
        title: title, image: UIImage(systemName: symbol), tag: 0)
}
```

```swift
// AppKit — NSSplitViewController with sidebar + content + detail.
let split = NSSplitViewController()
split.splitViewItems = [
    NSSplitViewItem(sidebarWithViewController: SidebarVC()),
    NSSplitViewItem(viewController: ContentVC()),
    NSSplitViewItem(viewController: DetailVC()),
]
// Every sidebar action corresponds to a menu bar command.
```

## See also

- [`../components/bars.md`](../components/bars.md) — the hard rules for tab bars, toolbars, sidebars.
- [`../components/sheets-and-popovers.md`](../components/sheets-and-popovers.md) — sheet vs push vs popover.
- [`../platforms/ios.md`](../platforms/ios.md) and [`../platforms/macos.md`](../platforms/macos.md) — platform-specific navigation expectations.
- [`../patterns/modality.md`](./modality.md) — when a task is modal instead of navigational.
- [`../patterns/search.md`](./search.md) — search as a primary navigation target.
