package com.vayu.health

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vayu.health.data.HealthConnectManager
import com.vayu.health.data.HealthMetricsRepository
import com.vayu.health.data.model.HealthConnectStatus
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

    private val viewModel: HealthDashboardViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HealthDashboardViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        healthConnectManager = HealthConnectManager(this)
        repository = HealthMetricsRepository(healthConnectManager)

        // Health Connect Permissions Contract
        val requestPermissionsContract = PermissionController.createRequestPermissionResultContract()
        requestPermissionsLauncher = registerForActivityResult(requestPermissionsContract) { _ ->
            // Immediately reload metrics after user grants/denies permissions
            viewModel.loadMetrics()
        }

        setContent {
            VayuTheme {
                val uiState by viewModel.uiState.collectAsState()
                var selectedTab by remember { mutableStateOf(VayuTab.OVERVIEW) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SpaceBlack
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Ambient radial air glow
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
                            // Update Required Banner
                            if (uiState.sdkStatus == HealthConnectStatus.UPDATE_REQUIRED) {
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    backgroundColor = Color(0x303E2723),
                                    borderBrush = Brush.linearGradient(
                                        listOf(StatusWarning, GlassBorder)
                                    )
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.SystemUpdate,
                                                contentDescription = "Update Health Connect",
                                                tint = StatusWarning
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Health Connect Update Required",
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Please update Health Connect from the Google Play Store to access health data.",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))
                                                    startActivity(intent)
                                                } catch (e: Exception) {
                                                    try {
                                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"))
                                                        startActivity(webIntent)
                                                    } catch (_: Exception) {
                                                        android.widget.Toast.makeText(this@MainActivity, "Unable to launch Play Store", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = VayuCyan,
                                                contentColor = SpaceBlack
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Update from Play Store", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            // Permission banner if NO permissions granted
                            if (uiState.sdkStatus == HealthConnectStatus.AVAILABLE &&
                                uiState.grantedPermissions.isEmpty() &&
                                !uiState.isLoading
                            ) {
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                                text = "Health Connect Permissions Needed",
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Grant read permissions so Vayu can display your authentic wearable records.",
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
        viewModel.loadMetrics()
    }
}
