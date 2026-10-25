# visionOS

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/designing-for-visionos
**Last verified:** 2026-04-21
**Applies to:** Apple Vision Pro (visionOS)

visionOS puts your app inside a room. Users look, pinch, and speak; your content lives in 3D space alongside the physical world. This is **not** an iOS app bigger, or a Mac app floating. It's a platform where **comfort is a design principle** — get this wrong and users take the headset off.

## Table of contents

- [What makes visionOS visionOS](#what-makes-visionos-visionos)
- [Device traits](#device-traits)
- [Scene types](#scene-types)
- [Immersion styles](#immersion-styles)
- [Glass material](#glass-material)
- [Input — eyes, hands, voice](#input--eyes-hands-voice)
- [Ornaments](#ornaments)
- [Layout and depth](#layout-and-depth)
- [Typography](#typography)
- [Color](#color)
- [Motion and comfort](#motion-and-comfort)
- [Audio and Spatial Audio](#audio-and-spatial-audio)
- [SharePlay and Personas](#shareplay-and-personas)
- [Accessibility](#accessibility)
- [Safety](#safety)
- [Common mistakes](#common-mistakes)
- [SwiftUI — a visionOS app](#swiftui--a-visionos-app)
- [See also](#see-also)

## What makes visionOS visionOS

- **Your content shares space with the physical world.** Windows float, volumes sit on surfaces, immersive scenes surround the user.
- **Look to target, pinch to activate.** The default input model. No cursor.
- **Comfort is mandatory.** Motion sickness, eye strain, neck strain are design failures, not edge cases.
- **No haptics.** Audio is the feedback channel — use standard controls to get system sounds.
- **No Dark Mode.** The glass material adapts to the luminance behind it.
- **Passthrough** — real-world video from the device's cameras. Users can make more or less visible via the Digital Crown.
- **Multi-tasking in the Shared Space** — multiple apps run side by side, user arranges freely.

## Device traits

- **Display.** Two micro-OLED displays with 23M pixels total — each eye sees UI rendered to match perspective. Effective "screen size" is the user's field of view.
- **Ergonomics.** The system positions content relative to the user's head, so height, sitting/standing/reclining don't matter. Hands stay near the lap; arms don't need to reach.
- **Inputs:** eyes (gaze), hands (pinch, tap, direct touch), voice (Siri, dictation), external keyboard + trackpad + Magic Mouse + Bluetooth game controller, Apple Magic Keyboard for iPad.
- **Session length.** Typically 10–40 minutes before the user takes a break.
- **System features:** Shared Space, Full Space, Environments, Spatial Audio, passthrough, Digital Crown, Top button (capture), Siri, Focus modes, Control Center, Apple ID + Optic ID, Mac Virtual Display.

## Scene types

Three kinds of scene your app can present:

### Window

A **2D plane floating in space** with rounded corners. The standard container for UI. Window controls (close, resize, move) live just outside the window bounds.

- **Stretch/resize** via the corner grabbers.
- **Reposition** by dragging the window chrome below it.
- **Close** via the close button below it.
- Windows have a depth — content can extend along z within the window, but too far along z causes clipping.

### Volume

A **3D bounded container** for spatial content (models, scenes). Sits in the user's space; users can walk around it. Content has real depth that renders volumetrically.

- Used sparingly — "3D object I want to inspect" kind of content.
- Still a bounded container — not immersive.

### Immersive Space

A **fully immersive scene** that takes over the user's view (partially or fully). The user dials immersion via the Digital Crown.

- **Mixed immersion** — your content blended with passthrough.
- **Progressive immersion** — user can widen or narrow via the Crown.
- **Full immersion** — you replace the environment.
- Only **one Full Space at a time.** Shared Space (windows and volumes of many apps) pauses.

## Immersion styles

For each key moment in your app, pick the **minimum** level of immersion that works. Not every moment needs to be fully immersive.

| Style | When |
|---|---|
| **Window-only** | Standard productivity, reading, chat, dashboards. Lightweight. |
| **Window + volume** | Browse in the window, inspect 3D content in the volume. |
| **Shared Space window + occasional Full Space** | Your app lives in the Shared Space, but users can enter a Full Space for a focused experience (e.g., a museum tour). |
| **Full Space** | Immersive games, focused media, meditation, training scenarios. |

Let users **transition between modes** on purpose — don't force them into full immersion to do routine tasks.

## Glass material

visionOS windows use an unmodifiable system material called **glass**. It lets light from the room + virtual content show through, grounding the user in their space.

### Rules

- **No Dark Mode.** Glass adapts to the luminance of what's behind it — the room, other windows, virtual content. Design for that, not for a light/dark theme toggle.
- **Prefer translucency to opaque fills** in windows. Opaque blocks the environment and feels constricting.
- **Use system materials for custom components** when needed — see [`../foundations/materials.md`](../foundations/materials.md#visionos).
  - `.elevated` → interactive elements (buttons, selected items).
  - `.grouped` → visual separation (sidebar sections, grouped table view).
  - Dark-on-grouped → dark element on top of a grouped background.
- **Vibrancy for text and symbols** to preserve contrast against whatever is behind the window:
  - Standard — body text.
  - Vibrant — footnotes, subtitles.
  - Inactive — disabled states only.

## Input — eyes, hands, voice

### Eyes

- **Gaze targets interactive items.** When the user looks at a button, it highlights subtly. When they pinch, it activates.
- **Do not anchor content to the head.** It follows the user around and prevents assistive tech like Pointer Control.
- **Don't put UI in peripheral vision.** Central field of view is where things read; peripheral motion causes discomfort.
- **Minimum 60×60pt targets.** Hit area must be large enough for the eye to comfortably settle on.
- **≥60pt center-to-center between buttons** so gaze + hover effects don't collide.

### Hands

- **Indirect pinch** — hands resting in lap or at sides. The user pinches to activate what they're looking at. This is the default — design for it.
- **Direct touch** — user reaches out and touches the window with a fingertip. Only use for content **close enough to reach** — if it's far, direct touch is impractical.
- **Drag** — pinch-and-hold then move. Drag handles follow the hand.
- **Don't require large or repetitive gestures.** Fatigue is real. Keep hands low.

### Voice

- **Siri and dictation are first-class.** Many Vision Pro users prefer voice for text entry.
- **Support the system's Look to Dictate** for text fields.

### Keyboard, trackpad, mouse, game controller

- Bluetooth peripherals pair easily. When paired, users expect standard shortcuts and behaviors.
- Design for all common pair combinations — Vision Pro + Magic Keyboard is a common setup.

## Ornaments

An **ornament** is a UI element anchored to a window that renders *outside* the window's bounds. Toolbars, tab bars, inspectors often live as ornaments.

### Rules

- **Ornaments don't consume window space.** The inside of the window stays clear for content.
- **Tab bar is a vertical ornament** on the leading side. Labels reveal on gaze.
- **Toolbar is a horizontal ornament** at the bottom (above the window controls). Uses a variable blur to keep content legible.
- **Don't obscure system window chrome** (close/resize/move controls below the window).
- **Don't use pull-down menus in a visionOS toolbar** — they overlap the window chrome below.

## Layout and depth

### Window content

- **Center important content and controls.** Users can look anywhere, but center is the comfort zone.
- **Keep window content within bounds.** System window controls live just outside — cramming content there makes them hard to hit.
- **Keep interactive components apart.** ≥60pt center-to-center.
- **If a control must live outside the window, use an ornament.**

### Depth

- **Content extends along z** when you add depth — but excess clipping happens if it extends too far.
- **Small amounts of z work best.** A card that lifts 2–5pt on hover is elegant. A card that leaps 100pt is disorienting.
- **Don't animate z changes aggressively** — forward/backward motion in depth is the most nauseating kind.

See [`../foundations/spatial-layout.md`](../foundations/spatial-layout.md) (pending) for deeper guidance.

## Typography

- **SF Pro** is the system font. NY available with explicit type styles.
- **Default body 17pt, minimum 12pt.**
- **visionOS uses bolder Body and Title styles** than other platforms so text reads through the glass material.
- **Two extra styles** — Extra Large Title 1 and 2 — for editorial-style layouts.
- **Prefer 2D text.** Depth on characters makes them harder to read. Small amounts of 3D for accent only.
- **Billboarding** — for text anchored to a 3D object, rotate so it faces the user from any angle.
- **Contrast against the glass matters.** Default white text is the safe choice; test any other color in multiple environments.

See [`../foundations/typography.md`](../foundations/typography.md#visionos).

## Color

- **Sparingly on glass.** Room + virtual content shows through; colorful small text struggles.
- **Bold and large areas** are where color lands well.
- **In full immersion, keep brightness balanced.** Bright objects on dark backgrounds can fatigue or startle. Gentle transitions.
- **No Dark Mode setting.** See glass material above.

See [`../foundations/color.md`](../foundations/color.md#visionos).

## Motion and comfort

**Comfort is mandatory on visionOS.** The user's whole visual field is affected; motion sickness, peripheral-motion discomfort, and neck strain are real.

### Rules

- **Avoid motion in peripheral vision.** Distracting; can make the user feel they're moving.
- **Large virtual objects moving** → increase translucency or lower contrast so motion reads as subtle.
- **Relocate objects with fades**, not translations. Fade out, move, fade in.
- **Never rotate the virtual world.** Even slight self-controlled rotation upsets balance. Use instant cuts with fades instead.
- **Give users a stationary frame of reference** in immersive scenes — a fixed horizon, a stable window.
- **Avoid sustained oscillations near 0.2 Hz.** Humans are sensitive to that frequency.
- **Respect Reduce Motion** — swap transitions for fades.

See [`../foundations/motion.md`](../foundations/motion.md#visionos).

## Audio and Spatial Audio

Audio is the **primary feedback channel** on Vision Pro (no haptics).

- **Standard controls play system sounds.** Use them to get audible feedback for free.
- **Spatial Audio** places sound in 3D space. Use it for immersive content, navigation hints, object-anchored audio.
- **Tune for the user's environment** via the `SurroundingsEffect` API — sound can feel like it belongs in the room.
- **Don't over-audio.** Every interaction making a sound becomes fatigue. Use audio for confirmation, alerting, ambient — not as decoration.

## SharePlay and Personas

- **Shared activities across FaceTime.** When users SharePlay your app, each participant sees the others as spatial Personas — floating representations of the real person.
- **Design for co-viewing.** Synchronize state (playback, selections, cursors) so everyone is in the same context.
- **Permission-respectful.** Explain what gets shared before sharing.

## Accessibility

Vision Pro supports VoiceOver, Switch Control, Dwell Control, Guided Access, Head Pointer, Zoom, and more.

- **Standard controls support these by default.** Your job is to not break them.
- **Pointer Control** — users with mobility limitations navigate via head movement or hands with reduced capability. Don't anchor UI to the head (blocks Head Pointer).
- **Dwell Control** — users activate by looking at something for a moment. Standard controls work; custom controls need appropriate accessibility traits.
- **High-contrast and Increase Contrast** — test glass rendering with these on.
- **Don't assume eyes work.** Support full keyboard / game controller / voice input paths.

## Safety

- **Vision Pro is not for operating vehicles, heavy machinery, or moving through hazards** (balconies, stairs, streets). Your app design must not encourage unsafe use.
- **Age restriction:** Vision Pro is for users 13+.
- **Photosensitivity** — avoid strobing, rapid flashes, high-contrast rapid changes.

## Common mistakes

- Head-anchored UI (blocks assistive tech, feels trapped).
- Content or controls in peripheral vision.
- Vertical toolbar (toolbars are horizontal; tab bars are vertical).
- Pull-down menu in a toolbar (obscures window chrome below).
- Sheet emerging from the bottom edge.
- Dense colorful UI over glass without contrast testing.
- Large motion that translates objects across the user's view.
- Rotating the virtual world, even subtly.
- Forcing users into Full Space for routine tasks.
- Fully opaque windows.
- Content within the window-chrome zone.
- Very small buttons (<60pt) or too-close buttons (<60pt apart).
- Designing assuming Dark Mode exists.
- Haptic-oriented feedback expectations (there are no haptics).
- Light or thin font weights (hard to read through glass).
- Aggressive z-axis animation (forward/backward motion is nausea territory).
- Custom hover effects (the system provides them; don't override).

## SwiftUI — a visionOS app

```swift
import SwiftUI
import RealityKit

@main
struct GalleryApp: App {
    @State private var model = GalleryModel()

    var body: some Scene {
        WindowGroup(id: "browse") {
            RootView()
                .environment(model)
        }
        .defaultSize(width: 1100, height: 700)

        WindowGroup(id: "piece", for: Artwork.ID.self) { $id in
            if let id, let art = model.artwork(id) {
                ArtworkDetailView(artwork: art)
            }
        }
        .defaultSize(width: 700, height: 500)

        ImmersiveSpace(id: "exhibit") {
            ExhibitImmersiveView()
        }
        .immersionStyle(selection: .constant(.progressive), in: .progressive)
    }
}

struct RootView: View {
    @Environment(GalleryModel.self) private var model
    @Environment(\.openWindow) private var openWindow
    @Environment(\.openImmersiveSpace) private var openImmersive
    @Environment(\.dismissImmersiveSpace) private var dismissImmersive
    @State private var selectedSection: Section.ID?
    @State private var inExhibit = false

    var body: some View {
        NavigationSplitView {
            List(selection: $selectedSection) {
                ForEach(model.sections) { section in
                    Label(section.name, systemImage: section.systemImage).tag(section.id)
                }
            }
            .navigationTitle("Gallery")
        } detail: {
            if let id = selectedSection, let section = model.section(id) {
                ScrollView {
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 200), spacing: 24)],
                              spacing: 24) {
                        ForEach(section.artworks) { art in
                            Button {
                                openWindow(id: "piece", value: art.id)
                            } label: {
                                ArtworkCard(artwork: art)
                            }
                            .buttonStyle(.plain)
                            .hoverEffect(.highlight)
                        }
                    }
                    .padding(24)
                }
                .navigationTitle(section.name)
            } else {
                ContentUnavailableView("Pick a section", systemImage: "photo.on.rectangle")
            }
        }
        .ornament(attachmentAnchor: .scene(.bottom)) {
            HStack(spacing: 16) {
                Button {
                    Task {
                        if inExhibit {
                            await dismissImmersive()
                            inExhibit = false
                        } else {
                            _ = await openImmersive(id: "exhibit")
                            inExhibit = true
                        }
                    }
                } label: {
                    Label(inExhibit ? "Exit Exhibit" : "Enter Exhibit",
                          systemImage: inExhibit ? "xmark" : "sparkles")
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
            }
            .padding()
            .glassBackgroundEffect()
        }
    }
}

struct ArtworkDetailView: View {
    let artwork: Artwork

    var body: some View {
        VStack(spacing: 16) {
            Model3D(named: artwork.usdzName)
                .frame(depth: 200)                              // small z; resist nausea
            Text(artwork.title).font(.title)
            Text(artwork.artist).font(.headline).foregroundStyle(.secondary)
            Text(artwork.caption).font(.body).foregroundStyle(.secondary)
        }
        .padding(24)
    }
}
```

What this demonstrates: a multi-window app with window groups, a progressive Immersive Space, a bottom ornament containing a primary action, `NavigationSplitView` with a sidebar and detail, `.hoverEffect` for gaze, and `Model3D` with a conservative `depth:` so the 3D object doesn't push too aggressively along z.

## See also

- [`../foundations/accessibility.md`](../foundations/accessibility.md) — Pointer Control and comfort rules.
- [`../foundations/materials.md`](../foundations/materials.md#visionos) — glass material and vibrancy.
- [`../foundations/motion.md`](../foundations/motion.md#visionos) — comfort-first motion.
- [`../foundations/typography.md`](../foundations/typography.md#visionos) — bolder styles for glass.
- [`../foundations/color.md`](../foundations/color.md#visionos) — sparing color on glass.
- [`../foundations/spatial-layout.md`](../foundations/spatial-layout.md) (pending) — windows, volumes, immersive spaces.
- [`../components/bars.md`](../components/bars.md) — vertical tab bar, bottom toolbar ornament.
- [`../components/split-views.md`](../components/split-views.md#visionos) — split-view-over-new-window.
- [`../components/sheets-and-popovers.md`](../components/sheets-and-popovers.md) — center-in-view sheets.
- [`../inputs/spatial-input.md`](../inputs/spatial-input.md) (pending) — eyes + hands deep dive.
