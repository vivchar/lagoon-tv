# Sliders, steppers, toggles (switches / checkboxes / radio buttons)

**HIG sources:**
- https://developer.apple.com/design/human-interface-guidelines/sliders
- https://developer.apple.com/design/human-interface-guidelines/steppers
- https://developer.apple.com/design/human-interface-guidelines/toggles

**Last verified:** 2026-04-21
**Applies to:** varies per component (see below)

Three value-changing controls. Pick the right one for the data type:

| Control | Best for | Platforms |
|---|---|---|
| **Slider** | Continuous values over a range | iOS, iPadOS, macOS, visionOS, watchOS |
| **Stepper** | Small incremental integer changes (often paired with a text field) | iOS, iPadOS, macOS, visionOS |
| **Toggle / switch** | On-off state for a single setting | all |
| **Checkbox** | On-off state in a hierarchy; mixed state | macOS |
| **Radio button** | 2–5 mutually exclusive options | macOS |

## Sliders

A horizontal (or vertical) track with a **thumb** that slides between a minimum and maximum. The track fills with color between minimum and thumb.

### Rules

- **Familiar directions.** Minimum on the leading side (or bottom for vertical), maximum on the trailing side (or top). Don't invert.
- **Icons for min/max meaning** (small-image ↔ large-image icon for a size slider).
- **Pair with a text field + stepper for wide ranges.** The slider gives coarse control; the field accepts exact values.
- **Don't use sliders for audio volume.** Use the system volume view (`MPVolumeView` or SwiftUI equivalents) — it includes output routing and respects the hardware volume controls.
- **Live feedback** (macOS) — show the effect as the user drags, don't wait for `.onEnded`.

### Styles (macOS)

- **Linear** (horizontal or vertical) with optional tick marks — ideal for fixed start/end ranges.
- **Circular** — for values that wrap (rotation degrees, repeating cycles).
- **Tick marks** help users hit specific values. Label min/max always; label intermediate ticks only if useful.
- **Tooltips on hover** to display the current thumb value.

### Platform notes

- **iOS / iPadOS / visionOS:** standard horizontal slider. visionOS prefers horizontal over vertical (easier gesture).
- **watchOS:** `Slider` presents as a horizontal track with `+` / `-` tap targets on either end. Supply custom glyphs when plus/minus doesn't communicate meaning.
- **tvOS:** sliders are not supported.

## Steppers

A two-segment `−` / `+` control for **integer increments**. Sits next to the value it's changing; does not display the value itself.

### Rules

- **Always label the value.** The stepper is buttons; the context is a number somewhere next to it.
- **Pair with a text field** when large values are expected (a stepper for 1-of-500 pages is punishing; a field is faster).
- **macOS:** Shift-click can change the value by 10× for large ranges.
- **Not supported on watchOS or tvOS.**

## Toggles

The SwiftUI `Toggle` renders as a switch on iOS / iPadOS / visionOS / watchOS / (in list rows) macOS, and as a checkbox or switch on macOS depending on context. All of them communicate **a single on/off state**.

### Switch toggle rules

- **Use in list rows on iOS / iPadOS.** The surrounding row label provides context — you don't need an extra label.
- **Default color green on iOS / iPadOS.** Change only when the accent color provides meaningful continuity with your app; ensure contrast is acceptable.
- **Outside a list row, don't use a switch.** Use a button that behaves like a toggle (icon button with an alt state) — the Phone app's "Filter" toolbar button is the canonical example.
- **Visual difference must be perceivable beyond color alone** — on/off also differs in position, and (in the macOS switch) in a visible control fill.

### macOS — when to use switch vs checkbox

- **Switch for emphasized settings.** More visual weight than a checkbox; right for settings that control a group (sync on/off for everything).
- **Mini switch** in grouped forms — matches button height for consistent row heights. Use a regular switch for the parent and mini switches for subordinates.
- **Checkbox for hierarchies or mixed state.** Alignment and indentation show subordination. Mixed state (`-`) when subordinates disagree.
- **Don't replace existing checkboxes with switches** just because switches are newer.

### Checkboxes (macOS)

- **Small square button** with a checkmark when on, a dash when mixed.
- **Title on the trailing side** (by default).
- **Empty in a checklist row** when the row itself is the label.
- **Use for hierarchical settings** — alignment conveys relationships that switches can't.
- **Mixed state** on a parent checkbox when children disagree.

### Radio buttons (macOS)

- **2–5 mutually exclusive options.**
- **More than ~5 → use a pop-up button or picker.** Radio buttons get visually noisy fast.
- **Prefer radio buttons over a single switch when there are 3+ options.**
- **Prefer a checkbox over a radio pair when the choice is binary** — the checkmark-present/absent is faster to read than two radio states.
- **Consistent horizontal spacing** — measure to the longest label; use that everywhere.

### Don't mix toggles and buttons in the same group

A switch says "state." A button says "action." Side by side, the meanings blur.

## Accessibility

- **VoiceOver reads the state** ("switch, on / off"; "checkbox, checked / unchecked").
- **Hit target ≥44pt on iOS/iPadOS/watchOS, ≥28pt on macOS, ≥60pt on visionOS** — see [`../foundations/accessibility.md`](../foundations/accessibility.md).
- **Reduce Motion** — switch flip animations respect the setting.
- **Dynamic Type** — row labels next to switches scale; layout must accommodate.

## Common mistakes

