package com.focuscrew.app.domain

import com.focuscrew.app.service.DnsPacketParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SafeSearchEnforcementTest — Verifies that SafeSearch cannot be disabled
 * and that explicit media (images, videos, thumbnails, tamper URLs) are strictly blocked
 * while safe educational searches remain fully functional.
 */
class SafeSearchEnforcementTest {

    // ─────────────────────────────────────────────────────────────
    // 1. ADULT IMAGE URLS & QUERIES BLOCKED
    // ─────────────────────────────────────────────────────────────
    @Test
    fun `test adult image searches and URLs are strictly blocked`() {
        val adultImageQueries = listOf(
            "porn images",
            "porn pics",
            "porn photo",
            "nude images",
            "nude pics",
            "naked photos",
            "naked pics",
            "xxx pics",
            "hentai images",
            "boobs pics",
            "pussy pics",
            "sexy pics hd",
            "hot pics nude",
            "https://image.pornhub.com/media/photo.jpg",
            "https://thumbs.xvideos-cdn.com/galleries/pic1.jpg"
        )

        for (query in adultImageQueries) {
            val result = FuzzyAdultContentDetector.detect(query)
            assertNotNull("Adult image query/URL '$query' must be strictly blocked", result)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 2. ADULT VIDEO URLS & QUERIES BLOCKED
    // ─────────────────────────────────────────────────────────────
    @Test
    fun `test adult video searches and URLs are strictly blocked`() {
        val adultVideoQueries = listOf(
            "sex video",
            "sex videos",
            "porn video",
            "hardcore sex video",
            "nude video hd",
            "naked girls video",
            "desi sex video",
            "pakistani sex video",
            "xxx video streaming",
            "https://www.pornhub.com/view_video.php?viewkey=123",
            "https://www.xvideos.com/video12345/watch",
            "https://redtube.com/999888"
        )

        for (query in adultVideoQueries) {
            val result = FuzzyAdultContentDetector.detect(query)
            assertNotNull("Adult video query/URL '$query' must be strictly blocked", result)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. ADULT THUMBNAILS & PREVIEWS BLOCKED
    // ─────────────────────────────────────────────────────────────
    @Test
    fun `test adult thumbnails and preview queries are blocked`() {
        val thumbnailQueries = listOf(
            "porn gif",
            "sex gif",
            "nude gif",
            "xxx gif",
            "leaked nudes preview",
            "thumbzilla.com",
            "nsfw pics thumbnail"
        )

        for (query in thumbnailQueries) {
            val result = FuzzyAdultContentDetector.detect(query)
            assertNotNull("Adult thumbnail query '$query' must be strictly blocked", result)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 4. ATTEMPTS TO DISABLE SAFESEARCH VIA QUERY PARAM / SETTINGS
    // ─────────────────────────────────────────────────────────────
    @Test
    fun `test all attempts to disable SafeSearch via query params or settings are blocked`() {
        val bypassAttempts = listOf(
            "safe=off",
            "safe=images",
            "safesearch off",
            "safesearch: off",
            "safesearch=0",
            "safesearch=off",
            "adlt=off",
            "adlt=demote",
            "kp=-2",
            "turn off safesearch",
            "turn safesearch off",
            "disable safesearch",
            "disable safe search",
            "turn off safe search",
            "turn safe search off",
            "how to turn off safesearch",
            "safesearch is off",
            "google.com/safesearch",
            "bing.com/account/general",
            "duckduckgo.com/settings",
            "google.com/preferences"
        )

        for (attempt in bypassAttempts) {
            val result = FuzzyAdultContentDetector.detect(attempt)
            assertNotNull("SafeSearch disable attempt '$attempt' must be detected & blocked", result)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 5. SAFE CONTENT & EDUCATIONAL RESEARCH LOADS NORMALLY
    // ─────────────────────────────────────────────────────────────
    @Test
    fun `test clean educational queries and research load correctly without false blocks`() {
        val safeEducationalQueries = listOf(
            "sex",
            "research on sex",
            "biology of sex",
            "sex education in schools",
            "human sex chromosomes",
            "nature photography landscape",
            "google search tutorial",
            "machine learning in healthcare",
            "how to cook pork chops",
            "travel guide to portugal",
            "financial analyst career path",
            "classical music documentary",
            "high resolution wallpaper 4k"
        )

        for (query in safeEducationalQueries) {
            val result = FuzzyAdultContentDetector.detect(query)
            assertNull("Safe query '$query' must NOT be blocked (got: $result)", result)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 6. DNS LAYER SAFESEARCH VIP REDIRECTION VERIFICATION
    // ─────────────────────────────────────────────────────────────
    @Test
    fun `test DNS packet parser builds valid Google SafeSearch VIP response`() {
        // Dummy standard DNS query for www.google.com
        val dummyQuery = DnsPacketParser.DnsQuery(
            transactionId = 0x1234,
            domain = "www.google.com",
            queryType = 1, // A record
            rawPacket = byteArrayOf(
                0x12, 0x34, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x03, 'w'.code.toByte(), 'w'.code.toByte(), 'w'.code.toByte(),
                0x06, 'g'.code.toByte(), 'o'.code.toByte(), 'o'.code.toByte(), 'g'.code.toByte(), 'l'.code.toByte(), 'e'.code.toByte(),
                0x03, 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(), 0x00,
                0x00, 0x01, 0x00, 0x01
            )
        )

        val response = DnsPacketParser.buildGoogleSafeSearchResponse(dummyQuery)
        assertNotNull(response)
        assertTrue("DNS response must not be empty", response.isNotEmpty())

        // Check transaction ID echo
        assertEquals(0x12.toByte(), response[0])
        assertEquals(0x34.toByte(), response[1])

        // Verify forcesafesearch IP bytes: 216.239.38.120
        val len = response.size
        assertEquals(216.toByte(), response[len - 4])
        assertEquals(239.toByte(), response[len - 3])
        assertEquals(38.toByte(), response[len - 2])
        assertEquals(120.toByte(), response[len - 1])
    }
}
