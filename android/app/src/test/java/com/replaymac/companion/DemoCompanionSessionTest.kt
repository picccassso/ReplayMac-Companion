package com.replaymac.companion

import com.replaymac.companion.data.demo.DemoCompanionSession
import com.replaymac.companion.data.demo.DemoCompanionState
import com.replaymac.companion.data.demo.DemoSaveStatus
import com.replaymac.companion.data.demo.DemoThumbnailRenderer
import com.replaymac.companion.data.model.CompanionActions
import com.replaymac.companion.data.model.CompanionServerEvent
import com.replaymac.companion.data.model.CompanionStateSnapshot
import com.replaymac.companion.data.model.CompanionVolumeTargets
import com.replaymac.companion.data.model.ConnectionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class DemoCompanionSessionTest {

    private val now = 1_760_000_000L

    /** A session on the test's virtual clock, plus every event it sends. */
    private class Harness(val session: DemoCompanionSession, val events: MutableList<CompanionServerEvent>) {
        val snapshots get() = events.filterIsInstance<CompanionServerEvent.StateChanged>().map { it.snapshot }
        val latest: CompanionStateSnapshot get() = snapshots.last()
    }

    private fun TestScope.harness(): Harness {
        val session = DemoCompanionSession(
            scope = backgroundScope,
            clock = { now + testScheduler.currentTime / 1_000 },
            zone = { ZoneOffset.UTC },
            thumbnailRenderer = DemoThumbnailRenderer { clipId -> "jpeg-of-$clipId" }
        )
        val events = mutableListOf<CompanionServerEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            session.events.collect { events += it }
        }
        return Harness(session, events)
    }

    /** Runs the virtual clock forward [millis], including anything due at that instant. */
    private fun TestScope.wait(millis: Long) {
        advanceTimeBy(millis)
        runCurrent()
    }

    @Test
    fun startConnectsToTheDemoMacAndSendsItsState() = runTest {
        val h = harness()
        assertEquals(ConnectionState.Disconnected, h.session.connectionState.value)

        h.session.start()

        val connected = h.session.connectionState.value as ConnectionState.Connected
        assertEquals("Demo Mac", connected.serverName)
        assertEquals("Demo", connected.serverVersion)
        assertEquals("demo", connected.host)
        assertEquals(0, connected.port)
        assertNull(connected.serverId)
        assertEquals(listOf(DemoCompanionState.initial(now, ZoneOffset.UTC).toSnapshot()), h.snapshots)
    }

    @Test
    fun theRecordingTicksOnceASecond() = runTest {
        val h = harness()
        h.session.start()

        wait(3_000)

        assertEquals(757.0, h.latest.recordingElapsedSeconds, 0.0)
        assertEquals(4, h.snapshots.size)
    }

    @Test
    fun aSaveShowsSavingThenSavedThenIdle() = runTest {
        val h = harness()
        h.session.start()

        assertTrue(h.session.sendAction(CompanionActions.SAVE_REPLAY))
        assertEquals(DemoSaveStatus.SAVING, h.latest.saveStatus)

        wait(1_199)
        assertEquals(DemoSaveStatus.SAVING, h.latest.saveStatus)
        assertEquals(3, h.latest.recentClips.size)

        wait(1)
        assertEquals(DemoSaveStatus.SAVED, h.latest.saveStatus)
        val clip = h.latest.recentClips.first()
        assertEquals("demo-clip-4", clip.id)
        assertEquals("replay", clip.kind)
        assertEquals(now + 1.0, clip.savedAt, 0.0)

        wait(1_999)
        assertEquals(DemoSaveStatus.SAVED, h.latest.saveStatus)
        wait(1)
        assertEquals(DemoSaveStatus.IDLE, h.latest.saveStatus)
        assertEquals(4, h.latest.recentClips.size)
    }

    @Test
    fun aRetriedFailedSaveRestartsTheResetTimer() = runTest {
        val h = harness()
        h.session.start()
        h.session.sendAction(CompanionActions.TOGGLE_BUFFER)

        h.session.sendAction(CompanionActions.SAVE_REPLAY)
        assertEquals(DemoSaveStatus.FAILED, h.latest.saveStatus)
        wait(1_500)
        h.session.sendAction(CompanionActions.SAVE_REPLAY)

        // The first failure's reset would have landed here.
        wait(1_000)
        assertEquals(DemoSaveStatus.FAILED, h.latest.saveStatus)
        wait(1_000)
        assertEquals(DemoSaveStatus.IDLE, h.latest.saveStatus)
    }

    @Test
    fun stopDisconnectsAndNothingMoreHappens() = runTest {
        val h = harness()
        h.session.start()
        h.session.sendAction(CompanionActions.SAVE_REPLAY)

        h.session.stop()
        val sentBeforeStop = h.events.size
        wait(10_000)

        assertEquals(ConnectionState.Disconnected, h.session.connectionState.value)
        assertFalse(h.session.isRunning)
        assertNull(h.session.currentState)
        assertEquals(sentBeforeStop, h.events.size)
        assertFalse(h.session.sendAction(CompanionActions.TOGGLE_BUFFER))
        assertFalse(h.session.getThumbnail("demo-clip-1"))
        assertFalse(h.session.resync())
    }

    @Test
    fun thumbnailsAreAnsweredWithTheRenderersImage() = runTest {
        val h = harness()
        h.session.start()

        assertTrue(h.session.getThumbnail("demo-clip-2"))
        assertTrue(h.session.getThumbnail("no-such-clip"))

        val answers = h.events.filterIsInstance<CompanionServerEvent.Thumbnail>()
        assertEquals(
            listOf(
                CompanionServerEvent.Thumbnail("demo-clip-2", "jpeg-of-demo-clip-2"),
                CompanionServerEvent.Thumbnail("no-such-clip", null)
            ),
            answers
        )
    }

    @Test
    fun commandsChangeTheStateTheAppIsSent() = runTest {
        val h = harness()
        h.session.start()

        assertTrue(h.session.sendAction(CompanionActions.TOGGLE_MICROPHONE))
        assertTrue(h.session.setVolume(CompanionVolumeTargets.SYSTEM_AUDIO, 0.4))
        assertTrue(h.session.toggleAppMute("com.hnc.Discord"))
        assertTrue(h.session.selectDisplay("demo-display-builtin"))
        assertTrue(h.session.applyProfile("demo-profile-tutorials"))
        assertTrue(h.session.trimClip("demo-clip-1"))
        assertTrue(h.session.deleteClip("demo-clip-2"))
        assertFalse(h.session.sendAction("notAnAction"))

        val snapshot = h.latest
        assertTrue(snapshot.isMicrophoneMuted)
        assertEquals(0.4, snapshot.systemAudioVolume, 0.0)
        assertEquals(0, snapshot.mutedAudioAppsCount)
        assertEquals("Built-in Retina Display", snapshot.capturedDisplayName)
        assertEquals("Tutorials", snapshot.activeProfileName)
        assertEquals(listOf("demo-clip-4", "demo-clip-1", "demo-clip-3"), snapshot.recentClips.map { it.id })
    }

    @Test
    fun resyncResendsTheCurrentState() = runTest {
        val h = harness()
        h.session.start()
        h.session.sendAction(CompanionActions.TOGGLE_SESSION)
        val before = h.snapshots.size

        assertTrue(h.session.resync())

        assertEquals(before + 1, h.snapshots.size)
        assertTrue(h.latest.isSessionRecording)
    }
}
