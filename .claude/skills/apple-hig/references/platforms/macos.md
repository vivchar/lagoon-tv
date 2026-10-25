# macOS / Mac

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/designing-for-macos
**Last verified:** 2026-04-20
**Applies to:** Mac (macOS). See [Mac Catalyst](../technologies/mac-catalyst.md) for iPadOS-bridged apps.

This is the deep-dive platform file for macOS. Load it whenever you're building or reviewing a Mac app. The single biggest mistake Claude makes here is shipping iOS idioms on a desktop: tab bars instead of menu bars + sidebars, full-bleed sheets instead of windows, "Tap" instead of "Click." Don't do that.

## Table of contents

- [What makes macOS macOS](#what-makes-macos-macos)
- [Device traits](#device-traits)
- [The big idioms](#the-big-idioms)
- [Windowing](#windowing)
- [Menu bar — first-class](#menu-bar--first-class)
- [Toolbar](#toolbar)
- [Sidebar and split views](#sidebar-and-split-views)
- [Keyboard and pointer](#keyboard-and-pointer)
- [Layout rules](#layout-rules)
- [Typography](#typography)
- [Color and materials](#color-and-materials)
- [Settings](#settings)
- [Writing](#writing)
- [Mac Catalyst notes](#mac-catalyst-notes)
- [Common mistakes](#common-mistakes)
- [SwiftUI — a complete macOS app](#swiftui--a-complete-macos-app)
- [AppKit — the same app, sketched](#appkit--the-same-app-sketched)
- [See also](#see-also)

## What makes macOS macOS

A Mac is a desk. People sit still, run many apps at once, expect to resize windows, hit every shortcut they know, and treat their mouse/trackpad as a precision tool. Your app should:

- Use the **menu bar** as the authoritative command surface.
- Use **windows** — resizable, restorable, multiple where appropriate.
- Use **sidebars** and **toolbars** instead of tab bars.
- Respect the **keyboard** as a first-class input.
- Support **high-precision pointer** interactions (hover states, ≥28×28pt targets).
- Offer **personalization**: customizable toolbars, color/font choices, window restoration.

## Device traits

- **Display.** Large, high-resolution, often multiple. Users can span content across displays, dock iPads as second screens, attach external monitors.
- **Ergonomics.** Sitting position. Viewing distance ~30–90cm. One primary task often, but many background apps.
- **Inputs.** Keyboard + trackpad + mouse. Stylus on supported Macs. External hardware galore.
- **Session length.** Anywhere from 1 minute to many hours. Smooth active/inactive state transitions between foreground and background matter.
- **System features:** Mission Control / Spaces, Mission-Control full-screen, Stage Manager, Quick Look, Spotlight, Shortcuts, Universal Control, Continuity, Focus modes, Screen Time, iCloud.

## The big idioms

- **Menu bar is the source of truth** for every command your app can do.
- **Toolbar** surfaces frequent commands; every toolbar item also exists in the menu bar.
- **Sidebar** is the primary navigation surface for hierarchical data.
- **Contextual menus** (right-click / Control-click / secondary-click) everywhere relevant.
- **Keyboard shortcuts** for every common action. ⌘-key shortcuts are a contract with users.
- **Personalization** — customizable toolbars, resizable columns, preference persistence.
- **Multi-window** documents. Same document can open in multiple windows.

## Windowing

- **Standard window chrome:** traffic lights (close, minimize, zoom), title bar, toolbar, content area.
- **Minimum size:** set a sensible floor so the UI doesn't break.
- **Resizable.** Every content layout must adapt to any width/height within your allowed range.
- **Full-screen mode** — your window can be made its own Space. Support it. Keep chrome accessible (cursor-to-top reveals).
- **Split-screen** — two apps share a Space. Same rules as full-screen.
- **Restore on next launch.** State + window positions + selection should come back.
- **Never put critical controls at the bottom of a window.** Users drag window bottoms below the screen all the time.
- **Camera housing** (notch on MacBooks). Don't put content inside the housing area at the top center.
- **Traffic lights** live top-leading. Don't hide them without a very good reason.

## Menu bar — first-class

- **Every command exists in the menu bar.** Toolbar, button, and keyboard shortcut are shortcuts to it.
- **Standard menus** (in order): App, File, Edit, View, [app-specific menus], Window, Help.
- Group items with separators. Disable items instead of hiding them when context doesn't apply.
- Keyboard shortcuts follow conventions: ⌘N new, ⌘O open, ⌘S save, ⌘W close, ⌘Z undo, ⌘Shift+Z redo (or ⌘Y on some keyboards), ⌘F find, ⌘, settings/preferences, ⌘Q quit, ⌘H hide.
- Provide a **Help** menu with app-specific help entries.
- If you're a "menu bar app" (extra status item), provide a menu from the status icon, respect the menu bar's visual conventions.

## Toolbar

- **Frequent commands only.** A toolbar is not a complete UI — the menu bar is.
- **Customizable by users.** `NSToolbar` makes this free; in SwiftUI, `.toolbar { ... }` + `Commands` gives you customizable placement.
- **Icons from SF Symbols.** Outline variant (not fill).
- **Labels optional per user setting** — icon-only, icon + label, label-only are user choices.
- **Put search in the toolbar** (right side usually), and the most frequent primary action adjacent.
- On Liquid Glass, toolbars adapt appearance to the content; don't bake a custom background.

## Sidebar and split views

- **`NavigationSplitView`** is the standard structure for most Mac apps. Two or three columns: sidebar → optional list/content → detail.
- Sidebar is **translucent material** by default (`.sidebar` material in AppKit).
- **Sidebar icons** — SF Symbols, colored for semantic meaning, respect user's accent color unless you want a fixed color for clarity.
- **Source lists** (indentable outline in sidebar) for hierarchical navigation.
- **Section headers** (small-cap) to group items.
- Let users resize columns and remember the size.

See [`../components/split-views.md`](../components/split-views.md) (pending).

## Keyboard and pointer

- **Keyboard navigation first-class.** Full Keyboard Access should work. Tab cycles focus in sensible order.
- **Every interactive element has a shortcut in the menu bar** for power users.
- **Pointer hover states.** Buttons reveal affordances on hover; list rows lighten; toolbar items gain background.
- **Minimum pointer target: 28×28pt** (20×20 absolute floor for dense UI).
- **Context menus** everywhere a user might want one. `.contextMenu { ... }` in SwiftUI; `NSMenu` + `menuNeedsUpdate` in AppKit.
- **Undo / Redo** mandatory for any editable state. Use `UndoManager`.

## Layout rules

- **Extend content to edges.** Don't leave solid bars where Liquid Glass could show scroll behavior.
- **Window titles** follow the platform: title + subtitle in the title bar.
- **Large titles** (Liquid Glass-era) appear below the toolbar in modern Mac apps.
- **Use `Form`** in SwiftUI for settings-like content on macOS — renders as a grouped macOS-style form automatically.
- **Document-based apps** use the Document architecture; multi-window behavior, `NSDocument` restoration.

## Typography

- **SF Pro** is the system font. NY available via Mac Catalyst or `.fontDesign(.serif)`.
- **13pt** is the default body size on macOS. Minimum is 10pt.
- **macOS does NOT support Dynamic Type.** Use the macOS text style table in [`../foundations/typography.md`](../foundations/typography.md#macos-built-in-text-styles).
- **Dynamic font variants** match specific system chrome (control content, label, menu, menu bar, message, palette, title, tool tip).
- Use emphasized variants via symbolic traits for a second weight without switching styles.

## Color and materials

- **Semantic colors.** `NSColor.labelColor`, `NSColor.controlBackgroundColor`, `NSColor.windowBackgroundColor`, etc. See [`../foundations/color.md`](../foundations/color.md#semantic-colors-macos).
- **User accent color.** Unless user picks a specific accent, the system uses yours. Respect user choice for standard UI chrome.
- **Desktop tinting.** When graphite is the accent, window backgrounds pick up desktop picture color. Allow transparency in custom components with a visible background **only in neutral state**.
- **Liquid Glass** for toolbars and floating chrome.
- **Named materials** (`NSVisualEffectView.Material`): sidebar, titlebar, HUD, menu, popover, sheet, headerView, selection, etc. Use the named one that matches the purpose, not a tinted custom fill.
- **Dark Mode** — mandatory. Test with a range of desktop pictures. Do not hardcode hex.

## Settings

- **Use the `Settings` scene** in SwiftUI (`Settings { ... }`). Produces a standard macOS preferences window with ⌘, shortcut wired up.
- **Tabbed** or **sidebar** settings layout — system decides based on content.
- **Form-driven.** `Form { ... }` renders the macOS-idiomatic grouped/bordered form.
- **Don't ship a sheet named "Settings."** Use the Settings scene or `NSWindowController` for it.

## Writing

- **"Click"**, not "tap."
- **Title case** for menu bar items (matches macOS convention: "Save As…", "Open Recent").
- **Sentence case** for button labels and most titles within app content.
- **Menu items with dialogs get trailing …** ("Save As…", "Find…").
- **Avoid "icon"** in labels; call the thing what it is ("Attach file", not "Attach icon").

## Mac Catalyst notes

If your app is Mac Catalyst (iPadOS code running on Mac):

- The app gets a real menu bar. Provide `UIMenuBuilder` commands for all your actions.
- Tab bars morph into sidebar-style navigation when possible. Prefer `.sidebarAdaptable` `TabView`.
- Touch-sized controls are scaled — still ensure pointer interactions via `.hoverEffect`.
- Not a replacement for native AppKit when you need deep Mac integration (NSDocument multi-window, Auto Save, Quick Look providers).

## Common mistakes

- Tab bar instead of menu bar + sidebar.
- Missing menu bar commands (only toolbar buttons).
- Toolbar that isn't user-customizable.
- Sheets used as the main UI instead of windows.
- Non-resizable window.
- Content at the bottom of a window (hidden when dragged off-screen).
- No keyboard shortcuts.
- No context menu.
- Fixed font sizes that don't match macOS conventions.
- Writing "Tap" in a Mac app.
- Custom window chrome that replaces traffic lights.
- Settings implemented as a sheet instead of a Settings scene.
- Ignoring user accent color.
- No Undo/Redo for an editable state.
- Hardcoded fills that ignore desktop tinting and Dark Mode.

## SwiftUI — a complete macOS app

```swift
import SwiftUI

@main
struct LibraryApp: App {
    @State private var store = LibraryStore()

    var body: some Scene {
        WindowGroup("Library") {
            RootView()
                .environment(store)
                .frame(minWidth: 720, minHeight: 480)
        }
        .commands {
            CommandGroup(replacing: .newItem) {
                Button("New Collection") { store.newCollection() }
                    .keyboardShortcut("n")
            }
            CommandMenu("Collection") {
                Button("Sort by Title") { store.sort(by: .title) }
                    .keyboardShortcut("1", modifiers: [.command, .option])
                Button("Sort by Date")  { store.sort(by: .date)  }
                    .keyboardShortcut("2", modifiers: [.command, .option])
                Divider()
                Button("Archive…") { store.archiveSelected() }
                    .keyboardShortcut(.delete, modifiers: [.command])
            }
        }

        Settings {
            SettingsView()
                .frame(width: 520, height: 360)
        }
    }
}

struct RootView: View {
    @Environment(LibraryStore.self) private var store
    @State private var selection: Collection.ID?
    @State private var detailSelection: Book.ID?
    @State private var searchText = ""

    var body: some View {
        NavigationSplitView {
            List(selection: $selection) {
                Section("Library") {
                    ForEach(store.collections) { collection in
                        Label(collection.name, systemImage: collection.systemImage)
                            .tag(collection.id)
                    }
                }
            }
            .navigationTitle("Library")
            .navigationSplitViewColumnWidth(min: 200, ideal: 240, max: 320)
        } content: {
            if let selection, let books = store.books(in: selection) {
                List(books, selection: $detailSelection) { book in
                    BookRow(book: book)
                        .tag(book.id)
                        .contextMenu {
                            Button("Rename…") { store.rename(book.id) }
                            Button("Move to Collection…") { store.startMove(book.id) }
                            Divider()
                            Button("Archive", role: .destructive) { store.archive(book.id) }
                        }
                }
                .searchable(text: $searchText, placement: .sidebar)
            } else {
                ContentUnavailableView("Select a collection", systemImage: "books.vertical")
            }
        } detail: {
            if let detailSelection { BookDetailView(id: detailSelection) }
            else { ContentUnavailableView("Select a book", systemImage: "book.closed") }
        }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button { store.addBook() } label: {
                    Label("Add Book", systemImage: "plus")
                }
                .keyboardShortcut("n", modifiers: [.command, .shift])
            }
            ToolbarItem(placement: .principal) {
                if store.isSyncing { ProgressView().controlSize(.small) }
            }
        }
    }
}

struct SettingsView: View {
    @AppStorage("library.autoSync") private var autoSync = true
    @AppStorage("library.defaultSort") private var defaultSort: SortField = .title

    var body: some View {
        TabView {
            Form {
                Section {
                    Toggle("Sync automatically", isOn: $autoSync)
                    Picker("Default sort", selection: $defaultSort) {
                        Text("Title").tag(SortField.title)
                        Text("Date added").tag(SortField.date)
                    }
                }
            }
            .formStyle(.grouped)
            .tabItem { Label("General", systemImage: "gear") }

            Form {
                Section {
                    LabeledContent("Version", value: Bundle.main.shortVersion)
                    LabeledContent("Build", value: Bundle.main.build)
                }
            }
            .formStyle(.grouped)
            .tabItem { Label("About", systemImage: "info.circle") }
        }
    }
}
```

What this demonstrates: three-column `NavigationSplitView` with sidebar + list + detail, Liquid Glass toolbar with primary action and progress, searchable on the sidebar, context menus, menu bar commands via `.commands` (including replacing `newItem`), Settings scene with tabbed layout and `Form` in grouped style, keyboard shortcuts, semantic system colors via default styling.

## AppKit — the same app, sketched

```swift
import AppKit

final class WindowController: NSWindowController {
    convenience init() {
        let window = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 1040, height: 640),
            styleMask: [.titled, .closable, .miniaturizable, .resizable, .fullSizeContentView],
            backing: .buffered, defer: false
        )
        window.minSize = NSSize(width: 720, height: 480)
        window.titlebarAppearsTransparent = true
        window.isReleasedWhenClosed = false
        window.contentViewController = NSSplitViewController().apply {
            $0.splitViewItems = [sidebar(), content(), detail()]
        }
        let toolbar = NSToolbar(identifier: "Library")
        toolbar.allowsUserCustomization = true
        toolbar.autosavesConfiguration = true
        toolbar.displayMode = .iconOnly
        window.toolbar = toolbar
        window.toolbarStyle = .unified
        self.init(window: window)
    }

    private func sidebar() -> NSSplitViewItem {
        let item = NSSplitViewItem(sidebarWithViewController: SidebarViewController())
        item.minimumThickness = 200
        item.maximumThickness = 320
        return item
    }
    private func content() -> NSSplitViewItem { .init(viewController: ContentViewController()) }
    private func detail() -> NSSplitViewItem { .init(viewController: DetailViewController()) }
}

extension NSObject {
    func apply<T>(_ block: (T) -> Void) -> T where T == Self { block(self); return self }
}
```

Register menu commands in `MainMenu.xib` (or programmatically via `NSApplication.shared.mainMenu`). Every user-visible command lives there.

## See also

- [`../foundations/accessibility.md`](../foundations/accessibility.md)
- [`../foundations/layout.md`](../foundations/layout.md)
- [`../foundations/typography.md`](../foundations/typography.md#macos-built-in-text-styles)
- [`../foundations/color.md`](../foundations/color.md#semantic-colors-macos)
- [`../foundations/materials.md`](../foundations/materials.md#macos)
- [`../components/bars.md`](../components/bars.md) (pending) — menu bar + toolbar specifics.
- [`../components/split-views.md`](../components/split-views.md) (pending).
- [`../components/sheets-and-popovers.md`](../components/sheets-and-popovers.md) (pending).
- [`../patterns/settings.md`](../patterns/settings.md) (pending) — Settings scene.
- [`../technologies/mac-catalyst.md`](../technologies/mac-catalyst.md) (pending).
