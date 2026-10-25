# iOS / iPhone

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/designing-for-ios
**Last verified:** 2026-04-20
**Applies to:** iPhone (iOS)

This is the deep-dive platform file for iOS. Load it whenever you're building or reviewing anything that ships on iPhone. Pair with [`foundations/accessibility.md`](../foundations/accessibility.md) and whichever component/pattern files apply.

## Table of contents

- [What makes iOS iOS](#what-makes-ios-ios)
- [Device traits](#device-traits)
- [System features to integrate](#system-features-to-integrate)
- [Navigation paradigm](#navigation-paradigm)
- [Layout rules](#layout-rules)
- [Typography](#typography)
- [Color and materials](#color-and-materials)
- [Touch and gestures](#touch-and-gestures)
- [Feedback](#feedback)
- [Modality — used sparingly](#modality--used-sparingly)
- [Writing](#writing)
- [Common mistakes](#common-mistakes)
- [SwiftUI — a complete screen](#swiftui--a-complete-screen)
- [UIKit — equivalent screen](#uikit--equivalent-screen)
- [See also](#see-also)

## What makes iOS iOS

iPhone is one or two hands, on the go, switching apps every few seconds. Your screen has a minute of attention, not an hour. Primary actions live where the thumb lives (bottom half). Secondary actions are reachable with minimal interaction. Everything adapts — orientation, Dynamic Type, Dark Mode, appearance.

## Device traits

- **Display.** Medium, high-resolution. Rounded corners, sensor housing, Dynamic Island. Portrait is the default; many apps support landscape too.
- **Ergonomics.** One- or two-handed use. Viewing distance ~30–60cm. **Put primary controls in the bottom half** of the screen when you can.
- **Inputs.** Multi-Touch, keyboard (software and external), Apple Pencil on iPad-only (don't plan for it on iPhone), Voice Control, AssistiveTouch, Switch Control, Full Keyboard Access, Camera Control (on supported models), Action button (on Pro / 16 / 17 / Air), USB-C accessories.
- **App use patterns.** Sometimes 30 seconds (notifications, reply, glance); sometimes 60 minutes (media, game). People switch apps constantly.

## System features to integrate

Design for these or you're leaving ergonomics, discoverability, and delight on the floor:

- **Dynamic Island** (iPhone 14 Pro+). Host a Live Activity for in-progress tasks (ride tracking, sports scores, timers, delivery).
- **Lock Screen widgets** — glanceable data without unlocking.
- **Home Screen widgets** — small / medium / large / extra large (iPadOS) / accessory (watch-like) variants.
- **Live Activities** — on Lock Screen and Dynamic Island; short-duration, user-relevant, always fresh.
- **Control Center controls** — custom toggles for app features.
- **Lock Screen app icon + wallpaper integration** — tinted icon variants.
- **App Clips** — small experiences for a single task, often NFC/QR launched.
- **Shortcuts / Siri intents** — voice + automation.
- **Action button** — shortcut target.
- **Camera Control** — surface relevant camera-related controls.
- **Share Extension** — accept shares from other apps.
- **Spotlight** — make your content searchable.
- **Focus modes** — filter notifications and content for the current focus.
- **Universal Links / Handoff / SharePlay** — continuity.
- **Biometric auth** — Face ID / Touch ID via `LocalAuthentication`.
- **Sign in with Apple** — required if you offer third-party auth.

## Navigation paradigm

Pick one top-level structure. Don't nest two paradigms.

- **Tab bar** — flat navigation across 2–5 major areas. iOS tab bar lives at the bottom, uses fill SF Symbol variants, and is now Liquid Glass with a search tab (`.role(.search)`). Prefer `TabView` with `.sidebarAdaptable` for apps that also ship on iPad/Mac Catalyst.
- **Navigation stack** — hierarchical drill-in. Use `NavigationStack` with value-based `navigationDestination`.
- **Sidebar** — not a pattern for iPhone at compact widths. Becomes a tab bar on iPhone.

Within a tab, use `NavigationStack`. Across tabs, the user never navigates hierarchically — switching tabs is lateral.

## Layout rules

- **Respect safe areas.** Dynamic Island at the top, home indicator at the bottom. Content flows around, controls respect.
- **Extend content to edges.** Backgrounds go full-bleed. Chrome (tab bar, toolbar, nav bar) is Liquid Glass and floats above content — do not leave a colored band.
- **Use system margins.** In SwiftUI, default `.padding()` and `.padding(.horizontal)` give you the right insets. In UIKit, use `layoutMarginsGuide`.
- **Avoid full-width buttons.** Inset from edges. Only exception: primary CTAs that harmonize with hardware corner radius.
- **Hide the status bar only when it earns hiding.** Games, media full-screen — OK. Normal app screens — keep it.
- **Support both portrait and landscape** unless the experience demands one.
- **Size classes.** All iPhones are compact-width in portrait. Some are regular-width in landscape (Plus / Max / Air). Your layout must handle both.
- **Dynamic Type must reflow.** Use `.font(.body)` etc. — never fixed point sizes for body copy.

## Typography

- **SF Pro** (system). NY available as an optional serif.
- Default body size: **17pt**. Minimum: **11pt**.
- Use text styles (`.body`, `.headline`, `.title3`, etc.) exclusively for body copy. See [`../foundations/typography.md`](../foundations/typography.md).
- Support **Dynamic Type** all the way through AX5. Test at the largest size before shipping.
- SF Symbols match text weight automatically when used inline.

## Color and materials

- **Semantic colors.** `Color(.label)`, `Color(.systemBackground)`, etc. Never hex in production UI.
- **Two background sets:** system (plain views) and grouped (grouped list/table views).
- **Liquid Glass** for chrome (tab bar, nav bar, toolbar, floating buttons). Picks up color from behind; tint only for primary actions.
- **Standard materials** (`.ultraThinMaterial` ... `.thickMaterial`) for structural cards in the content layer.
- **Dark Mode** is mandatory. Base vs elevated backgrounds handled automatically by the system.
- Accent color defined once in the asset catalog.

## Touch and gestures

- **Minimum tap target: 44×44pt.** Default. 28pt is the absolute floor for dense UI — only use it when there's no alternative.
- **Spacing between targets:** ~12pt around bezeled, ~24pt around bezel-less.
- **Standard gestures:** tap, long press, swipe, drag, pinch, double-tap, multi-finger. Don't invent new ones.
- **Leading-edge swipe in a nav stack = back.** Don't override it.
- **Swipe actions on list rows** for row-level actions (archive, delete, flag).
- **Haptics pair with gestures and state changes.** `.sensoryFeedback(.success, trigger:)` in SwiftUI.

## Feedback

- **Haptics** for confirming an action (`.success`), flagging an error (`.error`), soft impact (`.impact(flexibility: .soft)`), selection change (`.selection`).
- **Toasts / inline confirmation** — prefer over modal alerts for non-blocking success.
- **Alerts** — blocking, user-actionable errors only.
- **Confirmation dialog** — when a tap needs to pick between a few paths.
- **Symbol animations** — bounce/replace for state changes; pulse for ongoing activity.

## Modality — used sparingly

- **Sheets (`.sheet`)** — focused sub-tasks that return to the parent. Detents: `.medium`, `.large`, custom, `.presentationDetents([.medium, .large])`.
- **Full-screen cover** — rare; use only for end-to-end flows (onboarding, modal video).
- **Popover** — only on iPad (iPhone automatically morphs popovers to sheets).
- **Don't go modal for things that are part of the main flow.** Drill-in is almost always right.

See [`../patterns/modality.md`](../patterns/modality.md) (pending) for when to say no to a sheet.

## Writing

- **"Tap"**, not "click."
- **Sentence case** for button labels and most titles. Avoid Title Case.
- **Short first.** If you need more, disclose progressively.
- **No destructive verbs without confirmation copy.** "Delete" + a confirmation dialog saying what will happen.

## Common mistakes

- Tab bars with 6+ tabs. Max 5; ship a More tab or convert lesser sections to rows.
- Custom tab bars that reinvent the system one.
- Full-width edge-to-edge buttons.
- Fixed-pt body text (breaks Dynamic Type).
- Hex color literals (break Dark Mode and increased contrast).
- Sheets for drill-in content.
- Sheets for settings (use a real settings screen; see [`../patterns/settings.md`](../patterns/settings.md)).
- Writing "Click" on iOS.
- Navigation titles centered when the iOS convention is leading-aligned for large titles, centered for inline.
- Icon-only buttons with no accessibility label.
- Full-screen modal flows that can't be dismissed.
- Ignoring the home indicator and sliding content under it.
- Inventing a custom "back" gesture.

## SwiftUI — a complete screen

```swift
import SwiftUI

@Observable final class InboxStore {
    var items: [Message] = []
    var isSyncing = false
    func markRead(_ id: Message.ID) { /* ... */ }
    func archive(_ id: Message.ID) { /* ... */ }
}

struct InboxView: View {
    @State private var store = InboxStore()
    @State private var selection: Message.ID?
    @State private var showingCompose = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        NavigationStack {
            List(selection: $selection) {
                ForEach(store.items) { message in
                    NavigationLink(value: message.id) {
                        MessageRow(message: message)
                    }
                    .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                        Button(role: .destructive) { store.archive(message.id) } label: {
                            Label("Archive", systemImage: "archivebox")
                        }
                    }
                    .swipeActions(edge: .leading) {
                        Button { store.markRead(message.id) } label: {
                            Label("Read", systemImage: "envelope.open")
                        }
                        .tint(.blue)
                    }
                }
            }
            .listStyle(.plain)
            .navigationTitle("Inbox")
            .navigationDestination(for: Message.ID.self) { id in
                MessageDetailView(messageID: id)
            }
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button { showingCompose = true } label: {
                        Image(systemName: "square.and.pencil")
                    }
                    .accessibilityLabel("Compose")
                }
                ToolbarItem(placement: .topBarLeading) {
                    if store.isSyncing {
                        ProgressView()
                    }
                }
            }
            .refreshable { await refresh() }
            .sheet(isPresented: $showingCompose) {
                ComposeView()
                    .presentationDetents([.medium, .large])
                    .presentationDragIndicator(.visible)
            }
            .searchable(text: .constant(""), placement: .toolbar)
        }
        .sensoryFeedback(.success, trigger: store.items.count)
    }

    private func refresh() async { /* ... */ }
}

struct MessageRow: View {
    let message: Message

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: message.isRead ? "envelope.open" : "envelope.fill")
                .foregroundStyle(message.isRead ? Color.secondary : Color.accentColor)
                .accessibilityHidden(true)
                .frame(width: 28)
            VStack(alignment: .leading, spacing: 2) {
                Text(message.sender).font(.headline)
                Text(message.preview).font(.subheadline).foregroundStyle(.secondary).lineLimit(2)
            }
            Spacer()
            Text(message.date, style: .time).font(.caption).foregroundStyle(.tertiary)
        }
        .frame(minHeight: 44)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(message.sender), \(message.preview)")
        .accessibilityAddTraits(.isButton)
    }
}
```

What this demonstrates: tab-less `NavigationStack`, Liquid Glass toolbar with primary action and leading status, searchable, swipe actions (no row taps), Dynamic Type via text styles, SF Symbols inline, sheet with detents, refresh, sensory feedback, VoiceOver labels, Reduce Motion (via SwiftUI's default handling).

## UIKit — equivalent screen

```swift
import UIKit

final class InboxViewController: UITableViewController {
    private var items: [Message] = []
    private lazy var composeButton: UIBarButtonItem = {
        let item = UIBarButtonItem(
            image: UIImage(systemName: "square.and.pencil"),
            style: .plain, target: self, action: #selector(compose)
        )
        item.accessibilityLabel = "Compose"
        return item
    }()

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        title = "Inbox"
        navigationItem.largeTitleDisplayMode = .always
        navigationController?.navigationBar.prefersLargeTitles = true
        navigationItem.rightBarButtonItem = composeButton

        tableView.register(UITableViewCell.self, forCellReuseIdentifier: "cell")
        tableView.refreshControl = UIRefreshControl()
        tableView.refreshControl?.addTarget(self, action: #selector(refresh), for: .valueChanged)

        navigationItem.searchController = UISearchController(searchResultsController: nil)
        navigationItem.hidesSearchBarWhenScrolling = true
    }

    override func tableView(_ t: UITableView, trailingSwipeActionsConfigurationForRowAt indexPath: IndexPath)
        -> UISwipeActionsConfiguration? {
        let archive = UIContextualAction(style: .destructive, title: "Archive") { [weak self] _, _, done in
            self?.archive(at: indexPath)
            done(true)
        }
        archive.image = UIImage(systemName: "archivebox")
        return UISwipeActionsConfiguration(actions: [archive])
    }

    override func tableView(_ t: UITableView, numberOfRowsInSection section: Int) -> Int { items.count }

    override func tableView(_ t: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = t.dequeueReusableCell(withIdentifier: "cell", for: indexPath)
        var cfg = UIListContentConfiguration.subtitleCell()
        let m = items[indexPath.row]
        cfg.text = m.sender
        cfg.secondaryText = m.preview
        cfg.image = UIImage(systemName: m.isRead ? "envelope.open" : "envelope.fill")
        cfg.imageProperties.tintColor = m.isRead ? .secondaryLabel : .tintColor
        cell.contentConfiguration = cfg
        cell.accessibilityLabel = "\(m.sender), \(m.preview)"
        return cell
    }

    @objc private func compose() { /* present sheet */ }
    @objc private func refresh() { /* ... */ }
    private func archive(at indexPath: IndexPath) { /* ... */ }
}
```

## See also

- [`../foundations/accessibility.md`](../foundations/accessibility.md)
- [`../foundations/layout.md`](../foundations/layout.md)
- [`../foundations/typography.md`](../foundations/typography.md)
- [`../foundations/color.md`](../foundations/color.md)
- [`../foundations/materials.md`](../foundations/materials.md)
- [`../components/bars.md`](../components/bars.md) (pending) — tab bar, nav bar, toolbar on iOS.
- [`../components/lists-and-tables.md`](../components/lists-and-tables.md) (pending) — list styles, swipe actions.
- [`../components/sheets-and-popovers.md`](../components/sheets-and-popovers.md) (pending) — sheet detents.
- [`../patterns/modality.md`](../patterns/modality.md) (pending) — when not to go modal.
- [`../patterns/settings.md`](../patterns/settings.md) (pending) — iOS Settings structure.
- [`../technologies/dynamic-island.md`](../technologies/dynamic-island.md) (pending).
- [`../technologies/widgets-and-live-activities.md`](../technologies/widgets-and-live-activities.md) (pending).
