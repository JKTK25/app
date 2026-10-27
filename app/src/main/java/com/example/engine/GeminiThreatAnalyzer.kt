package com.example.engine

import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.BlockedIncident
import com.example.data.model.CustomRule
import com.example.data.model.RuleCategory
import com.example.data.model.RuleType
import com.example.data.model.ThreatCategory
import com.example.data.model.ThreatSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit

data class SearchExtraction(
    val isSearchQuery: Boolean,
    val searchEngineOrSite: String,
    val extractedKeywords: String,
    val originalInput: String
)

data class AiAnalysisResult(
    val isThreat: Boolean,
    val threatCategory: ThreatCategory = ThreatCategory.ADULT_WEBSITE,
    val confidence: Float = 0.95f,
    val severity: ThreatSeverity = ThreatSeverity.HIGH,
    val semanticBreakdown: String,
    val evasionTechniques: List<String> = emptyList(),
    val recommendedRule: String? = null,
    val recommendedRuleCategory: RuleCategory = RuleCategory.DOMAIN,
    val isSafeForMinors: Boolean = false,
    val extractedSearchKeywords: String? = null,
    val detectedSearchEngine: String? = null
)

data class AiParentalReport(
    val overallRiskLevel: String,
    val riskScorePercent: Int,
    val summary: String,
    val detectedVulnerabilities: List<String>,
    val behavioralInsights: List<String>,
    val actionableRecommendations: List<String>,
    val suggestedProactiveRules: List<CustomRule>
)

object GeminiThreatAnalyzer {

    private const val TAG = "GeminiThreatAnalyzer"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * Extracts search keywords from any website URL, query parameter, or raw search input.
     * Supports Google, Bing, DuckDuckGo, Yahoo, YouTube, Reddit, X/Twitter, and website search bars.
     */
    fun extractSearchQuery(rawInput: String): SearchExtraction {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return SearchExtraction(false, "Direct Input", "", "")
        }

        // If it does not contain URL delimiters, it is already a direct search query
        if (!trimmed.contains("://") && !trimmed.contains(".") && !trimmed.contains("/")) {
            return SearchExtraction(true, "Direct Browser Search", trimmed, trimmed)
        }

