package site.tiedan.module

import site.tiedan.data.DomainWhiteListData
import java.net.InetAddress
import java.net.URI

object UrlWhitelistValidator {

    /**
     * @param url 用户传入的完整 URL
     */
    fun isAllowed(url: String): Boolean {
        val uri = try {
            URI(url)
        } catch (_: Exception) {
            return false
        }

        // 只允许 http / https
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") {
            return false
        }
        val host = uri.host?.lowercase() ?: return false
        // 禁止 IP / localhost / 内网
        if (isUnsafeHost(host)) {
            return false
        }
        // 白名单匹配
        return DomainWhiteListData.WhiteList.any { rule ->
            matchDomain(host, rule.lowercase())
        }
    }

    /**
     * 判断是否是危险主机
     */
    fun isUnsafeHost(host: String): Boolean {
        if (host.equals("localhost", ignoreCase = true)) return true
        val ip = try {
            InetAddress.getByName(host)
        } catch (_: Exception) {
            return false // 无法解析域名，放行交给白名单
        }
        return ip.isAnyLocalAddress || ip.isLoopbackAddress || ip.isLinkLocalAddress || ip.isSiteLocalAddress
    }

    /**
     * 域名匹配规则：
     *  - example.com      → 只匹配 example.com
     *  - sub.example.com  → 只匹配 sub.example.com
     *  - *.example.com    → 匹配 a.example.com / b.c.example.com
     */
    private fun matchDomain(host: String, rule: String): Boolean {
        return if (rule.startsWith("*.")) {
            val baseDomain = rule.removePrefix("*.")
            host == baseDomain || host.endsWith(".$baseDomain")
        } else {
            host == rule
        }
    }
}
