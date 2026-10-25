# Typography

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/typography
**Last verified:** 2026-04-20
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

Use the system fonts and the system-defined text styles. They are variable-font, dynamic-optical-sized, Dynamic-Type-aware, and paired with SF Symbols by weight and scale. That's the whole argument.

## The system fonts

- **SF Pro** — default sans-serif on iOS, iPadOS, macOS, tvOS, and visionOS.
- **SF Compact** — default on watchOS (and SF Compact Rounded in complications).
- **New York (NY)** — optional serif family, available on all platforms.
- **SF Pro Rounded** — optional rounded sans, for a softer voice.
- Scripts: **SF Arabic, SF Armenian, SF Georgian, SF Hebrew, SF Mono**, and NY variants of same where applicable.

All ship as **variable fonts** with **dynamic optical sizing** — the system interpolates each glyph's design based on point size, so you do not need to pick a discrete optical size.

Do not embed the system fonts in your app. Use the provided API constants (`Font.system(...)`, `UIFont.systemFont(...)`, `NSFont.systemFont(...)`).

## Text styles — use them

Text styles are the system's typographic hierarchy. They combine weight, size, leading, and tracking so you get Dynamic Type, Bold Text, Larger Accessibility Sizes, and correct Bold Text behavior for free.

Names (shared across platforms that support Dynamic Type): Large Title, Title 1, Title 2, Title 3, Headline, Body (default), Callout, Subheadline, Footnote, Caption 1, Caption 2.

Emphasized variants exist via symbolic traits (`.bold()` in SwiftUI, `symbolicTraits` in UIKit).

```swift
Text("Today")
    .font(.largeTitle.bold())
Text("Sync is on — last sync 2 min ago")
    .font(.footnote)
    .foregroundStyle(.secondary)
```

## Minimum and default sizes

Follow these when you cannot use text styles. If using a custom font with a thin weight, go larger than the minimum — light weights lose legibility fast.

| Platform | Default | Minimum |
|---|---|---|
| iOS / iPadOS | 17pt | 11pt |
| macOS | 13pt | 10pt |
| tvOS | 29pt | 23pt |
| visionOS | 17pt | 12pt |
| watchOS | 16pt | 12pt |

Prefer Regular / Medium / Semibold / Bold. Avoid Ultralight / Thin / Light for anything readable.

## Dynamic Type

Available on iOS, iPadOS, tvOS, visionOS, and watchOS. Not on macOS.

- Every body copy string uses a text style, not a fixed point size.
- Test at **Larger Accessibility Sizes** (AX1–AX5) and confirm layouts reflow or scroll.
- Text should never truncate in a scrollable region unless an expandable view shows the rest.
- SF Symbols scale with Dynamic Type when declared via `Image(systemName:)` / `UIImage(systemName:)`.
- Consider switching from side-by-side to stacked layouts at AX sizes.
- Reduce column count at AX sizes in multi-column layouts.

```swift
// SwiftUI — correct
Text(event.title).font(.headline)
Text(event.summary).font(.body)

// SwiftUI — wrong (breaks Dynamic Type)
Text(event.title).font(.system(size: 17, weight: .semibold))
```

```swift
// UIKit — scaling a custom font with Dynamic Type
let base = UIFont(name: "Georgia-Bold", size: 17)!
let scaled = UIFontMetrics(forTextStyle: .headline).scaledFont(for: base)
label.font = scaled
label.adjustsFontForContentSizeCategory = true
```

## macOS built-in text styles

macOS does not support Dynamic Type. Instead, use the built-in macOS text styles, which include matching emphasized weights.

| Style | Weight | Size | Line height | Emphasized |
|---|---|---|---|---|
| Large Title | Regular | 26 | 32 | Bold |
| Title 1 | Regular | 22 | 26 | Bold |
| Title 2 | Regular | 17 | 22 | Bold |
| Title 3 | Regular | 15 | 20 | Semibold |
| Headline | Bold | 13 | 16 | Heavy |
| Body | Regular | 13 | 16 | Semibold |
| Callout | Regular | 12 | 15 | Semibold |
| Subheadline | Regular | 11 | 14 | Semibold |
| Footnote | Regular | 10 | 13 | Semibold |
| Caption 1 | Regular | 10 | 13 | Medium |
| Caption 2 | Medium | 10 | 13 | Semibold |

