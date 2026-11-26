package com.inkqilin.ledger.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.w3c.dom.Element
import java.io.StringReader
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

/** The endpoint identifies an existing WebDAV collection; prefix is app-owned. */
data class WebDavConfig(
    val endpoint: String = "",
    val username: String = "",
    val password: String = "",
    val prefix: String = "backups/v1"
) {
    val normalizedEndpoint: String get() = endpoint.trim().trimEnd('/')
    val isConfigured: Boolean
        get() = username.isNotBlank() && password.isNotEmpty() && runCatching {
            val uri = URI(normalizedEndpoint)
            val localHttp = uri.scheme == "http" && uri.host?.removePrefix("[")?.removeSuffix("]") in setOf("localhost", "127.0.0.1", "::1")
            (uri.scheme == "https" || localHttp) && !uri.host.isNullOrBlank() &&
                uri.userInfo == null && uri.query == null && uri.fragment == null &&
                prefix.split('/').all { it != "." && it != ".." && !it.contains('\\') && it.none { ch -> ch.code < 32 } }
        }.getOrDefault(false)
    val displayEndpoint: String
        get() = normalizedEndpoint.toHttpUrlOrNull()?.let { url ->
            "${url.scheme}://${url.host}:${url.port}"
        }.orEmpty()

    override fun toString(): String = "WebDavConfig(endpoint=$endpoint, username=$username, password=***, prefix=$prefix)"
}

data class WebDavObjectMeta(val path: String, val size: Long, val lastModified: String)

object WebDavClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private const val PROPFIND_BODY = "<d:propfind xmlns:d=\"DAV:\"><d:prop><d:getcontentlength/><d:getlastmodified/><d:resourcetype/></d:prop></d:propfind>"

    private fun prefixSegments(config: WebDavConfig): List<String> {
        val segments = config.prefix.trim().ifBlank { "backups/v1" }.trim('/').split('/')
        require(segments.all { segment ->
            segment.isNotBlank() && segment != "." && segment != ".." &&
                !segment.contains('\\') && segment.none { it.code < 32 }
        }) { "WebDAV 备份目录包含非法路径段" }
        return segments
    }

    private fun safeName(name: String): String {
        require(name.isNotBlank() && name.endsWith(".zip") && !name.contains('/') &&
            !name.contains('\\') && name.none { it.code < 32 }) { "非法备份文件名" }
        return name
    }

    private fun endpoint(config: WebDavConfig): HttpUrl {
        require(config.isConfigured) { "请填写有效的 WebDAV 地址、用户名和密码" }
        return requireNotNull(config.normalizedEndpoint.toHttpUrlOrNull()) { "WebDAV URL 无效" }
    }

    private fun collectionUrl(config: WebDavConfig, count: Int = prefixSegments(config).size): HttpUrl {
        val base = endpoint(config)
        return base.newBuilder().apply {
            if (base.pathSegments.lastOrNull() == "") removePathSegment(base.pathSegments.lastIndex)
            prefixSegments(config).take(count).forEach { addPathSegment(it) }
            addPathSegment("")
        }.build()
    }

    private fun fileUrl(config: WebDavConfig, name: String): HttpUrl =
        collectionUrl(config).newBuilder().addPathSegment(safeName(name)).build()

    private fun request(config: WebDavConfig, url: HttpUrl, method: String, body: RequestBody? = null): Request =
        Request.Builder().url(url).method(method, body)
            .header("Authorization", Credentials.basic(config.username, config.password, Charsets.UTF_8))
            .header("User-Agent", "InkQilinLedger/backup").build()

    private fun propfind(config: WebDavConfig, url: HttpUrl, depth: String): String {
        val request = request(config, url, "PROPFIND", PROPFIND_BODY.toRequestBody("application/xml; charset=utf-8".toMediaType()))
            .newBuilder().header("Depth", depth).build()
        return client.newCall(request).execute().use { response ->
            check(response.code == 207) { "读取 WebDAV 目录失败：HTTP ${response.code}" }
            response.body?.string() ?: error("WebDAV 响应为空")
        }
    }

    private fun document(xml: String): Element {
        require(!xml.contains("<!DOCTYPE", ignoreCase = true) && !xml.contains("<!ENTITY", ignoreCase = true)) {
            "WebDAV XML 不允许文档类型或实体声明"
        }
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        // Android's XML provider does not implement all desktop Xerces flags.
        runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { factory.setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        val builder = factory.newDocumentBuilder()
        builder.setEntityResolver { _, _ -> error("WebDAV XML 不允许外部实体") }
        val root = builder.parse(InputSource(StringReader(xml))).documentElement
        require(root.localName == "multistatus" && root.namespaceURI == "DAV:") { "服务器返回了无效的 WebDAV 目录" }
        return root
    }

    private fun children(element: Element, name: String): List<Element> =
        (0 until element.childNodes.length).mapNotNull { index ->
            (element.childNodes.item(index) as? Element)?.takeIf { it.localName == name && it.namespaceURI == "DAV:" }
        }

    private fun property(response: Element): Element? = children(response, "propstat").firstOrNull { stat ->
        val status = children(stat, "status").firstOrNull()?.textContent.orEmpty()
        Regex("HTTP/[^ ]+ 2[0-9]{2}(?: .*|$)").matches(status.trim())
    }?.let { children(it, "prop").firstOrNull() }

    private fun resolvedHref(response: Element, base: HttpUrl): HttpUrl? {
        val href = children(response, "href").firstOrNull()?.textContent?.trim() ?: return null
        val resolved = base.resolve(href) ?: return null
        return resolved.takeIf { it.scheme == base.scheme && it.host == base.host && it.port == base.port &&
            it.username.isEmpty() && it.password.isEmpty() && it.query == null && it.fragment == null }
    }

    private fun verifyCollection(config: WebDavConfig, url: HttpUrl) {
        val root = document(propfind(config, url, "0"))
        val exists = children(root, "response").any { response ->
            val href = resolvedHref(response, url)
            val prop = property(response)
            href?.pathSegments?.filter { it.isNotEmpty() } == url.pathSegments.filter { it.isNotEmpty() } &&
                prop != null && prop.getElementsByTagNameNS("DAV:", "collection").length > 0
        }
        check(exists) { "WebDAV 路径不是可用目录" }
    }

    suspend fun ensureBackupDirectory(config: WebDavConfig): Unit = withContext(Dispatchers.IO) {
        for (count in 1..prefixSegments(config).size) {
            val url = collectionUrl(config, count)
            client.newCall(request(config, url, "MKCOL")).execute().use { response ->
                check(response.code == 201 || response.code == 405) { "创建 WebDAV 备份目录失败：HTTP ${response.code}" }
            }
            verifyCollection(config, url)
        }
    }

    suspend fun testConnection(config: WebDavConfig) {
        withContext(Dispatchers.IO) {
            val base = endpoint(config).newBuilder().addPathSegment("").build()
            verifyCollection(config, base)
        }
        ensureBackupDirectory(config)
    }

    suspend fun putObject(config: WebDavConfig, name: String, bytes: ByteArray) {
        safeName(name)
        ensureBackupDirectory(config)
        withContext(Dispatchers.IO) {
            client.newCall(request(config, fileUrl(config, name), "PUT", bytes.toRequestBody("application/zip".toMediaType()))).execute().use { response ->
                check(response.isSuccessful) { "WebDAV 上传失败：HTTP ${response.code}" }
            }
        }
    }

    suspend fun getObject(config: WebDavConfig, name: String): ByteArray = withContext(Dispatchers.IO) {
        client.newCall(request(config, fileUrl(config, name), "GET")).execute().use { response ->
            check(response.isSuccessful) { "WebDAV 下载失败：HTTP ${response.code}" }
            response.body?.bytes() ?: error("WebDAV 响应为空")
        }
    }

    suspend fun deleteObject(config: WebDavConfig, name: String): Unit = withContext(Dispatchers.IO) {
        val url = fileUrl(config, name)
        client.newCall(request(config, url, "DELETE")).execute().use { response ->
            check(response.isSuccessful || response.code == 404) { "WebDAV 删除失败：HTTP ${response.code}" }
        }
        client.newCall(request(config, url, "HEAD")).execute().use { response ->
            check(response.code == 404 || response.code == 410) { "WebDAV 删除后校验失败：HTTP ${response.code}" }
        }
    }

    suspend fun listObjects(config: WebDavConfig): List<WebDavObjectMeta> = withContext(Dispatchers.IO) {
        parsePropfind(propfind(config, collectionUrl(config), "1"), config)
    }

    internal fun parsePropfind(xml: String, config: WebDavConfig): List<WebDavObjectMeta> {
        val base = collectionUrl(config)
        val parent = base.pathSegments.filter { it.isNotEmpty() }
        return children(document(xml), "response").mapNotNull { response ->
            val url = resolvedHref(response, base) ?: return@mapNotNull null
            val segments = url.pathSegments
            if (segments.size != parent.size + 1 || segments.take(parent.size) != parent) return@mapNotNull null
            val name = segments.last()
            if (runCatching { safeName(name) }.isFailure) return@mapNotNull null
            val prop = property(response) ?: return@mapNotNull null
            if (prop.getElementsByTagNameNS("DAV:", "collection").length > 0) return@mapNotNull null
            val size = children(prop, "getcontentlength").firstOrNull()?.textContent?.toLongOrNull() ?: 0L
            val modified = children(prop, "getlastmodified").firstOrNull()?.textContent.orEmpty()
            WebDavObjectMeta(name, size.coerceAtLeast(0), modified)
        }.distinctBy { it.path }.sortedByDescending { it.path }
    }
}
