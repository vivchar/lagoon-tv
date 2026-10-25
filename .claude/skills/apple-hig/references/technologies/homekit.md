# HomeKit

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/homekit
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · watchOS

HomeKit is Apple's framework for smart home accessories. Your app **lives alongside** the system **Home app** — it complements, doesn't replace. The Home app is the system-standard accessory controller; your job is to offer **setup**, **custom features**, **brand-specific nuance**, and **support** — not a competing generic UI.

## The HomeKit object model

Memorize the vocabulary. Your app uses these terms throughout UI.

- **Home** — a physical home, office, or location. A user can have multiple.
- **Room** — a physical room in a home. Names like "Bedroom", "Kitchen".
- **Zone** — a group of rooms. "Upstairs", "Downstairs".
- **Accessory** — a physical connected device. "Bedroom Lamp".
- **Service** — a controllable feature of an accessory. An accessory can have multiple services (a ceiling fan has a fan service AND a light service).
- **Characteristic** — a controllable attribute of a service (brightness, speed, temperature).
- **Service group** — several services people want to control together ("Reading Lamps" = 3 lamps in a corner).
- **Action** — changing a characteristic (dim to 40%, turn on).
- **Scene** — a named group of actions across services ("Movie Time" = dim lights, lower shades, turn on TV).
- **Automation** — event-triggered actions (sunset, arrival, sensor detection, time of day).

Use `home`, `room`, `zone`, `accessory`, `service`, `scene`, `automation` in your UI labels. **Don't use "action set"** (that's the API term for scene) — always say "scene" to users.

## Rules

### Respect the hierarchy

- **Acknowledge the model** even if your app organizes its UI differently. If the user has rooms and zones set in the Home app, reflect them.
- **Make HomeKit info findable** from an accessory's detail view (current room, zone, scene membership). Don't hide it deep in settings.
- **Support multiple homes.** A detail view should show which home the accessory belongs to.
- **Don't duplicate home settings.** Never re-ask the user to set up rooms, zones, or scenes you can get from HomeKit. Always defer to existing settings.

### Setup

- **Use the system-provided setup flow.** Fast, familiar, handles network pairing, naming, room assignment, category assignment, favorites. Your app adds custom post-setup features — not a parallel setup.
- **Purpose string for Home permission.** Apple requires a reason. "Lets you control this accessory with the Apple Home app and Siri across your Apple devices."
- **Don't require an account** for HomeKit control. Defer to HomeKit for identity.
- **Custom services can be optional add-ons.** Cloud services, extra automations — offer after core HomeKit setup, make opt-in.
- **Don't force cross-platform setup in the HomeKit flow.** If you support Google Home / Amazon, offer that separately.

### Help people pick good names

Service names become voice commands ("turn on floor lamp"). Good names are specific and natural.

- **Suggest better names** if the user writes something suboptimal. Never suggest company names or model numbers.
- **Enforce HomeKit naming rules:**
  - Alphanumeric, space, apostrophe only.
  - Start and end with alphabetic or numeric character.
  - No emojis.
- **Avoid putting room names in service names.** "Bedroom lamp" confuses voice commands if the lamp is already assigned to the Bedroom room. Rename to "Bedside lamp" and assign to Bedroom.

### Siri interactions

HomeKit + Siri is the killer feature. Apple did the NLP; you just need good service / room / zone / scene names.

