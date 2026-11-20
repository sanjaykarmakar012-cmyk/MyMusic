# MyMusic
Open-source online music player (Kotlin + Jetpack Compose) for YouTube's music category.

- Trending music + search (YouTube Data API v3, category 10 = Music)
- Playback via the official YouTube IFrame player (android-youtube-player)
- Customisation: light/dark/system, AMOLED black, Material You, accent colours, region
- Queue with auto-advance, previous/next, animated list and player

## Get the APK
**GitHub Actions (no setup):** push this folder to a GitHub repo -> Actions -> "Build APK" -> download artifact `MyMusic-debug-apk`.

**Android Studio:** File -> Open this folder -> Build -> Build APK(s). Or run `gradle assembleDebug`
(Gradle 8.9, JDK 17) -> `app/build/outputs/apk/debug/app-debug.apk`.

On first launch open Settings and paste a YouTube Data API key.
