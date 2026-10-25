# Live-viewing apps

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/live-viewing-apps
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS (live video apps); tvOS is the canonical platform

Live TV and live streaming apps are different from video-on-demand. Live content has **now-ness** — a specific moment the viewer cares about missing. Design should elevate the live experience: prioritize live content on every screen, minimize the interval between launch and playback, and make live content visually distinct from VOD.

## Rules

- **Feature live content prominently.** First tab. Top of the first screen. One tap (or zero) from launch to content.
- **"Watch Now" button on featured content.** On tap, the button disappears, the UI recedes, and full-screen live video fills the screen.
- **Live content looks live.** Playing the video is the strongest signal; augment with a **LIVE badge**, a sash on thumbnails, or a colored border.
- **Progress indicator for in-progress content.** Users want to know how far through a game / show they're about to jump. A thin progress bar beneath or on top of the thumbnail works.
- **Primary action is playback.** Other actions (record, restart, download, favorite) come second and are consistently ordered throughout the app — e.g., Watch, Start Over, Record, Favorite.
- **Visual feedback on channel change.** When the user changes channels, the UI must confirm immediately — even before the new stream buffers. Loading placeholder, channel badge.
- **Audio matches context.** Live audio continues while the user browses within the live-viewing context (content footer, EPG, thumbnails of live). Audio stops when the user navigates away from the live area entirely.

## Electronic Program Guide (EPG)

Most live-viewing apps ship an EPG — a time-grid view of what's on each channel.

### Rules

- **Current now + current channel highlighted.** When the EPG opens, the user immediately spots "what I'm watching, at the current time."
- **Effortless browsing.** Page, scroll, jump. Use Page Up / Page Down or their touch equivalents on tvOS remotes.
- **Favorites / My Channels group.** Quick access to what the user cares about.
- **Category grouping.** Movies / TV / Kids / Sports / Popular. Consistent with thumbnails elsewhere in the app.
- **Play in background while browsing.** Continue playing in Picture-in-Picture or the background so users don't lose their spot while exploring.

## Content footer (during playback)

A persistent thin footer over full-screen playback that lets users browse without leaving.

### Rules

- **Subtle treatment** — darkening, gradient — keeps text legible without dominating.
- **Currently-playing badge on the thumbnail.** Users identify "where I am" in the footer.
- **Match categories in the footer to the EPG.** Consistent mental model.
- **Predictable invocation + dismissal.** Swipe up reveals, swipe down dismisses. Or a dedicated button.

## Cloud DVR

If your service supports recording:

- **Start / stop recording from the info panel.** While playing live, users reveal a panel with record controls.
- **Future recording via a details view.** Selecting a program in the EPG → details → "Record" / "Record All Episodes."
- **Record-only-certain-conditions** — current episode only, new episodes only, games involving specific teams. Match real-world viewer preferences.
- **Playback + management inside the cloud DVR area.** View recorded content, delete, edit recording settings.
- **Storage management.** Automatic overwrite-oldest or overwrite-watched to avoid running out.

## Live Activities for sports, events, shows

Pair live viewing with a **Live Activity** so users glance at scores / progress on Lock Screen / Dynamic Island / StandBy without opening the app. See [`../technologies/widgets-and-live-activities.md`](../technologies/widgets-and-live-activities.md).

## Platform specifics

### tvOS (canonical platform for live TV)

- **Live tab first** in the tab bar.
- **Compatible remote buttons** — Guide / Channel Up / Channel Down buttons behave per standard. See [`../inputs/focus-and-remote.md`](../inputs/focus-and-remote.md#compatible-remotes-and-live-tv).
- **Back button during playback** → pause or exit per standard.
- **Continue audio during browsing** inside the live context.

### iOS / iPadOS

- **Picture-in-Picture** for live when the user multitasks. Essential.
- **Live Activity + Dynamic Island** for scores and breaking news. Essential for sports apps.

### macOS

- **Menu bar access for live channel switching** — menu bar extras with the current channel / score.
- **Spaces** — full-screen live video in its own Space.

### visionOS

- **Full Space for immersive viewing** of live events (concerts, sports).
- **Ornament toolbars** for controls that don't steal space from video.

## Common mistakes

- Live content buried behind multiple taps on launch.
- Live content visually indistinguishable from VOD.
- No progress indicator on in-progress content.
- Channel change with no visual feedback (user wonders if it worked).
- Audio continuing after the user leaves the live context.
- Playback interruption every time the user browses the EPG.
- No Live Activity for events where users would glance.
- No Picture-in-Picture on iPhone.
- Content footer too intrusive (full opacity, blocks video).
- Cloud DVR UX that doesn't let users manage storage.
- EPG that doesn't default to "now".

## SwiftUI

```swift
import SwiftUI
import AVKit

struct LiveChannelsView: View {
    let channels: [Channel]

    var body: some View {
        ScrollView {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 240), spacing: 16)],
                      spacing: 16) {
                ForEach(channels) { channel in
                    ChannelCard(channel: channel)
                }
            }
            .padding()
        }
        .navigationTitle("Live")
    }
}

struct ChannelCard: View {
    let channel: Channel

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            ZStack(alignment: .topLeading) {
                AsyncImage(url: channel.thumbnailURL) { img in img.resizable() }
                    placeholder: { Color.gray.opacity(0.3) }
                    .aspectRatio(16/9, contentMode: .fill)
                    .clipShape(.rect(cornerRadius: 12))
                    .overlay(alignment: .bottomLeading) {
                        if let progress = channel.currentProgress {
                            ProgressView(value: progress)
                                .tint(.red)
                                .padding(8)
                        }
                    }
                Text("LIVE")
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(.red, in: .capsule)
                    .padding(8)
            }
            Text(channel.currentProgram).font(.headline).lineLimit(1)
            Text(channel.name).font(.caption).foregroundStyle(.secondary)
        }
        .contextMenu {
            Button("Watch Now") { play(channel) }
            Button("Start Over") { playFromBeginning(channel) }
            Button("Record") { record(channel) }
            Divider()
            Button("Add to Favorites") { favorite(channel) }
        }
    }
}

struct PlayerWithFooter: View {
    let channel: Channel
    @State private var footerVisible = false

    var body: some View {
        ZStack(alignment: .bottom) {
            VideoPlayer(player: playerForChannel(channel))
                .ignoresSafeArea()
                .onTapGesture { /* reveal controls */ }

            if footerVisible {
                ChannelFooter(channel: channel)
                    .background(.black.opacity(0.6))
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .gesture(
            DragGesture()
                .onEnded { value in
                    if value.translation.height < -50 { footerVisible = true }
                    else if value.translation.height > 50 { footerVisible = false }
                }
        )
    }
}
```

## See also

- [`../platforms/tvos.md`](../platforms/tvos.md) — tvOS focus engine and remote.
- [`../inputs/focus-and-remote.md`](../inputs/focus-and-remote.md) — Siri Remote for live viewing.
- [`../technologies/widgets-and-live-activities.md`](../technologies/widgets-and-live-activities.md) — Live Activities for scores / events.
- [`./multitasking.md`](./multitasking.md) — Picture-in-Picture.
- [`./going-full-screen.md`](./going-full-screen.md) — full-screen video mode.
- [`../components/bars.md`](../components/bars.md#tab-bars) — tab bar placement of Live tab.
