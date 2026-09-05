# Feed parser parity references

The parser fixture strategy is informed by these mature implementations:

- [feed-rs](https://docs.rs/feed-rs/latest/feed_rs/): RSS 0.x/1.0/2.0, Atom, optional normalized fields, and namespace extensions.
- [gofeed](https://github.com/mmcdole/gofeed): root-format detection, bounded readers, format-specific parsing, and normalization.
- [Python Universal Feedparser](https://feedparser.readthedocs.io/): tolerant handling of missing fields and parser diagnostics.
- [Feedjira](https://github.com/feedjira/feedjira): SAX parsing and extensible parser selection.
- [ruby/rss](https://github.com/ruby/rss): RSS 0.91/1.0/2.0, Atom, and iTunes podcast fields.

Fixtures are intentionally small and original. They test observable behavior rather than copying
upstream test corpora. The existing complete parser remains the parity oracle on device because its
Android XML implementation is not reliable in local JVM tests.

## Implementation boundary

Booth favors mature, stable, data-specific parsing over a broad custom reimplementation:

- `Prof18FeedParser` remains the semantic authority for complete RSS/Atom parsing.
- Booth normalization maps parser output into `RssFeed` and `RssEntry` without inventing a new
  feed model for every extension.
- The streaming parser is an application adapter for bounded discovery previews, cancellation, and
  chunk delivery; it is not a replacement for every mature parser feature.
- Refresh and subscription flows continue to collect complete results and persist atomically.
- Any streaming result that cannot meet the complete-parser parity contract must fall back before
  it affects durable state.

This intentionally keeps the runtime dependency and compatibility fallback until a real-feed
corpus demonstrates equivalent behavior.
