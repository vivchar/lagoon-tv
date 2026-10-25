# Printing

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/printing
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · visionOS
**Not supported:** tvOS · watchOS

If your app shows content that could reasonably be printed — documents, photos, receipts, tickets, itineraries — ship printing. It's cheap to wire up and users expect it.

## Core rules

- **Discoverable in standard locations.**
  - **macOS:** File menu → Print (⌘P). Optional toolbar button (customizable).
  - **iOS / iPadOS / visionOS:** toolbar Action button → Print activity in the share sheet.
- **Don't offer print when there's nothing to print** or no printers are available. Dim the menu item (macOS) or remove the share-sheet activity (iOS).
- **Use the system print panel.** Page range, copies, double-sided, paper handling, layout — all built in. Don't recreate.
- **Preview included.** System panel shows it; honor page size and orientation.

## macOS — custom print categories

The system panel has standard categories (Layout, Paper Handling, Media & Quality, Watermark, etc.). You can add a **custom category** with app-specific options.

### Rules

- **Unique name** — usually your app name. "Keynote" category for presentation options.
- **App-specific options only.** Don't duplicate what the system offers.
- **Advanced options hidden by default.** Use a disclosure control. Label as "Advanced Options".
- **Interdependencies visible.** If selecting A disables B, show B as disabled.
- **Let users preview the effect** — thumbnail updates with setting changes.
- **Page Setup dialog** for document-specific settings (paper size, orientation, scaling). Only needed if your document has defaults that aren't per-print-job.
- **Store modified settings with the document** at least until it closes, so reprints keep the settings.

### Example app-specific options

- **Keynote:** Print presenter notes, slide backgrounds, skipped slides.
- **Mail:** Include attachments.
- **Photos:** Print contact sheet, print as borderless.
- **Safari:** Header/footer content.

## iOS / iPadOS / visionOS

### Rules

- **Print via `UIActivityViewController`** with `UIPrintInteractionController`.
- **Options:** page range, copies, double-sided, paper size, orientation — all standard.
- **Custom page renderer** (`UIPrintPageRenderer` subclass) for non-standard content (structured receipts, book pages, multi-column layouts).
- **AirPrint discovery** is automatic — don't prompt users to set up printers.
- **PDF export via print** — iOS/iPadOS print panel exposes "Save to Files" → PDF. Don't ship a separate PDF export if print to PDF would suffice.

## SwiftUI / UIKit / AppKit

```swift
// macOS — SwiftUI's .printDialog() for basic cases.
import SwiftUI
import AppKit

struct DocumentView: View {
    let document: NotebookDocument

    var body: some View {
        ScrollView {
            NotebookRenderer(document: document)
        }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button { showPrintPanel() } label: {
                    Label("Print", systemImage: "printer")
                }
                .keyboardShortcut("p")
            }
        }
    }

    private func showPrintPanel() {
        let operation = NSPrintOperation(
            view: NotebookPrintView(document: document),
            printInfo: .shared
        )
        operation.run()
    }
}
```

```swift
// iOS / iPadOS — UIPrintInteractionController.
import UIKit

func presentPrint(url: URL) {
    let controller = UIPrintInteractionController.shared
    let info = UIPrintInfo(dictionary: nil)
    info.outputType = .general
    info.jobName = url.deletingPathExtension().lastPathComponent
    info.duplex = .longEdge
    controller.printInfo = info
    controller.printingItem = url
    controller.present(animated: true) { _, completed, error in
        if let error { showError(error) }
    }
}

// Custom page renderer for structured content.
class ReceiptRenderer: UIPrintPageRenderer {
    let receipt: Receipt
    init(receipt: Receipt) {
        self.receipt = receipt
        super.init()
        headerHeight = 40
        footerHeight = 40
    }

    override func drawHeaderForPage(at pageIndex: Int, in headerRect: CGRect) {
        let title = receipt.merchantName as NSString
        title.draw(in: headerRect,
                   withAttributes: [.font: UIFont.preferredFont(forTextStyle: .headline)])
    }

    override func drawContentForPage(at pageIndex: Int, in contentRect: CGRect) {
        // Draw line items, totals, etc.
    }
}
```

## Common mistakes

- Ship your own print panel instead of using the system's.
- Print button visible when no content is printable.
- Print button visible when no printers are available (don't even require this — AirPrint is the default).
- Duplicate system options in a custom category.
- No advanced-options disclosure — flat wall of checkboxes.
- Ignoring document-specific page settings.
- No PDF fallback on iOS (users print to PDF all the time via "Save to Files" — make sure your layout survives).
- Fixed font sizes that pixelate when printed at 300 DPI.
- Layouts with screen-only assumptions (translucent overlays, dark-mode gradients) that print terribly.

## See also

- [`../components/bars.md`](../components/bars.md) — toolbar Print button.
- [`../components/system-experiences.md`](../components/system-experiences.md) — iOS share sheet includes Print.
- [`./file-management.md`](./file-management.md) — documents often have a print equivalent.
- [`../platforms/macos.md`](../platforms/macos.md) — File menu Print convention.
