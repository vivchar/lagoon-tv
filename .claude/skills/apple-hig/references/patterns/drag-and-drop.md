# Drag and drop

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/drag-and-drop
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · visionOS
**Not supported:** tvOS · watchOS

Users drag to **move** content within the same container and **copy** between containers. Cross-app drags **always copy**. On iPad, macOS, and visionOS, users expect drag-drop to work everywhere — list rows, grid cells, text selections, images, files. If a view holds content, it should probably accept and produce drags.

## Default semantics

- **Same container → move.** Dragging a file within a Finder window moves it.
- **Different container → copy.** Dragging a file to another Finder window copies.
- **Across apps → always copy.** A photo from Photos into Mail is copied, not moved.
- **Hold Option while dragging on macOS** → force a copy even within the same container.
- **Hold Command + Option on macOS** → create an alias / link.

Think carefully before changing defaults. Users expect these behaviors; breaking them loses data.

## Rules

- **Support drag-drop wherever it's obvious.** Text fields and text views get it free. Lists, grids, and custom views need you to wire it up.
- **Offer alternatives.** Menu commands (Copy/Cut/Paste), keyboard shortcuts, Share Sheet — for users who can't or don't want to drag. Accessibility APIs let assistive tech drive drops.
- **Multi-item drag.** Select many, drag as a group. iPad supports **additive drag** — start dragging one item, then tap others to pile them on before dropping.
- **Undo support.** Undo a drag-drop operation. If it can't be undone, confirm before completing when the target is consequential (write-only folder, shared stream).
- **Multiple fidelities.** Offer dragged content in multiple formats from high-fidelity to low (native → PDF → PNG → JPEG). Destinations pick the best they can accept.
- **Spring loading.** Drag over a button, tab, or folder → it activates without a drop, allowing the user to continue dragging (force-click on Mac trackpad; hover-then-activate on iPad).

## Providing feedback

Drag-drop is direct manipulation — feedback must be continuous.

