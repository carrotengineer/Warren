package engineer.carrot.warren.warren.handler

import engineer.carrot.warren.kale.IKaleHandler
import engineer.carrot.warren.kale.irc.message.rpl.Rpl353Message
import engineer.carrot.warren.warren.state.ChannelsState
import engineer.carrot.warren.warren.state.UserPrefixesState
import engineer.carrot.warren.warren.state.generateUser

class Rpl353Handler(val channelsState: ChannelsState, val userPrefixesState: UserPrefixesState) : IKaleHandler<Rpl353Message> {
    override val messageType = Rpl353Message::class.java

    override fun handle(message: Rpl353Message) {
        val names = message.names

        val channel = channelsState.joined[message.channel]
        if (channel == null) {
            println("got a 353 for a channel we don't think we're in - bailing: ${message.channel}")
            return
        }

        for (name in names) {
            val (prefixes, nick) = trimPrefixes(name)

            if (nick.isEmpty()) {
                println("nick was empty after trimming: ${name}")
                continue
            }

            var modes = setOf<Char>()
            for (prefix in prefixes) {
                val mode = userPrefixesState.prefixesToModes[prefix]
                if (mode != null) {
                    modes += mode
                }
            }

            channel.users += generateUser(nick, modes)
        }

        println("channel state after 353: $channel")
    }


    private fun trimPrefixes(rawNick: String): Pair<Set<Char>, String> {
        var nick = rawNick
        var prefixes = setOf<Char>()

        for (char in nick) {
            if (userPrefixesState.prefixesToModes.keys.contains(char)) {
                prefixes += char
                nick = nick.substring(1)
            } else {
                return Pair(prefixes, nick)
            }
        }

        return Pair(prefixes, nick)
    }
}