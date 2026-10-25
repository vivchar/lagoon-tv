# Platform parity review checklist

> Run when shipping the same feature across multiple Apple platforms. The goal
> is each version feeling native to its platform — not pixel-identical.

## Navigation

- [ ] iOS: tab bar or `NavigationStack`.
- [ ] iPadOS: `NavigationSplitView` for regular size class, `NavigationStack`
      for compact.
- [ ] macOS: menu bar + toolbar + sidebar. No tab bar.
- [ ] watchOS: page or hierarchical navigation. No tab bar emulation.
- [ ] tvOS: focus-driven, top-level groups as full-screen collections.
- [ ] visionOS: ornaments for tool palettes, tabs for window-level sections.

## Input

- [ ] Touch hit targets on iOS / iPadOS / watchOS.
- [ ] Pointer hover states on iPadOS and macOS.
- [ ] Keyboard shortcuts on iPadOS and macOS (menu commands registered).
- [ ] Focus visuals on tvOS.
- [ ] Pinch + gaze on visionOS.

## Chrome

- [ ] iOS: respect home indicator and status bar.
- [ ] macOS: window min/max, full-screen support, restore on launch.
- [ ] iPadOS: Stage Manager & multitasking windowing works.
- [ ] watchOS: Digital Crown and side button wired where relevant.
- [ ] visionOS: window glass material, resize handles, close chrome.

## Writing

- [ ] "Tap" on touch, "Click" on macOS, "Select" where platform-neutral.
- [ ] Menu bar text follows macOS title-case convention.
- [ ] iOS / iPadOS button labels use sentence case.

## Features that only make sense on one platform

- [ ] Hidden or adapted on platforms where they don't belong (e.g., Apple
      Pencil features hidden on iPhone, widget configuration surfaced
      appropriately per platform).
