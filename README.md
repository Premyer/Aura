## 🌟 Aura - AI Assistant Android App

A modern Android application featuring an AI-powered assistant with chat functionality built using Jetpack Compose and Material Design 3.

### Features

- 💬 **Interactive Chat Interface** - Clean and intuitive conversation UI
- 🤖 **AI Assistant** - Powered directly by the Anthropic API (Claude)
- 📱 **Modern UI** - Built with Jetpack Compose and Material Design 3
- 🔄 **Real-time Communication** - Instant message responses
- ⚡ **Kotlin-first** - Fully written in Kotlin with coroutines

### Project Structure

```
Aura/
├── app/
│   ├── src/main/
│   │   ├── java/com/premyer/aura/
│   │   │   ├── MainActivity.kt
│   │   │   ├── ui/
│   │   │   │   ├── screen/
│   │   │   │   │   └── AssistantScreen.kt
│   │   │   │   └── theme/
│   │   │   │       ├── Theme.kt
│   │   │   │       ├── Color.kt
│   │   │   │       └── Type.kt
│   │   │   ├── viewmodel/
│   │   │   │   └── AssistantViewModel.kt
│   │   │   └── data/
│   │   │       ├── AssistantRepository.kt
│   │   │       ├── AuraApiService.kt
│   │   │       ├── AnthropicDto.kt
│   │   │       └── ChatModels.kt
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

### Requirements

- Android 8.0 (API 26) or higher
- JDK 17 or higher
- An [Anthropic API key](https://console.anthropic.com/settings/keys)

### Building the Project

1. Clone the repository:
```bash
git clone https://github.com/Premyer/Aura.git
cd Aura
```

2. Configure your API key (see **Configuration** below).

3. Build the project:
```bash
./gradlew build
```

4. Build APK:
```bash
./gradlew assembleRelease
```

The APK will be generated at: `app/build/outputs/apk/release/app-release.apk`

### Configuration

The app calls the Anthropic Messages API (`https://api.anthropic.com/v1/messages`) directly from
`AuraApiService.kt`. The API key is **not** hardcoded — it's read at build time from
`local.properties`, which is already in `.gitignore` and never committed.

1. Copy the example file:
```bash
cp local.properties.example local.properties
```
2. Open `local.properties` and set your key:
```properties
ANTHROPIC_API_KEY=sk-ant-your-key-here
```
3. (Optional) Change the model used, in `app/build.gradle.kts`:
```kotlin
buildConfigField("String", "ANTHROPIC_MODEL", "\"claude-sonnet-5\"")
```

⚠️ **Security note:** embedding the API key in the app via `BuildConfig` is fine for development
and personal use, but anyone can extract a key from a published APK. Before releasing this app
publicly (e.g. on the Play Store), move the Anthropic call behind your own backend (e.g. the
Cloud Run service this project used before) so the key never ships inside the app.

### Building automatically with GitHub Actions

Every push to `main` (and manual runs via the "Actions" tab) builds a debug APK in the cloud —
no local Android SDK needed. To enable it:

1. In the GitHub repo, go to **Settings → Secrets and variables → Actions** and add a new
   repository secret named `ANTHROPIC_API_KEY` with your key as the value.
2. Push to `main`, or trigger the **Build APK** workflow manually from the **Actions** tab.
3. Once it finishes, open the workflow run and download the `aura-debug-apk` artifact — that's
   `app-debug.apk`, already signed with the Android debug key so it installs directly on a
   device or emulator for testing (`adb install app-debug.apk`).

This produces a **debug** build for testing, not a Play Store release build (which needs its own
signing key — see [Android's docs on app signing](https://developer.android.com/studio/publish/app-signing)
when you're ready for that).

### Running Tests

```bash
./gradlew test
./gradlew connectedAndroidTest
```

### Technologies Used

- **Jetpack Compose** - Modern UI toolkit
- **Material Design 3** - Design system
- **Kotlin Coroutines** - Asynchronous programming
- **OkHttp** - HTTP client
- **GSON** - JSON serialization
- **Anthropic Messages API** - Claude as the AI backend

### License

MIT License - see LICENSE file for details

### Author

**Premyer** - [GitHub Profile](https://github.com/Premyer)

---

**Version:** 1.0.0  
**Last Updated:** 2026-05-06
