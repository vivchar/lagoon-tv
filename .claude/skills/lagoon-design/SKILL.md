---
name: lagoon-design
description: Use for ANY interface work in Lagoon TV (the Android TV launcher in this repo) — a new screen, control, panel, animation, colour, text, a restyle or a design review. Holds the house look (tvOS 26 brought to Android TV: frosted glass, white focus without rings, capsule buttons, words instead of switches) and how to check a change with before/after screenshots. The only design skill of this project.
---

# Lagoon TV design

Lagoon TV is a launcher for a Xiaomi TV Stick (Android 11, 2 GB, weak CPU), watched from a sofa
with a D-pad remote. The look follows **tvOS 26** (Apple TV) as closely as Android 11 allows.
Tokens and component names live in `Ui.java` and `docs/design-system.md`; this skill is the
rules for using them and why. Read `docs/design-system.md` next to it.

Where these rules and Apple disagree, these win: they are Apple's rules already tried on this
stick and this owner. Where something is not covered, do what tvOS does (sources at the end),
then add the rule here.

## 1. Sizes and layout

- Design units are the 1280×720 mockup frame. Only `Ui.px(context, n)`; no dp, no raw px.
- Safe area (tvOS: 60 pt top/bottom, 80 pt sides → ~40/53 units): pages use 72 at the sides,
  56 top, 48 bottom. The dock sits 64 from the sides and 40 from the bottom.
- An overlay that covers the dock (the quick panel) uses the dock's own side and bottom insets,
  so no strip of the dock shows around it.
- **Shapes.** Buttons, chips, segments, the segmented groove and text fields are capsules:
  `Ui.PILL` (the radius is always half the height, whatever the height). Rows, cards, tiles and
  panels are rounded rectangles: floating panels over Home (dock, its tile menu, quick panel)
  share `Ui.RADIUS_PANEL`, dialogs `Ui.RADIUS_DIALOG`. Never a literal radius on a button or a panel.
- Leave room around every focusable element for its growth and shadow (see 3): parents of
  growing views get `unclip()` / `setClipChildren(false)`, and a page keeps 40 units under its
  last row, or that row's shadow is cut.

## 2. Text, colour, languages

