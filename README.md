# Klipy Android SDK (local module)

This module is a small Android SDK around the Klipy API, extracted from the official
demo app and refactored into a clean `KlipyRepository` interface.

It handles:

- Retrofit + OkHttp client and JSON parsing (Gson)
- Ads / device query parameters via an interceptor
- DTO → domain mapping
- Simple pagination per filter (`recent`, `trending`, or search query)
- Category fetching

## Installation

1. Copy the `klipy-sdk` module into your Android project.
2. In your root `settings.gradle`, include it:

```kotlin
include(":klipy-sdk")
