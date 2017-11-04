package net.discordbot.bots;

import net.discordbot.core.BotCommand;
import net.discordbot.core.DiscordBot;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageReaction;

public final class ReactBot extends DiscordBot {

  public void react(Message message) {
    // TODO: implement a meme-reply to `message`.
  }

  public void registerReaction(long messageID, MessageReaction reaction, int factor) {
    // TODO: record how people receive the emitted reaction to tune future reactions.
  }

  @BotCommand("Greets the player")
  public void hello(Message message) {
    reply(message, "hello m8!").now();
  }
}
