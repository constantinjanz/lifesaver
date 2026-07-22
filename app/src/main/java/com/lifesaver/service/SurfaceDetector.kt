package com.lifesaver.service

import android.view.accessibility.AccessibilityNodeInfo
import com.lifesaver.detection.DetectionConfig

/**
 * Inspects the active window's node tree for the 2x "fast feed" surface — Reels / Shorts (PRD §3.4).
 * Markers come entirely from [DetectionConfig]. Traversal is bounded (node + depth caps) so it stays
 * cheap enough to run on debounced content-changed events. If confidence is unclear it reports NOT
 * fast, so we never over-punish a false positive (§3.4).
 */
object SurfaceDetector {

    private const val MAX_NODES = 500
    private const val MAX_DEPTH = 40
    private const val MAX_SEEN_IDS = 60

    data class Result(val isFast: Boolean, val seenViewIds: List<String>)

    fun detect(root: AccessibilityNodeInfo?, target: DetectionConfig.Target): Result {
        if (root == null) return Result(false, emptyList())
        val markers = target.fastSurfaceViewIdMarkers
        val seen = LinkedHashSet<String>()
        var isFast = false
        var visited = 0

        // Iterative DFS with an explicit stack of (node, depth). We must recycle nodes we obtain.
        val stack = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        stack.addLast(root to 0)
        while (stack.isNotEmpty() && visited < MAX_NODES) {
            val (node, depth) = stack.removeLast()
            visited++
            val id = node.viewIdResourceName
            if (id != null) {
                val shortId = id.substringAfterLast('/')
                if (seen.size < MAX_SEEN_IDS) seen.add(shortId)
                if (!isFast && markers.any { shortId.contains(it, ignoreCase = true) }) {
                    isFast = true
                }
            }
            if (depth < MAX_DEPTH) {
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    stack.addLast(child to depth + 1)
                }
            }
            // Do not recycle `root` (owned by caller); recycle traversed children.
            if (node !== root) {
                @Suppress("DEPRECATION")
                node.recycle()
            }
        }
        return Result(isFast, seen.toList())
    }
}
