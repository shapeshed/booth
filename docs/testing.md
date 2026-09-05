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
