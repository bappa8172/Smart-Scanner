@file:OptIn(ExperimentalMaterial3Api::class)

package com.smartprivacy.scanner.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.Formatter
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.smartprivacy.scanner.analyzer.SafetyStatus
import com.smartprivacy.scanner.data.AppEntity
import com.smartprivacy.scanner.system.AppStorageStats
import com.smartprivacy.scanner.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    state: ScannerUiState,
    onStartScan: () -> Unit,
    onCleanCache: () -> Unit,
    onNavigateToAppList: () -> Unit,
    onNavigateToRiskyApps: () -> Unit,
    onDismissPermissionDialog: () -> Unit = {},
    onRequestPermission: () -> Unit = {},
    onOpenSystemCleaner: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        val statusColor = when {
            state.safetyResult.highRiskCount > 0 -> RiskHigh
            state.safetyResult.mediumRiskCount > 0 -> RiskMedium
            else -> SafetySafe
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .blur(120.dp)
                .background(Brush.verticalGradient(listOf(statusColor.copy(alpha = 0.15f), Color.Transparent)))
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))
            Text(text = "SMART PRIVACY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, letterSpacing = 3.sp, color = PrimaryBlue)
            Spacer(modifier = Modifier.height(40.dp))
            
            ModernSafetyDashboard(
                highRiskCount = state.safetyResult.highRiskCount,
                onClick = { if (state.safetyResult.totalThreats > 0) onNavigateToRiskyApps() }
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // DEFINITIVELY PASSING state.cacheSize
            BoostCard(
                cacheSizeBytes = state.cacheSize,
                isCleaning = state.isCleaning,
                onClean = onCleanCache,
                onDeepClean = onOpenSystemCleaner
            )

            Spacer(modifier = Modifier.height(16.dp))
            DashboardSummary(state.safetyResult.summary)
            Spacer(modifier = Modifier.weight(1f))
            ModernScanButton(onStartScan)
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onNavigateToAppList) {
                Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Manage ${state.apps.size} Apps", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = PrimaryBlue)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        if (state.isCleaning) { CleaningOverlay(state.cleaningProgress) }
        if (state.showStoragePermissionDialog) { 
            StoragePermissionDialog(
                permissionType = state.permissionTypeNeeded,
                onDismiss = onDismissPermissionDialog, 
                onConfirm = onRequestPermission
            ) 
        }
    }
}
// Keep the rest of the file the same...
// ... (All other Composables remain unchanged)
@Composable
fun AppListScreen(
    systemApps: List<AppEntity>,
    installedApps: List<AppEntity>,
    storageStats: Map<String, AppStorageStats>,
    showRiskyOnly: Boolean,
    onAppClick: (String) -> Unit,
    onBack: () -> Unit,
    initialTab: Int = 0
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val context = LocalContext.current
    
    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Text(
                            if (showRiskyOnly) "RISKY APPS DETECTED" else "SECURITY REPORT", 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Black
                        ) 
                    },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark, titleContentColor = Color.White, navigationIconContentColor = Color.White)
                )
                if (!showRiskyOnly) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = BackgroundDark,
                        contentColor = PrimaryBlue,
                        divider = {},
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = PrimaryBlue)
                            }
                        }
                    ) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                            Text("INSTALLED (${installedApps.size})", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                        }
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                            Text("SYSTEM (${systemApps.size})", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        val fullList = if (selectedTab == 0) installedApps else systemApps
        val currentList = if (showRiskyOnly) {
            fullList.filter { it.riskScore >= 40 }
        } else {
            fullList
        }

        if (currentList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No risks detected in this category.", color = TextSecondary)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(currentList) { app ->
                    val stats = storageStats[app.packageName]
                    AppItem(
                        app = app,
                        cacheSize = stats?.cacheSize?.let { Formatter.formatFileSize(context, it) },
                        onAppClick = onAppClick
                    )
                }
            }
        }
    }
}

