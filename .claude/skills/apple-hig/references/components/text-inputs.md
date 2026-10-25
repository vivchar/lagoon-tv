# Text inputs — text fields, secure fields, combo boxes

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/text-fields (plus combo-boxes on macOS)
**Last verified:** 2026-04-20
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS (combo boxes: macOS only)

A text field is for **small, specific** pieces of text — a name, an email, a search query. For longer input, use a text view. For sensitive input, use a secure field. On macOS only, a combo box combines a field with a list of defaults.

## Rules that apply everywhere

- **Match field size to expected content.** Users visually estimate how much to type from the box. A 200pt-wide field for "Zip code" is misleading; a 40pt-wide field for "Full name" is punishing.
- **Labels first.** Persistent label next to or above the field. **Placeholder text is not a label** — it disappears when the user starts typing, leaving Voice Control / VoiceOver users without context.
- **Placeholder text describes the purpose** or hints at format ("you@example.com"). Sentence case.
- **Logical tab order.** Tabbing between fields moves in reading order. SwiftUI and UIKit do this automatically most of the time; override only when you have a reason.
- **Validate at the right moment.**
  - **Email / phone / URL:** validate when the user moves to the next field (don't yell while they're still typing).
  - **Password creation / username:** validate as they type (requirements visible and live).
  - **Form submit:** always validate everything one more time.
- **Use a number formatter for numeric input.** Don't roll your own — formatters localize correctly.
- **Secure field for any sensitive input** (password, PIN, card CVV). `SecureField` / `UITextField.isSecureTextEntry` / `NSSecureTextField`.
- **Don't ship placeholder-only labels.** See [`../foundations/accessibility.md`](../foundations/accessibility.md) — every input has a visible accessible label.

## Line breaking, truncation

- By default, the system clips text extending beyond the field bounds.
- You can wrap at character or word level, or truncate at start, middle, or end.
- **Expansion tooltip** — the same tooltip style used elsewhere — can show the full value when the pointer hovers over a clipped field (macOS, visionOS).

## Layout of multiple fields

- **Evenly space** multiple fields. Users need to see which label belongs with which field.
- **Stack vertically** when possible. Easier to scan than a dense grid.
- **Consistent widths** inside groupings. Example: first name + last name same width; address + city different but consistent within their rows.

## iOS, iPadOS

- **Keyboard type matches content.** `.emailAddress`, `.numberPad`, `.phonePad`, `.URL`, `.decimalPad`, `.asciiCapable`. Huge usability win — numeric keypads for phone numbers, @-key for emails, etc.
- **Clear button** (trailing `×`) — add it to long fields so people can reset in one tap rather than holding Delete. `.clearButtonMode = .whileEditing` on `UITextField`; in SwiftUI use `.overlay(alignment: .trailing)` with a button that checks `focusedField`.
- **Leading image/symbol** indicates purpose ("search" magnifying glass). Trailing image/button offers features ("bookmark", "favorite").
- **Auto-capitalization + auto-correction** per context. Email field: no caps, no autocorrect. Name field: words-caps, no autocorrect.
- **Content type hints** enable AutoFill, strong passwords, and passkey suggestions: `.textContentType(.emailAddress)`, `.textContentType(.password)`, `.textContentType(.oneTimeCode)`, `.textContentType(.newPassword)`. Use these — Apple users expect AutoFill.
- **Submit label** appropriate to the action: `.submitLabel(.done)`, `.search`, `.send`, `.continue`, `.next`.
- **`.onSubmit { }`** ties Return key to an action.

## macOS

- **Field chrome is the system's.** Don't restyle borders or backgrounds.
- **Focus ring** appears automatically. Don't suppress it.
- **Tab** moves forward; Shift-Tab moves backward.
- **Find and Replace** — if your app has editable text, provide a Find command in the Edit menu (`CMD-F`).
- **Combo box** (macOS only) — field + pull-down list of predefined values. Use when you have common defaults but want custom input to be legit. Rules:
  - Default the field with a meaningful value from the list.
  - Introductory label, title case, trailing colon.
  - Provide **relevant** list items (most-likely choices).
  - List item width ≤ text field width, otherwise truncation hurts.
- **Secure text fields** render with `•`. Never display the characters.
- **Tooltips** on all non-obvious fields.

## visionOS

