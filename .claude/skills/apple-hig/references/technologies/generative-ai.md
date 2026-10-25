# Generative AI

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/generative-ai
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

Generative AI can write text, create images, produce dialogue, generate structured data. Used well, it unlocks new creativity and productivity. Used poorly, it spreads misinformation, leaks private data, replicates biases, produces copyright problems, and erodes user trust. This page is **design rules for shipping generative features responsibly on Apple platforms**, not a tutorial on prompts or model architecture.

## The core rules

### Keep people in control

- **Users decide.** Honor their requests when clear and in-scope. Handle sensitive content with care.
- **Dismiss, revert, retry.** Let users drop content they don't want, undo transformations, ask for something different.
- **Identify AI.** Make it clear when and where you use AI. Never trick someone into thinking they're interacting with a human or content authored by a human.

### Ensure inclusivity

Models trained on imperfect data replicate bias.

- **Avoid assumptions about people.** Ask for what you need rather than inferring gender, relationship, culture.
- **Test across diverse inputs.** Identify and correct stereotypes before shipping.
- **Gendered or identity-specific defaults** are a common failure mode — default to neutral where meaning permits.

### Be transparent

- **Disclose where you use AI** — inline badges, labels, tooltips. "Generated with AI" / "Written with AI assistance".
- **Set clear expectations.** Curated example prompts for open-ended features. Brief tutorials for novel capabilities. Show known limitations up front.
- **Follow regional regulations.** Some jurisdictions (EU, California) require disclosure.

### Design for privacy

- **Prefer on-device models** when possible. Apple's **Foundation Models framework** gives you on-device LLMs; no data leaves the device, faster latency, works offline.
- **Server when necessary.** Bigger models, rarer hardware. Process locally first and minimize what's shared.
- **Permission before using personal data.** Sensitive info (messages, photos, contacts, usage) gets explicit consent. Use the minimum you need.
- **Clear disclosure of storage and training use.** Tell users if data leaves the device and if it's used for training.
- **Minors and children's data** follow stricter rules — review COPPA / similar regulations for the regions where you ship.
- **Outputs can leak inputs.** Model outputs may echo sensitive prompts back. Post-process and filter.

### Provide a non-AI fallback when you can

Some experiences are AI-only by design (Genmoji). Many are complementary. When the AI is enhancement rather than essence, offer a non-AI path.

- **Summarization in notifications** — optional, keep reading the notification verbatim.
- **Writing tools** — grammar check is AI; typing is not.
- **Image generation** — creative feature; a standard emoji picker still exists.

## Model choice

- **On-device first.** Fast, private, available offline, zero per-call cost. Limited capability vs. frontier models.
- **Server models** for bigger context, more reasoning, multimodal tasks. Higher latency, privacy tradeoffs, cost.
- **Availability varies.** Some features need a compatible device (Apple Intelligence). Handle unavailable cases gracefully — disable the feature with explanation, don't crash.

## Dataset and training

- **Know where your data comes from.** Licensing, provenance, consent.
- **Diverse datasets** mitigate bias. Evaluate the model across a range of subjects and scenarios.
- **Real-world data is imperfect.** Testing isn't a one-time event — monitor and iterate.

## Inputs — guide the user

### Guide users toward good prompts

- **Predefined example prompts** that hint at capabilities.
- **Suggested edits / templates** for text generation ("Make it friendlier", "Shorten").
- **Category chips** for image generation ("Abstract", "Cartoon", "Portrait").

### Minimize hallucinations

- **Scope tightly.** Don't ask the model for facts it doesn't have reliable grounding for. Dates, prices, policy details, medical info — either ground with tool use (RAG, APIs) or don't generate.
- **Disclose the risk.** "AI-generated content may contain errors." Make this visible, not buried.
- **Don't use AI in safety-critical contexts** where a hallucination could harm.
- **Verify before acting.** If the model suggests an action (send email, delete file), confirm before execution.

### Confirm destructive actions

- **Never auto-delete, auto-purchase, auto-send** on AI's say-so.
- **Always confirm** significant or hard-to-undo actions.
- **Some regions regulate AI-driven decisions** (credit, hiring, medical, legal). Check local law.

## Outputs — handle what comes back

### Content filtering

- **Unexpected and harmful outcomes happen** — accidental or intentional misuse, sensitive topics.
- **Design and test to minimize.** Try poorly phrased, vague, ambiguous, sensitive, controversial, adversarial requests. Use what you learn to harden.
- **Scoped refusals.** When blocked, coach users to ask differently. Image Playground says "Unable to use that description" + suggests alternatives — don't just fail.
- **Tell people why** when a specific output can't be produced.

### Avoid replicating copyrighted content

- **Build on models that protect against copyright reproduction.**
- **Curate inputs.** Pre-approved prompt sets, explicit "avoid X style" instructions.
- **Post-filter** for near-duplicates of known-copyrighted content.

### Latency

- **Generative models take seconds, not milliseconds.** Design a loading experience that:
  - Shows work happening (progress, shimmer).
  - Keeps the user oriented (task name, expected duration).
  - Lets the user cancel.
- **Background generation** when it makes sense — user does something else while the model runs.

### Multiple results

- **Often better than one.** Three options let users pick; a single result puts the model's interpretation in tension with the user's intent.
- **Image Playground**-style: show a grid of variations.
- **Writing rewrites**: show 2–3 alternatives with different tones.

