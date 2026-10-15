# Privacy Policy for Vayu ("Inspired by Air")

*Last updated: October 2026*

**Vayu** is an open-source, truthful personal health dashboard for Android designed to visualize authentic wearable data via Android Health Connect without altering, deriving, or inventing metrics.

Your privacy and the security of your health data are our foundational principles. This Privacy Policy details our strict on-device data architecture and zero-collection policy.

---

## 1. Zero Data Collection & Storage Policy

- **No Remote Servers**: Vayu does not operate, communicate with, or connect to any backend server, cloud service, database, or external API.
- **No Internet Permission**: The Vayu application does not declare or request `android.permission.INTERNET` in its manifest. By architectural design, the app cannot transmit data across the internet or local networks.
- **No Analytics or Trackers**: Vayu contains **zero** third-party telemetry, crash-reporting SDKs, advertising libraries, or user tracking frameworks.
- **No User Accounts**: Vayu does not require registration, passwords, phone numbers, email addresses, or cloud account links.
- **In-Memory Only**: Health data accessed via Health Connect is processed strictly in-memory during active app sessions. Health metrics are never stored in unencrypted local databases or persistent files on disk.

---

## 2. Health Connect Data Usage & Permissions

Vayu acts strictly as a read-only pass-through visualizer for health metrics stored in Android Health Connect by your connected apps and hardware (such as Fitbit, Google Fit, Samsung Health, or other wearables).

### Requested Permissions
Vayu requests only explicit read permissions for metrics supported on your device:
- `READ_STEPS` — Daily step totals
- `READ_DISTANCE` — Walking / running distance
- `READ_TOTAL_CALORIES_BURNED` — Daily total energy burned
- `READ_ACTIVE_CALORIES_BURNED` — Active energy expenditure
- `READ_HEART_RATE` — Continuous heart rate series and samples
- `READ_RESTING_HEART_RATE` — Daily resting heart rate records
- `READ_HEART_RATE_VARIABILITY` — Heart Rate Variability (HRV / RMSSD)
- `READ_SLEEP` — Sleep sessions and sleep stage intervals
- `READ_RESPIRATORY_RATE` — Nocturnal/daily breathing rate
- `READ_OXYGEN_SATURATION` — Blood oxygen levels (SpO₂)
- `READ_SKIN_TEMPERATURE` — Skin temperature baseline deviations

### Data Provenance & Attribution
Every metric displayed in Vayu retains its exact Health Connect `DataOrigin` package name. Vayu will never disguise or re-label third-party records under generic hardware model names unless explicitly provided by that data source.

---

## 3. Backup & Data Protection

To safeguard sensitive biometric information against unauthorized extraction:
- `android:allowBackup="false"` is enforced across all builds.
- Custom `data_extraction_rules.xml` and `backup_rules.xml` configurations explicitly prohibit cloud backups, Google Drive backups, and device-to-device transfers of app preferences.
- Local UI state preferences (e.g. customized dashboard tile order) are stored locally in private application sandbox storage and never synced off-device.

---

## 4. User Control & Revoking Permissions

You maintain full control over Health Connect permissions at all times:
1. Open your Android device **Settings**.
2. Navigate to **Security & Privacy** > **Privacy** > **Health Connect** (or open the **Health Connect** app directly).
3. Tap **App permissions** and select **Vayu**.
4. You can selectively enable or disable individual data types or tap **Remove all permissions**.

Upon revocation, Vayu immediately ceases reading that data category and displays "Data unavailable" without storing cached records.

---

## 5. Non-Medical Disclaimer

**Vayu is not a medical device.** All visualizations, graphs, and statistics are for personal informational and educational purposes only. Vayu does not provide medical advice, diagnosis, treatment, or clinical assessment. Always consult a qualified healthcare provider for questions regarding medical conditions or clinical health decisions.

---

## 6. Open Source & Transparency

Vayu is published under the Apache 2.0 open-source license. The complete source code, manifest permissions, and build configuration are publicly available for independent audit:
- GitHub Repository: [https://github.com/rishindra-mateti-tech/Google-Fitbit-Air-Data-Representation-App](https://github.com/rishindra-mateti-tech/Google-Fitbit-Air-Data-Representation-App)

---

## 7. Contact

For inquiries, privacy questions, or security disclosures, please open an issue on the official GitHub repository:
- Issues: [https://github.com/rishindra-mateti-tech/Google-Fitbit-Air-Data-Representation-App/issues](https://github.com/rishindra-mateti-tech/Google-Fitbit-Air-Data-Representation-App/issues)
