package com.recentr.core.model

/**
 * Apps known by the demo catalogue (mirrors the Figma "App Icon" component). [packageNames] maps
 * the device's packages to them (screen time shows these apps with their Figma icon).
 */
enum class AppId(val displayName: String, val packageNames: Set<String>) {
    Instagram("Instagram", setOf("com.instagram.android", "com.instagram.lite")),
    X("X", setOf("com.twitter.android")),
    TikTok("TikTok", setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.zhiliaoapp.musically.go")),
    Spotify("Spotify", setOf("com.spotify.music", "com.spotify.lite")),
    Roblox("Roblox", setOf("com.roblox.client")),
    Gmail("Gmail", setOf("com.google.android.gm", "com.google.android.gm.lite")),
    Facebook("Facebook", setOf("com.facebook.katana", "com.facebook.lite")),
    YouTube("YouTube", setOf("com.google.android.youtube")),
    ;

    companion object {
        /** The catalogue app published under [packageName], if any. */
        fun fromPackageName(packageName: String): AppId? = entries.firstOrNull { packageName in it.packageNames }
    }
}
