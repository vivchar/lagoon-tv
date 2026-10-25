# Settings

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/settings
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS (custom settings UI) · watchOS (inline only)

People expect apps to just work. Settings exist so the rare tinkerer can tune behavior — not so you can push every decision onto the user. Ship smart defaults. Keep the settings surface small.

## Rules

- **Smart defaults.** Choose the default that works for the largest number of people. Don't launch into a mandatory configuration flow.
- **Minimize count.** Fewer, more meaningful settings. Too many → users can't find anything and the app feels fiddly.
- **Open settings the way users expect.** ⌘, on macOS/iPadOS with a keyboard. Esc opens game settings. Never rebind these.
- **Auto-detect what you can.** Connected controllers, Dark Mode, system language — read them; don't ask.
- **Never duplicate systemwide settings.** Don't ship your own "scrolling direction" or "authentication method" toggle. Respect the system's.
- **Don't hide important options as "Advanced."** If users need them, surface them.

## Where settings live

### Global, rarely-changed settings → app's settings area
Things the user adjusts once or twice: account details, window configuration, notification categories, default sort, external service connections.

### Task-specific options → in the task itself
Sort, filter, show/hide, reorder — these belong next to the content they affect, not in a separate settings screen. A filter control lives on the list. Show/hide lives in the view menu or toolbar. Reorder is an `EditButton`.

### System-level integration → system Settings app
Permissions (location, mic, contacts), notification allowance, Siri & Shortcuts integration, Focus filters — these surface in the system Settings app. In your app, provide a button that deep-links to your section via `UIApplication.openSettingsURLString`.

## iOS / iPadOS

- Put custom settings in a dedicated screen inside your app. Usually reached from a Settings row on the Profile tab, or a gear icon in the top toolbar.
- Use **`Form`** with grouped `List` sections — the platform convention.
- Include a button to open the system Settings app's page for your app when permission or system-integration adjustments are needed.
- Don't pop a settings sheet from random places — put the link where users expect (profile area, inside the specific feature that needs tuning).

## macOS

- **Use the `Settings` scene** (`Settings { SettingsView() }`). This wires up ⌘, for free and renders the standard macOS preferences window.
- **Multi-pane** with a toolbar — Tabbed (`TabView` with tab items) for a few panes, or sidebar-nav for many.
- **Window title follows the active pane** ("General", "Account", etc.). If there's only one pane, title it "App Name Settings".
- **Dim the minimize and maximize buttons** on the settings window — it isn't meant to be kept in the Dock or fullscreened.
- **Non-customizable toolbar** in the settings window. The toolbar is navigation.
- **Restore the last pane** opened on next launch.
- **Add "Settings…" in the App menu** via the standard command group — `CommandGroup(replacing: .appSettings)`. Standard shortcut: ⌘,.
- **Don't add a settings button to regular window toolbars.** Use the menu bar item.
- **Use `Form { ... }.formStyle(.grouped)`** for Mac-native grouped sections.

## watchOS

- Apps don't add themselves to the system Settings app on watchOS.
- Surface a few essential toggles at the bottom of the main view or via a More menu. Prefer inline over a whole settings screen.

## tvOS

- System-level settings for the Apple TV cover most global behaviors (appearance, audio, screen savers). App-specific settings belong within the app, accessed via a Settings row on a main tab.

## visionOS

- Follows the iOS/iPadOS conventions. Keep settings small.

## Privacy-adjacent settings

Respect the platform's permission primitives. Don't build your own toggle for "enable camera" — ask the system when the feature is used, honor the system's answer, and surface a link to system Settings if it's denied.

## Writing settings copy

- **Sentence case** for toggle labels.
- **Describe the effect, not the mechanism.** "Show sync status" > "Enable sync indicator".
- **Match button label to action.** "Sign Out" not "Log Out" (unless your product uses "Log"). Be consistent.
- **Explain side effects briefly** below a control if they're not obvious.
- **Don't use "Advanced" as a catchall.** If a setting matters, give it a real home.

