# Changelog

All notable changes to Booth will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and version numbers follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

Initial release candidate.

### Added

- OPML import and export for moving subscriptions between podcast apps.
- All Episodes view with episode search, filtering, queue actions, and downloads.
- Podcast discovery with search, categories, and Podcast Index support.
- Per-podcast playback speed and skip-silence overrides.
- Background podcast refresh through WorkManager, scheduled hourly on any network.
- Podcast notifications for new episodes.
- Automatic Up Next and download settings for new episodes.
- Settings footer showing release, nightly, and dirty-build identifiers.

### Changed

- Simplified global and podcast settings around Playback, Up Next, Notifications, and Discovery.
- Updated top bars and scrolling behavior with Material 3 patterns.
- Added adaptive podcast grids that adjust the number of columns to the available width.
- Standardized settings hierarchy and supporting text across global and podcast settings.

### Fixed

- Podcast and episode database migrations for persisted playback and download settings.
- Download progress and completion state reconciliation after process death.
- Subscription and episode navigation after importing OPML subscriptions.
