# Contributing

Thanks for considering contributing to Booth.

## Code of Conduct

This project follows the [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected
to uphold it.

## Reporting bugs and requesting features

Please use the repository's issue tracker and search existing issues first. For security
vulnerabilities, see [SECURITY.md](SECURITY.md) instead of opening a public issue.

Useful bug reports include:

- The device model, Android version, and Booth version or commit
- The steps needed to reproduce the problem
- Relevant logs, feed URLs, or screenshots, with credentials and personal data removed
- Whether the issue affects streaming, downloads, offline playback, refresh, or notifications

## Development setup

Build, test, and release instructions live in [DEVELOPERS.md](../DEVELOPERS.md). In short:

```sh
./gradlew quality
./gradlew assembleDebug
```

Keep changes focused, preserve the calm listening experience, and use the existing Jetpack Compose
Material 3 patterns. Do not add third-party dependencies without discussing their maintenance,
licensing, and runtime cost first.

## Submitting a pull request

1. Create a branch from `main`.
2. Keep the pull request focused on one change where possible.
3. Run `./gradlew quality` locally before pushing; CI runs the same gate.
4. Use [Conventional Commit](https://www.conventionalcommits.org/) messages, for example
   `fix(player): recover after audio focus loss` or `feat(search): add provider filtering`.
5. Explain what changed, how it was tested, and any device-specific observations.
6. Include screenshots or a short recording for meaningful UI changes.

## Translations

English strings in `app/src/main/res/values/strings.xml` are the source strings. Locale-specific
resources live in the corresponding `values-*` directories. Preserve string names, placeholders
such as `%1$s`, and XML markup when updating translations.

## License

By contributing, you agree that your contributions will be licensed under the project's
[Apache License 2.0](../LICENSE).
