package net.discordbot.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks instance methods that represent commands for the DiscordBot. The string parameter should
 * describe its behavior. The format of the command should be as follows:
 *
 * @BotCommand("describe the command here")
 * public void [commandName](Message messageThatTriggeredTheCommand, other args...) {...}
 *
 * At the moment, the only argument types that are allowed are int, double and string. To change
 * this, please modify Utils.covert().
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface BotCommand {
  String value();
}
