# Vayu — Inspired by Air
### Google Fitbit Air Data Representation App for Android

A private, high-performance personal health dashboard for Android (optimized for Samsung Galaxy S25 Ultra). Vayu connects directly to **Google Health Connect** to present your authentic Fitbit wearable measurements in an ultra-clean, Bevel- and WHOOP-inspired dark liquid-glass interface.

---

## 🌬️ Core Philosophy: Strict Pass-Through, No Invented Metrics

Vayu is built upon a single, non-negotiable principle: **Zero Synthetic Scores**.

Unlike conventional health applications that fabricate proprietary "Recovery %", "Readiness Scores", "Strain", "Sleep Performance", or "Sleep Consistency" numbers:
* **Pure Pass-Through Architecture**: Vayu queries the original raw measurements that Google and Fitbit save into your device's local Health Connect store.
* **No Fabricated or Estimated Data**: If a metric is not recorded or reported by your wearable, Vayu displays **"Data unavailable"** with no mock charts or simulated spline curves.
* **No Label Conflation**: No invented scores derived from HRV, RHR, or sleep duration. Metrics are shown by their authentic physiological names (e.g. RMSSD HRV in ms, Resting Heart Rate in bpm, Sleep Duration in hours/minutes).
* **100% Local & Private**: Absolutely zero network permissions (`android.permission.INTERNET` is completely absent), no backend servers, no analytics, no cloud databases, and no cloud backups of sensitive health data (`android:allowBackup="false"`).

---

## 💎 Design System: Liquid Glass & OLED Dark Mode

* **Palette**: Deep space obsidian (`#06080D`), frosted glass surfaces, and translucent card layers.
* **Authentic Vitals Accents**:
  * 🩵 **Vayu Air Cyan** (`#00F2FE` / `#4FACFE`) — Signature theme & steps
  * 💜 **Sleep Indigo & Lavender** (`#3D5AFE` / `#7C4DFF`) — Sleep stages (Deep, REM, Light, Sleeping, Awake, Out of Bed)
  * ❤️ **Cardiovascular Crimson** (`#FF3366`) — Resting Heart Rate & authentic pulse samples
  * 💚 **HRV Emerald** (`#00E676`) — RMSSD Heart Rate Variability
  * 🧡 **Metabolic Amber** (`#FF9100`) — De-duplicated Active & Total Caloric burn
  * 🌊 **Teal & Mint** (`#00E5FF`) — Blood Oxygen (SpO₂), Respiration & Skin Temperature
* **Customizable Dashboard**: Long-press/edit reordering allows personalized card arrangement with automatic layout persistence.

---

## 📊 Health Connect Implementation & Raw Semantics

| Category | Record Type | Handling & Attribution |
| :--- | :--- | :--- |
| **Sleep Session** | `SleepSessionRecord` | Preserves raw stage types (`OUT_OF_BED` is preserved distinct from `AWAKE`). Separates total session elapsed time from actual sleep duration. |
| **7-Day Sleep History** | `SleepSessionRecord` | Queries authentic recorded sessions over a 7-day window. Shows empty state if no sessions exist. |
| **Resting Heart Rate** | `RestingHeartRateRecord` | Exact recorded BPM and timestamp with source package attribution. |
| **Heart Rate Variability** | `HeartRateVariabilityRmssdRecord` | Raw RMSSD value in milliseconds. |
| **Heart Rate Curve** | `HeartRateRecord` | Paginated retrieval (1,000 records/page) extracting authentic sample points for the curve; displays true min/max/average. |
| **Activity Totals** | `client.aggregate(...)` | Uses Health Connect Aggregate APIs (`COUNT_TOTAL`, `DISTANCE_TOTAL`, `ENERGY_TOTAL`) for user-priority de-duplicated totals across apps. Lists contributing package origins. |
| **Respiratory & Oxygen** | `RespiratoryRateRecord`, `OxygenSaturationRecord` | Exact breaths per minute and SpO₂ percentage. |
| **Skin Temperature** | `SkinTemperatureRecord` | Feature-flag checked (`FEATURE_SKIN_TEMPERATURE`), accurately averaging deltas with baseline and min/max delta reporting. |
| **Database Inspector** | Raw Records | Real-time audit tool inspecting raw Health Connect database entries, record IDs, and timestamps. |

