# Changelog

This changelog tracks the Klipy Android SDK release history and the commit-level changes that shipped in each version.

## [Unreleased]

- Add a picker theme API with automatic, light, and dark modes plus color overrides.
- Upgrade the sample app into a benchmark-style picker configurator for theme, layout, feed, and media tabs.
- Tint picker image loading indicators yellow and keep the search placeholder text theme-aware and grey by default.
- Append `ad-iframe=1` to ad-enabled SDK requests and harden ad query enrichment against missing platform strings in tests.
- Add a dedicated Ads Demo flow to the demo app with a live masonry feed and inline ad URL rendering.

## [0.1.12] - 2026-04-13

- Add the Klipy test API key as the committed sample app fallback.
  Commit: `13b3552`
- Fix the README JitPack coordinates for `klipy` and `klipy-ui`.
  Commit: `4898bce`
- Speed up picker grid rendering by preferring lightweight preview assets and bitmap thumbnails.
  Commit: `643ebd7`
- Update the published SDK version references to `0.1.12`.

## [0.1.11] - 2026-04-11

- Debounce picker searches while typing and handle Enter consistently.
  Commit: `f52914e`

## [0.1.10] - 2026-04-11

- Add a changelog file for SDK releases and in-progress changes.
  Commit: `99c641b`
- Allow the picker search field to submit when Enter is pressed.
  Commit: `402b07d`
- Remove the Material theme requirement from the picker retry button.
  Commit: `69fb8fa`

## [0.1.9] - 2026-04-11

- Show a dedicated offline state when the picker cannot reach Klipy.
  Commit: `42a5335`
- Update the published SDK version references to `0.1.9`.
  Commit: `5066e52`

## [0.1.8] - 2026-04-11

- Add a sticky Powered by Klipy footer to the Android picker.
  Commit: `516e395`
- Update the published SDK version references to `0.1.8`.
  Commit: `6a609ff`

## [0.1.7] - 2026-04-10

- Fix the GitHub build by making the Gradle wrapper executable.
  Commit: `bcef6ec`
- Update the published SDK version references to `0.1.7`.
  Commit: `8474828`

## [0.1.6] - 2026-04-10

- Replace the custom project license with Apache 2.0 and add a NOTICE file.
  Commit: `de6e4e0`
- Harden media item mapping against incomplete Klipy API payloads.
  Commit: `47c2147`
- Add continuous verification for the SDK and remove unused core library themes.
  Commit: `6b7b45a`
- Update the public dependency references to version `0.1.6`.
  Commit: `69c4da0`

## [0.1.5] - 2026-04-10

- Align the Android SDK with the current Klipy API contract.
  Commit: `9b88f28`
- Document the current Tenor-to-Klipy migration flow in the README.
  Commit: `a6e69d0`
- Remove the stale Giphy dependency entry from the version catalog.
  Commit: `8e35a46`
- Update the sample apps to use the current Klipy SDK integration pattern.
  Commit: `3847bd0`
- Remove hardcoded sample API keys before the production release.
  Commit: `1a36f3c`
- Replace the MIT license with an attribution-required custom license.
  Commit: `51504e6`
- Update the published SDK version references to `0.1.5`.
  Commit: `110672e`

## [0.1.4]

- Remove usage of my personal architecture library.
  Commit: `9def95b`

## [0.1.3]

- Update implementation flow.
  Commit: `30f50a3`

## [0.1.0] through [0.1.2]

- These early releases predate the structured changelog.
