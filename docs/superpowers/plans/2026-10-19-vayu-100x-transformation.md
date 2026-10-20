# Vayu 100x Transformation: Google Health Parity & Obsidian Visual Overhaul

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform Vayu into an elite, 100x better personal health dashboard with 100% genuine Google Health Connect metrics, multi-timeframe analytics (Day, Week, Month, Year), interactive scrubbable charts, complete Sleep & Resilience 3-pillar breakdowns, and a pristine WHOOP/Bevel-inspired obsidian liquid-glass UI.

**Architecture:** 
1. Expand `HealthConnectManager` and `HealthMetricsRepository` with paginated multi-range queries (7d, 30d, 90d, 365d) for all records (Sleep, HR, RHR, HRV, SpO2, RespiratoryRate, SkinTemp, Steps, Calories) with zero synthetic fallbacks.
2. Model complete Google Health vitals parity including 3-pillar Resilience (Sleep Patterns, Activity Balance, Physical Calmness), Sleep staging with efficiency & sleeping pulse dip, and dynamic multi-range aggregations (`TimeRangeTab`: D, W, M, 3M, Y).
3. Introduce `BaroMultiRangeChart` with interactive touch scrubbing, floating tooltip pills, and typical baseline range bands.
4. Elevate all metric detail screens and the main dashboard to obsidian AMOLED finish (`#000000`), removing broken legacy controls and streamlining navigation.

**Tech Stack:** Kotlin 1.9.22, Jetpack Compose, Material 3, Android Health Connect SDK (1.1.0-alpha07), Coroutines/Flow, JUnit 4.

**Spec / Principles:**
- **Zero Synthetic Hallucinations**: Never invent fake defaults (no fake 420m sleep, 48ms HRV, or zero-filled days). Missing data must show "Incomplete Data" or "No readings yet".
- **Transparent Attribution**: Mark on-device calculated scores with asterisk `*` and exact formula explanations.
- **Full Timeframe Support**: Day (D), Week (W), Month (M), 3 Months (3M), Year (Y).

---

## Global Constraints

- Android 14+ minimum SDK compatibility (target SDK 34/35).
- 100% local on-device processing via Health Connect without mandatory accounts or third-party telemetry.
- Zero regression on existing 45 unit tests.
- High-efficiency Compose layouts tailored for Samsung Galaxy S25 Ultra 120Hz display.

## Review Focus

1. Ensure multi-range historical queries properly handle missing days (nullable values) without crashing or drawing fake zeroes.
2. Verify Resilience 3-pillar breakdown uses genuine Sleep, Steps, and HRV/HR data without hardcoded mocks.
3. Verify interactive chart scrubber correctly calculates touch X coordinates to index mapping without IndexOutOfBoundsException.
4. Ensure SpO2 and Breathing Rate overnight sleep window queries capture records recorded during the previous night's sleep session.
5. Verify back button and navigation bar behavior across all detail screens.

---

### Task 1: Historical Lookback & Multi-Range Aggregations Expansion

**Files:**
- Modify: `app/src/main/java/com/vayu/health/data/HealthConnectManager.kt`
- Modify: `app/src/main/java/com/vayu/health/data/HealthMetricsRepository.kt`
- Modify: `app/src/main/java/com/vayu/health/data/model/HealthDataModels.kt`
- Test: `app/src/test/java/com/vayu/health/data/HealthDataMappingTest.kt`

- [ ] Add `TimeRangeTab` enum (`DAY`, `WEEK`, `MONTH`, `THREE_MONTHS`, `YEAR`) to `HealthDataModels.kt`.
- [ ] Add `HistoricalMetricSeries` data class holding list of `(LocalDate, Double?)` with min, max, avg, and baseline typical range calculations.
- [ ] Implement multi-range historical query methods in `HealthConnectManager.kt` for Resting HR, HRV, Respiratory Rate, SpO2, Skin Temp, and Steps.
- [ ] Update `HealthMetricsRepository.kt` to load historical series for active metric details based on selected timeframe.
- [ ] Write unit tests verifying multi-range aggregation logic, baseline calculations, and null-safe missing day handling.

---

### Task 2: Google Health Resilience 3-Pillar & Sleep Quality Parity

**Files:**
- Modify: `app/src/main/java/com/vayu/health/data/model/HealthDataModels.kt`
- Modify: `app/src/main/java/com/vayu/health/data/HealthConnectManager.kt`
- Modify: `app/src/main/java/com/vayu/health/ui/viewmodel/HealthDashboardViewModel.kt`
- Test: `app/src/test/java/com/vayu/health/data/HealthDataMappingTest.kt`

