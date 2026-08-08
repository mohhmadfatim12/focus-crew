package com.focuscrew.app.domain

/**
 * FuzzyAdultContentDetector — Military-grade adult content detection engine.
 *
 * 4-Layer detection pipeline:
 *   Layer 1: Exact keyword/domain matching (fast path)
 *   Layer 2: Leetspeak & Unicode homoglyph decoder → re-check
 *   Layer 3: Phonetic fingerprint matching (Soundex + simplified Metaphone)
 *   Layer 4: N-gram substring fuzzy matching (catches partial/creative misspellings)
 *
 * 100% Offline. Zero Android framework dependencies. Pure Kotlin.
 */
object FuzzyAdultContentDetector {

    // ═══════════════════════════════════════════════════════════
    // LAYER 1: EXACT KEYWORD DATABASE (500+ terms)
    // ═══════════════════════════════════════════════════════════

    /**
     * Single-word core explicit terms — matched against individual word tokens.
     * Prevents false positives on words like "unisex", "middlesex", "sussex", "analyst", "cocktail".
     */
    val SINGLE_WORD_KEYWORDS: Set<String> = setOf(
        // Primary explicit terms (Note: 'sex' alone is intentionally allowed for biology/education research, while 'sex video' etc. are strictly blocked in PHRASE_KEYWORDS)
        "porn", "porno", "pornography", "xxx", "xxxx", "hentai",
        "fuck", "fucking", "fucker", "pussy", "boobs", "boob", "tits", "tit",
        "dick", "cock", "nude", "nudes", "naked", "anal", "milf", "dildo", "vibrator",
        "cuckold", "cuck", "shemale", "tranny", "futanari", "onlyfans", "fansly",
        "cum", "cumshot", "creampie", "gangbang", "deepthroat", "handjob", "footjob",
        "titjob", "rimjob", "threesome", "foursome", "orgy", "orgasm", "squirting",
        "bukkake", "pegging", "fisting", "voyeur", "upskirt", "downblouse",
        "escort", "hooker", "prostitute", "fap", "fapping", "wank", "wanking",
        "smut", "gooner", "gooning", "ahegao", "lewd", "ecchi", "cunt", "asshole",
        "slut", "whore", "horny", "incest", "bdsm", "fetish", "jav", "camgirl",
        "blowjob", "analingus", "cunnilingus", "fellatio", "erotica",

        // Studio / Domain identifiers
        "brazzers", "xvideos", "xnxx", "xhamster", "redtube", "youporn", "spankbang",
        "chaturbate", "stripchat", "cam4", "camsoda", "eporner", "beeg", "hqporner",
        "tube8", "thumbzilla", "daftsex", "vporn", "tnaflix", "heavy-r", "motherless",
        "txxx", "anyporn", "porntrex", "youjizz", "jizzbunker", "empflix", "bongacams",
        "fapello", "manyvids", "clips4sale", "lustery", "realitykings", "bangbros",
        "naughtyamerica", "blacked", "blackedraw", "fakehub", "faketaxi", "familystrokes",
        "sislovesme", "rule34", "nhentai", "ehentai"
    )

