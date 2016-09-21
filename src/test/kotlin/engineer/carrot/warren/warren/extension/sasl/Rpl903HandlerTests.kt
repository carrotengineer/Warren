package engineer.carrot.warren.warren.extension.sasl

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import engineer.carrot.warren.kale.irc.message.IMessage
import engineer.carrot.warren.kale.irc.message.ircv3.CapEndMessage
import engineer.carrot.warren.kale.irc.message.ircv3.sasl.Rpl903Message
import engineer.carrot.warren.warren.IMessageSink
import engineer.carrot.warren.warren.extension.cap.CapLifecycle
import engineer.carrot.warren.warren.extension.cap.CapState
import engineer.carrot.warren.warren.extension.sasl.Rpl903Handler
import engineer.carrot.warren.warren.extension.sasl.SaslState
import engineer.carrot.warren.warren.state.AuthLifecycle
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class Rpl903HandlerTests {

    lateinit var handler: Rpl903Handler
    lateinit var capState: CapState
    lateinit var saslState: SaslState
    lateinit var sink: IMessageSink

    @Before fun setUp() {
        val capLifecycleState = CapLifecycle.NEGOTIATING
        capState = CapState(lifecycle = capLifecycleState, negotiate = setOf(), server = mapOf(), accepted = setOf(), rejected = setOf())
        saslState = SaslState(shouldAuth = false, lifecycle = AuthLifecycle.AUTHING, credentials = null)
        sink = mock()

        handler = Rpl903Handler(capState, saslState, sink)
    }

    @Test fun test_handle_LifecycleSetToAuthed() {
        handler.handle(Rpl903Message(source = "", target = "", contents = "SASL auth succeeded"), mapOf())

        assertEquals(AuthLifecycle.AUTHED, saslState.lifecycle)
    }

    @Test fun test_handle_RemainingCaps_DoesNotEndNegotiation() {
        capState.lifecycle = CapLifecycle.NEGOTIATING
        capState.negotiate = setOf("cap1", "cap2")
        capState.accepted = setOf("cap1")

        handler.handle(Rpl903Message(source = "", target = "", contents = "SASL auth succeeded"), mapOf())

        verify(sink, never()).write(any<IMessage>())
    }

    @Test fun test_handle_NoRemainingCaps_EndsNegotiation() {
        capState.lifecycle = CapLifecycle.NEGOTIATING
        capState.negotiate = setOf("cap1", "cap2")
        capState.accepted = setOf("cap1", "cap2")

        handler.handle(Rpl903Message(source = "", target = "", contents = "SASL auth succeeded"), mapOf())

        verify(sink).write(CapEndMessage())
    }

}