---

## 🛡️ Truthful Source Attribution & Robust Error Handling

* **Truthful Provenance**: Cards display the authentic Health Connect `DataOrigin` package name (such as `com.fitbit.FitbitMobile`, `com.google.android.apps.fitness`, or `com.samsung.health`). Labels never default to unverified hardware strings like "Fitbit Air".
* **Health Connect SDK Lifecycle**:
  * Checks SDK availability (`AVAILABLE`, `UPDATE_REQUIRED`, `UNAVAILABLE`) and guides the user if an update from Google Play is needed.
  * Checks feature support (e.g. Skin Temperature availability) before querying.
  * Handles partial permissions gracefully: queries only metrics granted by the user without failing other cards.
  * Safe error surfaces: Swallowed exceptions are replaced with visible, informative error banners on the dashboard.
  * Immediate refresh: Listens to permission launcher results and `onResume()` to reload records without requiring an app restart.

---

## 🧪 Testing & Continuous Integration

The repository includes comprehensive unit and instrumented tests:

### Unit Tests (`app/src/test`)
* `testSleepMetric_computesSessionElapsedAndActualSleepDurationAccurately`: Verifies session elapsed vs sleep duration calculation, out-of-bed isolation, and stage breakdown.
* `testSleepMetric_nullActualSleepWhenNoStagesRecorded`: Ensures `actualSleepDurationMinutes` is null when no stages are reported.
* `testHeartRateCalculations_accurateAggregations`: Verifies min, max, average, and latest calculations.
* `testSkinTemperature_accurateAverageAndDeltas`: Verifies multi-delta averaging and baseline handling.
* `testTruthfulSourceAttribution_packageNamesPreserved`: Validates `DataOrigin` string preservation.
* `testRepository_handlesSdkUnavailable` & `testRepository_handlesSdkUpdateRequired`: Verifies SDK state resilience.
* `testRepository_handlesEmptyPermissions` & `testRepository_handlesPartialMetricFailuresGracefully`: Verifies partial permissions and non-fatal error propagation.

### Instrumented Tests (`app/src/androidTest`)
* `testHealthConnectManager_sdkStatusResolution`: Validates Health Connect SDK status querying on device.
* `testDashboardUi_displaysDataUnavailableWhenMetricsNull`: Asserts "Data unavailable" rendering on null metrics and verifies total absence of synthetic score labels ("Readiness", "Recovery (WHOOP)", "Strain (WHOOP)", etc.).
* `testDashboardUi_displaysUpdateRequiredBanner`: Tests update-required prompt rendering.

### CI Workflow Specification
Automated workflow specification is located at `ci/android.yml` (can be deployed to `.github/workflows/android.yml`):
* Sets up Java 17 (Temurin).
* Runs unit tests: `./gradlew testDebugUnitTest`.
* Runs Android Lint: `./gradlew lintDebug`.
* Assembles debug build: `./gradlew assembleDebug`.
* Uploads debug APK artifacts.

---

## 🔨 Building and Running

### Prerequisites
* JDK 17
* Android SDK 35 (minSdk 28)

### Commands
```powershell
# Run unit tests
./gradlew testDebugUnitTest

# Run lint checks
./gradlew lintDebug

# Build Debug APK
./gradlew assembleDebug

# Build Release APK (unsigned / clean production configuration)
./gradlew assembleRelease
```

---

## 📱 Sideloading onto Samsung Galaxy S25 Ultra

1. Download the generated APK directly to your phone.
2. Open **My Files** on your Galaxy S25 Ultra and tap the APK.
3. If prompted, toggle **Allow from this source**.
4. Tap **Install**.
5. Launch **Vayu**, tap **Grant Read Permissions**, and toggle read access for the metrics you wish to display.
