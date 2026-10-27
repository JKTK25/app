package com.example.engine

import android.net.Uri
import com.example.data.model.CustomRule
import com.example.data.model.RuleCategory
import com.example.data.model.RuleType
import com.example.data.model.ThreatCategory
import com.example.data.model.ThreatSeverity
import java.util.Locale

data class EvaluationResult(
    val isBlocked: Boolean,
    val category: ThreatCategory = ThreatCategory.ADULT_WEBSITE,
    val severity: ThreatSeverity = ThreatSeverity.HIGH,
    val triggerReason: String = "",
    val matchedTerm: String = "",
    val sanitizedUrl: String? = null
)

object BlockEngine {

    // Common adult TLDs
    private val ADULT_TLDS = setOf(
        ".xxx", ".adult", ".porn", ".sex", ".sexy", ".cam", ".dating", ".fetish", ".erotica"
    )

    // Common adult tube, cam, escort and porn domain signatures (strictly distinct adult brands)
    private val ADULT_DOMAIN_SIGNATURES = setOf(
        "pornhub", "xvideos", "xnxx", "xhamster", "youporn", "redtube", "brazzers",
        "chaturbate", "onlyfans", "camsoda", "stripchat", "livejasmin", "myfreecams",
        "cam4", "flirt4free", "bonga", "adultfriendfinder", "fapbox", "faproulette",
        "beeg", "spankbang", "eporner", "daftsex", "tnaflix", "thumbzilla", "tube8",
        "youjizz", "pornmd", "heavy-r", "motherless", "hqporner", "luscious",
        "hentaihaven", "hanime", "nhentai", "gelbooru", "danbooru", "rule34", "e-hentai",
        "nudevista", "playboy", "penthouse", "hustler",
        "bangbros", "realitykings", "naughtyamerica", "evilangel", "twistys", "mofos",
        "teamskeet", "digitalplayground", "wickedpictures", "swapporn", "porngifs", "pornstars",
        "sexvid", "freetube", "hqporn", "xxxbunker", "hotmovs", "xmovies", "palimas"
    )

    // Comprehensive list of innocent words that contain explicit sub-strings (Prevents Scunthorpe false positives)
    private val KNOWN_SAFE_WORDS = setOf(
        "assistant", "assistance", "assist", "assisted", "assisting", "assists",
        "class", "classic", "classical", "classmate", "classroom", "classify", "classes", "classified",
        "pass", "passion", "passionate", "passport", "passage", "passenger", "passive", "passed", "passing", "password", "passwords",
        "mass", "massage", "massages", "massive", "massachusetts", "masses",
        "grass", "glass", "glasses", "brass", "compass", "embarrass", "harass", "harassment", "trespass",
        "assembly", "assemble", "assembled", "assume", "assuming", "assumption", "asset", "assets", "assert", "assertion",
        "assign", "assigned", "assignment", "assignments", "associate", "associated", "association", "associations",
        "assess", "assessed", "assessment", "assessments",
        "document", "documentation", "cucumber", "circumstance", "cumbersome", "accumulate", "accumulation",
        "analytics", "analysis", "analyze", "analyzed", "analyst", "analog", "analogy",
        "title", "titles", "titan", "titanic", "entity", "entities", "attitude", "substitute", "institute", "institution", "constitution",
        "competitor", "competition",
        "dickens", "dickinson", "cockpit", "cocktail", "cockatoo", "peacock", "shuttlecock",
        "section", "sector", "semester", "consecutive", "prosecute", "prosecution", "sense", "sensitive", "sensory",
        "kumasi", "takoradi", "mataka", "matoke", "matatu", "kitabu", "samaki", "rafiki", "mwalimu", "habari", "asante"
    )