Use dynamic font variants to match system controls: control content, label, menu, menu bar, message, palette, title, tool tip, document text, monospaced document text. In SwiftUI on macOS use `Font.body`, `.headline`, etc. or `NSFont` named variants.

## tvOS built-in text styles

| Style | Weight | Size | Leading | Emphasized |
|---|---|---|---|---|
| Title 1 | Medium | 76 | 96 | Bold |
| Title 2 | Medium | 57 | 66 | Bold |
| Title 3 | Medium | 48 | 56 | Bold |
| Headline | Medium | 38 | 46 | Bold |
| Subtitle 1 | Regular | 38 | 46 | Medium |
| Callout | Medium | 31 | 38 | Bold |
| Body | Medium | 29 | 36 | Bold |
| Caption 1 | Medium | 25 | 32 | Bold |
| Caption 2 | Medium | 23 | 30 | Bold |

## Platform notes

### iOS / iPadOS
SF Pro. Dynamic Type. NY available.

### macOS
SF Pro. No Dynamic Type — use text styles + emphasized weights. NY available via Mac Catalyst.

### tvOS
SF Pro. Uses the tvOS-specific style table above. NY available.

### visionOS
SF Pro. Introduces **Extra Large Title 1** and **Extra Large Title 2** for editorial layouts. Uses **bolder** Body and Title styles than other platforms to stay legible through the glass material. Prefer 2D text — depth makes characters hard to read. Use **billboarding** so text faces the wearer. Maximize contrast against the underlying glass and environment.

### watchOS
SF Compact is the system font. SF Compact Rounded in complications. NY available. Dynamic Type supported.

## Custom fonts — when you must

- Must implement Dynamic Type (`UIFontMetrics` / `scaledFont(for:)`).
- Must support Bold Text and larger minimum sizes when weight is thin.
- Test Switch Control, VoiceOver, and Increase Contrast against it.
- Never override system font for system controls.

## Writing hierarchy rules

- Minimize the number of typefaces — mixing many obscures hierarchy.
- Prioritize important content when text scales up; don't grow chrome proportionally.
- Keep relative visual distinction intact across every text size.
- Use emphasized weights (bold/semibold) to step up within a style rather than jumping a style.

## Common mistakes

- Setting `font: .system(size: 17)` instead of `.body`.
- Hardcoding custom pt sizes for body text.
- Using Ultralight/Thin/Light body copy.
- Mixing 4+ typefaces.
- Embedding SF Pro instead of using system API.
- Title bars growing with Dynamic Type (they shouldn't — only content should).
- Tight leading applied across 3+ lines of running text.
- Not testing at AX5 size.

## SwiftUI

```swift
VStack(alignment: .leading, spacing: 4) {
    Text("Preferences").font(.largeTitle.bold())
    Text("General").font(.title3.weight(.semibold))
    Text("Change how the app looks, where data is stored, and more.")
        .font(.body)
        .foregroundStyle(.secondary)
    Text("Last saved just now")
        .font(.caption)
        .foregroundStyle(.tertiary)
}
.lineLimit(nil)                       // reflow freely at AX sizes
.dynamicTypeSize(...DynamicTypeSize.accessibility5)
```

## UIKit

```swift
let label = UILabel()
label.font = .preferredFont(forTextStyle: .body)
label.adjustsFontForContentSizeCategory = true
label.numberOfLines = 0

let headline = UILabel()
let base = UIFont.preferredFont(forTextStyle: .headline)
let bold = UIFont(descriptor: base.fontDescriptor.withSymbolicTraits(.traitBold)!, size: 0)
headline.font = bold
headline.adjustsFontForContentSizeCategory = true
```

## AppKit

```swift
// No Dynamic Type — prefer the macOS text styles.
let title = NSTextField(labelWithString: "Preferences")
title.font = .preferredFont(forTextStyle: .largeTitle, options: [:])
title.allowsDefaultTighteningForTruncation = true
```

## See also

- [`accessibility.md`](./accessibility.md) — Dynamic Type + Bold Text rules.
- [`sf-symbols.md`](./sf-symbols.md) — symbols match SF weights and scales.
- [`color.md`](./color.md) — label/secondaryLabel/tertiaryLabel pair with text styles.
