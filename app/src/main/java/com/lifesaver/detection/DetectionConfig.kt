package com.lifesaver.detection

/**
 * THE single source of truth for all app/surface detection markers (PRD §3.4).
 *
 * Instagram and YouTube change their UI regularly. When detection breaks, this file
 * — and only this file — should need editing. The AccessibilityService and
 * SurfaceDetector read everything from here; no marker strings live anywhere else.
 *
 * Browser markers (§9.3) are included now so v1.1 slots in without touching the service.
 * Detection is best-effort: when confidence is unclear, callers default to 1x (§3.4).
 */
object DetectionConfig {

    /** A target app Lifesaver intercepts and budgets. */
    data class Target(
        val appId: String,
        val label: String,
        /** Substrings that, when found in a view id, mark the 2x "fast feed" surface. */
        val fastSurfaceViewIdMarkers: List<String>,
        /** Human name of the 2x surface, for logging/debug. */
        val fastSurfaceName: String,
    )

    const val INSTAGRAM = "com.instagram.android"
    const val YOUTUBE = "com.google.android.youtube"

    val targets: List<Target> = listOf(
        Target(
            appId = INSTAGRAM,
            label = "Instagram",
            // Modern IG has no view IDs (Litho) — match content-descriptions unique to the immersive
            // Reels VIEWER (the Reels tab), NOT things a reel embedded in the home feed also shows.
            // "create a reel" is the Reels-tab top-bar button and is absent from feed reel cards.
            // Deliberately NOT "reshare number"/"reels": those also appear on feed-embedded reels /
            // the bottom-nav tab and would wrongly block the normal feed.
            fastSurfaceViewIdMarkers = listOf("clips_", "reels_", "create a reel"),
            fastSurfaceName = "Reels",
        ),
        Target(
            appId = YOUTUBE,
            label = "YouTube",
            // reel_recycler / reel_player markers, plus Shorts-player content-descriptions.
            fastSurfaceViewIdMarkers = listOf(
                "reel_", "shorts_", "shorts player", "short remix", "remix this short",
            ),
            fastSurfaceName = "Shorts",
        ),
    )

    val targetPackages: Set<String> = targets.map { it.appId }.toSet()

    /** 1-tap intentions shown at the pause (intention-gap). Last option is always the honest one. */
    fun intentionsFor(appId: String?): List<String> = when (appId) {
        INSTAGRAM -> listOf("Messages", "Someone's profile", "Search", "Just browsing")
        YOUTUBE -> listOf("A specific video", "Search", "Just browsing")
        else -> listOf("Something specific", "Just browsing")
    }

    fun targetFor(appId: String?): Target? = targets.firstOrNull { it.appId == appId }

    fun isTarget(appId: String?): Boolean = appId != null && appId in targetPackages

    // --- Browser blocking (§9.3) — wired for v1.1; unused by v1 enforcement. ---

    data class Browser(
        val appId: String,
        val label: String,
        /** View ids of the URL/address bar to read the current domain from. */
        val urlBarViewIds: List<String>,
    )

    val browsers: List<Browser> = listOf(
        Browser(
            appId = "com.android.chrome",
            label = "Chrome",
            urlBarViewIds = listOf("com.android.chrome:id/url_bar"),
        ),
        Browser(
            appId = "com.sec.android.app.sbrowser",
            label = "Samsung Internet",
            urlBarViewIds = listOf("com.sec.android.app.sbrowser:id/location_bar_edit_text"),
        ),
    )

    /** Domain -> which target budget it counts against, plus whether it is a 2x path. */
    data class DomainRule(
        val domainMarker: String,
        val countsAsAppId: String,
        val isFastSurface: Boolean,
    )

    val domainRules: List<DomainRule> = listOf(
        DomainRule("instagram.com/reels", INSTAGRAM, isFastSurface = true),
        DomainRule("instagram.com", INSTAGRAM, isFastSurface = false),
        DomainRule("youtube.com/shorts", YOUTUBE, isFastSurface = true),
        DomainRule("m.youtube.com/shorts", YOUTUBE, isFastSurface = true),
        DomainRule("youtube.com", YOUTUBE, isFastSurface = false),
    )

    /** Longest-marker-first so "youtube.com/shorts" wins over "youtube.com". */
    fun domainRuleFor(url: String?): DomainRule? {
        if (url.isNullOrBlank()) return null
        val u = url.lowercase()
        return domainRules
            .sortedByDescending { it.domainMarker.length }
            .firstOrNull { u.contains(it.domainMarker) }
    }
}
