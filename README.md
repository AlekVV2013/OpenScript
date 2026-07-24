# Free Assistant - Android Voice Assistant App

A comprehensive Android voice assistant application built with Kotlin, featuring:

## Features

- **Voice Input**: Speech recognition for hands-free operation
- **Text-to-Speech Output**: Spoken responses with male/female voice selection
- **Multi-language Support**: English and Russian language support
- **Task System**: Modular task handlers for various commands

### Available Commands

#### Time & Alarms
- Current time: "What time is it?" / "Который час?"
- Set alarm: "Set alarm for 07:30" / "Поставь будильник на 07:30"
- Set timer: "Set timer for 10 minutes" / "Поставь таймер на 10 минут"

#### Weather
- Weather: "What is the weather?" / "Какая погода?"

#### Photos
- Index photos: "Index my photos" / "Индексировать мои фото"
- Search photos by name: "Search photos by name for cat" / "Найти фото по имени кошка"
- Search photos by description/tags: "Search photos by description for cat" / "Найти фото по описанию кошка"
- List all tags: "List all tags" / "Покажи все теги"
- List tags for specific photo: "List tags for this photo" / "Покажи теги для этого фото"

#### Notes
- Import notes: "Import notes" / "Импортировать заметки"
- Search notes: "Search notes for meeting" / "Найти заметки про встречу"

#### Apps
- Open apps: "Open calculator" / "Открыть калькулятор"

#### General
- Help: "Help" / "Помощь"
- Show commands: "Commands" / "Команды"

### Settings

Accessible from the sidebar menu:
- Language selection (English/Russian)
- Automatic photo indexing (enabled/disabled)
- Auto-index time selection
- Speech output (enabled/disabled)
- Voice gender (male/female)

## Project Structure

```
FreeAssistant/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        └── main/
            ├── AndroidManifest.xml
            ├── java/com/example/freeassistant/
            │   ├── App.kt
            │   ├── MainActivity.kt
            │   ├── SettingsActivity.kt
            │   ├── ResultAdapter.kt
            │   ├── BaseActivity.kt
            │   ├── LanguageManager.kt
            │   ├── SettingsManager.kt
            │   ├── CatalogValidator.kt
            │   ├── VoiceManager.kt
            │   ├── AutoIndexScheduler.kt
            │   ├── AutoIndexWorker.kt
            │   ├── BootReceiver.kt
            │   ├── notes/
            │   │   └── NotesRepository.kt
            │   ├── photos/
            │   │   ├── PhotoRepository.kt
            │   │   ├── DescriptionEngine.kt
            │   │   └── PhotoDescriptionIndexer.kt
            │   ├── weather/
            │   │   └── WeatherRepository.kt
            │   └── tasks/
            │       ├── TaskModels.kt
            │       ├── RegexUtils.kt
            │       ├── InputNormalizer.kt
            │       ├── IntentTranslator.kt
            │       ├── PhraseCatalog.kt
            │       ├── WeatherPhrases.kt
            │       ├── TimePhrases.kt
            │       ├── RussianIntentMapper.kt
            │       ├── TaskRegistry.kt
            │       ├── HelpTask.kt
            │       ├── ImportNotesTask.kt
            │       ├── IndexPhotosTask.kt
            │       ├── ListTagsTask.kt
            │       ├── ListPhotoTagsTask.kt
            │       ├── SearchNotesTask.kt
            │       ├── SearchPhotosByNameTask.kt
            │       ├── SearchPhotosByDescriptionTask.kt
            │       ├── OpenAppTask.kt
            │       ├── WeatherTask.kt
            │       ├── CurrentTimeTask.kt
            │       ├── SetAlarmTask.kt
            │       ├── SetTimerTask.kt
            │       └── TimeParser.kt
            └── res/
                ├── layout/
                │   ├── activity_main.xml
                │   ├── activity_settings.xml
                │   └── item_result.xml
                ├── menu/
                │   └── drawer_menu.xml
                └── values/
                    ├── strings.xml
                    ├── themes.xml
                    └── colors.xml
                └── values-ru/
                    └── strings.xml
```

## Building

### Android Studio
1. Open the project in Android Studio
2. Sync Gradle: File → Sync Project with Gradle Files
3. Build APK: Build → Build Bundle(s) / APK(s) → Build APK(s)
4. Output: `app/build/outputs/apk/debug/app-debug.apk`

### Command Line
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

## Requirements

- Android Studio with Kotlin support
- Android SDK 34
- Minimum SDK: API 26 (Android 8.0)
- Java 17

## Permissions

The app requires the following permissions:
- INTERNET: For weather data
- RECEIVE_BOOT_COMPLETED: To restore scheduling after reboot
- POST_NOTIFICATIONS: For notifications
- FOREGROUND_SERVICE: For background operations
- RECORD_AUDIO: For speech recognition
- SET_ALARM: For setting alarms via system clock app
- READ_MEDIA_IMAGES: For accessing photos
- READ_MEDIA_VISUAL_USER_SELECTED: For accessing user-selected media

## Notes

- Weather functionality requires an OpenWeatherMap API key (replace `YOUR_API_KEY` in WeatherRepository.kt)
- Alarm and timer functionality delegates to the system clock app
- Photo indexing uses ML Kit for on-device image labeling
- Auto-indexing uses WorkManager with a boot receiver for scheduling

## License

This project is created based on the Free Assistant building guide.