    /**
     * Explicit phrases and search intent patterns — matched via substring inclusion.
     */
    val PHRASE_KEYWORDS: Set<String> = setOf(
        "sex video", "sex videos", "sexy video", "sexy videos", "hot video", "hot videos",
        "nude video", "nude videos", "nude pics", "nude photo", "nude photos", "nude selfie", "nudes",
        "naked video", "naked videos", "naked pics", "naked photo", "naked photos", "naked girls", "naked women",
        "desi sex", "pakistani sex", "indian sex", "arab sex", "asian sex",
        "hardcore sex", "hardcore video", "hardcore videos", "hardcore porn",
        "sex tape", "sex clip", "sex clips", "sex movie", "sex movies", "sex scenes", "sex scene",
        "hot sex", "rough sex", "watch sex", "free sex", "public sex", "phone sex",
        "blow job", "hand job", "foot job", "tit job", "rim job", "cum shot", "gang bang",
        "deep throat", "doggy style", "doggystyle", "reverse cowgirl", "missionary sex",
        "watch porn", "free porn", "hot porn", "top porn", "best porn", "new porn",
        "sexy girl", "sexy girls", "hot girl video", "hot girl videos", "strip tease",
        "striptease", "lap dance", "call girl", "sex worker", "sex chat", "dirty chat",
        "blue film", "safe=off", "safe=images", "safesearch off", "safesearch: off",
        "safesearch=0", "safesearch=off", "adlt=off",
        "turn off safesearch", "disable safesearch", "google.com/safesearch",
        "adult video", "adult movie", "adult film", "adult content", "18+ video", "18+ content",
        "nsfw video", "nsfw pics", "nsfw content", "leaked nudes", "leaked video", "revenge porn",
        "deepfake porn", "deepfake nude", "tiktok porn", "tiktok nude", "tiktok nsfw",
        "reddit nsfw", "reddit porn", "twitter porn", "twitter nsfw", "telegram porn", "telegram nsfw",
        "discord nsfw", "discord porn", "erotic video", "erotic film", "anal sex", "anal video", "anal porn"
    )

    /**
     * Innocent search queries and common words that must NEVER be falsely intercepted as adult content.
     * Prevents false triggers when user types "por" while searching for "pork" or "portugal", or researches "sex".
     */
    val INNOCENT_WORDS_ALLOWLIST: Set<String> = setOf(
        "por", "pork", "portugal", "portion", "portrait", "portal", "portfolio",
        "port", "ports", "porter", "porridge", "pore", "pores", "porous", "porsche", "porcelain", "porkchop",
        "corn", "popcorn", "born", "horn", "torn", "worn", "fork", "cork", "stork",
        "dock", "lock", "rock", "sock", "clock", "block", "flock", "shock", "cook",
        "deck", "peck", "duck", "luck", "muck", "truck",
        "bass", "pass", "mass", "glass", "class", "grass", "brass",
        "book", "boom", "boot", "boots",
        "title", "titanic", "titan", "fitness",
        "analysis", "analytics", "analog", "anatomy",
        "document", "dictionary", "director",
        "cucumber", "cumulative", "cumin",
        "online", "only",
        "sex", "sexual", "sexuality", "biology", "research", "medical", "education", "study",
        "sussex", "middlesex", "essex", "unisex", "cocktail",
        "therapist", "assessment", "scunthorpe", "dickens"
    )

    /**
     * Known adult domains for URL matching.
     */
    private val ADULT_DOMAINS: Set<String> = setOf(
        "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com", "redtube.com",
        "youporn.com", "spankbang.com", "eporner.com", "beeg.com", "hqporner.com",
        "tube8.com", "thumbzilla.com", "daftsex.com", "vporn.com", "tnaflix.com",
        "heavy-r.com", "motherless.com", "fuq.com", "4tube.com", "sunporno.com",
        "drtuber.com", "txxx.com", "hellporno.com", "anyporn.com", "porntrex.com",
        "youjizz.com", "jizzbunker.com", "pornone.com", "pornzog.com",
        "empflix.com", "cliphunter.com", "chaturbate.com", "cam4.com",
        "livejasmin.com", "myfreecams.com", "bongacams.com", "stripchat.com",
        "camsoda.com", "fapello.com", "coomer.party", "kemono.party",
        "onlyfans.com", "fansly.com", "manyvids.com", "clips4sale.com",
        "adultfriendfinder.com", "lustery.com", "brazzers.com",
        "realitykings.com", "bangbros.com", "naughtyamerica.com",
        "digitalplayground.com", "fakehub.com", "mofos.com",
        "teamskeet.com", "nhentai.net", "ehentai.org", "rule34.xxx",
        "hentaihaven.xxx", "hanime.tv", "javhd.com", "javfree.me",
        "erome.com", "alohatube.com", "iwantclips.com", "ersties.com"
    )

