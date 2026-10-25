# Color

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/color
**Last verified:** 2026-04-20
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

Use semantic system colors. Never hardcode hex values for anything that ships. The system colors adapt for Dark Mode, Increase Contrast, vibrancy, desktop tinting (macOS), wide-gamut displays, and accessibility settings you haven't thought about.

## Rules

- **Don't hardcode system color values.** They can change between releases. Use the API (`Color.accentColor`, `Color(.label)`, `UIColor.systemBlue`, `NSColor.controlAccentColor`).
- **Don't redefine semantic meanings.** `Color(.label)` is for labels. Do not use it as a background. `Color(.systemBackground)` is for backgrounds — not text.
- **Use the same color consistently for the same thing.** If your accent means "interactive," don't use the same shade for non-interactive text.
- **Provide light, dark, and increased-contrast variants** for every custom color. Use asset catalogs (Color Sets).
- **Don't rely on color alone** to communicate meaning. Add an icon, label, or pattern.
- **Test in bright and dim environments.** Colors look more muted in sunlight, more saturated in the dark. The True Tone display shifts white points.
- **Consider cultural meaning.** Red = danger / red = prosperity — know your audience.

## The 12 system colors

Available on all platforms. Use these before inventing anything.

| Name | SwiftUI | UIKit |
|---|---|---|
| Red | `Color.red` | `UIColor.systemRed` |
| Orange | `Color.orange` | `UIColor.systemOrange` |
| Yellow | `Color.yellow` | `UIColor.systemYellow` |
| Green | `Color.green` | `UIColor.systemGreen` |
| Mint | `Color.mint` | `UIColor.systemMint` |
| Teal | `Color.teal` | `UIColor.systemTeal` |
| Cyan | `Color.cyan` | `UIColor.systemCyan` |
| Blue | `Color.blue` | `UIColor.systemBlue` |
| Indigo | `Color.indigo` | `UIColor.systemIndigo` |
| Purple | `Color.purple` | `UIColor.systemPurple` |
| Pink | `Color.pink` | `UIColor.systemPink` |
| Brown | `Color.brown` | `UIColor.systemBrown` |

Each has default-light, default-dark, increased-contrast-light, and increased-contrast-dark variants built in. visionOS uses the default dark values.

## Six system grays

| SwiftUI | UIKit | Use |
|---|---|---|
| `Color.gray` | `UIColor.systemGray` | Primary gray |
| — (use `UIColor.systemGray2`) | `UIColor.systemGray2` | Slightly lighter |
| — | `UIColor.systemGray3` | Lighter |
| — | `UIColor.systemGray4` | Lighter |
| — | `UIColor.systemGray5` | Lightest non-background |
| — | `UIColor.systemGray6` | Background-level gray |

`Color.gray` in SwiftUI maps to `UIColor.systemGray`. For 2–6 in SwiftUI on iOS, use `Color(uiColor: .systemGray3)` etc.

## Semantic foreground colors (iOS / iPadOS / visionOS)

| Color | SwiftUI | UIKit | Use for |
|---|---|---|---|
| Label | `Color(.label)` | `UIColor.label` | Primary text |
| Secondary label | `Color(.secondaryLabel)` | `UIColor.secondaryLabel` | Secondary text |
| Tertiary label | `Color(.tertiaryLabel)` | `UIColor.tertiaryLabel` | Tertiary text |
| Quaternary label | `Color(.quaternaryLabel)` | `UIColor.quaternaryLabel` | Watermark, disabled |
| Placeholder | `Color(.placeholderText)` | `UIColor.placeholderText` | Placeholders |
| Separator | `Color(.separator)` | `UIColor.separator` | See-through separators |
| Opaque separator | `Color(.opaqueSeparator)` | `UIColor.opaqueSeparator` | Fully opaque separators |
| Link | `Color(.link)` | `UIColor.link` | Text links |

## Semantic background colors (iOS / iPadOS)

Two sets. Pick the one that matches your view:

- **System set** — `systemBackground`, `secondarySystemBackground`, `tertiarySystemBackground`. Use for plain views.
- **Grouped set** — `systemGroupedBackground`, `secondarySystemGroupedBackground`, `tertiarySystemGroupedBackground`. Use inside a grouped list/table.

Pattern: Primary for the overall view, Secondary for groupings, Tertiary for groupings within secondaries.

## Semantic colors (macOS)

macOS is richer — it has distinct colors for every control state. Prefer AppKit APIs (`NSColor.labelColor`, `NSColor.controlBackgroundColor`, etc.) or `Color(nsColor:)` in SwiftUI. Key ones:

