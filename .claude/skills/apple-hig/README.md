# apple-hig

A Claude Code skill that makes Claude actually follow Apple's [Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines/) when you're building for iOS, iPadOS, macOS, watchOS, tvOS, or visionOS — instead of producing generic "Apple-ish" output.

Works with **SwiftUI**, **UIKit**, **AppKit**, and web/Figma mockups that need to feel Apple-native.

> Status: **stable · v1.0.0.** Feature-complete across foundations, patterns, components, inputs, platforms, and technologies — 68 reference files, ~15k lines, every one cited to the live HIG. See [`CHANGELOG.md`](./CHANGELOG.md).

## What it does

When you mention building something for an Apple platform, this skill kicks in automatically and:

1. **Asks a couple of targeting questions first** (platform, framework, what you're actually building) — so the answer fits your context, not a generic mold.
2. **Loads only the reference files that matter** for your task — progressive disclosure, no context bloat.
3. **Applies the non-negotiables** every time: semantic colors, Dynamic Type, SF Symbols, ≥44pt touch targets, VoiceOver labels, Reduce Motion, platform-correct navigation.
4. **Cites the live HIG** at the top of every reference file so you can verify.

## Install

Paste this into Claude Code:

```bash
git clone --single-branch --depth 1 https://github.com/ebuntario/apple-hig.git ~/.claude/skills/apple-hig && cd ~/.claude/skills/apple-hig && ./setup
```

Then start a new Claude Code session. Ask about an Apple-platform UI and the skill auto-triggers.

## Update

```bash
cd ~/.claude/skills/apple-hig && git pull
```

## Uninstall

```bash
~/.claude/skills/apple-hig/bin/apple-hig-uninstall
```

Or just `rm -rf ~/.claude/skills/apple-hig`.

## What it covers

| Area | Depth |
|---|---|
| iOS / iPhone | Deep |
| macOS | Deep |
| iPadOS | Solid |
| watchOS | Solid |
| tvOS | Solid |
| visionOS | Solid |
| SwiftUI snippets | Primary |
| UIKit / AppKit snippets | Secondary |
| Web / Figma native-feel | Notes where relevant |

Reference tree mirrors the HIG and covers:

| Area | Files |
|---|---|
| `foundations/` — accessibility, typography, color, dark-mode, layout, sf-symbols, materials, motion | 8 |
| `patterns/` — navigation, modality, settings, search, onboarding, loading, feedback, entering-data, drag-and-drop, managing-accounts, in-app-purchase, managing-notifications, undo-and-redo, file-management, offering-help, multitasking, printing, charting-data, going-full-screen, live-viewing-apps, workouts, ratings-and-reviews | 22 |
| `components/` — bars, buttons, lists-and-tables, sheets-and-popovers, text-inputs, pickers-and-menus, collections-and-scrolling, segmented-controls, sliders-steppers-toggles, progress-and-activity, labels-and-badges, split-views, system-experiences | 13 |
| `inputs/` — touch-and-gestures, pointer-and-keyboard, apple-pencil, digital-crown, focus-and-remote, spatial-input, game-controllers | 7 |
| `platforms/` — ios (deep), macos (deep), ipados, watchos, tvos, visionos | 6 |
| `technologies/` — widgets-and-live-activities, dynamic-island, control-center, lock-screen, shortcuts-and-siri, app-clips, share-extension, carplay, homekit, imessage-apps, game-center, generative-ai | 12 |
| `assets/checklists/` — pre-submission-hig-review, accessibility-review, platform-parity-review | 3 |

Full routing index: [`SKILL.md`](./SKILL.md).

## How it differs from similar skill packs

- **No CLAUDE.md injection.** `./setup` does not edit your global config. The skill is discovered by the Claude Code harness on its own. If you want it imported into `CLAUDE.md` explicitly, run `./setup --inject-claudemd`.
- **Question-first.** The skill asks before generating, because "an iOS app" and "a macOS menu bar app" need very different answers.
- **Cited, not invented.** Every reference file starts with the HIG URL it's sourced from. If the HIG is silent on something, the file says so and labels the default.

## Contributing

PRs welcome — especially for:
- HIG updates after a WWDC release.
- Missing component/pattern files.
- Better SwiftUI/UIKit/AppKit snippets.

Keep snippets idiomatic for the current deployment targets listed in each file. Don't paraphrase large blocks of Apple's prose; short quotes + citations only.

## License

[MIT](./LICENSE)
