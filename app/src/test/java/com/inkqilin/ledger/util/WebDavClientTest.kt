package com.inkqilin.ledger.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebDavClientTest {
    private val config = WebDavConfig("https://dav.example.test/remote.php/dav/files/u", "user", "secret", "ledger/backups")

    @Test fun configAllowsHttpsAndLoopbackOnlyForHttp() {
        assertTrue(config.isConfigured)
        assertFalse(config.copy(endpoint = "http://dav.example.test/").isConfigured)
        assertTrue(config.copy(endpoint = "http://127.0.0.1:8080/dav").isConfigured)
        assertFalse(config.copy(endpoint = "http://192.168.1.2/dav").isConfigured)
        assertFalse(config.copy(endpoint = "https://user:pass@dav.example.test/dav").isConfigured)
    }

    @Test fun propfindKeepsOnlySameOriginFilesInConfiguredDirectory() {
        val xml = """<?xml version="1.0"?><d:multistatus xmlns:d="DAV:">
            <d:response><d:href>/remote.php/dav/files/u/ledger/backups/</d:href><d:propstat><d:prop><d:resourcetype><d:collection/></d:resourcetype></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response>
            <d:response><d:href>/remote.php/dav/files/u/ledger/backups/ledger_1.zip</d:href><d:propstat><d:prop><d:getcontentlength>42</d:getcontentlength><d:getlastmodified>Thu, 01 Jan 2026 00:00:00 GMT</d:getlastmodified><d:resourcetype/></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response>
            <d:response><d:href>https://evil.example/outside.zip</d:href><d:propstat><d:prop><d:getcontentlength>1</d:getcontentlength><d:resourcetype/></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response>
            <d:response><d:href>/remote.php/dav/files/u/ledger/backups/sub/hidden.zip</d:href><d:propstat><d:prop><d:getcontentlength>1</d:getcontentlength><d:resourcetype/></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response>
            <d:response><d:href>/remote.php/dav/files/u/ledger/backups/%2e%2e/escape.zip</d:href><d:propstat><d:prop><d:getcontentlength>1</d:getcontentlength><d:resourcetype/></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response>
        </d:multistatus>"""
        val listed = WebDavClient.parsePropfind(xml, config)
        assertEquals(1, listed.size)
        assertEquals("ledger_1.zip", listed.single().path)
        assertEquals(42L, listed.single().size)
    }

    @Test fun prefixRejectsTraversalAndControlCharacters() {
        assertTrue(runCatching { WebDavClient.parsePropfind("<x/>", config.copy(prefix = "safe/../escape")) }.isFailure)
        assertTrue(runCatching { WebDavClient.parsePropfind("<x/>", config.copy(prefix = "safe\\\\escape")) }.isFailure)
        assertTrue(runCatching { WebDavClient.parsePropfind("<x/>", config.copy(prefix = "safe\u0001bad")) }.isFailure)
    }

    @Test fun propfindPreservesPlusInFilenameAndUrlEncodesPrefix() {
        val plusConfig = config.copy(prefix = "ledger+data/backup folder")
        val xml = """<d:multistatus xmlns:d="DAV:"><d:response><d:href>/remote.php/dav/files/u/ledger%2Bdata/backup%20folder/a+b.zip</d:href><d:propstat><d:prop><d:getcontentlength>9</d:getcontentlength><d:resourcetype/></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response></d:multistatus>"""
        assertEquals("a+b.zip", WebDavClient.parsePropfind(xml, plusConfig).single().path)
    }

    @Test fun propfindRejectsHostAndDirectoryPrefixLookalikes() {
        val xml = """<d:multistatus xmlns:d="DAV:"><d:response><d:href>https://dav.example.test.evil/remote.php/dav/files/u/ledger/backups/x.zip</d:href><d:propstat><d:prop><d:resourcetype/></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response><d:response><d:href>/remote.php/dav/files/u/ledger/backups-extra/x.zip</d:href><d:propstat><d:prop><d:resourcetype/></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response></d:multistatus>"""
        assertTrue(WebDavClient.parsePropfind(xml, config).isEmpty())
    }
    @Test fun propfindRequiresSuccessfulStatusAndRejectsCollections() {
        val xml = """<d:multistatus xmlns:d="DAV:">
            <d:response><d:href>/remote.php/dav/files/u/ledger/backups/failed.zip</d:href><d:propstat><d:prop><d:resourcetype/></d:prop><d:status>HTTP/1.1 404 Not Found</d:status></d:propstat></d:response>
            <d:response><d:href>/remote.php/dav/files/u/ledger/backups/missing.zip</d:href><d:propstat><d:prop><d:resourcetype/></d:prop></d:propstat></d:response>
            <d:response><d:href>/remote.php/dav/files/u/ledger/backups/folder.zip</d:href><d:propstat><d:prop><d:resourcetype><d:collection/></d:resourcetype></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response>
        </d:multistatus>"""
        assertTrue(WebDavClient.parsePropfind(xml, config).isEmpty())
    }

    @Test fun propfindRejectsExternalEntities() {
        val xml = """<!DOCTYPE x [<!ENTITY secret SYSTEM "file:///etc/passwd">]><d:multistatus xmlns:d="DAV:">&secret;</d:multistatus>"""
        assertTrue(runCatching { WebDavClient.parsePropfind(xml, config) }.isFailure)
    }
}