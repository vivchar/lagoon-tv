# App Clips

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/app-clips
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS
**Not supported:** macOS · tvOS · visionOS · watchOS

An App Clip is a **small, focused version** of your app that launches instantly without a download. Users scan an App Clip Code, tap an NFC tag, follow a Smart App Banner, or open a link in Messages — and they're in. No App Store, no install flow, no account. Used right, it's the fastest path from "I'm standing at the thing" to "I'm doing the thing."

## When to ship an App Clip

Two clear use cases:

- **On-the-go task.** Rent the bike. Pay for parking. Order from this food truck. Scan this museum exhibit.
- **Demo / try-before-buy.** Play the first level. Preview the workout. Try the editor.

If your app is a service people commit to (social network, bank, mail client), an App Clip probably isn't the right match. Stick with the full app.

## Discovery paths

- **App Clip Code** (physical) — Apple's recognizable circular badge. Scan with Camera or Code Scanner in Control Center.
- **NFC tag** — hold the iPhone close. Either a standalone NFC tag or an NFC-integrated App Clip Code.
- **QR code** — Camera app recognizes.
- **Smart App Banner** on your website.
- **App Clip card in Safari** when the user visits a linked page.
- **Siri Suggestions** — location-based (Maps, Siri).
- **Shared links in Messages** — recipient taps, launches the App Clip inside Messages.
- **Deep-link from another app** (iOS 17+) — apps can present previews and links to other App Clips.

## The 10-second rule

Users expect the App Clip to launch, work, and be useful within seconds. Every second of setup, every form field, every network wait is attrition.

- **Omit splash screens.** Launch directly to the task.
- **Show the relevant part.** If the scan is at a parking meter, show the payment screen — not a home feed.
- **Small download.** Target < 15 MB uncompressed. Trim unused code, ship only required assets.
- **Include everything in-package.** Don't block the user on a background download.
- **Use native components.** No web views wrapping a site — users expect app quality.

## Scope

An App Clip is not a miniature app. It's a **single task** or a **single demo**.

- **Focused UI** — no tab bars, no nested navigation, no settings screen.
- **Linear flow** — start → input → confirm → done.
- **Minimal forms.** Use Apple Pay for checkout. Use Sign in with Apple for auth (if truly needed).
- **No advertising.** No in-app purchases pushed before the task completes. No "sign up for our newsletter" interstitial.

## Rules

- **Complete the task in the clip.** The user shouldn't need to install the full app to finish the rental, place the order, see the demo.
- **Reserve advanced features for the full app.** History, settings, deep customization, account management — those belong in the app.
- **Don't require an account up-front.** Offer Sign in with Apple when sign-in is truly needed, and offer it after the task if possible.
- **Make the clip shareable.** Deep links that take recipients to specific contexts inside the clip.
- **Make payment easy.** Apple Pay express checkout beats typing shipping info on the spot.
- **Privacy limits apply.** No background operation. Store as little as possible; assume the system will remove your clip between launches.

## App Clip card

The card appears before launch (from Smart App Banner, Safari, Maps, Messages). It's your user's first impression.

- **Informative image** — photography or graphics. Not a screenshot of your UI.
- **Avoid text in the image** — not localizable, hard to read, aesthetically weak.
- **1800×1200 px PNG or JPEG.** No transparency.
- **Title ≤ 30 characters.**
- **Subtitle ≤ 56 characters.**
- **Action verb** — View, Play, or Open. Play for games, View for informational/educational content, Open for everything else.

## App Clip Codes

Apple's distinct circular badge. Users trust it.

- **Two variants:** scan-only (camera icon in center) and NFC-integrated (iPhone icon in center).
- **Use the NFC-integrated variant** when the code is physically accessible (tabletops, registers, storefronts, signage at reach).
- **Use the scan-only variant** for codes people see from a distance (posters, billboards, printed marketing).
- **Follow Apple's size, placement, and printing guidelines** — the code has minimum print sizes and contrast requirements.
- **Choose colors** — default pair, or custom foreground/background. The logo remains Apple-branded.

## Showcasing the full app

App Clips should lead interested users to the full app, but politely.

