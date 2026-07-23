package com.lifesaver.service

import android.view.accessibility.AccessibilityNodeInfo
import com.lifesaver.detection.DetectionConfig

/**
 * Inspects the active window's node tree for the 2x "fast feed" surface — Reels / Shorts (PRD §3.4).
 * Modern Instagram/YouTube render with Litho/Compose and expose almost no classic view IDs, so we
 * scan view ids AND content-descriptions AND text, matching the markers from [DetectionConfig]
 * against all of them. Bounded (node + depth caps) so it stays cheap on debounced events.
 */
object SurfaceDetector {

    private const val MAX_NODES = 800
    private const val MAX_DEPTH = 40
    private const val MAX_SEEN = 80

    data class Result(val isFast: Boolean, val seenTokens: List<String>)

    fun detect(root: AccessibilityNodeInfo?, target: DetectionConfig.Target): Result {
        if (root == null) return Result(false, emptyList())
        val markers = target.fastSurfaceViewIdMarkers
        val seen = LinkedHashSet<String>()
        var isFast = false
        var visited = 0

        val stack = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        stack.addLast(root to 0)
        while (stack.isNotEmpty() && visited < MAX_NODES) {
            val (node, depth) = stack.removeLast()
            visited++

            // Only match/collect VISIBLE nodes. Instagram preloads the adjacent Reels tab off-screen
            // while you're on the feed/stories, so its "Create a reel" etc. is in the tree but not
            // visible — counting it would wrongly block the feed. isVisibleToUser filters that out.
            if (node.isVisibleToUser) {
                considerToken(seen, markers, "id:", node.viewIdResourceName?.substringAfterLast('/'))?.let { if (it) isFast = true }
                considerToken(seen, markers, "cd:", node.contentDescription?.toString())?.let { if (it) isFast = true }
                considerToken(seen, markers, "tx:", node.text?.toString())?.let { if (it) isFast = true }
            }

            if (depth < MAX_DEPTH) {
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    stack.addLast(child to depth + 1)
                }
            }
        }
        return Result(isFast, seen.toList())
    }

    /** Records a token (capped) and returns true if it matches a fast marker. */
    private fun considerToken(
        seen: LinkedHashSet<String>,
        markers: List<String>,
        prefix: String,
        raw: String?,
    ): Boolean? {
        val v = raw?.trim()?.replace('\n', ' ')?.take(48) ?: return null
        if (v.isEmpty()) return null
        if (seen.size < MAX_SEEN) seen.add("$prefix$v")
        return markers.any { v.contains(it, ignoreCase = true) }
    }
}
