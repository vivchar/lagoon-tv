# iPadOS

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/designing-for-ipados
**Last verified:** 2026-04-21
**Applies to:** iPad (iPadOS)

iPad sits between iPhone and Mac. It has a big high-res screen, Multi-Touch, Apple Pencil, pointer + keyboard, and windows that resize fluidly through Stage Manager and Split Screen. The single biggest mistake Claude makes here is treating an iPad app as "a bigger iPhone app" — iPad wants sidebars, split views, drag and drop, pointer hover states, keyboard shortcuts, and Pencil.

## Table of contents

- [What makes iPadOS iPadOS](#what-makes-ipados-ipados)
- [Device traits](#device-traits)
- [Windowing and multitasking](#windowing-and-multitasking)
- [Navigation](#navigation)
- [Input model](#input-model)
- [Apple Pencil and Scribble](#apple-pencil-and-scribble)
- [Drag and drop](#drag-and-drop)
- [Layout](#layout)
- [Typography, color, materials](#typography-color-materials)
- [Mac Catalyst notes](#mac-catalyst-notes)
- [Common mistakes](#common-mistakes)
- [SwiftUI — a complete iPad app](#swiftui--a-complete-ipad-app)
- [See also](#see-also)

## What makes iPadOS iPadOS

- **Resizable windows** (Stage Manager, Split Screen, Slide Over). Design for every width.
- **Sidebar-first navigation** when the size class is regular. Tab bar when compact.
- **Multiple input modes** — touch, Pencil, pointer, trackpad, hardware keyboard — often combined.
- **Drag and drop** between apps and within the app.
- **Menu bar appears** when a hardware keyboard is attached — wire up `Commands` (SwiftUI) or `UIMenuBuilder` (UIKit).
- **Transitions to macOS** via Mac Catalyst. Design so the same code reads correctly when running as a Mac app.

## Device traits

- **Display.** Large, high-resolution; up to 13" (iPad Pro 12.9). Regular size class in every orientation, on every iPad in full-screen.
- **Ergonomics.** Often held, but also set on a stand, desk, or keyboard case. Viewing distance ranges up to 3 feet depending on posture.
- **Inputs.** Multi-Touch, Apple Pencil, Magic Keyboard / external keyboard, Magic Trackpad / mouse, Camera Control (on supported models), external accessories via USB-C.
- **Session length.** Anywhere from a few minutes to hours of deep work.
- **System features:** Stage Manager, Split View, Slide Over, Picture-in-Picture, Universal Control, Continuity, AirDrop, SharePlay, Focus modes, Control Center, Spotlight, Shortcuts, Siri, Universal Print.

## Windowing and multitasking

- **Resizable windows.** iPad windows behave like Mac windows in Stage Manager — users drag corners, use the green window-control menu, tile to halves/thirds/quadrants.
- **Design for minimum, intermediate, and maximum widths.** The layout must survive at each. Check half-screen, third-screen, and quadrant-screen widths.
- **Defer switching to compact** as the window narrows. Keep the full layout as long as it fits; collapse gradually.
- **Hide tertiary columns first** (inspector → list → sidebar). Don't drop the sidebar at the first sign of narrowness.
- **Split View** (two apps side by side) is the user's decision. Your app's layout must handle the resulting width.
- **Slide Over** (a compact window floating on top of another app) — treat your app as a compact-width iPhone layout here.
- **Picture-in-Picture** for video: don't block it, and resume cleanly when the user returns.
- **Stage Manager** — multiple app windows per stage, user arranges freely. Your app may be one of many on screen; don't assume you're alone.

## Navigation

See [`../patterns/navigation.md`](../patterns/navigation.md) for the decision matrix.

- **Regular size class:** prefer `NavigationSplitView` with sidebar + content + detail.
- **Compact size class:** the split view collapses to a `NavigationStack`; or use a `TabView` with `.sidebarAdaptable` so the same structure renders as a tab bar compact and a sidebar regular.
- **Tab bar lives at the top on iPadOS** (not the bottom). It can coexist with a toolbar.
- **Don't ship a bottom tab bar.** That's iPhone. On iPad, tab bar is at the top.
- **Drill-in inside a column** — `NavigationStack` within the detail column is common.

## Input model

### Multi-Touch

- **44×44pt minimum.** Default for tap targets.
- Standard gestures — tap, long press, swipe, drag, pinch, rotate, two-finger scroll, three-finger undo/redo/copy/paste. Don't invent new ones.
- **Two-finger trackpad scroll** translates to touch swipe in many contexts — test both.

### Pointer and keyboard

- **Hover states** — every interactive element gets a pointer effect (highlight, lift, grow). `.hoverEffect()` in SwiftUI; `UIPointerInteraction` in UIKit.
- **Focus ring** appears on keyboard navigation. Don't suppress it.
- **Keyboard shortcuts** for every menu command. Users with a keyboard expect ⌘ shortcuts — wire them up through `Commands` (SwiftUI) or `UIKeyCommand` / `UIMenuBuilder` (UIKit).
- **Tab moves focus forward**; Shift-Tab moves backward. Logical reading order.
- **Esc dismisses** sheets, popovers, alerts.
- **⌘,** opens Settings (system convention).
- **Full Keyboard Access** — every interactive element reachable via keyboard alone.

### Trackpad / mouse

- Click-and-drag, scroll, two-finger right-click, swipe between spaces.
- Right-click should reveal a context menu where one makes sense.

### Accessibility inputs

- Voice Control, Switch Control, AssistiveTouch, Full Keyboard Access, Pointer Control — all supported. Ensure every action is reachable through at least two input methods.

## Apple Pencil and Scribble

- **Pencil is a first-class input on iPad.** Detect it and route precision interactions through it.
- **Scribble** — write in any text field; the system converts to text. Works automatically for standard `UITextField` / `NSTextField` / SwiftUI `TextField`. Custom text views need Scribble integration.
- **Low-latency drawing** — use `PKCanvasView` (PencilKit) for drawing surfaces. Custom renderers need to handle Pencil force, tilt, azimuth, and the optional Squeeze gesture (Apple Pencil Pro).
- **Hover effects with Pencil** (on Apple Pencil Pro and M2 iPad Pro models) — treat like a pointer hover.
- **Palm rejection** is the system's job; don't roll your own.

## Drag and drop

iPad is where drag and drop matters most. Design around it.

- **Drag from and drop onto** every item view where movement makes sense: list rows, grid cells, document views, text selections, images.
- **Support drops from other apps.** A photo from Photos, a file from Files, a URL from Safari.
- **Spring loading** — dragging over a navigation control (tab, folder) should switch to that context after a brief hold so users can drop into a new location.
- **Visual feedback.** Highlight the drop target; animate the drop.
- **Multi-item drag.** Users can accumulate items before dropping. Support it.

## Layout

See [`../foundations/layout.md`](../foundations/layout.md) for the general rules.

- **Regular × regular size class** in all iPad orientations in full-screen. Sub-window widths can go compact.
- **Readable content width** — constrain long prose to a readable measure (`.frame(maxWidth: .readable)` in SwiftUI).
- **Symmetry and balance.** Large-screen design rewards generous whitespace and balanced spacing; don't crowd just because the canvas is big.
- **Adaptive columns** for grids: `LazyVGrid` with `GridItem(.adaptive(minimum:))` reflows naturally across widths.
- **Safe areas** — respect the home indicator at the bottom; on Stage Manager, account for window chrome.
- **Scroll edge effects** — Liquid Glass chrome auto-applies them above scrolling content.

## Typography, color, materials

- **SF Pro** is the system font. NY available.
- **Default body size 17pt, minimum 11pt.** Same as iOS.
- **Dynamic Type** — mandatory. Test at AX5.
- **Semantic colors** — `Color(.label)`, `Color(.systemBackground)`, `Color(.secondarySystemBackground)`. No hex.
- **Liquid Glass** on toolbars, tab bars, sidebars, floating chrome.
- **Standard materials** (`.ultraThinMaterial`…`.thickMaterial`) for cards in the content layer.
- **Dark Mode** — base vs elevated backgrounds, same as iOS.

Detail in [`../foundations/typography.md`](../foundations/typography.md), [`../foundations/color.md`](../foundations/color.md), [`../foundations/materials.md`](../foundations/materials.md), [`../foundations/dark-mode.md`](../foundations/dark-mode.md).

## Mac Catalyst notes

If your iPad app runs on Mac via Mac Catalyst:

- **A real menu bar appears.** Provide `Commands` (SwiftUI) or `UIMenuBuilder` (UIKit) commands for every action your app supports.
- **Window chrome, minimum sizes, resizability** — set sensible values.
- **Pointer hover, keyboard shortcuts, context menus** must all work. Test them.
- **Toolbar items are customizable** on Mac — no extra code needed with SwiftUI's toolbar.
- **Catalyst ≠ native AppKit.** For deep Mac idioms (NSDocument multi-window, AppKit-specific APIs, Quick Look providers), native macOS is the right choice.

Full macOS reference: [`./macos.md`](./macos.md).

## Common mistakes

- Bottom-anchored tab bar (that's iPhone).
- No sidebar in regular width (users expect it on iPad).
- Layout that only works at full-screen; breaks in Stage Manager at half-width.
- No pointer hover states on interactive elements.
- Missing keyboard shortcuts for the menu bar actions.
- No Scribble support in custom text views.
- Drag-and-drop absent from items users would naturally want to move.
- Touch-only buttons sized for finger taps everywhere, but no pointer affordances — looks cheap under a cursor.
- Modals (sheets) used for what should be a sidebar selection.
- Fixed-width layouts that don't adapt at AX5 Dynamic Type.
- Popovers in compact size class (system auto-adapts to a sheet — but only if you opted in).
- Ignoring Stage Manager entirely; the app is broken at half-screen.

## SwiftUI — a complete iPad app

```swift
import SwiftUI

@main
struct NotebookApp: App {
    @State private var store = NotebookStore()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(store)
        }
        .commands {
            CommandGroup(replacing: .newItem) {
                Button("New Note") { store.newNote() }
                    .keyboardShortcut("n")
            }
            CommandMenu("Format") {
                Button("Bold")       { toggleStyle(.bold) }.keyboardShortcut("b")
                Button("Italic")     { toggleStyle(.italic) }.keyboardShortcut("i")
                Button("Underline")  { toggleStyle(.underline) }.keyboardShortcut("u")
            }
        }
    }
}

struct RootView: View {
    @Environment(NotebookStore.self) private var store
    @State private var selectedFolder: Folder.ID?
    @State private var selectedNote: Note.ID?
    @State private var search = ""

    var body: some View {
        NavigationSplitView {
            List(selection: $selectedFolder) {
                Section("Notebooks") {
                    ForEach(store.folders) { folder in
                        Label(folder.name, systemImage: folder.systemImage)
                            .tag(folder.id)
                            .badge(folder.unreadCount)
                    }
                }
            }
            .navigationTitle("Notes")
            .navigationSplitViewColumnWidth(min: 200, ideal: 260, max: 340)
        } content: {
            if let id = selectedFolder, let folder = store.folder(id) {
                List(folder.notes, selection: $selectedNote) { note in
                    NoteRow(note: note)
                        .tag(note.id)
                        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                            Button(role: .destructive) { store.archive(note.id) } label: {
                                Label("Archive", systemImage: "archivebox")
                            }
                        }
                        .contextMenu {
                            Button("Open in New Window") { openInNewWindow(note.id) }
                            Button("Duplicate") { store.duplicate(note.id) }
                            Divider()
                            Button("Delete", role: .destructive) { store.delete(note.id) }
                        }
                }
                .searchable(text: $search, placement: .sidebar)
            } else {
                ContentUnavailableView("Select a notebook", systemImage: "books.vertical")
            }
        } detail: {
            if let id = selectedNote {
                NoteEditor(id: id)
                    .focusedSceneValue(\.currentNoteID, id)      // for menu commands
            } else {
                ContentUnavailableView("Select a note", systemImage: "doc.text")
            }
        }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button { store.newNote() } label: {
                    Label("New Note", systemImage: "square.and.pencil")
                }
                .keyboardShortcut("n")
            }
            ToolbarItem(placement: .primaryAction) {
                ShareLink(item: selectedNoteURL)
                    .disabled(selectedNote == nil)
            }
        }
        .dropDestination(for: Data.self) { data, _ in
            for datum in data { store.importFile(datum) }
            return true
        }
    }
}
```

What this demonstrates: `NavigationSplitView` with sidebar + content + detail, searchable on the sidebar, swipe actions, context menu, drag-drop handling, menu bar commands via `.commands`, keyboard shortcuts, and `ShareLink` integration — all of which give an iPad app its proper feel across Stage Manager and Split View.

## See also

- [`../foundations/accessibility.md`](../foundations/accessibility.md)
- [`../foundations/layout.md`](../foundations/layout.md) — size classes, iPad dimensions.
- [`../components/bars.md`](../components/bars.md) — tab bar + toolbar + sidebar on iPad.
- [`../components/split-views.md`](../components/split-views.md) — fluid-width handling.
- [`../patterns/navigation.md`](../patterns/navigation.md) — the paradigm matrix.
- [`../patterns/multitasking.md`](../patterns/multitasking.md) (pending) — Stage Manager and Split View.
- [`../patterns/drag-and-drop.md`](../patterns/drag-and-drop.md) (pending) — deeper drag-and-drop design.
- [`../inputs/apple-pencil.md`](../inputs/apple-pencil.md) (pending) — Pencil-specific design.
- [`../inputs/pointer-and-keyboard.md`](../inputs/pointer-and-keyboard.md) (pending) — pointer/keyboard UX.
- [`./macos.md`](./macos.md) — when running via Mac Catalyst.