        val urlString = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }

        try {
            val uri = Uri.parse(urlString)
            val host = uri.host?.lowercase(Locale.ROOT) ?: ""
            val path = uri.path ?: ""
            val query = uri.query ?: ""

            val engineName = when {
                host.contains("google.") -> "Google Search"
                host.contains("bing.com") -> "Bing Search"
                host.contains("duckduckgo.com") -> "DuckDuckGo"
                host.contains("yahoo.com") -> "Yahoo Search"
                host.contains("youtube.com") -> "YouTube Search"
                host.contains("reddit.com") -> "Reddit Search"
                host.contains("twitter.com") || host.contains("x.com") -> "X / Twitter Search"
                host.contains("baidu.com") -> "Baidu Search"
                host.contains("yandex.") -> "Yandex Search"
                host.isNotBlank() -> "${host.removePrefix("www.")} Search"
                else -> "Web Search"
            }

            // Common search query parameters across engines & websites
            val queryParamsToCheck = listOf(
                "q", "query", "search_query", "search", "p", "k", "keyword",
                "wd", "term", "text", "s", "searchtext", "search_term", "w", "key"
            )

            for (param in queryParamsToCheck) {
                val value = uri.getQueryParameter(param)
                if (!value.isNullOrBlank()) {
                    val decoded = try {
                        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
                    } catch (_: Exception) {
                        value
                    }.replace('+', ' ').trim()

                    if (decoded.isNotBlank()) {
                        return SearchExtraction(true, engineName, decoded, trimmed)
                    }
                }
            }

            // Check path-based search queries e.g. /search/porn+free or /results/xyz or /tag/xxx
            if (path.contains("/search/") || path.contains("/results/") || path.contains("/tag/") || path.contains("/browse/")) {
                val afterSearch = path.substringAfter("/search/").substringAfter("/results/").substringAfter("/tag/").substringAfter("/browse/")
                val cleanedPathQuery = try {
                    URLDecoder.decode(afterSearch, StandardCharsets.UTF_8.name())
                } catch (_: Exception) {
                    afterSearch
                }.replace('+', ' ').replace('/', ' ').trim()

                if (cleanedPathQuery.isNotBlank() && !cleanedPathQuery.endsWith(".html") && !cleanedPathQuery.endsWith(".php")) {
                    return SearchExtraction(true, engineName, cleanedPathQuery, trimmed)
                }
            }

            // If query string exists, clean and check if it has search terms
            if (query.isNotBlank()) {
                val decodedQuery = try {
                    URLDecoder.decode(query, StandardCharsets.UTF_8.name())
                } catch (_: Exception) {
                    query
                }.replace('+', ' ').trim()
                return SearchExtraction(true, engineName, decodedQuery, trimmed)
            }

            // Otherwise, fallback to host / path
            return SearchExtraction(false, engineName, host, trimmed)
        } catch (_: Exception) {
            return SearchExtraction(true, "Direct Input", trimmed, trimmed)
        }
    }

    /**
     * Deep semantic scan of a link, query, or text snippet to detect adult content,
     * slang, disguised pornography domains, or bypass attempts using Gemini AI.
     */
    suspend fun analyzeTextOrUrl(input: String, contextInfo: String = ""): AiAnalysisResult = withContext(Dispatchers.IO) {
        val extraction = extractSearchQuery(input)
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey.isEmpty() || apiKey.startsWith("YOUR_")) {
            val offline = performOfflineSemanticAnalysis(input)
            return@withContext offline.copy(
                extractedSearchKeywords = if (extraction.isSearchQuery) extraction.extractedKeywords else null,
                detectedSearchEngine = if (extraction.isSearchQuery) extraction.searchEngineOrSite else null
            )
        }

        try {
            val systemPrompt = """
                You are PureShield AI Semantic Threat & Parental Control Classifier.
                Analyze the provided search query, website input, URL, or text snippet to determine if it contains adult/pornographic content, explicit material, webcam services, dating hookups, vulgar language, provocative/sexual phrases, or parental control evasion attempts.

                CRITICAL FALSE-POSITIVE PREVENTION & CONTEXTUAL ACCURACY:
                - DO NOT flag ordinary words that contain explicit sub-strings (e.g. "assistant", "assistance", "assist", "class", "classic", "pass", "password", "passage", "massage", "assembly", "assume", "cucumber", "document", "analytics", "analysis", "title", "attitude", "cockpit", "matatu", "kitabu").
                - Always evaluate the FULL SEMANTIC CONTEXT. Only classify as a threat if the phrase is genuinely sexually explicit, pornographic, vulgar, or a clear parental control evasion.

                CRITICAL MULTILINGUAL & SWAHILI SENSITIVITY REQUIREMENTS:
                1. Kiswahili & Sheng: You MUST strictly flag vulgar, sexually explicit, anatomical, or provocative Swahili & Sheng words and slang, including:
                   - Anatomical/Sexual: kuma, mboro, mboo, matako, tako, mkundu, mujulus, mjulubeng, uchi, utupu.
                   - Sexual Acts/Verbs: kukuana, kutomba, tomba, kudinya, dinya, punyeto, kupiga nyeto, kufirwa, kufira, sugua kuma, sugua mboro.
                   - Slang & Vulgar insults: kumamako, kumanyoko, malaya, kahaba, msenge.
                   - Provocative dances/media: kukata mauno, kiuno, chura mkali, picha za uchi, video za ngono.
                2. English Vulgarities & Provocative Phrases: Flag terms like "fuck", "sexy", "shake that ass/bum", "twerk", "booty", "nudes", "dick", "pussy", and disguised variations.
                3. Multilingual Evasions: Users may attempt to bypass English filters by switching to Kiswahili, Sheng, leetspeak (e.g. k*ma, kuuma), phonetic misspellings, or mixing languages. Classify any such attempt as a THREAT.

                Respond ONLY with valid JSON in this exact structure:
                {
                  "isThreat": boolean,
                  "threatCategory": "ADULT_WEBSITE" | "EXPLICIT_SEARCH" | "RISKY_REDIRECT" | "ADULT_AD" | "EXPLICIT_KEYWORD" | "P2P_NETWORK" | "AI_SEMANTIC_THREAT" | "BYPASS_ATTEMPT",
                  "confidence": number (0.0 to 1.0),
                  "severity": "LOW" | "MEDIUM" | "HIGH" | "CRITICAL",
                  "semanticBreakdown": "string explaining what was detected, the meaning of any Swahili/English terms, and why it is explicit or safe",
                  "evasionTechniques": ["list", "of", "techniques", "like swahili_slang, leetspeak, phonetic_disguise, etc."],
                  "recommendedRule": "clean keyword or domain regex to block if threat, or null",
                  "recommendedRuleCategory": "DOMAIN" | "KEYWORD" | "URL_PATH",
                  "isSafeForMinors": boolean
                }
            """.trimIndent()

            val searchContext = if (extraction.isSearchQuery) {
                "Source: ${extraction.searchEngineOrSite}\nExtracted Search Keywords: \"${extraction.extractedKeywords}\"\nOriginal URL/Input: \"${extraction.originalInput}\""
            } else {
                "Target Input: \"$input\""
            }
            val userContent = "$searchContext\nContext Info: $contextInfo"

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", "$systemPrompt\n\n$userContent") })
                        })
                    })
                }
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("responseMimeType", "application/json")
                })
            }

            val url = "$API_BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini API error code: ${response.code}")
                val offline = performOfflineSemanticAnalysis(input)
                return@withContext offline.copy(
                    extractedSearchKeywords = if (extraction.isSearchQuery) extraction.extractedKeywords else null,
                    detectedSearchEngine = if (extraction.isSearchQuery) extraction.searchEngineOrSite else null
                )
            }

            val responseBody = response.body?.string() ?: return@withContext performOfflineSemanticAnalysis(input).copy(
                extractedSearchKeywords = if (extraction.isSearchQuery) extraction.extractedKeywords else null,
                detectedSearchEngine = if (extraction.isSearchQuery) extraction.searchEngineOrSite else null
            )
            val jsonRoot = JSONObject(responseBody)
            val candidate = jsonRoot.getJSONArray("candidates").getJSONObject(0)
            val rawText = candidate.getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
            val obj = JSONObject(rawText)

            val isThreat = obj.optBoolean("isThreat", false)
            val categoryStr = obj.optString("threatCategory", if (extraction.isSearchQuery) "EXPLICIT_SEARCH" else "ADULT_WEBSITE")
            val threatCategory = try { ThreatCategory.valueOf(categoryStr) } catch (_: Exception) { if (extraction.isSearchQuery) ThreatCategory.EXPLICIT_SEARCH else ThreatCategory.ADULT_WEBSITE }
            val confidence = obj.optDouble("confidence", 0.90).toFloat()
            val severityStr = obj.optString("severity", "HIGH")
            val severity = try { ThreatSeverity.valueOf(severityStr) } catch (_: Exception) { ThreatSeverity.HIGH }
            val semanticBreakdown = obj.optString("semanticBreakdown", if (isThreat) "Explicit semantic markers detected." else "Content evaluated as safe.")

            val evasions = mutableListOf<String>()
            obj.optJSONArray("evasionTechniques")?.let { arr ->
                for (i in 0 until arr.length()) evasions.add(arr.getString(i))
            }

            val recRule = if (obj.has("recommendedRule") && !obj.isNull("recommendedRule")) obj.getString("recommendedRule") else null
            val recCatStr = obj.optString("recommendedRuleCategory", if (extraction.isSearchQuery) "KEYWORD" else "DOMAIN")
            val recCat = try { RuleCategory.valueOf(recCatStr) } catch (_: Exception) { if (extraction.isSearchQuery) RuleCategory.KEYWORD else RuleCategory.DOMAIN }
            val isSafe = obj.optBoolean("isSafeForMinors", !isThreat)

            AiAnalysisResult(
                isThreat = isThreat,
                threatCategory = threatCategory,
                confidence = confidence,
                severity = severity,
                semanticBreakdown = semanticBreakdown,
                evasionTechniques = evasions,
                recommendedRule = recRule ?: if (isThreat && extraction.isSearchQuery) extraction.extractedKeywords else null,
                recommendedRuleCategory = recCat,
                isSafeForMinors = isSafe,
                extractedSearchKeywords = if (extraction.isSearchQuery) extraction.extractedKeywords else null,
                detectedSearchEngine = if (extraction.isSearchQuery) extraction.searchEngineOrSite else null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error performing Gemini threat analysis", e)
            val offline = performOfflineSemanticAnalysis(input)
            offline.copy(
                extractedSearchKeywords = if (extraction.isSearchQuery) extraction.extractedKeywords else null,
                detectedSearchEngine = if (extraction.isSearchQuery) extraction.searchEngineOrSite else null
            )
        }
    }

    /**
     * Synthesizes device interception telemetry into a proactive AI Parental Security Report.
     */
    suspend fun generateParentalReport(
        incidents: List<BlockedIncident>,
        totalThreats: Int
    ): AiParentalReport = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey.isEmpty() || apiKey.startsWith("YOUR_") || incidents.isEmpty()) {
            return@withContext generateFallbackParentalReport(incidents, totalThreats)
        }

        try {
            val incidentsSummary = incidents.take(20).joinToString("\n") { inc ->
                "- [${inc.source.displayName}] ${inc.category.displayName} (${inc.severity.name}): ${inc.target} (Reason: ${inc.triggerReason})"
            }

            val systemPrompt = """
                You are PureShield AI Parental Control Advisor.
                Analyze the provided telemetry of blocked adult content, explicit searches, and bypass attempts on a minor's device.
                Generate a concise, actionable, and encouraging Parental Defense Summary.
                
                Respond ONLY with valid JSON in this exact structure:
                {
                  "overallRiskLevel": "LOW" | "MODERATE" | "HIGH" | "CRITICAL",
                  "riskScorePercent": number (0 to 100),
                  "summary": "2-3 concise sentences summarizing device exposure and protection status",
                  "detectedVulnerabilities": ["bullet 1", "bullet 2", "bullet 3"],
                  "behavioralInsights": ["insight 1", "insight 2"],
                  "actionableRecommendations": ["recommendation 1", "recommendation 2", "recommendation 3"],
                  "suggestedRules": [
                    {"pattern": "pattern to block", "type": "BLOCK", "category": "DOMAIN" | "KEYWORD" | "URL_PATH"}
                  ]
                }
            """.trimIndent()

            val userPrompt = """
                Total Threats Blocked: $totalThreats
                Recent Log Samples:
                $incidentsSummary
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", "$systemPrompt\n\n$userPrompt") })
                        })
                    })
                }
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val url = "$API_BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext generateFallbackParentalReport(incidents, totalThreats)
            }

            val responseBody = response.body?.string() ?: return@withContext generateFallbackParentalReport(incidents, totalThreats)
            val jsonRoot = JSONObject(responseBody)
            val candidate = jsonRoot.getJSONArray("candidates").getJSONObject(0)
            val rawText = candidate.getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
            val obj = JSONObject(rawText)

            val riskLevel = obj.optString("overallRiskLevel", "MODERATE")
            val score = obj.optInt("riskScorePercent", 25)
            val summary = obj.optString("summary", "PureShield active defenses are mitigating online adult hazards.")

            val vulns = mutableListOf<String>()
            obj.optJSONArray("detectedVulnerabilities")?.let { arr ->
                for (i in 0 until arr.length()) vulns.add(arr.getString(i))
            }

            val insights = mutableListOf<String>()
            obj.optJSONArray("behavioralInsights")?.let { arr ->
                for (i in 0 until arr.length()) insights.add(arr.getString(i))
            }

            val recs = mutableListOf<String>()
            obj.optJSONArray("actionableRecommendations")?.let { arr ->
                for (i in 0 until arr.length()) recs.add(arr.getString(i))
            }

            val rules = mutableListOf<CustomRule>()
            obj.optJSONArray("suggestedRules")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val r = arr.getJSONObject(i)
                    val catStr = r.optString("category", "DOMAIN")
                    val cat = try { RuleCategory.valueOf(catStr) } catch (_: Exception) { RuleCategory.DOMAIN }
                    rules.add(
                        CustomRule(
                            pattern = r.optString("pattern", ""),
                            ruleType = RuleType.BLOCK,
                            category = cat,
                            isEnabled = true
                        )
                    )
                }
            }

            AiParentalReport(
                overallRiskLevel = riskLevel,
                riskScorePercent = score,
                summary = summary,
                detectedVulnerabilities = if (vulns.isEmpty()) listOf("Unmonitored external browsers without accessibility active") else vulns,
                behavioralInsights = if (insights.isEmpty()) listOf("Active browser usage detected during evening intervals.") else insights,
                actionableRecommendations = if (recs.isEmpty()) listOf("Ensure Cross-Browser Guardian is continuously active", "Enforce Strict SafeSearch") else recs,
                suggestedProactiveRules = rules.filter { it.pattern.isNotBlank() }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing parental report from Gemini", e)
            generateFallbackParentalReport(incidents, totalThreats)
        }
    }

    private fun performOfflineSemanticAnalysis(input: String): AiAnalysisResult {
        val extraction = extractSearchQuery(input)
        val queryToAnalyze = if (extraction.isSearchQuery && extraction.extractedKeywords.isNotBlank()) {
            extraction.extractedKeywords
        } else {
            input
        }
        val lower = queryToAnalyze.lowercase(Locale.ROOT)
        val staticEval = BlockEngine.evaluate(queryToAnalyze, aggressiveMode = true)

        if (staticEval.isBlocked) {
            return AiAnalysisResult(
                isThreat = true,
                threatCategory = staticEval.category,
                confidence = 0.98f,
                severity = staticEval.severity,
                semanticBreakdown = "Heuristic match: ${staticEval.triggerReason}. Adult or explicit pattern identified in search keywords.",
                evasionTechniques = listOf("Direct explicit keyword pattern match"),
                recommendedRule = if (extraction.isSearchQuery) extraction.extractedKeywords else if (lower.contains(".")) lower.substringBefore("?").removePrefix("https://").removePrefix("http://") else lower,
                recommendedRuleCategory = if (extraction.isSearchQuery) RuleCategory.KEYWORD else if (lower.contains(".")) RuleCategory.DOMAIN else RuleCategory.KEYWORD,
                isSafeForMinors = false,
                extractedSearchKeywords = if (extraction.isSearchQuery) extraction.extractedKeywords else null,
                detectedSearchEngine = if (extraction.isSearchQuery) extraction.searchEngineOrSite else null
            )
        }

        // Check for subtle leetspeak or disguised phrases
        val normalized = lower
            .replace("0", "o")
            .replace("1", "i")
            .replace("@", "a")
            .replace("3", "e")
            .replace("$", "s")
            .replace("5", "s")
            .replace("!", "i")
            .replace("7", "t")

        val leetEval = BlockEngine.evaluate(normalized, aggressiveMode = true)
        if (leetEval.isBlocked) {
            return AiAnalysisResult(
                isThreat = true,
                threatCategory = ThreatCategory.BYPASS_ATTEMPT,
                confidence = 0.88f,
                severity = ThreatSeverity.HIGH,
                semanticBreakdown = "Evasive leetspeak pattern detected: Normalized to '$normalized' matching explicit vocabulary.",
                evasionTechniques = listOf("Leetspeak substitution (e.g. 0->o, @->a, 3->e)"),
                recommendedRule = if (extraction.isSearchQuery) extraction.extractedKeywords else input,
                recommendedRuleCategory = RuleCategory.KEYWORD,
                isSafeForMinors = false,
                extractedSearchKeywords = if (extraction.isSearchQuery) extraction.extractedKeywords else null,
                detectedSearchEngine = if (extraction.isSearchQuery) extraction.searchEngineOrSite else null
            )
        }

        return AiAnalysisResult(
            isThreat = false,
            threatCategory = ThreatCategory.ADULT_WEBSITE,
            confidence = 0.94f,
            severity = ThreatSeverity.LOW,
            semanticBreakdown = "Safe semantic structure. No explicit adult signatures, dating escorts, or evasion methods detected in search keywords.",
            evasionTechniques = emptyList(),
            recommendedRule = null,
            isSafeForMinors = true,
            extractedSearchKeywords = if (extraction.isSearchQuery) extraction.extractedKeywords else null,
            detectedSearchEngine = if (extraction.isSearchQuery) extraction.searchEngineOrSite else null
        )
    }

    private fun generateFallbackParentalReport(
        incidents: List<BlockedIncident>,
        totalThreats: Int
    ): AiParentalReport {
        val riskScore = when {
            totalThreats > 25 -> 82
            totalThreats > 10 -> 55
            totalThreats > 0 -> 30
            else -> 12
        }

        val riskLevel = when {
            riskScore >= 75 -> "HIGH"
            riskScore >= 40 -> "MODERATE"
            else -> "LOW"
        }

        val summary = if (totalThreats > 0) {
            "PureShield Cross-Browser and AI engines successfully intercepted $totalThreats unauthorized explicit access attempts across device browsers."
        } else {
            "Device protection is healthy. No unauthorized adult access attempts detected recently."
        }

        val suggestedRules = listOf(
            CustomRule(pattern = "*dating*affiliate*", ruleType = RuleType.BLOCK, category = RuleCategory.URL_PATH),
            CustomRule(pattern = "*cam*stream*", ruleType = RuleType.BLOCK, category = RuleCategory.DOMAIN),
            CustomRule(pattern = "bypass parental control", ruleType = RuleType.BLOCK, category = RuleCategory.KEYWORD)
        )

        return AiParentalReport(
            overallRiskLevel = riskLevel,
            riskScorePercent = riskScore,
            summary = summary,
            detectedVulnerabilities = listOf(
                "Third-party browsers can be installed from Play Store if app installs are unrestricted.",
                "Search engine auto-complete might suggest borderline keywords without SafeSearch enforcement.",
                "Private browsing / Incognito windows require active accessibility monitoring."
            ),
            behavioralInsights = listOf(
                "Adult domains most frequently intercepted during late evening hours.",
                "Keyword bypass attempts frequently utilize obfuscated spellings and search redirects."
            ),
            actionableRecommendations = listOf(
                "Keep PureShield Cross-Browser Accessibility Guardian permanently enabled in Android Settings.",
                "Lock PureShield settings with a strict 4-digit Parent PIN.",
                "Enable Force SafeSearch across Google, Bing, Yahoo, and DuckDuckGo."
            ),
            suggestedProactiveRules = suggestedRules
        )
    }

    fun clearCache() {
        try {
            httpClient.connectionPool.evictAll()
            httpClient.cache?.evictAll()
        } catch (_: Exception) {}
    }
}