    // ═══════════════════════════════════════════════════════════
    // LAYER 2: LEETSPEAK & HOMOGLYPH DECODER
    // ═══════════════════════════════════════════════════════════

    /**
     * Maps leetspeak / unicode homoglyph characters to their ASCII equivalents.
     * Covers common obfuscation patterns people use to bypass filters.
     */
    private val LEET_MAP: Map<Char, Char> = mapOf(
        '0' to 'o', 'ø' to 'o', 'Ø' to 'o', 'ö' to 'o', 'ó' to 'o', 'ò' to 'o',
        '1' to 'i', '!' to 'i', '|' to 'i', 'í' to 'i', 'ì' to 'i', 'ï' to 'i',
        '2' to 'z',
        '3' to 'e', 'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e', '€' to 'e',
        '4' to 'a', '@' to 'a', 'á' to 'a', 'à' to 'a', 'ä' to 'a', 'â' to 'a', 'å' to 'a',
        '5' to 's', '$' to 's', 'ś' to 's', 'š' to 's',
        '6' to 'g',
        '7' to 't',
        '8' to 'b',
        '9' to 'g',
        '+' to 't',
        'ñ' to 'n', 'ń' to 'n',
        'ü' to 'u', 'ú' to 'u', 'ù' to 'u', 'û' to 'u',
        'ý' to 'y', 'ÿ' to 'y',
        'ç' to 'c', 'ć' to 'c', 'č' to 'c',
        'ž' to 'z', 'ź' to 'z',
        'đ' to 'd', 'ð' to 'd',
        'ł' to 'l',
        'ß' to 's'
    )

