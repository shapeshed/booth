# Architecture and coverage plan

Written after the review pass in [#34](https://github.com/shapeshed/booth/pull/34). This records
where the codebase actually stands and what to do next, with the measurements behind each claim so
they can be re-checked rather than taken on trust.

## Summary

Architecture invariants are now pinned by tests, and 15 composables no longer need a ViewModel. The
remaining problem is that the application's behaviour is untested in the metric the build enforces,
and that is because the units holding the behaviour are too large to test rather than because
testing was skipped.

The order below is deliberate. Item 1 is the root cause; most of the rest is downstream of it.

## Where the numbers stand

Measured on commit `0588460` with
`./gradlew :app:jacocoDeviceTestUnitTestReport` and `connectedDeviceTestAndroidTest` on a
Pixel 9 Pro XL, Android 17.

| | 2026-09-05 | now |
|---|---|---|
| lines covered | 1,198 | 1,428 |
| total lines | 16,983 | 18,605 |
| line coverage | 7.05% | 7.68% |
| instruction | — | 6.77% |
| branch | — | 9.57% |

255 JVM unit tests and 55 instrumentation tests, 0 failures.

### Coverage is concentrated, and the pattern is the point

Every orchestration class is at 0% in the measured metric:

| class | lines | covered |
|---|---|---|
| `PodcastViewModel` | 485 | 0 |
| `PodcastBackupManager` | 337 | 0 |
| `PodcastRepository` | 321 | 0 |
| `PodcastPlaybackViewModel` | 276 | 0 |
| `PodcastDownloadManager` | 174 | 4 |
| `PodcastPlaybackService` | 163 | 0 |
| `PodcastRefreshWorker` | 75 | 0 |
| `EpisodeDownloadWorker` | 6 | 0 |
| `BoothModule` | 41 | 0 |

Roughly 1,880 lines of orchestration at zero. What *is* covered is covered well, and it is all
deterministic logic:

| | coverage |
|---|---|
| `RssPodcastMappingKt` | 100% (30/30) |
| `SaxStreamingFeedParser` | 85–97% across four classes |
| `DownloadProgressKt` | 94.7% (36/38) |
| `PodcastTagsKt` | 89.4% (42/47) |
| `ApplePodcastSearchProvider` | 78.7% (74/94) |
| `EpisodeEntity`, `PodcastEntity` | 100% |

The rule this reveals is the one worth internalising: **extract a pure function and it lands at
90–100%; leave it as an orchestrator and it lands at 0%.** Every well-covered class in the list
above is a function that takes its inputs and returns a value.

### Two caveats about the metric itself

1. **It only counts JVM unit tests.** `PodcastRepository` is not unexercised — it has 10
   instrumentation tests — but the report reads 0%. A change that breaks it can pass the floor
   while coverage still shows nothing.
2. **The instrumentation coverage is shallow regardless.** 55 tests, most asserting that a node is
   displayed or a callback fired. Two tests against 337 lines of backup and restore is not coverage.

Neither is a reason to distrust the number. It is a reason not to treat 7.68% as "7.68% of the
app".

## The plan

### 1. Split `PodcastViewModel` — 1,325 lines, 0% covered

The root cause of everything below. It currently owns discovery and category browsing, search, the
queue, inbox, settings, OPML import and export, backup import and export, and subscription
management. Those are separable, and each becomes testable the moment it exists as its own unit.

Suggested seams, roughly in dependency order:

- `PodcastSettingsViewModel` — the `setPodcast*` family, currently ~30 one-line methods that only
  forward to a DataStore
- `PodcastImportExportViewModel` — OPML and backup, including the `observeOpmlImport` poller
- `PodcastDiscoveryViewModel` — `preview`, `loadDiscovery`, `loadCategory`, `loadMoreCategory`,
  `preloadCategory`
- `PodcastSearchViewModel` — global search
- `PodcastQueueViewModel` — queue, inbox, up-next

The existing `PodcastHomeUiState` aggregation can stay as the composition point so screens do not
change shape during the split. Nothing here should require a UI change.

Do this before writing ViewModel tests. A test against 485 lines of orchestration with forty
injected collaborators produces something nobody will maintain.

### 2. Close the untested behaviour fixes

Three of the eight behaviour fixes in #34 have no regression test. Two of them are the kind that
fail silently.

- **`onPlaybackResumption` leaving its future pending forever** (`180cb3f`). A hang is invisible in
  review and catastrophic on a car display, because Android Auto simply waits. The fix extracted
  `resumptionItems()` as a suspend function taking state, so it is directly testable against a fake
  repository. Also assert the future completes when the scope is cancelled — that is the regression.
- **The import confirmation replaying on rotation** (`b74ac4b`). `PodcastUiEvent` is now a
  `Channel`, so the test is: emit, assert one delivery, assert no second on recomposition.
- **`POST_NOTIFICATIONS` request timing** (`d773e44`). `ensureMediaNotificationPermission` is a
  single lambda on `PodcastHomePlatformActions`; assert it is requested when an episode starts and
  not before.

### 3. Move the coverage floor

`app/build.gradle` pins `minimum = 0.07` with a comment measured at 7.05% on 2026-09-05. The
codebase has grown 1,622 lines since and the floor has not moved, so it currently gates almost
nothing.

- Set it to 0.076 now, the measured value.
- Raise it with each batch, as the comment already asks. Nothing enforces that today, which is why
  it has sat unchanged.
- Consider a second rule on `PodcastViewModel` and `PodcastRepository` specifically, so a split
  cannot be offset by unrelated additions elsewhere.

### 4. Include instrumentation in coverage, or rename the metric

Either merge the instrumentation execution data into the JaCoCo report, or rename the gate to
"unit-test line coverage" so nobody reads it as total coverage. A floor that cannot see half the
tests is a false signal, and the current name invites that reading.

### 5. Split `PodcastHomeScreen.kt` — 1,992 lines

Lower priority than the ViewModel, and for a different reason: after the hoisting work this file is
now *testable*, just untested. That is an easier problem than untestable.

The 36-entry ktlint baseline is all in this package and mostly points here. `Scoped*` wrappers,
`PodcastHomeDerivedState` and the `*Destination` split are the existing seams to cut along.

### 6. The remaining 36 ktlint entries

None are `standard`, so none are formatting debt — they are architectural complaints:

| rule | count | note |
|---|---|---|
| `parameter-naming` | 15 | the rule reads "Played" and "Enabled" as past tense in `onSetPlayed` and `onSetAllEnabled`, which are adjectives. Several of these want a deliberate decision, not a rename |
| `lambda-param-in-effect` | 10 | genuine staleness risk. `PodcastHomePlaybackOverlays` was one of these and held a real bug: a `LaunchedEffect` keyed on swipe state calling three callbacks the parent recreates each recomposition |
| `param-order-check` | 3 | two are `modifier`-before-required shapes, one is `PodcastHomeAppContent` where `content` must stay last for the trailing lambda |
| `content-slot-reused` | 3 | |
| `multiple-emitters-check` | 2 | |
| `vm-forwarding-check` | 2 | `PodcastHomeEffects` and `PodcastHomeBackHandlers`, both left deliberately — see below |
| `compositionlocal-allowlist` | 1 | |

`PodcastHomeEffects` was left taking its ViewModel on purpose. It renders nothing, so there is no
testability to win, and suppressing the count by restructuring it would be dishonest. The same
applies to `PodcastHomeBackHandlers`.

### 7. Merge the two 30-parameter pass-throughs

`PodcastHomeSettingsDestination` and `PodcastSubscriptionsContent` are both ~30 parameters, and the
second forwards verbatim to the first. This is the same shape as `PodcastAppSettingsContent`, which
had 34 and was deleted once already in #34. It has grown back twice since.

Grouping the setters was step one. The real fix is probably to merge the two functions, since one
does nothing the other does not.

## What is already done, for context

Not part of the plan, but these are the invariants a future change must not break. Each is pinned by
a test:

- **One object graph.** A second `PodcastDatabase` or `OkHttpClient` silently breaks Room flow
  invalidation and corrupts the shared HTTP cache directory.
  Pinned by `BoothWorkerEntryPointDeviceTest`.
- **No mutable `object` singletons.** A process-global is invisible in the object graph, cannot be
  replaced in a test, and any lock it holds is per-instance rather than per-process, so it provides
  no mutual exclusion at all. Same test.
- **Room at version 1.** Booth is local-first with no server copy, so a bad migration is
  unrecoverable. The 27 versions were squashed before the first release.
- **The 2 Hz played position stays scoped.** Collecting it once in the home screen body is the
  obvious deduplication and would put the whole screen at 2 Hz. See `AGENTS.md`.
- **The playback service stays exported.** A `MediaSessionService` must be discoverable by Android
  Auto, the assistant, Bluetooth and Wear, all of which bind from outside the app.

## Re-measuring

Coverage numbers in this document are a snapshot. To re-check:

```sh
./gradlew :app:jacocoDeviceTestUnitTestReport
# report at app/build/reports/jacoco

./gradlew :app:jacocoDeviceTestUnitTestCoverageVerification   # the 7% floor

ANDROID_SERIAL=<serial> ./gradlew :app:connectedDeviceTestAndroidTest
```

Note the caveat from `docs/testing.md`: an interrupted Gradle build leaves `app/build` in a state
that makes every Compose test fail with "No compose hierarchies found in the app" while every
non-Compose test passes. `./gradlew clean` first if a result looks like a regression.
