# Collections and scrolling

**HIG sources:**
- https://developer.apple.com/design/human-interface-guidelines/collections
- https://developer.apple.com/design/human-interface-guidelines/scroll-views

**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS (scroll views only; collections not supported on watchOS)

Collections are for image-heavy ordered content. Lists are for text (see [`./lists-and-tables.md`](./lists-and-tables.md)). Scroll views are the container everything else lives inside — most of the rules come down to "don't fight the platform's scroll."

## Collections

### When to use

- **Image-based content.** Photos, albums, movie posters, product grids.
- **Variably sized or visually rich items.** Lists handle uniform text rows well; collections handle shape variety.
- **Ordered sets** where people scan visually.

If your items are mostly text, use a list or table instead. Collections with text-heavy cells are harder to scan than rows.

### Rules

- **Standard row or grid layout whenever possible.** Don't invent a custom layout that confuses people or calls attention to itself.
- **Adequate padding around images** so focus, hover, and selection affordances don't overlap content.
- **Standard gestures by default:** tap to select, touch-and-hold to edit, swipe to scroll.
- **Animate insertions, deletions, and reorders.** Users track state changes better when they see the motion. The system supplies standard animations; custom animations are allowed but must be purposeful.
- **Don't change layout mid-interaction.** If the grid density changes, do it in response to an explicit action (rotation, window resize, user toggle), not silently.

### Platform notes

- **iOS / iPadOS:** dynamic layout changes must be obvious and easy to track.
- **macOS / tvOS / visionOS:** no additional considerations.
- **watchOS:** collections are not supported — use a list.

## Scroll views

A scroll view has no visible chrome of its own beyond an optional translucent scroll indicator. That indicator tells people where they are in the content (near the start, middle, or end).

### Rules

