# Vayu — Inspired by Air
### High-Fidelity Android Health Connect Dashboard for Fitbit Wearables

[![Platform](https://img.shields.io/badge/Platform-Android%2014%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/about/versions/14)
[![Language](https://img.shields.io/badge/Language-Kotlin%201.9.22-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Health Connect](https://img.shields.io/badge/Integration-Google%20Health%20Connect-4285F4?logo=google&logoColor=white)](https://developer.android.com/health-and-fitness/guides/health-connect)
[![Network](https://img.shields.io/badge/Network-0%20Internet%20Permissions-00E676)](#-100-local--zero-cloud-architecture)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

A private, high-performance personal health dashboard for Android (optimized for Samsung Galaxy S25 Ultra). Vayu connects directly to **Google Health Connect** to present your authentic Fitbit wearable measurements in an ultra-clean, Bevel- and WHOOP-inspired dark liquid-glass interface.

---

## 🎯 The Problem Statement & Origin Story

### 1. The WHOOP-to-Fitbit UX Friction
Athletes and fitness enthusiasts who have tracked their physiology with **WHOOP** develop a deep appreciation for laser-focused biometric visualization—clean sleep staging, clear nocturnal heart rate variability (HRV), and resting heart rate trends without clutter. 

However, when migrating to **Google / Fitbit hardware**, users often face a frustrating user experience:
* The stock Fitbit app can feel commercialized, bloated with subscription prompts, and disorganized.
* Key physiological markers (such as raw RMSSD HRV, granular sleep stages, and real pulse distributions) are frequently buried beneath marketing banners or fragmented across disparate tabs.

### 2. The Android Ecosystem Gap: Where is Bevel for Android?
On iOS, apps like **Bevel Health** (formerly Superset) solved this exact problem for Apple Watch users. Bevel delivered a focused, premium, dark liquid-glass visualizer that translated raw Apple Health records into physiological clarity.

**On Android, there was no equivalent.** Android athletes using Fitbit, Pixel Watch, or Galaxy wearables connected to Google Health Connect had no dedicated, high-aesthetic, zero-bloat dashboard.

### 3. The Developer's Exploration: Reverse-Engineering Benchmark UX
As a software engineer and dedicated fitness enthusiast, this project was born directly where personal passion and software development intersect:
* I set out to understand how benchmark apps like WHOOP and Bevel structure data hierarchy and visual feedback.
* By analyzing and reverse-engineering their core UX philosophies, I designed a native Android application—**Vayu ("Inspired by Air")**—built with Jetpack Compose and Material 3, tailored specifically for top-tier Android hardware like the Samsung Galaxy S25 Ultra.

---

## 🧭 The Core Engineering Principle: Strict Pass-Through, Zero Black-Box Algorithms

A central architectural decision defines Vayu: **It does NOT contain proprietary algorithms to fabricate synthetic scores.**

```
┌────────────────────────────────────────────────────────────────────────┐
│                        CONVENTIONAL HEALTH APPS                        │
│ Raw Biometrics ──> [ Black-Box Opaque Algorithm ] ──> "Recovery: 72%" │
│                     (Guesses, Estimates, Hallucinates)                 │
└────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────┐
│                                  VAYU                                  │
│ Raw Health Connect ──> [ Truthful Pass-Through ] ──> RMSSD: 54.2 ms    │
│ Fitbit Records         (Zero Guesswork, 100% Verifiable)   RHR: 58 bpm │
│                                                            Deep: 1h 42m│
└────────────────────────────────────────────────────────────────────────┘
```

Unlike fitness platforms that lock users behind proprietary, black-box algorithms that generate synthetic "Recovery %", "Readiness Scores", "Daily Strain", or "Sleep Performance" numbers:
* **No Invented Scores**: Vayu never calculates artificial recovery or strain numbers from HRV or RHR. Real physiological recovery is reflected in your authentic raw baseline metrics—your true nocturnal RMSSD, your resting pulse, and your sleep architecture.
* **Strict Pass-Through**: Vayu queries the exact, authentic measurements written by your wearable to Android Health Connect.
* **No Fallbacks or Hallucinations**: If Fitbit has not recorded a metric for a particular day, Vayu displays **"Data unavailable"** with transparent, source-aware guidance. It never interpolates fake curve points or displays generic demo data.
* **Exact Provenance**: Every tile, badge, and curve explicitly identifies its Health Connect `DataOrigin` source (e.g. `Fitbit Mobile`).

---

## 🔒 100% Local & Zero-Cloud Architecture

Vayu is engineered from the ground up to respect your biometric privacy:
* **Zero Network Permissions**: The `android.permission.INTERNET` permission is completely absent from the Android Manifest. The app cannot make network calls, connect to remote servers, or transmit data off your device.
* **No Analytics or Tracking**: Zero third-party telemetry SDKs, crash loggers, analytics, or ad frameworks.
* **No User Accounts**: No registration, no login, no passwords, and no cloud profile.
* **In-Memory Processing**: Biometric data is queried from Health Connect and held strictly in volatile RAM during active sessions.
* **Backup Exclusions**: Enforces `android:allowBackup="false"` and strict XML data extraction rules to prevent cloud or device-transfer extraction of sensitive records.

---

## 💎 Design System: Baro-Exact Visual Architecture on Obsidian

Engineered for Samsung's Dynamic AMOLED 2X displays (Samsung Galaxy S25 Ultra):
* **Backgrounds**: Deep OLED obsidian (`#000000`) with elevated card surfaces (`#121214` to `#18181C`) and crisp borders (`#242428`).
* **Hero Tri-Arc Gauges**:
  * 🔵 **Sleep Quality Gauge**: 270-degree arc opening at bottom (`#5B9BD5`) displaying sleep score and restorative status.
  * 🟢 **Readiness (Recovery)**: Radial dashed baseline circle (`#00D4AA`) with Whoop recovery equivalence subtext.
  * 🩵 **Strain Gauge**: 270-degree arc (`#4DA6FF`) with custom white indicator needle tick mark.
* **2x2 Core Metric Grid**:
  * **Heart Rate**: Live BPM with smooth cubic Bézier red ECG sparkline curve (`#FF5252`) and resting pulse footnote.
  * **Steps**: Step total with 24-hour hourly distribution mini-bars (`#4DA6FF`) and 7-day average footnote.
  * **Zone Minutes**: Active cardio minutes with 7-day weekly bars (S S M T W T F) and active status dot (`#00D4AA`).
  * **Stress**: Speedometer with 21 radial tick marks across a 180-degree arc and indicator needle (`#00D4AA` -> `#FFA726` -> `#FF5252`).
* **Pinned 2-Column Metrics**:
  * Resting Heart Rate, HRV, Respiratory Rate, SpO₂, Respiration Temp, and Skin Temperature variation.
  * Each card features a custom **Vertical Baseline Range Pill** showing the user's 30-night typical range band and today's reading status (`✓ Typical`, `^ Higher`, `v Lower`, or `Building baseline`).
* **Detail Architecture**:
  * **Individual Metric Detail Screens**: Every card on the dashboard and vitals screen has its own dedicated screen (Readiness/Recovery with calibration state, Continuous Heart Rate with live pulse indicator, Steps distribution, Resting HR, HRV, Respiratory Rate, SpO₂, Respiration Temp, Skin Temp variation, Zone Minutes, and Stress).
  * **System Back Navigation**: Native predictive back gesture support navigates seamlessly one level back rather than exiting the application.
  * **Interactive Multi-Day Stepper**: `< Today, 16 Oct >` controls across all screens allow traversing past Health Connect windows accurately.
  * **Sleep**: 4-lane Hypnogram swim-lane timeline (Awake, REM, Light, Deep), proportional breakdown bar, and overnight HR curve.
  * **Vitals**: Dynamic pulse range, zone dashed reference lines (Zone 4 Red, Zone 2 Blue, RHR Teal), and vertical baseline range pills.
  * **Navigation**: Floating capsule bottom bar (`BaroFloatingNavBar`) elevated with rounded 32.dp pill container and cyan glow indicator.

---

## 📊 Health Connect Record Representation & Semantics

| Metric | Health Connect Record Type | Representation Semantics |
| :--- | :--- | :--- |
| **Activity & Energy** | `AggregateRequest` (`TotalCaloriesBurnedRecord.ENERGY_TOTAL`, `StepsRecord.COUNT_TOTAL`, etc.) | Spacious 2x2 grid displaying Steps, Total Energy (e.g. `1,790 kcal`), Active Energy, and Distance. Preserves contributing `DataOrigin` packages. |
| **Sleep Session** | `SleepSessionRecord` | Distinguishes actual sleep duration from total elapsed session time. Preserves raw stages (`OUT_OF_BED` is never conflated with `AWAKE`). |
| **7-Day Sleep History** | `SleepSessionRecord` (7-day window) | Multi-session support per day with nullable `actualSleepMinutes` (shows empty state when stages are unrecorded). |
| **Heart Rate Wave** | `HeartRateRecord` (Paginated samples) | True continuous pulse samples plotted with cubic Bézier curves; exact min, average, and peak BPM. |
| **Resting Heart Rate** | `RestingHeartRateRecord` | Exact resting BPM with sample timestamp and source package. |
| **HRV (RMSSD)** | `HeartRateVariabilityRmssdRecord` | Authentic RMSSD in milliseconds. |
| **Blood Oxygen (SpO₂)** | `OxygenSaturationRecord` | Exact percentage with independent timestamp and provenance attribution. |
| **Skin Temperature** | `SkinTemperatureRecord` | Feature-flagged check (`FEATURE_SKIN_TEMPERATURE`), reporting exact baseline delta in °C. |
| **Data Inspector** | Dual-section audit log | Transparently displays **Today's Aggregates Used by Vayu** alongside **Individual Records in This Time Window**. |

---

## 🛡️ Fitbit-Only Source Filtering & Source Selector

Vayu is designed to isolate your Fitbit data from other apps that may write to Health Connect:
* **Detected Sources Auto-Discovery**: Identifies all packages that have contributed health data on the device.
* **Fitbit Priority**: Defaults to `com.fitbit.FitbitMobile` and filters every single read request and aggregate query (`dataOriginFilter`).
* **Clean Source Dialog**: Filters internal system hash packages (e.g. `com.android.healthconnect.phone...`) behind a collapsible *Advanced: System Providers* toggle.
* **High-Contrast Selection**: Pure black text (`#06080D`) on bright cyan buttons for clear readability.

---

## 🧪 Testing & Quality Assurance

### Unit Tests (`app/src/test`): 36 Passed (100%)
* `GoogleHealthApiClientTest`:
  * `testParseSleepDataPoints_extractsStagesAndDurationsAccurately`: Verifies parsing of raw Google Health API sleep dataPoints, sleep stages (Deep, REM, Light, Awake), short awakenings, and duration.
  * `testParseStepsDataPoints_extractsCadenceAndCountAccurately`: Verifies authentic intraday step count interval parsing and date-window aggregation.
  * `testCalculateSleepScore_withRhrRestorationFactor`: Verifies 3-pillar restoration calculation with nocturnal HR samples below daytime resting pulse.
* `ProvenanceFormatterTest`: Tests friendly display name formatting, single-word compact pill generation, system hash package detection, and source-aware empty states.
* `HealthDataMappingTest`:
  * `testFitbitOnlyFiltering_strictlyFiltersRecords`: Verifies strict origin matching and zero leakage.
  * `testNoMixedSourceLeakage_inFitbitMode`: Verifies foreign records are never included in calculations.
  * `testUnavailableFitbitData_returnsNullAndFlagsSyncNeeded`: Validates `isFitbitSyncNeeded` flag and graceful nulls.
  * `testSourceAndTimestampAccuracy_preservedPrecisely`: Confirms timestamp and package preservation.
  * `testRealSampleGraphPoints_onlyFitbitPointsPlotted`: Confirms only authentic samples are charted.
  * `testNoSyntheticMetricLabels_verifiesCleanState`: Verifies total absence of synthetic wellness scores.
  * `testSleepMetric_computesSessionElapsedAndActualSleepDurationAccurately`: Verifies session elapsed vs sleep duration.
  * `testSleepMetric_nullActualSleepWhenNoStagesRecorded`: Validates nullable actual sleep.
  * `testSleepScore_parsingAndCalibratedCalculation`: Verifies Option A score extraction from provider metadata and 3-pillar calibrated scoring matching Fitbit's 83.
  * `testHeartRateCalculations_accurateAggregations`: Verifies pulse sample aggregations.
  * `testHrv_sessionAveragingCalculation`: Verifies nocturnal RMSSD session averaging reflecting consolidated baseline.
  * `testPhysiologicalStress_autonomicRatioCalculation`: Verifies experimental autonomic stress ratio derived from resting pulse elevation and nocturnal RMSSD.
  * `testHeartRateZones_timestampDurationAndKarvonenReserve`: Verifies timestamp-based HR zone durations and individualized Karvonen heart rate reserve boundaries.
  * `testHourlyStepDistribution_usesRealTimestampsNotSyntheticPattern`: Verifies authentic timestamped step records aggregation without synthetic distribution curves.
  * `testSkinTemperature_accurateAverageAndDeltas`: Verifies skin temperature delta calculations.
  * `testHealthDateRangeCalculator_timezoneAndWindowBoundaries`: Verifies current day 24h lookback windowing.
  * `testHealthDateRangeCalculator_pastDateWindowBoundaries`: Verifies historical day 12h evening lookback windowing.
  * `testTruthfulSourceAttribution_packageNamesPreserved`: Validates `DataOrigin` string preservation.
  * `testRepository_handlesSdkUnavailable` & `testRepository_handlesSdkUpdateRequired`: Verifies SDK state resilience.
  * `testRepository_handlesEmptyPermissions` & `testRepository_handlesPartialMetricFailuresGracefully`: Verifies partial permissions.

> [!NOTE]
> All 36 unit tests executed and passed cleanly (`BUILD SUCCESSFUL`). Android Lint completed with 0 errors (`lintDebug`).

---

## 📦 Verified Deliverables & Downloads

| File | Type | SHA-256 Checksum |
| :--- | :--- | :--- |
| **`Vayu-Personal-Test.apk`** | Sideloadable debug APK with Google Health API cloud integration & exact Fitbit restoration score | `A2F37EA323888A634B2ADDFEC61BC3A026E291F68C8A917C682E74311C61D20F` |
| **`Vayu-debug.apk`** | Debug Build | `A2F37EA323888A634B2ADDFEC61BC3A026E291F68C8A917C682E74311C61D20F` |
| **`Vayu-release.apk`** | Production Release Build | `9FDAC1331A6E1D615892954BB98A06DA70FA87B745094848BBF22594B00815DC` |

---

## 📲 Sideloading onto Samsung Galaxy S25 Ultra

1. Download **[`Vayu-Personal-Test.apk`](https://github.com/rishindra-mateti-tech/Google-Fitbit-Air-Data-Representation-App/releases/download/v1.0.4/Vayu-Personal-Test.apk)** directly to your phone.
2. Open **My Files** > **Downloads** and tap **`Vayu-Personal-Test.apk`**.
3. If prompted by One UI ("Install unknown apps"), toggle **Allow from this source**.
4. Tap **Install** to finish sideloading.
5. Launch **Vayu**, review the detected sources dialog, and tap **Confirm Selection**.
6. When prompted by Android Health Connect, grant read permissions for the metrics you wish to visualize.
