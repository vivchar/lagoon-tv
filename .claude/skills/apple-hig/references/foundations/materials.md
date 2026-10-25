# Materials (Liquid Glass and standard materials)

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/materials
**Last verified:** 2026-04-20
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

Two material systems live side by side. **Liquid Glass** is the 2025 cross-platform design language for the control layer: tab bars, sidebars, toolbars, floating controls. **Standard materials** (ultra-thin / thin / regular / thick) live in the content layer and create local structure.

## Liquid Glass vs content layer

Liquid Glass forms a **distinct functional layer** that floats above content. It lets content scroll through and peek behind navigation chrome, giving the interface depth while keeping controls legible.

- **Use Liquid Glass for controls and navigation.** Tab bars, sidebars, toolbars, floating action buttons.
- **Don't use Liquid Glass in the content layer.** It causes visual noise and a confused hierarchy.
- **Exception:** transient interactive elements in the content layer (a value knob being dragged, a time slider being scrubbed) can take on a Liquid Glass appearance while active.

System components pick up Liquid Glass automatically. Only apply the effect to a custom control if it's essential to the control's identity — and then reserve it for the few most important controls in the app.

## Liquid Glass variants

Two variants. Default is regular.

| Variant | Behavior | Use for |
|---|---|---|
| **Regular** (default) | Blurs and adjusts the luminosity of the content behind to keep foreground text legible. | Alerts, sidebars, popovers, tab bars, most chrome. |
| **Clear** | Highly translucent; prioritizes visibility of rich content behind. | Controls over photos, videos, or other visually rich content (e.g., media playback bars). |

Clear variant legibility: if the content behind is bright, **add a 35% dark dimming layer** behind the control. If it's sufficiently dark (or you're using AVKit standard media controls which already dim), skip the dimming layer.

Appearance shifts automatically in response to system settings like Reduce Transparency, Increase Contrast, and the user's chosen Liquid Glass look.

## Applying color to Liquid Glass

Liquid Glass has no inherent color by default — it inherits from the content behind. You can tint it ("stained glass") for primary calls-to-action.

- **Color the background, not the glyph.** The system does this for prominent buttons (e.g., the Done button gets the accent tint behind the label; label stays monochrome).
- **Don't tint more than a few controls per screen.** Emphasis collapses when everything is emphasized.
- **Monochromatic labels on colorful backgrounds.** If the app has colorful content, keep toolbar and tab bar labels monochrome or use an accent with enough contrast.
- **Resting state must be legible.** The top of the scroll view (before any scroll) is what the user sees at rest; if your content color blends with the chrome tint there, it won't read.

## Standard materials (content layer)

Use standard materials to provide local structure within the content layer — sections, cards, overlays that feel heavier than a flat fill. They are **not** a replacement for Liquid Glass chrome; they complement it.

iOS and iPadOS provide four:

| Material | SwiftUI | Translucency |
|---|---|---|
| Ultra Thin | `.ultraThinMaterial` | Most transparent |
| Thin | `.thinMaterial` | |
| Regular (default) | `.regularMaterial` | |
| Thick | `.thickMaterial` | Most opaque |

Pick by **semantic meaning**, not by the color it happens to produce — system settings shift the look.

- **Thicker** → better legibility for fine text and small elements.
- **Thinner** → preserves context with more of the background visible.

Use **vibrant colors** (labels, fills, separators) on top of materials. They're designed to hold legible contrast regardless of what's behind.

### iOS / iPadOS vibrancy

- Labels: default / secondary / tertiary / quaternary. Avoid **quaternary** on ultra-thin or thin materials — contrast fails.
- Fills: default / secondary / tertiary.
- Separators: single vibrancy level.

## macOS

macOS provides multiple named materials (sidebars, title bar, HUD, selection, popover, etc.) via `NSVisualEffectView`, each with a designated purpose. Prefer the named material over picking a color.

- **Background blending modes:**
  - `.behindWindow` — shows what's behind the window.
  - `.withinWindow` — shows what's behind the view within the window.
- Vibrancy applies to foreground content. Test with a range of desktop pictures and accent colors — desktop tinting will shift appearance.

## tvOS

