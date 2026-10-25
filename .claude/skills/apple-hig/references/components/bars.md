# Bars — tab, navigation, toolbar, sidebar

**HIG sources:**
- https://developer.apple.com/design/human-interface-guidelines/tab-bars
- https://developer.apple.com/design/human-interface-guidelines/toolbars
- https://developer.apple.com/design/human-interface-guidelines/sidebars

**Last verified:** 2026-04-20
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS (toolbar only)

Bars are how people navigate and issue commands. In 2025 HIG, they float above content on the Liquid Glass layer and pick up color from the content behind — do not give them a solid background. Note: HIG now consolidates "navigation bars" into toolbars — for iOS that means the thing at the top is still a nav bar to you, but it follows toolbar rules.

## Table of contents

- [Which bar goes where](#which-bar-goes-where)
- [Tab bars](#tab-bars)
- [Toolbars (and iOS navigation bars)](#toolbars-and-ios-navigation-bars)
- [Sidebars](#sidebars)
- [Platform quick reference](#platform-quick-reference)
- [Common mistakes](#common-mistakes)
- [SwiftUI](#swiftui)
- [UIKit / AppKit](#uikit--appkit)
- [See also](#see-also)

## Which bar goes where

| Bar | Purpose | iOS | iPadOS | macOS | visionOS |
|---|---|---|---|---|---|
| Tab bar | Top-level navigation across 2–5 peer sections | Bottom, Liquid Glass | Top (fixed or convertible) | — | Vertical, leading-side |
| Navigation bar / top toolbar | Title + nav controls + actions for the current view | Top | Top (may combine with tab bar) | Top (toolbar integrated with titlebar) | Bottom (toolbar) |
| Toolbar | Quick access to commands acting on the view | Same as nav bar | Same + customizable | Top, customizable | Bottom |
| Sidebar | Navigation surface for deep hierarchies | Avoid | Primary for regular size class | Primary navigation | Inside a tab |

Never nest two navigation paradigms (e.g., tab bar on macOS, or a sidebar plus a tab bar on iPhone).

## Tab bars

A tab bar is for **navigation between top-level sections**. Not for actions. Not for settings.

### Rules

- **2–5 tabs.** Fewer is better. An overflow "More" tab on iOS hides content — avoid needing it.
- **Keep the tab bar visible** as people navigate within a section. The exception is a modal — modals cover the tab bar because they're temporary and self-contained.
- **Never disable or hide a tab button** based on emptiness. An empty section should say why; the tab stays in place. Instability is worse than emptiness.
- **Every tab gets a label.** Single word when possible.
- **Use SF Symbols.** Fill variants. On compact views, icon-above-label; on regular views, icon-beside-label.
- **Badges only for critical information.** Diluting them ruins signal.
- **On colorful content, keep the tab bar monochromatic** or pick an accent with enough contrast. Liquid Glass picks up content color; labels will fight if contrast is weak.

### Platform specifics

- **iOS** — Tab bar floats at the bottom on Liquid Glass. Optional accessory (like Music's MiniPlayer) can minimize inline when scrolling. Tab bar can include a dedicated **search** tab at the trailing end (`role: .search`).
- **iPadOS** — Tab bar is at the top. Can convert to/from a sidebar via a button; use `.sidebarAdaptable` `TabView`. Supports user-customizable tab order. If you let people pick tabs, default to five or fewer for parity between compact and regular layouts.
- **macOS** — No tab bar at the top level. Use menu bar + sidebar.
- **tvOS** — 68pt tall, fixed 46pt from top. Translucent; only selected tab is opaque. Max items visible without scrolling truncate with a fade. Menu button on the remote returns focus to the tab bar.
- **visionOS** — Tab bar is **vertical**, fixed to leading side. Labels reveal on gaze; supply both symbol and short label. Can contain a sidebar within a tab for secondary navigation.

## Toolbars (and iOS navigation bars)

A toolbar provides access to frequent commands and navigation. In iOS it's the thing at the top of a `NavigationStack`; on macOS it's the thing at the top of the window; on visionOS it's at the bottom of the window.

### Content

Three kinds of items:

1. **Title** — current view (large title on iOS, inline on macOS/visionOS).
2. **Navigation controls** — Back, Close, sidebar toggle.
3. **Actions / bar items** — what the user does in this view.

### Rules

- **Don't overcrowd.** Space is scarce; every item fights for attention.
- **Let the system's overflow menu do the work.** On macOS and iPadOS, items that don't fit spill into the overflow automatically. Do not add your own overflow menu manually. Avoid layouts that overflow by default.
- **Add a More menu** for low-priority actions when you genuinely need more room.
- **Customizable on iPadOS and macOS** for power users — the API makes this essentially free.
- **No custom background.** Liquid Glass takes its appearance from the content. Heavy tinting fights the system.
- **Prefer borderless system symbols.** The bar provides the container; you don't need a circle around your icon.
- **One prominent primary action per view.** Trailing edge. Tint only this one.
- **Standard Back and Close icons** — don't ship a custom "Back" or "Close" label; use the standard symbols.

### Layout groups

- **Leading edge** — Back, sidebar toggle, view title, document menu (Duplicate / Rename / Export). These don't get customized by the user.
- **Center** — common commands. Customizable on macOS/iPadOS. Collapses into overflow when the window narrows.
- **Trailing edge** — always-visible essentials: inspector toggles, search field, More menu, primary action (Done).

- **Keep text-labeled buttons separated.** Two text buttons in a row read as one combined button. Insert fixed space.
- **≤3 logical groups** per toolbar — more reads as cluttered.

### Titles

- A window title confirms location and disambiguates multiple windows.
- **Don't use the app name as a title.** Useless.
- **Under 15 characters** if you can — room for controls.
- **Leave the title empty** when the content makes context obvious (single-window Notes shows the first line instead).

### Platform specifics

- **iOS** — Prioritize only the most important items; More menu for the rest. Large title collapses to inline on scroll (system-managed).
- **iPadOS** — Toolbar + tab bar can coexist horizontally at the top.
- **macOS** — Toolbar is integrated with the window titlebar. Items are borderless. **Every toolbar item has a menu-bar command** because users can customize or hide the toolbar.
- **visionOS** — Toolbar floats at the bottom, slightly in front of the window in z. Uses variable blur so content reading underneath stays legible. Supplies tooltip on gaze. **Do not use a pull-down menu in a visionOS toolbar** — it obscures window controls below. Prevent windows from resizing below toolbar width.
- **watchOS** — Corner toolbar buttons stay visible above scrolling content. Scrolling toolbar buttons reveal on scroll-up. Use for actions that aren't the primary app function.

## Sidebars

Sidebars are broad, flat navigation surfaces for deep hierarchies. They float above content (like other chrome) in the Liquid Glass layer.

### Rules

- **Max two levels of hierarchy.** If your data is deeper, add a content list between sidebar and detail (three-column split view).
- **Extend content behind the sidebar.** Either horizontally scroll content under, or use a background extension view so the wall of content looks continuous.
- **Customizable content** when it makes sense — users know what matters most.
- **Disclosure for groups** if the sidebar is long. Keep rows scannable.
- **SF Symbols for item icons.** Use custom symbols when necessary; not bitmaps.
- **Allow hide/show.** On iPadOS use the built-in edge-swipe; on macOS ship the View menu's Show/Hide Sidebar commands. Don't hide by default on first launch — people won't find it.

### Platform specifics

- **iOS** — Avoid sidebars. They eat landscape space and don't exist in portrait. Use a tab bar.
- **iPadOS** — Use `.sidebarAdaptable` `TabView` for flexible tab↔sidebar conversion, or `NavigationSplitView` for a permanent sidebar. Prefer a tab bar first; convert to sidebar only when needed.
- **macOS** — Sidebar icon size is tied to General settings (small/medium/large). **Use the user's accent color** for sidebar icons by default. Fixed colors only when color carries meaning. Consider auto-hiding when window narrows. No critical content at the bottom (hidden when window is dragged offscreen).
- **tvOS** — Supported (rarely needed).
- **watchOS** — Not supported.
- **visionOS** — Use sidebar **inside a tab** for secondary navigation in deep apps. Don't let sidebar selection switch the current tab.

## Platform quick reference

| Task | iOS | iPadOS | macOS | visionOS |
|---|---|---|---|---|
| Top-level sections (2–5) | Bottom tab bar | Top tab bar (or sidebar-adaptive) | Sidebar + menu bar | Vertical tab bar |
| View-level commands | Top nav bar + More menu | Top toolbar + overflow | Top toolbar + menu bar | Bottom toolbar |
| Deep navigation | `NavigationStack` inside a tab | 3-column `NavigationSplitView` | 3-column `NavigationSplitView` | Sidebar in tab |

## Common mistakes

- Tab bar with 6+ tabs and a More overflow you rely on.
- Tab bar used for actions (compose, share, etc.) instead of navigation.
- Hiding tab buttons when sections are empty.
- Tab bar on macOS instead of sidebar + menu bar.
- Custom "Back" label instead of the system Back symbol.
- Solid tinted toolbar background that fights Liquid Glass.
- Two text-labeled toolbar buttons with no separator (reads as one button).
- Manually adding an overflow menu on iPadOS/macOS (the system does it).
- More than three logical groups in a toolbar.
- Toolbar item without a corresponding menu-bar command on macOS.
- Sidebar with 3+ levels of hierarchy (use a content list).
- Fixed-color sidebar icons instead of the user's accent.
- Pull-down menu in a visionOS toolbar (obscures window controls).
- Window title = app name.

## SwiftUI

```swift
// iOS / iPadOS — tab bar with a dedicated search tab, adaptive to sidebar.
TabView {
    Tab("Home", systemImage: "house") { HomeView() }
    Tab("Library", systemImage: "books.vertical") { LibraryView() }
    Tab("Profile", systemImage: "person.crop.circle") { ProfileView() }
        .badge(unreadBadge)
    Tab("Search", systemImage: "magnifyingglass", role: .search) { SearchView() }
}
.tabViewStyle(.sidebarAdaptable)

// iOS — nav stack + toolbar with leading/trailing/primary structure.
NavigationStack {
    InboxList()
        .navigationTitle("Inbox")
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("Edit") { isEditing.toggle() }
            }
            ToolbarItemGroup(placement: .topBarTrailing) {
                Button { showingCompose = true } label: { Image(systemName: "square.and.pencil") }
                    .accessibilityLabel("Compose")
                Menu {
                    Button("Archive All") { archiveAll() }
                    Button("Mark All Read") { markAllRead() }
                    Divider()
                    Button("Settings…") { showingSettings = true }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
                .accessibilityLabel("More")
            }
        }
}

// macOS — NavigationSplitView with sidebar + customizable toolbar.
NavigationSplitView {
    SidebarView()
        .navigationSplitViewColumnWidth(min: 200, ideal: 240, max: 320)
} content: {
    MessageList()
} detail: {
    MessageDetail()
}
.toolbar(id: "main") {
    ToolbarItem(id: "refresh", placement: .navigation) {
        Button(action: refresh) { Image(systemName: "arrow.clockwise") }
    }
    ToolbarItem(id: "compose", placement: .primaryAction) {
        Button { compose() } label: {
            Label("Compose", systemImage: "square.and.pencil")
        }
    }
}
.toolbarRole(.editor)

// Primary action tinted on Liquid Glass.
Button("Send") { send() }
    .buttonStyle(.borderedProminent)
    .tint(.accentColor)
    .keyboardShortcut(.defaultAction)
```

## UIKit / AppKit

```swift
// UIKit — UITabBarController with five tabs, last being search.
let tabs = UITabBarController()
tabs.tabBar.tintColor = .label
tabs.viewControllers = [
    nav(HomeVC(), title: "Home", symbol: "house"),
    nav(LibraryVC(), title: "Library", symbol: "books.vertical"),
    nav(ProfileVC(), title: "Profile", symbol: "person.crop.circle"),
    nav(SearchVC(), title: "Search", symbol: "magnifyingglass"),
]

// UIKit — nav bar toolbar with standard placements.
navigationItem.leftBarButtonItem = UIBarButtonItem(
    title: "Edit", style: .plain, target: self, action: #selector(toggleEdit))
let compose = UIBarButtonItem(
    image: UIImage(systemName: "square.and.pencil"),
    style: .plain, target: self, action: #selector(compose))
compose.accessibilityLabel = "Compose"
navigationItem.rightBarButtonItems = [compose, moreMenuItem()]
navigationItem.largeTitleDisplayMode = .always
```

```swift
// AppKit — NSToolbar with customizable items.
let toolbar = NSToolbar(identifier: "MainToolbar")
toolbar.delegate = self
toolbar.allowsUserCustomization = true
toolbar.autosavesConfiguration = true
toolbar.displayMode = .iconOnly
window.toolbar = toolbar
window.toolbarStyle = .unified

// Every toolbar item also gets a menu bar entry.
let composeItem = NSToolbarItem(itemIdentifier: .compose)
composeItem.image = NSImage(systemSymbolName: "square.and.pencil",
                            accessibilityDescription: "Compose")
composeItem.label = "Compose"
composeItem.toolTip = "Compose new message"
composeItem.target = self
composeItem.action = #selector(compose(_:))
```

## See also

- [`../platforms/ios.md`](../platforms/ios.md) — iOS navigation paradigm.
- [`../platforms/macos.md`](../platforms/macos.md) — menu bar is authoritative.
- [`../foundations/materials.md`](../foundations/materials.md) — Liquid Glass rules.
- [`../foundations/color.md`](../foundations/color.md) — tinting the primary action.
- [`./split-views.md`](./split-views.md) (pending) — three-column layouts.
- [`./sheets-and-popovers.md`](./sheets-and-popovers.md) — modals that cover the tab bar.
