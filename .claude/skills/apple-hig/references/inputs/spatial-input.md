# Spatial input (eyes + hands on visionOS)

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/eyes
**Last verified:** 2026-04-21
**Applies to:** visionOS (Apple Vision Pro)
**Not supported:** iOS · iPadOS · macOS · tvOS · watchOS

On Vision Pro, the user **looks at things to target them, then pinches to activate**. It's indirect, low-fatigue, and happens mostly with hands resting in the lap. Getting this right is the difference between "I can wear this for an hour" and "my neck hurts."

## The default interaction model

1. **Look** at an element.
2. **Pinch** (thumb + index) to activate.

That's 90% of interaction on visionOS. Design as if this is the only input — then add keyboard, trackpad, and direct touch where they help.

## Eyes — hover effect (not focus)

When the user looks at an interactive element, visionOS shows a **hover effect** — a subtle highlight confirming the element is targetable.

- **Hover effect ≠ focus.** Focus is for connected input devices (keyboard, game controller). See [`./focus-and-remote.md`](./focus-and-remote.md#visionos-focus).
- **Standard components already support hover automatically.** Buttons, rows, tabs, chips — they all highlight on look.
- **Some components expand on look.** Tab bars reveal text labels next to icons. Buttons can reveal tooltips.

### Privacy

visionOS **doesn't tell your app** where the user is looking before they pinch. The system reports taps (pinch = tap) after they happen. This preserves gaze privacy. Don't design as if you can predict what the user will tap.

## Making items easy to see

### Minimize visual distraction

- **Visual noise drowns targets.** Reduce surrounding motion, simplify backgrounds, give content room.
- **Movement in peripheral vision grabs the eye involuntarily.** Revealing new content near a button the user is looking at can yank their gaze away and break the interaction.

### Space between items

Eyes naturally make small adjustments in direction even while looking at one place. Crowded UI means the user's gaze jumps between nearby items.

- **Margin ≥ 16pt** around each interactive item, **or** centers ≥ 60pt apart. The latter is the stricter rule; follow both when you can.
- **Avoid repeating patterns or textures** that fill the field of view. Some patterns trick the eye into locking onto different layers at different depths.

### Rounded shapes

Corners attract the eye (because of contrast and angles). Circle or capsule shapes let the eye rest on the center; sharp-cornered rectangles don't.

- **Prefer circular or capsule-shaped interactive items.**
- **For multi-element interactive components** (image + label combined), wrap in a container shape the system can highlight as one unit. Otherwise gaze targeting the label alone won't highlight the image.

## Encouraging interaction

Subtle visual cues steer gaze.

- **Placement in the center** of the field of view.
- **Gentle motion** to draw attention.
- **Increased contrast** or color variation.
- **Scale differences.**

Prefer noticeable-but-not-flashy. Harsh motion or a strobing element drives discomfort.

## Hands — indirect and direct gestures

See [`./touch-and-gestures.md`](./touch-and-gestures.md#visionos) for the full gesture catalog. Summary:

### Indirect (default, ergonomic)

Hands in lap or at sides; pinch targets the element the user is looking at.

- Pinch (tap)
- Pinch + hold (context menu / start drag)
- Pinch + drag (move, scroll)
- Two-hand pinch + spread/together (zoom)
- Two-hand pinch + circular (rotate)

### Direct (reach and touch)

Hands reach out and physically touch virtual content. Fatiguing over time.

- **Close-up interaction** — keyboard typing, game objects within reach, inspecting 3D content.
- **Not for chrome** — standard buttons, menus, and navigation use indirect.

### Offer both when possible

- **Prefer indirect for standard UI.**
- **Reserve direct for things that invite touching** — 3D models, close-up games, in-reach controls.
- **Support standard gestures everywhere.** Tap is the first thing the user tries.

## Custom hover effects

You can design a custom hover effect that animates when the user looks at an element — with strong constraints.

### How they work

- You define **two states** for the element: default + hover-active.
- The **system applies the hover effect out-of-process.** Your app doesn't know when the user is looking.
- **Your code can't run during hover** — only the predefined visual change happens.
- You **can't perform actions** (like adding to favorites) on hover — there's no event.

### Rules

- **Reserve for special moments.** Overuse dilutes impact and can cause discomfort.
- **Delay timing matters:**
  - **No delay** (default) — subtle effects, interaction invites (slider knob appearing).
  - **Short delay** — quick-to-interact-with elements (tab labels expanding).
  - **Long delay** — supplementary info (tooltip below a button; most people won't read it every time).
- **Keep at least one primary view unchanged** between the two states. Full-view-changes disorient.
- **Test in the headset.** Simulator doesn't replicate eye behavior.

## Comfort rules

Apply on every interaction:

- **Place content in the comfortable field of view.** Center, not peripheral.
- **Viewing distance ≥ 1 meter** for content the user engages with for more than a moment.
- **Don't require large or repetitive gestures.** Arms-raised for long = fatigue.
- **Don't require a specific hand.** Left-only or right-only excludes users.
- **Don't require specific body movement.** Disability, environment, or simply "I'm on a couch" constraints.
- **No sudden large visual changes** — especially near the edges of vision.

## Accessibility

Vision Pro supports VoiceOver, Switch Control, Dwell Control, Guided Access, Head Pointer, Zoom, and more.

- **Standard controls are accessible by default.** Don't break them.
- **Don't anchor UI to the user's head** — blocks Head Pointer and makes users feel trapped.
- **Dwell Control** activates by gaze-and-wait. Works with standard controls; custom components need `accessibilityAction` and proper traits.
- **Test with accessibility settings on.** Your hover and motion behaviors may need to back off when Reduce Motion is active.

## Platform considerations

Spatial input (eyes + hands as designed here) is unique to visionOS. No equivalent on other Apple platforms.

## Common mistakes

- **Crowded UI** with items <60pt apart, center-to-center.
- **Peripheral motion** that yanks gaze.
- **Custom controls without a container shape** — gaze highlights the wrong sub-element.
- **Head-anchored UI** (blocks Head Pointer; feels constraining).
- **Relying on direct touch** for standard chrome — fatiguing.
- **Custom hover effect for a common control** — dilutes the system design language.
- **Hover effect that tries to perform an action** — can't; the system doesn't tell you when the user is looking.
- **Sharp-cornered rectangles for primary buttons** — eye drifts to the corners.
- **Repeating patterns that fill the field of view** — depth perception glitches.
- **Requiring both hands** or one specific hand.
- **Small targets** (<60pt) that the eye can't settle on comfortably.
- **Assuming you know where the user is looking.** You don't — by design.

## SwiftUI

```swift
// Standard hover effect on a custom card.
ArtworkCard(artwork: art)
    .hoverEffect()                              // system provides the effect

// Named hover effects with custom animations (visionOS).
GalleryItem(item: item)
    .hoverEffect { effect, isActive, proxy in
        effect
            .animation(.spring(duration: 0.2)) { c in
                c.scaleEffect(isActive ? 1.04 : 1)
            }
    }

// Group children into a single hoverable region.
HStack {
    AsyncImage(url: art.thumbnailURL) { img in img.resizable() }
        placeholder: { Color.clear }
    VStack(alignment: .leading) {
        Text(art.title).font(.headline)
        Text(art.artist).font(.subheadline).foregroundStyle(.secondary)
    }
}
.padding()
.hoverEffect(.highlight)                        // one hover target for both children
.contentShape(.hoverEffect, RoundedRectangle(cornerRadius: 16))

// Rounded target shape for easier eye landing.
Button { favorite() } label: {
    Image(systemName: "heart")
        .font(.title)
}
.buttonStyle(.bordered)
.clipShape(.capsule)                            // capsule, not sharp rect
.frame(minWidth: 60, minHeight: 60)             // 60pt minimum

// Adequate spacing between buttons.
HStack(spacing: 24) {                           // at least 60pt center-to-center
    Button("Previous") { prev() }.buttonStyle(.bordered)
    Button("Next")     { next() }.buttonStyle(.borderedProminent)
}

// Place content near the center of the window.
VStack(spacing: 32) {
    Spacer()
    HeroView()
    ActionRow()
    Spacer()
}
.frame(maxWidth: .infinity, maxHeight: .infinity)
```

## RealityKit

```swift
// Enable hover effect on a RealityKit entity for indirect gaze.
entity.components.set(HoverEffectComponent())

// Input component so the entity can receive pinch gestures.
entity.components.set(InputTargetComponent())
entity.components.set(CollisionComponent(shapes: [.generateBox(size: .one)]))
```

## See also

- [`./touch-and-gestures.md`](./touch-and-gestures.md#visionos) — full gesture catalog.
- [`./focus-and-remote.md`](./focus-and-remote.md#visionos) — keyboard/game-controller focus on visionOS.
- [`./pointer-and-keyboard.md`](./pointer-and-keyboard.md#visionos-pointer) — paired peripherals.
- [`../platforms/visionos.md`](../platforms/visionos.md) — platform idioms.
- [`../foundations/materials.md`](../foundations/materials.md#visionos) — glass material adapts behind gaze.
- [`../foundations/motion.md`](../foundations/motion.md#visionos) — comfort-first motion.
- [`../components/bars.md`](../components/bars.md) — vertical tab bar reveals labels on look.
