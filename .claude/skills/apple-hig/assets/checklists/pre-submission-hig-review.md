# Pre-submission HIG review checklist

> Run before calling any UI design or implementation complete. Every item must
> pass or be explicitly waived with a one-line reason.

## Foundations

- [ ] Typography: text uses Dynamic Type text styles (`.body`, `.headline`, …)
      or `UIFontMetrics` — no hardcoded body point sizes.
- [ ] Color: semantic colors or asset catalog colors only. No hex literals in
      production UI.
- [ ] Dark Mode: every screen verified in light and dark.
- [ ] SF Symbols used for every glyph where a system equivalent exists.
- [ ] Layout respects safe areas and system margins.
- [ ] Motion respects `@Environment(\.accessibilityReduceMotion)`.

## Platform fit

- [ ] Navigation paradigm matches the target platform.
- [ ] Window chrome / toolbar / menu bar handled per platform (macOS).
- [ ] Tab bar only used where appropriate (iOS, iPadOS compact).
- [ ] Sidebar / split view used for iPadOS regular size class and macOS where
      appropriate.
- [ ] Focus engine wired for tvOS; ornaments / glass used for visionOS.

## Components

- [ ] Buttons use the correct role (`.destructive`, `.cancel`, default) and
      style for context.
- [ ] Modals only where modality is justified (see `patterns/modality.md`).
- [ ] Alerts reserved for user-actionable errors; inline feedback otherwise.
- [ ] Lists use appropriate list style; swipe actions where idiomatic.

## Accessibility

- See `accessibility-review.md` for the full checklist.

## Writing

- [ ] Tone matches platform: "Tap" on touch, "Click" on macOS.
- [ ] Sentence case in buttons and labels.
- [ ] No destructive actions without confirmation copy.

## Citation

- [ ] If the deliverable is a design doc, every non-obvious rule cites the
      matching HIG URL.
