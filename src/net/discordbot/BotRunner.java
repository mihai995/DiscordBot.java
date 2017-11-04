package net.discordbot;

import net.discordbot.core.DiscordListener;
import net.discordbot.core.Utils;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.Properties;

public final class BotRunner extends ListenerAdapter {

  private BotRunner() {}

  public static void main(String[] args) throws Exception {
    Properties data = Utils.getData();
    DiscordListener listener = new DiscordListener(data);

    new JDABuilder(AccountType.BOT)
        .setToken(data.getProperty("token"))
        .addEventListener(listener)
        .buildBlocking();
  }
}
