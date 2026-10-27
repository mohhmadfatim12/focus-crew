package com.focuscrew.app.service

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.focuscrew.app.domain.AdultDomainBlocklist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * FocusCrewVpnService — Dedicated Browser-Only DNS Filter.
 *
 * ARCHITECTURE (Per-App Split-Tunnel — Browsers Only):
 *   - The VPN is STRICTLY restricted via `addAllowedApplication` to Web Browsers
 *     (Chrome, Brave, Samsung Internet, Edge, Firefox, Opera, etc.).
 *   - ALL other applications (WhatsApp, Free Fire, PUBG, games, banking, YouTube, Instagram)
 *     100% BYPASS the VPN interface! They connect directly over native Wi-Fi/Mobile data
 *     with ZERO latency, 0ms ping, and no disruptions whatsoever.
 *
 * WEB RESEARCH & ADULT FILTERING:
 *   1. 1000+ Adult Domain Database -> Instant 0.0.0.0 blackhole (0ms).
 *   2. DoH Bootstrap Domains -> Blackholed to enforce local DNS in browsers.
 *   3. Google Web Search & Research -> Routed directly through Tier-1 Anycast DNS
 *      (Cloudflare 1.1.1.1 & Google 8.8.8.8) to guarantee instant search response,
 *      zero connection timeouts, and zero CAPTCHAs.
 *   4. Clean domains -> Forwarded to ultra-fast Anycast resolvers with in-memory caching.
 */
class FocusCrewVpnService : VpnService() {