- **No text under 15 units** (23 pt on 1080p — tvOS's floor for reading from the sofa).
  Exceptions: section caps (`Ui.caps`) 14 with tracking, the small «Видео» badge 14.
- Secondary text `TEXT_DIM` (white 75%), hints `TEXT_FAINT` (65%). Nothing fainter on glass:
  the blurred bay can be bright by day and contrast must stay ≥ 4.5:1.
- **State is said in words**, never by colour alone: «Вкл/Выкл», «включён», «Ночь · вкл»,
  «Сон через 30 мин».
- No blue for "switched on". `Ui.ON` (blue) is only an accent: the icon of an action in the
  button picker. Something switched on gets brighter glass (`GLASS_ON`) and a word.
- **Six languages** (ru default, en, th, zh, es, de). Every string lives in all six
  `res/values*/strings.xml`. Design for the longest one: labels on buttons are single-line,
  segments size to their label, dialogs with three or more buttons get wider; German and Spanish
  are the ones that break first. Thai and Chinese fall back to the system font (Inter has no glyphs).
- Name things the same everywhere: it is «Яркость» in the panel and in Settings, «Фон» for the
  bay page. A new colour or component goes into `Ui` and `docs/design-system.md` first.

## 3. Focus (the cursor)

tvOS has no focus rings. The focused element **turns white, its text and line icons turn dark,
it grows and lifts with a shadow**.

- Controls: `Ui.control` / `Ui.toggle` / `Ui.segment` backgrounds + `Ui.focusable(view)`.
  `Ui.focusable` re-inks children itself (`Ui.inkOn`): plain-coloured TextViews and VectorDrawable
  icons go dark under the cursor; pictures (app icons, photos) are left alone. A view with its own
  focus listener (the weather line on Home) calls `Ui.inkOn` itself.
- Growth `Ui.FOCUS_SCALE` 1.07 (wide rows at most 10 px a side), `translationZ` 12, 280 ms,
  `FocusRing.EASE`. Inside something tight, `Ui.growWithin(view, units)` keeps the growth inside
  (segments: 4 of the groove's 5 units) and the lift low.
- Dock tiles (pictures): no ring, no white fill — grow 1.1 (`FocusRing.TILE_SCALE`), lift 8,
  `translationZ` 18 with a rounded outline so the shadow shows.
- **The cursor and the choice never look alike.** Cursor = white, dark text, grows. Chosen value
  = `SELECTED` (brighter glass, white 32%), white text, no growth. Moving the cursor never
  changes a value; OK does.
- The shadow is only for the white state: on leaving, `translationZ` drops to 0 at once
  (`Ui.grow`). A shadow fading under translucent glass shows through as a dark square.
- **Text fields are the one exception**: they keep a white ring, as tvOS does for fields.
- Default focus is the most likely next action and **never a slider** (←/→ there would change a
  value straight away): Settings opens on its first tile, Home on the dock's first app — a note
  takes the cursor only when it arrives while Home is open. Returning restores the last focus.
  Set `nextFocus…Id` when the nearest element is not the natural next one (↓ from the quick
  panel's slider goes to the first pill, not the one under the knob).
- Never trap focus. Back always closes the page / panel / dialog / move mode.

## 4. Materials (glass)

Android 11 has no blur, and the bay plays in a `SurfaceView` no view can read. So the glass is
**fake but honest**: `Backdrop` keeps the four bay backdrops decoded tiny (96×54), box-blurred
once, mixed by the same weights as the bay behind, with Home's scrim and shades drawn over;
`Ui.Glass` draws the part of that mix under each panel, then the tint. While the clips play the
mix is the current clip's picture (they switch without a crossfade).

- **One tint for all glass: `DARK_GLASS`** — dock, notes, tile menu, dialogs, settings pages, the
  quick panel over Home (`Ui.glassBackground(v, DARK_GLASS, GLASS_LINE, radius)`). Pages once
  had a darker tint and looked dull next to the panel; don't bring separate tints back.
- Exceptions: the quick panel over another app is `Ui.Rim(PANEL_OVER_APP)` (nothing to read back
  there); the «Фон» page has no glass, plain `#0C1016` 55% — the bay itself is the preview.
- Everything lies **under** the brightness / «Ночь» dark layer, the quick panel included
  (`Dim.raise` after showing it). A panel kept above it looked brighter than the dock it covers.
- **No glass on glass.** A glass card holds plain content; segments inside a groove have no
  background of their own; a glass card never holds another glass groove.
- Control glass (`Ui.glass` → `Ui.Rim`) has a rim lit from above (`RIM_TOP` → `RIM_BOTTOM`), not
  a flat outline.

## 5. Controls

- **Buttons / pills / tiles / round buttons**: `chip()`, `Ui.pill`, `Ui.tile`, `Ui.round` —
  glass when idle, white under the cursor.
- **Segmented switch** (`segmented()`): one glass capsule, segments via `Ui.segment` (no own
  fill). The cursor moves over segments freely and **OK chooses**. tvOS chooses on focus, but on
  this remote that was tried and rejected: a slip to the right switched the light to night, and
  the cursor jumping to the chosen segment on entry felt broken. Don't bring it back.
- **No switches with knobs** (tvOS has none). An on/off setting is a row with its value on the
  right in words; OK flips it.
- **Lists**: rows of glass with the value right-aligned in `TEXT_DIM`; the focused row is white.
- **Slider**: tvOS has none. Brightness is the only one, the same `QuickPanel.brightness` row in
  the quick panel and at the top of Settings (owner's request): `Ui.Slider` inside a focusable
  row, ←/→ move it, it draws dark on the white focused row. Nothing else next to it in its row.
  Don't add other sliders; use a segmented switch or a list of values.
- **Arranging items**: long OK opens a small dialog (Переместить / Скрыть / Отмена). While an item
  is carried it is `SELECTED`, a hint at the bottom says how («← → двигать · OK или Back —
  поставить»), arrows move it along its row (↑/↓ in vertical lists), OK or Back put it down.
  Whatever can be hidden must come back: a dashed item at the end of the row («Скрытые · N»)
  appears only while something is hidden.
- The quick panel has no title and no hint line (owner's request): it covers a playing picture, and ←/→ on its slider and
  Back are obvious. Everywhere else:
- **A hidden gesture is always written down**: a page with long OK says so in its bottom hint
  («↑ или Back — на главный · удерживайте OK — переставить или скрыть»); a mode says how to leave it.
- **Dashed = not there**: dashed outline for «Добавить кнопку», «Скрытые». Under the cursor they
  are white like any control.

## 6. Motion

- Only `translation`, `scale`, `alpha`; 90–280 ms, `FocusRing.EASE`; no bounce, no frame loops.
- What there is now: focus growth and lift; **OK press** — the white control dips to 95% while OK
  is down and springs back (`Ui.press`, from `dispatchKeyEvent`); the **app name** over the dock
  rises 6 units into place; the **quick panel** rises 40 units from the bottom and fades in
  (240 ms), sinks back out on hide (180 ms); page sheets slide; the tile menu fades in.
- Nothing redraws every frame unless it plays media. Glass is redrawn only when the bay's mix
  changes (checked once a minute). Keep new motion this cheap: the stick is slow and the bay
  video is already decoding.
- `ViewPropertyAnimator` respects Android's animator scale ("remove animations") for free.

## 7. Checking a change (always, with before/after)

1. Before editing, screenshot the affected screens on the emulator (`docs/testing.md`):
   `adb -s emulator-5554 shell am start -n io.github.vivchar.lagoon/.MainActivity --es phase day
   [--es page x|scene|panel|remote|calendar|places|telegram] [--ez quick true] [--es lang de]`,
   then `adb -s emulator-5554 exec-out screencap -p > before.png`. Reset prefs between shots
   (`run-as … rm -f shared_prefs/dim.xml shared_prefs/actions.xml shared_prefs/settings_layout.xml
   shared_prefs/lang.xml`); `--es lang` sticks in the intent, so force-stop before checking without it.
2. After, the same shots; for anything on glass also day / sunset / night (`--es phase`); for
   anything with text also `de` and `th`.
3. Motion and transitions: `adb shell screenrecord`, then `ffmpeg -vf fps=30` frames — the dark
   square under a fading shadow was found this way, not on stills.
4. Compose «До / После» pairs with `magick` (labels, `+append`, `-append`) into
   `app/build/*.jpg` and send it to the owner.
5. Walk every page with the remote: white on white, clipped labels and shadows, the last row of a
   page, focus landing somewhere unexpected.
6. The stick can't be screenshotted (`screencap` returns a blank frame) and is in use: install
   on it only when asked (`docs/device.md`), and ask the owner to look.

## Known deviations from tvOS

- Segmented switches choose on OK, not on focus (see 5).
- A brightness slider (no sliders on tvOS).
- Lucide icons and Inter instead of SF Symbols and SF Pro (licensing; Inter's metrics are close).
- The quick panel over other apps can't be glass.

## Sources

- HIG: Focus and selection (the five focus states: unfocused glass, focused white and larger,
  highlighted, selected, unavailable), Sliders («Not supported in tvOS»), Segmented controls
  (tvOS: selected on focus, keep other elements away), Layout (tvOS safe area), Color
  (tvOS: never colour alone for focus), Materials (tvOS: Liquid Glass on navigation and
  Control Center, standard materials in content). https://developer.apple.com/design/human-interface-guidelines/
- tvOS 26 as shipped: Control Center tiles of glass with lit rims; Settings rows dark and
  translucent with the value on the right, the focused row white; sidebars plain text with a
  white capsule for the focused item; playback buttons glass capsules.
