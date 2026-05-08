# Mic2Bluetooth

Stream your Android phone's microphone live to a connected Bluetooth speaker, headset, or earbuds.

Useful for turning a phone into a wireless mic into a BT speaker — small public speaking, field recording monitor, hearing assistance, etc.

## Features

- **A2DP mode (default):** routes mic audio as media to any paired Bluetooth speaker. ~150–250 ms latency, full audio bandwidth.
- **SCO low-latency mode:** routes via the Bluetooth HFP profile to headsets/earbuds. ~40–80 ms latency, narrowband speech quality. Pure speakers without a mic profile won't appear.
- **Toggle / Hold-to-speak:** tap the big red button to keep streaming, or hold it for push-to-talk.
- Auto-detects when a Bluetooth device connects/disconnects while the app is open.

## Requirements

- Android 12 (API 31) or newer.

## Permissions

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Capture from the microphone. |
| `MODIFY_AUDIO_SETTINGS` | Switch audio routing modes. |
| `BLUETOOTH_CONNECT` | Required only when low-latency (SCO) mode is enabled, to route to a Bluetooth communication device. |

The app does not access the network, contacts, location, or any other data.

## Build from source

Requires JDK 17 and the Android SDK with `platforms;android-34` and `build-tools;34.0.0+`.

```sh
./gradlew assembleDebug
# or, for a signed release:
./gradlew assembleRelease \
  -PreleaseStoreFile=/path/to/release.jks \
  -PreleaseStorePassword=… -PreleaseKeyAlias=… -PreleaseKeyPassword=…
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

## License

[GPL-3.0-or-later](LICENSE).
