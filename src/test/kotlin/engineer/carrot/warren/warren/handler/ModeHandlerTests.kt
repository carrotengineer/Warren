package engineer.carrot.warren.warren.handler

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import engineer.carrot.warren.kale.irc.message.rfc1459.ModeMessage
import engineer.carrot.warren.kale.irc.prefix.Prefix
import engineer.carrot.warren.warren.ChannelModeEvent
import engineer.carrot.warren.warren.IWarrenEventDispatcher
import engineer.carrot.warren.warren.UserModeEvent
import engineer.carrot.warren.warren.state.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals

class ModeHandlerTests {

    lateinit var handler: ModeHandler
    lateinit var mockEventDispatcher: IWarrenEventDispatcher
    lateinit var channelsState: ChannelsState

    @Before fun setUp() {
        mockEventDispatcher = mock()
        val channelTypes = ChannelTypesState(types = setOf('#'))
        channelsState = ChannelsState(joining = mutableMapOf(), joined = mutableMapOf())
        val userPrefixesState = UserPrefixesState(prefixesToModes = mapOf('+' to 'v', '@' to 'o'))
        handler = ModeHandler(mockEventDispatcher, channelTypes, channelsState, userPrefixesState)
    }

    @Test fun test_handle_ChannelModeChange_NoPrefix_FiresEvents() {
        val firstExpectedModifier = ModeMessage.ModeModifier(type = '+', mode = 'x', parameter = "someone")
        val secondExpectedModifier = ModeMessage.ModeModifier(type = '+', mode = 'y')

        handler.handle(ModeMessage(source = null, target = "#channel", modifiers = listOf(firstExpectedModifier, secondExpectedModifier)))

        verify(mockEventDispatcher).fire(ChannelModeEvent(user = null, channel = "#channel", modifier = firstExpectedModifier))
        verify(mockEventDispatcher).fire(ChannelModeEvent(user = null, channel = "#channel", modifier = secondExpectedModifier))
    }

    @Test fun test_handle_ChannelModeChange_WithPrefix_FiresEvents() {
        val firstExpectedModifier = ModeMessage.ModeModifier(type = '+', mode = 'x', parameter = "someone")
        val secondExpectedModifier = ModeMessage.ModeModifier(type = '+', mode = 'y')

        handler.handle(ModeMessage(source = Prefix(nick = "admin"), target = "#channel", modifiers = listOf(firstExpectedModifier, secondExpectedModifier)))

        verify(mockEventDispatcher).fire(ChannelModeEvent(user = Prefix(nick = "admin"), channel = "#channel", modifier = firstExpectedModifier))
        verify(mockEventDispatcher).fire(ChannelModeEvent(user = Prefix(nick = "admin"), channel = "#channel", modifier = secondExpectedModifier))
    }

    @Test fun test_handle_ChannelModeChange_UserPrefixAdded() {
        channelsState.joined["#channel"] = ChannelState("#channel", mutableMapOf("someone" to ChannelUserState(nick = "someone", modes = mutableSetOf())))

        val addVoiceModifier = ModeMessage.ModeModifier(type = '+', mode = 'v', parameter = "someone")

        handler.handle(ModeMessage(target = "#channel", modifiers = listOf(addVoiceModifier)))

        assertEquals(mutableSetOf('v'), channelsState.joined["#channel"]!!.users["someone"]!!.modes)
    }

    @Test fun test_handle_ChannelModeChange_UserPrefixRemoved() {
        channelsState.joined["#channel"] = ChannelState("#channel", mutableMapOf("someone" to ChannelUserState(nick = "someone", modes = mutableSetOf('o'))))

        val addVoiceModifier = ModeMessage.ModeModifier(type = '-', mode = 'o', parameter = "someone")

        handler.handle(ModeMessage(target = "#channel", modifiers = listOf(addVoiceModifier)))

        assertEquals(mutableSetOf<Char>(), channelsState.joined["#channel"]!!.users["someone"]!!.modes)
    }

    @Test fun test_handle_UserModeChange_FiresEvents() {
        val firstExpectedModifier = ModeMessage.ModeModifier(type = '+', mode = 'v', parameter = "someone")
        val secondExpectedModifier = ModeMessage.ModeModifier(type = '+', mode = 'x')

        handler.handle(ModeMessage(source = null, target = "someone", modifiers = listOf(firstExpectedModifier, secondExpectedModifier)))

        verify(mockEventDispatcher).fire(UserModeEvent(user = "someone", modifier = firstExpectedModifier))
        verify(mockEventDispatcher).fire(UserModeEvent(user = "someone", modifier = secondExpectedModifier))
    }

}