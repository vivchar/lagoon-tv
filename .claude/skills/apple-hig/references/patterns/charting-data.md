# Charting data

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/charting-data
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

A chart communicates a pattern in data faster than prose can. On Apple platforms, **Swift Charts** is the right tool — declarative, accessible, matches the visual aesthetic of the OS. A chart that requires a lot of explanation usually isn't working.

## When to chart

- **Highlight important information.** Trends, anomalies, totals at a glance.
- **Historical or predicted values.** Time series.
- **Current state of a changing quantity** — speed, temperature, bandwidth.
- **Multi-dimensional comparison** — categories, products, regions.

## When NOT to chart

- **The data is small and the user just needs to see it.** Use a list or table.
- **The user needs to sort, filter, or scan individual values.** Chart first is an obstacle; use a table with optional chart toggle.
- **Decorative visualization** — don't add a chart because it looks cool.

## Rules

- **Prefer common chart types.** Bar, line, area, scatter, pie (sparingly), donut. Users know how to read them. Invent a new type only when the data genuinely demands it.
- **If you invent a type, teach it.** Animate the chart on first view, caption what each axis means. Activity Rings teach their mapping through animation on pairing.
- **Keep it simple.** Don't pack every dimension into one chart. If the data is dense, let users expand / drill into detail.
- **Progressive disclosure.** Start with the aggregate (total, trend, most important data point). Let users tap to explore subsets.
- **Descriptive headline or summary.** "Chance of light rain in the next hour" above Weather's hourly forecast. Puts the actionable takeaway in plain text; chart reinforces visually.
- **Annotations for non-obvious values.** Peak, average, threshold crossed. Don't assume users will eyeball it.
- **Consistent chart types across an app.** Multiple charts for different datasets → same type unless you have a reason to signal difference.
- **Consistent visuals for the same data across charts.** Same colors, marks, styles. If Health shows a trend with a particular blue line, the detail view uses the same blue line.
- **Size to content + functionality.** Tiny chart for glanceable state. Full-screen chart for interactive exploration.

## Accessibility

Charts are visual. The non-visual user loses everything unless you compensate.

- **Accessibility labels on every chart.** Describe the chart type, axes, and the dominant trend.
- **Accessibility values per data point.** VoiceOver reads individual values.
- **`AXChartDescriptor`** (UIKit) or SwiftUI's `.accessibilityChartDescriptor(...)` — a structured description of the chart for VoiceOver and accessibility exploration.
- **Audio graphs** — VoiceOver can play a chart as a series of tones. Swift Charts exposes this for free.
- **Headline / summary text** — works as a natural language alternative to the visual.
- **Color is never the only differentiator.** Use shape, pattern, or label alongside color.
- **Dynamic Type** on chart labels. Test at AX5.
- **Reduce Motion** — disable chart intro animations when set.

## Platform specifics

No platform-specific rules beyond the above. Swift Charts works the same across platforms.

- **Apple Watch** — glanceable only; no interactivity. Usually tiny, non-interactive charts summarizing trends.
- **tvOS** — focus-driven; chart items must be focusable if the user interacts.
- **visionOS** — 3D charts are possible but usually unnecessary. Prefer 2D.

## Writing chart copy

- **Title that names the data.** "Daily steps this week" not "Chart".
- **Short summary** under the title. "Average 8,500 / day — 4% below last week."
- **Axis labels** when needed. Skip when the scale is obvious (hourly = implicit).
- **Unit in axis or annotation.** "km / h" — don't expect users to infer.

## Common mistakes

- Chart with no headline — users hunt for the point.
- Pie chart with 15 slices.
- Stacked bar chart without a legend.
- Multi-line chart without clear color differentiation (fails for color-blind users).
- Color as the only encoding.
- Charts with tiny text labels (< 11pt, no Dynamic Type support).
- No accessibility labels — VoiceOver reads "chart" and nothing else.
- Custom chart type invented for a standard pattern (you wanted a bar chart — ship a bar chart).
- Interactivity buried behind a long tap without feedback.
- Chart shown during loading (no data → show skeleton or summary text, not an empty chart).

## Swift Charts (SwiftUI)

```swift
import Charts
import SwiftUI

struct StepsChart: View {
    let history: [DailySteps]   // date + count

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Daily steps this week")
                .font(.title3.weight(.semibold))
            Text("Average \(averageFormatted) / day — \(trendDescription) last week")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            Chart(history) { day in
                BarMark(
                    x: .value("Day", day.date, unit: .day),
                    y: .value("Steps", day.count)
                )
                .foregroundStyle(day.count >= goal ? .green : .accentColor)
                .annotation(position: .top) {
                    if day.count >= goal {
                        Image(systemName: "star.fill")
                            .foregroundStyle(.yellow)
                            .font(.caption)
                    }
                }
            }
            .chartXAxis {
                AxisMarks(values: .stride(by: .day)) { value in
                    if let date = value.as(Date.self) {
                        AxisValueLabel(format: .dateTime.weekday(.abbreviated))
                    }
                }
            }
            .chartYAxis {
                AxisMarks(position: .leading, values: .automatic(desiredCount: 4))
            }
            .frame(height: 220)
            .accessibilityChartDescriptor(self)
        }
    }

    private var averageFormatted: String { "\(history.map(\.count).reduce(0, +) / history.count)" }
    private var trendDescription: String { /* ... */ "4% below" }
    private var goal: Int { 10_000 }
}

// AXChartDescriptor for VoiceOver exploration.
extension StepsChart: AXChartDescriptorRepresentable {
    func makeChartDescriptor() -> AXChartDescriptor {
        let xAxis = AXCategoricalDataAxisDescriptor(
            title: "Day",
            categoryOrder: history.map { $0.date.formatted(.dateTime.weekday(.wide)) }
        )
        let yAxis = AXNumericDataAxisDescriptor(
            title: "Steps",
            range: 0...(history.map(\.count).max() ?? 0),
            gridlinePositions: []
        ) { value in "\(Int(value)) steps" }

        let series = AXDataSeriesDescriptor(
            name: "Daily steps",
            isContinuous: false,
            dataPoints: history.map {
                AXDataPoint(x: $0.date.formatted(.dateTime.weekday(.wide)),
                            y: Double($0.count))
            }
        )

        return AXChartDescriptor(
            title: "Daily steps this week",
            summary: "Bar chart, each bar is one day's step count.",
            xAxis: xAxis,
            yAxis: yAxis,
            series: [series]
        )
    }
}
```

## Common chart types — what to use when

| Type | Use for |
|---|---|
| Line | Time-series trends |
| Bar | Category comparison |
| Stacked bar | Parts of a whole over time |
| Area | Filled time-series, often for cumulative data |
| Scatter | Correlation between two variables |
| Heatmap | Density across two dimensions |
| Pie / donut | Parts of a whole (≤5 categories, ideally) |
| Gauge | Current value in a bounded range (see [`../components/progress-and-activity.md`](../components/progress-and-activity.md#gauges)) |
| Box plot | Distributions |

Avoid stacked charts for more than ~4 categories — readability collapses.

## See also

- [`../foundations/color.md`](../foundations/color.md) — semantic colors in charts.
- [`../foundations/typography.md`](../foundations/typography.md) — Dynamic Type on chart labels.
- [`../foundations/accessibility.md`](../foundations/accessibility.md) — audio graphs, VoiceOver.
- [`../components/progress-and-activity.md`](../components/progress-and-activity.md#gauges) — gauge vs chart.
- [Swift Charts documentation](https://developer.apple.com/documentation/charts) — the framework to build with.
