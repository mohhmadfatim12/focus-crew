package com.focuscrew.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReelSwipeClassifierTest {

    // =========================================================================
    // isGenuineReelSwipe() tests — Reel vs Non-Reel Isolation
    // =========================================================================

    @Test
    fun testTouchesAndMicroTapsAreRejected() {
        // Micro-jitter tap (5px)
        assertFalse(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 5, deltaX = 2, fromIndex = 0, toIndex = 0
        ))

        // Minor touch wiggle (20px)
        assertFalse(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 20, deltaX = 1, fromIndex = 0, toIndex = 0
        ))

        // Below 80px threshold (75px)
        assertFalse(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 75, deltaX = 5, fromIndex = 1, toIndex = 1
        ))
    }

    @Test
    fun testHorizontalAndDiagonalSwipesAreRejected() {
        // Horizontal camera swipe or story flip
        assertFalse(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 50, deltaX = 300, fromIndex = 0, toIndex = 0
        ))

        // Diagonal swipe (not strictly vertical: 60 <= 50 * 1.3 = 65)
        assertFalse(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 60, deltaX = 50, fromIndex = 0, toIndex = 0
        ))
    }

    @Test
    fun testGenericScrollViewRejected() {
        // Normal YouTube video page using ScrollView -> REJECTED!
        assertFalse(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.google.android.youtube",
            deltaY = 300, deltaX = 10, fromIndex = -1, toIndex = -1,
            className = "android.widget.ScrollView"
        ))
    }

    @Test
    fun testYouTubeFeedAndCommentsRecyclerViewRejected() {
        // Normal YouTube Home Feed or Video Watch Comments using android.support.v7.widget.RecyclerView -> REJECTED!
        assertFalse(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.google.android.youtube",
            deltaY = 250, deltaX = 10, fromIndex = -1, toIndex = -1,
            className = "android.support.v7.widget.RecyclerView"
        ))

        assertFalse(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.google.android.youtube",
            deltaY = 300, deltaX = 5, fromIndex = 2, toIndex = 6,
            className = "android.support.v7.widget.RecyclerView"
        ))
    }

    @Test
    fun testInstagramChatAndMultiItemFeedRejected() {
        // Instagram DMs / Chat: multiple message bubbles visible (indices 10 to 15, span = 5)
        assertFalse(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 150, deltaX = 5, fromIndex = 10, toIndex = 15,
            className = "androidx.recyclerview.widget.RecyclerView"
        ))

        // Instagram Chat with multiple messages visible (span = 3 > MAX_VISIBLE_REEL_SPAN)
        assertFalse(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 150, deltaX = 5, fromIndex = 1, toIndex = 4,
            className = "androidx.recyclerview.widget.RecyclerView"
        ))

        // Instagram Home Feed: 5 posts visible in list
        assertFalse(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 400, deltaX = 20, fromIndex = 0, toIndex = 5
        ))

        // Instagram unindexed scroll event (not Reels)
        assertFalse(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 250, deltaX = 10, fromIndex = -1, toIndex = -1,
            className = "android.view.ViewGroup"
        ))
    }

    @Test
    fun testNonReelClassNamesAreRejected() {
        // EditText (typing a DM or search)
        assertFalse(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 200, deltaX = 10, fromIndex = 0, toIndex = 0,
            className = "android.widget.EditText"
        ))

        // Horizontal story tray
        assertFalse(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 100, deltaX = 10, fromIndex = 0, toIndex = 0,
            className = "android.widget.HorizontalScrollView"
        ))

        // Explore grid
        assertFalse(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 300, deltaX = 5, fromIndex = 0, toIndex = 0,
            className = "android.widget.GridView"
        ))
    }

    @Test
    fun testInstagramReelsAccepted() {
        // Fullscreen Reel: single item visible (span = 0)
        assertTrue(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 150, deltaX = 10, fromIndex = 4, toIndex = 4
        ))

        // Reel snap transition: 2 items partially visible during transition (span = 1)
        assertTrue(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 300, deltaX = 15, fromIndex = 2, toIndex = 3
        ))

        // Reverse reel swipe (scrolling up to previous reel)
        assertTrue(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 200, deltaX = 10, fromIndex = 5, toIndex = 4
        ))

        // Instagram custom view where deltas are 0 but indices are valid
        assertTrue(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 0, deltaX = 0, fromIndex = 2, toIndex = 3,
            className = "androidx.viewpager.widget.ViewPager"
        ))
    }

    @Test
    fun testYouTubeShortsUnindexedAccepted() {
        // YouTube Shorts: unindexed viewports (fromIndex == -1, toIndex == -1) with standard container classes
        assertTrue(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.google.android.youtube",
            deltaY = 180, deltaX = 10, fromIndex = -1, toIndex = -1,
            className = "android.view.ViewGroup"
        ))

        assertTrue(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.google.android.youtube",
            deltaY = 250, deltaX = 15, fromIndex = -1, toIndex = -1,
            className = "android.widget.FrameLayout"
        ))

        assertTrue(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.google.android.youtube",
            deltaY = 250, deltaX = 15, fromIndex = -1, toIndex = -1,
            className = "androidx.viewpager.widget.ViewPager"
        ))
    }

    // =========================================================================
    // shouldCountSwipe() tests — 400ms Swipe Debounce & Rapid Consecutive Swipes
    // =========================================================================

    @Test
    fun testMomentumSettlingFramesRejected() {
        val t0 = 10000L

        // Fling drag and settling frames within 400ms are strictly absorbed
        assertFalse(ReelSwipeClassifier.shouldCountSwipe(t0, t0 + 100L))
        assertFalse(ReelSwipeClassifier.shouldCountSwipe(t0, t0 + 250L))
        assertFalse(ReelSwipeClassifier.shouldCountSwipe(t0, t0 + 350L))
        assertFalse(ReelSwipeClassifier.shouldCountSwipe(t0, t0 + 399L))
    }

    @Test
    fun testRapidConsecutiveDeliberateSwipesAccepted() {
        val t0 = 10000L

        // Exactly 400ms elapsed -> deliberate consecutive swipe ACCEPTED!
        assertTrue(ReelSwipeClassifier.shouldCountSwipe(t0, t0 + 400L))

        // 500ms elapsed (rapid double-flick) -> deliberate swipe ACCEPTED!
        assertTrue(ReelSwipeClassifier.shouldCountSwipe(t0, t0 + 500L))

        // 700ms elapsed -> deliberate swipe ACCEPTED!
        assertTrue(ReelSwipeClassifier.shouldCountSwipe(t0, t0 + 700L))
    }

    // =========================================================================
    // Instagram DMs / Chat Messaging vs Reels Isolation
    // =========================================================================

    @Test
    fun testInstagramDmAndChatMessagesAreStrictlyRejected() {
        // 1. Explicit chat/DM context flag
        assertFalse(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 150, deltaX = 10, fromIndex = 2, toIndex = 3,
            isChatOrDmContext = true
        ))

        // 2. Chat message list viewId
        assertFalse(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 150, deltaX = 10, fromIndex = 1, toIndex = 2,
            viewId = "com.instagram.android:id/message_list"
        ))

        // 3. Direct thread composer viewId
        assertFalse(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 150, deltaX = 10, fromIndex = 0, toIndex = 1,
            viewId = "com.instagram.android:id/row_thread_composer_edittext"
        ))

        // 4. Instagram comments thread viewId
        assertFalse(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 200, deltaX = 10, fromIndex = 3, toIndex = 4,
            viewId = "com.instagram.android:id/layout_comment_thread_edittext"
        ))

        // 5. Instagram event outside verified clips container (e.g. Home feed or DM inbox)
        assertFalse(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 200, deltaX = 10, fromIndex = 0, toIndex = 1,
            isClipsContext = false
        ))
    }

    @Test
    fun testInstagramClipsViewerAccepted() {
        // Genuine Reels ViewPager2 container
        assertTrue(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 250, deltaX = 15, fromIndex = 0, toIndex = 1,
            viewId = "com.instagram.android:id/clips_viewer_view_pager",
            isClipsContext = true
        ))

        // Genuine Reels Video Container
        assertTrue(ReelSwipeClassifier.isGenuineReelSwipe(
            packageName = "com.instagram.android",
            deltaY = 300, deltaX = 20, fromIndex = 1, toIndex = 2,
            viewId = "com.instagram.android:id/clips_video_container",
            isClipsContext = true
        ))
    }
}