- [ ] Model `ResiliencePillar` with `title`, `levelLabel` (Optimal, Typical, Low), `insightText`, and `isExpanded`.
- [ ] Model `ResilienceBreakdown` with overall level, subtext, and the 3 pillars (Sleep patterns, Activity balance, Physical calmness) derived from real biometrics.
- [ ] Enhance `SleepMetric` with:
  - `sleepEfficiencyPercentage` (time asleep vs time in bed).
  - `sleepingHeartRateAverage` and `restorationDipBpm` (sleeping HR vs daytime resting HR).
  - `restlessnessMinutes` and `soundSleepMinutes`.
- [ ] Write unit tests verifying Resilience 3-pillar calculation and Sleep efficiency computation.

---

### Task 3: Interactive Multi-Timeframe Scrubbable Chart Component

**Files:**
- Create: `app/src/main/java/com/vayu/health/ui/components/BaroMultiRangeChart.kt`
- Create: `app/src/main/java/com/vayu/health/ui/components/TimeRangeTabRow.kt`
- Modify: `app/src/main/java/com/vayu/health/ui/theme/Color.kt`

- [ ] Create `TimeRangeTabRow` rendering `[ D | W | M | 3M | Y ]` pills with smooth selection indicator.
- [ ] Build `BaroMultiRangeChart` with:
  - Smooth pointer input drag detection for touch scrubbing.
  - Floating glassmorphic tooltip displaying exact date, formatted value, and range status pill.
  - Baseline typical range background ribbon (`Typical Range`).
  - Bar mode for discrete metrics (Steps, Calories) and Bézier curve mode for continuous vitals (HR, HRV).
  - Null-value gap rendering (connecting adjacent valid points or showing empty dashed slot for missing days).

---

### Task 4: Complete Google Health Detail Screens Overhaul

**Files:**
- Modify: `app/src/main/java/com/vayu/health/ui/screens/MetricDetailScreen.kt`
- Modify: `app/src/main/java/com/vayu/health/ui/screens/SleepScreen.kt`
- Modify: `app/src/main/java/com/vayu/health/ui/screens/HeartVitalsScreen.kt`

- [ ] Overhaul `SleepDetailContent`:
  - Score badge, sleep duration (bedtime to wake-up), efficiency pill.
  - "What impacted your score?" cards.
  - 4-lane Hypnogram swimlane timeline with time markers.
  - Sleep Quality breakdown (Time to sound sleep, Sound sleep, Restlessness, Sleeping HR avg).
  - Multi-timeframe tabs (`D`, `W`, `M`, `3M`, `Y`) with scrubbable trend chart.
- [ ] Overhaul `ResilienceDetailContent`:
  - Replicate Google Health Resilience screen (`media_1792431092931_ce1e4e03.png`).
  - Overall status, timeframe tabs, "How we calculate your resilience*" section with 3 expandable accordion cards (Sleep patterns, Activity balance, Physical calmness).
- [ ] Overhaul Vitals detail contents (Resting HR, HRV, Breathing Rate, SpO2, Skin Temp) with multi-range scrubbable chart, stats summary, and source provenance.
- [ ] Overhaul Activity detail contents (Steps, Calories, Zone Minutes) with hourly intraday and weekly/monthly bar views.

---

### Task 5: Dashboard Overview UI/UX Refinement & Polish

**Files:**
- Modify: `app/src/main/java/com/vayu/health/ui/screens/BaroOverviewScreen.kt`
- Modify: `app/src/main/java/com/vayu/health/MainActivity.kt`
- Modify: `app/src/main/java/com/vayu/health/ui/components/BaroFloatingNavBar.kt`

- [ ] Remove non-working "Edit" button next to "HEALTH METRICS".
- [ ] Ensure 5 core vitals cards (Resting HR, HRV, Breathing Rate, SpO2, Skin Temp) match the 2-column layout cleanly with range pills.
- [ ] Seamless bottom navigation visibility: hide bottom bar when in deep metric detail view or ensure it never obscures scrollable content.
- [ ] Apply subtle glow gradients and tactile card clicks across all overview widgets.

---

### Task 6: Verification, Test Suite & Release Build

- [ ] Run `./gradlew testDebugUnitTest` and ensure 100% test pass rate with new tests.
- [ ] Run `./gradlew lintDebug` and ensure zero errors.
- [ ] Build release and debug APKs (`./gradlew assembleDebug`).
- [ ] Verify SHA-256 checksum and deliver detailed completion report.