- **Drag image** appears at ~3pt of movement. Translucent representation of the dragged content. Persists until drop.
- **Modify the drag image** when it clarifies the result (expand to show the image's target size in the document).
- **Drag flocking** — visually group multi-item drags.
- **Highlight valid destinations** only when the dragged content is over them. Clear the highlight when the user drags away.
- **Show "not allowed"** when a destination can't accept — `xmark.circle` or the macOS "operation not allowed" pointer.
- **One highlight at a time** when multiple destinations could accept — focus attention on the closest or most likely.
- **On failure**: animate the item back to the source, or scale up + fade out to suggest evaporation.

## Accepting drops

- **Auto-scroll inside a container** when the user drags near its edge. Stop when they drag back out.
- **Richest version first.** If the drag offers a chart object and a fallback image, take the chart if you support it.
- **Extract relevant parts.** Dropping a contact on a recipient field takes the email, not the mailing address.
- **Respect modifier keys at drop time.** Check if Option is held to override default move/copy semantics.
- **Progress indicator for slow transfers.** Placeholder row in a list during upload.
- **Start the action immediately** if the drop initiates a task (print, convert). Show progress.
- **Apply destination styling to dropped text** when the source and destination don't share style systems. Preserve style when they do.
- **Selection state** — dropped content stays selected in the destination. Source selection state depends: move clears it; cross-container copy deselects the source.

## Platform specifics

### iOS / iPadOS

- **Touch drag** — press and hold on a draggable item, then drag.
- **Additive drag** — while dragging, tap additional items to add them to the session.
- **Multiple simultaneous drag sessions** — each finger owns a session.
- **Cross-app drag in Split View / Stage Manager / Slide Over.**
- **Universal Control** lets users drag between paired Mac and iPad.
- **Accessibility** — expose sources and destinations via `UIDragItem` / `UIDropInteraction` API so VoiceOver can narrate drags.

### macOS

- **Finder drags** — let your app produce files dropped into Finder (Calendar drags events as `.ics`). Clippings (temp containers for dragged content) are an option for unsupported targets.
- **Inactive window drag** — users drag from an inactive window without bringing it forward. Preserve background selection appearance while dragging.
- **Drag an unselected item** from an inactive window without deselecting the active selection.
- **Badge with count** on multi-item drags — if the destination only accepts some, update the count as the user hovers.
- **Change pointer appearance** during drag — copy, drag link, disappearing item, operation not allowed.
- **Single motion** — let users select and drag in one gesture. No required pause.

### visionOS

- **Pinch-and-drag** in any direction, including **along z-axis** (the 3D one).
- **Drop into empty space** — when the user drops content into open air, your app can open a window or scene to handle it. URL drops → Safari; Quick Look-supported types → Quick Look.

## Common mistakes

- Drag-only action with no menu or keyboard alternative (excludes users who can't drag).
- Ignoring Option key for copy-within-same-container.
- Drag image that stays in the source position after release (looks broken).
- No highlight on valid destination.
- Multi-item drag that consolidates into one item at drop.
- Cross-app drag that claims move semantics (always copy).
- iPad additive drag not supported where users expect it (Home Screen, Photos).
- No drop-into-empty-space handling on visionOS.
- Macos Finder drag that produces a file the user can't reopen.
- Drop UI that blocks while a long transfer completes — no progress indicator.
- Accessibility labels missing on drag source / drop destination.

## SwiftUI

```swift
// Dragging items from a list.
List(photos) { photo in
    PhotoRow(photo: photo)
        .draggable(photo) {
            // Optional preview customization
            Image(photo.thumbnail).resizable().frame(width: 60, height: 60)
        }
}

// Accepting drops on a view.
PhotoAlbumView()
    .dropDestination(for: Photo.self) { items, location in
        album.insert(contentsOf: items, at: insertionIndex(for: location))
        return !items.isEmpty
    } isTargeted: { targeted in
        // Highlight when targeted == true
        isDropTargetActive = targeted
    }

// Multi-format drag with prioritized fidelity.
Image(uiImage: chartImage)
    .draggable(chartExport) {
        Image(uiImage: chartImage).resizable().frame(width: 120, height: 80)
    }

struct ChartExport: Transferable {
    let chart: Chart
    let rendered: Image
    let pdfData: Data
    let pngData: Data

    static var transferRepresentation: some TransferRepresentation {
        DataRepresentation(exportedContentType: .pdf) { $0.pdfData }
        DataRepresentation(exportedContentType: .png) { $0.pngData }
        ProxyRepresentation(exporting: \.rendered)       // lowest fidelity
    }
}

// Drop into empty space on visionOS — register a handler at the WindowGroup.
WindowGroup(id: "viewer") { URLViewer() }
    .handlesExternalEvents(matching: ["url"])
```

## UIKit

```swift
// Source — UIDragInteractionDelegate
let drag = UIDragInteraction(delegate: self)
drag.isEnabled = true
thumbnailView.addInteraction(drag)

extension ViewController: UIDragInteractionDelegate {
    func dragInteraction(_ i: UIDragInteraction,
                        itemsForBeginning session: UIDragSession) -> [UIDragItem] {
        let provider = NSItemProvider(object: photo.image)
        let item = UIDragItem(itemProvider: provider)
        item.localObject = photo
        return [item]
    }
}

// Destination — UIDropInteractionDelegate
let drop = UIDropInteraction(delegate: self)
view.addInteraction(drop)

extension ViewController: UIDropInteractionDelegate {
    func dropInteraction(_ i: UIDropInteraction, canHandle session: UIDropSession) -> Bool {
        session.canLoadObjects(ofClass: UIImage.self)
    }
    func dropInteraction(_ i: UIDropInteraction,
                         sessionDidUpdate session: UIDropSession) -> UIDropProposal {
        UIDropProposal(operation: .copy)
    }
    func dropInteraction(_ i: UIDropInteraction, performDrop session: UIDropSession) {
        session.loadObjects(ofClass: UIImage.self) { images in
            // append to album
        }
    }
}
```

## AppKit

```swift
// Register for drags.
view.registerForDraggedTypes([.fileURL, .png, .tiff])

// Source implements NSDraggingSource; destination implements NSDraggingDestination.
override func draggingEntered(_ sender: any NSDraggingInfo) -> NSDragOperation {
    sender.draggingPasteboard.canReadObject(forClasses: [NSImage.self]) ? .copy : []
}

override func performDragOperation(_ sender: any NSDraggingInfo) -> Bool {
    guard let images = sender.draggingPasteboard.readObjects(forClasses: [NSImage.self])
        as? [NSImage] else { return false }
    album.append(contentsOf: images)
    return true
}
```

## See also

- [`../platforms/ipados.md`](../platforms/ipados.md) — drag-drop is iPad-native.
- [`../platforms/macos.md`](../platforms/macos.md) — Finder drags, inactive window drags.
- [`../platforms/visionos.md`](../platforms/visionos.md) — z-axis drags, drop-into-empty-space.
- [`../components/lists-and-tables.md`](../components/lists-and-tables.md) — list row reorder via drag.
- [`../components/collections-and-scrolling.md`](../components/collections-and-scrolling.md) — grids and drag.
- [`../foundations/accessibility.md`](../foundations/accessibility.md) — drag-drop a11y APIs.
