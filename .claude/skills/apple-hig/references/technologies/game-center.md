# Game Center

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/game-center
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS

Game Center is Apple's social gaming network. It ships a **full-featured UI** (the "access point" → Game Overlay or dashboard) that players already know, it **surfaces your game** across the Apple Games app + App Store + system recommendations, and it handles **achievements**, **leaderboards**, **challenges**, and **multiplayer**. Shipping Game Center is usually cheap and always valuable.

## What Game Center gives you

- **Player identity** — Game Center account, friends graph, social recommendations.
- **Achievements** — locked / in-progress / hidden / completed states; collectible cards.
- **Leaderboards** — classic (all-time) or recurring (periodic reset); sets that group related boards.
- **Challenges** — turn single-player activities into multiplayer competitions.
- **Multiplayer** — party codes, matchmaking, turn-based games.
- **Discovery** — your game appears in the Games app, Top Played chart, friend recommendations.

## The access point

A small floating UI element that leads to the Game Overlay (iOS / iPadOS / macOS) or in-game dashboard (visionOS / tvOS).

### Rules

- **Show it on menu screens** — main menu, settings area. Not during active gameplay.
- **Any of the four screen corners.** Pick a spot where the expanded version doesn't cover important UI.
- **Check collision.** The access point has a collapsed and an expanded version — verify your layout works with the expanded one.
- **Pause the game when the Overlay or dashboard is present.** Players interact with Game Center data without feeling the game is continuing without them.

### visionOS positions

Access point position varies by game type (immersive vs volume-based). The GameKit docs specify the options; don't fight them.

## Achievements

A visual + motivational system. Apple presents achievements as collectible cards.

### Rules

- **Map to the four Game Center states.** Locked, in-progress, hidden, completed. The system groups completed vs locked automatically.
- **Display order = upload order.** Think about your preferred order before uploading.
- **Progressive achievements** — partial progress is visible, encouraging messages fire automatically ("You're more than halfway to Great Lakes Freighter. Keep going!").
- **Title ≤ 2 lines. Description ≤ 2 lines.** Truncation beyond.
- **Title-case for title. Sentence case for description.**
- **Unique, high-quality circular images per achievement.** The system applies a circular mask — keep content centered.

### Image specs

- **Circular masked** — keep the focal point within a circle.
- **Unique images** for every achievement. Placeholder image shows if you skip.
- Follow Apple's achievement image size specs (published in the Game Center docs).

## Leaderboards

### Types

- **Classic** — best all-time score. Always active.
- **Recurring** — resets on a time interval (daily, weekly). Drives engagement.

### Rules

- **Choose the right type.** Recurring + daily puzzles = great. Classic + speedrun records = great.
- **Group related boards in leaderboard sets** — difficulty modes, activity types, genres. Players find them easier.
- **Per-leaderboard artwork** — unique images that showcase the gameplay.
- **tvOS** — animated focusable image sets. Primary content must stay visible after focus crop.
- **iOS/iPadOS/macOS** — single image per board.
- **Images may be cropped** when the board is part of a set — keep primary content centered.

## Challenges

Turn single-player into multiplayer with friends.

### Rules

- **1–5 minute gameplay.** Short, skill-based, completable individually.
- **Recent score per attempt, not all-time best.** Keeps the field level; prevents regulars from dominating.
- **Deep-link to the exact level/mode** where the challenge begins. First-time players still need onboarding, but jump to the challenge after.
- **Artwork that encourages engagement.** Shown in Game Overlay, Games app, and invitation previews.
- **Leave space in the image** where the system overlays title and description — 1920×1080pt with cropped area 1465×767pt.

## Multiplayer

Real-time and turn-based.

### Party codes

Alphanumeric, typically 8 chars ("2MP4-9CMF").

- **Players can enter party codes manually.**
- **Join late, leave early, return later.**
- **View the current party code inside the game.**
- **Use party codes for real-time sessions**, whether you use GameKit networking or your own.

### In-game multiplayer UI

- **Default UI via GameKit** for inviting friends, nearby players, contacts.
- **Custom UI** is also fine — deep-link into specific leaderboards, profile, or multiplayer flows.

## Custom UI — deep links into Game Overlay / dashboard

Your in-game custom UI can link into Game Center.

- **Use Apple's official artwork.** Don't adjust dimensions or visual effects.
- **Correct terminology:**

