## 🌟 Aura - AI Assistant Android App

A modern Android application featuring an AI-powered assistant with chat functionality built using Jetpack Compose and Material Design 3.

### Features

- 💬 **Interactive Chat Interface** - Clean and intuitive conversation UI
- 🤖 **AI Assistant** - Powered by your custom API endpoint
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
│   │   │       └── AuraApiService.kt
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

### Requirements

- Android 8.0 (API 26) or higher
- JDK 11 or higher
- Gradle 8.0+

### Building the Project

1. Clone the repository:
```bash
git clone https://github.com/Premyer/Aura.git
cd Aura
```

2. Build the project:
```bash
./gradlew build
```

3. Build APK:
```bash
./gradlew assembleRelease
```

The APK will be generated at: `app/build/outputs/apk/release/app-release.apk`

### Configuration

Update the API endpoint in `app/src/main/java/com/premyer/aura/data/AuraApiService.kt`:

```kotlin
private val baseUrl = "https://ais-pre-6nunyp4pojxildv7adxgq3-523251667507.europe-west2.run.app"
```

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
- **Retrofit** - REST client
- **GSON** - JSON serialization

### License

MIT License - see LICENSE file for details

### Author

**Premyer** - [GitHub Profile](https://github.com/Premyer)

---

**Version:** 1.0.0  
**Last Updated:** 2026-05-06
