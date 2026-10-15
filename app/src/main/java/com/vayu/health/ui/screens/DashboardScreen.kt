package com.vayu.health.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.R
import com.vayu.health.data.model.HealthDashboardState
import com.vayu.health.data.model.ProvenanceFormatter
import com.vayu.health.ui.components.*
import com.vayu.health.ui.theme.*
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel
import java.time.format.DateTimeFormatter

enum class DashboardCardId {
    SLEEP_SESSION,
    DAILY_ACTIVITY,
    HEART_RATE_CURVE,
    RESTING_HR_AND_HRV,
    SLEEP_HISTORY_7_DAYS,
    RESPIRATORY_AND_TEMP
}

@Composable
fun DashboardScreen(
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    onOpenTab: (VayuTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var isEditMode by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }

    val isFirstRun by viewModel.isFirstRun.collectAsState()
    LaunchedEffect(isFirstRun) {
        if (isFirstRun) {
            showSourceDialog = true
        }
    }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("vayu_layout_prefs", Context.MODE_PRIVATE) }

    val defaultOrder = remember {
        listOf(
            DashboardCardId.SLEEP_SESSION,
            DashboardCardId.DAILY_ACTIVITY,
            DashboardCardId.HEART_RATE_CURVE,
            DashboardCardId.RESTING_HR_AND_HRV,
            DashboardCardId.SLEEP_HISTORY_7_DAYS,
            DashboardCardId.RESPIRATORY_AND_TEMP
        )
    }

    var cardOrder by remember {
        val saved = prefs.getString("card_order", null)
        val initialOrder = if (saved != null) {
            val list = saved.split(",").mapNotNull { name ->
                try {
                    DashboardCardId.valueOf(name)
                } catch (e: Exception) {
                    null
                }
            }
            if (list.size == defaultOrder.size && list.containsAll(defaultOrder)) list else defaultOrder
        } else {
            defaultOrder
        }
        mutableStateOf(initialOrder)
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }

    if (showSourceDialog) {
        SourceSelectionDialog(
            state = state,
            currentSelected = state.selectedSource,
            onSelectSource = { newSource ->
                viewModel.setSourceFilter(newSource)
                showSourceDialog = false
            },
            onDismiss = {
                viewModel.dismissFirstRun()
                showSourceDialog = false
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_vayu_logo),
                    contentDescription = "Vayu Logo",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Today",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Source Selector Pill
                val sourceLabel = ProvenanceFormatter.formatCompact(state.selectedSource)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x1AFFFFFF))
                        .border(0.5.dp, if (state.selectedSource != null) VayuCyan.copy(alpha = 0.5f) else EnergyAmber.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { showSourceDialog = true }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (state.selectedSource != null) VayuCyan else EnergyAmber)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = sourceLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Source Settings",
                            tint = TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { showPrivacyDialog = true },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Privacy Policy & Transparency Notice",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.loadMetrics(forceRefresh = true) },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Health Connect data",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Button(
                    onClick = { isEditMode = !isEditMode },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEditMode) VayuCyan else Color(0x1AFFFFFF),
                        contentColor = if (isEditMode) SpaceBlack else TextPrimary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isEditMode) "Save Card Order" else "Edit Card Layout",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEditMode) "Done" else "Edit",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Edit Mode Information Banner
        AnimatedVisibility(visible = isEditMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x2000F2FE))
                .border(1.dp, VayuCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Use arrows to reorder cards • Layout persists automatically",
                    fontSize = 12.sp,
                    color = VayuCyan,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Fitbit Sync Needed Card (Fitbit selected but no records found)
        if (state.isFitbitSyncNeeded) {
            Spacer(modifier = Modifier.height(6.dp))
            FitbitSyncNeededCard(
                state = state,
                onRefresh = { viewModel.loadMetrics(forceRefresh = true) },
                onOpenSourceSettings = { showSourceDialog = true }
            )
        }

        // All-source mirror mode: provenance remains visible on every reading.
        if (state.selectedSource == null) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x20FF9100))
                    .border(0.5.dp, EnergyAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Health Connect mirror: showing exact records from your connected sources. Each card identifies its original source.",
                    fontSize = 11.sp,
                    color = TextPrimary
                )
            }
        }

        // Error banners if any
        if (state.errorMessages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.errorMessages.forEach { err ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x20FF3366))
                            .border(0.5.dp, HeartCrimson.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(text = err, fontSize = 11.sp, color = TextPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Cards list
        cardOrder.forEachIndexed { index, cardId ->
            val cardModifier = if (isEditMode) {
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.5.dp, VayuCyan.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            }

            Box(modifier = cardModifier) {
                when (cardId) {
                    DashboardCardId.SLEEP_SESSION -> {
                        SleepSessionCard(state = state, viewModel = viewModel) { onOpenTab(VayuTab.SLEEP) }
                    }
                    DashboardCardId.DAILY_ACTIVITY -> {
                        DailyActivityCard(state = state) { onOpenTab(VayuTab.ACTIVITY) }
                    }
                    DashboardCardId.HEART_RATE_CURVE -> {
                        HeartRateCurveCard(state = state, viewModel = viewModel) { onOpenTab(VayuTab.VITALS) }
                    }
                    DashboardCardId.RESTING_HR_AND_HRV -> {
                        RestingHrAndHrvCard(state = state) { onOpenTab(VayuTab.VITALS) }
                    }
                    DashboardCardId.SLEEP_HISTORY_7_DAYS -> {
                        SleepHistory7DaysCard(state = state) { onOpenTab(VayuTab.SLEEP) }
                    }
                    DashboardCardId.RESPIRATORY_AND_TEMP -> {
                        RespiratoryAndTempCard(state = state) { onOpenTab(VayuTab.VITALS) }
                    }
                }

                // Edit Mode Reorder Arrows
                if (isEditMode) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (index > 0) {
                            IconButton(
                                onClick = {
                                    val newOrder = cardOrder.toMutableList()
                                    val item = newOrder.removeAt(index)
                                    newOrder.add(index - 1, item)
                                    cardOrder = newOrder
                                    prefs.edit().putString("card_order", newOrder.joinToString(",") { it.name }).apply()
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x4000F2FE))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Move Up",
                                    tint = SpaceBlack,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        if (index < cardOrder.size - 1) {
                            IconButton(
                                onClick = {
                                    val newOrder = cardOrder.toMutableList()
                                    val item = newOrder.removeAt(index)
                                    newOrder.add(index + 1, item)
                                    cardOrder = newOrder
                                    prefs.edit().putString("card_order", newOrder.joinToString(",") { it.name }).apply()
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x4000F2FE))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Move Down",
                                    tint = SpaceBlack,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// SETUP CARD: Fitbit Sync Needed
@Composable
fun FitbitSyncNeededCard(
    state: HealthDashboardState,
    onRefresh: () -> Unit,
    onOpenSourceSettings: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        backgroundColor = Color(0x351F1A12),
        borderBrush = Brush.linearGradient(
            listOf(EnergyAmber.copy(alpha = 0.8f), GlassBorder)
        )
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(EnergyAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Fitbit Sync Needed",
                        tint = EnergyAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Fitbit Health Connect Sync Needed",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Fitbit-only filter active (${state.selectedSource})",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "No records from Fitbit were found in Health Connect. Vayu strictly displays only original Fitbit records and never uses data from other apps as a substitute.\n\nTo view your wearable data:\n1. Open the Fitbit app on your Galaxy S25 Ultra.\n2. Confirm Health Connect sync is turned ON in Fitbit settings.\n3. Allow Fitbit a moment to sync your steps, sleep, and heart rate.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VayuCyan,
                        contentColor = SpaceBlack
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Text("Check for Fitbit Data", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenSourceSettings,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary
                    ),
                    border = BorderStroke(1.dp, GlassBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Source Settings", fontSize = 12.sp)
                }
            }
        }
    }
}

// SOURCE SELECTION DIALOG
@Composable
fun SourceSelectionDialog(
    state: HealthDashboardState,
    currentSelected: String?,
    onSelectSource: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPackage by remember { mutableStateOf(currentSelected) }
    var showAdvancedSystemSources by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = VayuCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Health Connect Data Source",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Vayu displays only authentic records from Google Health Connect. Select which DataOrigin source to visualize:",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "FITBIT WEARABLE (RECOMMENDED)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = VayuCyan,
                    letterSpacing = 0.5.sp
                )

                val fitbitPackages = state.detectedSources.filter { it.contains("fitbit", ignoreCase = true) }.toSet()
                val packagesToShow = if (fitbitPackages.isNotEmpty()) fitbitPackages else setOf("com.fitbit.FitbitMobile")

                packagesToShow.forEach { pkg ->
                    val isSelected = selectedPackage == pkg
                    val isDetected = state.detectedSources.contains(pkg)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0x2000F2FE) else Color(0x10FFFFFF))
                            .border(1.dp, if (isSelected) VayuCyan else Color(0x18FFFFFF), RoundedCornerShape(12.dp))
                            .clickable { selectedPackage = pkg }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedPackage = pkg },
                            colors = RadioButtonDefaults.colors(selectedColor = VayuCyan, unselectedColor = TextMuted)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = ProvenanceFormatter.formatDisplayName(pkg),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isDetected) VayuCyan.copy(alpha = 0.2f) else EnergyAmber.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isDetected) "Detected" else "Default",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDetected) VayuCyan else EnergyAmber
                                    )
                                }
                            }
                            Text(
                                text = pkg,
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                // Regular detected non-Fitbit apps (excluding system hashes)
                val regularOtherSources = state.detectedSources.filter {
                    !it.contains("fitbit", ignoreCase = true) && !ProvenanceFormatter.isInternalSystemPackage(it)
                }
                if (regularOtherSources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "OTHER DETECTED APPS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )

                    regularOtherSources.forEach { pkg ->
                        val isSelected = selectedPackage == pkg
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0x2000F2FE) else Color(0x0CFFFFFF))
                                .border(1.dp, if (isSelected) VayuCyan else Color(0x10FFFFFF), RoundedCornerShape(12.dp))
                            .clickable { selectedPackage = pkg }
                            .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedPackage = pkg },
                                colors = RadioButtonDefaults.colors(selectedColor = VayuCyan, unselectedColor = TextMuted)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ProvenanceFormatter.formatDisplayName(pkg),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = pkg,
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                // Internal system packages hidden behind an expandable debug toggle
                val systemInternalSources = state.detectedSources.filter {
                    ProvenanceFormatter.isInternalSystemPackage(it)
                }
                if (systemInternalSources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showAdvancedSystemSources = !showAdvancedSystemSources }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showAdvancedSystemSources) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showAdvancedSystemSources) "Hide System Providers" else "Advanced: Show System Providers (${systemInternalSources.size})",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (showAdvancedSystemSources) {
                        systemInternalSources.forEach { pkg ->
                            val isSelected = selectedPackage == pkg
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0x2000F2FE) else Color(0x08FFFFFF))
                                    .border(1.dp, if (isSelected) VayuCyan else Color(0x0EFFFFFF), RoundedCornerShape(10.dp))
                                    .clickable { selectedPackage = pkg }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedPackage = pkg },
                                    colors = RadioButtonDefaults.colors(selectedColor = VayuCyan, unselectedColor = TextMuted)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Internal Provider",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = pkg,
                                        fontSize = 9.sp,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "MIXED-SOURCE MODE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EnergyAmber,
                    letterSpacing = 0.5.sp
                )

                val isAllSelected = selectedPackage == null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isAllSelected) Color(0x20FF9100) else Color(0x0CFFFFFF))
                        .border(1.dp, if (isAllSelected) EnergyAmber else Color(0x10FFFFFF), RoundedCornerShape(12.dp))
                        .clickable { selectedPackage = null }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isAllSelected,
                        onClick = { selectedPackage = null },
                        colors = RadioButtonDefaults.colors(selectedColor = EnergyAmber, unselectedColor = TextMuted)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "All Health Connect Sources", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = "Aggregates records across all installed health apps. Not restricted to Fitbit.",
                            fontSize = 10.sp,
                            color = EnergyAmber,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelectSource(selectedPackage) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = VayuCyan,
                    contentColor = SpaceBlack
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Confirm Selection",
                    color = SpaceBlack,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary, fontSize = 12.sp)
            }
        },
        containerColor = Color(0xFF141822),
        shape = RoundedCornerShape(20.dp)
    )
}

