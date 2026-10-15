# Vayu Release Checklist & Deployment Guide

This document defines the release standards, security checks, and verification procedures for **Vayu ("Inspired by Air")**.

---

## 1. Security & Manifest Verifications

- [ ] **Zero Internet Permission**: Verify `app/src/main/AndroidManifest.xml` does not contain `android.permission.INTERNET`.
- [ ] **Backup Exclusion**: Verify `android:allowBackup="false"`, `android:dataExtractionRules="@xml/data_extraction_rules"`, and `android:fullBackupContent="@xml/backup_rules"`.
- [ ] **Health Connect Permissions**: Verify only truthful read-only permissions are requested. No write permissions, no unneeded permissions.
- [ ] **No Mock Data / Fallbacks**: Verify all screens display "Data unavailable" or empty states when records are missing. Never invent scores (Readiness, Recovery, Strain, Sleep Performance).
- [ ] **Truthful Attribution**: Verify every metric indicates its authentic `DataOrigin` package name (e.g., `com.fitbit.FitbitMobile`), and aggregated metrics are labeled as "Health Connect aggregated total" with contributing sources.

---

## 2. Release Signing Configuration

Release builds must never use the default Android debug keystore.

### Option A: Local Keystore (`keystore.properties`)
1. Create your release keystore (if not already created):
   ```bash
   keytool -genkey -v -keystore release.keystore -alias vayu_release_key -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Copy `keystore.properties.example` to `keystore.properties`:
   ```properties
   storeFile=../release.keystore
   storePassword=your_keystore_password
   keyAlias=vayu_release_key
   keyPassword=your_key_password
   ```
   *(Note: `keystore.properties` and `*.keystore` are git-ignored and must never be committed).*

### Option B: CI/CD Environment Variables
Configure the following secrets in GitHub Repository Secrets or your build pipeline:
- `KEYSTORE_FILE`: Path or base64-decoded location of your release keystore
- `KEYSTORE_PASSWORD`: Keystore password
- `KEY_ALIAS`: Key alias
- `KEY_PASSWORD`: Key password

---

## 3. Pre-Flight Test & Lint Validation

Run all unit tests and lint checks cleanly before building release artifacts:

```powershell
# Run unit test suite
./gradlew testDebugUnitTest

# Run Android lint
./gradlew lintDebug
```

All tests must pass with 0 failures and 0 errors.

---

## 4. Build Artifacts

Generate the production APK and/or Android App Bundle:

```powershell
# Build signed/unsigned release APK
./gradlew assembleRelease

# Build Google Play App Bundle (AAB)
./gradlew bundleRelease
```

Artifact outputs:
- Release APK: `app/build/outputs/apk/release/app-release.apk` (or `app-release-unsigned.apk` if no keystore configured)
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`

---

## 5. Checksum Verification

Always calculate and publish the SHA-256 checksum for distribution artifacts:

```powershell
Get-FileHash -Algorithm SHA256 app/build/outputs/apk/release/app-release.apk
```

Linux / macOS:
```bash
sha256sum app/build/outputs/apk/release/app-release.apk
```

---

## 6. Google Play Store & Health Connect Declaration

When submitting Vayu to the Google Play Console:
1. **Health Connect Declarations**: Declare read access for the specific Health Connect data types used (Steps, Distance, Calories, Heart Rate, Resting Heart Rate, HRV, Sleep, Respiration, Oxygen Saturation, Skin Temperature).
2. **Data Safety Form**:
   - Data collection: **No** (Vayu does not collect or transmit user data off the device).
   - Data sharing: **No** (No third-party SDKs, no trackers, no ads).
   - Data handling: Ephemeral in-memory reading only.
3. **Privacy Policy**: Provide the URL to `PRIVACY_POLICY.md` on the GitHub repository.

---

## 7. GitHub Actions / CI Workflow Note

If pushing changes to `.github/workflows/` using the GitHub CLI (`gh`), ensure your authorization token has the `workflow` scope:
```bash
gh auth refresh -h github.com -s workflow
```
