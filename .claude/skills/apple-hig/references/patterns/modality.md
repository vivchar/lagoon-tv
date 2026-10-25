# Modality

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/modality
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

Modality is a design choice, not a shortcut. Going modal takes the user out of their current context and demands an explicit action to return. Most apps ship too many modals.

## When modality is the right call

- **Ensure critical information lands** (error requiring user action).
- **Offer options that confirm or modify the most recent action** (unsaved edits: Discard / Keep / Save).
- **Support a distinct, narrowly scoped task** that would be disruptive to inline (compose message, attach file, pick from a constrained set).
- **Immersive focus** (play video, edit photo, view fullscreen camera).

## When modality is wrong

- **Drill-in navigation.** If the user will come back deeper in the app, use a push (`NavigationStack`) — a sheet that slides up and then back implies they're returning to the previous screen.
- **Top-level sections.** Tab bars or sidebars exist for this.
- **Passing informational content.** Prefer inline status, toasts, badges, or non-modal banners.
- **Multi-step flows that create an "app within an app."** If the modal has its own nav stack with drill-ins, users lose track of how to exit.
- **Settings that the user toggles frequently.** Put them where they apply, not in a modal.

## Rules

- **Keep modal tasks simple, short, and streamlined.** Long modals are how people forget what they were doing.
- **No nested modals without closing the previous.** Stack clutter = cognitive load. Alerts are the exception and can sit on top of any modal.
- **Always provide an obvious dismiss.** iOS/iPadOS/watchOS: top toolbar button or swipe-down. macOS/tvOS: a dismiss button in the content area. Every platform also supports keyboard dismissal (Esc, ⌘.).
- **Confirm before discarding unsaved user work.** If swipe-dismissing or tapping Cancel would drop user-entered content, ask via an action sheet / confirmation dialog.
- **Title the modal's task.** When users return from another app or screen, a title reorients them.
- **Avoid deep hierarchies inside a modal.** Single path through any sub-steps. No button that could be confused with the dismiss button.
- **Full-screen modals** are for immersive or multi-step tasks (video, photo editing, camera). On visionOS, in a Shared Space they fill the window; transitioning to a Full Space can make the experience more immersive.

## Decision tree

```
 Is the task returning the user to the same place?
 ├─ No  →  Navigation (push / replace). Not modal.
 └─ Yes
    ├─ Is it urgent or a confirm/cancel for the last action?
    │   └─ Yes → Alert or Confirmation Dialog (action sheet)
    └─ Is it a scoped sub-task with its own view?
        ├─ Short form, a few fields  → Sheet
        ├─ Tiny widget with a source → Popover
        ├─ Multi-step immersive flow → Full-screen cover / Full Space
        └─ Repeated input + observation → Panel (macOS) / non-modal sheet (iOS/iPadOS)
```

## Platform notes

### iOS / iPadOS
- Non-modal sheets exist and are a good fit for tools that affect the parent (Notes' formatting sheet).
- Provide a grabber on resizable sheets.
- On iPhone, prefer `.medium` + `.large` detents for progressive disclosure; use `.large` only when content demands full height.
- On iPad, prefer `.page` or `.form` sheet presentation — fixed size, centered on dimmed parent.

### macOS
- Sheets dim the parent window but other app windows remain interactive.
- For repeated-input workflows (Find & Replace), use a **panel** (NSPanel) — observable + modeless.
- Present sheets in a reasonable default size.

### visionOS
- Sheets float in front of their parent window and become the interaction target.
- Center in the field of view — don't emerge from the bottom of the window.
- Resizable is fine; don't cover the whole window by default.
- Alerts anchor to the window that presented them.

### watchOS
- Sheets are full-screen with a material background that blurs the parent.
- Only use a sheet when an alert or action sheet can't express the content.
- Keep brief.

## Writing for modals

- **Title the task**, not the screen: "New Reminder" not "Reminders - Editor".
- **Pair Done with Cancel.** Never ship only Done — implies completion is the only exit.
- **Multi-step flow:** use Back instead of Cancel after step 1.
- **Swipe-dismiss unsaved work:** confirmation dialog with Discard (destructive) / Keep Editing (cancel).

## Common mistakes

- Sheet used for drill-in navigation (user expects to return, not go deeper).
- Full-screen cover for a one-field input.
- Nested sheets without closing the first.
- Alert used to announce success (should be toast or inline).
- Alert used to deliver information with no action (should be inline).
- Modal with its own `NavigationStack` and back button that looks like app nav.
- No confirmation when swipe-dismiss would drop user data.
- Modal without a title.
- No keyboard dismissal.
- On visionOS: modal emerging from the bottom edge.

## SwiftUI

```swift
// Sheet — scoped sub-task.
@State private var showingCompose = false

Button("Compose") { showingCompose = true }
    .sheet(isPresented: $showingCompose) {
        NavigationStack {
            ComposeView()
                .navigationTitle("New Message")
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { askDiscardIfNeeded() }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Send") { send() }.disabled(!canSend)
                    }
                }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

// Full-screen cover — immersive multi-step.
.fullScreenCover(isPresented: $isEditing) { PhotoEditor(photo: photo) }

// Confirm before dismiss drops user work.
.interactiveDismissDisabled(hasUnsavedChanges)
.confirmationDialog("Discard your changes?",
                    isPresented: $askDiscard) {
    Button("Discard Changes", role: .destructive) { dismiss() }
    Button("Keep Editing", role: .cancel) { }
}

// Non-modal iPad/iPhone sheet (tools that affect the parent).
.sheet(isPresented: $showingFormat) {
    FormatToolsView()
        .presentationDetents([.height(140)])
        .presentationBackgroundInteraction(.enabled)  // parent stays interactive
        .presentationDragIndicator(.visible)
}
```

## See also

- [`../components/sheets-and-popovers.md`](../components/sheets-and-popovers.md) — the full decision table and rules per presentation type.
- [`./feedback.md`](./feedback.md) — non-modal feedback alternatives.
- [`./navigation.md`](./navigation.md) — push vs sheet.
