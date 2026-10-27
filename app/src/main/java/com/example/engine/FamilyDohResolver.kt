package com.example.engine

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.example.data.model.ThreatCategory
import com.example.data.model.ThreatSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class FamilyDohProvider(
    val displayName: String,
    val dohUrl: String,
    val privateDnsHostname: String,
    val description: String
) {
    CLOUDFLARE_FAMILY(
        displayName = "Cloudflare 1.1.1.3 Family",
        dohUrl = "https://family.cloudflare-dns.com/dns-query",
        privateDnsHostname = "family.cloudflare-dns.com",
        description = "Blocks malware, adult content, explicit domains, and zero-day threat hosts."
    ),
    CLEANBROWSING_FAMILY(
        displayName = "CleanBrowsing Family",
        dohUrl = "https://doh.cleanbrowsing.org/doh/family-filter/",
        privateDnsHostname = "family-filter-dns.cleanbrowsing.org",
        description = "Strict adult content filter, safe search enforcer, and malicious domain barrier."
    ),
    ADGUARD_FAMILY(
        displayName = "AdGuard Family Shield",
        dohUrl = "https://dns.adguard.com/dns-query",
        privateDnsHostname = "dns-family.adguard.com",
        description = "Blocks adult websites, aggressive trackers, and explicit advertising networks."
    ),
    OPENDNS_FAMILY(
        displayName = "OpenDNS FamilyShield",
        dohUrl = "https://doh.familyshield.opendns.com/dns-query",
        privateDnsHostname = "familyshield.opendns.com",
        description = "Cisco OpenDNS adult website blocking and parental protection."
    )
}

data class DohTestResult(
    val domain: String,
    val provider: FamilyDohProvider,
    val isBlocked: Boolean,
    val responseIps: List<String>,
    val latencyMs: Long,
    val statusMessage: String
)

object FamilyDohResolver {

    private const val TAG = "FamilyDohResolver"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .build()

    /**
     * Resolves a domain via standard DNS-over-HTTPS JSON API (RFC 8427 / Cloudflare & Google JSON format).
     * Returns true if the domain is blocked (resolved to 0.0.0.0, 127.0.0.1, or NXDOMAIN).
     */
    suspend fun checkDomainWithDoh(
        domain: String,
        provider: FamilyDohProvider = FamilyDohProvider.CLOUDFLARE_FAMILY
    ): EvaluationResult = withContext(Dispatchers.IO) {
        val cleanDomain = sanitizeDomain(domain)
        if (cleanDomain.isBlank() || cleanDomain.contains("/")) {
            return@withContext EvaluationResult(isBlocked = false)
        }

        try {
            val urlBuilder = provider.dohUrl.toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("name", cleanDomain)
                ?.addQueryParameter("type", "A")
                ?: return@withContext EvaluationResult(isBlocked = false)

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("Accept", "application/dns-json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext EvaluationResult(isBlocked = false)
            }

            val body = response.body?.string() ?: return@withContext EvaluationResult(isBlocked = false)
            val json = JSONObject(body)
            val status = json.optInt("Status", -1)

            // DNS Status: 0 = NOERROR, 3 = NXDOMAIN
            val answers = json.optJSONArray("Answer")
            val resolvedIps = mutableListOf<String>()

            if (answers != null) {
                for (i in 0 until answers.length()) {
                    val answerObj = answers.optJSONObject(i)
                    val data = answerObj?.optString("data", "") ?: ""
                    if (data.isNotBlank()) {
                        resolvedIps.add(data)
                    }
                }
            }

            val isBlockedBySinkhole = resolvedIps.any { ip ->
                ip == "0.0.0.0" || ip == "127.0.0.1" || ip == "::" || ip == "::1"
            }

            val isNxDomainBlock = (status == 3 && resolvedIps.isEmpty())

            if (isBlockedBySinkhole || isNxDomainBlock) {
                return@withContext EvaluationResult(
                    isBlocked = true,
                    category = ThreatCategory.ADULT_WEBSITE,
                    severity = ThreatSeverity.HIGH,
                    triggerReason = "Blocked by Dual-Layer Family DoH Resolver (${provider.displayName})",
                    matchedTerm = cleanDomain,
                    sanitizedUrl = domain
                )
            }

            EvaluationResult(isBlocked = false)
        } catch (e: Exception) {
            Log.w(TAG, "DoH lookup failed or timed out for $cleanDomain: ${e.message}")
            EvaluationResult(isBlocked = false)
        }
    }

