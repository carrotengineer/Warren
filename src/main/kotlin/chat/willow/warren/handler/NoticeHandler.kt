package chat.willow.warren.handler

import chat.willow.kale.IKaleHandler
import chat.willow.kale.irc.message.rfc1459.NoticeMessage
import chat.willow.warren.helper.loggerFor
import chat.willow.warren.state.ChannelTypesState

class NoticeHandler(val channelTypesState: ChannelTypesState) : IKaleHandler<NoticeMessage> {

    private val LOGGER = loggerFor<NoticeHandler>()

    override val messageType = NoticeMessage::class.java

    override fun handle(message: NoticeMessage, tags: Map<String, String?>) {
        val source = message.source
        val target = message.target
        val messageContents = message.message

        if (source == null) {
            LOGGER.warn("got a Notice but the source was missing - bailing: $message")
            return
        }

        if (channelTypesState.types.any { char -> target.startsWith(char) }) {
            // Channel notice

            LOGGER.info("NOTICE: $target <${source.nick}> $messageContents")
        } else {
            // Private notice

            LOGGER.info("NOTICE PM: <${source.nick}> $messageContents")
        }
    }

}