# Game controllers

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/game-controls
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS
**Not supported:** watchOS

Every Apple platform except watchOS supports physical game controllers. Most players won't own one — so **also support the platform's default input** (touch on iOS/iPadOS, keyboard + mouse/trackpad on macOS, Siri Remote on tvOS, eyes + hands on visionOS). Controllers are a power-user amplifier, not a replacement for the native input model.

## Core rules

- **Support the platform's default interaction method.** Controller is additive.
- **Auto-detect paired controllers.** No manual setup screen.
- **Gracefully prompt for a controller** if your app/game requires one and none is connected. Users can open the app at any time; pause + prompt is the pattern.
- **Customize onscreen content to the connected controller.** The Game Controller framework gives standard names, but the buttons on the actual hardware may have different colors, symbols, or layouts. Use SF Symbols for the real controller's labeling scheme.
- **Support multiple connected controllers.** Multi-player games need to show the right labels and glyphs per player.
- **Prefer symbols over text** when referring to controller buttons. SF Symbols ships controller glyphs for most buttons across brands.

## Touch controls (iOS / iPadOS games)

If you support touch, follow these rules:

### When to add virtual controls

- **Many actions or movement control needed** → virtual controls.
- **Few actions, directly-manipulable objects** → prefer in-game gestures (tap objects to select, swipe to dodge). Reduces overlap with game content.

### Placement

- **Respect safe area and hardware** — Home indicator, Dynamic Island.
- **Movement on the left side of the screen**; camera on the right. Universal convention.
- **Primary action buttons near the thumb** (bottom corners). Avoid the large circular regions where movement and camera controls live — they're for joystick-like input.
- **Secondary controls** (menus, inventory) at the top of the screen.

### Sizing

- **Frequently-used controls ≥ 44×44pt.**
- **Secondary controls** (menu icons) **≥ 28×28pt**.
- Accommodate fingers; err toward larger.

### Press states and feedback

- **Always include visible and tactile press states.** A virtual button without feedback feels unresponsive.
- **Press state readable under the thumb** — use a glow or external ring, since the thumb covers the center of the button.
- **Combine with sound and haptics** for strong feedback.

### Icons and labels

- **Use symbols that communicate the action.** A weapon icon for attack, not "A" or "R1".
- **Abstract shapes or controller-based naming** (A, X, R1) make it harder for players to understand.

### Adapt to gameplay

- **Show/hide controls contextually.** Hide movement controls before the player first touches; reveal on interaction. Reduces UI clutter while giving the player space for their content.
- **Combine functionality into a single control** where possible. Gestures (double-tap, touch-and-hold) can add depth to a single button. Sprint via "touch-and-hold the move control" is better than two buttons.
- **Virtual thumbstick under the thumb.** Not at a fixed position — show the joystick wherever the player first touches the left side. Camera panning uses direct touch, not a virtual joystick.

## Physical controllers

### MFi and standard controllers

Apple platforms support a **wide range of MFi-certified controllers** plus PlayStation, Xbox, and Nintendo-brand wireless controllers (platform and model dependent). The **Game Controller framework** (`GCController`, `GCExtendedGamepad`) abstracts differences.

### Button mapping for UI navigation

**Outside of gameplay** — in menus, settings, splash screens — map controller buttons to expected UI behavior. This is a contract:

| Button | Expected UI behavior |
|---|---|
| A | Activates a control |
| B | Cancels / returns to previous screen |
| X | — (game-specific) |
| Y | — (game-specific) |
| Left shoulder | Navigate left to a different screen or section |
| Right shoulder | Navigate right to a different screen or section |
| Left trigger | — |
| Right trigger | — |
| Left / right thumbstick | Move selection |
| Directional pad | Move selection |
| Home / logo button | Reserved for system controls |
| Menu button | Open game settings or pause gameplay |

Follow this mapping in every menu / non-gameplay context. Players switch between games constantly; consistency matters.

### In-game requirements (tvOS / visionOS)

On tvOS and visionOS you can **require** a physical controller. If so:

- The App Store displays a "Game Controller Required" badge.
- **Check for a connected controller on launch.**
- **If none, prompt gracefully.** Don't crash, don't show a broken screen. Explain what's needed.

### Showing controller glyphs

When your game references specific buttons ("Press A to jump"), **use symbols, not text**:

- The Game Controller framework provides SF Symbols for most controller elements including brand-specific variations.
- **Match the connected controller's labeling scheme.** Don't show Xbox "A" when a PlayStation controller is connected — show the PlayStation cross.
- **In multiplayer, show each player's controller's glyphs.**

### Multiple controllers

- **Identify which controller the player is using.** Use correct labels and glyphs per player.
- **Refer to multiple controllers' buttons when listing** (e.g., pause screen explanation of controls).

### Vibration / haptics

- **Use haptic feedback via the Game Controller framework.** Different controller brands have different capabilities — the framework abstracts them.
- **Match haptic intensity to the event.** Heavy hit = stronger feedback.

## Keyboard for games

Keyboard binding is its own thing on top of the general keyboard rules in [`./pointer-and-keyboard.md`](./pointer-and-keyboard.md). For games:

- **Prioritize single-key commands.** Single keys are faster to press, especially while using a mouse or trackpad.
  - Space bar as the main action (large, easy to hit).
  - First-letter-of-menu-item shortcuts (I = Inventory, M = Map).
- **Test on an Apple keyboard.** The Control key on PC keyboards often maps better to ⌘ on Mac (next to the Space bar).
- **Proximity matters.** If WASD is your navigation, put related actions on nearby keys (Q/E/R/F).
- **Group related actions by physical key proximity** — number keys for inventory categories, for example.
- **Let players customize key bindings.** Defaults matter; personal comfort and play style matter more.

## visionOS spatial controllers

Vision Pro supports **spatial game controllers** like the PlayStation VR2 Sense controller.

- **Match spatial controller behavior to hand input.**
- **Indirect:** look at an object + press the left or right trigger to activate (mirrors indirect pinch).
- **Direct:** reach out, physically point, and press the trigger (mirrors direct touch).

Design so the same interaction flows work whether the user uses hands or a spatial controller.

## Platform considerations

| Platform | Controller support | Required on |
|---|---|---|
| iOS / iPadOS | ✓ | never |
| macOS | ✓ | never |
| tvOS | ✓ | can require |
| visionOS | ✓ (including spatial controllers) | can require |
| watchOS | — | n/a |

## Common mistakes

- **Controller required on iOS/iPadOS** — you lose every player without one.
- **No fallback for the platform's default input.**
- **Text button references ("Press A")** that don't match the connected controller's labeling.
- **Abstract virtual-button icons** (A / X / R1) on touch controls that teach nothing.
- **Movement and camera regions too small** — players can't reach the edges comfortably.
- **Virtual thumbstick at a fixed position** — moves the player's thumb, not the controls.
- **No pause-on-B-button** in a menu where B is the expected cancel.
- **Home / logo button intercepted** (reserved for the system).
- **Single-letter menu shortcuts using awkward keys.**
- **Hardcoded Xbox glyphs** shown when the user has a PlayStation controller connected.
- **Haptics burning out** from firing on every input.
- **No key-binding customization** in a game with keyboard + mouse controls.
- **"Controller Required" apps that don't check for one at launch**, or that fail silently.

## SwiftUI