## Continuous improvement

- **Plan for updates.** Models improve; your prompt engineering ages.
- **Swap-friendly architecture.** Separate model from UX so you can upgrade.
- **Fine-tune on new data** when you own the model.
- **Retest after every model change.**

## Feedback

- **Let users share feedback** on outputs.
- **Thumbs up / thumbs down** at minimum. Detailed feedback for complex issues.
- **Voluntary and non-interrupting.** Don't block the flow.
- **Resolve feedback quickly.** Users who report and see no action stop reporting.

## Accessibility

- **Generated content inherits accessibility obligations.**
  - Generated images need alt text — have the model generate one, or let users edit.
  - Generated documents need headings, lists, structure.
  - Generated audio needs transcripts.
- **Dynamic Type, VoiceOver, Reduce Motion** apply to AI-driven surfaces.
- **Test AI features with assistive technology enabled.**

## Apple Foundation Models

Apple's on-device LLM framework (iOS 26+/macOS 26+) gives you access to a capable on-device model.

- **Swift-native.** `FoundationModel` API.
- **Private by design.** Nothing leaves the device.
- **Works on compatible hardware with Apple Intelligence enabled.**
- **Design for availability variance.** Some users won't have it — provide a fallback or disable the feature gracefully.

## Common mistakes

- **No disclosure** that content is AI-generated.
- **AI-generated factual claims without grounding.**
- **Auto-delete / auto-send** based on AI suggestions.
- **No cancel** during a multi-second generation.
- **Gendered / identity-specific defaults.**
- **Prompt inputs echoed back in outputs** (privacy leak).
- **No fallback** when Apple Intelligence is unavailable.
- **Feedback mechanism absent** — can't learn from user reactions.
- **No alt text on generated images.**
- **Feedback button that interrupts the flow every time.**
- **Server-only when on-device would suffice** — avoidable privacy cost.
- **No testing across diverse prompts** — bias slips through.
- **Models treated as magic** — not versioned, not A/B tested, not monitored after ship.

## SwiftUI — Apple Foundation Models sketch

```swift
import FoundationModels

@MainActor
@Observable final class Composer {
    var input: String = ""
    var output: String = ""
    var isGenerating: Bool = false
    var error: String?

    private var task: Task<Void, Never>?

    func rewrite(tone: Tone) {
        task?.cancel()
        isGenerating = true
        error = nil

        task = Task {
            do {
                let model = try await FoundationModel.load(.default)
                let prompt = """
                Rewrite the following text in a \(tone.label) tone. \
                Preserve meaning. Keep roughly the same length.

                Text:
                \(input)
                """
                let stream = try await model.stream(prompt: prompt)
                output = ""
                for try await chunk in stream {
                    output += chunk
                }
            } catch is CancellationError {
                // user canceled; no error to display
            } catch {
                self.error = "Rewrite failed. Please try again."
            }
            isGenerating = false
        }
    }

    func cancel() {
        task?.cancel()
        isGenerating = false
    }
}

enum Tone: String, CaseIterable, Identifiable {
    case friendly, professional, concise
    var id: String { rawValue }
    var label: String { rawValue }
}

struct ComposeView: View {
    @State private var composer = Composer()

    var body: some View {
        VStack(spacing: 12) {
            TextField("Your message", text: $composer.input, axis: .vertical)
                .textFieldStyle(.roundedBorder)
                .lineLimit(3...8)

            HStack {
                ForEach(Tone.allCases) { tone in
                    Button(tone.label) { composer.rewrite(tone: tone) }
                        .buttonStyle(.bordered)
                }
                Spacer()
                if composer.isGenerating {
                    ProgressView()
                    Button("Cancel") { composer.cancel() }
                }
            }

            if let error = composer.error {
                Label(error, systemImage: "exclamationmark.triangle.fill")
                    .foregroundStyle(.red)
                    .font(.footnote)
            }

            VStack(alignment: .leading, spacing: 4) {
                Label("Rewritten with AI", systemImage: "sparkles")
                    .font(.caption).foregroundStyle(.secondary)
                Text(composer.output)
                    .textSelection(.enabled)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding()
            .background(.thinMaterial, in: .rect(cornerRadius: 12))

            HStack {
                Button("Use") { /* replace input, log positive feedback */ }
                Button("Try Again") { composer.rewrite(tone: .friendly) }
                Spacer()
                Button("Feedback") { /* thumbs up/down sheet */ }
                    .buttonStyle(.borderless)
                    .foregroundStyle(.secondary)
            }
        }
        .padding()
    }
}
```

## See also

- [`../foundations/accessibility.md`](../foundations/accessibility.md) — generated content inherits a11y.
- [`../foundations/writing.md`](../foundations/writing.md) (pending) — AI-generated copy tone.
- [`../foundations/privacy.md`](../foundations/privacy.md) (pending) — consent, permissions, data handling.
- [`../foundations/motion.md`](../foundations/motion.md) — loading animations for long generations.
- [`../patterns/loading.md`](../patterns/loading.md) — multi-second generation UX.
- [`../patterns/feedback.md`](../patterns/feedback.md) — rating / thumbs-up-down patterns.
- [`./shortcuts-and-siri.md`](./shortcuts-and-siri.md) — AI-driven App Intents.
