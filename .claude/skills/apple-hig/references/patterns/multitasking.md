# Multitasking

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/multitasking
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS
**Not supported:** watchOS

People multitask constantly — switching apps, arranging windows, dragging content between apps, playing a video while they work. Every app must assume it can be **suspended at any moment** and resumed with full context. Get this wrong and users lose work.

## Core rules

- **Save and restore context on every transition.** App-switch, screen rotation, window resize, Focus change, reboot. Use `SceneStorage` (SwiftUI) or `NSUserActivity` (AppKit/UIKit).
- **Pause tasks that need attention when the user switches away.** Media playback, games, video calls. The user who switched back expects to pick up where they left.
- **Finish user-initiated tasks in the background.** File uploads, video exports, downloads. If the user started it, honor the intent — don't cancel on app suspend.
- **Respond to audio interruptions correctly.**
  - **Primary audio (music, podcast, audiobook):** pause indefinitely on interruption.
  - **Transient audio (navigation voice prompt):** duck or briefly pause, resume at original volume when the interruption ends.
  - Handle via `AVAudioSession.interruptionNotification`.
- **Notifications sparingly.** Notify when a genuinely interesting background task completes (upload done, export finished). Don't notify for every routine background operation.

## iPhone (iOS)

- **App Switcher** swipes up from the bottom edge.
- **Picture-in-Picture** for video and FaceTime. Any video playback should participate unless your app's primary experience is the video itself.
- **No side-by-side apps.**

## iPad (iPadOS)

The multitasking powerhouse. Design for:

- **Full-screen** apps.
- **Split View** — two apps side by side.
- **Slide Over** — a compact window floating on top.
- **Stage Manager** — multiple app windows per stage, user arranges freely.
- **Multiple windows of your own app** — Mail, Safari, Notes all do it. Users expect to open a message in a second window while referring back to the inbox.
- **Picture-in-Picture** — video + FaceTime overlay on top of whatever the user is doing.

### Rules

- **Resizable layouts.** Your app must render correctly at every width from compact to regular. See [`../platforms/ipados.md`](../platforms/ipados.md).
- **Defer switching to compact layout.** Keep the full layout until the width really can't support it.
- **Hide tertiary columns first** when the window narrows.
- **Multi-window via `UIScene`** — make sure your app has proper scene delegate handling.
- **Your app doesn't control or receive the multitasking config.** You can't ask "am I in Split View?" — respond to the layout you're given.

## Mac (macOS)

Multitasking is the default — many windows, many apps, constant switching.

- **Full-screen + Split Screen Spaces** — users put two apps in one Space. Don't fight it.
- **Drop shadows layered** on non-frontmost windows. The system handles the appearance.
- **Active vs inactive window states.** A macOS app has many appearance variants depending on window state — key, main, inactive, invisible. Respect system chrome transitions; don't force-hold the active appearance.
- **Window restoration** — on relaunch, restore the windows the user had open and their sizes.

## tvOS

- **Picture-in-Picture** for supported video apps. Users watch content while browsing elsewhere.
- **No side-by-side apps on tvOS** — one app is active at a time.

## visionOS

- **Shared Space** runs multiple apps side-by-side (windows + volumes).
- **Only one window is active at a time.** When the user looks from one window to another, the previously active window becomes translucent and recedes in z. The system applies a **feathered mask** on the inactive window.
- **Don't override the feathered mask.** Don't change window edge appearance.
- **Don't pause video when the user looks away.** In visionOS and macOS, users expect playback to continue while they use another window.
- **Audio ducking** — non-Now-Playing apps may have their audio ducked when the user shifts attention. Handle gracefully.
- **Closing an app window in Shared Space backgrounds the app but doesn't quit it.** The app can continue running (playing audio, tracking activity) with no window visible.
- **Now Playing exception** — closing the Now Playing window auto-pauses audio. Resume via Control Center.
- **Full Space** replaces all other windows. Your app owns the environment; no other apps are visible.

## Common mistakes

- Discarding state on app switch — user comes back to a reset.
- Not supporting multiple windows on iPad / Mac / visionOS when the use case clearly benefits.
- Pausing media when the user looks away in visionOS / macOS.
- Ignoring audio interruptions (music keeps playing through a phone call).
- Cancelling in-progress uploads when the app suspends.
- Sending a notification every time a routine task completes.
- Assuming a fixed screen size on iPad.
- Overriding visionOS feathered mask on inactive windows.
- Fighting macOS window-state appearance transitions.
- Not restoring window positions on Mac relaunch.

## SwiftUI

```swift
// Scene storage persists per-scene state across backgrounding.
struct EditorScene: Scene {
    @SceneStorage("editorText") private var text: String = ""
    @SceneStorage("editorCursor") private var cursor: Int = 0

    var body: some Scene {
        WindowGroup { EditorView(text: $text, cursor: $cursor) }
    }
}

// Multiple windows of the same app.
@main
struct NotesApp: App {
    var body: some Scene {
        WindowGroup("Notes") { RootView() }

        // Secondary window for an opened note.
        WindowGroup("Note", for: Note.ID.self) { $id in
            if let id { NoteDetailView(id: id) }
        }
    }
}

// Open a new window.
@Environment(\.openWindow) var openWindow
Button("Open in New Window") { openWindow(id: "Note", value: note.id) }

// Audio interruption handling.
import AVFAudio

NotificationCenter.default.addObserver(
    forName: AVAudioSession.interruptionNotification,
    object: AVAudioSession.sharedInstance(),
    queue: .main
) { note in
    guard let info = note.userInfo,
          let typeValue = info[AVAudioSessionInterruptionTypeKey] as? UInt,
          let type = AVAudioSession.InterruptionType(rawValue: typeValue) else { return }

    switch type {
    case .began:
        audioPlayer.pause()
    case .ended:
        if let options = info[AVAudioSessionInterruptionOptionKey] as? UInt,
           AVAudioSession.InterruptionOptions(rawValue: options).contains(.shouldResume) {
            audioPlayer.play()
        }
    @unknown default: break
    }
}
```

## See also

- [`../platforms/ipados.md`](../platforms/ipados.md) — Stage Manager, Split View, Slide Over.
- [`../platforms/macos.md`](../platforms/macos.md) — window state, Spaces.
- [`../platforms/visionos.md`](../platforms/visionos.md) — Shared Space vs Full Space.
- [`../components/split-views.md`](../components/split-views.md) — fluid-width layouts.
- [`./going-full-screen.md`](./going-full-screen.md) — when to offer full-screen mode.