- **Support default gestures and keyboard shortcuts.** If you build a custom scroll view, match the platform's elastic behavior; don't fight it.
- **Make scrollability visible.** Users generally swipe instinctively, but a partially-visible row or a scroll indicator tells them content continues.
- **Don't nest same-axis scroll views.** Vertical inside vertical is a trap. Vertical + horizontal nesting is fine.
- **Page-by-page scrolling** for content that benefits from a fixed unit (stories, slides, Weather's saved locations). Define the page size and include an overlap unit (e.g., a line of text) to preserve context across page boundaries.
- **Auto-scroll to the point of action** when users can't see what's happening: found text, new insertion point, selection extending off-screen, drag past the viewport edge. Scroll only as much as needed — don't yank the whole view.
- **Zoom limits.** Maximum and minimum zoom values must be sensible. Zooming to where a single character fills the screen rarely helps.

### Scroll edge effects

iOS 26 / iPadOS 26 / macOS 26 (Liquid Glass era) use **scroll edge effects** — a variable blur that transitions between content and adjacent floating controls (toolbars, tab bars, sidebars).

- **System applies automatically** in most cases when a pinned element overlaps scrolling content.
- **Two styles:** `.soft` (subtle, default for iOS/iPadOS) and `.hard` (stronger boundary, for macOS with interactive text, backless controls, or pinned table headers).
- **One effect per view.** In split views, each pane can have its own effect; keep heights consistent for alignment.
- **Not decorative.** Scroll edge effects exist to clarify where content meets controls — don't use them as generic styling.

### Pull to refresh

Lives on `UIRefreshControl` (UIKit) and SwiftUI's `.refreshable { ... }`.

- **Determinate indicator while refreshing.** Dismiss when done.
- **Not for background sync.** Background refresh should use inline chrome instead (status in the toolbar, a subtle spinner elsewhere).
- **Fail visibly.** If a refresh errors, surface it where the user can recover — don't silently leave the spinner looping or drop the refresh result.

## Platform specifics

### iOS / iPadOS

- **Page control** for page-by-page scroll views (Weather's location dots). If you show a page control, don't also show the scroll indicator on the same axis — redundant.

### macOS

- **Scroll indicators are "scroll bars"** in macOS usage.
- **Small or mini scroll bar styles** for tight panels. Use the same size across all controls in the panel.

### tvOS

- **Views scroll; scroll views aren't distinct objects with indicators.** The focus engine auto-scrolls to keep focused items visible.
- Layout must tolerate the focus-engine scale — padding between rows so scaled-up focused items don't overlap.

### visionOS

- **Scroll indicator lives at fixed positions:** vertically centered on the trailing edge for vertical scroll; horizontally centered at the bottom edge for horizontal.
- **Thicker than on iOS.** Increase content margins if they're tight so the indicator doesn't overlap content.
- **Look to Scroll:** users can scroll using only their eyes. Enable per scroll view.
  - **Use for reading / browsing views** (long text, media feeds).
  - **Don't use for dense control-heavy views or secondary content** — precise scrolling with the eyes is hard; dense UI magnifies the problem.
  - **Consistency across similar views.** If one feed supports Look to Scroll, all feeds should.
  - **Clear edges.** Prefer full-width or full-height scroll views; add clear boundaries when insetting.
  - **Remove custom scroll-position-based effects** (parallax, scroll-driven animation) before enabling Look to Scroll — they fight the feature.

### watchOS

- **Vertical scrolling is the default** — the Digital Crown drives it.
- **Tab views as pages** for horizontal paging. Place a vertical `TabView` with the Digital Crown page indicator.
- **Fixed-height pages** first, variable-height pages after, and sparingly. Mixed-height paging can disorient users.

## Common mistakes

- Collection of text rows (use a list).
- Grid with no padding — focus effects hit adjacent cells.
- Nested vertical scroll views.
- Scroll indicator + page control on the same axis.
- Custom scroll physics that don't match the platform's rubber-band.
- Auto-scroll that yanks the user to a location they didn't ask for.
- Custom scroll edge effect when the system already provides one.
- Pull-to-refresh that spins forever on network error.
- watchOS app that scrolls horizontally for no reason.
- visionOS Look to Scroll in a dense control view where precision matters.

## SwiftUI

```swift
// Grid collection with adequate spacing.
ScrollView {
    LazyVGrid(columns: [GridItem(.adaptive(minimum: 140), spacing: 12)],
              spacing: 12) {
        ForEach(albums) { album in
            NavigationLink(value: album.id) {
                AlbumCard(album: album)
            }
        }
    }
    .padding(16)
}

// Horizontal scroll inside vertical — fine.
ScrollView(.vertical) {
    VStack(alignment: .leading, spacing: 24) {
        ForEach(sections) { section in
            Text(section.title).font(.title2.bold()).padding(.horizontal)
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 12) {
                    ForEach(section.items) { ItemCard(item: $0) }
                }
                .padding(.horizontal)
            }
        }
    }
}

// Page-by-page with SwiftUI's TabView.
TabView(selection: $page) {
    ForEach(Array(story.pages.enumerated()), id: \.offset) { idx, page in
        PageView(page: page).tag(idx)
    }
}
.tabViewStyle(.page(indexDisplayMode: .always))
.indexViewStyle(.page(backgroundDisplayMode: .always))

// Pull to refresh — iOS / iPadOS / macOS (List).
List(items) { ItemRow(item: $0) }
    .refreshable { await store.reload() }

// Auto-scroll to a value after a search lands.
ScrollViewReader { proxy in
    List(items) { item in ItemRow(item: item).id(item.id) }
        .onChange(of: foundItemID) { _, id in
            guard let id else { return }
            withAnimation { proxy.scrollTo(id, anchor: .center) }
        }
}

// visionOS — opt in to Look to Scroll for a reading view.
#if os(visionOS)
Text(longArticle)
    .lookToScroll()          // hypothetical SwiftUI modifier; see Apple's API for current name
#endif
```

## UIKit / AppKit

```swift
// UICollectionView with compositional layout — modern API.
let section = NSCollectionLayoutSection(group: NSCollectionLayoutGroup.horizontal(
    layoutSize: .init(widthDimension: .fractionalWidth(1.0),
                      heightDimension: .estimated(200)),
    subitems: [NSCollectionLayoutItem(
        layoutSize: .init(widthDimension: .fractionalWidth(0.5),
                          heightDimension: .fractionalHeight(1.0)))]
))
section.interGroupSpacing = 12
section.contentInsets = .init(top: 12, leading: 16, bottom: 12, trailing: 16)
let layout = UICollectionViewCompositionalLayout(section: section)
let collection = UICollectionView(frame: .zero, collectionViewLayout: layout)

// NSCollectionView on macOS
let collection = NSCollectionView()
let flow = NSCollectionViewFlowLayout()
flow.itemSize = NSSize(width: 160, height: 160)
flow.minimumInteritemSpacing = 12
flow.minimumLineSpacing = 12
collection.collectionViewLayout = flow
```

## See also

- [`./lists-and-tables.md`](./lists-and-tables.md) — text-oriented row-based content.
- [`./split-views.md`](./split-views.md) — multi-pane containers.
- [`./bars.md`](./bars.md) — toolbars and tab bars that scroll edge effects transition against.
- [`../patterns/loading.md`](../patterns/loading.md) — loading within scroll views.
