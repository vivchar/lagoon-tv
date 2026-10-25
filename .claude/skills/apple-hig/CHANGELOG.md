# Changelog

All notable changes to `apple-hig` will be documented here. Format:
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versioning:
[SemVer](https://semver.org/). Each authoring batch maps to a minor release.

## [1.0.0] — 2026-04-21

**Stable release.** The skill is feature-complete across the original
scope: foundations, patterns, components, inputs, platforms, and
technologies. 68 reference files, ~15,000 lines of reference content,
every file cited to the live Apple HIG.

This release changes no files beyond `VERSION`, `README.md`, `SKILL.md`
status labels, and this changelog — content shipped across 0.2.0
through 0.10.0.

### Coverage

| Area | Files | Depth |
|---|---|---|
| Foundations | 8 | accessibility, typography, color, dark-mode, layout, sf-symbols, materials, motion |
| Patterns | 22 | navigation, modality, settings, search, onboarding, loading, feedback, entering-data, drag-and-drop, managing-accounts, in-app-purchase, managing-notifications, undo-and-redo, file-management, offering-help, multitasking, printing, charting-data, going-full-screen, live-viewing-apps, workouts, ratings-and-reviews |
| Components | 13 | bars, buttons, lists-and-tables, sheets-and-popovers, text-inputs, pickers-and-menus, collections-and-scrolling, segmented-controls, sliders-steppers-toggles, progress-and-activity, labels-and-badges, split-views, system-experiences |
| Inputs | 7 | touch-and-gestures, pointer-and-keyboard, apple-pencil, digital-crown, focus-and-remote, spatial-input, game-controllers |
| Platforms | 6 | ios (deep), macos (deep), ipados, watchos, tvos, visionos |
| Technologies | 12 | widgets-and-live-activities, dynamic-island, control-center, lock-screen, shortcuts-and-siri, app-clips, share-extension, carplay, homekit, imessage-apps, game-center, generative-ai |
| Checklists | 3 | pre-submission-hig-review, accessibility-review, platform-parity-review |

### Non-negotiables enforced by the skill

- Minimum 44×44pt touch targets on iOS / iPadOS / watchOS (28pt macOS,
  60pt visionOS, always-focusable tvOS).
- Dynamic Type via text styles.
- Full Dark Mode via semantic colors.
- VoiceOver labels on every non-decorative view.
- SF Symbols preferred over custom icons when a system glyph exists.
- Respect `@Environment(\.accessibilityReduceMotion)`.
- Platform-appropriate navigation (tab bar vs sidebar vs menu bar vs
  ornaments).
- Safe-area-aware layout.
- RTL-safe layouts.
- Writing tone matches platform (Tap / Click / Select).

### Phase 0

The skill asks targeting questions first — platform, framework, what
the user is building — before generating. Skipped when the user's
brief already answers them.

### Install

```
git clone --single-branch --depth 1 https://github.com/ebuntario/apple-hig.git ~/.claude/skills/apple-hig && cd ~/.claude/skills/apple-hig && ./setup
```

### Maintenance

HIG updates yearly at WWDC. The `Last verified:` date at the top of
every reference file indicates when the file was sourced from the
live HIG. Refresh these dates after each WWDC and revise any changed
numbers, APIs, or guidance.

## [0.10.0] — 2026-04-21

Specialized patterns. Seven files for flows that show up in narrower app
categories: multitasking, printing, charting data, going full screen, live
viewing, workouts, and ratings/reviews. Each cited to the live HIG.

### Added — Patterns (specialized)

- `patterns/multitasking.md` — save and restore context; pause user-
  attention tasks; audio interruption handling; per-platform
  behaviors for iPadOS (Split View, Stage Manager, multi-window),
  macOS, tvOS (PiP), visionOS (Shared vs Full Space, feathered mask).
- `patterns/printing.md` — discoverable in standard locations (File
  menu / share sheet); system print panel; macOS custom print
  categories; iOS `UIActivityViewController` + `UIPrintInteractionController`;
  `UIPrintPageRenderer` for custom layouts.
- `patterns/charting-data.md` — Swift Charts first; common chart types;
  descriptive headlines; consistency across charts; accessibility
  (AXChartDescriptor, audio graphs, color-plus-shape); when NOT to
  chart.
- `patterns/going-full-screen.md` — when to offer; system mechanism
  (don't fight it); preserve essential controls; pause on switch;
  iOS defer system gestures for games; macOS ⌃⌘F.
- `patterns/live-viewing-apps.md` — live content prominence; "looks
  live" visual cues; one-tap Watch Now; EPG basics; content footer;
  cloud DVR UX; Live Activity + PiP integration; per-platform rules.
- `patterns/workouts.md` — watchOS active session rules (unique
  layout, large numerals, motion-legible, pause/resume/stop easy);
  summary screen with Activity Rings; sensor caveats; Live Activity
  + Dynamic Island on iPhone workouts; HealthKit permission basics.
- `patterns/ratings-and-reviews.md` — ask after engagement, not
  launch/onboarding; natural stopping points; system prompt
  (`SKStoreReviewController` / `requestReview`); 3-prompt annual
  limit; reset-summary trade-offs; separate in-app feedback channel.

## [0.9.0] — 2026-04-21

Common app patterns. Eight files covering the patterns that show up in
almost every app: data entry, drag and drop, account management, in-app
purchase, notifications, undo and redo, file management, and offering
help. Each cited to the live HIG.

### Added — Patterns (common)

- `patterns/entering-data.md` — pre-fill from system, keyboard types and
  content-type hints for AutoFill/strong-password/2FA, secure fields,
  dictation/Scribble/Continuity keyboard, dynamic validation.
- `patterns/drag-and-drop.md` — move vs copy semantics; feedback rules;
  multi-format drags with prioritized fidelity; spring loading; iPad
  additive drag; macOS inactive-window drags; visionOS z-axis + drop-
  into-empty-space.
- `patterns/managing-accounts.md` — when to require accounts, Sign in
  with Apple first, passkeys over passwords, mandatory account
  deletion, TV provider accounts, tvOS another-device auth.
- `patterns/in-app-purchase.md` — IAP vs Apple Pay; four purchase
  types; Family Sharing; subscription signup required content; offer
  codes; system-provided management UI; exit surveys and win-back.
- `patterns/managing-notifications.md` — permission earned after value;
  four interruption levels; Focus + delivery scheduling; marketing
  requires separate opt-in; never Time Sensitive for marketing;
  Lock Screen privacy.
- `patterns/undo-and-redo.md` — multi-level undo; action names;
  scroll-to-change; batched undo for incremental adjustments;
  iOS three-finger swipe/shake; macOS ⌘Z/⇧⌘Z.
- `patterns/file-management.md` — document launcher (iOS 18+/iPadOS
  18+); autosave default; Quick Look previews and generators; file
  provider extensions; Finder Sync; DocumentGroup and NSDocument.
- `patterns/offering-help.md` — contextual over global; TipKit;
  macOS tooltips; Help menu with ⌘?.

### Pending

- Remaining patterns (batch 10 / v0.10.0): multitasking, printing,
  charting-data, going-full-screen, live-viewing-apps, workouts,
  ratings-and-reviews.

## [0.8.0] — 2026-04-21

Technologies — the high-leverage integration surfaces where your app meets
the rest of the system. Twelve files covering widgets, Live Activities,
Dynamic Island, Control Center + Controls, Lock Screen, Shortcuts + Siri,
App Clips, share and action extensions, CarPlay, HomeKit, iMessage apps,
Game Center, and Generative AI. Where the HIG doesn't ship a standalone
page (Dynamic Island, Control Center, Lock Screen, Shortcuts), the file is
a synthesis across related HIG sources with explicit citation.

### Added — Technologies

- `technologies/widgets-and-live-activities.md` — widget families and
  sizes per device, rendering modes (full-color / accented / vibrant),
  Live Activity presentations, Always On rules.
- `technologies/dynamic-island.md` — the three presentations in depth;
  snug-to-camera layouts; key line tinting; alert discipline.
- `technologies/control-center.md` — Controls (iOS 18+) built on App
  Intents; Lock Screen / Action button placement; one-action purity.
- `technologies/lock-screen.md` — all Lock Screen surfaces (accessory
  widgets, Live Activity banner, Controls, notifications, StandBy,
  media lock screens); vibrant rendering; privacy in public view.
- `technologies/shortcuts-and-siri.md` — App Intents as the primitive
  behind Shortcuts / Siri / Action button / Controls; voice-only
  design; background operation; AppShortcuts declaration.
- `technologies/app-clips.md` — the 10-second rule; discovery paths
  (Codes, NFC, QR, links); linear focused UX; card design; post-task
  app promotion without nagging.
- `technologies/share-extension.md` — share vs action extensions; fast
  focused flows; system composition view; long-running work
  continues in the app.
- `technologies/carplay.md` — supported categories; iPhone-independence;
  template-driven UI; sunlight-tested colors;
  MPNowPlayingInfoCenter.
- `technologies/homekit.md` — the object model (home / room / zone /
  accessory / service / characteristic / scene / automation); Siri
  vocabulary discipline; custom features vs Home app boundary.
- `technologies/imessage-apps.md` — compact vs expanded tray; sticker
  sizes and formats; APNG over GIF; alt text on every sticker.
- `technologies/game-center.md` — access point discipline; four
  achievement states; classic vs recurring leaderboards; challenges
  track recent score; terminology lexicon; party codes.
- `technologies/generative-ai.md` — transparency; inclusivity; privacy
  (Foundation Models on-device); hallucination mitigation; confirm
  destructive actions; latency UX; feedback loops; accessibility of
  generated content.

### Pending

- Remaining patterns (batch 10): entering-data, drag-and-drop,
  managing-accounts, in-app-purchase, multitasking, printing,
  managing-notifications, undo-and-redo, charting-data,
  going-full-screen, live-viewing-apps, workouts,
  ratings-and-reviews, offering-help, file-management.

## [0.7.0] — 2026-04-21

Inputs — the "how people interact" references that platform and component
files link to repeatedly. Seven files, one per input surface, each cited to
the live HIG and organized by platform rules + rules of use + SwiftUI and
UIKit/AppKit snippets.

### Added — Inputs

- `inputs/touch-and-gestures.md` — standard gesture catalog across all six
  platforms (tap, swipe, drag, touch-and-hold, double-tap, zoom, rotate);
  iOS/iPadOS extras (three-finger swipe undo/redo, three-finger pinch
  copy/paste, four-finger app switch, shake); visionOS indirect vs direct
  gestures; tvOS clickpad rules; custom-gesture discipline (discoverable,
  distinct, not the only path).
- `inputs/pointer-and-keyboard.md` — iPadOS content effects (highlight /
  lift / hover), pointer shapes and accessories, pointer magnetism; macOS
  trackpad gesture catalog, standard pointer shapes; visionOS pointer +
  gaze coexistence; keyboards, Full Keyboard Access, standard shortcut
  catalog, modifier-key ordering and semantics, visionOS ⌘-hold overlay,
  game keybinding comfort rules.
- `inputs/apple-pencil.md` — per-model capability matrix (pressure, tilt,
  azimuth, hover, double tap, squeeze, barrel roll); hover preview rules;
  double-tap + squeeze + barrel-roll discipline (no destructive actions on
  double-tap/squeeze, marking-only for barrel-roll); Scribble rules
  (invisible focus, don't move fields while writing, hide placeholder,
  don't autocomplete aggressively, don't autoscroll); PencilKit canvas
  rules.
- `inputs/digital-crown.md` — Vision Pro Crown is system-only (volume,
  immersion, recentering, accessibility, exit); Apple Watch Crown is
  primary navigation + value tuning; haptic detent rules; no Crown press
  handling in apps; SwiftUI `.digitalCrownRotation(...)` example.
- `inputs/focus-and-remote.md` — focus system across iPadOS, macOS, tvOS,
  visionOS; iPadOS focus groups (Tab between, arrow within); tvOS five
  focus states (unfocused, focused, selected, checked, disabled); halo vs
  highlighted appearance; Siri Remote layout and gesture catalog; Back
  button contract (apps vs games); incidental-tap protection during
  playback; compatible remote buttons for EPG and live TV.
- `inputs/spatial-input.md` — visionOS eyes + hands in depth; hover effect
  ≠ focus; gaze privacy (no pre-pinch reporting); 60pt target + spacing
  rule; rounded shapes for comfort; custom hover effects (delay timing,
  constant primary view, out-of-process nature, no action-on-hover);
  indirect-default / direct-for-reach rules; comfort rules across the
  board.
- `inputs/game-controllers.md` — touch controls for iOS/iPadOS games
  (virtual controls vs direct manipulation, placement, sizing, press
  states, contextual show/hide, virtual thumbstick under the thumb);
  physical controller auto-detect; UI button contract (A activates, B
  cancels, left/right shoulder = page, menu = pause); brand-correct
  glyphs via SF Symbols; visionOS spatial controllers mirroring hand
  input; keyboard bindings proximity and customization.

### Pending

- Technologies (batch 9): widgets-and-live-activities, dynamic-island,
  control-center, lock-screen, shortcuts-and-siri, app-clips,
  share-extension, carplay, homekit, imessage-apps, game-center,
  generative-ai.
- Remaining patterns (batch 10): entering-data, drag-and-drop,
  file-management, offering-help, managing-accounts, in-app-purchase,
  multitasking, printing, ratings-and-reviews, undo-and-redo,
  managing-notifications, charting-data, going-full-screen,
  live-viewing-apps, workouts.

## [0.6.0] — 2026-04-21

The remaining four platform files — iPadOS, watchOS, tvOS, visionOS — each
structured the same way as the deep iOS and macOS references: device traits,
navigation, input, layout, typography, color/materials, platform-specific
idioms, common mistakes, and a complete SwiftUI app example. Lighter depth
than iOS and macOS, per the project priorities, but each file stands alone
as a full platform reference.

### Added — Platforms

- `platforms/ipados.md` — resizable windows and Stage Manager / Split View /
  Slide Over; sidebar-adaptive tab bars; three-column `NavigationSplitView`;
  Multi-Touch, pointer, keyboard, trackpad, and Apple Pencil input model;
  Scribble and low-latency drawing; drag-and-drop as a first-class feature;
  menu bar commands when a hardware keyboard is attached; Mac Catalyst
  considerations; complete `NavigationSplitView` app example.
- `platforms/watchos.md` — glanceable design discipline; Digital Crown for
  scrolling and value tuning; the four surfaces (app, complications, Smart
  Stack, notifications); vertical `TabView` navigation; Always On display
  considerations; SF Compact typography; always-dark color model with
  tinted complications; haptic-first feedback and no indeterminate
  indicators; Action button on Ultra; double-tap gesture semantics;
  complete app with a Digital Crown slider and a WidgetKit complication.
- `platforms/tvos.md` — the focus engine as the navigation model;
  Siri Remote vocabulary; 60×80pt safe area; tab bar at 68pt top; layouts
  that do not adapt to screen size; multiuser profile handling; Top Shelf
  requirements; Picture-in-Picture cooperation; no pre-rounded image
  corners; 23pt minimum type; complete SwiftUI screen with a horizontal
  scroll grid using `.card` button style.
- `platforms/visionos.md` — the deepest of the four because visionOS has the
  most unique idioms. Covers windows vs volumes vs immersive spaces; the
  glass material (no Dark Mode); eyes + indirect pinch + direct touch input
  model; ornaments for off-window controls; 60pt target + spacing rules;
  bolder typography for glass; color sparingly; motion and comfort rules
  (no world rotation, avoid peripheral motion, fades over translations,
  0.2 Hz sensitivity); Spatial Audio as the feedback channel; SharePlay
  and spatial Personas; safety and age restrictions; complete app with a
  window group, detail windows, an `ImmersiveSpace`, and a bottom
  ornament.

### Pending

- Inputs (batch 8): touch-and-gestures, pointer-and-keyboard, apple-pencil,
  digital-crown, focus-and-remote, spatial-input, game-controllers.
- Technologies (batch 9): widgets-and-live-activities, dynamic-island,
  control-center, lock-screen, shortcuts-and-siri, app-clips,
  share-extension, carplay, homekit, imessage-apps, game-center,
  generative-ai.
- Remaining patterns (batch 10): entering-data, drag-and-drop,
  file-management, offering-help, managing-accounts, in-app-purchase,
  multitasking, printing, ratings-and-reviews, undo-and-redo,
  managing-notifications, charting-data, going-full-screen,
  live-viewing-apps, workouts.

## [0.5.0] — 2026-04-21

Remaining component references. Seven files covering the rest of the
component spine — the controls, containers, and system surfaces that weren't
in the 0.3.0 core set. Each is cited to the live HIG and follows the shared
template.

### Added — Components

- `components/collections-and-scrolling.md` — when collections (image-heavy)
  vs lists (text); nested scroll-view rules; scroll edge effects (soft vs
  hard); pull-to-refresh; visionOS Look to Scroll and scroll-indicator
  specifics; watchOS Digital Crown page indicator.
- `components/segmented-controls.md` — 5–7 segment cap (5 on iPhone),
  text-or-images-not-both, selection-vs-action rules, macOS tab-view
  preference for top-level switching, tvOS split-view-over-segmented
  guidance.
- `components/sliders-steppers-toggles.md` — slider direction conventions;
  the volume-view exception; stepper + text-field pairing; switch-vs-
  checkbox-vs-radio decision table; macOS mini switches in grouped forms;
  mixed-state checkboxes; never-rely-on-color-alone.
- `components/progress-and-activity.md` — determinate vs indeterminate;
  don't switch bar ↔ spinner mid-flight; pause + cancel semantics; gauges
  as state (not progress); macOS level indicators; the strict Activity
  rings rules (black background, no recoloring, never decorative).
- `components/labels-and-badges.md` — label is uneditable text; selectable
  diagnostic strings; the four semantic label colors; badge discipline
  (critical new only, hide at zero, no "new feature" abuse); tab bar,
  list row, app icon, Dynamic Island badge surfaces; iOS 17+ badge APIs.
- `components/split-views.md` — when to use 2 vs 3 columns; selection-
  highlight in every driving pane; macOS draggable thin-divider rule;
  iPadOS fluid-width handling (hide tertiary first); tvOS default 1/3 vs
  2/3 proportions; watchOS full-screen list-or-detail behavior;
  visionOS split-view-over-new-window preference.
- `components/system-experiences.md` — share sheet (activity view) rules;
  no-duplicate-system-activities; share / action extensions; Quick Look
  expectations; Spotlight indexing pointer; the macOS Services menu;
  `ShareLink`, `UIActivityViewController`, `NSSharingServicePicker` idioms.

### Pending

- Lighter platform files (batch 7): iPadOS, watchOS, tvOS, visionOS.
- Inputs (batch 8): touch-and-gestures, pointer-and-keyboard, apple-pencil,
  digital-crown, focus-and-remote, spatial-input, game-controllers.
- Technologies (batch 9): widgets-and-live-activities, dynamic-island,
  control-center, lock-screen, shortcuts-and-siri, app-clips,
  share-extension, carplay, homekit, imessage-apps, game-center,
  generative-ai.
- Remaining patterns (batch 10): entering-data, drag-and-drop,
  file-management, offering-help, managing-accounts, in-app-purchase,
  multitasking, printing, ratings-and-reviews, undo-and-redo,
  managing-notifications, charting-data, going-full-screen,
  live-viewing-apps, workouts.

## [0.4.0] — 2026-04-21

Core pattern references. Seven files that describe the decisions behind a
screen, not just the components on it. Each file documents when the pattern
applies, when it doesn't, the platform differences, and the writing rules for
the surfaces it covers. SwiftUI plus UIKit/AppKit snippets where they add
clarity.

### Added — Patterns

- `patterns/navigation.md` — the "one paradigm per platform per view" rule,
  the tab bar / stack / sidebar / menu bar / focus / ornament matrix, and
  decision guidance for per-screen navigation choices. Synthesis across HIG
  (no single HIG navigation page; cites composite sources).
- `patterns/modality.md` — when modality is correct, when it's wrong, a
  decision tree (alert / sheet / popover / full-screen / panel), platform
  rules for sheets and alerts, and writing guidance (pair Done with Cancel,
  confirm before discarding unsaved work).
- `patterns/settings.md` — smart defaults, minimizing count, three homes
  (global → app settings area; task-specific → in-task; system-level →
  system Settings app), macOS Settings scene conventions (⌘,, noncustomizable
  toolbar, dim minimize/zoom), iOS/iPadOS deep-link to system Settings,
  watchOS inline-only guidance.
- `patterns/search.md` — one search location, scope communication, suggestions
  and recent-query privacy, Spotlight integration, platform-specific search
  placement (search-role tab on iOS 18+, toolbar search on macOS, voice on
  tvOS), and writing for suggestions and results.
- `patterns/onboarding.md` — make it fast, fun, and optional; teach through
  interactivity; prefer contextual tips (TipKit) over a global tour; when to
  request permissions; splash-screen discipline; what not to put in
  onboarding (EULA, paywall, global setup).
- `patterns/loading.md` — show something immediately, don't block interactions
  you don't have to, match indicator kind to expected duration, skeleton and
  cached-content patterns, optimistic UI, writing while loading, watchOS's
  "no indeterminate indicators" rule.
- `patterns/feedback.md` — match significance to surface, multi-channel
  feedback as an accessibility baseline, haptics catalog (iOS / iPadOS /
  watchOS), SF Symbols animations as feedback, toast and inline-validation
  patterns, empty/error states with `ContentUnavailableView`, platform
  specifics including the "no haptics in visionOS" rule.

### Pending

- Remaining components (batch 6): collections-and-scrolling, segmented-controls,
  sliders-steppers-toggles, progress-and-activity, labels-and-badges,
  split-views, system-experiences.
- Lighter platform files (batch 7): iPadOS, watchOS, tvOS, visionOS.
- Inputs (batch 8): touch-and-gestures, pointer-and-keyboard, apple-pencil,
  digital-crown, focus-and-remote, spatial-input, game-controllers.
- Technologies (batch 9): widgets-and-live-activities, dynamic-island,
  control-center, lock-screen, shortcuts-and-siri, app-clips,
  share-extension, carplay, homekit, imessage-apps, game-center,
  generative-ai.
- Remaining patterns (batch 10 if warranted): entering-data, drag-and-drop,
  file-management, offering-help, managing-accounts, in-app-purchase,
  multitasking, printing, ratings-and-reviews, undo-and-redo,
  managing-notifications, charting-data, going-full-screen,
  live-viewing-apps, workouts.

## [0.3.0] — 2026-04-20

Core component references. Six files covering the bars, buttons, lists/tables,
sheets/popovers/alerts, text inputs, and pickers/menus that form the backbone
of any Apple-platform UI. Each file is cited to the live HIG and includes
SwiftUI examples plus UIKit and AppKit equivalents for iOS and macOS.

### Added — Components

- `components/bars.md` — tab bar / navigation bar / toolbar / sidebar in one
  file. Covers which bar belongs where, tab-bar rules (2–5 items,
  Liquid Glass, platform placement), toolbar anatomy (leading / center /
  trailing groupings; overflow is system-managed), and sidebar rules
  (max two levels of hierarchy, extend content behind). Synthesizes the
  2025 consolidation of "navigation bars" into toolbars.
- `components/buttons.md` — style / content / role triad; hit-target floors
  per platform; primary vs destructive vs cancel roles; macOS button types
  (push, flexible-height, square/gradient, help, image) with placement
  guidance; visionOS shape + size rules (circular / capsule / rounded-rect,
  ≥60pt between centers); watchOS full-width idioms.
- `components/lists-and-tables.md` — when to pick list vs table vs outline
  vs collection; row feedback for navigational vs option lists; all the
  SwiftUI list styles; macOS sortable multi-column tables and outline
  views; tvOS focus padding; watchOS elliptical style and vertical
  page-based nav constraints.
- `components/sheets-and-popovers.md` — the "which presentation" decision
  table; sheet rules and detents (`medium` vs `large`, grabber,
  swipe-to-dismiss); popover rules (no cascades, no warnings, detachable
  on macOS); alert anatomy and copy rules; action sheet / confirmation
  dialog usage; platform-specific placement and button rules including
  visionOS accessory-view 154pt max height.
- `components/text-inputs.md` — placeholder ≠ label; match field size to
  content; keyboard types and content-type hints for AutoFill, strong
  passwords, and one-time codes; validation timing; secure fields; macOS
  combo boxes; tvOS/watchOS minimize-text-entry guidance.
- `components/pickers-and-menus.md` — the "picker vs menu vs pull-down vs
  pop-up" decision table; menu label/icon/organization rules; submenu
  discipline (one level, ≤5 items); toggled items with checkmarks; pickers
  and date pickers with compact/inline/wheels/graphical styles; iOS small /
  medium / large menu layouts; macOS menu-bar conventions and
  title-case labels.

### Pending

- Core patterns (batch 5): navigation, modality, settings, search,
  onboarding, loading, feedback.
- Remaining components: collections-and-scrolling, alerts (standalone),
  segmented-controls, sliders-steppers-toggles, progress-and-activity,
  labels-and-badges, split-views, system-experiences.
- Lighter platform files: iPadOS, watchOS, tvOS, visionOS.

## [0.2.0] — 2026-04-20

Foundations complete + deep platform references for iOS and macOS. Every file
cites its source on developer.apple.com and follows the shared template
(What / When / Platform differences / Sizing / Accessibility / Common mistakes
/ SwiftUI / UIKit or AppKit / See also).

### Added — Foundations (8 files)

- `foundations/accessibility.md` — hit-target and Dynamic Type tables per
  platform, WCAG contrast minimums, VoiceOver rules, Reduce Motion
  alternatives, Voice Control / Full Keyboard Access / Switch Control /
  Assistive Access.
- `foundations/typography.md` — SF Pro / SF Compact / New York, the text-style
  hierarchy, default and minimum point sizes per platform, macOS built-in text
  styles, tvOS built-in text styles, Dynamic Type rules, custom-font
  requirements.
- `foundations/color.md` — 12 system colors, 6 system grays, iOS and macOS
  semantic color catalogs, Liquid Glass tinting guidance, inclusive-color
  rules, sRGB vs Display P3 management.
- `foundations/dark-mode.md` — base vs elevated backgrounds on iOS/iPadOS,
  desktop tinting on macOS, contrast floors in dark appearance, asset-catalog
  practice, why an app-specific appearance toggle is wrong.
- `foundations/layout.md` — hierarchy, adaptability, safe areas and guides,
  per-platform layout rules, iPhone / iPad / Apple Watch dimension tables, and
  iOS / iPadOS size classes.
- `foundations/sf-symbols.md` — rendering modes, gradients, variable color,
  weights and scales, design variants, the full animation vocabulary, custom
  symbols and annotation.
- `foundations/materials.md` — Liquid Glass regular vs clear variants,
  standard materials (ultra-thin / thin / regular / thick), vibrancy levels,
  per-platform differences including the visionOS glass material.
- `foundations/motion.md` — purposeful motion, Reduce Motion alternatives,
  visionOS comfort rules (peripheral motion, 0.2 Hz oscillation, no
  world-rotation).

### Added — Platforms (2 deep files)

- `platforms/ios.md` — device traits, system features to integrate (Dynamic
  Island, Live Activities, widgets, Action button, App Clips, Shortcuts),
  navigation paradigm, layout rules, typography, color and materials, touch
  and gestures, feedback via haptics, modality used sparingly, writing style,
  common mistakes, a full SwiftUI inbox screen, and a UIKit equivalent.
- `platforms/macos.md` — device traits, the big Mac idioms (menu bar is
  authoritative), windowing rules, menu bar / toolbar / sidebar / split view
  structure, keyboard and pointer expectations, layout rules, typography
  (with the correct point sizes for macOS — no Dynamic Type), color and
  materials, Settings scene, writing conventions, Mac Catalyst notes, common
  mistakes, a full SwiftUI three-column app with menu commands and a Settings
  scene, and an AppKit sketch.

### Pending

- Core components: bars, buttons, lists-and-tables, sheets-and-popovers,
  text-inputs, pickers-and-menus (batch 4).
- Core patterns: navigation, modality, settings, search, onboarding, loading,
  feedback (batch 5).
- Lighter platform files: iPadOS, watchOS, tvOS, visionOS (batch 8).
- Checklists refresh, technologies, inputs.

## [0.1.0] — 2026-04-20

Initial scaffold. Skill triggers, structure, and install flow are in place.
Reference file content is being authored from the live HIG.

### Added

- `SKILL.md` with trigger-heavy frontmatter, Phase 0 question flow, decision
  tree, core non-negotiables, common mistakes, and final review checklist.
- Reference directory tree:
  `references/{foundations,patterns,components,inputs,platforms,technologies}`.
- Checklist stubs under `assets/checklists/`.
- `setup` script (idempotent, no user-config edits by default).
- `bin/apple-hig-uninstall`.
- MIT license.
- GitHub Actions workflow scaffold (manual trigger only at this stage).

### Pending

- Authoring of ~60 reference files against the live HIG.
- Depth pass on iOS and macOS (SwiftUI + UIKit/AppKit snippets).
- Populated pre-submission and accessibility review checklists.
