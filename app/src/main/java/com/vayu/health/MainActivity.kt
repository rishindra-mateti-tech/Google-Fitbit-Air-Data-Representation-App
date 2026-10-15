package com.vayu.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vayu.health.data.HealthConnectManager
import com.vayu.health.data.HealthMetricsRepository
import com.vayu.health.ui.components.GlassCard
import com.vayu.health.ui.components.GlassTabBar
import com.vayu.health.ui.components.VayuTab
import com.vayu.health.ui.screens.*
import com.vayu.health.ui.theme.*
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel

class MainActivity : ComponentActivity() {

    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var repository: HealthMetricsRepository
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Set<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        healthConnectManager = HealthConnectManager(this)
        repository = HealthMetricsRepository(healthConnectManager)

        // Health Connect Permissions Contract
        val requestPermissionsContract = PermissionController.createRequestPermissionResultContract()
        requestPermissionsLauncher = registerForActivityResult(requestPermissionsContract) { _ ->
            // Permissions result received; reload metrics
        }

        setContent {
            VayuTheme {
                val viewModel: HealthDashboardViewModel = viewModel {
                    HealthDashboardViewModel(repository)
                }
                val uiState by viewModel.uiState.collectAsState()
                var selectedTab by remember { mutableStateOf(VayuTab.OVERVIEW) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SpaceBlack
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Ambient air/cyan background glow
                        Box(
                            modifier = Modifier
                                .size(350.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 100.dp, y = (-100).dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            VayuCyan.copy(alpha = 0.08f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        Column(modifier = Modifier.fillMaxSize()) {
                            // Permission banner if not granted
                            if (!uiState.hasPermissions && !uiState.isLoading && uiState.isHealthConnectAvailable) {
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    backgroundColor = Color(0x301E1B18),
                                    borderBrush = Brush.linearGradient(
                                        listOf(StatusWarning.copy(alpha = 0.6f), GlassBorder)
                                    )
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Security,
                                                contentDescription = "Permissions",
                                                tint = StatusWarning
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Health Connect Access Needed",
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Grant read-only permission so Vayu can display your Fitbit Air sleep, HRV, and vitals.",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                requestPermissionsLauncher.launch(healthConnectManager.permissions)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = VayuCyan,
                                                contentColor = SpaceBlack
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = "Grant Read Permissions",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Active Screen Content
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                when (selectedTab) {
                                    VayuTab.OVERVIEW -> DashboardScreen(
                                        state = uiState,
                                        viewModel = viewModel
                                    )
                                    VayuTab.SLEEP -> SleepScreen(
                                        state = uiState,
                                        viewModel = viewModel
                                    )
                                    VayuTab.VITALS -> HeartVitalsScreen(
                                        state = uiState,
                                        viewModel = viewModel
                                    )
                                    VayuTab.ACTIVITY -> ActivityScreen(
                                        state = uiState,
                                        viewModel = viewModel
                                    )
                                    VayuTab.INSPECTOR -> InspectorScreen(
                                        state = uiState,
                                        viewModel = viewModel
                                    )
                                }
                            }

                            // Floating Glass Bottom Navigation
                            GlassTabBar(
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }
}