    // Extensive explicit, vulgar, and pornographic keyword dictionary (Multilingual: English, Kiswahili, Sheng, leetspeak)
    private val EXPLICIT_KEYWORDS = setOf(
        // Standard English Adult & Explicit Terms
        "porn", "xxx", "sex", "pussy", "vagina", "penis", "cock", "dick", "dildo",
        "blowjob", "handjob", "cum", "cumshot", "creampie", "orgasm", "masturbat",
        "masturbation", "hentai", "erotic", "erotica", "nude", "nudity", "naked",
        "stripper", "striptease", "gangbang", "threesome", "three sum", "foursome", "orgy", "squirt",
        "swinger", "shemale", "transsexual", "fetish", "bdsm", "bondage", "sadomaso",
        "anal", "rimming", "pegging", "deepthroat", "bitch", "horny", "slut", "whore",
        "tits", "boobs", "cleavage", "playboy", "penthouse", "hustler", "cams",
        "webcam girl", "live cam", "cam model", "escort", "hardcore sex", "softcore",
        "incest", "taboo sex", "stepmom", "stepsister", "stepdaughter", "stepson",
        "teen porn", "milf", "mature porn", "amateur sex", "voyeur", "upskirt", "creepshot",
        "cuckold", "femdom", "strapon", "watersports", "golden shower", "bukkake",
        "snuff", "leaked nudes", "onlyfans leak", "sex tape", "sex video", "porn video",
        "free porn", "watch porn", "porno", "sex film", "adult movie", "nude photo",
        "erotic massage", "happy ending massage", "adult hookup", "one night stand",
        "sex chat", "dirty chat", "cyber sex", "sexting", "phone sex", "fap", "fapping",
        "camgirl", "nsfw", "ecchi", "adult game", "adult games", "xxx game", "sex game",
        "hot video", "erotic video", "nude pics", "naked girls", "naked women",
        "sexy", "sexy video", "sexy girl", "sexy girls", "sexy hot", "sexy dance",
        "fuck", "fucking", "fucked", "fuck me", "fuck you", "motherfucker", "bastard",
        "ass", "asshole", "bum", "booty", "big booty", "juicy ass", "big ass", "nude ass",
        "shake that ass", "shake that bum", "shake ass", "shake bum", "shake your ass",
        "shake that booty", "twerk", "twerking", "booty dance", "booty shake",
        "kiss", "party", "toxic masculinity",

        // Kiswahili & Sheng Vulgarities, Adult Anatomy & Sexual Slang
        "kuma", "kumamako", "kumanyoko", "kumayako", "kukuana", "kukuana ngono",
        "matako", "tako", "machoko", "shika matako", "matako makubwa",
        "mujulus", "mjulubeng", "mjulubenga", "mboro", "mboo", "mkundu",
        "kutomba", "tomba", "kutombana", "tombana", "tombwa",
        "kudinya", "dinya", "kudinyana", "dinyana",
        "malaya", "kahaba", "malaya wa mtaani", "malaya nairobi", "malaya mombasa", "malaya dar",
        "punyeto", "kupiga punyeto", "piga nyeto", "nyeto", "saga sumu",
        "ngono", "ngono tupu", "ngono za siri", "uchafu wa ngono",
        "uchi", "utupu", "picha za uchi", "video za uchi", "video za ngono",
        "video za ngono kenya", "video za ngono tanzania", "bongo nudes", "kenya nudes",
        "msenge", "mashoga", "basha", "kufirwa", "kufira", "firwa", "fira",
        "sugua", "sugua kuma", "sugua mboro", "sugua tako",
        "kukata mauno", "mauno", "kiuno cha ngono", "kukata kiuno", "chura mkali", "chura wa bongo",
        "dem mkali uchi", "nudes za", "mchezo wa ngono", "shangazi nudes"
    )

    // Risk indicator prefixes or URL path tokens
    private val SUSPICIOUS_PATH_TOKENS = setOf(
        "porn", "xxx", "adult", "sex", "erotic", "nude", "naked", "milf", "hentai",
        "video_nsfw", "gallery_nsfw", "cams", "livecam", "webcam", "free_sex"
    )

