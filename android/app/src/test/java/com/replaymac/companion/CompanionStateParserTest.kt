package com.replaymac.companion

import com.replaymac.companion.data.model.CompanionServerEvent
import com.replaymac.companion.data.network.CompanionProtocolParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionStateParserTest {

    @Test
    fun testParseAuthResponse() {
        val approvedJson = """
            {
                "type": "auth_response",
                "status": "approved",
                "serverName": "Alex's MacBook Pro",
                "serverVersion": "1.7.3"
            }
        """.trimIndent()

        val approvedEvent = CompanionProtocolParser.parse(approvedJson)
        assertTrue(approvedEvent is CompanionServerEvent.Auth)
        val auth = (approvedEvent as CompanionServerEvent.Auth).auth
        assertEquals("approved", auth.status)
        assertEquals("Alex's MacBook Pro", auth.serverName)
        assertEquals("1.7.3", auth.serverVersion)

        val pendingJson = """{"type":"auth_response","status":"pending","serverName":"Mac Mini","serverVersion":"1.0"}"""
        val pendingEvent = CompanionProtocolParser.parse(pendingJson)
        assertTrue(pendingEvent is CompanionServerEvent.Auth)
        assertEquals("pending", (pendingEvent as CompanionServerEvent.Auth).auth.status)
    }

    @Test
    fun testParseStateSnapshot() {
        val snapshotJson = """
            {
                "type": "state_snapshot",
                "isRecording": true,
                "isSessionRecording": false,
                "saveStatus": "idle",
                "recordingElapsedSeconds": 42.5,
                "extendedBufferElapsedSeconds": 120.0,
                "sessionElapsedSeconds": 0.0,
                "bufferedSeconds": 30.0,
                "bufferDurationSeconds": 30,
                "bufferMemoryBytes": 104857600,
                "bufferMemoryFormatted": "100 MB",
                "longBufferEnabled": true,
                "longBufferDurationSeconds": 300,
                "capturedDisplayName": "Built-in Retina Display",
                "isUsingFallbackDisplay": true,
                "isMicrophoneEnabled": true,
                "isMicrophoneMuted": false,
                "isSystemAudioEnabled": true,
                "isSystemAudioMuted": false,
                "recentClips": [
                    {
                        "id": "clip-1",
                        "filename": "Replay 2026-10-08 14.00.00.mov",
                        "kind": "replay",
                        "savedAt": 1728392400.0,
                        "duration": 30.0,
                        "fileSize": 45000000,
                        "hasThumbnail": true
                    }
                ]
            }
        """.trimIndent()

        val event = CompanionProtocolParser.parse(snapshotJson)
        assertTrue(event is CompanionServerEvent.Snapshot)
        val snapshot = (event as CompanionServerEvent.Snapshot).snapshot
        assertTrue(snapshot.isRecording)
        assertEquals(42.5, snapshot.recordingElapsedSeconds, 0.01)
        assertEquals(30, snapshot.bufferDurationSeconds)
        assertEquals("100 MB", snapshot.bufferMemoryFormatted)
        assertEquals("Built-in Retina Display", snapshot.capturedDisplayName)
        assertTrue(snapshot.isUsingFallbackDisplay)
        assertEquals(1, snapshot.recentClips.size)
        assertEquals("clip-1", snapshot.recentClips[0].id)
        assertTrue(snapshot.recentClips[0].hasThumbnail)
    }

    @Test
    fun testParseStateChanged() {
        val json = """
            {
                "type": "state_changed",
                "isRecording": false,
                "isSessionRecording": true,
                "saveStatus": "saving",
                "recordingElapsedSeconds": 0.0,
                "extendedBufferElapsedSeconds": 0.0,
                "sessionElapsedSeconds": 15.0,
                "bufferedSeconds": 0.0,
                "bufferDurationSeconds": 30,
                "bufferMemoryBytes": 0,
                "bufferMemoryFormatted": "0 MB",
                "longBufferEnabled": false,
                "longBufferDurationSeconds": 300,
                "capturedDisplayName": null,
                "isMicrophoneEnabled": false,
                "isMicrophoneMuted": false,
                "isSystemAudioEnabled": true,
                "isSystemAudioMuted": false,
                "recentClips": []
            }
        """.trimIndent()

        val event = CompanionProtocolParser.parse(json)
        assertTrue(event is CompanionServerEvent.StateChanged)
        val snapshot = (event as CompanionServerEvent.StateChanged).snapshot
        assertNotNull(snapshot)
        assertTrue(snapshot.isSessionRecording)
        assertEquals("saving", snapshot.saveStatus)
        assertTrue(snapshot.isSaveInProgress)
        // Older ReplayMac builds don't send the fallback flag at all.
        assertFalse(snapshot.isUsingFallbackDisplay)
    }

    @Test
    fun testParseTelemetryTick() {
        val tickJson = """
            {
                "type": "telemetry_tick",
                "recordingElapsedSeconds": 12.3,
                "extendedBufferElapsedSeconds": 45.6,
                "sessionElapsedSeconds": 0.0,
                "bufferedSeconds": 12.3,
                "bufferMemoryBytes": 52428800,
                "bufferMemoryFormatted": "50 MB"
            }
        """.trimIndent()

        val event = CompanionProtocolParser.parse(tickJson)
        assertTrue(event is CompanionServerEvent.Telemetry)
        val tick = (event as CompanionServerEvent.Telemetry).tick
        assertEquals(12.3, tick.recordingElapsedSeconds, 0.01)
        assertEquals(52428800L, tick.bufferMemoryBytes)
        assertEquals("50 MB", tick.bufferMemoryFormatted)
    }

    @Test
    fun testParseThumbnailResponse() {
        val thumbJson = """
            {
                "type": "thumbnail_response",
                "clipId": "clip-99",
                "thumbnailBase64": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
            }
        """.trimIndent()

        val event = CompanionProtocolParser.parse(thumbJson)
        assertTrue(event is CompanionServerEvent.Thumbnail)
        val thumb = (event as CompanionServerEvent.Thumbnail)
        assertEquals("clip-99", thumb.clipId)
        assertTrue(thumb.base64?.startsWith("iVBORw0") == true)
    }

    @Test
    fun testParseThumbnailResponseWithNullBase64() {
        val thumbJson = """
            {
                "type": "thumbnail_response",
                "clipId": "clip-pending",
                "thumbnailBase64": null
            }
        """.trimIndent()

        val event = CompanionProtocolParser.parse(thumbJson)
        assertTrue(event is CompanionServerEvent.Thumbnail)
        val thumb = (event as CompanionServerEvent.Thumbnail)
        assertEquals("clip-pending", thumb.clipId)
        assertEquals(null, thumb.base64)
    }

    @Test
    fun testParseDeniedAuthResponse() {
        val deniedJson = """
            {
                "type": "auth_response",
                "status": "denied",
                "serverName": "Security Mac",
                "serverVersion": "1.7.3"
            }
        """.trimIndent()

        val event = CompanionProtocolParser.parse(deniedJson)
        assertTrue(event is CompanionServerEvent.Auth)
        val auth = (event as CompanionServerEvent.Auth).auth
        assertEquals("denied", auth.status)
        assertEquals("Security Mac", auth.serverName)
    }

    @Test
    fun testParsePongAndError() {
        val pongEvent = CompanionProtocolParser.parse("""{"type":"pong"}""")
        assertTrue(pongEvent is CompanionServerEvent.Pong)

        val errEvent = CompanionProtocolParser.parse("""{"type":"error","message":"Invalid token"}""")
        assertTrue(errEvent is CompanionServerEvent.Error)
        assertEquals("Invalid token", (errEvent as CompanionServerEvent.Error).message)
        assertNull(errEvent.code)
    }

    @Test
    fun errorCarriesItsCode() {
        val event = CompanionProtocolParser.parse(
            """{"type":"error","message":"ReplayMac is already pairing another phone.","code":"busy"}"""
        )
        assertEquals(
            CompanionServerEvent.Error("ReplayMac is already pairing another phone.", "busy"),
            event
        )
    }

    @Test
    fun parsesAuthChallenge() {
        val event = CompanionProtocolParser.parse(
            """{"type":"auth_challenge","nonce":"bm9uY2U=","serverId":"5F1C","serverName":"Studio Mac","serverVersion":"1.8.0"}"""
        )
        assertTrue(event is CompanionServerEvent.AuthChallenge)
        val challenge = (event as CompanionServerEvent.AuthChallenge).challenge
        assertEquals("bm9uY2U=", challenge.nonce)
        assertEquals("5F1C", challenge.serverId)
        assertEquals("Studio Mac", challenge.serverName)
        assertEquals("1.8.0", challenge.serverVersion)
    }

    @Test
    fun authResponseCarriesPairingFieldsWhenSent() {
        val pending = CompanionProtocolParser.parse(
            """{"type":"auth_response","status":"pending","serverName":"Mac","serverVersion":"1.8.0","pairingCode":"4821"}"""
        ) as CompanionServerEvent.Auth
        assertEquals("4821", pending.auth.pairingCode)

        val approved = CompanionProtocolParser.parse(
            """{"type":"auth_response","status":"approved","serverName":"Mac","serverVersion":"1.8.0","serverId":"5F1C","pairingSecret":"c2VjcmV0"}"""
        ) as CompanionServerEvent.Auth
        assertEquals("c2VjcmV0", approved.auth.pairingSecret)

        val denied = CompanionProtocolParser.parse(
            """{"type":"auth_response","status":"denied","serverName":"Mac","serverVersion":"1.8.0","reason":"revoked"}"""
        ) as CompanionServerEvent.Auth
        assertEquals("revoked", denied.auth.reason)

        val plainPending = CompanionProtocolParser.parse(
            """{"type":"auth_response","status":"pending","serverName":"Mac","serverVersion":"1.8.0"}"""
        ) as CompanionServerEvent.Auth
        assertNull(plainPending.auth.pairingCode)
        assertNull(plainPending.auth.pairingSecret)
        assertNull(plainPending.auth.reason)
    }

    @Test
    fun testParseMalformedOrUnknown() {
        val unknownEvent = CompanionProtocolParser.parse("""{"type":"custom_future_type","foo":"bar"}""")
        assertTrue(unknownEvent is CompanionServerEvent.Unknown)

        val malformedEvent = CompanionProtocolParser.parse("NOT_JSON")
        assertTrue(malformedEvent is CompanionServerEvent.Error)
    }

    @Test
    fun authResponseCarriesTheMacsStableIdWhenSent() {
        val withId = CompanionProtocolParser.parse(
            """{"type":"auth_response","status":"approved","serverName":"Mac","serverVersion":"1.7.3","serverId":"5F1C"}"""
        )
        assertEquals("5F1C", (withId as CompanionServerEvent.Auth).auth.serverId)

        // ReplayMac builds before the identity change send no id.
        val withoutId = CompanionProtocolParser.parse(
            """{"type":"auth_response","status":"approved","serverName":"Mac","serverVersion":"1.7.3"}"""
        )
        assertEquals(null, (withoutId as CompanionServerEvent.Auth).auth.serverId)
    }

    @Test
    fun authResponseCarriesDeviceNameWhenAssignedOrConfirmedByMac() {
        val withDevice = CompanionProtocolParser.parse(
            """{"type":"auth_response","status":"approved","serverName":"Studio Mac","serverVersion":"1.7.3","serverId":"5F1C","deviceName":"Pixel 9 Pro"}"""
        )
        assertTrue(withDevice is CompanionServerEvent.Auth)
        val authWithDevice = (withDevice as CompanionServerEvent.Auth).auth
        assertEquals("Studio Mac", authWithDevice.serverName)
        assertEquals("Pixel 9 Pro", authWithDevice.deviceName)

        val withoutDevice = CompanionProtocolParser.parse(
            """{"type":"auth_response","status":"approved","serverName":"Studio Mac","serverVersion":"1.7.3","serverId":"5F1C"}"""
        )
        assertTrue(withoutDevice is CompanionServerEvent.Auth)
        assertEquals(null, (withoutDevice as CompanionServerEvent.Auth).auth.deviceName)

        val withNullDevice = CompanionProtocolParser.parse(
            """{"type":"auth_response","status":"approved","serverName":"Studio Mac","serverVersion":"1.7.3","deviceName":null}"""
        )
        assertTrue(withNullDevice is CompanionServerEvent.Auth)
        assertEquals(null, (withNullDevice as CompanionServerEvent.Auth).auth.deviceName)
    }

    @Test
    fun parsesExtendedSnapshotWithAudioMixerDisplaysAndProfiles() {
        val json = """
            {
                "type": "state_snapshot",
                "isRecording": true,
                "isSessionRecording": false,
                "saveStatus": "idle",
                "recordingElapsedSeconds": 30.0,
                "bufferedSeconds": 30.0,
                "bufferDurationSeconds": 30,
                "capturedDisplayName": "Studio Display",
                "isMicrophoneEnabled": true,
                "isMicrophoneMuted": false,
                "isSystemAudioEnabled": true,
                "isSystemAudioMuted": false,
                "microphoneVolume": 0.85,
                "systemAudioVolume": 0.6,
                "perAppAudioEnabled": false,
                "audioApps": [
                    {
                        "bundleId": "com.spotify.client",
                        "name": "Spotify",
                        "isMuted": true,
                        "isRunning": true,
                        "iconBase64": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
                    },
                    {
                        "bundleId": "com.hnc.Discord",
                        "name": "Discord",
                        "isMuted": false,
                        "isRunning": false
                    }
                ],
                "displays": [
                    {
                        "id": "display-1",
                        "name": "Built-in Retina Display",
                        "isActive": false,
                        "isConnected": true
                    },
                    {
                        "id": "display-2",
                        "name": "Studio Display",
                        "isActive": true,
                        "isConnected": true
                    }
                ],
                "profiles": [
                    {
                        "id": "profile-1",
                        "name": "Competitive 120fps",
                        "summary": "1440p · 120 FPS · HEVC",
                        "isActive": true
                    },
                    {
                        "id": "profile-2",
                        "name": "Desktop Review",
                        "summary": "4K · 60 FPS",
                        "isActive": false
                    }
                ],
                "recentClips": []
            }
        """.trimIndent()

        val event = CompanionProtocolParser.parse(json)
        assertTrue(event is CompanionServerEvent.Snapshot)
        val snapshot = (event as CompanionServerEvent.Snapshot).snapshot

        assertEquals(0.85, snapshot.microphoneVolume, 0.001)
        assertEquals(0.6, snapshot.systemAudioVolume, 0.001)
        assertFalse(snapshot.perAppAudioEnabled)

        assertEquals(2, snapshot.audioApps.size)
        assertEquals("com.spotify.client", snapshot.audioApps[0].bundleId)
        assertEquals("Spotify", snapshot.audioApps[0].name)
        assertTrue(snapshot.audioApps[0].isMuted)
        assertTrue(snapshot.audioApps[0].isRunning)
        assertTrue(snapshot.audioApps[0].iconBase64?.startsWith("iVBORw0") == true)
        assertFalse(snapshot.audioApps[1].isMuted)
        assertFalse(snapshot.audioApps[1].isRunning)
        assertNull(snapshot.audioApps[1].iconBase64)
        assertEquals(1, snapshot.mutedAudioAppsCount)

        assertEquals(2, snapshot.displays.size)
        assertEquals("Studio Display", snapshot.activeDisplayName)
        assertTrue(snapshot.displays[1].isActive)

        assertEquals(2, snapshot.profiles.size)
        assertEquals("Competitive 120fps", snapshot.activeProfileName)
        assertEquals("1440p · 120 FPS · HEVC", snapshot.profiles[0].summary)
        assertTrue(snapshot.hasCaptureSetupOptions)
    }

    @Test
    fun legacySnapshotDefaultsNewAudioAndSetupFieldsCleanly() {
        val legacyJson = """
            {
                "type": "state_snapshot",
                "isRecording": false,
                "saveStatus": "idle"
            }
        """.trimIndent()

        val event = CompanionProtocolParser.parse(legacyJson)
        assertTrue(event is CompanionServerEvent.Snapshot)
        val snapshot = (event as CompanionServerEvent.Snapshot).snapshot

        assertEquals(1.0, snapshot.microphoneVolume, 0.001)
        assertEquals(1.0, snapshot.systemAudioVolume, 0.001)
        assertFalse(snapshot.perAppAudioEnabled)
        assertTrue(snapshot.audioApps.isEmpty())
        assertTrue(snapshot.displays.isEmpty())
        assertTrue(snapshot.profiles.isEmpty())
        assertEquals(0, snapshot.mutedAudioAppsCount)
        assertFalse(snapshot.hasCaptureSetupOptions)
    }
}
