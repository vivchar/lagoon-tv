# Managing accounts

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/managing-accounts
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

Accounts are barriers. The fewer, the faster, the better. If your app can work without an account, let it. If it requires one, make signup feel like nothing.

## When to require an account

- **Core functionality depends on it.** Cloud-synced data, purchases, personalized content that doesn't exist without identity.
- **Legal or safety requires it.** Age verification, financial services, medical records.
- **The value only exists with it.** Social graph, shared history.

If any of the above — yes. If the user can get meaningful value without one — no. Most apps push account creation too early.

## Rules

- **Explain the benefit.** "Create an account to sync your library across devices." Never a wall of bullet points.
- **Delay sign-in as long as possible.** Let users try, create, browse — require the account only when the next action truly needs it. A shopping app lets users browse; a check-out screen is the right place to sign in.
- **Sign in with Apple first.** Zero-friction auth, hides email, matches the system aesthetic, works on every Apple device.
- **Passkeys over passwords** when not using Sign in with Apple. Users provide just a username; passkey is authenticated via device biometrics. No password to remember, no password to leak.
- **Name the authentication method.** "Sign In with Face ID" not just "Sign In". Makes trust visible.
- **Context-check biometric availability.** Don't reference Face ID on a Touch ID device. Use `LAContext.canEvaluatePolicy(...)`.
- **Don't offer an app-level biometric toggle.** Users control this at the system level; a per-app switch is redundant and confusing.
- **Two-factor auth for password-based accounts.** Always. `textContentType(.oneTimeCode)` autofills from Messages.
- **Never use "passcode"** for app auth. Passcode is the device lock. Using the word confuses users into thinking they should reuse their device passcode.

## Deleting accounts (required)

If your app lets users create accounts, it **must also let them delete them** — not just deactivate. Apple enforces this. Align with your region's right-to-be-forgotten laws too.

### Rules

- **Discoverable.** A visible "Delete Account" option in your app or game. Not buried in Terms of Service or Privacy Policy.
- **In-app deletion preferred.** If you can't fully delete from the app, provide a **direct link** to the web page that does it. No "go to our site and dig through support."
- **Revoke Sign in with Apple tokens** when the user deletes. Apple provides the API.
- **Consistent flow** — web-based and in-app deletion should feel the same. Don't make one version longer or more annoying.
- **Scheduled deletion option** — let users schedule for a future date (end of subscription, etc.). Still offer immediate deletion.
- **Completion notification.** Tell the user when deletion is fully complete (may take time for distributed systems). Notify them when done.

### In-app purchases and subscription context

Users often confuse "deleted account" with "cancelled subscription." They're separate.

- **Explain** that billing for auto-renewable subscriptions continues through Apple until the subscription is cancelled — regardless of account state.
- **Link to cancellation/refund flow** alongside the deletion option.
- **Even if the subscription was purchased outside your app**, you still must support account deletion.

