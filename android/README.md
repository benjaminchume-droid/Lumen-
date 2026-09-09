# Lumen Android

## Local build

```bash
cd android
gradle wrapper --gradle-version 8.9   # first time only

export LUMEN_KEYSTORE_PATH=app/release.keystore
export LUMEN_KEY_ALIAS=lumen
export LUMEN_KEY_PASSWORD=...
export LUMEN_STORE_PASSWORD=...

./gradlew assembleRelease
```

## CI secrets (required for signed release)

| Secret | Description |
|--------|-------------|
| `SIGNING_KEY_STORE_BASE64` | **Pure** base64 of the `.jks` / `.keystore` file |
| `LUMEN_KEY_ALIAS` | Key alias (e.g. `lumen`) |
| `LUMEN_KEY_PASSWORD` | Key password |
| `LUMEN_STORE_PASSWORD` | Keystore password |

### Encode the keystore correctly

**Linux:**
```bash
base64 -w0 release.keystore > keystore.b64
# paste the *entire* contents of keystore.b64 into the secret (no quotes, no spaces)
```

**macOS:**
```bash
base64 -i release.keystore | tr -d '\n' > keystore.b64
```

**Common failure:** `base64: invalid input`
- Secret was pasted with newlines, quotes, or PEM headers
- Secret is the raw binary keystore instead of base64
- Secret was truncated

Re-encode with the commands above and update the GitHub Action secret.

## CI

Tag `v1.0.0` or run **Build & Release APK** (workflow_dispatch).

The workflow:
1. Strips whitespace from base64 and validates alphabet before decode
2. Bootstraps Gradle 8.9 wrapper if missing
3. Builds signed `assembleRelease`
4. Uploads APK artifact + creates a GitHub Release

## In-app updates

Settings → **Check for updates** hits GitHub Releases (idempotent: same tag never re-installed).