- **App Clip card** always has a link to the App Store page.
- **System app banner** appears at the top of the screen on first launch.
- **Overlay inside your clip** — display it at a natural pause, not mid-task. Apple provides `SKOverlay` for this.
- **Never nag.** Don't ask repeatedly. Don't send push notifications selling the app.
- **Clarify value.** When you prompt for the full app, explain what the full app adds.

## Notifications

App Clips can schedule notifications for up to 8 hours post-launch by default.

- **Permission for > 8 hours is opt-in.** Ask only when truly needed (car rental due date).
- **Task-related only.** No promotional notifications.
- **In response to user action.** Not push-spam.

## Transition to the full app

When the user installs the full app, it replaces the App Clip.

- **Don't require re-login.** The full app should pick up where the clip left off.
- **Preserve the clip's context** if the user was mid-flow.
- **Familiar UX** — the full app starts where the clip ended, then reveals the full navigation.

## Multi-business / platform-provider scenarios

A single App Clip can power many businesses (parking meter vendor, restaurant platform).

- **Branding tone down.** The business's brand goes front and center on the card; your platform brand recedes.
- **Detect and switch between businesses** if the user interacts with more than one.
- **Location-aware launch** — pick the right business based on the scan context.

## Common mistakes

- App Clip that demands account creation before the task.
- Splash screen that adds seconds before the task starts.
- Web-view-wrapped clip — looks and feels wrong.
- Clip that downloads extra assets in the foreground.
- Clip too large (> 15 MB) — slow to launch.
- Tab bar, nested nav, or settings screen inside the clip.
- Full-app prompts at launch, mid-task, or repeatedly.
- Clip card image that's a screenshot of a login screen.
- Title > 30 chars (truncates).
- Required login flow that can't be bypassed for a simple task.
- Requesting notification permission without reason.

## SwiftUI

```swift
import SwiftUI
import StoreKit

@main
struct ParkingClip: App {
    var body: some Scene {
        WindowGroup {
            ParkingClipView()
                .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
                    guard let url = activity.webpageURL else { return }
                    // Parse the lot ID and spot number from the App Clip URL.
                    ParkingSession.shared.configure(from: url)
                }
        }
    }
}

struct ParkingClipView: View {
    @State private var durationHours: Int = 1
    @State private var isPaying = false

    var body: some View {
        VStack(spacing: 24) {
            Text(ParkingSession.shared.lotName)
                .font(.title.bold())
            Text("Spot \(ParkingSession.shared.spotNumber)")
                .font(.headline)
                .foregroundStyle(.secondary)

            Stepper(value: $durationHours, in: 1...12) {
                Text("\(durationHours) hour\(durationHours == 1 ? "" : "s")")
                    .font(.headline.monospacedDigit())
            }
            .padding()

            Spacer()

            PayWithAppleButton {
                isPaying = true
                Task { await pay() }
            }
            .frame(maxWidth: .infinity, minHeight: 52)

            // Offer the full app after the task, not before.
            if ParkingSession.shared.isComplete {
                Button("Get the full app for history and autopay") {
                    showOverlay()
                }
                .buttonStyle(.bordered)
            }
        }
        .padding()
    }

    private func pay() async { /* ... Apple Pay flow ... */ }
    private func showOverlay() {
        let config = SKOverlay.AppClipConfiguration(position: .bottom)
        // Present SKOverlay in the scene
    }
}
```

## See also

- [`./shortcuts-and-siri.md`](./shortcuts-and-siri.md) — App Clips can start Live Activities via App Intents.
- [`./widgets-and-live-activities.md`](./widgets-and-live-activities.md) — Live Activity to track the ongoing clip task.
- [`../patterns/onboarding.md`](../patterns/onboarding.md) — don't front-load setup in the clip.
- [`../patterns/managing-accounts.md`](../patterns/managing-accounts.md) (pending) — Sign in with Apple as the minimum-friction auth.
- [`../patterns/in-app-purchase.md`](../patterns/in-app-purchase.md) (pending) — Apple Pay for checkout.
- [`../platforms/ios.md`](../platforms/ios.md) — App Clip invocations on iOS.
