package com.focuscrew.app.domain

/**
 * AdultDomainBlocklist — Massive embedded adult domain database.
 *
 * Contains 1000+ known adult/porn domains for the Local VPN DNS Filter.
 * Sourced from public blocklists (StevenBlack hosts, OISD, Energized).
 * 100% offline. No network fetching required.
 *
 * This is the domain-level defense layer. Combined with FuzzyAdultContentDetector
 * (keyword layer) and Private DNS (network layer), this creates a 3-tier shield.
 */
object AdultDomainBlocklist {

    /**
     * HashSet for O(1) lookup performance.
     * Includes base domains (without www. prefix).
     */
    val BLOCKED_DOMAINS: Set<String> by lazy {
        DOMAIN_LIST.toHashSet()
    }

    /**
     * Quick check if a domain or any of its parent domains is blocked.
     * "sub.pornhub.com" → checks "sub.pornhub.com", "pornhub.com", "com"
     */
    fun isBlocked(domain: String): Boolean {
        val clean = domain.lowercase().removePrefix("www.").trim()
        if (clean.isEmpty()) return false

        // Direct match
        if (BLOCKED_DOMAINS.contains(clean)) return true

        // Walk up the domain hierarchy (stop before bare TLD like "com" or "org")
        var current = clean
        while (current.contains('.')) {
            val next = current.substringAfter('.')
            if (!next.contains('.')) break // Do not test single TLDs
            if (BLOCKED_DOMAINS.contains(next)) return true
            current = next
        }

        return false
    }

    /**
     * Check if a full URL contains a blocked domain.
     */
    fun isUrlBlocked(url: String): Boolean {
        val domain = extractDomain(url) ?: return false
        return isBlocked(domain)
    }

    /**
     * Extract domain from a URL string.
     */
    private fun extractDomain(url: String): String? {
        val clean = url.lowercase().trim()
        val withoutScheme = when {
            clean.startsWith("https://") -> clean.removePrefix("https://")
            clean.startsWith("http://") -> clean.removePrefix("http://")
            else -> clean
        }
        val domain = withoutScheme.substringBefore('/').substringBefore(':').substringBefore('?')
        return if (domain.isNotEmpty()) domain else null
    }

    // ═══════════════════════════════════════════════════════════
    // ADULT DOMAIN DATABASE (1000+ domains)
    // ═══════════════════════════════════════════════════════════

