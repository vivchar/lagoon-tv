# Accessibility review checklist

> Minimum bar. Every screen must pass these before shipping.

## Touch / focus

- [ ] iOS / iPadOS: every tap target ≥ 44×44pt.
- [ ] watchOS: every tap target ≥ 44×44pt on 44mm+, ≥ 40pt on smaller.
- [ ] macOS: every pointer target ≥ 28×28pt.
- [ ] tvOS: every actionable view is focusable and shows focus effect.

## Dynamic Type

- [ ] Body text scales with user's preferred size without clipping.
- [ ] Layouts reflow or scroll at largest accessibility sizes (AX1–AX5).
- [ ] No critical UI hidden behind a fixed-height constraint.

## Color & contrast

- [ ] Text ≥ 4.5:1 contrast (or 3:1 for large text) against its background.
- [ ] Does not rely on color alone to convey meaning (adds icon, label, or
      pattern).
- [ ] Verified in Increase Contrast mode.

## VoiceOver

- [ ] Every non-decorative view has `.accessibilityLabel(...)` (or correct
      inferred label).
- [ ] Actionable views expose `.accessibilityHint(...)` when the action is
      non-obvious.
- [ ] Decorative images use `.accessibilityHidden(true)`.
- [ ] Groups use `.accessibilityElement(children: .combine)` where the group
      reads better as one element.
- [ ] Reading order is logical (`.accessibilitySortPriority` if needed).

## Motion

- [ ] Reduce Motion path exists for any non-trivial animation (spring,
      parallax, scene transitions).
- [ ] Auto-playing video / animation can be paused.

## Input

- [ ] Keyboard navigation works on iPadOS and macOS.
- [ ] Full Keyboard Access works on iOS.
- [ ] Voice Control labels match visible labels.
- [ ] Switch Control reachable.

## Content

- [ ] All form fields have labels (not just placeholders).
- [ ] Error messages announced via VoiceOver when they appear.
- [ ] Time-limited interactions can be extended.