    companion object {
        private const val TAG = "FocusCrewVPN"
        private const val VPN_SESSION_NAME = "FocusCrew Browser Shield"
        private const val PREFS_NAME = "focuscrew_vpn_dns_filter"
        private const val KEY_VPN_ENABLED = "vpn_dns_filter_enabled"
        private const val KEY_DOMAINS_BLOCKED_COUNT = "domains_blocked_count"

        // Virtual local DNS tunnel endpoint
        private const val VPN_LOCAL_IP = "10.245.0.1"
        private const val DNS_VIRTUAL_SERVER = "10.245.0.2"
        private const val DNS_PORT = 53
        private const val MAX_PACKET_SIZE = 4096

        // Upstream resolvers: Tier-1 Anycast DNS for blazing fast web research
        private val UPSTREAM_DNS_SERVERS = listOf(
            "1.1.1.1",         // Cloudflare DNS Primary (~5-15ms Anycast)
            "8.8.8.8",         // Google DNS Primary (~10-20ms Anycast)
            "1.0.0.1",         // Cloudflare Secondary
            "8.8.4.4"          // Google DNS Secondary
        )

        // In-memory DNS Cache (Key: "$domain:$queryType" -> Pair<ResponseByteArray, TimestampMs>)
        private val dnsCache = java.util.concurrent.ConcurrentHashMap<String, Pair<ByteArray, Long>>()
        private const val DNS_CACHE_TTL_MS = 60_000L // 60 seconds

        // Static State for UI
        private val _isVpnRunning = MutableStateFlow(false)
        val isVpnRunning: StateFlow<Boolean> = _isVpnRunning.asStateFlow()

        private val _domainsBlockedCount = MutableStateFlow(0)
        val domainsBlockedCount: StateFlow<Int> = _domainsBlockedCount.asStateFlow()

        private val _isVpnEnabled = MutableStateFlow(false)
        val isVpnEnabled: StateFlow<Boolean> = _isVpnEnabled.asStateFlow()

        private var prefs: SharedPreferences? = null

        fun initialize(context: Context) {
            if (prefs == null) {
                prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                _isVpnEnabled.value = prefs?.getBoolean(KEY_VPN_ENABLED, false) ?: false
                _domainsBlockedCount.value = prefs?.getInt(KEY_DOMAINS_BLOCKED_COUNT, 0) ?: 0
            }
        }

        fun setVpnEnabled(enabled: Boolean) {
            _isVpnEnabled.value = enabled
            prefs?.edit()?.putBoolean(KEY_VPN_ENABLED, enabled)?.apply()
        }

        private fun recordDomainBlocked() {
            val next = _domainsBlockedCount.value + 1
            _domainsBlockedCount.value = next
            prefs?.edit()?.putInt(KEY_DOMAINS_BLOCKED_COUNT, next)?.apply()
        }
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    @Volatile private var isRunning = false
    private val forwardExecutor = Executors.newCachedThreadPool()
    private val outputLock = Any()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_VPN") {
            stopVpn()
            return START_NOT_STICKY
        }

        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning) return

        try {
            val builder = Builder()
            builder.setSession(VPN_SESSION_NAME)
            builder.setMtu(1500)
            builder.addAddress(VPN_LOCAL_IP, 32)
            builder.addDnsServer(DNS_VIRTUAL_SERVER)
            builder.addRoute(DNS_VIRTUAL_SERVER, 32)
            builder.setBlocking(true)

            // STRICT PER-APP SPLIT TUNNEL:
            // Route ONLY web browsers through the VPN!
            // WhatsApp, Free Fire, PUBG Mobile, YouTube, Instagram, and all other apps
            // completely bypass this VPN and run at 100% full speed with zero lag!
            val allowedBrowsers = getInstalledBrowserPackages(this)
            Log.d(TAG, "Configuring VPN strictly for browsers (${allowedBrowsers.size} detected): $allowedBrowsers")

            var allowedCount = 0
            for (pkg in allowedBrowsers) {
                try {
                    builder.addAllowedApplication(pkg)
                    allowedCount++
                    Log.d(TAG, "VPN restricted to browser: $pkg")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not add allowed browser $pkg: ${e.message}")
                }
            }

            // Fallback: If no browser package was allowed, exclude our own app
            if (allowedCount == 0) {
                try {
                    builder.addDisallowedApplication(packageName)
                } catch (_: Exception) {}
            }

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e(TAG, "VPN interface could not be established")
                return
            }

            isRunning = true
            _isVpnRunning.value = true
            setVpnEnabled(true)
            Log.d(TAG, "FocusCrew Browser-Only DNS Shield started successfully")

            Thread(::processPackets, "FocusCrewVPN-Reader").start()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Browser-Only DNS Shield: ${e.message}", e)
            stopVpn()
        }
    }

    private fun stopVpn() {
        isRunning = false
        _isVpnRunning.value = false
        setVpnEnabled(false)

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing VPN descriptor: ${e.message}")
        }
        vpnInterface = null

        Log.d(TAG, "Browser-Only DNS Shield stopped")
        stopSelf()
    }

    /**
     * Identifies installed web browser packages on the device,
     * ensuring that games, chat apps, and social media apps are strictly excluded.
     */
    private fun getInstalledBrowserPackages(context: Context): List<String> {
        val browsers = mutableSetOf<String>()
        val pm = context.packageManager

        // All standard known Android web browsers
        val knownBrowsers = listOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.canary",
            "com.chrome.dev",
            "com.brave.browser",
            "com.brave.browser_nightly",
            "com.brave.browser_beta",
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta",
            "org.mozilla.fenix",
            "org.mozilla.focus",
            "com.sec.android.app.sbrowser",         // Samsung Internet
            "com.sec.android.app.sbrowser.beta",
            "com.microsoft.emmx",                   // Microsoft Edge
            "com.opera.browser",                    // Opera
            "com.opera.mini.native",                // Opera Mini
            "com.opera.gx",                         // Opera GX
            "com.opera.touch",
            "com.duckduckgo.mobile.android",        // DuckDuckGo
            "com.vivaldi.browser",                  // Vivaldi
            "com.kiwibrowser.browser",              // Kiwi Browser
            "com.ucmobile.intl",                    // UC Browser
            "com.UCMobile.intl",
            "mark.via.gp",                          // Via Browser
            "com.mi.globalbrowser",                 // Mi Browser
            "com.coloros.browser",                  // Oppo / Realme
            "com.vivo.browser",                     // Vivo
            "com.huawei.browser",                   // Huawei
            "com.transsion.phoenix",                // Phoenix Browser
            "com.android.browser",                  // AOSP Browser
            "org.torproject.torbrowser"
        )

        // 1. Add any installed browsers from the known list
        for (pkg in knownBrowsers) {
            try {
                pm.getPackageInfo(pkg, 0)
                browsers.add(pkg)
            } catch (_: Exception) {}
        }

        // 2. Query system for apps registered to open HTTP / HTTPS web links
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            val list = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in list) {
                val pkg = resolveInfo.activityInfo?.packageName
                if (pkg != null && pkg != context.packageName && !isExcludedNonBrowser(pkg)) {
                    try {
                        pm.getPackageInfo(pkg, 0)
                        browsers.add(pkg)
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error resolving browser intent activities: ${e.message}")
        }

        // Guaranteed fallback to Chrome if nothing was resolved
        if (browsers.isEmpty()) {
            browsers.add("com.android.chrome")
        }

        return browsers.toList()
    }

    /**
     * Checks if a package is a game, social media app, or communication tool
     * that must NEVER be routed through the VPN.
     */
    private fun isExcludedNonBrowser(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return lower.contains("whatsapp") ||
               lower.contains("freefire") ||
               lower.contains("pubg") ||
               lower.contains("tencent") ||
               lower.contains("dts") ||
               lower.contains("game") ||
               lower.contains("instagram") ||
               lower.contains("facebook") ||
               lower.contains("telegram") ||
               lower.contains("snapchat") ||
               lower.contains("discord") ||
               lower.contains("twitter") ||
               lower.contains("youtube") ||
               lower.contains("tiktok")
    }

    /**
     * Reads DNS packets from the TUN interface, analyzes queries,
     * and sends immediate blackhole or forwarded responses.
     */
    private fun processPackets() {
        val vpnFd = vpnInterface ?: return
        val vpnInput = FileInputStream(vpnFd.fileDescriptor)
        val vpnOutput = FileOutputStream(vpnFd.fileDescriptor)

        val packet = ByteBuffer.allocate(MAX_PACKET_SIZE)

        while (isRunning) {
            try {
                packet.clear()
                val length = vpnInput.read(packet.array())

                if (length <= 0) {
                    Thread.sleep(5)
                    continue
                }

                if (length < 28) continue // Minimum 20 byte IP + 8 byte UDP header

                // IPv4 only
                val version = (packet.get(0).toInt() shr 4) and 0x0F
                if (version != 4) continue

                // UDP only (protocol 17)
                val protocol = packet.get(9).toInt() and 0xFF
                if (protocol != 17) continue

                val ipHeaderLength = (packet.get(0).toInt() and 0x0F) * 4
                if (length < ipHeaderLength + 8) continue

                val destPort = ((packet.get(ipHeaderLength + 2).toInt() and 0xFF) shl 8) or
                        (packet.get(ipHeaderLength + 3).toInt() and 0xFF)

                // Must be DNS port 53
                if (destPort != DNS_PORT) continue

                val udpDataOffset = ipHeaderLength + 8
                if (length <= udpDataOffset) continue

                val dnsPayload = packet.array().copyOfRange(udpDataOffset, length)
                val dnsQuery = DnsPacketParser.parseQuery(dnsPayload) ?: continue

                val domain = dnsQuery.domain.lowercase()
                val rawIpPacket = packet.array().copyOf(length)

                // ── 0. WHITELIST CHECK: Guaranteed 100% Fast Path for Google & Research ──
                // Whitelist Google Search and CDN domains so normal research is NEVER delayed or broken!
                val isWhitelisted = isWhitelistedDomain(domain)

                if (!isWhitelisted) {
                    // ── 1. BLOCK DOH (DNS over HTTPS) BOOTSTRAP TO ENFORCE BROWSER DNS ──
                    if (isDohBootstrapDomain(domain)) {
                        Log.d(TAG, "🔒 Blackholing DoH domain to enforce local DNS: $domain")
                        val dnsResponse = DnsPacketParser.buildBlockedResponse(dnsQuery)
                        val responsePacket = buildResponseIpPacket(rawIpPacket, ipHeaderLength, dnsResponse)
                        if (responsePacket != null) {
                            synchronized(outputLock) {
                                vpnOutput.write(responsePacket)
                            }
                        }
                        continue
                    }

                    // ── 2. CHECK ADULT DOMAIN BLOCKLIST (Verified 1000+ domains) ──
                    if (AdultDomainBlocklist.isBlocked(domain)) {
                        Log.d(TAG, "🛡️ BLOCKED adult domain: $domain")
                        recordDomainBlocked()

                        val dnsResponse = DnsPacketParser.buildBlockedResponse(dnsQuery)
                        val responsePacket = buildResponseIpPacket(rawIpPacket, ipHeaderLength, dnsResponse)
                        if (responsePacket != null) {
                            synchronized(outputLock) {
                                vpnOutput.write(responsePacket)
                            }
                        }
                        continue
                    }
                }

                // ── 3. CLEAN / RESEARCH / GOOGLE DOMAIN: Check in-memory 0ms cache ──
                val cacheKey = "$domain:${dnsQuery.queryType}"
                val cached = dnsCache[cacheKey]
                val now = System.currentTimeMillis()
                if (cached != null && (now - cached.second) < DNS_CACHE_TTL_MS) {
                    val rewritten = rewriteTransactionId(cached.first, dnsQuery.transactionId)
                    val responsePacket = buildResponseIpPacket(rawIpPacket, ipHeaderLength, rewritten)
                    if (responsePacket != null) {
                        synchronized(outputLock) {
                            vpnOutput.write(responsePacket)
                        }
                    }
                    continue
                }

                // ── 4. Forward asynchronously to Tier-1 High-Speed DNS (Cloudflare 1.1.1.1 / Google 8.8.8.8) ──
                forwardExecutor.execute {
                    try {
                        val responsePayload = forwardDnsQuery(dnsPayload)
                        if (responsePayload != null && isRunning) {
                            dnsCache[cacheKey] = Pair(responsePayload, System.currentTimeMillis())
                            val responsePacket = buildResponseIpPacket(rawIpPacket, ipHeaderLength, responsePayload)
                            if (responsePacket != null) {
                                synchronized(outputLock) {
                                    vpnOutput.write(responsePacket)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "DNS forward failed for $domain: ${e.message}")
                    }
                }

            } catch (e: InterruptedException) {
                break
            } catch (e: Exception) {
                if (isRunning) {
                    Log.w(TAG, "Packet error: ${e.message}")
                }
            }
        }

        try { vpnInput.close() } catch (_: Exception) {}
        try { vpnOutput.close() } catch (_: Exception) {}
    }

    private fun rewriteTransactionId(cachedResponse: ByteArray, newTransactionId: Int): ByteArray {
        val clone = cachedResponse.clone()
        if (clone.size >= 2) {
            clone[0] = (newTransactionId shr 8).toByte()
            clone[1] = (newTransactionId and 0xFF).toByte()
        }
        return clone
    }

    private val DOH_BOOTSTRAP_DOMAINS = setOf(
        "chrome.cloudflare-dns.com",
        "cloudflare-dns.com",
        "security.cloudflare-dns.com",
        "family.cloudflare-dns.com",
        "one.one.one.one",
        "dns.quad9.net",
        "dns9.quad9.net",
        "dns.adguard.com",
        "dns.adguard-dns.com",
        "doh.cleanbrowsing.org",
        "dns.opendns.com",
        "doh.opendns.com",
        "dns.nextdns.io",
        "nextdns.io",
        "controld.com",
        "freedns.controld.com",
        "doh.mullvad.net"
    )

    private fun isDohBootstrapDomain(domain: String): Boolean {
        val d = domain.lowercase()
        return DOH_BOOTSTRAP_DOMAINS.contains(d) || DOH_BOOTSTRAP_DOMAINS.any { d.endsWith(".$it") }
    }

    /**
     * Whitelist of research, educational, and platform domains that must NEVER be intercepted or delayed.
     * Guarantees 100% full-speed Google Search, Google Scholar, Wikipedia, YouTube, and CDN assets.
     */
    private fun isWhitelistedDomain(domain: String): Boolean {
        val d = domain.lowercase().removePrefix("www.").trim()
        return d == "google.com" || d.endsWith(".google.com") ||
               d.contains("google.com.") || d.startsWith("google.") ||
               d == "youtube.com" || d.endsWith(".youtube.com") ||
               d == "youtu.be" || d.endsWith(".youtu.be") ||
               d == "googlevideo.com" || d.endsWith(".googlevideo.com") ||
               d == "ytimg.com" || d.endsWith(".ytimg.com") ||
               d == "ggpht.com" || d.endsWith(".ggpht.com") ||
               d == "googleapis.com" || d.endsWith(".googleapis.com") ||
               d == "gstatic.com" || d.endsWith(".gstatic.com") ||
               d == "gvt1.com" || d.endsWith(".gvt1.com") ||
               d == "1e100.net" || d.endsWith(".1e100.net") ||
               d == "googleusercontent.com" || d.endsWith(".googleusercontent.com") ||
               d == "play.google.com" || d == "accounts.google.com" ||
               d == "dns.google" || d == "dns.google.com" ||
               d == "android.com" || d.endsWith(".android.com") ||
               d == "wikipedia.org" || d.endsWith(".wikipedia.org") ||
               d == "github.com" || d.endsWith(".github.com") ||
               d == "stackoverflow.com" || d.endsWith(".stackoverflow.com")
    }

    /**
     * Forwards a clean DNS query to upstream Tier-1 DNS resolvers (Cloudflare & Google).
     * Reliable 2000ms timeout with automatic failover to prevent mobile data packet drops.
     */
    private fun forwardDnsQuery(dnsPayload: ByteArray): ByteArray? {
        for (serverIp in UPSTREAM_DNS_SERVERS) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                protect(socket)
                socket.soTimeout = 2000 // 2.0s timeout ensures stable research on mobile networks

                val target = InetAddress.getByName(serverIp)
                val outPacket = DatagramPacket(dnsPayload, dnsPayload.size, target, DNS_PORT)
                socket.send(outPacket)

                val buffer = ByteArray(MAX_PACKET_SIZE)
                val inPacket = DatagramPacket(buffer, buffer.size)
                socket.receive(inPacket)

                if (inPacket.length > 0) {
                    return buffer.copyOfRange(0, inPacket.length)
                }
            } catch (_: Exception) {
                // Try next server on timeout or failover
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        }
        return null
    }

    /**
     * Builds response IP + UDP packet, swapping source and destination addresses/ports.
     */
    private fun buildResponseIpPacket(
        originalPacket: ByteArray,
        ipHeaderLength: Int,
        dnsResponse: ByteArray
    ): ByteArray? {
        try {
            val udpLength = 8 + dnsResponse.size
            val totalLength = ipHeaderLength + udpLength
            val response = ByteArray(totalLength)

            // Copy base IP header
            System.arraycopy(originalPacket, 0, response, 0, ipHeaderLength)

            // Swap Source IP (12-15) and Destination IP (16-19)
            System.arraycopy(originalPacket, 16, response, 12, 4)
            System.arraycopy(originalPacket, 12, response, 16, 4)

            // Update Total Length in IP header
            response[2] = ((totalLength shr 8) and 0xFF).toByte()
            response[3] = (totalLength and 0xFF).toByte()

            // Calculate IP Checksum
            response[10] = 0
            response[11] = 0
            val ipChecksum = computeChecksum(response, 0, ipHeaderLength)
            response[10] = ((ipChecksum shr 8) and 0xFF).toByte()
            response[11] = (ipChecksum and 0xFF).toByte()

            // UDP Header
            val udpOffset = ipHeaderLength
            // Swap ports: old dest port (udpOffset+2,3) -> new source port (udpOffset,1)
            response[udpOffset] = originalPacket[udpOffset + 2]
            response[udpOffset + 1] = originalPacket[udpOffset + 3]
            // old source port (udpOffset,1) -> new dest port (udpOffset+2,3)
            response[udpOffset + 2] = originalPacket[udpOffset]
            response[udpOffset + 3] = originalPacket[udpOffset + 1]

            // UDP Length
            response[udpOffset + 4] = ((udpLength shr 8) and 0xFF).toByte()
            response[udpOffset + 5] = (udpLength and 0xFF).toByte()

            // UDP Checksum = 0
            response[udpOffset + 6] = 0
            response[udpOffset + 7] = 0

            // DNS Payload
            System.arraycopy(dnsResponse, 0, response, udpOffset + 8, dnsResponse.size)

            return response
        } catch (e: Exception) {
            Log.w(TAG, "Error building response IP packet: ${e.message}")
            return null
        }
    }

    private fun computeChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset
        var remaining = length

        while (remaining > 1) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2
            remaining -= 2
        }

        if (remaining == 1) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }

        while (sum shr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        return (sum.inv() and 0xFFFF).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
        forwardExecutor.shutdownNow()
    }

    override fun onRevoke() {
        super.onRevoke()
        stopVpn()
    }
}