Phrases Siri understands (depending on the names in the user's home):

- "Turn on the floor lamp" — service name.
- "Turn on the light" — category.
- "Turn off the living room light" — room + category.
- "Make the living room a little bit brighter" — room + implied accessory + brightness characteristic.
- "Turn on the recessed lights" — service group.
- "Turn off the lights upstairs" — category + zone.
- "Run Good Night" — scene.
- "Did I leave the garage door open?" — accessory category + state query.
- "It's dark in here" — implied room (via HomePod) + implied category.

**Teach users by example.** After setup, show 2–3 sample phrases. Don't over-document; trust discoverability.

**Offer shortcuts only for things HomeKit can't do.** If HomeKit already understands "turn off the lights", don't suggest the user make a shortcut for it — they'll get confused about which system handled it.

### Custom functionality

This is where your app shines. Features the Home app doesn't have.

- **Accessory-specific features.** A smart light with millions of colors could let users capture colors from photos.
- **Visualization.** Energy dashboards, usage charts.
- **Advanced automations.** Complex conditions the Home app doesn't expose.
- **Support / troubleshooting.** Diagnostics, firmware updates, Wi-Fi health.
- **Be clear about when to use the Home app** vs your app. If the user needs to add a non-your-brand accessory, point them to Home.

### Handling conflicts

If your local database diverges from HomeKit (e.g., renamed service), **defer to HomeKit**. Present the conflict visually — show both names side by side and let the user pick.

## Per-platform notes

### iOS / iPadOS
Home app is primary. Your app extends. Ships `HomeKit` framework.

### macOS
Same as iOS/iPadOS. `HomeKit` available.

### tvOS
Control-only. Apple TV is often a home hub.

### watchOS
Quick control. `HomeKit` on-device. Use complications for frequent accessories.

### visionOS
Not officially supported.

## Common mistakes

- **Parallel setup flow** — asking users to re-create their home, rooms, zones.
- **Ignoring existing rooms.** Accessory detail view doesn't show the room.
- **Using "action set" in UI** — must be "scene".
- **Suggesting service names that include the room** — breaks Siri commands.
- **Shortcuts for things Siri already knows.**
- **Requiring an account for basic HomeKit control.**
- **Hiding HomeKit info** in a settings screen three levels down.
- **Not recognizing multiple homes.**
- **Conflicts with HomeKit source of truth** (e.g., user renames in Home, your app shows the old name).
- **Using terms like "service", "characteristic", "action set" in UI.** API-only.

## SwiftUI / HomeKit

```swift
import HomeKit
import SwiftUI

struct HomeListView: View {
    @State private var homes: [HMHome] = []
    private let manager = HMHomeManager()

    var body: some View {
        NavigationStack {
            List(homes, id: \.uniqueIdentifier) { home in
                Section(home.name) {
                    ForEach(home.rooms, id: \.uniqueIdentifier) { room in
                        NavigationLink(room.name) {
                            RoomAccessoriesView(room: room)
                        }
                    }
                    if !home.zones.isEmpty {
                        NavigationLink("Zones") { ZoneListView(home: home) }
                    }
                    NavigationLink("Scenes") { SceneListView(home: home) }
                }
            }
            .navigationTitle("Homes")
            .task {
                // Observe HMHomeManager delegate to populate homes.
                manager.delegate = HomeManagerAdapter { self.homes = $0 }
            }
        }
    }
}

struct RoomAccessoriesView: View {
    let room: HMRoom

    var body: some View {
        List {
            ForEach(room.accessories, id: \.uniqueIdentifier) { accessory in
                Section(accessory.name) {
                    ForEach(accessory.services, id: \.uniqueIdentifier) { service in
                        NavigationLink(service.name) {
                            ServiceDetailView(service: service)
                        }
                    }
                }
            }
        }
        .navigationTitle(room.name)
    }
}

struct ServiceDetailView: View {
    let service: HMService

    var body: some View {
        Form {
            Section {
                if let brightness = service.characteristics
                    .first(where: { $0.characteristicType == HMCharacteristicTypeBrightness }) {
                    // Slider for brightness — control the characteristic.
                    BrightnessSlider(characteristic: brightness)
                }
                // Other characteristics (hue, saturation, color temperature, etc.)
            } header: {
                Text(service.name)
            } footer: {
                // Show which room this service lives in — don't hide HomeKit context.
                if let room = service.accessory?.room {
                    Text("In \(room.name)")
                }
            }
        }
    }
}
```

## See also

- [`./shortcuts-and-siri.md`](./shortcuts-and-siri.md) — Siri voice commands for HomeKit.
- [`./control-center.md`](./control-center.md) — home automation Controls.
- [`./widgets-and-live-activities.md`](./widgets-and-live-activities.md) — accessory state in widgets.
- [`../foundations/writing.md`](../foundations/writing.md) (pending) — HomeKit vocabulary consistency.
- [`../platforms/ios.md`](../platforms/ios.md) — HomeKit on iPhone.
