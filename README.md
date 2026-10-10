# FarmMarket – Jetpack Compose (Kotlin)

1. Android Studio -> File -> Open -> this folder (FarmMarketCompose). Wait for Gradle sync.
2. Backend must be running (see original README). Server address:
   - Emulator:   http://10.0.2.2:5000/api
   - Real phone: http://<laptop-ip>:5000/api   (same Wi-Fi, port 5000 open in firewall)
   Change it in the app: Profile -> Server address. Default is set in Store.kt.
3. Run: pick a device, press the green Run button.
4. APK: Build -> Build Bundle(s) / APK(s) -> Build APK(s).
   Debug:   app/build/outputs/apk/debug/app-debug.apk
   Release: ./gradlew assembleRelease  -> app/build/outputs/apk/release/app-release.apk
Test login: 9876543210 / farmer123   (or "Continue in Demo Mode")