Same rules as iOS. Keyboard appears in the user's field of view; position the field thoughtfully so the keyboard isn't awkward. Submit labels and content types work the same.

## tvOS

- **Minimize text entry.** Text fields on a TV are a pain (remote keypad). Every field is a fight.
- Prefer **list selection, pickers, or dictation** over open text fields.
- If text entry is truly required, support **continuity keyboard** (enter text via the iPhone Keyboard that appears when you open a text field on Apple TV).

## watchOS

- **Minimize text entry.** Dictation, Scribble, or predefined choices are almost always better.
- Show a text field only when nothing else suffices.

## Security

- **Use `.textContentType(.password)` / `.newPassword`** for password fields — unlocks iCloud Keychain AutoFill + strong password suggestions.
- **Use `.textContentType(.oneTimeCode)`** for 2FA inputs — picks up codes from Messages automatically.
- **Never log the contents of a secure field.**
- **Disable screenshots** for views with sensitive data only when strictly required (users rightly dislike the black screen).

## Common mistakes

- Placeholder as the only label ("Email" vanishes on typing; accessibility + memory fail).
- Wrong keyboard type (default keyboard for phone numbers).
- No content type hint — AutoFill and strong-password don't work.
- Validation that screams while the user is still typing.
- No Cancel / Clear on a long-typed field.
- Uniformly wide fields for zip / country / name — looks cheap.
- Restyling macOS field borders.
- Overriding the macOS focus ring.
- Open text fields on tvOS / watchOS where a picker or dictation would work.

## SwiftUI

```swift
// iOS — email + password with AutoFill, validation on blur.
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
        Button("Sign in", action: signIn)
            .buttonStyle(.borderedProminent)
            .disabled(email.isEmpty || password.isEmpty)
    }
}

// One-time code field for 2FA.
@State private var code = ""
TextField("Enter the 6-digit code", text: $code)
    .textContentType(.oneTimeCode)
    .keyboardType(.numberPad)

// macOS — a TextField inside a Form renders with correct chrome.
Form {
    TextField("Name", text: $name, prompt: Text("Your full name"))
    TextField("Email", text: $email, prompt: Text("you@example.com"))
        .textContentType(.emailAddress)
    SecureField("Password", text: $password)
}
.formStyle(.grouped)
```

## UIKit

```swift
// iOS — email field with AutoFill, clear button, proper keyboard.
let email = UITextField()
email.translatesAutoresizingMaskIntoConstraints = false
email.placeholder = "you@example.com"
email.borderStyle = .roundedRect
email.textContentType = .emailAddress
email.keyboardType = .emailAddress
email.autocapitalizationType = .none
email.autocorrectionType = .no
email.returnKeyType = .next
email.clearButtonMode = .whileEditing
email.accessibilityLabel = "Email"
email.delegate = self

extension ViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ tf: UITextField) -> Bool {
        if tf === email { password.becomeFirstResponder() }
        else { signIn() }
        return true
    }
}

// Secure password.
let password = UITextField()
password.isSecureTextEntry = true
password.textContentType = .password
```

## AppKit

```swift
// Rounded field, focus ring automatic.
let name = NSTextField()
name.placeholderString = "Your full name"
name.bezelStyle = .roundedBezel

// Secure field.
let password = NSSecureTextField()
password.placeholderString = "Password"

// Combo box — field + list of defaults.
let size = NSComboBox()
size.placeholderString = "Paper size"
size.usesDataSource = false
size.addItems(withObjectValues: ["US Letter", "Legal", "A4", "A5"])
size.completes = true
size.numberOfVisibleItems = 6
size.setContentCompressionResistancePriority(.defaultHigh, for: .horizontal)
```

## See also

- [`../foundations/accessibility.md`](../foundations/accessibility.md) — VoiceOver label and trait rules.
- [`../foundations/typography.md`](../foundations/typography.md) — Dynamic Type in fields.
- [`./buttons.md`](./buttons.md) — submit / primary action on forms.
- [`./pickers-and-menus.md`](./pickers-and-menus.md) — when a picker beats a text field.
- [`../patterns/entering-data.md`](../patterns/entering-data.md) (pending) — form design.
- [`../inputs/pointer-and-keyboard.md`](../inputs/pointer-and-keyboard.md) (pending) — keyboards and shortcuts.
