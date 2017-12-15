package net.discordbot.common;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks instance methods that represent commands for the DiscordBot. The string parameter should
 * describe its behavior. The format of the command should be as follows:
 *
 * \@BasicCommand("describe the command here")
 * public void [commandName](Message messageThatTriggeredTheCommand, other args...) {...}
 *
 * At the moment, the only argument types that are allowed are ints, doubles and strings. To change
 * this, please modify `CommandInvoker.CONVERTER`.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface BasicCommand {
  String value();
}
