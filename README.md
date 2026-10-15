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

---

## 🔒 Fitbit-Only Source Filtering & Zero Data Leakage

To guarantee complete fidelity to your wearable:
* **Automatic Origin Detection**: Detects all health record contributors on the device.
* **Fitbit Priority**: Defaults to `com.fitbit.FitbitMobile` and strictly filters every single read request and aggregate query (`dataOriginFilter`).
* **Zero Cross-Contamination**: In-memory verification discards any non-Fitbit records (e.g. Samsung Health, Google Fit) when in Fitbit mode.
* **Fitbit Sync Needed State**: If Fitbit has not yet exported data to Health Connect, Vayu presents a helpful "Fitbit Sync Needed" guide rather than silently substituting data from another app.
* **Source Selector**: A source pill button in the top bar allows checking detected packages or explicitly choosing "All Health Connect Sources" (labeled as mixed-source).

---

## 🧪 Testing & Continuous Integration

### Unit Tests (`app/src/test`): 23 Passed (100%)
* `testFitbitOnlyFiltering_strictlyFiltersRecords`: Verifies strict origin matching and zero leakage.
* `testNoMixedSourceLeakage_inFitbitMode`: Verifies foreign records are never included in calculations.
* `testUnavailableFitbitData_returnsNullAndFlagsSyncNeeded`: Validates `isFitbitSyncNeeded` flag and graceful nulls.
* `testSourceAndTimestampAccuracy_preservedPrecisely`: Confirms timestamp and package preservation.
* `testRealSampleGraphPoints_onlyFitbitPointsPlotted`: Confirms only authentic samples are charted.
* `testNoSyntheticMetricLabels_verifiesCleanState`: Verifies total absence of synthetic wellness scores.
* `testSleepMetric_computesSessionElapsedAndActualSleepDurationAccurately`: Verifies session elapsed vs sleep duration.
* `testSleepMetric_nullActualSleepWhenNoStagesRecorded`: Validates nullable actual sleep.
* `testHeartRateCalculations_accurateAggregations`: Verifies pulse sample aggregations.
* `testSkinTemperature_accurateAverageAndDeltas`: Verifies skin temperature delta calculations.
* `testTruthfulSourceAttribution_packageNamesPreserved`: Validates `DataOrigin` string preservation.
* `testRepository_handlesSdkUnavailable` & `testRepository_handlesSdkUpdateRequired`: Verifies SDK state resilience.
* `testRepository_handlesEmptyPermissions` & `testRepository_handlesPartialMetricFailuresGracefully`: Verifies partial permissions.

> [!NOTE]
> **Execution Status**: All 23 unit tests executed and passed cleanly (`BUILD SUCCESSFUL`). Instrumented tests in `app/src/androidTest` compiled cleanly, but were **not executed** because no physical device or emulator is currently attached to the build environment (`adb devices` is empty).

---

## 📦 Final Personal Test Deliverable

| File | Purpose | SHA-256 Checksum |
| :--- | :--- | :--- |
| **`Vayu-Personal-Test.apk`** | Sideloadable debug APK with authentic custom app icon for personal testing on Samsung Galaxy S25 Ultra | `88E0166E077FD5BB0019706C130ACF2DE67963926D415E79927445028378F556` |

---

## 📱 Sideloading onto Samsung Galaxy S25 Ultra

1. Transfer or download **`Vayu-Personal-Test.apk`** directly to your Galaxy S25 Ultra.
2. Open **My Files** > **Downloads** (or your transfer folder) and tap **`Vayu-Personal-Test.apk`**.
3. If prompted by One UI ("Install unknown apps"), toggle **Allow from this source**.
4. Tap **Install** to finish sideloading.
5. Launch **Vayu**, review the detected sources dialog, and tap **Continue with Fitbit Only**.
6. When prompted by Health Connect, grant read permissions for the metrics you wish to visualize.

