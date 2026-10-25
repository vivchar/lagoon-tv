# File management

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/file-management
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · visionOS
**Limited or not supported:** tvOS · watchOS (users don't manage files)

Document-based apps let users create, open, edit, save, and browse files. Your app interops with **Finder** (macOS) and **Files** (iOS / iPadOS / visionOS). Don't reinvent file management — extend it.

## Creating and opening files

### Rules

- **App menus and keyboard shortcuts.** iPadOS shortcut interface shows your File menu when a keyboard is attached. macOS menu bar does the same. Standard shortcuts: ⌘N new, ⌘O open, ⌘S save, ⇧⌘S save as, ⌘W close, ⌘P print.
- **Add (+) button on iPadOS / iOS** for a quick new-document action. In macOS, put the new action in the File menu.
- **Document launcher (iOS 18+, iPadOS 18+)** — the system-provided full-screen launcher for document-based apps. Consists of a title card with two app-specific buttons, a background image, accessory images, and a file browser sheet.

### iOS / iPadOS document launcher

- **Title card** — app name + two buttons. Assign the primary button to your most important function. Numbers uses "Start Writing" primary + "Choose a Template" secondary.
- **Background image** — solid color, gradient, or pattern. Clearly distinct from the accessories and title card.
- **Accessories** — images placed in front of or behind the title card to create depth. Don't clutter. Test across screen sizes and orientations.
- **File browser sheet** with optional custom toolbar controls.
- **Animation sparingly.** Subtle breathing or swaying is fine; heavy motion disorients.

### Custom file browsers

If you must build a custom file browser (rare — prefer the system one):

- **Support the platform's file system layout.** Users know Files and Finder — respect the basic structure (Documents, iCloud Drive, On My iPad, etc.).
- **Default to the most relevant location** — Documents folder, iCloud folder, most recent path.
- **Let users navigate anywhere they have access to.** Don't cage them.

## Saving

### Rules

- **Autosave by default.** Periodic saves while editing + on-close + on-app-switch. Don't make users explicitly save.
- **Hide file extensions by default.** Let users opt into showing them.
- **Reflect the user's extension preference** across all your Save/Open interfaces.
- **Name visibility in the title bar.** macOS shows "Edited" in the title bar when there are unsaved changes; remove it immediately after save.
- **Dot on close button** when the document has unsaved changes (macOS). Don't show the dot when autosave is on — implies manual action is needed.
- **Before quitting / closing without autosave enabled**, show the standard save-or-discard dialog.

### iOS / iPadOS saving

- Save is usually implicit. `UIDocument` subclass auto-saves on state changes.
- Let users rename in-place or via a prominent toolbar action.

### macOS saving

- `NSDocument` architecture handles most of this. Autosave, Versions, Lock / Duplicate / Revert.
- **Save As** → File menu, standard ⇧⌘S shortcut.
- **Move / Rename** inline via the title bar document menu.
- **Custom Save dialog accessory view** is OK when you need format-specific options (e.g., "Include attachments" in Mail).

## Quick Look

Quick Look is the system-wide file preview surface. Space bar on macOS. Pinch peek on iOS. Works in Mail, Messages, Files, Finder, Spotlight.

### Rules

- **Quick Look viewer in your app** — let users preview files your app doesn't open. User sees the content without leaving you.
- **Quick Look generator for custom file types** — lets Finder, Files, Spotlight, and other apps show your format's previews. Ship this if you have a custom format.
- **Static previews.** Not interactive — don't try to embed live UI.
- **Include accessibility metadata** (alt text, descriptions) so VoiceOver works.

## File provider extensions (iOS / iPadOS)

If your app stores files that other apps might import, export, open, or move, ship a **file provider extension**.

### Rules

- **Filter for context.** If a PDF editor loads your provider, show only PDFs.
- **Useful metadata** in the file list: size, modification date, local vs cloud.
- **Let users pick a destination** when exporting/moving — navigate your directory hierarchy, create subdirs.
- **No custom top toolbar.** Your extension loads inside a modal view with the system toolbar. Don't add a second.

## Finder Sync extensions (macOS)

For apps that sync local and remote files (Dropbox, OneDrive, etc.):

- **Badges** on files in the Finder to indicate sync status.
- **Contextual menu items** for file operations (favorite, lock, share).
- **Toolbar buttons** for global sync actions.

## Rules common across platforms

- **Trust autosave.** Never surprise the user with lost work.
- **Versions matter.** macOS Versions API gives free history. Your app should support it for document types.
- **iCloud by default** when appropriate. Users expect their documents to follow them.
- **Clear sync status.** Show when a file is syncing, pending, offline-only.
- **Respect user's folder organization.** Don't dump files in unexpected locations.

## Common mistakes

- No autosave — user loses work.
- Custom file browser that doesn't match Finder / Files idioms.
- Force-showing file extensions when the user prefers them hidden.
- Extra toolbar in a file provider extension.
- Quick Look generator missing for custom file types (files show as "no preview" everywhere).
- Document launcher with jarring animation.
- Document launcher background indistinguishable from foreground.
- Document launcher primary button that doesn't perform your app's main function.
- Files app / Finder integration absent — users can't find their documents from outside your app.
- iCloud sync that silently fails with no status indicator.
- File extensions changed or added silently by your save flow.
- No ⌘S / ⇧⌘S shortcuts.
- No "Edited" indicator when autosave is off.

## SwiftUI — DocumentGroup

```swift
@main
struct NotebookApp: App {
    var body: some Scene {
        DocumentGroup(newDocument: NotebookDocument()) { file in
            EditorView(document: file.$document)
        }
        .commands {
            CommandGroup(after: .newItem) {
                Button("New from Template…") { /* ... */ }
                    .keyboardShortcut("n", modifiers: [.command, .shift])
            }
        }
    }
}

// File-format-aware document.
import UniformTypeIdentifiers

struct NotebookDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.mynoteFormat] }

    var text: String

    init(text: String = "") { self.text = text }

    init(configuration: ReadConfiguration) throws {
        guard let data = configuration.file.regularFileContents,
              let text = String(data: data, encoding: .utf8)
        else { throw CocoaError(.fileReadCorruptFile) }
        self.text = text
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: Data(text.utf8))
    }
}

extension UTType {
    static let mynoteFormat = UTType(exportedAs: "com.example.mynote")
}
```

## Document launcher (iOS 18+ / iPadOS 18+)

```swift
DocumentGroupLaunchScene(for: NotebookDocument.self) {
    NewDocumentButton("Start Writing")
        .foregroundStyle(.primary)
    SecondaryDocumentButton("From Template") { TemplateGallery() }
} background: {
    LinearGradient(colors: [.indigo, .purple], startPoint: .top, endPoint: .bottom)
} foregroundAccessoryView: {
    Image("pencilHero").resizable().scaledToFit().frame(maxWidth: 400)
}
```

## UIKit / AppKit

```swift
// AppKit: Standard document-based app via NSDocument subclass.
class NotebookDocument: NSDocument {
    var text: String = ""
    override class var autosavesInPlace: Bool { true }

    override func data(ofType typeName: String) throws -> Data {
        Data(text.utf8)
    }
    override func read(from data: Data, ofType typeName: String) throws {
        text = String(data: data, encoding: .utf8) ?? ""
    }
}

// iOS UIKit: UIDocument subclass.
class NotebookDocument: UIDocument {
    var text: String = ""

    override func contents(forType typeName: String) throws -> Any {
        Data(text.utf8)
    }
    override func load(fromContents contents: Any, ofType typeName: String?) throws {
        guard let data = contents as? Data else { throw CocoaError(.fileReadCorruptFile) }
        text = String(data: data, encoding: .utf8) ?? ""
    }
}
```

## See also

- [`./drag-and-drop.md`](./drag-and-drop.md) — drag files from Finder / Files into your app.
- [`../technologies/share-extension.md`](../technologies/share-extension.md) — share from your documents to other apps.
- [`../platforms/ipados.md`](../platforms/ipados.md) — document launcher behavior on iPad.
- [`../platforms/macos.md`](../platforms/macos.md) — NSDocument, Finder integration.
- [`../components/text-inputs.md`](../components/text-inputs.md) — editing text inside documents.
- [`../components/bars.md`](../components/bars.md) — toolbar in a document window.
