package net.discordbot.core;

import com.google.common.base.Joiner;
import com.google.common.base.Verify;
import net.dv8tion.jda.core.entities.Message;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandParser extends DiscordBot {

  private static final Pattern IS_COMMAND =
      Pattern.compile(
          String.format("(%s)([a-zA-Z]+)(.*)", Joiner.on('|').join(Utils.COMMAND_MARKERS)));

  private final Map<String, CommandInvoker> commands = new HashMap<>();

  /** Adds all DiscordBot commands held by the bot instance. */
  public void registerCommands(DiscordBot bot) {
    for (Method method : bot.getClass().getDeclaredMethods()) {
      if (CommandInvoker.isCommand(method)) {
        String name = method.getName().toLowerCase();
        CommandInvoker oldCmd = commands.putIfAbsent(name, new CommandInvoker(bot, method, name));
        Verify.verify(oldCmd == null, "Defined duplicate command %s!", name);
      }
    }
  }

  /**
   * Parses `message` and runs the registered commands on it and returns whether a command was
   * parsed or not.
   */
  public boolean parseCommand(Message message) {
    Matcher commandMatcher = IS_COMMAND.matcher(message.getContent());
    if (!commandMatcher.matches()) {
      // Message does not have the command format. Abort.
      return false;
    }

    CommandInvoker command = commands.get(commandMatcher.group(2).toLowerCase());
    if (command == null || !command.run(message, commandMatcher.group(3))) {
      reply(message)
          .say("Could not resolve command \"%s\"! ", commandMatcher.group(2))
          .say("Run *help* for a list of available commands")
          .now();
    }
    return true;
  }

  @BotCommand("writes this list")
  public void help(Message message) {
    ActionBuilder action = reply(message);
    for (CommandInvoker cmd : commands.values()) {
      action = action.say("\n%s", cmd.getDescription());
    }
    action.now();
  }
}
