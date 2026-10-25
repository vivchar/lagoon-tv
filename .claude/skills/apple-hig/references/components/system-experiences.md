# System experiences (share sheet, activity views, share/action extensions)

**HIG sources:**
- https://developer.apple.com/design/human-interface-guidelines/activity-views

**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · visionOS (activity views); all platforms (share / action extensions where supported)

System experiences are the surfaces where your app plugs into the rest of the device — the **share sheet**, system action extensions, share extensions, Quick Look, Spotlight, and the Share menu on macOS. They are not components you style; they are platform contracts you integrate with.

For other "system experience" surfaces — widgets, Live Activities, Dynamic Island, Control Center, Lock Screen — see `../technologies/` (pending).

## Activity views (share sheet)

An activity view — commonly called the **share sheet** — presents a set of tasks people can perform on the current content: Share, Copy, Print, AirPlay, Add to Files, etc. Plus app-specific activities.

Available on iOS, iPadOS, and visionOS. Not supported on macOS, tvOS, or watchOS — on macOS, use a **Share menu** / toolbar Share button instead.

### How it works

- Users reveal a share sheet by tapping an **Action button** (`square.and.arrow.up`) while viewing content.
- The sheet appears as a sheet (iOS / iPadOS compact) or a popover (iPad regular, visionOS).
- Contents:
  1. **App-specific actions** (your custom activities) — first.
  2. **System / multi-app actions** — Copy, Print, AirPlay, Save to Files, Markup, etc.
  3. **Frequently-used apps** — personalized suggestions for sharing destinations.
  4. **All apps** — everything that advertises compatibility with this content type.
- Users can **edit the activity list** to surface what they use.

### Rules for app-specific activities

- **Don't duplicate existing system activities.** If Print already exists, don't add a second Print. If you need app-specific variance (e.g., "Print with cover page"), give it a distinctive title ("Print Transaction" — not just "Print").
- **Use an SF Symbol for the activity's icon.** Create a custom interface icon only when no system symbol fits. Custom icons should center in a ~70×70 px area.
- **Succinct, descriptive titles.** A single verb or a short verb phrase. Long titles wrap and may truncate.
- **Don't prefix with your company/product name** for app-specific actions. (Share activities — which represent a destination — do show the service name under the icon.)
- **Context-appropriate activity filtering.** Exclude system activities that don't apply (no Print for voice notes).
- **Use the Share button to open the share sheet.** Don't ship a second "send this somewhere" UI — users expect the system sheet.

### Common mistakes

- Redundant "Print" activity with the same title.
- Custom icon when an SF Symbol exists.
- Long multi-word activity titles.
- Activity list that doesn't filter by context (Print visible for content Print can't handle).
- Custom "Share" button that does something other than opening the share sheet.

### SwiftUI

```swift
// Trigger share sheet with ShareLink.
ShareLink("Share",
          item: article.url,
          subject: Text(article.title),
          message: Text("You might like this.")) {
    Label("Share", systemImage: "square.and.arrow.up")
}

// Inside a toolbar on iOS / iPadOS.
.toolbar {
    ToolbarItem(placement: .primaryAction) {
        ShareLink(item: document, preview: SharePreview(document.title,
                                                        image: document.thumbnail))
    }
}
```

### UIKit

```swift
let av = UIActivityViewController(
    activityItems: [article.url, article.title],
    applicationActivities: [CustomPrintTransactionActivity()]
)
av.excludedActivityTypes = [.assignToContact, .openInIBooks]
if let popover = av.popoverPresentationController {
    popover.sourceView = shareButton
    popover.sourceRect = shareButton.bounds
}
present(av, animated: true)
```

### AppKit

```swift
// macOS — NSSharingServicePicker in the toolbar.
let picker = NSSharingServicePicker(items: [article.url])
picker.show(relativeTo: shareButton.bounds, of: shareButton,
            preferredEdge: .minY)

// Toolbar Share button.
let share = NSToolbarItem(itemIdentifier: .share)
share.image = NSImage(systemSymbolName: "square.and.arrow.up",
                      accessibilityDescription: "Share")
share.label = "Share"
share.action = #selector(showSharePicker(_:))
```