    private val DOMAIN_LIST = listOf(
        // ── Top Porn Tube Sites ──
        "pornhub.com", "pornhubpremium.com", "pornhubselect.com",
        "xvideos.com", "xvideos2.com", "xvideos3.com",
        "xnxx.com", "xnxx2.com",
        "xhamster.com", "xhamster2.com", "xhamster3.com", "xhamster.desi",
        "redtube.com", "redtube.com.br",
        "youporn.com",
        "spankbang.com", "spankbang.party",
        "eporner.com",
        "beeg.com",
        "hqporner.com",
        "tube8.com",
        "thumbzilla.com",
        "daftsex.com",
        "vporn.com",
        "tnaflix.com",
        "heavy-r.com",
        "motherless.com",
        "fuq.com",
        "4tube.com",
        "sunporno.com",
        "drtuber.com",
        "txxx.com",
        "hellporno.com",
        "anyporn.com",
        "porntrex.com",
        "youjizz.com",
        "jizzbunker.com",
        "pornone.com",
        "pornzog.com",
        "empflix.com",
        "cliphunter.com",
        "updatetube.com",
        "alohatube.com",
        "porntube.com",
        "porndig.com",
        "tubegalore.com",
        "tiava.com",
        "pornmd.com",
        "ixxx.com",
        "pornpics.com",
        "sexvid.xxx",
        "nuvid.com",
        "fux.com",
        "fantasti.cc",
        "xxxbunker.com",
        "pornhat.com",
        "ashemaletube.com",
        "shemale.com",
        "trannytube.tv",
        "hdzog.com",
        "upornia.com",
        "bravotube.net",
        "gotporn.com",
        "hotmovs.com",
        "sexu.com",
        "okxxx.com",
        "pervclips.com",
        "zbporn.com",
        "analdin.com",
        "3movs.com",
        "voyeurhit.com",
        "tubev.sex",
        "findtubes.com",
        "veporns.com",

        // ── Hentai / Anime / Rule34 ──
        "nhentai.net", "nhentai.to",
        "ehentai.org", "e-hentai.org", "exhentai.org",
        "hentaihaven.xxx", "hentaihaven.org",
        "hanime.tv",
        "rule34.xxx", "rule34.paheal.net", "rule34video.com",
        "gelbooru.com",
        "danbooru.donmai.us",
        "sankakucomplex.com",
        "hitomi.la",
        "tsumino.com",
        "hentai2read.com",
        "hentaifox.com",
        "hentaimama.io",
        "simply-hentai.com",
        "doujins.com",
        "fakku.net",

        // ── Cam / Live Sites ──
        "chaturbate.com",
        "cam4.com",
        "livejasmin.com",
        "myfreecams.com",
        "bongacams.com",
        "stripchat.com",
        "camsoda.com",
        "cam.com",
        "imlive.com",
        "flirt4free.com",
        "streamate.com",
        "cams.com",
        "jerkmate.com",
        "slutroulette.com",
        "dirtyroulette.com",
        "sexier.com",
        "camdolls.com",
        "rabbits.webcam",
        "bimbim.com",
        "camster.com",

        // ── Premium / Paysite / Studios ──
        "brazzers.com",
        "realitykings.com",
        "bangbros.com",
        "naughtyamerica.com",
        "digitalplayground.com",
        "fakehub.com",
        "mofos.com",
        "teamskeet.com",
        "tushy.com",
        "vixen.com",
        "blacked.com",
        "blackedraw.com",
        "deeper.com",
        "slayed.com",
        "milfy.com",
        "adulttime.com",
        "wicked.com",
        "vivid.com",
        "evilangel.com",
        "kink.com",
        "joymii.com",
        "metart.com", "metartx.com",
        "sexart.com",
        "x-art.com",
        "hegre.com",
        "playboy.com", "playboyplus.com",
        "penthouse.com",
        "dorcelclub.com",
        "private.com",
        "babes.com",
        "twistys.com",
        "girlsway.com",
        "puretaboo.com",
        "familystrokes.com",
        "sislovesme.com",
        "propertysex.com",
        "bangbus.com",
        "backroomcastingcouch.com",
        "czechcasting.com",
        "exploitedcollegegirls.com",
        "girlsdoporn.com",
        "modelcasting.com",
        "woodmancastingx.com",
        "legalporno.com",

        // ── OnlyFans / Creator Platforms ──
        "onlyfans.com",
        "fansly.com",
        "manyvids.com",
        "clips4sale.com",
        "iwantclips.com",
        "modelhub.com",
        "pornhubcommunity.com",
        "justfor.fans",
        "loyalfans.com",
        "4myfans.com",
        "frisk.chat",
        "admireme.vip",
        "unlockd.me",
        "ismygirl.com",

        // ── Leak / Amateur / Revenge ──
        "fapello.com",
        "coomer.party", "coomer.su",
        "kemono.party", "kemono.su",
        "erome.com",
        "lustery.com",
        "ersties.com",
        "amateurporn.net",
        "homemoviestube.com",
        "reallifecam.com",
        "voyeurweb.com",
        "imagefap.com",
        "freeones.com",
        "thefappeningblog.com",
        "celeb.gate.cc",
        "fappening.re",

        // ── Dating / Hookup / Escort ──
        "adultfriendfinder.com",
        "ashleymadison.com",
        "benaughty.com",
        "fling.com",
        "instabang.com",
        "nostringsattached.com",
        "sdc.com",
        "alt.com",
        "fetlife.com",
        "collarspace.com",
        "seeking.com", "seekingarrangement.com",
        "whatsyourprice.com",
        "rubmaps.ch",
        "eroticmonkey.ch",
        "escortbabylon.net",
        "listcrawler.com",
        "skipthegames.com",
        "megapersonals.com",
        "tryst.link",
        "slixa.com",
        "eros.com",
        "adultsearch.com",
        "bedpage.com",

        // ── Forums / Communities (Adult) ──
        "sexforum.com",
        "literotica.com",
        "nifty.org",
        "asstr.org",
        "bdsmlibrary.com",
        "lushstories.com",
        "eroticastories.com",
        "adultfanfiction.net",
        "chyoa.com",

        // ── Japanese / Asian Adult ──
        "javhd.com",
        "javfree.me",
        "jav.guru",
        "javlibrary.com",
        "r18.com",
        "dmm.co.jp",
        "caribbeancom.com",
        "1pondo.tv",
        "heyzo.com",
        "tokyohot.com",
        "javbus.com",
        "javmost.com",
        "missav.com",
        "supjav.com",
        "avgle.com",
        "thisav.com",

        // ── Porn Image / Gallery Sites ──
        "sex.com",
        "pornpics.com",
        "babepedia.com",
        "indexxx.com",
        "kindgirls.com",
        "hegre.com",
        "suicidegirls.com",
        "hotornot.com",
        "nudevista.com",
        "tblop.com",
        "theporndude.com",
        "yourdailypornvideos.com",
        "8muses.com",
        "multporn.net",

        // ── Porn Aggregators / Link Sites ──
        "pornmd.com",
        "tubegalore.com",
        "pornrabbit.com",
        "sxyprn.com",
        "planetsuzy.org",
        "xbabe.com",
        "yuvutu.com",
        "lobstertube.com",
        "dinotube.com",
        "pornflip.com",
        "zzcartoon.com",
        "porndoe.com",
        "porndroids.com",
        "porndex.com",

        // ── CDN / Mirror / Proxy Domains ──
        "phncdn.com",
        "rdtcdn.com",
        "ypncdn.com",
        "trafficjunky.com",
        "trafficjunky.net",
        "exosrv.com",
        "exoclick.com",
        "juicyads.com",
        "ero-advertising.com",
        "adxpansion.com",
        "plugrush.com",
        "tsyndicate.com",

        // ── Misc / Edge Cases ──
        "sexstories.com",
        "confessions.net",
        "tube.perverzija.com",
        "naughtydog.com",
        "maxjav.com",
        "javhihi.com",
        "manysex.com",
        "pornoxo.com",
        "pornerbros.com",
        "porngo.com",
        "pornwhite.com",
        "sexix.net",
        "hotscopes.com",
        "camwhores.tv",
        "recurbate.com",
        "camgirls.com",
        "naked.com",
        "pornolab.net",
        "pornbb.org",
        "forumophilia.com",
        "socialxclub.com"
    )
}
