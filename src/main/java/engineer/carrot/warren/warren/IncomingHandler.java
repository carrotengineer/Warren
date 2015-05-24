package engineer.carrot.warren.warren;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.gson.Gson;
import engineer.carrot.warren.warren.irc.handlers.IMessageHandler;
import engineer.carrot.warren.warren.irc.handlers.RPL.*;
import engineer.carrot.warren.warren.irc.handlers.core.JoinHandler;
import engineer.carrot.warren.warren.irc.handlers.core.PartHandler;
import engineer.carrot.warren.warren.irc.handlers.core.PingHandler;
import engineer.carrot.warren.warren.irc.handlers.core.PrivMsgHandler;
import engineer.carrot.warren.warren.irc.messages.IMessage;
import engineer.carrot.warren.warren.irc.messages.IRCMessage;
import engineer.carrot.warren.warren.irc.messages.RPL.*;
import engineer.carrot.warren.warren.irc.messages.core.*;
import engineer.carrot.warren.warren.util.IMessageQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public class IncomingHandler implements IIncomingHandler {
    private final Logger LOGGER = LoggerFactory.getLogger(IncomingHandler.class);
    private final Gson messageGson = new Gson();

    private IWarrenDelegate botDelegate;
    private IMessageQueue outgoingQueue;
    private EventBus eventBus;

    private Map<String, IMessageHandler> commandDelegateMap;
    private Map<String, Class<? extends IMessage>> messageMap;

    private Set<String> nextExpectedCommands;

    public IncomingHandler(IWarrenDelegate botDelegate, IMessageQueue outgoingQueue, EventBus eventBus) {
        this.botDelegate = botDelegate;
        this.outgoingQueue = outgoingQueue;
        this.eventBus = eventBus;

        this.initialise();
    }

    private void initialise() {
        this.commandDelegateMap = Maps.newHashMap();
        this.messageMap = Maps.newHashMap();

        this.nextExpectedCommands = Sets.newHashSet();

        this.initialiseMessageHandlers();
    }

    private void initialiseMessageHandlers() {
        // TODO: Find this automatically with annotations or something

        this.addMessageToMap(new CreatedMessage());
        this.addMessageToMap(new NoticeMessage());
        this.addMessageToMap(new PongMessage());
        this.addMessageToMap(new TopicWhoTimeMessage());
        this.addMessageToMap(new YourHostMessage());

        this.addMessageHandlerPairToMap(new MOTDStartMessage(), new MOTDStartHandler());
        this.addMessageHandlerPairToMap(new MOTDMessage(), new MOTDHandler());
        this.addMessageHandlerPairToMap(new EndOfMOTDMessage(), new EndOfMOTDHandler());
        this.addMessageHandlerPairToMap(new NoTopicMessage(), new NoTopicHandler());
        this.addMessageHandlerPairToMap(new TopicMessage(), new TopicHandler());
        this.addMessageHandlerPairToMap(new WelcomeMessage(), new WelcomeHandler());
        this.addMessageHandlerPairToMap(new JoinedChannelMessage(), new JoinHandler());
        this.addMessageHandlerPairToMap(new PartChannelMessage(), new PartHandler());
        this.addMessageHandlerPairToMap(new PingMessage(), new PingHandler());
        this.addMessageHandlerPairToMap(new PrivMsgMessage(), new PrivMsgHandler());
        this.addMessageHandlerPairToMap(new NamReplyMessage(), new NamReplyHandler());
        this.addMessageHandlerPairToMap(new ISupportMessage(), new ISupportHandler());

        for (IMessageHandler handler : this.commandDelegateMap.values()) {
            handler.setBotDelegate(this.botDelegate);
            handler.setOutgoingQueue(this.outgoingQueue);
            handler.setIncomingHandler(this);
            handler.setEventBus(this.eventBus);
        }
    }

    private void addMessageToMap(IMessage message) {
        this.messageMap.put(message.getCommandID(), message.getClass());
    }

    private void addMessageHandlerPairToMap(IMessage message, IMessageHandler handler) {
        if (this.messageMap.containsKey(message.getCommandID())) {
            throw new RuntimeException("Cannot add a message handler pair when said message is already in the map: " + message.getCommandID());
        }

        this.messageMap.put(message.getCommandID(), message.getClass());
        this.commandDelegateMap.put(message.getCommandID(), handler);
    }

    @Nullable
    private IMessage createTypedMessageFromCommandCode(@Nonnull String commandCode) {
        Class<? extends IMessage> clazzMessage = this.messageMap.get(commandCode);
        if (clazzMessage == null) {
            return null;
        }

        try {
            return clazzMessage.newInstance();
        } catch (InstantiationException e) {
            LOGGER.error("Failed to instantiate new message: {}", e);
        } catch (IllegalAccessException e) {
            LOGGER.error("Failed to instantiate new message, access exception: {}", e);
        }

        return null;
    }

    @Override
    public void setNextExpectedCommands(Set<String> nextExpectedCommands) {
        this.nextExpectedCommands = nextExpectedCommands;
    }

    @Override
    public void setNextExpectedCommandToAnything() {
        this.nextExpectedCommands.clear();
    }

    @Override
    public boolean handleIRCMessage(IRCMessage message, String originalLine) {
        if (!this.nextExpectedCommands.isEmpty()) {
            // Check that this command is in the set of commands we're expecting next
            LOGGER.info("Next expected commands: " + this.nextExpectedCommands);
        }
//            LOGGER.info("Raw message: " + serverResponse);
        if (!this.messageMap.containsKey(message.command)) {
            LOGGER.info("Unknown: {}", message.buildPrettyOutput());
            return true;
        }

        IMessage typedMessage = this.createTypedMessageFromCommandCode(message.command);
        if (typedMessage == null) {
            LOGGER.error("Failed to make a message for code. Not processing: {}", message.command);
            return false;
        }

        boolean wellFormed = typedMessage.isMessageWellFormed(message);
        if (!wellFormed) {
            LOGGER.error("Message was not well formed. Not processing: {}", originalLine);
            return false;
        }

        // The IRCMessage being well formed guarantees that we can build() the correct typed message from it
        typedMessage.build(message);

        IMessageHandler messageHandler = this.commandDelegateMap.get(message.command);
        if (messageHandler == null) {
            LOGGER.info("{}: {}", message.command, messageGson.toJson(typedMessage));
            return true;
        }

        messageHandler.handleMessage(typedMessage);

        return true;
    }
}