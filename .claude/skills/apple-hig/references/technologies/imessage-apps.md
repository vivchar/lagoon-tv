# iMessage apps and stickers

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/imessage-apps-and-stickers
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS
**Not supported:** macOS · tvOS · visionOS · watchOS

iMessage apps and sticker packs live **inside a Messages conversation** and also in effects in Messages and FaceTime. They let people share content, collaborate, and decorate chat — without leaving the conversation. Ship as a standalone app or as an app extension of your iOS/iPadOS app.

## Two kinds

- **iMessage app** — a mini UI inside Messages. Share a playlist, vote on where to eat, play a quick game, send a structured card (reservation, event, receipt).
- **Sticker pack** — a set of images (static or animated) users drag into conversations. Can also be part of a larger iMessage app.

## iMessage app — when and what

- **One primary experience per iMessage app.** Users are in conversation flow; a single-purpose tool wins over a multi-tool.
- **Surface content from your main app.** Shared shopping list, trip itinerary, wishlist. The iMessage app is a conversational handle onto your main app's data.
- **Collaborative micro-tasks.** Decide where to eat. Pick a time. Vote on a movie.
- **Shareable artifacts.** Cards that capture a reservation or a purchase receipt — recipients can tap for detail.

## Compact vs expanded

An iMessage app has two views:

- **Compact** — a small tray that appears below the message transcript. About the size of the keyboard. Frequently-used actions, essential content, shortcuts.
- **Expanded** — occupies most of the window. Full UI, keyboard if needed, deep browsing or editing.

### Rules

- **Essential features in compact.** Frequently-used items must work without expanding.
- **Edit text only in expanded.** The keyboard takes the compact tray's space — keep editing upstairs where both keyboard and content are visible.
- **Transition between compact and expanded naturally.** The user drags up to expand.
- **Compact view is a tray, not a tab.** No multi-screen navigation in compact.

## Stickers — rules

- **Expressive, inclusive, versatile.** Reads against light and dark conversation backgrounds; still looks right when rotated or scaled.
- **Use transparency** to let stickers mix visually with text, photos, and other stickers.
- **Localized alternative description for every sticker.** VoiceOver reads it; enables sticker use by everyone.
- **Consistent pack.** If your pack is 24 stickers, they should feel like one family.
- **Static or animated.** Static is PNG; animated is APNG or GIF. JPEG is also supported (no transparency).

## Sizing

### Icon

Square-cornered icon (the system applies the rounded mask).

| Usage | @2x (px) | @3x (px) |
|---|---|---|
| Messages, notifications | 148×110 | — |
| Messages drawer | 143×100 | — |
| Alt | 120×90 | 180×135 |
| Small | 64×48 | 96×72 |
| Tiny | 54×40 | 81×60 |
| Settings | 58×58 | 87×87 |
| App Store | 1024×1024 | 1024×1024 |

### Stickers

Pick **one size** per pack; don't mix.

| Size | @3x dimensions (px) |
|---|---|
| Small | 300×300 |
| Regular | 408×408 |
| Large | 618×618 |

**Max 500 KB per sticker file.**

| Format | Transparency | Animation |
|---|---|---|
| PNG | 8-bit | No |
| APNG | 8-bit | Yes |
| GIF | Single-color | Yes |
| JPEG | No | No |

Prefer APNG for animated stickers — better quality, true transparency. GIF only when APNG is impractical.

## Writing copy

- **Your app name** in the icon. No long descriptions.
- **Inside the compact tray**, use verbs for actions ("Share playlist", "Start vote", "Send ticket").
- **Sticker alt text** describes the sticker, not your brand.

## Accessibility

- **Alt text on every sticker.**
- **Dynamic Type on any text in the iMessage app.**
- **VoiceOver labels on every interactive element.**
- **Respect Reduce Motion** — if your animated stickers can be paused or have a still fallback, honor the setting.

## Common mistakes

- **Two distinct experiences** in one iMessage app (e.g., "share playlist" + "play a game"). Split them.
- **Deep multi-screen navigation** in the compact tray.
- **Text editing in the compact tray** — keyboard covers the UI.
- **Mixing sticker sizes** in one pack.
- **Stickers with opaque white backgrounds** — read poorly against light conversation UI.
- **No alt text on stickers** — inaccessible.
- **GIF when APNG would be better** — lower visual quality.
- **App Store icon at any size other than 1024×1024.**
- **iMessage app that just launches the main iOS app.**

## SwiftUI / Messages framework

iMessage apps are built with the `Messages` framework (`MSMessagesAppViewController`). SwiftUI views hosted inside.

```swift
import Messages
import SwiftUI

class MessagesViewController: MSMessagesAppViewController {
    override func willBecomeActive(with conversation: MSConversation) {
        super.willBecomeActive(with: conversation)
        loadSwiftUI(presentationStyle: presentationStyle)
    }

    override func didTransition(to presentationStyle: MSMessagesAppPresentationStyle) {
        super.didTransition(to: presentationStyle)
        loadSwiftUI(presentationStyle: presentationStyle)
    }

    private func loadSwiftUI(presentationStyle: MSMessagesAppPresentationStyle) {
        children.forEach { $0.view.removeFromSuperview(); $0.removeFromParent() }

        let host = UIHostingController(rootView: RootView(
            presentation: presentationStyle,
            send: { [weak self] message in self?.sendMessage(message) }
        ))
        addChild(host)
        view.addSubview(host.view)
        host.view.frame = view.bounds
        host.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        host.didMove(toParent: self)
    }

    private func sendMessage(_ message: MSMessage) {
        activeConversation?.send(message) { _ in /* handle error */ }
        dismiss()
    }
}

struct RootView: View {
    let presentation: MSMessagesAppPresentationStyle
    let send: (MSMessage) -> Void

    var body: some View {
        switch presentation {
        case .compact:
            CompactTray(send: send)        // lightweight actions
        case .expanded:
            ExpandedComposer(send: send)   // full UI
        case .transcript:
            TranscriptView()               // inline in the message list
        @unknown default:
            EmptyView()
        }
    }
}

struct CompactTray: View {
    let send: (MSMessage) -> Void

    var body: some View {
        HStack(spacing: 12) {
            Button { send(MessageBuilder.newVote()) } label: {
                Label("Start Vote", systemImage: "checkmark.circle")
            }
            .buttonStyle(.borderedProminent)

            Button { send(MessageBuilder.shareList()) } label: {
                Label("Share List", systemImage: "list.bullet.rectangle")
            }
            .buttonStyle(.bordered)

            Spacer()
        }
        .padding()
    }
}
```

## Sticker pack target — no code, just assets

A sticker pack is a target type that contains only asset catalogs. The system renders the stickers for you. You provide:

- **Asset catalog** with a "Stickers" asset.
- **One size** per pack (Small, Regular, or Large).
- **Alt text per sticker** (in the asset catalog).

No view controller, no SwiftUI view. The system handles dragging into conversations and reactions.

## See also

- [`../foundations/accessibility.md`](../foundations/accessibility.md) — alt text and VoiceOver.
- [`../components/text-inputs.md`](../components/text-inputs.md) — editing happens in the expanded view.
- [`./share-extension.md`](./share-extension.md) — another way to expose your app inside Messages.
- [`../platforms/ios.md`](../platforms/ios.md) — Messages app context.