- Slider for audio volume (use the system volume view).
- Slider with inverted direction (max on the leading side).
- Stepper with no visible value.
- Switch outside a list row on iOS.
- Switch where a radio group would be clearer (>2 states).
- Radio button that also toggles — that's a switch.
- Checkbox in a toolbar (macOS — not where they belong).
- Mixed checkbox state never set when children disagree.
- Relying only on color to show on/off (fails color-blind users and Increase Contrast).
- Wide-range slider without a text field companion.
- Stepper for large ranges (imagine clicking `+` 300 times).

## SwiftUI

```swift
// Slider with icons and a paired text field for wide range.
@State private var fontSize: Double = 14

HStack(spacing: 12) {
    Image(systemName: "textformat.size.smaller").accessibilityHidden(true)
    Slider(value: $fontSize, in: 8...72, step: 1) {
        Text("Font size")
    } minimumValueLabel: {
        Text("8").font(.caption)
    } maximumValueLabel: {
        Text("72").font(.caption)
    }
    Image(systemName: "textformat.size.larger").accessibilityHidden(true)
    TextField("Size", value: $fontSize, format: .number)
        .frame(width: 60)
        .textFieldStyle(.roundedBorder)
    Stepper("Size", value: $fontSize, in: 8...72, step: 1)
        .labelsHidden()
}

// Switch toggle in a list row (iOS / iPadOS).
Form {
    Section("Notifications") {
        Toggle("Allow notifications", isOn: $allowNotifications)
        Toggle("Include badges", isOn: $includeBadges)
            .disabled(!allowNotifications)
    }
}

// macOS — mini switch in a grouped form with a parent/child hierarchy.
Form {
    Section {
        Toggle("Sync automatically", isOn: $autoSync)
        Toggle("Sync on cellular", isOn: $syncCellular)
            .controlSize(.mini)
            .disabled(!autoSync)
    }
}
.formStyle(.grouped)

// macOS — mixed-state checkbox (three-state toggle).
@State private var allStyles: Bool? = false   // nil when mixed; use custom binding for tri-state

Toggle(sources: [$bold, $italic, $underline], isOn: \.self) {
    Text("Any style")
}   // SwiftUI aggregates and shows mixed state automatically.

// macOS — radio group.
@State private var theme: Theme = .system
Picker("Appearance", selection: $theme) {
    Text("System").tag(Theme.system)
    Text("Light").tag(Theme.light)
    Text("Dark").tag(Theme.dark)
}
.pickerStyle(.radioGroup)

// Button-style toggle (icon with state).
@State private var isFiltering = false
Button {
    isFiltering.toggle()
} label: {
    Label("Filter", systemImage: "line.3.horizontal.decrease.circle\(isFiltering ? ".fill" : "")")
}
.buttonStyle(.bordered)
.tint(isFiltering ? .accentColor : .secondary)
.accessibilityAddTraits(isFiltering ? .isSelected : [])
```

## UIKit

```swift
// Slider.
let slider = UISlider()
slider.minimumValue = 8
slider.maximumValue = 72
slider.setValue(14, animated: false)
slider.minimumValueImage = UIImage(systemName: "textformat.size.smaller")
slider.maximumValueImage = UIImage(systemName: "textformat.size.larger")
slider.addTarget(self, action: #selector(fontSizeChanged(_:)), for: .valueChanged)

// Stepper + text field pair.
let stepper = UIStepper()
stepper.minimumValue = 1
stepper.maximumValue = 500
stepper.stepValue = 1
stepper.value = 1
stepper.addTarget(self, action: #selector(copiesChanged(_:)), for: .valueChanged)

// Switch in a cell.
let sw = UISwitch()
sw.isOn = allowNotifications
sw.addTarget(self, action: #selector(toggleNotifications(_:)), for: .valueChanged)
cell.accessoryView = sw
```

## AppKit

```swift
// Slider with tick marks.
let slider = NSSlider(value: 14, minValue: 8, maxValue: 72,
                      target: self, action: #selector(fontSizeChanged(_:)))
slider.numberOfTickMarks = 9
slider.allowsTickMarkValuesOnly = false
slider.toolTip = "Drag to resize"

// Stepper.
let stepper = NSStepper()
stepper.minValue = 1
stepper.maxValue = 500
stepper.increment = 1
stepper.integerValue = 1
stepper.valueWraps = false

// Switch — NSSwitch in macOS 10.15+.
let sw = NSSwitch()
sw.state = allowNotifications ? .on : .off
sw.target = self
sw.action = #selector(toggleNotifications(_:))

// Checkbox with mixed state.
let cb = NSButton(checkboxWithTitle: "Any style",
                  target: self, action: #selector(toggleStyles(_:)))
cb.allowsMixedState = true
cb.state = mixed ? .mixed : (on ? .on : .off)

// Radio buttons — use a single matrix or three NSButtons with shared action.
let buttons = ["System", "Light", "Dark"].map { title -> NSButton in
    let b = NSButton(radioButtonWithTitle: title, target: self,
                     action: #selector(themeChanged(_:)))
    b.identifier = NSUserInterfaceItemIdentifier(title)
    return b
}
```

## See also

- [`./buttons.md`](./buttons.md) — button-style toggles (icon button with a state).
- [`./pickers-and-menus.md`](./pickers-and-menus.md) — when a picker beats a radio group.
- [`./text-inputs.md`](./text-inputs.md) — the text field half of the field + stepper pattern.
- [`../foundations/accessibility.md`](../foundations/accessibility.md) — hit targets and a11y for value controls.
