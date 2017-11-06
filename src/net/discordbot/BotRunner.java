package net.discordbot;

import net.discordbot.bots.MusicBot;
import net.discordbot.bots.ReactBot;
import net.discordbot.bots.SimpleBot;
import net.discordbot.core.DiscordListener;
import net.discordbot.core.Utils;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;

import java.util.Properties;

public final class BotRunner {

  private BotRunner() {}

  public static void main(String[] args) throws Exception {
    Properties data = Utils.getData();

    // Add all DiscordBots to the listener. Bots that are listed earlier have higher priority.
    DiscordListener listener = new DiscordListener(data)
        .addBot(new ReactBot())
        .addBot(new MusicBot())
        .addBot(new SimpleBot());

    new JDABuilder(AccountType.BOT)
        .setToken(data.getProperty("token"))
        .addEventListener(listener)
        .buildBlocking();
  }
}
