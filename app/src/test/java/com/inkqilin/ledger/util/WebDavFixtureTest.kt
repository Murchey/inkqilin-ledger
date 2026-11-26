package com.inkqilin.ledger.util

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebDavFixtureTest {
    private fun config(server: MockWebServer) =
        WebDavConfig("http://127.0.0.1:${server.port}/dav", "user", "secret", "backups/v1")

    @Test fun mkdirStopsOnParentConflict() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(409))
            val error = runCatching { WebDavClient.ensureBackupDirectory(config(server)) }.exceptionOrNull()
            assertTrue(error is IllegalStateException && error.message?.contains("409") == true)
            assertEquals("MKCOL", server.takeRequest().method)
            assertEquals(1, server.requestCount)
        } finally { server.shutdown() }
    }

    @Test fun existingCollectionMustBeVerified() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(405))
            server.enqueue(MockResponse().setResponseCode(207).setBody("""<d:multistatus xmlns:d="DAV:"><d:response><d:href>/dav/backups/</d:href><d:propstat><d:prop><d:resourcetype/></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response></d:multistatus>"""))
            assertTrue(runCatching { WebDavClient.ensureBackupDirectory(config(server)) }.exceptionOrNull() is IllegalStateException)
            assertEquals("MKCOL", server.takeRequest().method)
            val request = server.takeRequest()
            assertEquals("PROPFIND", request.method)
            assertEquals("0", request.getHeader("Depth"))
            assertEquals(2, server.requestCount)
        } finally { server.shutdown() }
    }

    @Test fun deleteRequiresMissingHeadAfterSuccess() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(204))
            server.enqueue(MockResponse().setResponseCode(200))
            val error = runCatching { WebDavClient.deleteObject(config(server), "file.zip") }.exceptionOrNull()
            assertTrue(error is IllegalStateException && error.message?.contains("200") == true)
            assertEquals("DELETE", server.takeRequest().method)
            assertEquals("HEAD", server.takeRequest().method)
        } finally { server.shutdown() }
    }
}
