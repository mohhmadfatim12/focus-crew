package com.focuscrew.app.domain

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class FuzzyAdultContentDetectorTest {

    @Test
    fun `test exact adult keywords detect accurately`() {
        val result = FuzzyAdultContentDetector.detect("watch free porn videos hd")
        assertNotNull("Should detect porn keywords", result)
    }

    @Test
    fun `test user query comatozzze xxx is detected`() {
        val result = FuzzyAdultContentDetector.detect("comatozzze xxx")
        assertNotNull("User bypass attempt 'comatozzze xxx' must be detected", result)
    }

    @Test
    fun `test research on sex is allowed but explicit sex videos and terms are strictly blocked`() {
        // Research and educational queries containing 'sex' must be allowed
        val allowedResearchQueries = listOf(
            "sex",
            "research on sex",
            "biology of sex",
            "sex education",
            "human sex chromosomes"
        )
        for (q in allowedResearchQueries) {
            val r = FuzzyAdultContentDetector.detect(q)
            assertNull("Research query '$q' should be allowed for study (got: $r)", r)
        }

        // Explicit video / porn / act searches must be strictly blocked
        val blockedExplicitQueries = listOf(
            "sex videos",
            "sex video",
            "hot sex",
            "desi sex",
            "pakistani sex",
            "xxx sex",
            "hardcore sex videos",
            "sex movie hd",
            "porn",
            "fuck",
            "boobs",
            "pussy",
            "tits",
            "naked",
            "nude",
            "safe=off",
            "safesearch off",
            "safesearch: off",
            "turn off safesearch",
            "turn safe search off",
            "disable safesearch",
            "disable safe search",
            "how to turn off safesearch",
            "safesearch is off",
            "adlt=off",
            "kp=-2"
        )
        for (q in blockedExplicitQueries) {
            val r = FuzzyAdultContentDetector.detect(q)
            assertNotNull("Query '$q' must be detected as adult/tamper content", r)
        }
    }

    @Test
    fun `test incomplete typing and innocent words are not blocked prematurely`() {
        val innocentAndPartialQueries = listOf(
            "p",
            "po",
            "por",
            "pork",
            "portugal",
            "portion",
            "portrait",
            "portal",
            "portfolio",
            "pork chops",
            "visit portugal",
            "corn",
            "popcorn",
            "fork",
            "cork",
            "dock",
            "rock",
            "truck",
            "title",
            "titanic",
            "netlify",
            "github",
            "react",
            "render",
            "vercel"
        )
        for (q in innocentAndPartialQueries) {
            val r = FuzzyAdultContentDetector.detect(q)
            assertNull("Partial typing or innocent query '$q' must NOT be blocked (got: $r)", r)
        }
    }

    @Test
    fun `test leetspeak p0rn and s3x are detected`() {
        val r1 = FuzzyAdultContentDetector.detect("best p0rn sites")
        assertNotNull("p0rn must be detected", r1)

        val r2 = FuzzyAdultContentDetector.detect("hot s3x movie")
        assertNotNull("s3x must be detected", r2)

        val r3 = FuzzyAdultContentDetector.detect("pr0n video free")
        assertNotNull("pr0n must be detected", r3)
    }

    @Test
    fun `test spaced and punctuated keyword bypasses are detected`() {
        val r1 = FuzzyAdultContentDetector.detect("p.o.r.n website")
        assertNotNull("p.o.r.n must be detected", r1)

        val r2 = FuzzyAdultContentDetector.detect("p-o-r-n full movie")
        assertNotNull("p-o-r-n must be detected", r2)

        val r3 = FuzzyAdultContentDetector.detect("x x x clips")
        assertNotNull("x x x must be detected", r3)
    }

    @Test
    fun `test domain blocklist detection via FuzzyAdultContentDetector`() {
        val r1 = FuzzyAdultContentDetector.detect("https://www.pornhub.com/view_video")
        assertNotNull("Pornhub url must be detected", r1)

        val r2 = FuzzyAdultContentDetector.detect("xhamster.com/categories")
        assertNotNull("xhamster domain must be detected", r2)
    }

    @Test
    fun `test innocent words are not falsely flagged`() {
        // Essential words containing substrings like 'sex', 'anal', 'cock' etc.
        val innocentQueries = listOf(
            "Middlesex university courses",
            "Sussex county cricket club",
            "Financial analyst job opening",
            "Data analysis python pandas",
            "Cocktail recipes for party",
            "Classic car documentary",
            "Physical therapist near me",
            "Document management system",
            "Bass guitar lessons",
            "Assessment test results",
            "Scunthorpe united football",
            "Dickens great expectations",
            "Google search engine",
            "Google search preferences",
            "Ice cream shop near me",
            "Top rated movies on Netflix",
            "Live streaming camera setup",
            "Stripes pattern design wallpaper",
            "How to cook pork chops",
            "Squirrel facts in biology",
            "Razer gaming mouse setup",
            "Sore throat remedies",
            "This is only for you",
            "WhatsApp messenger chat",
            "Free Fire battlegrounds gameplay",
            "PUBG mobile update",
            "YouTube tutorial for Android"
        )

        for (query in innocentQueries) {
            val result = FuzzyAdultContentDetector.detect(query)
            assertNull("Innocent query '$query' should NOT be detected as adult (matched: $result)", result)
        }
    }

    @Test
    fun `test adult domain blocklist direct and subdomain matches`() {
        assertTrue(AdultDomainBlocklist.isBlocked("pornhub.com"))
        assertTrue(AdultDomainBlocklist.isBlocked("m.pornhub.com"))
        assertTrue(AdultDomainBlocklist.isBlocked("video.xvideos.com"))
        assertTrue(AdultDomainBlocklist.isBlocked("onlyfans.com"))
        assertTrue(AdultDomainBlocklist.isBlocked("rule34.xxx"))

        // Innocent domains
        assertFalse(AdultDomainBlocklist.isBlocked("google.com"))
        assertFalse(AdultDomainBlocklist.isBlocked("wikipedia.org"))
        assertFalse(AdultDomainBlocklist.isBlocked("github.com"))
        assertFalse(AdultDomainBlocklist.isBlocked("stackoverflow.com"))
    }
}
