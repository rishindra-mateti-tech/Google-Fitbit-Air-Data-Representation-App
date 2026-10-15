package com.vayu.health.data.model

/**
 * Utility functions for formatting Health Connect DataOrigin package names
 * into clean, user-friendly display labels and preventing broken text wrapping.
 */
object ProvenanceFormatter {

    /**
     * Checks if a package name belongs to an internal Health Connect system provider
     * (e.g. system hash providers or internal sync managers).
     */
    fun isInternalSystemPackage(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower.startsWith("com.android.healthconnect") ||
                lower.contains("j80ae1dda") ||
                lower.startsWith("com.google.android.apps.healthdata")
    }

    /**
     * Returns a human-friendly display name for a Health Connect source package.
     * Example: "com.fitbit.FitbitMobile" -> "Fitbit Mobile"
     */
    fun formatDisplayName(packageName: String?): String {
        if (packageName == null || packageName == "__ALL_SOURCES__") {
            return "All Health Connect Sources"
        }
        val lower = packageName.lowercase()
        return when {
            lower.contains("fitbit") -> "Fitbit Mobile"
            lower.contains("samsung") || lower.contains("shealth") -> "Samsung Health"
            lower.contains("google.android.apps.fitness") || lower.contains("fitness") -> "Google Fit"
            isInternalSystemPackage(packageName) -> "Health Connect System Provider"
            lower.contains("healthconnect") -> "Health Connect"
            else -> {
                val parts = packageName.split(".")
                if (parts.size >= 2) {
                    parts.takeLast(2).joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                } else {
                    packageName
                }
            }
        }
    }

    /**
     * Returns a compact, single-word friendly label for narrow cards, pills, and badges.
     * Guaranteed to be short and never wrap character-by-character.
     * Example: "com.fitbit.FitbitMobile" -> "Fitbit"
     */
    fun formatCompact(packageName: String?): String {
        if (packageName == null || packageName == "__ALL_SOURCES__") {
            return "Health Connect"
        }
        val lower = packageName.lowercase()
        return when {
            lower.contains("fitbit") -> "Fitbit"
            lower.contains("samsung") || lower.contains("shealth") -> "Samsung"
            lower.contains("fitness") -> "Google Fit"
            isInternalSystemPackage(packageName) -> "System"
            lower.contains("healthconnect") -> "Health Connect"
            else -> {
                val parts = packageName.split(".")
                if (parts.isNotEmpty()) parts.last().replaceFirstChar { it.uppercase() } else packageName
            }
        }
    }

    /**
     * Generates a clear, source-aware empty-state description.
     */
    fun formatEmptyState(selectedSource: String?, metricName: String): String {
        val sourceLabel = if (selectedSource != null && selectedSource.contains("fitbit", ignoreCase = true)) {
            "Fitbit"
        } else if (selectedSource != null) {
            formatDisplayName(selectedSource)
        } else {
            "Health Connect"
        }
        return "$sourceLabel has not written $metricName to Health Connect for this day."
    }
}
