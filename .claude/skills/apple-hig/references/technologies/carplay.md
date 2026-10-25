# CarPlay

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/carplay
**Last verified:** 2026-04-21
**Applies to:** iOS (CarPlay apps run on a connected vehicle's built-in display)
**Not supported:** iPadOS · macOS · tvOS · visionOS · watchOS

CarPlay shows **selected iPhone apps on the car's built-in display** using **Apple-supplied templates**. You don't hand-draw the UI — you supply content (audio lists, navigation routes, messages, fuel prices) and the system renders it with driving-safe constraints. The experience is for **drivers while driving**. Design for speed, clarity, and minimal attention.

## What you can ship

CarPlay apps fall into specific **categories** with system-defined templates:

- **Audio** — streaming music, podcasts, audiobooks.
- **Communication** — messaging, VoIP.
- **Navigation** — turn-by-turn directions.
- **EV Charging** — find and interact with chargers.
- **Fueling** — find and pay for gas.
- **Parking** — find and pay for parking.
- **Quick Food Ordering** — coffee, fast orders.
- **Driving Task** — narrow set of vehicle-adjacent tasks (e.g., car wash, roadside).

Apple requires an entitlement for each category. Not every app qualifies — only apps in the supported categories.

## Core rules

### Designed for driving

- **Fast, glanceable tasks.** Every action should be completable in a second or two.
- **Never require the iPhone** while CarPlay is active. If setup is needed, require it **before** the car is moving.
- **Never lock people out of CarPlay** because the iPhone needs input. If the iPhone is in a bag, the trunk, or locked, the app still works.
- **Works on a locked iPhone.** Most people drive with their phone locked.
- **Report errors in CarPlay, not the iPhone.** Never direct the driver to "check your phone."

### Layouts

CarPlay displays range from small 5:3 panels to ultra-wide 8:3 displays.

| Pixels | Aspect ratio |
|---|---|
| 800×480 | 5:3 |
| 960×540 | 16:9 |
| 1280×720 | 16:9 |
| 1920×720 | 8:3 |

The system auto-scales icons and UI. Provide:

- **High-resolution images** at @2x and @3x.
- **App icon** at 120×120 (@2x) and 180×180 (@3x). Don't use a black background — add a border or lighten.
- **Use your iPhone app icon** (mirror it in CarPlay).

### Primary content at top

- **Upper half of the screen** holds the most important content and controls — that's where the driver's eye lands.
- **Large items read as more important.** Design hierarchy through scale.
- **Clean layout.** Don't clutter. No unnecessary detail.

### Color

- **Limited palette** that coordinates with your app logo.
- **Subtle color** communicates brand without overwhelming.
- **Different color for interactive vs non-interactive elements.** If the same color means "tappable" and "label", drivers can't tell where to tap.
- **Test under real lighting conditions.** Sunlight washes out; night driving darkens. What looks great on your desk may be illegible in a car.
- **Supports both light and dark appearance.** The system may switch automatically based on ambient light.

## Audio

If your app plays audio, it coexists with the car's radio and navigation voice prompts.

- **Never auto-start playback** unless your app is a single-source player, or resuming interrupted audio.
- **Don't start an audio session until you're ready to play.** Starting silences the car's radio.
- **Show the Now Playing screen when audio is ready to play.** Don't wait for metadata.
- **Resume only temporary interruptions** (phone call ends → resume). Permanent interruptions (Siri starts a new playlist) don't resume.
- **Automatic relative volume adjustments** are OK for mixing; don't change the overall output volume.

## Driving-hostile patterns to avoid

- **Text input.** Use Siri for any freeform input. Short lists work; keyboards don't.
- **Deep navigation hierarchies.** Keep every task 1–2 levels deep.
- **Small tap targets.** Big, confident targets. Exact sizes depend on the car display, but the system template handles this.
- **Long-running interstitial screens.** Users can't wait.
- **Sign-in flows.** Sign-in happens on the iPhone before driving, or use Apple's standard auth interfaces.

## The Now Playing screen

For audio apps, the system provides a standard Now Playing screen. Populate:

- **Album art.**
- **Track title, artist, album.**
- **Play/pause, next/previous.**
- **Progress bar.**
- **Like/dislike (optional).**
- **Shuffle / repeat (optional).**

Use `MPNowPlayingInfoCenter` and standard playback controls — the system renders them in CarPlay automatically. Don't custom-draw.

## Navigation apps

- **Use the provided `CPMapTemplate`.** It handles the system-tinted map region, route overlays, and turn card.
- **Turn-by-turn card** at the top. Distance to next turn, maneuver icon, street name.
- **Voice guidance** — coexist with music; the system ducks audio for nav prompts.
- **Destinations and favorites** via list templates.
- **Alternate routes** offered as cards.

## Communication apps

- **Message list** via list template.
- **Compose via Siri** — never a keyboard.
- **Read incoming messages aloud** with a confirm-then-send reply flow.

## Messages while driving

- **Focus aware.** Respect Do Not Disturb While Driving.
- **Preview messages in the list but don't auto-open.**
- **Siri-first reply flow.**

## Quick ordering apps

Coffee / fast-food pickups:

- **Re-order recent items** at the top.
- **Minimal customization** — if the driver needs 5 steps to order, you've over-designed.
- **Apple Pay at checkout.** No typing payment info.

## EV charging / fueling / parking

- **Find nearby locations.** Map view + list.
- **One-tap initiate charge / pump / park** when possible.
- **Apple Pay for payment.**
- **Provide status at a glance** — current session, amount, time remaining.

## Common mistakes

- Requiring iPhone input after launch.
- Text fields expecting typing.
- Multi-screen onboarding or sign-in in CarPlay.
- Auto-playing audio the moment the app launches.
- Starting an audio session before audio is ready — silences the car radio for no reason.
- Custom Now Playing UI instead of `MPNowPlayingInfoCenter`.
- Tiny icons that blur when scaled to 16:9 ultra-wide.
- Same color for interactive + non-interactive elements.
- Colors untested in sunlight.
- Errors that say "Check your iPhone."
- Changing the overall output volume programmatically.
- Deep navigation trees (3+ levels).
- Long interstitial / splash screens.
- Blocking the UI while network requests resolve.

## SwiftUI / CarPlay

CarPlay apps are built with CarPlay-specific scene types and `CP*` template classes (UIKit-style, used even in SwiftUI-era apps). A minimal audio template scene:

```swift
import CarPlay

class CarPlaySceneDelegate: UIResponder, CPTemplateApplicationSceneDelegate {
    var interfaceController: CPInterfaceController?

    func templateApplicationScene(_ scene: CPTemplateApplicationScene,
                                  didConnect controller: CPInterfaceController) {
        self.interfaceController = controller

        let nowPlaying = CPNowPlayingTemplate.shared
        nowPlaying.isUpNextButtonEnabled = true
        nowPlaying.upNextTitle = "Up Next"

        let tabs = CPTabBarTemplate(templates: [
            makeList(title: "Recents", icon: UIImage(systemName: "clock")!, items: recents),
            makeList(title: "Favorites", icon: UIImage(systemName: "star.fill")!, items: favorites),
            nowPlaying
        ])
        controller.setRootTemplate(tabs, animated: true)
    }

    private func makeList(title: String, icon: UIImage, items: [Track]) -> CPListTemplate {
        let listItems = items.map { track -> CPListItem in
            let item = CPListItem(text: track.title, detailText: track.artist)
            item.handler = { _, completion in
                PlayerEngine.shared.play(track)
                completion()
            }
            return item
        }
        let section = CPListSection(items: listItems)
        let list = CPListTemplate(title: title, sections: [section])
        list.tabTitle = title
        list.tabImage = icon
        return list
    }
}
```

Now Playing info (works across iOS and CarPlay):

```swift
import MediaPlayer

let center = MPNowPlayingInfoCenter.default()
center.nowPlayingInfo = [
    MPMediaItemPropertyTitle: track.title,
    MPMediaItemPropertyArtist: track.artist,
    MPMediaItemPropertyAlbumTitle: track.album,
    MPMediaItemPropertyPlaybackDuration: track.duration,
    MPMediaItemPropertyArtwork: MPMediaItemArtwork(boundsSize: size) { _ in track.artwork }
]

let commands = MPRemoteCommandCenter.shared()
commands.playCommand.addTarget { _ in PlayerEngine.shared.play(); return .success }
commands.pauseCommand.addTarget { _ in PlayerEngine.shared.pause(); return .success }
commands.nextTrackCommand.addTarget { _ in PlayerEngine.shared.next(); return .success }
commands.previousTrackCommand.addTarget { _ in PlayerEngine.shared.previous(); return .success }
```

## See also

- [`./widgets-and-live-activities.md`](./widgets-and-live-activities.md) — Live Activities appear on the CarPlay Dashboard.
- [`./shortcuts-and-siri.md`](./shortcuts-and-siri.md) — Siri is the input of choice in the car.
- [`../foundations/color.md`](../foundations/color.md) — adapt colors for sunlight and night.
- [`../foundations/accessibility.md`](../foundations/accessibility.md) — high-contrast testing.
- [`../platforms/ios.md`](../platforms/ios.md) — CarPlay apps are iOS apps at heart.
