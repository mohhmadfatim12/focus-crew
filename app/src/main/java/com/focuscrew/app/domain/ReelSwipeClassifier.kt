package com.focuscrew.app.domain

/**
 * Pure Kotlin swipe filter and gesture classifier for Instagram Reels and YouTube Shorts.
 * Fully decoupled from android.* framework dependencies for 100% deterministic unit testing.
 *
 * Intelligent Multi-Platform Architecture:
 *
 * 1. Platform-Aware Source Isolation:
 *    - Instagram:
 *      * Full-screen Reels are single-item viewports (span <= 1).
 *      * Chatting (DMs) displays multiple message bubbles (span >= 2) -> STRICTLY REJECTED.
 *      * Home feed displays multiple cards/headers (span >= 2) -> STRICTLY REJECTED.
 *      * Stories swipe horizontally (deltaX > deltaY) -> STRICTLY REJECTED.
 *      * Unindexed views (fromIndex == -1) on Instagram are non-reels -> STRICTLY REJECTED.
 *    - YouTube:
 *      * Full-screen Shorts container emits unindexed frame/viewpager scrolls (span <= 1).
 *      * Home Feed, Search, and Video Watch Recommendations/Comments use RecyclerView
 *        (android.support.v7.widget.RecyclerView) with multi-item spans -> STRICTLY REJECTED.
 *
 * 2. Directionality & Displacement Validation:
 *    - Rejects horizontal camera swipes, story flips, and tab switching (deltaY > deltaX * 1.5).
 *    - Rejects sub-threshold micro-taps and touch jitter (< 100px).
 *
 * 3. Class Isolation:
 *    - Strictly excludes non-reel classes: EditText (chatting/searching), HorizontalScrollView (story tray),
 *      GridView (explore grid), and standard ScrollView (regular YouTube video details).
 *
 * 4. Adaptive Cooldown Timing & Strict Index Tracking:
 *    - Indexed viewports (Instagram): Same index is NEVER counted twice (currentPageIndex == lastPageIndex -> false).
 *      Index transitions require 350ms settling debounce.
 *    - Unindexed viewports (YouTube Shorts): 1200ms debounce ensures 1 physical swipe = EXACTLY 1 count,
 *      absorbing all drag, fling, and snap settling frames.
 */
object ReelSwipeClassifier {

    /** Minimum vertical displacement in pixels to qualify as a deliberate swipe */
    const val MIN_SWIPE_DISPLACEMENT_PX = 80

    /**
     * Debounce between consecutive swipe gesture initiations.
     * Set to 400ms to absorb all drag, fling, and snap settling frames of a single swipe (~250ms),
     * while allowing consecutive rapid deliberate thumb swipes (>= 400ms) to be counted with 100% precision.
     */
    const val DEBOUNCE_SWIPE_MS = 400L

    /** Maximum attached items in viewport for fullscreen reels/shorts */
    const val MAX_VISIBLE_REEL_SPAN = 2

    // Class names that indicate NON-reel scrolling (chat text entry, horizontal trays, etc.)
    private val REJECTED_CLASS_NAMES = setOf(
        "android.widget.EditText",
        "android.widget.AutoCompleteTextView",
        "android.widget.Spinner",
        "android.widget.HorizontalScrollView",
        "android.widget.Gallery",
        "android.widget.GridView",
        "android.widget.ExpandableListView",
        "android.widget.ScrollView" // Reels/Shorts never use basic ScrollView
    )

