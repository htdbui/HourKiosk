# Hour Kiosk

A minimal Android clock showing `HH` above `mm` on a black screen.

## Build

Open in Android Studio and build the debug APK, or run with Gradle 8.9+:

```bash
gradle :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Install and enable true kiosk mode

True kiosk restrictions require a fully managed/device-owner device. On a factory-reset test device with no accounts configured:

```bash
adb install -r app-debug.apk
adb shell dpm set-device-owner com.example.hourkiosk/.KioskAdminReceiver
adb shell am start -n com.example.hourkiosk/.MainActivity
```

Without device-owner provisioning, Android only permits immersive mode / screen pinning and the user may still escape using system controls.

## Exit

Tap the small `×` at the top-right. This stops lock-task mode and opens the home screen.

## Limitations

Android does not let an ordinary app suppress every physical key. Full lockdown depends on device-owner provisioning, Android version, OEM firmware, and optionally an EMM. The power button and forced reboot cannot be reliably disabled by this app.
