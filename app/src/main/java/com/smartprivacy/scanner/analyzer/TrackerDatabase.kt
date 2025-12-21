package com.smartprivacy.scanner.analyzer

object TrackerDatabase {
    // Mapping package names to known trackers (Exodus-style)
    val KNOWN_TRACKERS = mapOf(
        "com.facebook.katana" to listOf("Facebook Analytics", "Facebook Share", "Facebook Login"),
        "com.instagram.android" to listOf("Facebook Analytics", "Google CrashLytics", "Facebook Ads"),
        "com.whatsapp" to listOf("Facebook Shared SDK"),
        "com.zhiliaoapp.musically" to listOf("TikTok Analytics", "Google Firebase", "AppsFlyer", "Pangle"),
        "com.snapchat.android" to listOf("Snapchat Analytics", "Google Firebase", "CoreMetrics"),
        "com.twitter.android" to listOf("Google Firebase", "Twitter Analytics", "MoPub"),
        "com.amazon.mShop.android.shopping" to listOf("Amazon Metrics", "Google Firebase"),
        "com.uber.user" to listOf("Google Firebase", "Mixpanel", "Braintree"),
        "com.spotify.music" to listOf("Google Firebase", "Appboy", "Adjust"),
        "com.truecaller" to listOf("Google Firebase", "Facebook Analytics", "MoPub", "AppsFlyer")
    )

    val TRACKER_DESCRIPTIONS = mapOf(
        "Facebook Analytics" to "Collects data on how you use the app to build an advertising profile.",
        "Google Firebase Analytics" to "Provides app usage statistics to developers and helps target Google ads.",
        "AppsFlyer" to "Used for marketing attribution to see which ads led you to install the app.",
        "TikTok Analytics" to "Monitors behavior patterns for content serving and tracking.",
        "MoPub" to "An advertising platform that serves ads and tracks your interactions.",
        "Adjust" to "Mobile marketing platform used for tracking and attribution.",
        "Mixpanel" to "Tracks user interactions to help developers understand engagement.",
        "Facebook Shared SDK" to "Shared library that identifies your device across the Facebook ecosystem."
    )
    
    fun getTrackersForApp(packageName: String): List<String> = KNOWN_TRACKERS[packageName] ?: emptyList()
    
    fun getDescription(trackerName: String): String = TRACKER_DESCRIPTIONS[trackerName] ?: "Third-party service used for analytics or advertising."
}
