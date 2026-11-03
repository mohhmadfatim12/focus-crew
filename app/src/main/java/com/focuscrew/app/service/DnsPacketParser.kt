package com.focuscrew.app.service

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * DnsPacketParser — Lightweight DNS packet parser & response builder.
 *
 * Parses DNS query packets to extract the queried domain name,
 * and builds DNS response packets (NXDOMAIN or blocked-redirect).
 *
 * 100% Offline. No Android framework dependencies.
 */
object DnsPacketParser {

    /**
     * Represents a parsed DNS query.
     */
    data class DnsQuery(
        val transactionId: Int,
        val domain: String,
        val queryType: Int,       // 1 = A (IPv4), 28 = AAAA (IPv6)
        val rawPacket: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DnsQuery) return false
            return transactionId == other.transactionId && domain == other.domain
        }

        override fun hashCode(): Int {
            return 31 * transactionId + domain.hashCode()
        }
    }

    /**
     * Parse a raw DNS query packet and extract the domain name.
     * Returns null if the packet is malformed or not a standard query.
     */
    fun parseQuery(packet: ByteArray): DnsQuery? {
        if (packet.size < 12) return null

        val buffer = ByteBuffer.wrap(packet)

        // Transaction ID (2 bytes)
        val transactionId = buffer.short.toInt() and 0xFFFF

        // Flags (2 bytes) — check it's a standard query (QR=0, OPCODE=0)
        val flags = buffer.short.toInt() and 0xFFFF
        val isQuery = (flags and 0x8000) == 0      // QR bit = 0
        val opcode = (flags shr 11) and 0x0F       // OPCODE = 0
        if (!isQuery || opcode != 0) return null

        // QDCOUNT (2 bytes)
        val qdCount = buffer.short.toInt() and 0xFFFF
        if (qdCount < 1) return null

        // Skip ANCOUNT, NSCOUNT, ARCOUNT (6 bytes)
        buffer.position(buffer.position() + 6)

        // Parse domain name from Question section
        val domain = parseDomainName(buffer) ?: return null

        // Query Type (2 bytes)
        val queryType = if (buffer.remaining() >= 2) {
            buffer.short.toInt() and 0xFFFF
        } else {
            1 // Default to A record
        }

        return DnsQuery(
            transactionId = transactionId,
            domain = domain,
            queryType = queryType,
            rawPacket = packet
        )
    }

    /**
     * Parse a DNS domain name from the packet buffer.
     * Handles label encoding (length-prefixed segments).
     */
    private fun parseDomainName(buffer: ByteBuffer): String? {
        val parts = mutableListOf<String>()
        var totalLength = 0

        while (buffer.hasRemaining()) {
            val labelLen = buffer.get().toInt() and 0xFF

            // End of name
            if (labelLen == 0) break

            // Compression pointer (not expected in queries, but handle gracefully)
            if ((labelLen and 0xC0) == 0xC0) {
                // Skip the second byte of the pointer
                if (buffer.hasRemaining()) buffer.get()
                break
            }

            // Safety: prevent absurdly long labels
            if (labelLen > 63 || !buffer.hasRemaining() || buffer.remaining() < labelLen) return null
            totalLength += labelLen + 1
            if (totalLength > 253) return null // Max domain length

            val label = ByteArray(labelLen)
            buffer.get(label)
            parts.add(String(label, Charsets.US_ASCII))
        }

        return if (parts.isNotEmpty()) parts.joinToString(".") else null
    }

    /**
     * Build a DNS NXDOMAIN (Name Error) response for a blocked domain.
     * This tells the browser "this domain doesn't exist".
     */
    fun buildNxdomainResponse(query: DnsQuery): ByteArray {
        val response = ByteArrayOutputStream()

        // Transaction ID (echo from query)
        response.write((query.transactionId shr 8) and 0xFF)
        response.write(query.transactionId and 0xFF)

        // Flags: QR=1 (response), OPCODE=0, AA=1 (authoritative), TC=0, RD=1, RA=1, RCODE=3 (NXDOMAIN)
        response.write(0x81) // 1000 0001
        response.write(0x83) // 1000 0011

        // QDCOUNT = 1
        response.write(0x00)
        response.write(0x01)

        // ANCOUNT = 0
        response.write(0x00)
        response.write(0x00)

        // NSCOUNT = 0
        response.write(0x00)
        response.write(0x00)

        // ARCOUNT = 0
        response.write(0x00)
        response.write(0x00)

        // Copy original question section (domain name + type + class)
        val questionStart = 12 // DNS header is always 12 bytes
        if (query.rawPacket.size > questionStart) {
            // Walk through the question to find its end
            var pos = questionStart
            while (pos < query.rawPacket.size) {
                val len = query.rawPacket[pos].toInt() and 0xFF
                if (len == 0) {
                    pos++ // skip the zero terminator
                    break
                }
                pos += len + 1
            }
            // Add QTYPE (2 bytes) + QCLASS (2 bytes)
            pos += 4

            val questionSection = query.rawPacket.copyOfRange(questionStart, minOf(pos, query.rawPacket.size))
            response.write(questionSection)
        }

        return response.toByteArray()
    }

    /**
     * Build an A-record response for a domain, resolving it to a custom IPv4 address.
     * Used for 0.0.0.0 blackholing and SafeSearch VIP redirection.
     */
    fun buildIpResponse(query: DnsQuery, ipBytes: ByteArray): ByteArray {
        val response = ByteArrayOutputStream()

        // Transaction ID (echoed from query)
        response.write((query.transactionId shr 8) and 0xFF)
        response.write(query.transactionId and 0xFF)

        // Flags: QR=1 (response), AA=1 (authoritative), RD=1, RA=1, RCODE=0 (No Error)
        response.write(0x81)
        response.write(0x80)

        // QDCOUNT = 1, ANCOUNT = 1
        response.write(0x00)
        response.write(0x01)
        response.write(0x00)
        response.write(0x01)

        // NSCOUNT = 0, ARCOUNT = 0
        response.write(0x00)
        response.write(0x00)
        response.write(0x00)
        response.write(0x00)

        // Copy original question section
        val questionStart = 12
        if (query.rawPacket.size > questionStart) {
            var pos = questionStart
            while (pos < query.rawPacket.size) {
                val len = query.rawPacket[pos].toInt() and 0xFF
                if (len == 0) {
                    pos++
                    break
                }
                pos += len + 1
            }
            pos += 4 // QTYPE + QCLASS
            val questionEnd = minOf(pos, query.rawPacket.size)
            response.write(query.rawPacket.copyOfRange(questionStart, questionEnd))
        }

        // Answer section: pointer to domain name in question
        response.write(0xC0) // Compression pointer
        response.write(0x0C) // Offset 12 (start of question name)

        // TYPE = A (0x0001)
        response.write(0x00)
        response.write(0x01)

        // CLASS = IN (0x0001)
        response.write(0x00)
        response.write(0x01)

        // TTL = 300 seconds
        response.write(0x00)
        response.write(0x00)
        response.write(0x01)
        response.write(0x2C)

        // RDLENGTH = 4 (IPv4 address)
        response.write(0x00)
        response.write(0x04)

        // RDATA (4 bytes IP)
        response.write(ipBytes[0].toInt() and 0xFF)
        response.write(ipBytes[1].toInt() and 0xFF)
        response.write(ipBytes[2].toInt() and 0xFF)
        response.write(ipBytes[3].toInt() and 0xFF)

        return response.toByteArray()
    }

    // IP Constants
    val BLACKHOLE_IP = byteArrayOf(0, 0, 0, 0)
    val BLACKHOLE_IPV6 = ByteArray(16)
    // Google SafeSearch VIP: 216.239.38.120 (forcesafesearch.google.com IPv4)
    val GOOGLE_SAFESEARCH_IP = byteArrayOf(216.toByte(), 239.toByte(), 38.toByte(), 120.toByte())
    // Google SafeSearch VIP IPv6: 2001:4860:4806::78 (forcesafesearch.google.com IPv6)
    val GOOGLE_SAFESEARCH_IPV6 = byteArrayOf(
        0x20.toByte(), 0x01.toByte(), 0x48.toByte(), 0x60.toByte(),
        0x48.toByte(), 0x06.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x78.toByte()
    )
    // Bing Strict SafeSearch VIP: 204.79.197.220 (strict.bing.com)
    val BING_SAFESEARCH_IP = byteArrayOf(204.toByte(), 79.toByte(), 197.toByte(), 220.toByte())
    // DuckDuckGo SafeSearch VIP: 52.142.124.215 (safe.duckduckgo.com)
    val DUCKDUCKGO_SAFESEARCH_IP = byteArrayOf(52.toByte(), 142.toByte(), 124.toByte(), 215.toByte())

    /**
     * Build an AAAA-record response for a domain, resolving it to a custom IPv6 address.
     * Used for IPv6 Google SafeSearch VIP (2001:4860:4806::78) and IPv6 blackholing.
     */
    fun buildIpv6Response(query: DnsQuery, ipv6Bytes: ByteArray): ByteArray {
        val response = ByteArrayOutputStream()

        // Transaction ID (echoed from query)
        response.write((query.transactionId shr 8) and 0xFF)
        response.write(query.transactionId and 0xFF)

        // Flags: QR=1 (response), AA=1 (authoritative), RD=1, RA=1, RCODE=0 (No Error)
        response.write(0x81)
        response.write(0x80)

        // QDCOUNT = 1, ANCOUNT = 1
        response.write(0x00)
        response.write(0x01)
        response.write(0x00)
        response.write(0x01)

        // NSCOUNT = 0, ARCOUNT = 0
        response.write(0x00)
        response.write(0x00)
        response.write(0x00)
        response.write(0x00)

        // Copy original question section
        val questionStart = 12
        if (query.rawPacket.size > questionStart) {
            var pos = questionStart
            while (pos < query.rawPacket.size) {
                val len = query.rawPacket[pos].toInt() and 0xFF
                if (len == 0) {
                    pos++
                    break
                }
                pos += len + 1
            }
            pos += 4 // QTYPE + QCLASS
            val questionEnd = minOf(pos, query.rawPacket.size)
            response.write(query.rawPacket.copyOfRange(questionStart, questionEnd))
        }

        // Answer section: pointer to domain name in question
        response.write(0xC0) // Compression pointer
        response.write(0x0C) // Offset 12 (start of question name)

        // TYPE = AAAA (0x001C)
        response.write(0x00)
        response.write(0x1C)

        // CLASS = IN (0x0001)
        response.write(0x00)
        response.write(0x01)

        // TTL = 300 seconds
        response.write(0x00)
        response.write(0x00)
        response.write(0x01)
        response.write(0x2C)

        // RDLENGTH = 16 (IPv6 address)
        response.write(0x00)
        response.write(0x10)

        // RDATA (16 bytes IPv6)
        response.write(ipv6Bytes)

        return response.toByteArray()
    }

    fun buildBlockedResponse(query: DnsQuery): ByteArray {
        return if (query.queryType == 28) {
            buildIpv6Response(query, BLACKHOLE_IPV6)
        } else {
            buildIpResponse(query, BLACKHOLE_IP)
        }
    }

    fun buildGoogleSafeSearchResponse(query: DnsQuery): ByteArray {
        return if (query.queryType == 28) {
            buildIpv6Response(query, GOOGLE_SAFESEARCH_IPV6)
        } else {
            buildIpResponse(query, GOOGLE_SAFESEARCH_IP)
        }
    }

    fun buildBingSafeSearchResponse(query: DnsQuery): ByteArray {
        return if (query.queryType == 28) {
            buildNxdomainResponse(query)
        } else {
            buildIpResponse(query, BING_SAFESEARCH_IP)
        }
    }

    fun buildDuckDuckGoSafeSearchResponse(query: DnsQuery): ByteArray {
        return if (query.queryType == 28) {
            buildNxdomainResponse(query)
        } else {
            buildIpResponse(query, DUCKDUCKGO_SAFESEARCH_IP)
        }
    }
}
