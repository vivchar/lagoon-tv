# Going full screen

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/going-full-screen
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS
**Not supported:** tvOS (apps already fill the screen) · visionOS (immersive spaces instead) · watchOS (single-screen device)

Full-screen mode **expands a window to fill the screen, hides system chrome, and reduces distractions**. Use it for immersion and focused tasks — gameplay, media viewing, in-depth editing. Don't force it; let the user choose.

## When to offer full-screen

- **Games.** Gameplay benefits from covering the whole screen.
- **Media.** Video, slideshows, photo viewers.
- **Focused work.** Document editing, coding, long-form writing, photo/video editing.

## When NOT

- **Routine productivity UI.** Inbox, calendar, settings — users want to see other windows and the menu bar.
- **Any task where the Dock, menu bar, or other apps must stay reachable** by default.

## Rules

- **Use the system full-screen mechanism.** On macOS, the green window button enters full-screen; on iOS/iPadOS, the system handles immersive modes via `.statusBarHidden(...)` and the Home indicator.
- **Let the user choose entry and exit.** Don't force-enter on launch. Don't auto-exit when they switch apps.
- **Adjust layout, don't resize.** When your content expands to fill the screen, rebalance but keep the structure recognizable. Subtle adjustments — not a whole new layout.
- **Keep essential features reachable.** Full-screen media must expose playback controls (reveal on tap / pointer move). Full-screen editing must let users save, undo, exit.
- **Pause on switch.** Game or slideshow paused when the user leaves. Don't make them miss content they expect to see later.
- **Let people resume** where they left off after switching back.

## macOS specifics

- **Use the system full-screen API.** NSWindow's `toggleFullScreen(_:)` or SwiftUI's `.windowStyle(...)` variants. Gets you the right chrome, the right Spaces integration, the camera-housing accommodation on MacBooks.
- **Don't change display mode on full-screen entry in games.** Keep the user's display settings.
- **Let people enter full-screen via:**
  - Green window button.
  - View menu → Enter Full Screen (⌃⌘F).
  - Games may add a custom toggle in an in-game settings menu.
- **Custom full-screen window modes are wrong.** Don't ship a "Full Screen" menu with "Borderless Windowed / Exclusive Fullscreen / Windowed." Use the system mechanism.
- **Mac camera housing** — the notched top-center on MacBooks. System full-screen accommodates automatically.

## iOS / iPadOS specifics

- **Hide the status bar only when it earns hiding.** Games, media — yes. Content apps — no.
- **Hide the Home indicator** via `prefersHomeIndicatorAutoHidden` / SwiftUI `.persistentSystemOverlays(.hidden)`. Reveals after a short idle.
- **Defer system gestures** to prevent accidental exits. Home indicator edge swipe exits to Home. You can require **two swipes** instead of one for gameplay where accidental single swipes happen. Use `preferredScreenEdgesDeferringSystemGestures`.
- **Always allow explicit exit.** Even when gestures are deferred, a tappable exit must exist.

## Full-screen content apps (non-immersive)

Sometimes a content app (photo viewer, reading app) has a **full-screen mode for the current item** while the app itself isn't in macOS-full-screen. Examples:

- **Safari Reader mode.**
- **Photos fullscreen photo view.**
- **Apple Books reading mode.**

Rules:

- **Hide chrome on idle.** After a few seconds of inactivity, fade out toolbar/nav.
- **Reveal on interaction.** Tap, swipe down, pointer to top of screen.
- **Don't hide essential controls** if they're needed for navigation (page turn, back, close).

## Games specifically

- **Do not swap display modes** on full-screen entry.
- **Offer a custom in-game Settings menu** for graphics and resolution that don't require app restart. System-integrated.
- **Deferred system gestures** are legitimate for action games. Two-swipe exit is fine.

## Accessibility

- **Full-screen exit must be reachable via keyboard, VoiceOver, and Switch Control.** Don't hide it behind a gesture-only path.
- **Reduce Motion** — no parallax transitions on entry/exit.

## Common mistakes

- Auto-entering full-screen on launch.
- Auto-exiting when the app loses focus.
- Custom full-screen implementation on macOS (breaks Spaces, camera housing, window management).
- Full-screen that can't be exited (gesture-only, gesture disabled, no escape key).
- Full-screen media with no reveal for playback controls.
- Hiding status bar in an inbox / productivity app.
- Content app that hides chrome with no way to bring it back.
- Games that change display mode automatically.
- Slideshow that plays on while the user is in another app.

## SwiftUI

```swift
// iOS / iPadOS — hide status bar and Home indicator for a media view.
struct PlayerView: View {
    let url: URL

    var body: some View {
        VideoPlayer(player: AVPlayer(url: url))
            .ignoresSafeArea()
            .statusBarHidden(true)
            .persistentSystemOverlays(.hidden)       // Home indicator
            .defersSystemGestures(on: .bottom)       // prevent accidental exits
    }
}

// macOS — the window enters full-screen via the green button.
// Provide a menu command that toggles programmatically.
.commands {
    CommandMenu("View") {
        Button("Enter Full Screen") {
            NSApp.mainWindow?.toggleFullScreen(nil)
        }
        .keyboardShortcut("f", modifiers: [.control, .command])
    }
}

// Content app — reveal chrome on interaction.
struct ImmersiveReader: View {
    @State private var chromeVisible = true
    @State private var idleTimer: Task<Void, Never>?

    var body: some View {
        ZStack {
            ArticleView()
                .onTapGesture { toggleChrome() }
        }
        .overlay(alignment: .top) {
            if chromeVisible {
                Toolbar()
                    .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .animation(.easeInOut(duration: 0.2), value: chromeVisible)
        .onAppear { scheduleHide() }
    }

    private func toggleChrome() {
        chromeVisible.toggle()
        if chromeVisible { scheduleHide() } else { idleTimer?.cancel() }
    }

    private func scheduleHide() {
        idleTimer?.cancel()
        idleTimer = Task {
            try? await Task.sleep(for: .seconds(3))
            if !Task.isCancelled { chromeVisible = false }
        }
    }
}
```

## See also

- [`./multitasking.md`](./multitasking.md) — full-screen coexists with multitasking on iPad/Mac.
- [`../platforms/ios.md`](../platforms/ios.md) — iOS full-screen gestures.
- [`../platforms/macos.md`](../platforms/macos.md) — window chrome and full-screen Spaces.
- [`../platforms/visionos.md`](../platforms/visionos.md) — immersive spaces (visionOS equivalent).
- [`./live-viewing-apps.md`](./live-viewing-apps.md) — full-screen live video.