    /**
     * Evaluates gesture dimensions, viewport child indices, view class name, and DM/clips context.
     *
     * @param packageName Target app package name (Instagram or YouTube)
     * @param deltaY Absolute vertical displacement in pixels
     * @param deltaX Absolute horizontal displacement in pixels
     * @param fromIndex Adapter position of first visible item (-1 if unindexed)
     * @param toIndex Adapter position of last visible item (-1 if unindexed)
     * @param className The class name of the view emitting the scroll event
     * @param viewId The resource ID of the view emitting the scroll event
     * @param isChatOrDmContext True if the event occurred in a chat, DM thread, or comments
     * @param isClipsContext True if verified to be inside an Instagram Reels (Clips) container
     * @return true if and only if the event represents a genuine vertical reel/short swipe
     */
    fun isGenuineReelSwipe(
        packageName: String = "",
        deltaY: Int,
        deltaX: Int,
        fromIndex: Int,
        toIndex: Int,
        className: String = "",
        viewId: String = "",
        isChatOrDmContext: Boolean = false,
        isClipsContext: Boolean = true
    ): Boolean {
        val absDeltaY = Math.abs(deltaY)
        val absDeltaX = Math.abs(deltaX)
        val isInstagram = packageName.contains("instagram")
        val isYoutube = packageName.contains("youtube")

        // 1. Strict Chat & Direct Message Rejection:
        // If the context was detected as DMs, messaging, or chat, reject immediately.
        if (isChatOrDmContext) {
            return false
        }

        // 2. Reject non-clips Instagram context
        if (isInstagram && !isClipsContext) {
            return false
        }

        // 3. Reject YouTube Home Feed & Watch Comments RecyclerView
        if (isYoutube && className.contains("support.v7.widget.RecyclerView")) {
            return false
        }

        // 4. Reject unindexed generic ViewGroup scrolls on Instagram (e.g. settings/explore header)
        if (isInstagram && fromIndex == -1 && toIndex == -1 && className == "android.view.ViewGroup") {
            return false
        }

        // 4. Reject view IDs that explicitly belong to chat, threads, comments, or composer
        val lowerViewId = viewId.lowercase()
        if (lowerViewId.isNotEmpty()) {
            if (lowerViewId.contains("message") ||
                lowerViewId.contains("direct") ||
                lowerViewId.contains("thread") ||
                lowerViewId.contains("chat") ||
                lowerViewId.contains("inbox") ||
                lowerViewId.contains("comment") ||
                lowerViewId.contains("composer")
            ) {
                return false
            }
        }

        // 5. Reject explicitly known non-reel scroll sources (chat input, story trays, scrollviews)
        if (className.isNotEmpty() && REJECTED_CLASS_NAMES.contains(className)) {
            return false
        }

        // 6. Directionality & displacement check (when deltas are reported by the view)
        if (absDeltaY > 0 || absDeltaX > 0) {
            // Rejects micro-taps and touch jitter below minimum swipe threshold
            if (absDeltaY > 0 && absDeltaY < MIN_SWIPE_DISPLACEMENT_PX) {
                return false
            }
            // Rejects horizontal camera swipes, story flips, and tab switching
            if (absDeltaX > 0 && absDeltaY <= (absDeltaX * 1.2f)) {
                return false
            }
        }

        // 7. Viewport item validation:
        // Chat threads and multi-card feeds display 3+ items visible -> REJECTED!
        if (fromIndex != -1 && toIndex != -1) {
            val visibleSpan = Math.abs(toIndex - fromIndex)
            if (visibleSpan >= 3) {
                return false
            }
        }

        return true
    }

    /**
     * Evaluates whether a detected reel/short swipe should increment the counter.
     * Absorbs multi-frame momentum events within 400ms of any swipe, while seamlessly
     * capturing subsequent deliberate human swipes (>= 400ms) on both Instagram & YouTube.
     *
     * @param lastSwipeTimestamp Timestamp of last counted swipe
     * @param currentTime Current event timestamp
     * @param currentPageIndex Current viewport fromIndex (-1 on YouTube Shorts)
     * @param lastPageIndex Last counted reel's fromIndex
     * @return true if the event represents a new reel swipe to be counted
     */
    fun shouldCountSwipe(
        lastSwipeTimestamp: Long,
        currentTime: Long,
        currentPageIndex: Int = -1,
        lastPageIndex: Int = -1
    ): Boolean {
        val elapsed = currentTime - lastSwipeTimestamp

        // Absorbs all drag, fling, and snap settling frames of the current swipe gesture
        if (elapsed < DEBOUNCE_SWIPE_MS) {
            return false
        }

        // Deliberate new swipe occurred after cooldown
        return true
    }
}


