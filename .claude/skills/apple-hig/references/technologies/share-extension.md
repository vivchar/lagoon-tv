# Share and action extensions

**HIG sources (composite):**
- https://developer.apple.com/design/human-interface-guidelines/activity-views

**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS (share sheet) · macOS (Share menu / Finder / Services) · visionOS (share sheet)
**Not supported:** tvOS · watchOS

Extensions let your app plug into **other apps' share sheets** and **Share menus**. A user in Photos or Safari taps Share, sees your app, taps, and accomplishes something — all without leaving the host app. Done right, a share extension is the most-used entry point your app has.

For the **host-side** (how your app triggers the share sheet), see [`../components/system-experiences.md`](../components/system-experiences.md).

## Two kinds

- **Share extension** — take content from another app and post / save / send it somewhere. Examples: post a photo to your social network, save an article to your reading app, send a URL to your team chat.
- **Action extension** — transform or act on content without sending it somewhere. Examples: translate selected text, bookmark a page, edit an inline image, summarize a document.

The rules overlap. Both should be fast, focused, and respect the host app's context.

## Where they appear

### iOS / iPadOS

- In the share sheet that opens when the user taps a Share button.
- Action extensions and share extensions both appear there.

### macOS

- **Share menu** (from the toolbar Share button or a context menu).
- **Services menu** (for action extensions operating on selections).
- **Finder quick action**.
- **Action buttons** in hover menus on embedded content (e.g., an image in a Mail compose window).

### visionOS

- Share sheet, same as iOS/iPadOS.

## Rules

### Fast and focused

- **1–3 steps max.** Share extension pattern: select destination → (optional) add annotation → post.
- **Pre-fill aggressively.** The host app hands you the content; you fill in what you can.
- **No multi-screen flow.** Users won't tolerate "next page" inside an extension.

### Feels like your app, but restrained

- **System-provided composition view** is the default. It's familiar, handles keyboard, provides a compose field, and ships account picker support.
- **Custom UI only when the system composer can't express your concept.** If you do, include elements from your main app (color scheme, typography, iconography) so users know the extension and app are related.
- **Show the app icon** (automatic for share extensions). Users trust the icon.
- **No advertising, no "upsell to the full app" screens, no newsletters.** Users are here to share, not be marketed to.

### No modal stacking

- **Don't present a sheet or popover on top of the extension.** The extension itself is a modal view.
- **Alerts are OK** for errors. Keep brief; offer a resolution.

### Long-running work continues in the main app

- **Extension dismisses immediately after commit.** Don't hold the user while your network call finishes.
- **Background the work** — let it complete after dismissal.
- **Notify on failure.** If the post fails, surface a notification with a resolution ("Tap to retry"). Success usually needs no notification.

### Respect auth

- **No sign-in inside the extension** if possible. If the user isn't signed into the main app, show a clear "Open MyApp to sign in" message with a deep-link button.
- **Sign-in via Keychain / App Group** that's shared with the main app.
- **Sign in with Apple** works smoothly inside extensions when auth must happen there.

### Icon

- **Use your app's existing icon** (default). Don't create a separate extension icon.
- **For action extensions where the task is specific** (e.g., "Translate"), a task-oriented SF Symbol or interface icon is OK and clarifies purpose.

### Accessibility

- **VoiceOver labels on every control.** The extension is a whole surface.
- **Dynamic Type** for all text.
- **Keyboard navigation** works on iPadOS / macOS.

## macOS specifics

- **Share menu extensions** appear alongside the system options (Messages, Mail, Notes). Follow standard placement and icon conventions.
- **Action extensions (Services)** operate on selections. Tight scope: translate, lookup, convert.
- **Quick Actions in Finder** — long-running transformations on selected files (rotate image, convert format). Respect the file-operations idiom: show progress; handle errors per-file.

## iOS / iPadOS share sheet specifics

- **App-specific activities appear before system actions** in the sheet.
- **Distinctive title** — a verb phrase ("Save to MyApp"). Never prefix with the company name.
- **70×70 px custom icon** centered if you need it. SF Symbol preferred.

## Common mistakes