## Common mistakes

- A welcome flow that forces the user through a dozen toggles before they see the app.
- Settings duplicating system preferences (Dark Mode toggle, text size override that ignores Dynamic Type).
- macOS "Settings" as a sheet on the main window instead of using the `Settings` scene.
- iOS settings reached from a gear icon in the toolbar of every screen ("where are the settings?" now has three answers).
- No ⌘, shortcut on macOS.
- Task-specific options (sort, filter) buried in settings.
- "Advanced" pane hiding things new users genuinely need.
- Settings window that can be zoomed/minimized.
- Toolbar in the settings window that the user can customize (confusing — it's navigation).
- No deep-link to system Settings from a permission-denied state.

## SwiftUI — macOS Settings scene

```swift
@main
struct LibraryApp: App {
    var body: some Scene {
        WindowGroup("Library") { RootView() }
        Settings {
            SettingsView()
                .frame(width: 520, height: 360)
        }
    }
}

struct SettingsView: View {
    var body: some View {
        TabView {
            GeneralSettings()
                .tabItem { Label("General", systemImage: "gear") }
            AccountSettings()
                .tabItem { Label("Account", systemImage: "person.crop.circle") }
            AdvancedSettings()
                .tabItem { Label("Advanced", systemImage: "slider.horizontal.3") }
        }
    }
}

struct GeneralSettings: View {
    @AppStorage("autoSync") private var autoSync = true
    @AppStorage("defaultSort") private var defaultSort: SortField = .title
    @AppStorage("showPreview") private var showPreview = true

    var body: some View {
        Form {
            Section {
                Toggle("Sync automatically", isOn: $autoSync)
                Picker("Default sort", selection: $defaultSort) {
                    ForEach(SortField.allCases) { Text($0.label).tag($0) }
                }
                Toggle("Show preview in list", isOn: $showPreview)
            }
        }
        .formStyle(.grouped)
    }
}
```

## SwiftUI — iOS/iPadOS settings screen

```swift
struct SettingsScreen: View {
    @AppStorage("notifications") private var notifications = true
    @AppStorage("haptics") private var haptics = true

    var body: some View {
        NavigationStack {
            Form {
                Section("Notifications") {
                    Toggle("Allow notifications", isOn: $notifications)
                    Button("Manage in Settings…") {
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    }
                }
                Section("Feedback") {
                    Toggle("Haptics on actions", isOn: $haptics)
                }
                Section("Account") {
                    NavigationLink("Profile") { ProfileSettingsView() }
                    Button("Sign Out", role: .destructive) { signOut() }
                }
                Section {
                    LabeledContent("Version", value: appVersion)
                }
            }
            .navigationTitle("Settings")
        }
    }
}
```

## UIKit / AppKit

```swift
// UIKit — deep-link to system Settings for permission states.
if let url = URL(string: UIApplication.openSettingsURLString),
   UIApplication.shared.canOpenURL(url) {
    UIApplication.shared.open(url)
}
```

```swift
// AppKit — legacy settings window controller if you aren't using SwiftUI Settings scene.
class SettingsWindowController: NSWindowController {
    convenience init() {
        let tabs = NSTabViewController()
        tabs.addTabViewItem(NSTabViewItem(viewController: GeneralVC()))
        tabs.addTabViewItem(NSTabViewItem(viewController: AccountVC()))
        let window = NSWindow(contentViewController: tabs)
        window.title = "Settings"
        window.styleMask = [.titled, .closable]    // no minimize/zoom
        self.init(window: window)
    }
}
// Register the App menu item via NSApplication.shared.mainMenu.
```

## See also

- [`../platforms/macos.md`](../platforms/macos.md#settings) — the macOS Settings scene in context.
- [`./onboarding.md`](./onboarding.md) — defer non-critical setup out of onboarding.
- [`./managing-accounts.md`](./managing-accounts.md) (pending) — account management patterns.
- [`./privacy.md`](../foundations/privacy.md) (pending) — permission request UX.
