package com.aiassistant.feature.chat.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceInputCoordinatorTest {
    @Test
    fun `start exposes listening and partial is preview only`() {
        val fixture = fixture()

        fixture.coordinator.start()
        fixture.gateway.listener(0).onPartial("preview")

        assertEquals(1, fixture.gateway.startCount)
        assertEquals(VoiceInputState.Listening("preview"), fixture.observer.states.last())
        assertTrue(fixture.observer.finals.isEmpty())
    }

    @Test
    fun `provider final commits once and starts next segment`() {
        val fixture = fixture()
        fixture.coordinator.start()

        fixture.gateway.listener(0).onFinal("first")

        assertEquals(listOf("first"), fixture.observer.finals)
        assertEquals(2, fixture.gateway.startCount)
        assertEquals(VoiceInputState.Listening(), fixture.observer.states.last())
    }

    @Test
    fun `multiple provider finals remain ordered and nonduplicate`() {
        val fixture = fixture()
        fixture.coordinator.start()
        fixture.gateway.listener(0).onFinal("one")
        fixture.gateway.listener(1).onFinal("two")
        fixture.gateway.listener(0).onFinal("duplicate")

        assertEquals(listOf("one", "two"), fixture.observer.finals)
        assertEquals(3, fixture.gateway.startCount)
    }

    @Test
    fun `stop waits for terminal result commits it and does not restart`() {
        val fixture = fixture()
        fixture.coordinator.start()

        fixture.coordinator.stop()
        fixture.gateway.listener(0).onFinal("last")

        assertEquals(1, fixture.gateway.stopCount)
        assertEquals(listOf("last"), fixture.observer.finals)
        assertEquals(1, fixture.gateway.startCount)
        assertEquals(VoiceInputState.Idle, fixture.observer.states.last())
    }

    @Test
    fun `stop terminal error ends without guidance or restart`() {
        val fixture = fixture()
        fixture.coordinator.start()
        fixture.coordinator.stop()

        fixture.gateway.listener(0).onError(VoiceRecognitionError.Controlled("ended"))

        assertEquals(VoiceInputState.Idle, fixture.observer.states.last())
        assertEquals(1, fixture.gateway.startCount)
    }

    @Test
    fun `cancel discards partial and ignores delayed callback`() {
        val fixture = fixture()
        fixture.coordinator.start()
        fixture.gateway.listener(0).onPartial("discard")

        fixture.coordinator.cancel()
        fixture.gateway.listener(0).onFinal("late")

        assertEquals(1, fixture.gateway.cancelCount)
        assertTrue(fixture.observer.finals.isEmpty())
        assertEquals(VoiceInputState.Idle, fixture.observer.states.last())
    }

    @Test
    fun `cancel followed by immediate restart rejects operation A callback`() {
        val fixture = fixture()
        fixture.coordinator.start()
        fixture.coordinator.cancel()
        fixture.coordinator.start()

        fixture.gateway.listener(0).onFinal("old")
        fixture.gateway.listener(1).onFinal("new")

        assertEquals(listOf("new"), fixture.observer.finals)
    }

    @Test
    fun `duplicate start does not create concurrent recognition`() {
        val fixture = fixture()
        fixture.coordinator.start()
        fixture.coordinator.start()

        assertEquals(1, fixture.gateway.startCount)
    }

    @Test
    fun `release while active cancels destroys and rejects callbacks`() {
        val fixture = fixture()
        fixture.coordinator.start()

        fixture.coordinator.release()
        fixture.gateway.listener(0).onFinal("late")
        fixture.coordinator.start()

        assertEquals(1, fixture.gateway.cancelCount)
        assertTrue(fixture.gateway.released)
        assertEquals(1, fixture.gateway.startCount)
        assertTrue(fixture.observer.finals.isEmpty())
    }

    @Test
    fun `unavailable provider returns controlled guidance`() {
        val gateway = FakeGateway(available = false)
        val fixture = fixture(gateway)

        fixture.coordinator.start()

        assertEquals(0, gateway.startCount)
        assertTrue(fixture.observer.states.last() is VoiceInputState.Guidance)
    }

    @Test
    fun `start exception returns controlled guidance and stays restartable`() {
        val gateway = FakeGateway()
        gateway.nextStartResult = Result.failure(SecurityException("revoked"))
        val fixture = fixture(gateway)

        fixture.coordinator.start()
        assertTrue(fixture.observer.states.last() is VoiceInputState.Guidance)

        fixture.coordinator.start()
        assertEquals(2, gateway.startCount)
    }

    @Test
    fun `provider error cancels active segment and preserves editability`() {
        val fixture = fixture()
        fixture.coordinator.start()

        fixture.gateway.listener(0).onError(
            VoiceRecognitionError.Controlled("service failed")
        )

        assertEquals(
            VoiceInputState.Guidance("service failed"),
            fixture.observer.states.last()
        )
        assertTrue(fixture.observer.finals.isEmpty())
    }

    @Test
    fun `speech timeout continues overall interaction with a new segment`() {
        val fixture = fixture()
        fixture.coordinator.start()

        fixture.gateway.listener(0).onError(VoiceRecognitionError.RecoverableSegmentEnd)

        assertEquals(2, fixture.gateway.startCount)
        assertEquals(VoiceInputState.Listening(), fixture.observer.states.last())
        assertTrue(fixture.observer.finals.isEmpty())
    }

    @Test
    fun `no match continuation rejects stale callback from ended segment`() {
        val fixture = fixture()
        fixture.coordinator.start()
        val endedSegment = fixture.gateway.listener(0)

        endedSegment.onError(VoiceRecognitionError.RecoverableSegmentEnd)
        endedSegment.onFinal("stale")
        fixture.gateway.listener(1).onFinal("current")

        assertEquals(listOf("current"), fixture.observer.finals)
        assertEquals(3, fixture.gateway.startCount)
    }

    @Test
    fun `stop failure is controlled and cancels recognizer`() {
        val gateway = FakeGateway()
        val fixture = fixture(gateway)
        fixture.coordinator.start()
        gateway.nextStopResult = Result.failure(IllegalStateException("failed"))

        fixture.coordinator.stop()

        assertTrue(fixture.observer.states.last() is VoiceInputState.Guidance)
        assertEquals(1, gateway.cancelCount)
    }

    private fun fixture(gateway: FakeGateway = FakeGateway()): Fixture {
        val observer = RecordingObserver()
        val coordinator = VoiceInputCoordinator(gateway)
        coordinator.attach(observer)
        return Fixture(gateway, observer, coordinator)
    }

    private data class Fixture(
        val gateway: FakeGateway,
        val observer: RecordingObserver,
        val coordinator: VoiceInputCoordinator
    )

    private class RecordingObserver : VoiceInputCoordinator.Observer {
        val states = mutableListOf<VoiceInputState>()
        val finals = mutableListOf<String>()

        override fun onState(state: VoiceInputState) {
            states += state
        }

        override fun onFinalText(text: String) {
            finals += text
        }
    }

    private class FakeGateway(
        var available: Boolean = true
    ) : SpeechRecognitionGateway {
        private val listeners = mutableListOf<SpeechRecognitionGateway.Listener>()
        var startCount = 0
        var stopCount = 0
        var cancelCount = 0
        var released = false
        var nextStartResult: Result<Unit> = Result.success(Unit)
        var nextStopResult: Result<Unit> = Result.success(Unit)

        override val isAvailable: Boolean
            get() = available

        override fun start(listener: SpeechRecognitionGateway.Listener): Result<Unit> {
            startCount++
            listeners += listener
            return nextStartResult.also { nextStartResult = Result.success(Unit) }
        }

        override fun stop(): Result<Unit> {
            stopCount++
            return nextStopResult.also { nextStopResult = Result.success(Unit) }
        }

        override fun cancel() {
            cancelCount++
        }

        override fun release() {
            released = true
        }

        fun listener(index: Int): SpeechRecognitionGateway.Listener = listeners[index]
    }
}