    /**
     * Diagnostic testing utility to verify DNS-over-HTTPS filtering against any domain with latency metrics.
     */
    suspend fun testDohLookup(
        domain: String,
        provider: FamilyDohProvider
    ): DohTestResult = withContext(Dispatchers.IO) {
        val cleanDomain = sanitizeDomain(domain)
        val startTime = System.currentTimeMillis()

        try {
            val urlBuilder = provider.dohUrl.toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("name", cleanDomain)
                ?.addQueryParameter("type", "A")
                ?: return@withContext DohTestResult(
                    domain = cleanDomain,
                    provider = provider,
                    isBlocked = false,
                    responseIps = emptyList(),
                    latencyMs = 0,
                    statusMessage = "Invalid DoH URL configuration"
                )

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("Accept", "application/dns-json")
                .build()

            val response = httpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext DohTestResult(
                    domain = cleanDomain,
                    provider = provider,
                    isBlocked = false,
                    responseIps = emptyList(),
                    latencyMs = latency,
                    statusMessage = "HTTP Error ${response.code}"
                )
            }

            val json = JSONObject(body)
            val status = json.optInt("Status", -1)
            val answers = json.optJSONArray("Answer")
            val resolvedIps = mutableListOf<String>()

            if (answers != null) {
                for (i in 0 until answers.length()) {
                    val answerObj = answers.optJSONObject(i)
                    val data = answerObj?.optString("data", "") ?: ""
                    if (data.isNotBlank()) resolvedIps.add(data)
                }
            }

            val isSinkholed = resolvedIps.any { it == "0.0.0.0" || it == "127.0.0.1" }
            val isBlocked = isSinkholed || (status == 3 && resolvedIps.isEmpty())

            val msg = when {
                isSinkholed -> "BLOCKED: Sinkholed to 0.0.0.0 (Adult Content Blocked)"
                isBlocked -> "BLOCKED: Domain NXDOMAIN (Filtered by Family Shield)"
                resolvedIps.isNotEmpty() -> "SAFE: Resolved to ${resolvedIps.joinToString(", ")}"
                else -> "SAFE: No blocking record returned (Status $status)"
            }

            DohTestResult(
                domain = cleanDomain,
                provider = provider,
                isBlocked = isBlocked,
                responseIps = resolvedIps,
                latencyMs = latency,
                statusMessage = msg
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            DohTestResult(
                domain = cleanDomain,
                provider = provider,
                isBlocked = false,
                responseIps = emptyList(),
                latencyMs = latency,
                statusMessage = "Connection Exception: ${e.localizedMessage ?: "Unknown"}"
            )
        }
    }

    fun sanitizeDomain(raw: String): String {
        var d = raw.trim().lowercase()
        if (d.startsWith("http://")) d = d.removePrefix("http://")
        if (d.startsWith("https://")) d = d.removePrefix("https://")
        if (d.startsWith("www.")) d = d.removePrefix("www.")
        if (d.contains("/")) d = d.substringBefore("/")
        if (d.contains("?")) d = d.substringBefore("?")
        if (d.contains(":")) d = d.substringBefore(":")
        return d.trim()
    }

    /**
     * Opens Android System Network & Internet / Private DNS Settings
     */
    fun openPrivateDnsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallback)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open settings", e)
            }
        }
    }
}
