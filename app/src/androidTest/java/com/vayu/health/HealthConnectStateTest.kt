package com.vayu.health

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vayu.health.data.HealthConnectManager
import com.vayu.health.data.model.HealthConnectStatus
import com.vayu.health.data.model.HealthDashboardState
import com.vayu.health.ui.theme.VayuTheme
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for Health Connect integration states:
 * 1. HealthConnectManager initialization and SDK status query on Android environment.
 * 2. Truthful UI rendering for unavailable/permission-denied states (displays error banners and "Data unavailable").
 * 3. Absolute absence of invented metrics ("Readiness", "Strain", "Sleep Performance").
 */
@RunWith(AndroidJUnit4::class)
class HealthConnectStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testHealthConnectManager_sdkStatusResolution() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val manager = HealthConnectManager(context)
        val status = manager.getSdkStatus()
        assertNotNull(status)
        assertTrue(
            status in setOf(
                HealthConnectStatus.AVAILABLE,
                HealthConnectStatus.UPDATE_REQUIRED,
                HealthConnectStatus.UNAVAILABLE
            )
        )
    }

    @Test
    fun testDashboardUi_displaysDataUnavailableWhenMetricsNull() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val viewModel = com.vayu.health.ui.viewmodel.HealthDashboardViewModel(
            com.vayu.health.data.HealthMetricsRepository(HealthConnectManager(context))
        )
        val stateWithErrors = HealthDashboardState(
            isLoading = false,
            sdkStatus = HealthConnectStatus.AVAILABLE,
            grantedPermissions = emptySet(),
            sleep = null,
            dailySleepHistory = emptyList(),
            restingHeartRate = null,
            hrv = null,
            heartRate = null,
            activity = null,
            respiratory = null,
            errorMessages = listOf("Health Connect read permissions not yet granted.")
        )

        composeTestRule.setContent {
            VayuTheme {
                com.vayu.health.ui.screens.DashboardScreen(
                    state = stateWithErrors,
                    viewModel = viewModel
                )
            }
        }

        // Verify permission error banner is visible
        composeTestRule.onNodeWithText("Health Connect read permissions not yet granted.").assertIsDisplayed()

        // Verify "Data unavailable" text appears across cards where records are null
        composeTestRule.onAllNodesWithText("Data unavailable").assertCountEquals(3) // Sleep, Heart Rate, Activity

        // ABSOLUTE NON-NEGOTIABLE CHECK: Ensure NO invented/derived metric labels exist
        composeTestRule.onNodeWithText("Readiness").assertDoesNotExist()
        composeTestRule.onNodeWithText("Recovery (WHOOP)").assertDoesNotExist()
        composeTestRule.onNodeWithText("Strain (WHOOP)").assertDoesNotExist()
        composeTestRule.onNodeWithText("Sleep Performance").assertDoesNotExist()
        composeTestRule.onNodeWithText("Sleep Consistency").assertDoesNotExist()
    }

    @Test
    fun testDashboardUi_displaysUpdateRequiredBanner() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val viewModel = com.vayu.health.ui.viewmodel.HealthDashboardViewModel(
            com.vayu.health.data.HealthMetricsRepository(HealthConnectManager(context))
        )
        val updateRequiredState = HealthDashboardState(
            isLoading = false,
            sdkStatus = HealthConnectStatus.UPDATE_REQUIRED,
            grantedPermissions = emptySet(),
            errorMessages = listOf("Health Connect needs to be updated from the Google Play Store.")
        )

        composeTestRule.setContent {
            VayuTheme {
                com.vayu.health.ui.screens.DashboardScreen(
                    state = updateRequiredState,
                    viewModel = viewModel
                )
            }
        }

        // Verify update required message is shown
        composeTestRule.onNodeWithText("Health Connect needs to be updated from the Google Play Store.").assertIsDisplayed()
    }
}
