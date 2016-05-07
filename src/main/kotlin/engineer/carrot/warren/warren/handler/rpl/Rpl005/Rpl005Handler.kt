package engineer.carrot.warren.warren.handler.rpl.Rpl005

import engineer.carrot.warren.kale.IKaleHandler
import engineer.carrot.warren.kale.irc.message.rpl.Rpl005Message
import engineer.carrot.warren.warren.state.ParsingState

class Rpl005Handler(val state: ParsingState, val prefixHandler: IRpl005PrefixHandler, val channelModesHandler: IRpl005ChanModesHandler, val channelTypesHandler: IRpl005ChanTypesHandler) : IKaleHandler<Rpl005Message> {
    override val messageType = Rpl005Message::class.java

    override fun handle(message: Rpl005Message) {
        println("got isupport additions: ${message.tokens}")

        for ((key, value) in message.tokens) {
            if (value == null) {
                continue
            }

            when (key) {
                "PREFIX" -> prefixHandler.handle(value, state.userPrefixes)
                "CHANMODES" -> channelModesHandler.handle(value, state.channelModes)
                "CHANTYPES" -> channelTypesHandler.handle(value, state.channelTypes)
            }
        }
    }
}