    /**
     * Primary evaluation engine to check if a URL, domain, or raw search query contains adult content.
     */
    fun evaluate(
        input: String,
        aggressiveMode: Boolean = true,
        customRules: List<CustomRule> = emptyList()
    ): EvaluationResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return EvaluationResult(isBlocked = false)
        }

        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. Check Custom Whitelist (ALLOW) rules first
        val matchingAllowRule = customRules.firstOrNull { rule ->
            rule.isEnabled && rule.ruleType == RuleType.ALLOW && matchesPattern(lower, rule.pattern.lowercase())
        }
        if (matchingAllowRule != null) {
            return EvaluationResult(
                isBlocked = false,
                triggerReason = "Allowed by custom rule: ${matchingAllowRule.pattern}"
            )
        }

        // 2. Check Custom Blacklist (BLOCK) rules
        val matchingBlockRule = customRules.firstOrNull { rule ->
            rule.isEnabled && rule.ruleType == RuleType.BLOCK && matchesPattern(lower, rule.pattern.lowercase())
        }
        if (matchingBlockRule != null) {
            return EvaluationResult(
                isBlocked = true,
                category = when (matchingBlockRule.category) {
                    RuleCategory.KEYWORD -> ThreatCategory.EXPLICIT_KEYWORD
                    RuleCategory.DOMAIN -> ThreatCategory.ADULT_WEBSITE
                    RuleCategory.URL_PATH -> ThreatCategory.RISKY_REDIRECT
                },
                severity = ThreatSeverity.CRITICAL,
                triggerReason = "Matched custom block rule: ${matchingBlockRule.pattern}",
                matchedTerm = matchingBlockRule.pattern
            )
        }

        // 3. Inspect as a URL / Host if format resembles web link
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.contains(".") && !lower.contains(" ")) {
            val urlResult = evaluateUrl(lower, aggressiveMode)
            if (urlResult.isBlocked) return urlResult
        }

        // 4. Inspect as text query / keyword phrase
        return evaluateTextQuery(lower, aggressiveMode)
    }

    private fun evaluateUrl(lowerUrl: String, aggressiveMode: Boolean): EvaluationResult {
        val uri = try {
            val normalized = if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://")) {
                "https://$lowerUrl"
            } else {
                lowerUrl
            }
            Uri.parse(normalized)
        } catch (_: Exception) {
            null
        }

        val host = uri?.host ?: lowerUrl
        val path = uri?.path ?: ""
        val query = uri?.query ?: ""

        // Check Adult TLDs
        for (tld in ADULT_TLDS) {
            if (host.endsWith(tld)) {
                return EvaluationResult(
                    isBlocked = true,
                    category = ThreatCategory.ADULT_WEBSITE,
                    severity = ThreatSeverity.CRITICAL,
                    triggerReason = "Access blocked: Known adult top-level domain ($tld)",
                    matchedTerm = tld
                )
            }
        }

        // Check Adult Domain Signatures
        for (sig in ADULT_DOMAIN_SIGNATURES) {
            if (host.contains(sig)) {
                return EvaluationResult(
                    isBlocked = true,
                    category = ThreatCategory.ADULT_WEBSITE,
                    severity = ThreatSeverity.CRITICAL,
                    triggerReason = "Access blocked: High-risk adult website domain ($sig)",
                    matchedTerm = sig
                )
            }
        }

        // Check URL path tokens
        val pathSegments = path.split('/', '-', '_', '.', '+').filter { it.isNotBlank() }
        for (segment in pathSegments) {
            if (segment in SUSPICIOUS_PATH_TOKENS || segment in EXPLICIT_KEYWORDS) {
                return EvaluationResult(
                    isBlocked = true,
                    category = ThreatCategory.RISKY_REDIRECT,
                    severity = ThreatSeverity.HIGH,
                    triggerReason = "Access blocked: Explicit URL path parameter ($segment)",
                    matchedTerm = segment
                )
            }
        }

        // Check Query parameters for search engines
        if (query.isNotBlank()) {
            val queryResult = evaluateTextQuery(query, aggressiveMode)
            if (queryResult.isBlocked) {
                return queryResult.copy(
                    category = ThreatCategory.EXPLICIT_SEARCH,
                    triggerReason = "Access blocked: Explicit query payload detected in URL"
                )
            }
        }

        return EvaluationResult(isBlocked = false)
    }

    private fun evaluateTextQuery(lowerText: String, aggressiveMode: Boolean): EvaluationResult {
        // Tokenize into words and n-grams
        val sanitized = lowerText.replace(Regex("[^a-z0-9\\s]"), " ")
        val tokens = sanitized.split("\\s+".toRegex()).filter { it.isNotBlank() }

        // If entire query or any token is a known safe word, do not flag false positives
        val containsSafeContext = tokens.any { it in KNOWN_SAFE_WORDS }

        // 1. Single word exact match with safe words exemption
        for (token in tokens) {
            if (token in KNOWN_SAFE_WORDS) {
                continue
            }
            if (token in EXPLICIT_KEYWORDS) {
                return EvaluationResult(
                    isBlocked = true,
                    category = ThreatCategory.EXPLICIT_KEYWORD,
                    severity = ThreatSeverity.HIGH,
                    triggerReason = "Blocked explicit term: '$token'",
                    matchedTerm = token
                )
            }
        }

        // 2. Multi-word phrase check with strict word boundaries
        for (keyword in EXPLICIT_KEYWORDS) {
            if (keyword.contains(" ")) {
                val phraseRegex = Regex("\\b${Regex.escape(keyword)}\\b", RegexOption.IGNORE_CASE)
                if (phraseRegex.containsMatchIn(lowerText)) {
                    return EvaluationResult(
                        isBlocked = true,
                        category = ThreatCategory.EXPLICIT_SEARCH,
                        severity = ThreatSeverity.CRITICAL,
                        triggerReason = "Blocked explicit search phrase: '$keyword'",
                        matchedTerm = keyword
                    )
                }
            }
        }

        // 3. Aggressive substring matching for leetspeak or adult tube domains (strictly distinct adult brands >= 5 chars)
        if (aggressiveMode && !containsSafeContext) {
            val collapsed = lowerText.replace(" ", "").replace("_", "").replace("-", "")
            for (sig in ADULT_DOMAIN_SIGNATURES) {
                if (sig.length >= 5 && collapsed.contains(sig)) {
                    return EvaluationResult(
                        isBlocked = true,
                        category = ThreatCategory.ADULT_WEBSITE,
                        severity = ThreatSeverity.HIGH,
                        triggerReason = "Aggressive filter: Matched adult token signature '$sig'",
                        matchedTerm = sig
                    )
                }
            }
        }

        return EvaluationResult(isBlocked = false)
    }

    private fun matchesPattern(input: String, pattern: String): Boolean {
        return if (pattern.startsWith("*") || pattern.endsWith("*")) {
            val regex = pattern.replace(".", "\\.").replace("*", ".*")
            input.matches(Regex(regex))
        } else {
            input.contains(pattern)
        }
    }

    /**
     * Enforces SafeSearch by rewriting popular search engine URLs to strict safe mode.
     */
    fun enforceSafeSearch(url: String): String {
        val lower = url.lowercase(Locale.ROOT)
        return try {
            when {
                lower.contains("google.") -> {
                    if (url.contains("safe=active")) url
                    else {
                        val delimiter = if (url.contains("?")) "&" else "?"
                        "$url${delimiter}safe=active&ssui=on"
                    }
                }
                lower.contains("bing.com") -> {
                    if (url.contains("adlt=strict")) url
                    else {
                        val delimiter = if (url.contains("?")) "&" else "?"
                        "$url${delimiter}adlt=strict"
                    }
                }
                lower.contains("duckduckgo.com") -> {
                    if (url.contains("kp=1")) url
                    else {
                        val delimiter = if (url.contains("?")) "&" else "?"
                        "$url${delimiter}kp=1"
                    }
                }
                lower.contains("yahoo.com") -> {
                    if (url.contains("vm=r")) url
                    else {
                        val delimiter = if (url.contains("?")) "&" else "?"
                        "$url${delimiter}vm=r"
                    }
                }
                lower.contains("youtube.com") -> {
                    if (url.contains("safe_search=strict")) url
                    else {
                        val delimiter = if (url.contains("?")) "&" else "?"
                        "$url${delimiter}safe_search=strict"
                    }
                }
                else -> url
            }
        } catch (_: Exception) {
            url
        }
    }

    /**
     * Strictly verifies if a target, category, or reason represents pornographic or adult content.
     * Enforces the Zero-Tolerance Anti-Pornography policy where Master Password overrides are strictly forbidden.
     */
    fun isPornographyOrAdultThreat(
        target: String,
        category: ThreatCategory? = null,
        reason: String = ""
    ): Boolean {
        if (category == ThreatCategory.ADULT_WEBSITE ||
            category == ThreatCategory.EXPLICIT_SEARCH ||
            category == ThreatCategory.EXPLICIT_KEYWORD
        ) {
            return true
        }

        val lowerTarget = target.lowercase(Locale.ROOT).trim()
        val lowerReason = reason.lowercase(Locale.ROOT).trim()

        if (lowerReason.contains("adult") || lowerReason.contains("porn") ||
            lowerReason.contains("explicit") || lowerReason.contains("nsfw") ||
            lowerReason.contains("doh") || lowerReason.contains("family shield") ||
            lowerReason.contains("erotic")
        ) {
            return true
        }

        if (ADULT_TLDS.any { lowerTarget.contains(it) }) return true
        if (ADULT_DOMAIN_SIGNATURES.any { lowerTarget.contains(it) }) return true

        val eval = evaluate(lowerTarget, aggressiveMode = true)
        return eval.isBlocked && (
            eval.category == ThreatCategory.ADULT_WEBSITE ||
            eval.category == ThreatCategory.EXPLICIT_SEARCH ||
            eval.category == ThreatCategory.EXPLICIT_KEYWORD
        )
    }
}
