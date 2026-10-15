# Vayu — Inspired by Air
### Google Fitbit Air Data Representation App for Android

A private, high-performance personal health dashboard for Android (optimized for Samsung Galaxy S25 Ultra). Vayu connects directly to **Google Health Connect** to present your authentic Fitbit Air wearable measurements in an ultra-clean, Bevel- and WHOOP-inspired dark liquid-glass interface.

---

## 🌬️ Core Philosophy: No Invented Metrics

Vayu is built on a single, non-negotiable principle: **Zero Synthetic Scores**.

Unlike conventional health apps that calculate proprietary "Recovery %", "Readiness Scores", or "Strain", Vayu operates purely as an **authentic visualizer**:
* **Pass-Through Architecture**: Vayu queries the original raw measurements that Google and Fitbit save into your device's local Health Connect store.
* **No Fabricated Data**: If a metric is not recorded or reported by your wearable, Vayu shows **"Data unavailable"** rather than estimating or hallucinating a number.
* **100% Local & Private**: No cloud database, no backend server, no subscriptions, and no external data transmission. All computation happens on your phone.

---

## 💎 Design System: Liquid Glass & OLED Dark Mode

* **Palette**: Deep space obsidian (`#06080D`), frosted glass surfaces, and translucent card layers.
* **Vitals & Neon Accents**:
  * 🩵 **Vayu Air Cyan** (`#00F2FE` / `#4FACFE`) — Signature theme & steps
  * 💜 **Deep Sleep Indigo & Lavender** (`#3D5AFE` / `#7C4DFF`) — Sleep stages (Deep, REM, Light, Awake)
  * ❤️ **Cardiovascular Crimson** (`#FF3366`) — Resting Heart Rate & pulse
  * 💚 **Recovery Emerald** (`#00E676`) — RMSSD Heart Rate Variability (HRV)
  * 🧡 **Metabolic Amber** (`#FF9100`) — Active & Total Caloric burn
  * 🌊 **Respiratory Teal** (`#00E5FF`) — Oxygen Saturation (SpO₂) & Respiration
* **Glassmorphism**: Multi-layered frosted glass cards with specular highlight rims and ambient radiant glows.

---

## 📊 Monitored Health Metrics

| Category | Measurements Shown | Pass-Through Source |
| :--- | :--- | :--- |
| **Sleep** | Duration, Bedtime, Wake Time, Stages (Deep, REM, Light, Awake) | `SleepSessionRecord` |
| **Heart** | Resting Heart Rate, RMSSD HRV, Live BPM range | `RestingHeartRateRecord`, `HeartRateVariabilityRmssdRecord`, `HeartRateRecord` |
| **Activity** | Daily Steps, Active Calories, Total Energy, Distance | `StepsRecord`, `ActiveCaloriesBurnedRecord`, `DistanceRecord` |
| **Respiratory** | Respiratory Rate, Blood Oxygen Saturation (SpO₂), Skin Temp | `RespiratoryRateRecord`, `OxygenSaturationRecord`, `SkinTemperatureRecord` |
| **Data Inspector** | Raw timestamps, package origin (`com.fitbit.FitbitMobile`), Record IDs | Full Health Connect Database Audit |

---

## 📱 Sideloading onto Samsung Galaxy S25 Ultra

1. Download the generated `app-release.apk` (or `app-debug.apk`) directly to your phone.
2. Open **My Files** (or Downloads) on your Galaxy S25 Ultra and tap the APK.
3. If prompted, toggle **Allow from this source**.
4. Tap **Install**.
5. Launch **Vayu**, tap **Grant Read Permissions**, and toggle the switch for Google Health Connect.

---

## 🛠️ Tech Stack & Engineering

* **Language**: Kotlin 1.9.23
* **UI**: Jetpack Compose with Material 3 & Glassmorphic Custom Canvas
* **Health API**: Android Health Connect Client SDK (`androidx.health.connect:connect-client:1.1.0-alpha11`)
* **State Management**: Kotlin Coroutines & StateFlow (Clean MVVM)
* **Target OS**: Android 14 / 15+ (API 34)
* **Backend**: None (Serverless, local on-device)

---

## 📜 License

Private Personal Project — Built with Google Antigravity.
