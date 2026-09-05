# Booth implementation plan

This is the hand-off plan for the next agent. Keep the product audio-first, local-first and
quietly native. Changes should preserve existing playback, queue and notification behaviour.

## Current work: open video feeds

- Parse Podcasting 2.0 `podcast:alternateEnclosure` entries with `video/*` and HLS MIME types.
- Store a preferred video URL and MIME type alongside the normal audio enclosure.
- Prefer HLS for streaming when available, then MP4.
- Keep audio as the default playback path and keep the mini-player/media notification audio-first.
- Add a clearly labelled `Watch` action only when an episode has a supported video enclosure.
- Reuse the same episode position when switching between audio and video where the timelines match.
- Start with streaming; do not add video downloads until storage, cancellation, Wi-Fi policy and
  foreground/background behaviour are designed and tested.

Reference feed: `https://podcast-standards-project.github.io/hls-video/feed.xml`.

## Next: playback resilience

1. Test and harden the Media3 service lifecycle: pause/resume, audio-focus loss and gain,
   headphone/Bluetooth disconnect, process recreation, task removal and notification actions.
2. Make progress persistence durable on every meaningful transition and on service shutdown, while
   avoiding writes that reset a position during feed refresh or screen changes.
3. Define unavailable-network behaviour: retain the current item, expose a retry state, avoid
   silently advancing the queue, and distinguish a temporary buffering failure from an invalid URL.
4. Make queue continuation deterministic after process death: persist the active item and next queue
   order, resume the saved position, and remove an item only after confirmed completion.
5. Add playback transition unit tests around intro/end skips, completion, queue advancement and
   position restoration; add focused Android tests for service/controller behaviour.
6. Review automatic refresh/download workers for retry/backoff, network constraints, duplicate work,
   partial downloads, cancellation and notification/error visibility.
7. Add a small storage/download management surface: active, failed, downloaded and removable media,
   with explicit space impact before destructive actions.

### Download UX decision

- Follow Apple Podcasts' user model: an episode is either available offline or it is not; do not
  expose the worker lifecycle in the primary UI.
- Recover automatically through WorkManager constraints, retry/backoff, app refresh, and process
  recreation wherever the failure is plausibly temporary.
- Treat permanent failures (for example, a stable 404 or forbidden response) as durable records,
  but do not force the user into a download-management screen. Surface a quiet retry affordance in
  the episode row only when recovery has been exhausted.
- Removing a download removes only the local media. It must not remove the episode from a feed,
  subscription, saved state, or Up Next.
- Keep durable records for recovery, diagnostics, storage accounting, and a future Downloads
  surface, even though most of that state remains hidden from users.

## Deliberately later

Chapters, transcripts/captions, video downloads and richer creator metadata should follow the
resilience work. They should be added behind capability detection and should not complicate the
audio-first listening path.