| Correct | Don't use |
|---|---|
| Game Center | GameKit, GameCenter, game center |
| Game Center Profile | Profile, Account, Player Info |
| Achievements | Awards, Trophies, Medals |
| Leaderboards | Rankings, Scores, Leaders |
| Challenges | Competitions |
| Add Friends | Add, Include Friends |

- **Localize** "Game Center" and "Game Center Profile" — the system provides translations.

## Setup

- **Authenticate the player on launch.** If not signed in, initialize Game Center at launch time. Maximizes discovery (Top Played, social recommendations).
- **Don't block the game on auth.** Offer a silent background auth; if it fails, let the player play and remind later.

## Common mistakes

- **Terminology drift** — "Trophies" instead of "Achievements".
- **Blocking authentication screen** on launch.
- **No access point at all** — players can't reach their Game Center profile.
- **Access point during gameplay** — overlapping HUD.
- **Un-paused game while Overlay is open** — feels like the player is losing progress.
- **Achievements without images** — placeholder looks cheap.
- **Leaderboard images without centered primary content** — cropping hides the focal element.
- **Challenges tracking all-time best** — regulars dominate; no one joins.
- **Multi-step onboarding on a challenge invite link** — players bounce.
- **Missing artwork for Games app cards.**
- **Custom artwork that modifies Apple's Game Center logos.**
- **No party-code entry UI** — players who share codes verbally can't join.

## SwiftUI / GameKit

```swift
import GameKit
import SwiftUI

// Authenticate as early as possible.
struct GameCenterAuthenticator: ViewModifier {
    @State private var error: Error?

    func body(content: Content) -> some View {
        content.task {
            GKLocalPlayer.local.authenticateHandler = { viewController, error in
                if let viewController {
                    // Present if needed (usually you don't — the system handles it)
                } else if let error {
                    self.error = error
                }
            }
        }
    }
}

extension View { func authenticatingGameCenter() -> some View { modifier(GameCenterAuthenticator()) } }

// Game Center access point.
struct GameCenterAccessPoint: UIViewRepresentable {
    func makeUIView(context: Context) -> UIView {
        GKAccessPoint.shared.location = .topTrailing
        GKAccessPoint.shared.showHighlights = true
        GKAccessPoint.shared.isActive = true
        return UIView(frame: .zero)  // Access point is presented by the system; view is just a hook.
    }
    func updateUIView(_ view: UIView, context: Context) {}
}

// Reporting achievement progress.
func reportAchievement(id: String, percentComplete: Double) async throws {
    let achievement = GKAchievement(identifier: id)
    achievement.percentComplete = percentComplete
    achievement.showsCompletionBanner = true
    try await GKAchievement.report([achievement])
}

// Submitting a leaderboard score.
func submitScore(_ score: Int, to leaderboardID: String) async throws {
    try await GKLeaderboard.submitScore(score,
                                        context: 0,
                                        player: GKLocalPlayer.local,
                                        leaderboardIDs: [leaderboardID])
}

// Presenting the Overlay / dashboard.
struct GameCenterPresenter: UIViewControllerRepresentable {
    let state: GKGameCenterViewController.GameCenterViewControllerState

    func makeUIViewController(context: Context) -> GKGameCenterViewController {
        let vc = GKGameCenterViewController(state: state)
        vc.gameCenterDelegate = context.coordinator
        return vc
    }
    func updateUIViewController(_ vc: GKGameCenterViewController, context: Context) {}
    func makeCoordinator() -> Coordinator { Coordinator() }

    final class Coordinator: NSObject, GKGameCenterControllerDelegate {
        func gameCenterViewControllerDidFinish(_ vc: GKGameCenterViewController) {
            vc.dismiss(animated: true)
        }
    }
}
```

## See also

- [`../inputs/game-controllers.md`](../inputs/game-controllers.md) — controller support.
- [`../platforms/tvos.md`](../platforms/tvos.md) — Game Center dashboard on Apple TV.
- [`../platforms/visionos.md`](../platforms/visionos.md) — access point positions for immersive / volume games.
- [`../foundations/sf-symbols.md`](../foundations/sf-symbols.md) — controller glyphs match brand.
- [`./shortcuts-and-siri.md`](./shortcuts-and-siri.md) — App Intents to launch into a leaderboard or challenge.
