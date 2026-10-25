# Lock Screen

**HIG sources (composite — HIG's Lock Screen guidance lives across widgets, Live Activities, and per-platform pages):**
- https://developer.apple.com/design/human-interface-guidelines/widgets (accessory widgets)
- https://developer.apple.com/design/human-interface-guidelines/live-activities (Lock Screen presentation)
- https://developer.apple.com/design/human-interface-guidelines/always-on
- https://developer.apple.com/design/human-interface-guidelines/designing-for-ios

**Last verified:** 2026-04-21
**Applies to:** iPhone (iOS 16+) · iPad (iPadOS 17+)

The Lock Screen is a high-traffic surface. Users look at it dozens of times a day. Your app's slice of it comes through **accessory widgets**, **Live Activity Lock Screen presentations**, **custom Controls (iOS 18+)**, and **notifications**. All share one trait: they compete for attention without interrupting.

## Surfaces your app can ship to the Lock Screen

- **Accessory widgets** (Circular, Rectangular, Inline) — pinned above/below the clock.
- **Live Activity** Lock Screen presentation — banner at the bottom.
- **Controls** (iOS 18+) — the two custom button slots (replace flashlight / camera by default).
- **Notifications** — standard and Rich Notifications.
- **StandBy** (iPhone) — custom widget scaling when the device is docked horizontally.
- **App backgrounds** (photo selections, Music/Podcasts lock screens) — full-screen lock screens tied to actively playing media.

## Accessory widgets on Lock Screen

See [`./widgets-and-live-activities.md`](./widgets-and-live-activities.md#widgets) for the full widget rules. Lock Screen specifics:

- **Vibrant rendering mode.** The system desaturates your content and applies a Lock Screen-appropriate blur/highlight. Use **opaque grayscale** (not opacity) — brightness determines vibrancy.
- **Full opacity images, numbers, and text.** White or light gray for primary; darker grayscale for secondary.
- **No custom tint color** on iPhone Lock Screen widgets. The user's Lock Screen tint applies.
- **StandBy in low light** (iPhone) renders the widget monochromatic red.
- **Functionally similar to watch complications.** The design guidelines overlap significantly — consider designing Lock Screen widgets and watch complications together. See [`../platforms/watchos.md`](../platforms/watchos.md).
- **Glanceable content.** Users aren't unlocking; they're glancing.

## Live Activity Lock Screen banner

See [`./widgets-and-live-activities.md`](./widgets-and-live-activities.md#live-activities) for the Live Activity rules. Lock Screen specifics:

- **Sits at the bottom** of the Lock Screen.
- **Custom background and tint** are allowed here (unlike Dynamic Island).
- **14pt standard margins.**
- **Verify your design on Always On** — colors adjust as luminance drops. Test with reduced luminance enabled.
- **Dynamic height** — grow with content, shrink when less is needed.
- **Don't replicate notification banner layouts** — make your Live Activity its own design.
- **Fallback on non-DI iPhones** — when a Live Activity alerts, your Lock Screen presentation shows as a Home Screen banner briefly.
- **Sensitive content is visible to anyone looking at the locked screen.** Redact it or gate behind tap.

## Controls on Lock Screen (iOS 18+)

See [`./control-center.md`](./control-center.md). Lock Screen specifics:

- **Two slots** on the Lock Screen — one leading, one trailing. By default they're flashlight and camera. Users can replace them with any installed Control.
- **One-tap action.** Don't make a destructive action available here.
- **Don't require unlock** for the Control's common case (the point is acting from the locked state).
- **Icon-only rendering.** The label doesn't appear on the Lock Screen slot — your glyph does all the work.

## Notifications on Lock Screen

See [`../patterns/managing-notifications.md`](../patterns/managing-notifications.md) (pending).

- **Redact sensitive content** (pass "Show preview when unlocked" defaults into account).
- **Actionable with custom actions** — reply, mark as read, snooze — without unlocking when possible.
- **Notification Summary** groups multiple notifications into a daily digest; design notification titles + bodies to read well there.
- **Time-sensitive notifications** bypass Focus when critical — use carefully.

## StandBy (iPhone docked horizontally)

- **Accessory widgets scale up 2×** to fill the StandBy frame.
- **Custom background extends to the whole screen** for seamless full-screen designs.
- **Low light → monochromatic red rendering** with reduced luminance.
- **Charging context** — users put their phone on a stand; your widget has a chance at multi-hour visibility. Design for a dim, ambient read.

## App Lock Screens (media-tied full-screen)

Music, Podcasts, and some other media apps can present a full-screen Lock Screen overlay during playback. Not every app ships this — if yours does:

- **Album art or hero content** large and beautiful.
- **Playback controls** (play/pause, next/previous, scrubber).
- **Standard controls where possible** — MPNowPlayingInfoCenter provides them for free.

## Rules common to every Lock Screen surface

- **Glanceable.** Lock Screen readers don't unlock first.
- **No sensitive info.** Anyone can see the Lock Screen.
- **No marketing.** Users turn it off.
- **Test Always On.** Every item must work at reduced luminance.
- **Test Dark Mode and Increase Contrast.**
- **Respect Focus.** Don't flood the Lock Screen when a Focus is active.

## Common mistakes

- Lock Screen widget with sensitive data visible.
- Full-color images in an accessory widget (wrong rendering mode, looks off).
- Live Activity with a background color that vanishes in Always On.
- Control on the Lock Screen that opens the app instead of doing an action.
- Notifications that leak private content to the lock preview.
- StandBy widget that doesn't scale up; looks tiny.
- Music player Lock Screen with custom controls instead of MPNowPlayingInfoCenter.
- 14pt margin ignored on Live Activity Lock Screen banner.
- No StandBy design at all for a widget users would put on the nightstand.
- Using the Lock Screen for promotional content — users revoke widget permissions or disable Live Activities.

## SwiftUI — accessory widget example

```swift
struct StepsAccessoryWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "steps", provider: StepsProvider()) { entry in
            switch entry.family {
            case .accessoryCircular:
                Gauge(value: Double(entry.steps), in: 0...Double(entry.goal)) {
                    Image(systemName: "figure.walk")
                } currentValueLabel: {
                    Text("\(entry.steps)")
                }
                .gaugeStyle(.accessoryCircular)

            case .accessoryRectangular:
                VStack(alignment: .leading, spacing: 2) {
                    Label("Steps", systemImage: "figure.walk")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("\(entry.steps) / \(entry.goal)")
                        .font(.headline)
                    ProgressView(value: Double(entry.steps),
                                 total: Double(entry.goal))
                }

            case .accessoryInline:
                Text("\(entry.steps) steps · \(entry.percentString) of goal")
            default:
                EmptyView()
            }
        }
        .supportedFamilies([.accessoryCircular, .accessoryRectangular, .accessoryInline])
        .configurationDisplayName("Steps")
        .description("See your step count progress on the Lock Screen.")
    }
}
```

## See also

- [`./widgets-and-live-activities.md`](./widgets-and-live-activities.md) — widget and Live Activity core rules.
- [`./dynamic-island.md`](./dynamic-island.md) — Lock Screen is the fallback on non-DI iPhones.
- [`./control-center.md`](./control-center.md) — Controls reach the Lock Screen.
- [`../platforms/ios.md`](../platforms/ios.md) — iOS Lock Screen hierarchy.
- [`../platforms/watchos.md`](../platforms/watchos.md) — complications share design DNA with Lock Screen widgets.
- [`../patterns/managing-notifications.md`](../patterns/managing-notifications.md) (pending) — notification design.
