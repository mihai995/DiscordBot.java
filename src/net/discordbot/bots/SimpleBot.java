package net.discordbot.bots;

import net.discordbot.common.BasicCommand;
import net.discordbot.common.DiscordBot;
import net.dv8tion.jda.core.entities.Message;

public class SimpleBot extends DiscordBot {

  @BasicCommand("Greets the player")
  public void hello(Message message) {
    reply(message, "hello m8!").now();
  }
}
