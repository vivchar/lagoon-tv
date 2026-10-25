# Dark Mode

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/dark-mode
**Last verified:** 2026-04-20
**Applies to:** iOS · iPadOS · macOS · tvOS
**Not supported:** visionOS (glass adapts to environment luminance), watchOS (always dark)

Dark Mode is a systemwide appearance. People set it once and expect every app to respect it. Your app ships with both light and dark variants of every color, image, and icon that matters — not one or the other.

## Rules

- **Don't offer an app-specific appearance toggle.** It creates two settings people must manage and breaks their expectation that your app follows the system. Exceptions: immersive media apps that use a permanently dark appearance so the UI recedes.
- **Design for Auto.** People switch mid-use. Every screen handles both light and dark simultaneously.
- **Use semantic colors.** `Color(.label)`, `Color(.systemBackground)`, `UIColor.label`, `NSColor.labelColor` — these adapt automatically.
- **Asset catalog for custom colors.** Add both Any / Light and Dark variants, and separate Increased Contrast variants for each.
- **Asset catalog for images.** If a single asset works in both, ship it once; if not, ship light and dark variants.
- **SF Symbols adapt automatically.** Use them with dynamic tint.

## Contrast in dark appearance

The system uses vibrancy and increased contrast to keep text legible on dark backgrounds. Your custom content must also meet these floors:

- **Minimum:** 4.5:1 foreground-to-background contrast ratio.
- **Preferred for small text or custom colors:** 7:1.
- Test with **Increase Contrast on** and **Reduce Transparency on**, both separately and together. These combinations expose cases where dark text sits on a dark background after contrast adjustments collapse a vibrancy layer.

## Light and elevated backgrounds (iOS, iPadOS)

iOS and iPadOS use **two sets of backgrounds** in Dark Mode:

- **Base colors** — dimmer. Root screens sit here.
- **Elevated colors** — brighter. Popovers, modal sheets, and foreground windows in a multitasking context sit here.

The system switches automatically. Do not override with a custom dark background. If you do, the visual layering cue people use to parse depth collapses.

## Desktop tinting (macOS)

When the user picks the graphite accent, macOS pulls window background color from the current desktop picture. Windows visually harmonize with the wallpaper.

- Add transparency to custom components with a visible background **only when they're in a neutral state.**
- Do not add transparency when a component uses a color for meaning (e.g., active/selected). The component's color would shift as the desktop picture or window position changes.

## Text

- Use system-provided label colors: primary, secondary, tertiary, quaternary. They adapt to appearance automatically.
- Use system-provided text fields and text views. They handle vibrancy correctly.
- Do not manually mix a color onto vibrant text.

## Images and icons

- **SF Symbols first.** They tint automatically with dynamic colors and work with vibrancy.
- **Design separate interface icons for light and dark when the same image doesn't read.** Example: a full moon may need a subtle dark outline on a light background but no outline on a dark background.
- **Full-color images and photographs:** if the same asset looks fine in both, ship one. If the white background glows in dark mode, darken the asset slightly so it doesn't bloom against the dark UI.
- Combine light/dark assets into a single named image in the asset catalog.

## Testing checklist

- [ ] Every screen in light and dark.
- [ ] Every screen in **Increase Contrast** + light + dark.
- [ ] Every screen with **Reduce Transparency** on.
- [ ] Dark color variants have 4.5:1 minimum contrast (7:1 for small/custom).
- [ ] Photographs with white backgrounds don't bloom.
- [ ] Custom icons legible in both.
- [ ] No hex literals in production UI.
- [ ] No app-level appearance override switch.

## Platform notes

### iOS / iPadOS
Base vs elevated background switch is automatic. Prefer system backgrounds in sheets and popovers.

### macOS
Desktop tinting is a user-visible effect. Test with a few desktop pictures. Use AppKit dynamic colors (`NSColor.windowBackgroundColor`, etc.).

### tvOS
Follows the same rules; the system handles vibrancy on Liquid Glass in nav.

### visionOS
**No Dark Mode setting.** Glass windows adapt to the luminance of what's behind them (physical room + virtual content). Design for the glass, not for a dark or light theme. See [`materials.md`](./materials.md).

### watchOS
Always dark. Never ship a "light" variant.

## Common mistakes

- Hex color literal in production UI (`#FFFFFF`, `rgb(255, 0, 0)`).
- App-specific appearance setting.
- Custom dark background that collapses base/elevated layering.
- Testing only in Dark Mode with default contrast; missing Increase Contrast failures.
- Photographs with glowing white backgrounds.
- Icon that only works in light mode.
- Hardcoded tint that doesn't shift in Dark Mode.

## SwiftUI

```swift
// Semantic. Adapts automatically.
Rectangle().fill(Color(.systemBackground))
Text("Today").foregroundStyle(Color(.label))

// Asset catalog custom color (Any, Dark, Increased Contrast variants defined).
Rectangle().fill(Color("brand-surface"))

// Preview both modes during development
#Preview("Light") { HomeView().preferredColorScheme(.light) }
#Preview("Dark")  { HomeView().preferredColorScheme(.dark)  }
```

## UIKit

```swift
view.backgroundColor = .systemBackground
titleLabel.textColor = .label
bodyLabel.textColor = .secondaryLabel

// Custom color with dynamic provider
let adaptive = UIColor { trait in
    trait.userInterfaceStyle == .dark ? .init(white: 0.12, alpha: 1)
                                      : .init(white: 0.96, alpha: 1)
}
card.backgroundColor = adaptive
```

## AppKit

```swift
window.backgroundColor = .windowBackgroundColor
label.textColor = .labelColor

// Dynamic color
let adaptive = NSColor(name: nil) { appearance in
    appearance.bestMatch(from: [.darkAqua, .vibrantDark]) != nil
        ? NSColor(white: 0.12, alpha: 1)
        : NSColor(white: 0.96, alpha: 1)
}
customView.layer?.backgroundColor = adaptive.cgColor
```

## See also

- [`color.md`](./color.md) — system color catalog and semantic roles.
- [`materials.md`](./materials.md) — Liquid Glass and vibrancy in dark.
- [`accessibility.md`](./accessibility.md) — contrast minimums.
