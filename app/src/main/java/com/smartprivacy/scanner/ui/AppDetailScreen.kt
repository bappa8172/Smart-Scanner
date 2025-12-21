@file:OptIn(ExperimentalMaterial3Api::class)

package com.smartprivacy.scanner.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartprivacy.scanner.data.AppEntity
import com.smartprivacy.scanner.system.AppStorageStats
import com.smartprivacy.scanner.ui.theme.*

@Composable
fun AppDetailScreen(
    app: AppEntity,
    storageStats: AppStorageStats?,
    vtLoading: Boolean = false,
    onCheckVT: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("App Privacy Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundDark)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // App Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(packageName = app.packageName, size = 64.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(app.appName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(app.packageName, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    
                    val lastUsedText = if (app.lastUsed > 0) {
                        "Last used: ${DateUtils.getRelativeTimeSpanString(app.lastUsed)}"
                    } else {
                        "Usage data unavailable"
                    }
                    Text(lastUsedText, color = PrimaryBlue, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Risk Score Card
            RiskScoreBadge(app.riskScore, app.riskLevel)

            Spacer(modifier = Modifier.height(16.dp))

            // VirusTotal Analysis Card
            VTAnalysisCard(
                app = app,
                isLoading = vtLoading,
                onCheck = onCheckVT
            )

            // Storage Analysis Section
            if (storageStats != null) {
                Spacer(modifier = Modifier.height(24.dp))
                StorageInfoSection(storageStats)
            }

            // Risk Reasons / Explanation
            if (app.riskReasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                RiskDetailSection(app.riskReasons)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            AppActionButtons(
                packageName = app.packageName,
                onOpenSettings = { openAppSettings(context, app.packageName) },
                onDisableBackground = { openBatterySettings(context, app.packageName) },
                onUninstall = { uninstallApp(context, app.packageName) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Permissions List
            Text(
                text = "REQUESTED PERMISSIONS",
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryBlue,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            app.permissions.forEach { permission ->
                PermissionItem(permission)
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun VTAnalysisCard(
    app: AppEntity,
    isLoading: Boolean,
    onCheck: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "MALWARE ANALYSIS (VirusTotal)",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                
                if (app.vtDetectionRatio == null && !isLoading) {
                    TextButton(onClick = onCheck) {
                        Text("CHECK NOW", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryBlue)
            } else if (app.vtDetectionRatio != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val color = if (app.vtMaliciousCount > 0) RiskHigh else RiskLow
                    Icon(
                        imageVector = if (app.vtMaliciousCount > 0) Icons.Default.BugReport else Icons.Default.Shield,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (app.vtMaliciousCount > 0) "Malicious Engines: ${app.vtDetectionRatio}" else "Clean: ${app.vtDetectionRatio}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Text(
                            text = "Analysis based on global security vendors",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                Text(
                    "No global malware data available for this app yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun StorageInfoSection(stats: AppStorageStats) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "STORAGE USAGE",
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryBlue,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            StorageDetailRow("Application", Formatter.formatFileSize(context, stats.appSize))
            StorageDetailRow("Data & Files", Formatter.formatFileSize(context, stats.dataSize))
            StorageDetailRow("Cache Memory", Formatter.formatFileSize(context, stats.cacheSize), isHighlight = true)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.05f))
            
            StorageDetailRow("Total Occupied", Formatter.formatFileSize(context, stats.totalSize), isBold = true)
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "System prevents direct cache clearing. Click 'CLEAR CACHE' to manage it manually in settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun StorageDetailRow(label: String, value: String, isHighlight: Boolean = false, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(
            value, 
            style = MaterialTheme.typography.bodyMedium, 
            fontWeight = if (isBold || isHighlight) FontWeight.Black else FontWeight.Normal,
            color = if (isHighlight) PrimaryBlue else Color.White
        )
    }
}

@Composable
fun RiskDetailSection(reasons: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RiskHigh.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GppMaybe, contentDescription = null, tint = RiskHigh)
                Spacer(modifier = Modifier.width(12.dp))
                Text("THREAT ANALYSIS", style = MaterialTheme.typography.labelSmall, color = RiskHigh, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            reasons.forEach { reason ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(Icons.Default.Circle, contentDescription = null, tint = RiskHigh, modifier = Modifier.size(6.dp).offset(y = 6.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(reason, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
fun AppActionButtons(
    packageName: String,
    onOpenSettings: () -> Unit,
    onDisableBackground: () -> Unit,
    onUninstall: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Icon(Icons.Default.CleaningServices, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("CLEAR CACHE", fontWeight = FontWeight.Black)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onDisableBackground,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("STOP APP", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onUninstall,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RiskHigh)
            ) {
                Text("UNINSTALL", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun openAppSettings(context: Context, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private fun openBatterySettings(context: Context, packageName: String) {
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        openAppSettings(context, packageName)
    }
}

private fun uninstallApp(context: Context, packageName: String) {
    val intent = Intent(Intent.ACTION_DELETE).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
