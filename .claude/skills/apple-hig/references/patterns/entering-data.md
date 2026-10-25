# Entering data

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/entering-data
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

Every form is a tax on the user. The best form is the one that isn't there. If you can get the data from the system, pre-fill a reasonable default, or offer a choice instead of typing — do that first.

## Rules

- **Get info from the system whenever possible.** Don't ask for zip, city, state, or country when `CNContactStore` / `CLGeocoder` / `MKLocalSearch` can derive them. Don't ask for timezone, language, locale, device name, or account email when the system knows.
- **Be specific about what you need.** Label every field. Placeholder text can teach format ("`username@company.com`") but never replaces a visible label.
- **Pre-fill reasonable defaults.** Cut cognitive load, speed entry, and make "just use defaults" a one-tap path.
- **Secure fields for sensitive input.** Passwords, PINs, card CVV. Use `SecureField` / `UITextField.isSecureTextEntry` / `NSSecureTextField`. Never prepopulate a password field — always require re-entry or biometric/keychain auth.
- **Prefer choices over typing.** Pickers, menus, segmented controls, toggles, radio groups. Scan + tap beats typing on every platform, especially tvOS and watchOS.
- **Support drag-drop and paste.** Let the user feed data from the clipboard or another app. Never require typing data they already have copied.
- **Validate dynamically.** See [`../components/text-inputs.md`](../components/text-inputs.md#validation). As-you-type for password rules; on-blur for email/phone/URL format; on-submit for cross-field and server checks.
- **Number formatters for numeric input.** Localize automatically. Restrict non-numeric entry. Format decimals, percentages, currency.
- **Disable submit until the form is valid.** Don't surprise users with errors after they tap Continue — make the required state visible up front.

## Keyboard types (iOS / iPadOS / visionOS)

Match the keyboard to the content — huge usability win.

| Field | Keyboard | `textContentType` |
|---|---|---|
| Email | `.emailAddress` | `.emailAddress` |
| Phone | `.phonePad` / `.namePhonePad` | `.telephoneNumber` |
| URL | `.URL` | `.URL` |
| Number (unlabeled) | `.numberPad` | — |
| Decimal | `.decimalPad` | — |
| Name | `.default` (cap words) | `.name` / `.givenName` / `.familyName` |
| Password | default, secure | `.password` / `.newPassword` |
| One-time code (2FA) | `.numberPad` | `.oneTimeCode` |
| Search | `.default` with search return | — |
| Twitter/mention | `.twitter` | — |

Set `textContentType` on every field — it unlocks **AutoFill** from iCloud Keychain, **strong password suggestions**, **passkey detection**, and **one-time code pickup from Messages**.

## Secure fields

- **Visible secure field** masks input with dots. Don't display characters.
- **Password field:** `textContentType = .password` for sign-in; `.newPassword` for account creation (triggers strong-password suggestion).
- **Biometric auth** where possible — Face ID / Touch ID on compatible devices via `LocalAuthentication`.
- **visionOS** automatically blurs secure fields when the user is AirPlaying — don't override.

## Required vs optional

- **Mark required fields clearly** if some are optional.
- **Fewer asterisks than you think.** Most signup forms should require 2–3 fields. The rest is post-signup.
- **Progressive disclosure.** Collect what you need to get started; ask for more as the user needs it.

## Input alternatives

- **Dictation** — a dictation button on the iPadOS / iOS / visionOS keyboard.
- **Scribble** (Apple Pencil) — writes into any text field.
- **Voice Control** — users say "tap Email" to focus, then dictate.
- **Bluetooth keyboard** — users on iPad + keyboard / Mac are typists. Handle tab, shift-tab, return, esc correctly.
- **Paste** — users paste credentials from password managers. Don't block.

## Platform specifics

### iOS / iPadOS / visionOS
Keyboard types + content type hints + AutoFill are first-class. Use them.

### macOS
- **Expansion tooltip** — pointer hover over a clipped field shows full value. Enable for long values.
- **Focus ring** appears automatically on text fields. Don't suppress.
- **Tab moves focus forward** — wire the order correctly.

### tvOS
- **Minimize text entry.** Every letter costs time.
- **Continuity keyboard** — when a user focuses a text field in tvOS, their iPhone can accept the input.
- **Voice search via Siri Remote.**
- **Prefer prebuilt choices** whenever possible.

### watchOS
- **Dictation, Scribble, predefined choices.** Only show a text field when nothing else works.
- **One field at a time.** Don't stack forms on a watch.

## Common mistakes

- Asking for timezone / locale / country when the system knows.
- Placeholder text as the only label.
- Default keyboard for phone, email, URL — users type workarounds.
- No `textContentType` — AutoFill doesn't work.
- Password field that autofills from clipboard with prior password.
- Secure field with "show password" as the default state.
- Long form at signup when progressive disclosure would work.
- Required asterisks on every field.
- No validation until submit.
- Fields that move while the user is typing.
- Autocomplete that blocks a user finishing their input.
- tvOS app with multi-field forms.
- watchOS form instead of dictation / picker.

## SwiftUI

```swift
@State private var email = ""
@State private var password = ""
@FocusState private var field: Field?
enum Field { case email, password }

Form {
    Section {
        TextField("you@example.com", text: $email)
            .textContentType(.emailAddress)
            .keyboardType(.emailAddress)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .submitLabel(.next)
            .focused($field, equals: .email)
            .onSubmit { field = .password }

        SecureField("Password", text: $password)
            .textContentType(.password)
            .submitLabel(.go)
            .focused($field, equals: .password)
            .onSubmit { signIn() }
    } header: {
        Text("Sign in")
    }

    Section {
        Button("Sign In") { signIn() }
            .buttonStyle(.borderedProminent)
            .disabled(!isValid)
    }
}
.onAppear { field = .email }

// 2FA code, pre-parses from Messages.
TextField("6-digit code", text: $code)
    .textContentType(.oneTimeCode)
    .keyboardType(.numberPad)

// Phone.
TextField("(555) 555-5555", text: $phone, format: .phoneNumber)
    .textContentType(.telephoneNumber)
    .keyboardType(.phonePad)
```

## See also

- [`../components/text-inputs.md`](../components/text-inputs.md) — text field component rules.
- [`../components/pickers-and-menus.md`](../components/pickers-and-menus.md) — pickers beat text entry for bounded sets.
- [`../inputs/apple-pencil.md`](../inputs/apple-pencil.md) — Scribble rules.
- [`../patterns/managing-accounts.md`](./managing-accounts.md) — signup fields.
- [`../patterns/feedback.md`](./feedback.md) — inline validation feedback.