| AppKit | Purpose |
|---|---|
| `NSColor.labelColor` | Primary text |
| `NSColor.secondaryLabelColor` | Secondary text |
| `NSColor.tertiaryLabelColor` | Tertiary text |
| `NSColor.quaternaryLabelColor` | Watermark |
| `NSColor.textColor` | Document text |
| `NSColor.textBackgroundColor` | Behind document text |
| `NSColor.placeholderTextColor` | Placeholders |
| `NSColor.linkColor` | Links |
| `NSColor.separatorColor` | Separators |
| `NSColor.gridColor` | Table gridlines |
| `NSColor.controlBackgroundColor` | Control surface backgrounds |
| `NSColor.controlAccentColor` | User's chosen accent |
| `NSColor.windowBackgroundColor` | Window background |
| `NSColor.underPageBackgroundColor` | Behind documents |
| `NSColor.keyboardFocusIndicatorColor` | Focus ring |
| `NSColor.selectedContentBackgroundColor` | Selected content background |
| `NSColor.unemphasizedSelectedContentBackgroundColor` | Selection in non-key window |

## App accent color

Set a single accent color in your asset catalog. The system applies it to buttons, selection highlights, sidebar icons, and on Liquid Glass to primary call-to-action backgrounds (see below).

On macOS, if the user picks a non-multicolor accent in System Settings, the system replaces yours with theirs for relevant chrome. Fixed-color sidebar icons are the exception.

## Liquid Glass and color

By default, Liquid Glass has no inherent color — it picks up the content behind it. You **can** tint Liquid Glass to draw attention (the system does this for prominent buttons, applying the accent to the button background).

- **Apply color sparingly.** Reserve it for status indicators and primary actions.
- **Color the background, not the glyph.** To emphasize a primary action, tint the Liquid Glass background, keep the label/icon monochrome.
- **On colorful backgrounds, go monochromatic** on bars and tab bars or pick an accent with enough contrast — otherwise tinted chrome is unreadable.
- **Keep resting state legible.** The top of a scroll view (content behind the nav bar at rest) is where legibility matters most.

## Inclusive color

- Provide non-color indicators for every color-encoded state (icon, shape, text).
- Aim for the contrast ratios in [`accessibility.md`](./accessibility.md).
- Avoid pairings that color-blind users can't distinguish (red/green, blue/orange).
- Let people customize color schemes (chart colors, game characters) when it matters for usability.

## Color management

- Common color spaces: **sRGB** (safe default) and **Display P3** (wide color).
- Apply a color profile to every image you ship.
- Use Display P3 for richer photos/videos on wide-gamut displays. Provide color-space-specific variants in asset catalogs when P3 and sRGB diverge (especially with gradients).
- Test tvOS apps on multiple TVs and display modes. On Mac, switch color profiles in System Settings > Displays.

## Platform notes

### iOS / iPadOS
Two background sets (system, grouped). Dynamic colors adapt to light/dark/contrast.

### macOS
Rich set of dynamic AppKit colors. Respect the user's accent color. Graphite accent triggers desktop tinting — allow transparency in custom components with a visible background only in neutral states.

### tvOS
Limited palette; coordinate with your logo. **Don't use color alone to indicate focus** — the focus engine uses scale and parallax.

### visionOS
Use color sparingly, especially on glass. Windows use the adaptive glass material; bright content can be washed out by the environment behind it. Prefer color in bold text and large areas. In fully immersive experiences, keep brightness balanced — avoid bright flashes on dark backgrounds.

### watchOS
Background color establishes a sense of place; match it to an infographic's content. Avoid full-screen background color in long-running views (workouts, audio). Tinted graphic complications can collapse your art to a single user-chosen color — design for it.

## Common mistakes

- `Color(red: 0.1, green: 0.1, blue: 0.1)` instead of `Color(.systemBackground)`.
- Using `Color.accentColor` for all interactive states including disabled.
- Tinting every toolbar button with the accent color.
- Defining only a light variant for a custom color.
- Using `Color.red` to mean error without also adding an icon or label.
- Relying on the accent color to be blue on macOS.
- Putting bright colored text on glass in visionOS without testing against dark and bright environments.

## SwiftUI

```swift
// Correct — semantic + dynamic
Text("Title")
    .foregroundStyle(Color(.label))
    .frame(maxWidth: .infinity)
    .background(Color(.systemBackground))

// Custom color from asset catalog
Rectangle().fill(Color("BrandAccent"))

// Tinted button (works on Liquid Glass)
Button("Continue") { }
    .buttonStyle(.borderedProminent)
    .tint(.accentColor)

// A chart / status indicator that doesn't rely on color alone
Label("Offline", systemImage: "wifi.slash")
    .foregroundStyle(.secondary)
```

## UIKit

```swift
view.backgroundColor = .systemBackground
titleLabel.textColor = .label
subtitleLabel.textColor = .secondaryLabel
separator.backgroundColor = .separator

let brand = UIColor(named: "BrandAccent")!
button.tintColor = brand
```

## AppKit

```swift
window.backgroundColor = .windowBackgroundColor
titleLabel.textColor = .labelColor
tableView.backgroundColor = .controlBackgroundColor
tableView.gridColor = .gridColor
// Respect the user's accent
button.contentTintColor = .controlAccentColor
```

## See also

- [`dark-mode.md`](./dark-mode.md) — base vs elevated backgrounds, desktop tinting.
- [`accessibility.md`](./accessibility.md) — contrast minimums.
- [`materials.md`](./materials.md) — Liquid Glass, vibrant colors on materials.
- [`../platforms/macos.md`](../platforms/macos.md) — macOS-specific color usage.
