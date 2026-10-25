# tvOS

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/designing-for-tvos
**Last verified:** 2026-04-21
**Applies to:** Apple TV (tvOS)

Ten feet away, no cursor, no touch. A Siri Remote with a touch-enabled surface, directional buttons, and a microphone — and sometimes a game controller. Every interactive element is **focusable**; the focus engine does the navigation. The single biggest tvOS mistake is designing for a pointer.

## Table of contents

- [What makes tvOS tvOS](#what-makes-tvos-tvos)
- [Device traits](#device-traits)
- [The focus engine](#the-focus-engine)
- [Siri Remote](#siri-remote)
- [Navigation](#navigation)
- [Layout](#layout)
- [Typography](#typography)
- [Color and materials](#color-and-materials)
- [Multiuser and profiles](#multiuser-and-profiles)
- [Top Shelf](#top-shelf)
- [Picture-in-Picture](#picture-in-picture)
- [Common mistakes](#common-mistakes)
- [SwiftUI — a tvOS screen](#swiftui--a-tvos-screen)
- [See also](#see-also)

## What makes tvOS tvOS

- **10-foot UI.** Users are on a couch, not at a screen.
- **Focus engine does navigation.** Users move the focused item with directional input; your layout must support predictable, intentional focus movement.
- **Focused items scale up.** Design with room around every focusable cell so the scaled-focused view doesn't overlap neighbors.
- **Edge-to-edge cinematic visuals.** Big art, subtle motion, rich audio.
- **Remote-first; game controller optional.** Many apps also support Apple's MFi-certified game controllers.
- **Multiuser is real.** Apple TV supports multiple family profiles.
- **Long sessions.** Someone watches a movie for two hours. Design doesn't need to optimize for glancing.

## Device traits

- **Display.** Typically 1080p or 4K; a wide range of sizes (28" to 85"+). Don't assume a specific physical size.
- **Ergonomics.** 8+ feet away. Text must be legible from across the room. Targets must be chunky enough that focus affordances read.
- **Inputs:** Siri Remote (touch + directional), game controller, iPhone virtual remote (via Control Center), Siri (voice), AirPlay (media).
- **Session length.** Many hours for media; shorter for utilities and games.
- **System features:** Top Shelf, Control Center, Picture-in-Picture, AirPlay 2, HomeKit, Continuity, multiple profiles, Family Sharing, parental controls, SharePlay.

## The focus engine

The core interaction model. Users move focus via the remote or game controller; focus indicates what their next action affects.

### Rules

- **Every interactive element must be focusable.** Non-interactive decoration isn't focusable.
- **Focus moves in logical direction** — up/down/left/right. The engine picks the best candidate given your layout. Predictability matters; don't ship a layout where pressing Right lands on a random element.
- **Focused elements scale up.** By default 110%+ with a subtle lift. Your layout must have padding so the focused item doesn't crop its neighbors.
- **Focused items round corners.** The focus engine applies rounded corners to focused items (if you supply images with sharp edges, they'll look pre-clipped). Don't apply your own corner radius that fights the focus effect.
- **Don't rely on color alone** to show focus — scale, lift, and shadow matter more. Don't disable the focus effect.
- **Focus rings appear** on custom controls that use the system focus API. Don't replace them with custom bespoke highlights.
- **Segmented controls change on focus**, not click. Place them with enough space so users don't accidentally focus them.

## Siri Remote

The current Siri Remote (2021+) has:

- **Click pad** — a touch-enabled surface around a directional ring that both swipes and clicks.
- **Select** (center click).
- **Back** (top-right).
- **TV/Home**.
- **Play/Pause**.
- **Volume +/−**.
- **Siri / Mute**.
- **Power**.

### Behavior

- **Swipe on the click pad** → move focus.
- **Click direction** → step focus.
- **Center click** → activate focused item.
- **Back** → navigate back (in a stack) or dismiss a modal. Menu on older remotes had the same behavior.
- **Swipe-down on a playing video** → reveal metadata / alternate audio / subtitles.
- **Siri hold** → voice input. Titles, artists, "skip ahead 30 seconds."

Older Siri Remote (2015) with touch surface works the same way; just a slightly different physical layout.

## Navigation

### Structure

- **Top-level sections** → Tab bar at the top. 68pt tall, fixed 46pt from the top of the screen, translucent background; only the selected tab is opaque.
- **Content grids** within a tab. The focus engine traverses cells.
- **Split view** for filter-driven content — filters on the primary pane, results in the secondary. Use split views over segmented controls for filtering.
- **Navigation stack** for drill-in inside a tab. The Menu/Back button returns up the stack.

### Tab bar specifics

- Selected tab gets a drop shadow for emphasis.
- When there are more tabs than fit, the rightmost truncates with a fade.
- **Consistent ordering for live content:** live → recorded → other.
- **Back always returns focus to the tab bar** from a content area.
- **Tab bar can scroll offscreen** on single-view tabs as the user scrolls content; it pins when the tab's content is a split view.

See [`../components/bars.md`](../components/bars.md#tab-bars) for tab bar rules.

## Layout

- **Safe area:** 60pt top/bottom, 80pt left/right. Primary content stays inside.
- **Layouts do NOT auto-adapt to screen size.** Your UI renders identical dimensions on any TV — design once for the safe canvas.
- **Padding between focusable elements** — account for the focus-engine scale-up. Too-tight cells overlap when one focuses.
- **Consistent grid spacing.** Inconsistent gaps break the "scanning" feel of a focused grid.
- **Partially hidden offscreen content** should be symmetric width on both sides — directs attention to the center.
- **Don't pre-round image corners** — the focus engine handles rounding on focus; your masked corners will look clipped.

See [`../foundations/layout.md`](../foundations/layout.md#tvos).

## Typography

- **SF Pro** is the system font. NY available.
- **Default body 29pt, minimum 23pt.** Much larger than iOS — 10-foot readability.
- **Dynamic Type supported** but used less commonly than on iOS.

tvOS built-in text styles (used by default with the relevant font APIs):

| Style | Weight | Size | Leading |
|---|---|---|---|
| Title 1 | Medium | 76 | 96 |
| Title 2 | Medium | 57 | 66 |
| Title 3 | Medium | 48 | 56 |
| Headline | Medium | 38 | 46 |
| Body | Medium | 29 | 36 |
| Caption 1 | Medium | 25 | 32 |

Full table: [`../foundations/typography.md`](../foundations/typography.md#tvos-built-in-text-styles).

## Color and materials

- **Limited palette.** Coordinate with your app logo. Subtle color.
- **Don't use color alone for focus.** The focus engine uses scale + lift + shadow.
- **Test on multiple TVs.** Color reproduction varies wildly. Test HDR and SDR panels; verify in default and cinema picture modes.
- **Liquid Glass** applies to navigation and system experiences (Top Shelf, Control Center). Focused image views and buttons adopt Liquid Glass when focused.
- **Dark Mode** is supported; follow [`../foundations/dark-mode.md`](../foundations/dark-mode.md) semantics.
- **Materials** — thicker materials for full-screen overlays with text; thinner for context preservation. Full table: [`../foundations/materials.md`](../foundations/materials.md#tvos).

## Multiuser and profiles

Apple TV supports multiple family profiles. Make sign-in easy and infrequent.

### Rules

- **Handle shared sign-in** — when the family account signs in, your app recognizes all authorized profiles.
- **Automatic profile switching** when the viewer changes (if your service has a profile concept).
- **Minimize re-authentication.** Users hate typing passwords with a remote — support AirDrop-based device handoff or iPhone-assisted sign-in where possible.
- **Parental controls** — respect them. If the current profile is restricted, filter content accordingly.

## Top Shelf

The featured content area at the top of your app's icon on the Home Screen. Users see it as they hover your app.

- **Showcase recent or recommended content** — resumed shows, new episodes, personalized picks.
- **Keep it dynamic.** Static Top Shelf art is a missed opportunity.
- **Deep-link each item** so selecting it jumps straight to the content.
- **Different from the app icon.** Top Shelf art is separate.

## Picture-in-Picture

Users watch a video while browsing another app. tvOS handles the windowing; your job is to cooperate.

- **Support PiP for any video playback.** `AVPlayerViewController` with `allowsPictureInPicturePlayback` on. SwiftUI `VideoPlayer` handles it automatically.
- **Handle rejoining the video** cleanly when the user returns.
- **State restoration** — the video should resume where it was.

## Common mistakes

- Custom cursor or pointer metaphor.
- Focus disabled or replaced with a custom highlight that doesn't scale.
- Corners pre-rounded on images (conflicts with focus rounding).
- Focused cells that overlap neighbors because there wasn't enough padding.
- Text under 23pt.
- Reliance on color alone for focus state.
- Tab bar with a custom fixed height that fights the 68pt standard.
- Segmented controls used for filtering (prefer a split view).
- Focusable items positioned inside dense non-focusable decoration (hard to land on).
- Top Shelf static art.
- Long password-typing flows (handoff to iPhone).
- Edge-to-edge content that ignores the 60pt safe area at top/bottom.
- Background music competing with the current playing audio (respect `AVAudioSession`).
- Layouts that assume a specific TV size.

## SwiftUI — a tvOS screen

```swift
import SwiftUI

@main
struct WatchTVApp: App {
    var body: some Scene {
        WindowGroup { RootView() }
    }
}

struct RootView: View {
    var body: some View {
        TabView {
            LiveView()
                .tabItem { Text("Live") }
            OnDemandView()
                .tabItem { Text("On Demand") }
            LibraryView()
                .tabItem { Text("Library") }
            SearchView()
                .tabItem { Text("Search") }
        }
    }
}

struct OnDemandView: View {
    let sections: [Section]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 48) {
                ForEach(sections) { section in
                    VStack(alignment: .leading, spacing: 16) {
                        Text(section.title)
                            .font(.title2.weight(.semibold))
                            .padding(.leading, 80)                  // safe area

                        ScrollView(.horizontal, showsIndicators: false) {
                            LazyHStack(spacing: 40) {
                                ForEach(section.items) { item in
                                    PosterCell(item: item)
                                }
                            }
                            .padding(.horizontal, 80)
                        }
                    }
                }
            }
            .padding(.vertical, 60)
        }
        .background(Color.black.ignoresSafeArea())
    }
}

struct PosterCell: View {
    let item: Item

    var body: some View {
        NavigationLink(value: item) {
            VStack(alignment: .leading, spacing: 8) {
                Image(item.poster)
                    .resizable()
                    .aspectRatio(2/3, contentMode: .fit)
                    .frame(width: 220)                              // do not pre-round corners
                Text(item.title)
                    .font(.headline)
                    .foregroundStyle(.primary)
                    .lineLimit(1)
                Text(item.subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            .frame(width: 220)
        }
        .buttonStyle(.card)                                         // the system focus card style
    }
}

// Video playback with Picture-in-Picture via SwiftUI's VideoPlayer.
struct PlayerView: View {
    let url: URL

    var body: some View {
        VideoPlayer(player: AVPlayer(url: url))
            .ignoresSafeArea()
            .onAppear { AVPlayer.shared?.play() }
    }
}
```

What this demonstrates: a top tab bar, a grid of focusable cards with explicit 80pt horizontal padding for the safe area, the system `.card` button style that handles focus scaling/rounding correctly, and `VideoPlayer` that supports PiP with zero configuration.

## See also

- [`../foundations/layout.md`](../foundations/layout.md#tvos) — safe areas and grids.
- [`../foundations/typography.md`](../foundations/typography.md#tvos-built-in-text-styles) — tvOS text styles.
- [`../foundations/color.md`](../foundations/color.md#tvos) — palette conservatism.
- [`../components/bars.md`](../components/bars.md#tab-bars) — tvOS tab bar specifics.
- [`../components/lists-and-tables.md`](../components/lists-and-tables.md#tvos) — focus padding rules.
- [`../components/split-views.md`](../components/split-views.md#tvos) — split-view-over-segmented-control for filtering.
- [`../inputs/focus-and-remote.md`](../inputs/focus-and-remote.md) (pending) — deep focus engine guidance.
- [`../inputs/game-controllers.md`](../inputs/game-controllers.md) (pending) — controller support.
- [`../patterns/live-viewing-apps.md`](../patterns/live-viewing-apps.md) (pending) — live content ordering.