// IN-APP PRIVACY POLICY DIALOG
@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VayuCyan,
                    contentColor = SpaceBlack
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Understood", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = VayuCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Privacy & Data Policy",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "• Pure Read-Only Visualizer: Vayu connects directly to Google Health Connect to display authentic records written by your wearable. It never writes or alters data.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Text(
                    text = "• Zero Cloud / No Servers: Vayu does not request Internet permission (android.permission.INTERNET). Zero data leaves your device. No analytics, tracking SDKs, or cloud synchronization.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Text(
                    text = "• On-Device Processing: Health data is processed purely in volatile memory during the active session. Backup and device transfer of health data are explicitly disabled.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Text(
                    text = "• Permission Revocation: You can view or revoke Health Connect access anytime in Android Settings → Security & Privacy → Health Connect → App permissions.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Text(
                    text = "• Non-Medical Disclaimer: Vayu is for personal informational purposes only. It is not a medical device and provides no medical diagnosis, interpretation, or advice.",
                    fontSize = 13.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        containerColor = Color(0xFF141822),
        shape = RoundedCornerShape(20.dp)
    )
}

// CARD 1: Authentic Sleep Session & Stages
@Composable
fun SleepSessionCard(
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    onClick: () -> Unit
) {
    val sleep = state.sleep

    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        backgroundColor = Color(0xFF0E1015),
        borderBrush = SolidColor(Color(0x18FFFFFF))
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SLEEP SESSION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x14FFFFFF))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = ProvenanceFormatter.formatCompact(sleep?.sourcePackage ?: state.selectedSource),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VayuCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (sleep != null) {
                val actualMins = sleep.actualSleepDurationMinutes

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Actual Sleep Hero Box
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x18818CF8))
                            .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "ACTUAL SLEEP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA5B4FC),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = actualMins?.let { "${it / 60}h ${it % 60}m" } ?: "Data unavailable",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (actualMins != null) TextPrimary else TextUnavailable
                        )
                        Text(
                            text = if (actualMins != null) "Fitbit recorded duration" else "No stage records",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }

                    // Session Elapsed & Timing Box
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "SESSION ELAPSED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${sleep.sessionElapsedMinutes / 60}h ${sleep.sessionElapsedMinutes % 60}m",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${viewModel.formatTime(sleep.startTime)} – ${viewModel.formatTime(sleep.endTime)}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = ProvenanceFormatter.formatDisplayName(sleep.sourcePackage),
                            fontSize = 10.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (sleep.stages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SleepStageBar(sleep = sleep)
                }
            } else {
                Text(
                    text = "Data unavailable",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextUnavailable
                )
                Text(
                    text = ProvenanceFormatter.formatEmptyState(state.selectedSource, "sleep sessions"),
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// CARD 2: Daily Activity (Spacious 2x2 Metric Grid with Aggregates)
@Composable
fun DailyActivityCard(state: HealthDashboardState, onClick: () -> Unit) {
    val activity = state.activity
    val isFiltered = state.selectedSource != null

    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        backgroundColor = Color(0xFF0E1015),
        borderBrush = SolidColor(Color(0x18FFFFFF))
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVITY & ENERGY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x14FFFFFF))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = ProvenanceFormatter.formatCompact(state.selectedSource ?: activity?.sources?.firstOrNull()),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VayuCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (activity != null) {
                val steps = activity.steps
                val totalCal = activity.totalCaloriesKcal
                val activeCal = activity.activeCaloriesKcal
                val distanceMeters = activity.distanceMeters

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: Steps & Total Energy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Steps Tile
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x0CFFFFFF))
                                .border(0.5.dp, Color(0x18FFFFFF), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "STEPS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = VayuCyan,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = steps?.let { "%,d".format(it) } ?: "Data unavailable",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (steps != null) TextPrimary else TextUnavailable,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (steps != null) "Fitbit count" else "No steps today",
                                fontSize = 10.sp,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Total Energy Tile
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x0CFFFFFF))
                                .border(0.5.dp, Color(0x18FFFFFF), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "TOTAL ENERGY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EnergyAmber,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = totalCal?.let { "%,.0f kcal".format(it) } ?: "Data unavailable",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (totalCal != null) TextPrimary else TextUnavailable,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (totalCal != null) "Health Connect aggregate" else "No burn record",
                                fontSize = 10.sp,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Row 2: Active Energy & Distance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Active Energy Tile
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x0CFFFFFF))
                                .border(0.5.dp, Color(0x18FFFFFF), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "ACTIVE ENERGY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeCal?.let { "%,.0f kcal".format(it) } ?: "Data unavailable",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (activeCal != null) TextPrimary else TextUnavailable,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (activeCal != null) "Fitbit active burn" else "No active calories",
                                fontSize = 10.sp,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Distance Tile
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x0CFFFFFF))
                                .border(0.5.dp, Color(0x18FFFFFF), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "DISTANCE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = distanceMeters?.let { "%.2f km".format(it / 1000.0) } ?: "Data unavailable",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (distanceMeters != null) TextPrimary else TextUnavailable,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (distanceMeters != null) "Fitbit logged" else "No distance record",
                                fontSize = 10.sp,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (!isFiltered && activity.sources.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Contributing sources: ${activity.sources.joinToString(", ") { ProvenanceFormatter.formatDisplayName(it) }}",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            } else {
                Text(
                    text = "Data unavailable",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextUnavailable
                )
                Text(
                    text = ProvenanceFormatter.formatEmptyState(state.selectedSource, "activity records"),
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// CARD 3: Authentic Heart Rate Wave Curve
@Composable
fun HeartRateCurveCard(
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    onClick: () -> Unit
) {
    val hr = state.heartRate

    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        backgroundColor = Color(0xFF0E1015),
        borderBrush = SolidColor(Color(0x18FFFFFF))
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HEART RATE SAMPLES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x14FFFFFF))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    val hrSource = if ((hr?.sources?.size ?: 0) > 1) "Mixed" else (hr?.sourcePackage ?: state.selectedSource)
                    Text(
                        text = ProvenanceFormatter.formatCompact(hrSource),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VayuCyan
                    )
                }
            }

            if (hr?.latestBpm != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = "${hr.latestBpm}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HeartCrimson)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("bpm", fontSize = 11.sp, color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (hr != null && hr.samples.isNotEmpty()) {
                val wavePoints = hr.samples.map { sample ->
                    WavePoint(
                        xLabel = viewModel.formatTime(sample.time),
                        value = sample.beatsPerMinute.toFloat()
                    )
                }

                SmoothWaveChart(
                    points = wavePoints,
                    lineColor = HeartCrimson,
                    chartHeight = 85.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, Color(0x10FFFFFF), RoundedCornerShape(10.dp))
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MIN", fontSize = 10.sp, color = TextMuted)
                        Text(hr.minBpm?.let { "$it bpm" } ?: "--", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("AVERAGE", fontSize = 10.sp, color = TextMuted)
                        Text(hr.averageBpm?.let { "$it bpm" } ?: "--", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PEAK", fontSize = 10.sp, color = TextMuted)
                        Text(hr.maxBpm?.let { "$it bpm" } ?: "--", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HeartCrimson)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Calculated from actual timestamped Fitbit pulse samples • Updates when Fitbit syncs",
                    fontSize = 9.sp,
                    color = TextMuted
                )
            } else {
                Text(
                    text = "Data unavailable",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextUnavailable
                )
                Text(
                    text = ProvenanceFormatter.formatEmptyState(state.selectedSource, "heart-rate samples"),
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// CARD 4: Resting Heart Rate & HRV Dual Card
@Composable
fun RestingHrAndHrvCard(state: HealthDashboardState, onClick: () -> Unit) {
    val rhr = state.restingHeartRate
    val hrv = state.hrv

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Resting HR
        GlassCard(
            modifier = Modifier.weight(1f).clickable(onClick = onClick),
            backgroundColor = Color(0xFF0E1015),
            borderBrush = SolidColor(Color(0x18FFFFFF))
        ) {
            Column {
                Text("RESTING HEART RATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(6.dp))
                if (rhr != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${rhr.beatsPerMinute}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = HeartCrimson)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("bpm", fontSize = 11.sp, color = TextMuted)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ProvenanceFormatter.formatCompact(rhr.sourcePackage),
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text("Data unavailable", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextUnavailable)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ProvenanceFormatter.formatEmptyState(state.selectedSource, "RHR"),
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // HRV
        GlassCard(
            modifier = Modifier.weight(1f).clickable(onClick = onClick),
            backgroundColor = Color(0xFF0E1015),
            borderBrush = SolidColor(Color(0x18FFFFFF))
        ) {
            Column {
                Text("HRV (RMSSD)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(6.dp))
                if (hrv != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("%.1f".format(hrv.rmssdMillis), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = HrvEmerald)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("ms", fontSize = 11.sp, color = TextMuted)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ProvenanceFormatter.formatCompact(hrv.sourcePackage),
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text("Data unavailable", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextUnavailable)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ProvenanceFormatter.formatEmptyState(state.selectedSource, "HRV"),
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// CARD 5: 7-Day Sleep History
@Composable
fun SleepHistory7DaysCard(state: HealthDashboardState, onClick: () -> Unit) {
    val history = state.dailySleepHistory

    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        backgroundColor = Color(0xFF0E1015),
        borderBrush = SolidColor(Color(0x18FFFFFF))
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "7-DAY SLEEP HISTORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x14FFFFFF))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = ProvenanceFormatter.formatCompact(state.selectedSource),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VayuCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (history.isNotEmpty()) {
                val dayFormatter = DateTimeFormatter.ofPattern("EEE")
                val dateFormatter = DateTimeFormatter.ofPattern("M/d")

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    history.forEach { summary ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x08FFFFFF))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${summary.date.format(dayFormatter)} ${summary.date.format(dateFormatter)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                val sessionCountLabel = if (summary.sessionCount > 1) {
                                    "${summary.sessionCount} sessions • ${ProvenanceFormatter.formatCompact(summary.sourcePackage)}"
                                } else {
                                    ProvenanceFormatter.formatCompact(summary.sourcePackage)
                                }
                                Text(
                                    text = sessionCountLabel,
                                    fontSize = 9.sp,
                                    color = TextMuted
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                val actual = summary.actualSleepMinutes
                                if (actual != null) {
                                    Text(
                                        text = "${actual / 60}h ${actual % 60}m",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VayuCyan
                                    )
                                    Text(
                                        text = "Elapsed: ${summary.sessionElapsedMinutes / 60}h ${summary.sessionElapsedMinutes % 60}m",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                } else {
                                    Text(
                                        text = "Data unavailable",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextUnavailable
                                    )
                                    Text(
                                        text = "Elapsed: ${summary.sessionElapsedMinutes / 60}h ${summary.sessionElapsedMinutes % 60}m",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Data unavailable",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextUnavailable
                )
                Text(
                    text = ProvenanceFormatter.formatEmptyState(state.selectedSource, "7-day sleep records"),
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// CARD 6: Blood Oxygen & Skin Temperature
@Composable
fun RespiratoryAndTempCard(state: HealthDashboardState, onClick: () -> Unit) {
    val resp = state.respiratory
    val spo2 = resp?.spo2Percentage
    val skinTemp = resp?.skinTemperature

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // SpO2
        GlassCard(
            modifier = Modifier.weight(1f).clickable(onClick = onClick),
            backgroundColor = Color(0xFF0E1015),
            borderBrush = SolidColor(Color(0x18FFFFFF))
        ) {
            Column {
                Text("BLOOD OXYGEN (SpO₂)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(6.dp))
                if (spo2 != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("%.1f".format(spo2), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = VayuCyan)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("%", fontSize = 11.sp, color = TextMuted)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ProvenanceFormatter.formatCompact(resp.spo2SourcePackage ?: state.selectedSource),
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text("Data unavailable", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextUnavailable)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ProvenanceFormatter.formatEmptyState(state.selectedSource, "SpO₂"),
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Skin Temp
        GlassCard(
            modifier = Modifier.weight(1f).clickable(onClick = onClick),
            backgroundColor = Color(0xFF0E1015),
            borderBrush = SolidColor(Color(0x18FFFFFF))
        ) {
            Column {
                Text("SKIN TEMP DEVIATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(6.dp))
                if (skinTemp != null && skinTemp.averageDeltaCelsius != null) {
                    val delta = skinTemp.averageDeltaCelsius
                    val sign = if (delta >= 0) "+" else ""
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$sign%.2f".format(delta), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (delta >= 0) EnergyAmber else VayuCyan)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("°C", fontSize = 11.sp, color = TextMuted)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ProvenanceFormatter.formatCompact(skinTemp.sourcePackage),
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text("Data unavailable", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextUnavailable)
                    Spacer(modifier = Modifier.height(2.dp))
                    val caption = if (!state.isSkinTemperatureSupported) "Sensor unsupported" else ProvenanceFormatter.formatEmptyState(state.selectedSource, "skin temp")
                    Text(
                        text = caption,
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
