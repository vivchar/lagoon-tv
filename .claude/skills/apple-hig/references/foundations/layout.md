# Layout

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/layout
**Last verified:** 2026-04-20
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

A consistent layout that adapts to context is the difference between "looks like an app" and "feels native." Respect safe areas, let content extend to the edges, and trust the Liquid Glass control layer to float above it.

## Table of contents

- [Core rules](#core-rules)
- [Visual hierarchy](#visual-hierarchy)
- [Adaptability](#adaptability)
- [Guides and safe areas](#guides-and-safe-areas)
- [iOS](#ios)
- [iPadOS](#ipados)
- [macOS](#macos)
- [tvOS](#tvos)
- [visionOS](#visionos)
- [watchOS](#watchos)
- [Specifications](#specifications)
- [Common mistakes](#common-mistakes)
- [SwiftUI](#swiftui)
- [UIKit / AppKit](#uikit--appkit)

## Core rules

- **Group related items.** Use negative space, color, background shape, material, or separators — but keep content and controls visually distinct.
- **Give essential information space.** Don't crowd the primary task with nonessentials.
- **Extend content to the edges.** Backgrounds and scroll content reach the full display. Controls (tab bars, toolbars, sidebars) float above on the Liquid Glass layer — don't leave a colored band for them.
- **Background extension views.** When content doesn't span the full window (sidebar + detail), use a background extension view so the area behind the sidebar reads as continuous.

## Visual hierarchy

- **Differentiate controls from content** with Liquid Glass. Controls use the functional glass layer; content stays in the content layer. Use a **scroll edge effect** to transition between the two rather than a solid bar background.
- **Reading order places the most important item top-leading.** Left-to-right languages read top-down, leading-trailing. RTL reverses the horizontal axis. Use logical `leading`/`trailing`, never left/right.
- **Align components** to communicate organization. Alignment + indentation together convey hierarchy.
- **Progressive disclosure** signals more content: scroll indicators, partially visible items, inspectors.
- **Logical grouping + spacing** makes controls scannable. Unrelated controls placed together look related — don't.

## Adaptability

Your layout must handle:

- Different screen sizes, resolutions, color spaces.
- Portrait and landscape (prefer both on iOS unless the experience demands one).
- System features (Dynamic Island, Camera Control).
- External displays, Display Zoom, resizable iPad windows.
- **Dynamic Type** (see [`typography.md`](./typography.md)).
- RTL, locale-specific date/time/number formatting, font variation, text length.

Design against the biggest **and** smallest target first — between them is easy.

## Guides and safe areas

- A **layout guide** defines a rectangular region for positioning/alignment (standard margins, readable content width). Custom guides are fine.
- A **safe area** excludes space occupied by system chrome (toolbars, tab bars, Dynamic Island, camera housing). Always respect safe areas.
- SwiftUI: `.safeAreaInset(edge:)`, `.safeAreaPadding(edge:)`, `.containerRelativeFrame(...)`.
- UIKit: `safeAreaLayoutGuide` / `layoutMarginsGuide`.
- AppKit: `NSLayoutGuide`, toolbar/title bar height.

## iOS

- **Support both portrait and landscape** unless the experience really demands one.
- **Prefer a full-bleed game UI.** Fill the screen; accommodate the rounded corners, sensor housing, and Dynamic Island. Offer letterbox/pillarbox only as a player option.
- **Avoid full-width buttons.** Inset buttons from the edges per system margins. If you must ship a full-width button, harmonize with the hardware corner radius and align with adjacent safe areas.
- **Hide the status bar only when it adds value.** Games, full-screen media — yes. Normal app screens — no.
- Dynamic Island and the camera control live in the top safe area. Content flows around them.

## iPadOS

- **Windows are resizable** to a minimum width/height, like on macOS. Design for the full range, not a fixed canvas.
- **Defer switching to a compact layout** until the full one truly doesn't fit. Stability > flipping early.
- Hide tertiary columns (inspectors) first as width narrows; keep primary + detail last.
- **Test at half, third, and quadrant widths.** The system offers these quickly.
- **Convertible tab bar + sidebar** (`TabView` with `.sidebarAdaptable`) — one component becomes the other depending on width. Ship this instead of picking one.
- Support Stage Manager, Split View, Slide Over.

## macOS

- **Never put critical controls or info at the bottom of a window.** Users drag window bottoms below the screen.
- **Avoid displaying content inside the camera housing** at the top of MacBook displays.
- Use standard window chrome — title bar, traffic lights, toolbar, sidebar.
- Respect the user's window size; restore on next launch.
- Support full-screen and split-screen spaces.

## tvOS

Layouts do not adapt to screen size — apps render identical UI on every TV. Design once for a safe canvas.

- **Safe area:** 60pt top/bottom, 80pt sides. Primary content stays inside.
- **Padding between focusable elements:** account for the focus-engine scale-up; don't let focused items overlap adjacent content.
- Allow only deliberately offscreen content outside the safe area.
- Use consistent spacing in grids. Include extra vertical space between a row title and the row content.
- Partially hidden offscreen content should be symmetric (same width on each side) so attention goes to the centered item.

## visionOS

- Keep content within a window's bounds. System window controls (close, resize, Share menu) live just outside. Content pushed into those areas fights the controls.
- **Center important content and controls.** Easier to spot, easier to reach.
- Use **ornaments** for controls that don't fit inside the window (toolbars, tab bars). Ornaments sit adjacent to the window without interfering with system chrome.
- **Keep interactive components apart.** Center-to-center ≥ **60pt** so the hover/gaze affordance doesn't collide.
- Depth on content is fine but extending too far along z clips.

## watchOS

- **Edge-to-edge content.** The bezel is your padding.
- **Max two text buttons or three glyph buttons side by side.** Prefer full-width text buttons.
- Minimize padding between elements — space is precious.
- Support autorotation in views meant to be shown to others (QR code, photo).

## Specifications

### iPhone dimensions (pt, portrait)

Recent models:

| Model | Points |
|---|---|
| iPhone 17 Pro Max | 440×956 |
| iPhone 17 Pro | 402×874 |
| iPhone Air | 420×912 |
| iPhone 17 | 402×874 |
| iPhone 16 Pro Max | 440×956 |
| iPhone 16 Pro | 402×874 |
| iPhone 16 Plus | 430×932 |
| iPhone 16 | 393×852 |
| iPhone 16e | 390×844 |
| iPhone 15 Pro Max | 430×932 |
| iPhone 15 Pro | 393×852 |
| iPhone SE 4.7" | 375×667 |

### iPad dimensions (pt, portrait)

| Model | Points |
|---|---|
| iPad Pro 12.9" | 1024×1366 |
| iPad Air 13" | 1024×1366 |
| iPad Pro 11" | 834×1194 |
| iPad Air 11" | 820×1180 |
| iPad 11" | 820×1180 |
| iPad mini 8.3" | 744×1133 |

### Size classes (iOS / iPadOS)

- **Regular** — larger screens or landscape.
- **Compact** — smaller screens or portrait.

All iPads are **regular width, regular height** in all orientations (use `NavigationSplitView`).

All iPhones in portrait are **compact width, regular height**.

iPhones in landscape:

- Plus / Max / Air → **regular width, compact height**.
- Standard / Pro / mini / SE → **compact width, compact height**.

### Apple Watch dimensions (pixels)

| Series | Size | Width | Height |
|---|---|---|---|
| Ultra 3 | 49mm | 422 | 514 |
| Ultra 1–2 | 49mm | 410 | 502 |
| 10 / 11 | 46mm | 416 | 496 |
| 10 / 11 | 42mm | 374 | 446 |
| 7–9 | 45mm | 396 | 484 |
| 7–9 | 41mm | 352 | 430 |
| 4–6 / SE | 44mm | 368 | 448 |
| 4–6 / SE | 40mm | 324 | 394 |

## Common mistakes

- Not respecting the safe area at the home indicator.
- Full-width iOS buttons flush to the edge.
- Putting primary controls at the bottom of a macOS window.
- Ignoring the camera housing on MacBooks.
- Assuming a single iPad width (use resizable design from day one).
- tvOS content touching the screen edge.
- visionOS content encroaching on system window chrome.
- Using `leading`/`trailing` mental model but `.padding(.left, ...)` in code.
- Forgetting to hide inspector columns as iPad width narrows.

## SwiftUI

```swift
// Respect safe areas and provide a scroll edge effect.
NavigationStack {
    ScrollView {
        LazyVStack(spacing: 16) {
            ForEach(items) { ItemRow(item: $0) }
        }
        .padding(.horizontal)                   // system-standard horizontal margin
    }
    .navigationTitle("Inbox")
    .toolbarTitleDisplayMode(.large)
}
.safeAreaInset(edge: .bottom) {
    Button("Compose") { }
        .buttonStyle(.borderedProminent)
        .padding()
}

// iPad: convertible tab bar ↔ sidebar.
TabView {
    Tab("Home", systemImage: "house") { HomeView() }
    Tab("Library", systemImage: "books.vertical") { LibraryView() }
    Tab("Search", systemImage: "magnifyingglass", role: .search) { SearchView() }
}
.tabViewStyle(.sidebarAdaptable)

// Responsive to size class.
@Environment(\.horizontalSizeClass) private var hClass

var body: some View {
    if hClass == .regular {
        NavigationSplitView { SidebarView() } detail: { DetailView() }
    } else {
        NavigationStack { ListView() }
    }
}
```

## UIKit / AppKit

```swift
// UIKit — safe area constraints.
let guide = view.safeAreaLayoutGuide
stack.translatesAutoresizingMaskIntoConstraints = false
NSLayoutConstraint.activate([
    stack.leadingAnchor.constraint(equalTo: guide.leadingAnchor, constant: 20),
    stack.trailingAnchor.constraint(equalTo: guide.trailingAnchor, constant: -20),
    stack.topAnchor.constraint(equalTo: guide.topAnchor),
    stack.bottomAnchor.constraint(equalTo: guide.bottomAnchor),
])

// AppKit — minimum window size and restoration.
window.minSize = NSSize(width: 720, height: 480)
window.isRestorable = true
```

## See also

- [`typography.md`](./typography.md) — Dynamic Type and reflow.
- [`materials.md`](./materials.md) — Liquid Glass vs content layer.
- [`../platforms/ios.md`](../platforms/ios.md) / [`../platforms/macos.md`](../platforms/macos.md) / [`../platforms/ipados.md`](../platforms/ipados.md) — platform-specific layout rules.
- [`../components/split-views.md`](../components/split-views.md) — sidebar + detail.