- Multi-screen wizard inside an extension.
- Advertising the full app at extension launch.
- Blocking the user until the network call completes.
- Sign-in form inside the extension (no Keychain, no App Group, no Sign in with Apple).
- Custom composition view that's less usable than the system composer.
- No error feedback when a post fails silently.
- Restyled UI that's unrecognizable compared to the main app.
- Extension that does nothing the main app doesn't do better with one tap more.
- VoiceOver labels missing on custom controls.
- Modal sheet on top of an extension.

## Post-dismissal progress UX

```
Extension commits → dismisses immediately →
  App (in background via App Group) uploads →
    On success: no notification (or silent cache update) →
    On failure: local notification "Couldn't share: tap to retry"
```

Users feel the speed; failures are recoverable.

## iOS — share extension UI sketch (UIKit-style)

A share extension project template gives you `SLComposeServiceViewController`. Subclass and add your destination pickers.

```swift
import Social
import UniformTypeIdentifiers

class ShareViewController: SLComposeServiceViewController {
    override func isContentValid() -> Bool {
        return !(contentText?.isEmpty ?? true)
    }

    override func didSelectPost() {
        guard let extensionContext,
              let item = extensionContext.inputItems.first as? NSExtensionItem else {
            extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
            return
        }

        // Parse the incoming content (URL, image, text).
        Task.detached(priority: .userInitiated) {
            await ShareQueue.shared.enqueue(item, annotation: self.contentText ?? "")
        }

        // Return immediately — the host app moves on, we finish in the background.
        extensionContext.completeRequest(returningItems: [], completionHandler: nil)
    }

    override func configurationItems() -> [Any]! {
        let destination = SLComposeSheetConfigurationItem()!
        destination.title = "To"
        destination.value = currentDestination
        destination.tapHandler = { [weak self] in self?.pickDestination() }
        return [destination]
    }

    private func pickDestination() { /* push a small picker ... */ }
}
```

## SwiftUI (share extension container; iOS 17+)

```swift
@main
struct ShareExtension: App {
    var body: some Scene {
        WindowGroup {
            ShareComposer()
        }
    }
}

struct ShareComposer: View {
    @State private var note: String = ""
    @State private var destination: Destination = .inbox

    var body: some View {
        NavigationStack {
            Form {
                Section("Destination") {
                    Picker("Send to", selection: $destination) {
                        Text("Inbox").tag(Destination.inbox)
                        Text("Read Later").tag(Destination.readLater)
                        Text("Favorites").tag(Destination.favorites)
                    }
                }
                Section("Note (optional)") {
                    TextField("Add a note", text: $note, axis: .vertical)
                        .lineLimit(2...6)
                }
            }
            .navigationTitle("Save to MyApp")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("Save") { save() } }
            }
        }
    }

    private func dismiss() { /* complete the extension request */ }
    private func save() { /* enqueue + dismiss */ }
}
```

## AppKit — macOS Share menu extension

Ship as a share extension target (`.appex`). The system injects it into the Share menu. Configure in your `Info.plist` which activation rules apply (URL, image, text, document count limits).

```xml
<key>NSExtension</key>
<dict>
  <key>NSExtensionAttributes</key>
  <dict>
    <key>NSExtensionActivationRule</key>
    <dict>
      <key>NSExtensionActivationSupportsWebURLWithMaxCount</key><integer>1</integer>
      <key>NSExtensionActivationSupportsImageWithMaxCount</key><integer>5</integer>
    </dict>
  </dict>
  <key>NSExtensionPointIdentifier</key>
  <string>com.apple.share-services</string>
  <key>NSExtensionPrincipalClass</key>
  <string>$(PRODUCT_MODULE_NAME).ShareViewController</string>
</dict>
```

## See also

- [`../components/system-experiences.md`](../components/system-experiences.md) — host-side sharing (`ShareLink`, `UIActivityViewController`, `NSSharingServicePicker`).
- [`./shortcuts-and-siri.md`](./shortcuts-and-siri.md) — App Intents often cover the same tasks as action extensions.
- [`../patterns/managing-accounts.md`](../patterns/managing-accounts.md) (pending) — auth in the extension.
- [`../foundations/accessibility.md`](../foundations/accessibility.md) — VoiceOver and Dynamic Type in extensions.
- [`../platforms/ios.md`](../platforms/ios.md) — iOS share sheet context.
- [`../platforms/macos.md`](../platforms/macos.md) — Share menu / Services / Quick Actions.
