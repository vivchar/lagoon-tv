# Search

**HIG source:** https://developer.apple.com/design/human-interface-guidelines/searching
**Last verified:** 2026-04-21
**Applies to:** iOS · iPadOS · macOS · tvOS · visionOS · watchOS

Search is discovery. On Apple platforms it's a first-class pattern: one clearly identified surface for finding anything in your app, suggestions and scopes where useful, and integration with Spotlight so people can find your content without opening the app.

## When search is a primary action

- **Many users start with search.** Put it in the tab bar with `role: .search` (iOS/iPadOS), or in the toolbar prominently.
- **Large content libraries.** Apple TV, Music, Photos, Notes — all give search a dedicated tab.
- **Otherwise, in the toolbar.** Mail-style: persistent search accessible from the list header.

## Rules

- **One clearly-identified search location** for the whole app. Users appreciate knowing "this is where I search everything." If sections are distinct, local search per section is fine (Phone app filters Recents vs Contacts).
- **Placeholder text = what's searchable.** "Shows, Movies, and More" (Apple TV), "Search notes and attachments" (Notes). Be specific so users know what will and won't match.
- **Display the scope** (mailbox, folder, album, tab) clearly — placeholder text, a scope label, or a title.
- **Suggestions help people type less.** Show recent searches, completions, and suggestions before and during typing.
- **Privacy in history.** Don't leak the user's search history where others might see it. Provide a way to clear history. If in doubt, omit the history display and rely on suggestions.
- **Scopes and filters reduce result sets** — creation date, file type, author. Expose a scope bar or filter chips below the field.
- **Empty state matters.** "No results for 'xyz'" is a baseline. Offer alternatives: suggestions, recents, a way to refine.

## Systemwide search (Spotlight)

Index your app's content for Spotlight so users find it without opening your app.

- **CoreSpotlight** for structured content (emails, notes, documents).
- **Activities + handoff** for current-session context.
- **Quick Look generator** for custom file types so previews work.
- **Metadata importer plug-in** for custom file formats.
- **Open and Save panels** on macOS already include system search — prefer them for document import/export.

## Platform specifics

### iOS / iPadOS
- **`.searchable(text:)`** on a navigation stack adds a search bar at the top that collapses on scroll.
- **Search tab with `role: .search`** puts search in the tab bar at the trailing end.
- **Token-based search** (`.searchable(text:, tokens:)`) — chips for scope.
- **Scope bar** — categories underneath the field.
- **Searchable list-row swipe?** Not a thing — use filters.

### macOS
- **Toolbar search field.** Trailing edge of the toolbar is convention.
- **⌘F** always opens Find-in-current-view for documents; a separate toolbar search field is for "search my library."
- **Spotlight-style instant results** in a popover below the search field.
- **Autocomplete** via `NSSearchField.completions(for:…)`.

### tvOS
- **Dedicated Search tab.** Users swipe up to reveal recent searches / suggestions.
- **Voice search via Siri Remote.** Support `UISearchViewController` + native Voice Search.
- **Keyboard input is painful** — make sure suggestions fire aggressively.

### watchOS
- **Minimal text entry** — dictation, Scribble, or predefined categories.
- Skip search entirely when the data set is small (<20 items) and a list or picker would do.

### visionOS
- Same SwiftUI API. Keyboard is in the user's field of view — put the search field where the keyboard doesn't block neighboring content.

## Writing suggestions and results

- **Completions:** "kitten" → "kittens", "kitten names".
- **Corrections:** "kiten" → "Did you mean *kitten*?".
- **Recents:** the last 5–10 queries, clearly labeled "Recent searches."
- **Suggestions:** saved queries, trending queries, or items from your content that match "popular items."
- **Types of items in results:** group by section, with clear section headings ("Messages", "Contacts", "Files").

## Scope and filters

- **Scope bar** for mutually-exclusive filters (All / Unread / Flagged).
- **Token chips** inside the search field for compound queries ("From: Claude", "Label: urgent").
- **Filter sheet / popover** for advanced criteria (date ranges, file size).

## Common mistakes

- Two search surfaces in the same app, each searching different subsets, both called "Search."
- Empty placeholder ("Search…") — doesn't teach what's searchable.
- Search that ignores the current scope (searching "All Mail" when the user is in "Today's Mailbox").
- Showing search history on a shared device without a clear way to clear it.
- No suggestions — every keystroke is pure work.
- Results with no section headings — one giant blob.
- Hiding the scope bar behind a hamburger menu.
- `⌘F` opening global search in a document app (it should open in-doc find).
- No Spotlight integration for an app built around discoverable content.
- tvOS search without voice input support.

## SwiftUI

```swift
// iOS — search tab + `.searchable` on the list.
@State private var query = ""
@State private var tokens: [SearchToken] = []
@State private var scope: Scope = .all

NavigationStack {
    ResultsList(query: query, scope: scope, tokens: tokens)
        .navigationTitle("Library")
        .searchable(text: $query,
                    tokens: $tokens,
                    placement: .navigationBarDrawer(displayMode: .always),
                    prompt: Text("Books, authors, collections")) { token in
            Label(token.name, systemImage: token.systemImage)
        }
        .searchScopes($scope) {
            Text("All").tag(Scope.all)
            Text("Books").tag(Scope.books)
            Text("Authors").tag(Scope.authors)
        }
        .searchSuggestions {
            ForEach(recentQueries) { query in
                Label(query.term, systemImage: "clock").searchCompletion(query.term)
            }
        }
}

// macOS — toolbar search + autocompletion.
NavigationSplitView {
    LibraryView()
} detail: {
    Detail()
}
.searchable(text: $query, placement: .toolbar, prompt: Text("Search"))

// Search role for a tab in iOS 18+.
TabView {
    Tab("Home", systemImage: "house") { HomeView() }
    Tab("Search", systemImage: "magnifyingglass", role: .search) {
        NavigationStack { SearchView() }
    }
}
```

## UIKit

```swift
// UISearchController inside a navigation bar.
let search = UISearchController(searchResultsController: nil)
search.searchResultsUpdater = self
search.obscuresBackgroundDuringPresentation = false
search.searchBar.placeholder = "Books, authors, collections"
search.searchBar.scopeButtonTitles = ["All", "Books", "Authors"]
navigationItem.searchController = search
navigationItem.hidesSearchBarWhenScrolling = false

extension LibraryVC: UISearchResultsUpdating {
    func updateSearchResults(for controller: UISearchController) {
        let query = controller.searchBar.text ?? ""
        let scope = controller.searchBar.selectedScopeButtonIndex
        updateResults(query: query, scope: scope)
    }
}
```

## AppKit

```swift
// Toolbar search field on macOS.
let search = NSSearchToolbarItem(itemIdentifier: .searchField)
search.searchField.placeholderString = "Search"
search.searchField.target = self
search.searchField.action = #selector(searchFieldDidChange(_:))

// Completions.
extension LibraryVC: NSSearchFieldDelegate {
    func control(_ c: NSControl, textView: NSTextView,
                 completions words: [String],
                 forPartialWordRange r: NSRange,
                 indexOfSelectedItem i: UnsafeMutablePointer<Int>) -> [String] {
        return recentQueries.filter { $0.hasPrefix(textView.string) }
    }
}
```

## See also

- [`../components/bars.md`](../components/bars.md) — search in the tab bar and toolbar.
- [`../components/text-inputs.md`](../components/text-inputs.md) — search field is a text field with `.textContentType(.none)` and the right keyboard.
- [`./navigation.md`](./navigation.md) — search as a top-level navigation destination.
- [`./loading.md`](./loading.md) — live-updating results should feel immediate.
