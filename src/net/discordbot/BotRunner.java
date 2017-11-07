package net.discordbot;

import net.discordbot.bots.MusicBot;
import net.discordbot.bots.ReactBot;
import net.discordbot.bots.SimpleBot;
import net.discordbot.core.Config;
import net.discordbot.core.DiscordListener;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;

public final class BotRunner {

  /** The location of the config file. */
  private static final String CONFIG_FILE_PATH = "resources/data.txt";

  private BotRunner() {}

  public static void main(String[] args) throws Exception {
    Config cfg = Config.create(CONFIG_FILE_PATH);

    // Add all DiscordBots to the listener. Bots that are listed earlier have higher priority.
    DiscordListener listener = new DiscordListener(cfg)
        .addBot(new ReactBot())
        .addBot(new MusicBot())
        .addBot(new SimpleBot());

    new JDABuilder(AccountType.BOT)
        .setToken(cfg.getToken())
        .addEventListener(listener)
        .buildBlocking();
  }
}