## Share extensions

A share extension lets your app appear as a destination in other apps' share sheets.

### Rules

- **Prefer the system-provided composition view.** Consistent and familiar; reduces the UI you maintain.
- **Streamline interaction.** Ideally one tap/click to post; minimal setup within the extension.
- **Extension uses your app icon** by default — identifiable at a glance.
- **Avoid modal views on top of the extension.** An alert for errors is fine; don't stack sheets.
- **Long-running work continues in the app.** The extension dismisses immediately on commit. Display progress/state in the main app, notify when it finishes (non-trivial failures only).

### Platform notes

- **iOS / iPadOS:** appear in the share sheet.
- **macOS:** appear as items in the Share menu / Share toolbar button / Finder quick action / Services. Users reach them via right-click or the Share button.
- **visionOS:** same as iOS/iPadOS.

## Action extensions

Content-specific tasks: add bookmark, copy link, show selected text in another language, edit inline image, etc. Don't leave the current context.

### Rules

- **If you need a UI, make it feel like your app.** Include your app name; use elements from your main app's style. Users should know the extension and the app are related.
- **Prefer a custom interface icon** that communicates the task, or use an SF Symbol.
- **Streamline and limit.** 1–3 steps max inside the extension.
- **Avoid stacked modals.**
- **Long-running → continue in the main app**, same as share extensions.

### Platform notes

- **iOS / iPadOS:** show up in the share sheet.
- **macOS:** exposed via hover menu on embedded content (image in Mail compose), toolbar button, or Finder quick action.

## Quick Look

Quick Look is the system's preview surface — space bar on macOS, pinch-peek on iOS. If you own a custom file format, you're expected to ship a Quick Look generator so previews work in Mail, Messages, Files, Finder, Spotlight.

Rules in brief:

- **Render a faithful preview**, not a marketing page.
- **Static and fast.** Quick Look isn't interactive; don't try to embed a live view.
- **Accessibility metadata** — include alt text / descriptions so VoiceOver works.

## Spotlight

See [`../patterns/search.md`](../patterns/search.md#systemwide-search-spotlight). In summary:

- **CoreSpotlight** to index your content's attributes.
- **Metadata importer plug-in** for custom file formats.
- **Rich metadata** so users find things by title, author, date, and content.

## Services menu (macOS)

`NSServices` lets your app publish actions that other apps can invoke on their selected content (and consume actions from other apps). Follow the same "streamline / limit interaction" rule as share / action extensions.

## Common mistakes (across all system experiences)

- Building a custom "share" popover instead of using the system share sheet.
- Extension UI that doesn't feel like the host app (confuses users on the handoff back).
- Multiple "advertise my app" screens inside an extension.
- Long-running extension work that blocks dismissal.
- Custom Quick Look UI that tries to be interactive.
- Ignoring Spotlight indexing for a content-heavy app.
- macOS app that ships a Share toolbar item but doesn't wire up any services — users pick it, nothing happens.

## SwiftUI — rounding up ShareLink patterns

```swift
// URL
ShareLink(item: article.url)

// With custom label and preview thumbnail
ShareLink(item: document, preview: SharePreview(document.title,
                                                image: document.thumbnail))

// Multi-item (iOS 17+)
ShareLink(items: selectedPhotos) { photo in
    SharePreview(photo.title, image: photo.thumbnail)
}

// Toolbar Share (single-button)
.toolbar {
    ToolbarItem(placement: .primaryAction) {
        ShareLink(item: url) {
            Image(systemName: "square.and.arrow.up")
        }
    }
}
```

## See also

- [`../patterns/search.md`](../patterns/search.md) — Spotlight integration.
- [`./bars.md`](./bars.md) — placing a Share button in the toolbar.
- [`../technologies/widgets-and-live-activities.md`](../technologies/widgets-and-live-activities.md) (pending) — widgets, Live Activities, Dynamic Island.
- [`../technologies/share-extension.md`](../technologies/share-extension.md) (pending) — deeper dive on building share extensions.
- [`../technologies/imessage-apps.md`](../technologies/imessage-apps.md) (pending) — iMessage app extensions.
