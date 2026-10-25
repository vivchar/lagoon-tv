# SF Symbols

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/sf-symbols
**Last verified:** 2026-04-20
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

Use SF Symbols. They align to SF weights and scales, tint automatically, scale with Dynamic Type, and ship with thousands of standard and localized variants. Invent a custom icon only when no system equivalent exists.

## Rules

- **Use system symbols first.** Download the [SF Symbols app](https://developer.apple.com/sf-symbols/) and search there before drawing anything.
- **Don't use SF Symbols in app icons, logos, or trademarked art.** The license forbids it.
- **Don't replicate Apple products.** Some symbols (Apple product glyphs) can't be customized — the SF Symbols app flags them.
- **Match symbol weight to adjacent text weight.** The 9 symbol weights map 1:1 to SF font weights.
- **Pick the variant the context expects.** Fill in iOS tab bars, outline in toolbars. Enclosed variants read better at small sizes.
- **Test every rendering mode.** What works at 28pt may not work at 14pt.
- **Provide an accessibility label on every functional symbol.**

## Rendering modes

Four modes. Each symbol's paths are grouped into primary / secondary / tertiary layers.

| Mode | What it does | When to use |
|---|---|---|
| **Monochrome** | Single color across all layers. | Default. Toolbars, lists, labels. |
| **Hierarchical** | One color, varying opacity per layer. | Add depth without multiple colors. |
| **Palette** | Two or three colors, one per layer. | Custom color schemes; branded charts. |
| **Multicolor** | Symbol-intrinsic colors (e.g. `leaf.fill` is green, `exclamationmark.triangle.fill` is yellow). | Communicate meaning through color. |

Use the automatic setting to get a symbol's preferred rendering mode. Override when another mode reads better in your context.

**Gradients** (SF Symbols 7+) add a smooth linear gradient from a single source color, across all rendering modes, system or custom symbols. Render best at larger sizes.

## Variable color

For values that change over time — signal strength, volume, progress — use variable color. Layers light up progressively as the value moves from 0 to 100%.

Use variable color to communicate **change**, not **depth**. Depth goes through hierarchical rendering.

```swift
// SwiftUI
Image(systemName: "speaker.wave.3.fill", variableValue: volume)    // volume in [0, 1]
Image(systemName: "cellularbars", variableValue: signal)
```

## Weights and scales

- **9 weights:** Ultralight, Thin, Light, Regular, Medium, Semibold, Bold, Heavy, Black — one per SF font weight.
- **3 scales:** small, medium (default), large. Scale is relative to the SF cap height at the same point size.

Scale lets you nudge a symbol's emphasis without changing the point size of adjacent text — useful for icon-leading list rows.

```swift
Image(systemName: "star.fill")
    .symbolRenderingMode(.hierarchical)
    .font(.system(.body, weight: .semibold))
    .imageScale(.large)
```

## Design variants

- **Outline** (default): no solid areas; pairs with text.
- **Fill**: solid shapes; more emphasis. Preferred in iOS tab bars, iOS swipe actions, and selected states.
- **Slash**: diagonal bar for "disabled" / "muted" / "unavailable."
- **Enclosed** (circle / square / rectangle): better legibility at small sizes; can combine with fill.
- **Script-specific** (Latin / Arabic / Hebrew / Hindi / Thai / Chinese / Japanese / Korean / Cyrillic / Devanagari / Indic numerals) — auto-swap on device language.

Many views already pick the variant: tab bars prefer fill, toolbars prefer outline. You usually don't specify.

## Animations

SF Symbols ship with configurable animations. Apply sparingly — motion is expensive attention.

| Animation | Use for |
|---|---|
| **Appear** | Symbol emerges into view. |
| **Disappear** | Symbol recedes out. |
| **Bounce** | One-shot elastic — feedback for a completed action. |
| **Scale** | Persistent scale up/down (for selection emphasis). |
| **Pulse** | Opacity pulse on pulse-annotated layers; ongoing activity. |
| **Variable color** | Cumulative or iterative layer lighting; progress or broadcasting. |
| **Replace** | Swap one symbol for another. Configs: down-up (default), up-up (progress), off-up (next-action emphasis). |
| **Magic Replace** | Smart transition between related symbols (e.g., `play.fill` → `pause.fill`). Default in SF Symbols 5+. |
| **Wiggle** | Directional shake — highlight an easily-missed change. |
| **Breathe** | Opacity + size over time — ongoing state (recording). |
| **Rotate** | Indicator of progress or physical motion (fan blades). |
| **Draw On / Draw Off** (SF Symbols 7+) | Draw the symbol along guide points; progress or directional emphasis. |

Animation ≠ decoration. Every animation communicates something specific. Mismatched animations confuse.

```swift
// SwiftUI — SF Symbols 5+ effects API
Image(systemName: didSave ? "checkmark.circle.fill" : "tray.and.arrow.down.fill")
    .symbolEffect(.bounce, value: didSave)           // fires on change
    .contentTransition(.symbolEffect(.replace))      // smooth shape swap

Image(systemName: "waveform")
    .symbolEffect(.variableColor.iterative, isActive: isListening)

Image(systemName: "star.fill")
    .symbolEffect(.pulse, options: .repeating, isActive: isSyncing)
```

## Custom symbols

When nothing fits, roll your own. Follow the workflow:

1. In the SF Symbols app, pick the closest system symbol, **Export Template**.
2. Edit the template in a vector-editing tool.
3. Re-import into the SF Symbols app, annotate layers and hierarchy.
4. Ship as a template `.svg` in your asset catalog.

Design guidelines:

- Match the system aesthetic: level of detail, optical weight, alignment, perspective.
- Be simple, recognizable, inclusive.
- **Annotate layers** for rendering modes. Assign a hierarchy level (primary/secondary/tertiary) or explicit color per layer.
- **Don't recreate enclosure variants** manually — the SF Symbols app's component library generates them.
- **Add negative side margins** for optically-aligning symbols with badges (naming pattern `left-margin-Regular-M`).
- **Annotate for animations.** Draw shapes whole where possible so animations don't tear. Use erase layers for cutouts.
- **Accessibility description** — every custom symbol needs one for VoiceOver.

## Platform notes

No platform-specific considerations. SF Symbols behave uniformly across iOS, iPadOS, macOS, tvOS, visionOS, and watchOS. The only variance: availability per OS version.

## Common mistakes

- Shipping a custom icon when a system symbol exists.
- Mixing a fill symbol with outline neighbors in the same toolbar.
- Using variable color to convey depth (use hierarchical instead).
- Animating every symbol.
- Using SF Symbols in your app icon or marketing art.
- Not setting an accessibility label on a functional symbol button.
- Forgetting that a symbol introduced in iOS 26 doesn't exist on iOS 17.

## SwiftUI

```swift
// Standard icon in a toolbar — system picks the variant.
.toolbar {
    ToolbarItem(placement: .primaryAction) {
        Button(action: newItem) { Image(systemName: "plus") }
            .accessibilityLabel("New item")
    }
}

// Badge + fill in a tab bar.
TabView {
    Tab("Inbox", systemImage: "tray") { InboxView() }
        .badge(unreadCount)
    Tab("Sent", systemImage: "paperplane") { SentView() }
}

// Hierarchical tint for a header symbol.
Image(systemName: "icloud.fill")
    .symbolRenderingMode(.hierarchical)
    .foregroundStyle(.tint)
    .font(.largeTitle)

// Multicolor for semantic meaning.
Label("Error uploading", systemImage: "exclamationmark.triangle.fill")
    .symbolRenderingMode(.multicolor)
```

## UIKit

```swift
let config = UIImage.SymbolConfiguration(pointSize: 17, weight: .semibold, scale: .large)
let symbol = UIImage(systemName: "tray.and.arrow.down.fill", withConfiguration: config)
button.setImage(symbol, for: .normal)
button.imageView?.preferredSymbolConfiguration = config

// Hierarchical rendering
let hier = UIImage.SymbolConfiguration(hierarchicalColor: .systemBlue)
iconView.preferredSymbolConfiguration = hier

// Animate a checkmark fill
iconView.addSymbolEffect(.bounce, animated: true)
```

## AppKit

```swift
let cfg = NSImage.SymbolConfiguration(pointSize: 15, weight: .semibold, scale: .medium)
    .applying(.preferringMulticolor())
toolbar.image = NSImage(systemSymbolName: "tray.and.arrow.down.fill",
                        accessibilityDescription: "Save draft")?
    .withSymbolConfiguration(cfg)
```

## See also

- [`typography.md`](./typography.md) — SF font weights map to symbol weights.
- [`color.md`](./color.md) — tint symbols with system colors; adapts in dark mode.
- [`motion.md`](./motion.md) — respect Reduce Motion with symbol effects.
- [SF Symbols app](https://developer.apple.com/sf-symbols/) — always the source of truth for availability.
