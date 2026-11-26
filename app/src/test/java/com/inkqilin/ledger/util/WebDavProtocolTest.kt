package com.inkqilin.ledger.util

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class WebDavProtocolTest {
    private lateinit var server: MockWebServer
    private val objects = mutableMapOf<String, ByteArray>()
    private val collections = mutableSetOf("/dav/")
    private val calls = mutableListOf<Pair<String, String>>()

    @Before fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty().substringBefore('?')
                calls += request.method.orEmpty() to path
                if (request.getHeader("Authorization") != "Basic dXNlcjpzZWNyZXQ=") {
                    return MockResponse().setResponseCode(401)
                }
                return when (request.method) {
                    "MKCOL" -> {
                        val parent = path.trimEnd('/').substringBeforeLast('/', "") + "/"
                        if (parent in collections) {
                            collections.add(path.trimEnd('/') + "/")
                            MockResponse().setResponseCode(201)
                        } else MockResponse().setResponseCode(409)
                    }
                    "PROPFIND" -> {
                        val decoded = URLDecoder.decode(path.replace("+", "%2B"), StandardCharsets.UTF_8)
                        val collection = decoded.trimEnd('/') + "/"
                        val body = if (collection in collections) {
                            """<d:multistatus xmlns:d="DAV:"><d:response><d:href>$path</d:href><d:propstat><d:prop><d:resourcetype><d:collection/></d:resourcetype></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response></d:multistatus>"""
                        } else """<d:multistatus xmlns:d="DAV:"/>"""
                        MockResponse().setResponseCode(207).setBody(body)
                    }
                    "PUT" -> {
                        objects[path] = request.body.readByteArray()
                        MockResponse().setResponseCode(201)
                    }
                    "GET" -> objects[path]?.let {
                        MockResponse().setResponseCode(200).setBody(okio.Buffer().write(it))
                    } ?: MockResponse().setResponseCode(404)
                    "DELETE" -> { objects.remove(path); MockResponse().setResponseCode(204) }
                    "HEAD" -> MockResponse().setResponseCode(if (path in objects) 200 else 404)
                    else -> MockResponse().setResponseCode(405)
                }
            }
        }
        server.start()
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun crudAuthenticationRecursiveCollectionsAndEncodedNames() = runBlocking {
        val config = WebDavConfig("http://127.0.0.1:${server.port}/dav", "user", "secret", "backups/v1")
        val payload = byteArrayOf(1, 2, 3)
        WebDavClient.testConnection(config)
        WebDavClient.putObject(config, "中文 +.zip", payload)
        assertArrayEquals(payload, WebDavClient.getObject(config, "中文 +.zip"))
        WebDavClient.deleteObject(config, "中文 +.zip")
        val missing = runCatching { WebDavClient.getObject(config, "中文 +.zip") }.exceptionOrNull()
        assertTrue(missing is IllegalStateException && missing.message?.contains("404") == true)
        val unauthorized = runCatching { WebDavClient.testConnection(config.copy(password = "wrong")) }.exceptionOrNull()
        assertTrue(unauthorized is IllegalStateException && unauthorized.message?.contains("401") == true)
        assertTrue(runCatching { WebDavClient.putObject(config, "../escape.zip", payload) }.exceptionOrNull() is IllegalArgumentException)
        assertEquals(listOf("/dav/backups/", "/dav/backups/v1/"), calls.filter { it.first == "MKCOL" }.take(2).map { it.second })
        assertTrue(calls.any { it.first == "HEAD" && it.second.contains("%E4%B8%AD") })
    }
}
