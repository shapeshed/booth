# Testing Booth

Booth follows an Android testing pyramid: fast JVM tests for deterministic data and state
transforms, Compose tests for observable UI behavior, and isolated instrumentation for Android
runtime behavior.

## Test placement

- `app/src/test/` contains parser, repository policy, navigation contract, ViewModel, and other
  deterministic JVM tests.
- `app/src/androidTest/` contains Room, Media3, Activity lifecycle, and Compose interaction tests.
- Adaptive layout decisions should be unit-tested as policies and exercised on device for final
  inset, font-scale, and form-factor validation.

Every feature should test observable behavior at the lowest practical level. Every bug fix should
start with a regression test that fails before the fix and passes afterward.

## Commands

```sh
# Compile, lint, and run isolated-variant JVM tests
./gradlew quality

# Generate and verify local JaCoCo coverage
./gradlew :app:jacocoDeviceTestUnitTestReport
./gradlew :app:jacocoDeviceTestUnitTestCoverageVerification

# Run instrumentation without clearing the normal Booth installation
ANDROID_SERIAL=<test-device-serial> ./gradlew preservingDebugAndroidTest
```

Instrumentation uses the `deviceTest` build type and the isolated application ID
`com.shapeshed.booth.deviceTest`. `BoothTestRunner` fails fast if instrumentation targets the
normal application ID. Keep `installDebug` for normal development. Never run connected tests
against `com.shapeshed.booth` when it contains personal listening data.

### Stale build state looks like a regression

An interrupted Gradle build — a killed daemon, a cancelled task, a machine restart — can leave
`app/build` in a state that produces a convincing false failure. The observed signature is every
Compose UI test failing while every non-Compose test passes:

```text
java.lang.IllegalStateException: No compose hierarchies found in the app.
```

with no `FATAL EXCEPTION` in logcat, no crash, and the host `androidx.activity.ComponentActivity`
visibly launching once per test method. The activity comes up and the semantics tree never
registers, so any `onNodeWith...` call throws before reaching the code under test.

Do not investigate the application when you see this. Clear the build and run again:

```sh
./gradlew clean
ANDROID_SERIAL=<test-device-serial> ./gradlew :app:connectedDeviceTestAndroidTest
```

Two traps, both of which cost real time here:

- **Do not bisect across it.** `git bisect` with a stale `app/build` measures noise rather than
  changes, and will confidently blame a commit that cannot possibly be responsible, such as one
  that only edits documentation. If a bisect result is absurd, suspect the measurement.
- **A clean build at the current commit is the discriminator.** It settles the question in about a
  minute and needs no bisect.

Note that `./gradlew quality` does not clear this, and neither does `--rerun-tasks` on the Kotlin
compile alone. The generated Compose and Hilt code has to be regenerated too.

### The same signature, from a locked device

**A locked test device produces this identical error, and a clean build does not fix it.** The test
`ComponentActivity` launches behind the keyguard, so the window never becomes visible and the
semantics tree never registers. It is indistinguishable from the stale-build case by looking at the
test results alone, and it is intermittent, which makes it worse: it passes and fails on the same
commit minutes apart.

The discriminator is the device, not the build:

```sh
adb shell dumpsys window | grep -E 'mDreamingLockscreen|mCurrentFocus'
```

`mDreamingLockscreen=true`, or a `mCurrentFocus` naming the notification shade or a keyguard view,
means every Compose test will fail this way. Unlock the device and re-run; no build step is
involved. To keep it unlocked across a session, `adb shell svc power stayon usb`.

Order matters: check this **before** the clean. A clean costs a couple of minutes and fixes nothing
here, and reaching for it first is what makes this look like a build problem.

## Hilt test bindings

Directory providers are replaceable in instrumentation tests with
`@TestInstallIn`. `FakeDirectoryProviderModule` supplies a deterministic provider and
`DirectoryProviderInjectionDeviceTest` verifies that the production provider module is replaced.
Use the same seam for repository or catalog fakes when testing ViewModel and screen behavior; keep
network-backed providers out of tests that only need deterministic data.

## UI coverage

Compose UI tests use the v2 Compose test rule and semantic matchers. Important flows should cover
navigation/back behavior, loading and error states, confirmation actions, playback controls,
selection, and accessibility labels. Add screenshot tests when adaptive layout changes affect
compact, medium, expanded, dark-mode, or large-font rendering.

## Coverage policy

JaCoCo reports are generated for the isolated `deviceTest` unit-test variant. The current minimum
line-coverage floor is 7%, based on the measured 2026-09-05 baseline of 1,251/17,164 lines. Raise
it as new behavior and deterministic tests are added; the report inputs are configured explicitly
so an empty report cannot satisfy the gate.