@Composable
fun AppItem(app: AppEntity, cacheSize: String?, onAppClick: (String) -> Unit) {
    val riskColor = when (app.riskLevel) { "High Risk" -> RiskHigh; "Moderate Concern" -> RiskMedium; else -> RiskLow }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { onAppClick(app.packageName) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.75f)),
        border = if (app.riskLevel == "High Risk") BorderStroke(1.dp, RiskHigh.copy(alpha = 0.35f)) else null
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(packageName = app.packageName, size = 48.dp)
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = app.appName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                if (cacheSize != null && cacheSize != "0 B" && cacheSize != "0.00 B") {
                    Text(text = "Cache: $cacheSize", style = MaterialTheme.typography.labelSmall, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                } else {
                    Text(text = app.packageName, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = app.riskLevel.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = riskColor)
                Text(text = "${app.riskScore}% RISK", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

@Composable
fun ModernSafetyDashboard(highRiskCount: Int, onClick: () -> Unit) {
    val statusColor = if (highRiskCount > 0) RiskHigh else SafetySafe
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.03f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "pulseScale")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.graphicsLayer(scaleX = pulseScale, scaleY = pulseScale).clickable(onClick = onClick)) {
        Canvas(modifier = Modifier.size(280.dp)) {
            drawCircle(color = statusColor.copy(alpha = 0.12f), style = Stroke(2.dp.toPx()))
            drawCircle(color = statusColor.copy(alpha = 0.06f), radius = size.minDimension / 2.2f, style = Stroke(1.dp.toPx()))
        }
        Box(modifier = Modifier.size(210.dp).shadow(40.dp, CircleShape, ambientColor = statusColor, spotColor = statusColor).clip(CircleShape).background(Color(0xFF0A0A0A)).border(2.dp, statusColor.copy(alpha = 0.8f), CircleShape), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(color = statusColor, shape = RoundedCornerShape(6.dp), modifier = Modifier.size(28.dp).offset(y = (-14).dp)) {
                    Icon(imageVector = if (highRiskCount == 0) Icons.Default.Shield else Icons.Default.Close, contentDescription = null, tint = Color.Black, modifier = Modifier.padding(5.dp))
                }
                Text(text = if (highRiskCount == 0) "SAFE" else "$highRiskCount", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black, color = if (highRiskCount == 0) SafetySafe else Color.White, fontSize = if (highRiskCount == 0) 56.sp else 80.sp, lineHeight = 80.sp)
                Text(text = if (highRiskCount == 0) "PROTECTED" else "HIGH RISK", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = statusColor, letterSpacing = 3.sp)
            }
        }
    }
}

@Composable
fun BoostCard(cacheSizeBytes: Long, isCleaning: Boolean, onClean: () -> Unit, onDeepClean: () -> Unit) {
    val context = LocalContext.current
    val formattedSize = Formatter.formatFileSize(context, cacheSizeBytes)
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.7f)), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Device Optimization", style = MaterialTheme.typography.labelSmall, color = PrimaryBlue, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(text = if (cacheSizeBytes <= 1024 * 1024) "System Optimized" else "$formattedSize Junk Found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.White)
                }
                Button(onClick = onClean, enabled = !isCleaning, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151515)), border = BorderStroke(1.5.dp, PrimaryBlue.copy(alpha = 0.4f)), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("BOOST", color = PrimaryBlue, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
            if (cacheSizeBytes > 50 * 1024 * 1024) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth().clickable { onDeepClean() }.background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = RiskMedium, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Android prevents auto-cleaning system cache. Click here to clean GBs manually in Settings.", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun DashboardSummary(summary: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.45f))) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(40.dp), color = PrimaryBlue.copy(alpha = 0.12f), shape = CircleShape) { Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.padding(10.dp)) }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 20.sp)
        }
    }
}

@Composable
fun ModernScanButton(onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(68.dp).shadow(25.dp, RoundedCornerShape(22.dp), spotColor = PrimaryBlue), shape = RoundedCornerShape(22.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue), contentPadding = PaddingValues(0.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text("SCAN NOW", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
        }
    }
}

