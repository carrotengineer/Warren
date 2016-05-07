package engineer.carrot.warren.warren.handler

import engineer.carrot.warren.kale.IKaleHandler
import engineer.carrot.warren.kale.irc.message.ircv3.CapLsMessage
import engineer.carrot.warren.kale.irc.message.ircv3.CapReqMessage
import engineer.carrot.warren.warren.IMessageSink
import engineer.carrot.warren.warren.handler.helper.RegistrationHelper
import engineer.carrot.warren.warren.state.CapLifecycle
import engineer.carrot.warren.warren.state.CapState
import engineer.carrot.warren.warren.state.SaslState

class CapLsHandler(val capState: CapState, val saslState: SaslState, val sink: IMessageSink) : IKaleHandler<CapLsMessage> {
    override val messageType = CapLsMessage::class.java

    override fun handle(message: CapLsMessage) {
        val caps = message.caps
        val lifecycle = capState.lifecycle

        capState.server += message.caps

        println("server supports following caps: $caps")

        when(lifecycle) {
            CapLifecycle.NEGOTIATING -> {
                if (!message.isMultiline) {
                    val requestCaps = capState.server.keys.intersect(capState.negotiate)
                    val implicitlyRejectedCaps = capState.negotiate.subtract(requestCaps)

                    capState.rejected += implicitlyRejectedCaps

                    if (RegistrationHelper.shouldEndCapNegotiation(saslState, capState)) {
                        println("server gave us caps and ended with a non-multiline ls, not in the middle of SASL auth, implicitly rejecting: $implicitlyRejectedCaps, nothing left so ending negotiation")

                        RegistrationHelper.endCapNegotiation(sink, capState)
                    } else if (!requestCaps.isEmpty()) {
                        println("server gave us caps and ended with a non-multiline ls, requesting: $requestCaps, implicitly rejecting: $implicitlyRejectedCaps")

                        requestCaps.forEach { cap -> sink.write(CapReqMessage(caps = listOf(cap))) }
                    }
                } else {
                    println("server gave us a multiline cap ls, expecting more caps before ending")
                }
            }

            else -> println("server told us about caps but we don't think we're negotiating")
        }
    }
}