```swift
import SwiftUI
import GameController

// Observe controller connect / disconnect.
@Observable final class ControllerModel {
    var controller: GCController?
    var controllerKind: GCController.Kind = .unknown

    init() {
        NotificationCenter.default.addObserver(
            forName: .GCControllerDidConnect, object: nil, queue: .main) { [weak self] note in
                guard let ctrl = note.object as? GCController else { return }
                self?.controller = ctrl
                self?.controllerKind = .fromProductCategory(ctrl.productCategory)
            }

        NotificationCenter.default.addObserver(
            forName: .GCControllerDidDisconnect, object: nil, queue: .main) { [weak self] _ in
                self?.controller = nil
            }

        GCController.startWirelessControllerDiscovery(completionHandler: nil)
    }
}

// UI that prompts for a controller if required (tvOS / visionOS).
struct RootView: View {
    @State private var controllers = ControllerModel()

    var body: some View {
        Group {
            if controllers.controller != nil {
                GameView()
            } else {
                ContentUnavailableView {
                    Label("Connect a Controller", systemImage: "gamecontroller")
                } description: {
                    Text("This game requires a compatible game controller. Connect one to start.")
                } actions: {
                    Button("Help with Pairing") { openSettings() }
                        .buttonStyle(.borderedProminent)
                }
            }
        }
    }
}

// Show the right button glyph for the connected controller.
func primaryButtonSymbol(_ ctrl: GCController) -> String {
    // The Game Controller framework maps standard elements to brand-specific SF Symbols.
    if let pad = ctrl.extendedGamepad {
        return pad.buttonA.sfSymbolsName ?? "button.programmable.square"
    }
    return "button.programmable"
}

// Map pause button to menu.
controller.extendedGamepad?.buttonMenu.pressedChangedHandler = { _, _, pressed in
    guard pressed else { return }
    Task { @MainActor in showPauseMenu() }
}
```

## UIKit (iOS / iPadOS / tvOS)

```swift
import UIKit
import GameController

class GameViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()

        // tvOS: require the controller up-front and prompt if missing.
        NotificationCenter.default.addObserver(self,
            selector: #selector(controllerConnected),
            name: .GCControllerDidConnect, object: nil)
        NotificationCenter.default.addObserver(self,
            selector: #selector(controllerDisconnected),
            name: .GCControllerDidDisconnect, object: nil)

        if GCController.controllers().isEmpty {
            GCController.startWirelessControllerDiscovery { [weak self] in
                self?.showPairingPrompt()
            }
        }
    }

    @objc private func controllerConnected() { bindInputs() }
    @objc private func controllerDisconnected() { pauseGame() }

    private func bindInputs() {
        guard let pad = GCController.current?.extendedGamepad else { return }

        pad.leftThumbstick.valueChangedHandler = { [weak self] _, x, y in
            self?.applyMovement(x: x, y: y)
        }

        pad.buttonA.pressedChangedHandler = { [weak self] _, _, pressed in
            if pressed { self?.performPrimary() }
        }

        pad.buttonMenu.pressedChangedHandler = { [weak self] _, _, pressed in
            if pressed { self?.togglePause() }
        }
    }
}
```

## AppKit (macOS)

```swift
import AppKit
import GameController

final class GameWindowController: NSWindowController {
    override func windowDidLoad() {
        super.windowDidLoad()

        let nc = NotificationCenter.default
        nc.addObserver(forName: .GCControllerDidConnect, object: nil, queue: .main) { [weak self] _ in
            self?.bindInputs()
        }

        GCController.startWirelessControllerDiscovery(completionHandler: nil)
    }

    private func bindInputs() {
        guard let pad = GCController.current?.extendedGamepad else { return }

        pad.buttonB.pressedChangedHandler = { [weak self] _, _, pressed in
            if pressed { self?.cancel() }
        }
    }
}
```

## See also

- [`./touch-and-gestures.md`](./touch-and-gestures.md) — touch as the default input on iOS/iPadOS games.
- [`./pointer-and-keyboard.md`](./pointer-and-keyboard.md) — keyboard + mouse for Mac gaming.
- [`./focus-and-remote.md`](./focus-and-remote.md) — Siri Remote + focus on tvOS.
- [`./spatial-input.md`](./spatial-input.md) — eyes + hands + spatial controllers on visionOS.
- [`../platforms/tvos.md`](../platforms/tvos.md) — when controllers are required.
- [`../platforms/visionos.md`](../platforms/visionos.md) — spatial controller parity with hand input.
- [`../foundations/accessibility.md`](../foundations/accessibility.md) — always support multiple input paths.
- [`../technologies/game-center.md`](../technologies/game-center.md) (pending) — multiplayer and leaderboards.