@Composable
fun StoragePermissionDialog(permissionType: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val title = if (permissionType == "USAGE") "Usage Access Required" else "Storage Access Required"
    val message = if (permissionType == "USAGE") "To detect app cache and junk, you must enable 'Usage Access' for Smart Privacy in System Settings." else "To reach hidden system junk and GBs of deep app cache, Smart Privacy needs 'All Files Access'."
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title, fontWeight = FontWeight.Black) }, text = { Text(message) }, confirmButton = { Button(onClick = onConfirm) { Text("OPEN SETTINGS") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }, containerColor = CardBackground, titleContentColor = Color.White, textContentColor = TextSecondary)
}

@Composable
fun CleaningOverlay(progress: Float) {
    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark.copy(alpha = 0.96f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(40.dp)) {
            val infiniteTransition = rememberInfiniteTransition(label = "rocket")
            val translationY by infiniteTransition.animateFloat(initialValue = 0f, targetValue = -40f, animationSpec = infiniteRepeatable(animation = tween(450, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "rocketMove")
            Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(110.dp).graphicsLayer(translationY = translationY), tint = PrimaryBlue)
            Spacer(modifier = Modifier.height(36.dp))
            Text("OPTIMIZING PERFORMANCE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color.White)
            Spacer(modifier = Modifier.height(20.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape), color = PrimaryBlue, trackColor = Color.White.copy(alpha = 0.1f), strokeCap = StrokeCap.Round)
            Spacer(modifier = Modifier.height(14.dp))
            Text(text = "${(progress * 100).toInt()}% Scrubbing Junk...", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
fun ScanProgressScreen(state: ScannerUiState) {
    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark), contentAlignment = Alignment.Center) {
        val infiniteTransition = rememberInfiniteTransition(label = "scanning")
        val rotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing)), label = "radarRotation")
        Canvas(modifier = Modifier.size(320.dp)) {
            val center = center
            val radius = size.minDimension / 2
            val strokeWidth = 1.dp.toPx()
            drawCircle(color = PrimaryBlue.copy(alpha = 0.12f), radius = radius, style = Stroke(strokeWidth))
            drawCircle(color = PrimaryBlue.copy(alpha = 0.12f), radius = radius * 0.66f, style = Stroke(strokeWidth))
            drawCircle(color = PrimaryBlue.copy(alpha = 0.12f), radius = radius * 0.33f, style = Stroke(strokeWidth))
            drawArc(brush = Brush.sweepGradient(0f to Color.Transparent, 0.5f to PrimaryBlue.copy(alpha = 0.35f), 1f to Color.Transparent, center = center), startAngle = rotation, sweepAngle = 90f, useCenter = true)
        }
        Column(modifier = Modifier.fillMaxSize().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
            Text(text = "SEARCHING FOR THREATS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(modifier = Modifier.height(20.dp))
            LinearProgressIndicator(progress = { state.scanProgress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = PrimaryBlue, trackColor = Color.White.copy(alpha = 0.1f), strokeCap = StrokeCap.Round)
            Spacer(modifier = Modifier.height(14.dp))
            Text(text = state.currentScanningApp, style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center, maxLines = 1)
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun AppIcon(packageName: String, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(packageName) { withContext(Dispatchers.IO) { try { bitmap = context.packageManager.getApplicationIcon(packageName).toBitmap().asImageBitmap() } catch (e: Exception) { } } }
    if (bitmap != null) { Image(bitmap = bitmap!!, contentDescription = null, modifier = modifier.size(size).clip(RoundedCornerShape(10.dp))) }
    else { Box(modifier = modifier.size(size).background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Android, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(size * 0.6f)) } }
}

@Composable
fun RiskScoreBadge(score: Int, level: String) {
    val color = when (level) { "High Risk" -> RiskHigh; "Moderate Concern" -> RiskMedium; else -> RiskLow }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.06f)), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.2.dp, color.copy(alpha = 0.25f))) {
        Row(modifier = Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("PRIVACY THREAT LEVEL", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                Text("$score%", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color.White)
            }
            Box(modifier = Modifier.background(color, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 8.dp)) { Text(level.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color.White) }
        }
    }
}

@Composable
fun PermissionItem(permission: String) {
    val shortName = permission.split(".").last()
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(14.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(shortName, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.95f))
    }
}