Liquid Glass covers navigation and system experiences (Top Shelf, Control Center). Focused image views and buttons adopt Liquid Glass when focused.

Standard materials in the content layer provide structure for overlays. Thicker materials = more obscuring of the background.

## visionOS

visionOS windows use a single unmodifiable system material called **glass**. It lets light from the room and the user's environment show through to ground people in their space.

- **No Dark Mode.** Glass auto-adapts to the luminance of what's behind it.
- **Prefer translucency to opaque color fills** in windows. Opaque areas block the environment and can feel constricting.

For custom components, use these system materials:

- **Elevated** — bring attention to interactive elements (buttons, selected items).
- **Grouped** — separate sections (sidebar rows, grouped table views).
- **Dark-on-grouped** — create a dark element on top of a grouped background.

Vibrancy in visionOS has three values:

- Standard — body text.
- Vibrant — footnotes, subtitles.
- Inactive — dim elements; only when legibility isn't critical.

## watchOS

Full-screen modal views are common. Material backgrounds provide orientation and distinguish modal chrome from app content. **Don't replace the default material background on modal sheets.**

## Common mistakes

- Applying a Liquid Glass effect to a flat content card.
- Shipping a custom toolbar with a solid opaque color instead of using the system toolbar (which is Liquid Glass for free).
- Tinting every toolbar button with the accent color.
- Clear-variant Liquid Glass on a bright photo without a dimming layer — labels wash out.
- Adding transparency to a colored macOS custom control (causes color to shift with desktop picture).
- Using ultra-thin material in a text-heavy popover — fine type loses contrast.
- Opaque visionOS window backgrounds that block room-ambient light.

## SwiftUI

```swift
// Liquid Glass toolbar is automatic — just use the system toolbar.
NavigationStack {
    List(items) { ItemRow(item: $0) }
        .navigationTitle("Inbox")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Compose", systemImage: "square.and.pencil") { }
            }
        }
}

// Standard material behind a card in the content layer.
VStack(spacing: 12) {
    Text("Weekly summary").font(.headline)
    Text("12 items synced.").foregroundStyle(.secondary)
}
.padding()
.frame(maxWidth: .infinity, alignment: .leading)
.background(.regularMaterial, in: .rect(cornerRadius: 16))

// Prominent button that gets Liquid Glass tinted by the system.
Button("Continue") { }
    .buttonStyle(.borderedProminent)
    .tint(.accentColor)
    .controlSize(.large)

// Clear material with a dark dimming layer over a photo.
ZStack {
    Image("photo").resizable().scaledToFill()
    VStack {
        Spacer()
        HStack {
            Button("Share", systemImage: "square.and.arrow.up") { }
            Spacer()
            Button("Favorite", systemImage: "heart") { }
        }
        .padding()
        .background(Color.black.opacity(0.35))
        .background(.ultraThinMaterial)
        .clipShape(.rect(cornerRadius: 20))
        .padding()
    }
}
```

## UIKit

```swift
// Standard material under a content card.
let blur = UIVisualEffectView(effect: UIBlurEffect(style: .systemMaterial))
blur.translatesAutoresizingMaskIntoConstraints = false
card.insertSubview(blur, at: 0)

// Vibrant label on top.
let vibrancy = UIVibrancyEffect(blurEffect: blur.effect as! UIBlurEffect, style: .label)
let vibrancyView = UIVisualEffectView(effect: vibrancy)
vibrancyView.contentView.addSubview(titleLabel)
blur.contentView.addSubview(vibrancyView)
```

## AppKit

```swift
// Sidebar material.
let effect = NSVisualEffectView()
effect.material = .sidebar
effect.blendingMode = .behindWindow
effect.state = .followsWindowActiveState
effect.translatesAutoresizingMaskIntoConstraints = false
sidebarContainer.addSubview(effect, positioned: .below, relativeTo: nil)
```

## See also

- [`color.md`](./color.md) — tinting Liquid Glass, vibrant label colors.
- [`dark-mode.md`](./dark-mode.md) — contrast rules through vibrancy.
- [`layout.md`](./layout.md) — control layer vs content layer.
- [`../platforms/visionos.md`](../platforms/visionos.md) — the glass material in depth.
