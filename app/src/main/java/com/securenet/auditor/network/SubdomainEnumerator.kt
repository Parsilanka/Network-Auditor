package com.securenet.auditor.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

class SubdomainEnumerator {

    // Built-in wordlist of 200 most common subdomains
    val commonSubdomains = listOf(
        "www", "mail", "ftp", "smtp", "pop", "imap",
        "vpn", "remote", "dev", "staging", "test",
        "api", "cdn", "static", "assets", "img",
        "images", "video", "media", "download",
        "upload", "files", "docs", "wiki", "blog",
        "news", "shop", "store", "cart", "payment",
        "secure", "ssl", "portal", "admin", "panel",
        "dashboard", "manage", "management", "control",
        "cpanel", "webmail", "email", "autodiscover",
        "autoconfig", "mx", "ns1", "ns2", "ns3",
        "dns", "dns1", "dns2", "whois", "rdap",
        "app", "apps", "mobile", "m", "wap", "mobi",
        "beta", "alpha", "preview", "pre", "demo",
        "sandbox", "qa", "uat", "prod", "production",
        "internal", "intranet", "extranet", "corp",
        "office", "hr", "finance", "accounting",
        "git", "gitlab", "github", "bitbucket", "svn",
        "jenkins", "ci", "cd", "build", "deploy",
        "jira", "confluence", "wiki", "kb", "support",
        "help", "helpdesk", "tickets", "status",
        "monitor", "monitoring", "grafana", "kibana",
        "elastic", "splunk", "logs", "syslog",
        "db", "database", "mysql", "postgres", "mongo",
        "redis", "cache", "memcache", "rabbit", "kafka",
        "docker", "k8s", "kubernetes", "rancher",
        "proxy", "gateway", "lb", "loadbalancer",
        "firewall", "waf", "edge", "relay",
        "smtp1", "smtp2", "pop3", "imap4", "mx1", "mx2",
        "marketing", "analytics", "track", "tracking",
        "ad", "ads", "adserver", "pixel",
        "search", "elastic", "solr",
        "auth", "login", "sso", "oauth", "id",
        "accounts", "account", "user", "users",
        "forum", "community", "social",
        "chat", "im", "messaging", "slack",
        "video", "stream", "streaming", "live",
        "old", "new", "v1", "v2", "v3",
        "legacy", "archive", "backup"
    )

    data class SubdomainResult(
        val subdomain: String,
        val fullDomain: String,
        val ipAddresses: List<String>,
        val isAlive: Boolean,
        val responseTimeMs: Long,
        val httpStatus: Int?,
        val title: String?
    )

    fun enumerate(
        domain: String,
        onProgress: (Int, Int, String) -> Unit,
        onFound: (SubdomainResult) -> Unit
    ): Flow<SubdomainResult> = flow {
        val total = commonSubdomains.size
        var checked = 0

        coroutineScope {
            val semaphore = Semaphore(20)

            commonSubdomains.map { prefix ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        val fullDomain = "$prefix.$domain"
                        val start = System.currentTimeMillis()
                        try {
                            val addresses = InetAddress
                                .getAllByName(fullDomain)
                            val ips = addresses.map {
                                it.hostAddress ?: "" }
                            val responseTime =
                                System.currentTimeMillis() -
                                start

                            // Try HTTP/HTTPS to get status
                            var httpStatus: Int? = null
                            var pageTitle: String? = null
                            try {
                                val conn = URL(
                                    "https://$fullDomain")
                                    .openConnection()
                                    as HttpURLConnection
                                conn.connectTimeout = 2000
                                conn.readTimeout = 2000
                                conn.connect()
                                httpStatus = conn.responseCode
                                conn.disconnect()
                            } catch (e: Exception) {
                                try {
                                    val conn = URL(
                                        "http://$fullDomain")
                                        .openConnection()
                                        as HttpURLConnection
                                    conn.connectTimeout = 2000
                                    conn.readTimeout = 2000
                                    conn.connect()
                                    httpStatus =
                                        conn.responseCode
                                    conn.disconnect()
                                } catch (e2: Exception) {}
                            }

                            val result = SubdomainResult(
                                subdomain = prefix,
                                fullDomain = fullDomain,
                                ipAddresses = ips,
                                isAlive = true,
                                responseTimeMs = responseTime,
                                httpStatus = httpStatus,
                                title = pageTitle
                            )
                            onFound(result)
                            result
                        } catch (e: Exception) { null } finally {
                            checked++
                            onProgress(checked, total, fullDomain)
                        }
                    }
                }
            }.awaitAll().filterNotNull().forEach { result ->
                emit(result)
            }
        }
    }.flowOn(Dispatchers.IO)
}
