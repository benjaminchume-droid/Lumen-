# Lumen Android

## Local build

```bash
cd android
gradle wrapper --gradle-version 8.9

export LUMEN_KEYSTORE_PATH=app/release.keystore
export LUMEN_KEY_ALIAS=lumen
export LUMEN_KEY_PASSWORD=...
export LUMEN_STORE_PASSWORD=...

./gradlew assembleRelease
```

## CI

Tag `v1.0.0` or run the **Build & Release APK** workflow.
Secrets required:
- `SIGNING_KEY_STORE_BASE64`
- `LUMEN_KEY_ALIAS`
- `LUMEN_KEY_PASSWORD`
- `LUMEN_STORE_PASSWORD`
