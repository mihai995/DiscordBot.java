package net.discordbot.bots;

import net.discordbot.common.DiscordBot;
import net.discordbot.common.TextListener;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageReaction;

public final class ReactBot extends DiscordBot implements TextListener {

  @Override
  public void parseReaction(MessageReaction reaction, int factor) {
    // TODO: record how people receive the emitted reaction to tune future reactions.
  }

  @Override
  public boolean parseMessage(Message message) {
    return true;
  }
}