See [`./in-app-purchase.md`](./in-app-purchase.md#manage).

## TV provider accounts (tvOS)

If your app uses TV Provider Authentication, users sign in once at the system level and every app gets access.

- **No in-app sign-out button** when the user is signed in at system level. Direct them to Settings → TV Provider.
- **Never direct users to Privacy settings to sign out.** Privacy controls gate app access; they're not a sign-out mechanism.

## Sign-in UI

- **Minimal fields at sign-in.** Email + password (or username + passkey) is typical. Everything else is post-signup.
- **Clear error messages.** "Incorrect email or password" (don't reveal which is wrong — confirms attacker is trying known accounts). "Account locked, check email."
- **Forgot password / passkey recovery** prominent.
- **Sign in with Apple prominent** in the same visual tier as any other sign-in method.

## Sign-up UI

- **As few fields as possible.** Email, password (or passkey).
- **Progressive disclosure.** Collect name, preferences, avatar — after the account exists, in context.
- **Strong-password suggestion.** `textContentType(.newPassword)` triggers iCloud Keychain suggestion.
- **Clear privacy messaging.** Link to policy; don't make the user guess.

## Multi-user contexts

### tvOS

In tvOS 16+, your app can share credentials across all family users while storing profiles separately. Don't force each person to sign into the same household account.

### macOS

- **Fast User Switching** — account state is per-user. Don't leak one user's account into another's session.

### iPadOS

- **Shared iPad** (education) — per-user data isolation is automatic via system. Your app must respect the `UIUserIdentity`.

## watchOS

- **iCloud Keychain sync** — autofill usernames and passwords on the watch. The user authenticates once on iPhone.
- **Don't re-request credentials** on the watch if already signed in on paired iPhone.

## Platform specifics

### iOS / iPadOS / macOS / visionOS

Sign in with Apple is the safest, smoothest option. Passkeys are the strong second. Passwords + 2FA for fallback.

### tvOS

Remote typing is brutal. **Use another-device auth** whenever possible:

- **Associated Domains** → iPhone suggests credentials.
- **Continuity keyboard** — iPhone accepts the text input for your tvOS text field.
- Send a short code + URL to another device for full sign-up flows.

## Common mistakes

- Force sign-in at launch for an app that doesn't need it.
- Sign-in required to browse content.
- "Passcode" instead of "password" / "passkey".
- Face ID referenced on a Touch ID device.
- In-app biometric toggle that duplicates system setting.
- No "Delete Account" option — blocks App Store review.
- Account deletion hidden three pages deep in Settings.
- Account deletion requires emailing support or filling a web form when the app could do it.
- Sign in with Apple absent from a consumer app.
- Password-only sign-up in 2026 (passkeys or Sign in with Apple, please).
- One-time codes typed manually instead of `.oneTimeCode` content type.
- tvOS app asking for full sign-up via remote typing.
- Account deletion that doesn't revoke Sign in with Apple tokens.

## SwiftUI

```swift
import AuthenticationServices
import SwiftUI

// Sign in with Apple, prominent on the sign-in screen.
VStack(spacing: 16) {
    Text("Welcome").font(.largeTitle.bold())
    Text("Sign in to sync your library across devices.")
        .foregroundStyle(.secondary)
        .multilineTextAlignment(.center)

    SignInWithAppleButton { request in
        request.requestedScopes = [.fullName, .email]
    } onCompletion: { result in
        switch result {
        case .success(let auth): handleAppleAuth(auth)
        case .failure(let error): showError(error)
        }
    }
    .signInWithAppleButtonStyle(.whiteOutline)
    .frame(height: 52)

    Divider().overlay(Text("or").foregroundStyle(.secondary))

    // Passkey sign-in.
    Button("Sign In with Passkey") { Task { await signInWithPasskey() } }
        .buttonStyle(.borderedProminent)
        .controlSize(.large)
        .frame(maxWidth: .infinity)

    Button("Sign In with Password") { showPasswordSheet = true }
        .buttonStyle(.bordered)
        .controlSize(.large)
        .frame(maxWidth: .infinity)
}
.padding()

// Passkey via ASAuthorizationPlatformPublicKeyCredentialProvider.
func signInWithPasskey() async {
    let provider = ASAuthorizationPlatformPublicKeyCredentialProvider(
        relyingPartyIdentifier: "example.com")
    let request = provider.createCredentialAssertionRequest(challenge: await fetchChallenge())
    let ctl = ASAuthorizationController(authorizationRequests: [request])
    ctl.delegate = /* ... */
    ctl.performRequests()
}

// Account settings — "Delete Account" is visible and explicit.
Form {
    Section("Account") {
        LabeledContent("Signed in as", value: account.email)
        Button("Sign Out") { signOut() }
        Button("Delete Account", role: .destructive) { showDelete = true }
    }
}
.confirmationDialog("Delete your account?",
                    isPresented: $showDelete,
                    titleVisibility: .visible) {
    Button("Delete Account", role: .destructive) { Task { await deleteAccount() } }
    Button("Cancel", role: .cancel) { }
} message: {
    Text("This permanently removes your account and all data. Cannot be undone.")
}
```

## See also

- [`./entering-data.md`](./entering-data.md) — field rules for signup/sign-in.
- [`./onboarding.md`](./onboarding.md) — defer account creation past onboarding.
- [`./in-app-purchase.md`](./in-app-purchase.md) — subscription implications of account deletion.
- [`./managing-notifications.md`](./managing-notifications.md) — notification opt-in.
- [`../components/text-inputs.md`](../components/text-inputs.md) — secure fields, one-time code.
- [`../foundations/accessibility.md`](../foundations/accessibility.md) — Voice Control + Sign in with Apple accessibility.
