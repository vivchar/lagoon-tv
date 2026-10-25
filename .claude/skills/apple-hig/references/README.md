# references/

Progressive-disclosure reference files. `SKILL.md` routes to these on demand.

Structure:

- `foundations/` — typography, color, dark-mode, layout, sf-symbols, materials,
  motion, accessibility, inclusion, privacy, writing, app-icons, branding.
- `patterns/` — navigation, modality, onboarding, search, settings, loading,
  feedback, and more.
- `components/` — bars, buttons, lists-and-tables, sheets-and-popovers, and
  more.
- `inputs/` — touch, pointer, keyboard, Apple Pencil, Digital Crown, focus /
  remote (tvOS), spatial input (visionOS), game controllers.
- `platforms/` — iOS, iPadOS, macOS, watchOS, tvOS, visionOS. iOS and macOS are
  deepest.
- `technologies/` — Widgets, Live Activities, Dynamic Island, Control Center,
  Lock Screen, Shortcuts, App Clips, share extension, CarPlay, HomeKit,
  iMessage apps, Game Center, Apple Intelligence UX.

## File template

Every file starts with:

```markdown
# <Topic>

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/<slug>
**Last verified:** YYYY-MM-DD
**Applies to:** iOS · iPadOS · macOS · watchOS · tvOS · visionOS
```

Then: What it is / When to use / Platform differences / Sizing & spacing /
Accessibility / Common mistakes / SwiftUI snippet / UIKit or AppKit snippet /
See also.

Files longer than 300 lines include a table of contents at the top.

## Status

Scaffold in place, content authoring pending. See `CHANGELOG.md`.