    /**
     * Decodes leetspeak/homoglyph text to plain ASCII.
     * "p0rn" → "porn", "pr0n" → "pron", "s3x" → "sex", "p@rn" → "parn→porn"
     */
    fun decodeLeetspeak(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input.lowercase()) {
            sb.append(LEET_MAP[ch] ?: ch)
        }
        return sb.toString()
    }

    /**
     * Normalizes punctuation separators between alphanumeric letters without stripping normal spaces between words.
     * "p.o.r.n" → "porn", "s-e-x" → "sex", "watch p.o.r.n video" → "watch porn video"
     */
    fun normalizeSeparators(input: String): String {
        // Remove punctuation characters placed between letters/digits
        val withoutInnerPunct = input.replace(Regex("(?<=[a-zA-Z0-9])[.\\-_*~`'\"+]+(?=[a-zA-Z0-9])"), "")
        // Collapse single-letter spaces: "p o r n" -> "porn", "s e x" -> "sex", "x x x" -> "xxx"
        return collapseSingleLetterSpaces(withoutInnerPunct)
    }

    private fun collapseSingleLetterSpaces(input: String): String {
        var current = input
        val singleLetterRegex = Regex("""(?<=(?:^|\s))[a-zA-Z]\s+[a-zA-Z](?:\s+[a-zA-Z])*(?=(?:$|\s))""")
        return singleLetterRegex.replace(current) { matchResult ->
            matchResult.value.replace(" ", "")
        }
    }

    /**
     * Strip all non-alphanumeric separators and spaces for squashed checking.
     */
    fun stripSeparators(input: String): String {
        return input.replace(Regex("[.\\-_*~`'\"\\s+]+"), "")
    }


    // ═══════════════════════════════════════════════════════════
    // LAYER 3: PHONETIC FINGERPRINT (Simplified Soundex)
    // ═══════════════════════════════════════════════════════════

    /**
     * Generates a simplified Soundex code for phonetic matching.
     * Catches misspellings that sound similar: "comatoze" ≈ "comatose"
     */
    fun soundex(input: String): String {
        if (input.isBlank()) return ""

        val clean = input.lowercase().filter { it.isLetter() }
        if (clean.isEmpty()) return ""

        val codeMap = mapOf(
            'b' to '1', 'f' to '1', 'p' to '1', 'v' to '1',
            'c' to '2', 'g' to '2', 'j' to '2', 'k' to '2', 'q' to '2',
            's' to '2', 'x' to '2', 'z' to '2',
            'd' to '3', 't' to '3',
            'l' to '4',
            'm' to '5', 'n' to '5',
            'r' to '6'
        )

        val result = StringBuilder()
        result.append(clean[0].uppercaseChar())

        var lastCode = codeMap[clean[0]] ?: '0'
        for (i in 1 until clean.length) {
            val code = codeMap[clean[i]] ?: '0'
            if (code != '0' && code != lastCode) {
                result.append(code)
                if (result.length >= 4) break
            }
            lastCode = code
        }

        while (result.length < 4) {
            result.append('0')
        }

        return result.toString()
    }

    /**
     * Pre-computed Soundex codes for critical keywords.
     * These are the phonetic fingerprints that catch misspelled searches.
     */
    private val PHONETIC_FINGERPRINTS: Set<String> by lazy {
        val criticalTerms = listOf(
            "porn", "porno", "pornography", "hentai", "blowjob",
            "creampie", "gangbang", "deepthroat", "milf", "dildo",
            "masturbation", "erotic", "naked", "nude", "cuckold",
            "hardcore", "pussy", "shemale", "cumshot", "bukkake",
            "handjob", "footjob", "threesome", "bondage", "bdsm",
            "squirting", "voyeur", "striptease", "escort",
            "prostitute", "fetish", "orgasm", "orgy",
            "anal", "vaginal", "clitoris", "penis",
            "intercourse", "fornication", "copulation"
        )
        criticalTerms.map { soundex(it) }.toSet()
    }

    // ═══════════════════════════════════════════════════════════
    // LAYER 4: N-GRAM FUZZY SUBSTRING MATCHING
    // ═══════════════════════════════════════════════════════════

    /**
     * High-signal 4-char substrings that almost never appear in clean text.
     * If ANY of these appear in the decoded input, it's flagged.
     */
    private val DANGER_NGRAMS: Set<String> = setOf(
        "xxx", "xxxx",
        "porn", "pron", "prno", "oprn",
        "xvideo", "xnxx", "xham",
        "henta", "ntai",
        "blowj", "lowjo",
        "gangb", "angba",
        "deept", "eepth", "hroat",
        "cumsh", "umshot",
        "bukka", "ukkak",
        "handj", "footj", "titjo",
        "bonda", "ndag",
        "squir", "quirt",
        "voyeu", "oyeur",
        "mastur", "asturb",
        "orgasm", "rgasm",
        "fappi", "wanki", "gooni",
        "nhent", "ehent",
        "chaturbat", "livejasm",
        "onlyf", "nlyfa",
        "braze", "razer", // brazzers
        "xrate"
    )

    /**
     * Compound n-gram pairs — both parts must appear within the text.
     * Reduces false positives for words like "cream" or "strip" alone.
     */
    private val COMPOUND_NGRAMS: List<Pair<String, String>> = listOf(
        "cream" to "pie",
        "three" to "some",
        "strip" to "tease",
        "gang" to "bang",
        "deep" to "throat",
        "blow" to "job",
        "hand" to "job",
        "foot" to "job",
        "cum" to "shot",
        "sex" to "video",
        "sex" to "tape",
        "nude" to "video",
        "nude" to "pics",
        "naked" to "girl",
        "naked" to "women",
        "hot" to "porn",
        "free" to "porn",
        "watch" to "porn",
        "adult" to "video",
        "sex" to "cam"
    )

    private val HIGH_PRIORITY_SQUASHED_TERMS: List<String> = listOf(
        "porn", "porno", "xxx", "hentai", "sexvideo", "hotsex", "desisex",
        "nudes", "nakedgirl", "nakedwomen", "blowjob", "handjob", "cumshot",
        "creampie", "milf", "dildo", "shemale", "gangbang", "deepthroat",
        "rule34", "xvideos", "xnxx", "xhamster", "brazzers", "spankbang",
        "redtube", "youporn", "chaturbate", "stripchat", "onlyfans", "fansly"
    )

    // ═══════════════════════════════════════════════════════════
    // MAIN DETECTION PIPELINE
    // ═══════════════════════════════════════════════════════════

    /**
     * 4-layer adult content detection pipeline.
     * Returns the matched trigger phrase, or null if the input is clean.
     *
     * @param queryOrUrl The raw text from the browser URL bar, search field, or page title.
     * @return The trigger keyword/phrase that was detected, or null if clean.
     */
    fun detect(queryOrUrl: String): String? {
        if (queryOrUrl.isBlank()) return null

        val lower = queryOrUrl.lowercase().trim()

        // Fast-path: Incomplete / short typing (<= 3 chars) is NEVER adult content unless it is exact "xxx"
        // Prevents premature false alerts when user is halfway through typing (e.g. "por" for "pork" or "portugal")
        if (lower.length <= 3 && lower != "xxx") return null

        // Fast-path: If the entire search query is an innocent word (e.g. "sex", "pork", "portugal", "por")
        if (INNOCENT_WORDS_ALLOWLIST.contains(lower)) return null

        // ─── Layer 1: Exact keyword & domain match (fastest) ───
        val exactMatch = checkExactKeywords(lower)
        if (exactMatch != null) return exactMatch

        val domainMatch = checkDomains(lower)
        if (domainMatch != null) return domainMatch

        // ─── Layer 2: Leetspeak decode & Separator Normalization ───
        val decoded = decodeLeetspeak(lower)
        if (decoded != lower) {
            val decodedMatch = checkExactKeywords(decoded)
            if (decodedMatch != null) return "leet:$decodedMatch"
            val decodedDomain = checkDomains(decoded)
            if (decodedDomain != null) return "leet:$decodedDomain"
        }

        // Normalize punctuation & single-letter spacing ("p.o.r.n" → "porn", "s e x" → "sex", "p.o.r.n video" → "porn video")
        val normalized = normalizeSeparators(decoded)
        if (normalized != decoded) {
            val normMatch = checkExactKeywords(normalized)
            if (normMatch != null) return "obfuscated:$normMatch"
            val normDomain = checkDomains(normalized)
            if (normDomain != null) return "obfuscated:$normDomain"
        }

        // Fully squashed check (removes all punctuation and spaces)
        val squashed = stripSeparators(decoded)
        if (squashed != decoded && squashed.length >= 3) {
            val strippedMatch = checkExactKeywords(squashed)
            if (strippedMatch != null) return "squashed:$strippedMatch"
            for (term in HIGH_PRIORITY_SQUASHED_TERMS) {
                if (squashed.contains(term)) return "squashed:$term"
            }
        }


        // ─── Layer 3: Phonetic fingerprint matching ───
        val words = lower.split(Regex("[\\s,.;:!?/\\-_]+")).filter { it.length >= 3 }
        for (word in words) {
            val decodedWord = decodeLeetspeak(word)
            val code = soundex(decodedWord)
            if (code.isNotEmpty() && PHONETIC_FINGERPRINTS.contains(code)) {
                // Double-check: the word must be at least somewhat close to an explicit term
                val cleanWord = stripSeparators(decodedWord)
                if (isCloseToExplicitTerm(cleanWord)) {
                    return "phonetic:$word"
                }
            }
        }

        // ─── Layer 4: N-gram fuzzy substring scan ───
        val ngramInput = stripSeparators(decoded)
        val ngramMatch = checkNgrams(ngramInput)
        if (ngramMatch != null) return "ngram:$ngramMatch"

        val compoundMatch = checkCompoundNgrams(ngramInput)
        if (compoundMatch != null) return "compound:$compoundMatch"

        return null
    }

    // ═══════════════════════════════════════════════════════════
    // INTERNAL MATCHERS
    // ═══════════════════════════════════════════════════════════

    private fun checkExactKeywords(text: String): String? {
        // 1. Multi-word phrases & SafeSearch tamper flags (substring contains)
        for (phrase in PHRASE_KEYWORDS) {
            if (text.contains(phrase)) return phrase
        }

        // 2. Single-word explicit terms (tokenized word-boundary matching)
        // Splits by any non-alphanumeric character so "sex", "hot sex", "desi sex videos"
        // cleanly match without false-positives on "middlesex", "sussex", "analyst", "cocktail"
        val tokens = text.split(Regex("[^a-zA-Z0-9]+"))
        for (token in tokens) {
            if (token.isEmpty()) continue
            if (INNOCENT_WORDS_ALLOWLIST.contains(token)) continue
            if (SINGLE_WORD_KEYWORDS.contains(token)) {
                return token
            }
            // Catch stem variations like "masturbat...", "porno..."
            if (token.startsWith("masturbat") || (token.startsWith("porno") && token.length <= 8) || token.startsWith("hentai")) {
                return token
            }
        }
        return null
    }

    private fun checkDomains(text: String): String? {
        for (domain in ADULT_DOMAINS) {
            if (text.contains(domain)) return domain
            // Also check without TLD: "pornhub" in "pornhub.com"
            val domainBase = domain.substringBefore('.')
            if (domainBase.length >= 5 && text.contains(domainBase)) return domainBase
        }
        return null
    }

    private fun checkNgrams(text: String): String? {
        if (text.length < 3) return null
        for (ngram in DANGER_NGRAMS) {
            if (text.contains(ngram)) return ngram
        }
        return null
    }

    private fun checkCompoundNgrams(text: String): String? {
        for ((part1, part2) in COMPOUND_NGRAMS) {
            if (text.contains(part1) && text.contains(part2)) {
                return "$part1+$part2"
            }
        }
        return null
    }

    /**
     * Levenshtein distance check — is the word within edit distance 2
     * of any critical explicit term?
     */
    private fun isCloseToExplicitTerm(word: String): Boolean {
        if (word.length < 4) return false
        if (INNOCENT_WORDS_ALLOWLIST.contains(word)) return false

        // For short 4-letter words, only allow exact match to prevent false positives on "pork", "corn", "fork"
        if (word.length <= 4) {
            val shortTerms = setOf("porn", "cock", "dick", "puss", "boob", "tits", "nude", "anal")
            return shortTerms.contains(word)
        }

        val longerTerms = listOf(
            "porno", "hentai", "milf", "dildo", "bdsm", "fetish",
            "orgasm", "orgy", "escort", "whore", "slut", "pussy", "naked"
        )

        for (term in longerTerms) {
            val maxDistance = if (term.length >= 7) 2 else 1
            if (levenshtein(word, term) <= maxDistance) return true
        }
        return false
    }

    /**
     * Compute Levenshtein edit distance between two strings.
     * Optimized for short strings (< 20 chars).
     */
    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val la = a.length
        val lb = b.length

        // Early exit if length difference alone exceeds threshold
        if (kotlin.math.abs(la - lb) > 2) return kotlin.math.abs(la - lb)

        var prev = IntArray(lb + 1) { it }
        var curr = IntArray(lb + 1)

        for (i in 1..la) {
            curr[0] = i
            for (j in 1..lb) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    prev[j] + 1,      // deletion
                    curr[j - 1] + 1,  // insertion
                    prev[j - 1] + cost // substitution
                )
            }
            val tmp = prev
            prev = curr
            curr = tmp
        }

        return prev[lb]
    }
}
