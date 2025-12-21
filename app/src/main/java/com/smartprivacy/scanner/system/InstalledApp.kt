package com.smartprivacy.scanner.system

import android.graphics.drawable.Drawable

data class InstalledApp(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val isSystemApp: Boolean,
    val installSource: String // "Play Store", "Sideloaded", or "System"
)
