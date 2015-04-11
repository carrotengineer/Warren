package engineer.carrot.warren.irc.handlers.RPL;

import com.google.common.collect.Sets;
import engineer.carrot.warren.irc.handlers.MessageHandler;
import engineer.carrot.warren.irc.messages.MessageCodes;
import engineer.carrot.warren.irc.messages.RPL.MOTDMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class MOTDHandler extends MessageHandler<MOTDMessage> {
    final Logger LOGGER = LoggerFactory.getLogger(MOTDHandler.class);

    @Override
    public void handleMessage(@Nonnull MOTDMessage message) {
        LOGGER.info("MOTD: {} adds '{}'", message.forServer, message.contents);

        this.incomingHandler.setNextExpectedCommands(Sets.newHashSet(MessageCodes.RPL.MOTD, MessageCodes.RPL.ENDOFMOTD));
    }
}