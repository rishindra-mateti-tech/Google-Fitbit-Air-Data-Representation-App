package com.vayu.health.data

import com.vayu.health.data.model.ProvenanceFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProvenanceFormatterTest {

    @Test
    fun testFormatDisplayName() {
        assertEquals("Fitbit Mobile", ProvenanceFormatter.formatDisplayName("com.fitbit.FitbitMobile"))
        assertEquals("Samsung Health", ProvenanceFormatter.formatDisplayName("com.sec.android.app.shealth"))
        assertEquals("Google Fit", ProvenanceFormatter.formatDisplayName("com.google.android.apps.fitness"))
        assertEquals("All Health Connect Sources", ProvenanceFormatter.formatDisplayName(null))
        assertEquals("All Health Connect Sources", ProvenanceFormatter.formatDisplayName("__ALL_SOURCES__"))
    }

    @Test
    fun testFormatCompact() {
        assertEquals("Fitbit", ProvenanceFormatter.formatCompact("com.fitbit.FitbitMobile"))
        assertEquals("Samsung", ProvenanceFormatter.formatCompact("com.sec.android.app.shealth"))
        assertEquals("Google Fit", ProvenanceFormatter.formatCompact("com.google.android.apps.fitness"))
        assertEquals("Health Connect", ProvenanceFormatter.formatCompact(null))
        assertEquals("Health Connect", ProvenanceFormatter.formatCompact("__ALL_SOURCES__"))
    }

    @Test
    fun testIsInternalSystemPackage() {
        assertTrue(ProvenanceFormatter.isInternalSystemPackage("com.android.healthconnect.phone.j80ae1dda01a734b7b279cce77de41711"))
        assertTrue(ProvenanceFormatter.isInternalSystemPackage("com.android.healthconnect.phone"))
        assertFalse(ProvenanceFormatter.isInternalSystemPackage("com.fitbit.FitbitMobile"))
        assertFalse(ProvenanceFormatter.isInternalSystemPackage("com.sec.android.app.shealth"))
    }

    @Test
    fun testFormatEmptyState() {
        val fitbitEmpty = ProvenanceFormatter.formatEmptyState("com.fitbit.FitbitMobile", "heart-rate samples")
        assertEquals("Fitbit has not written heart-rate samples to Health Connect for this day.", fitbitEmpty)

        val nullEmpty = ProvenanceFormatter.formatEmptyState(null, "sleep sessions")
        assertEquals("Health Connect has not written sleep sessions to Health Connect for this day.", nullEmpty)
    }
}
