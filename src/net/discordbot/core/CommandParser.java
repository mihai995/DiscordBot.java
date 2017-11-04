package net.discordbot.core;

import com.google.common.base.Joiner;
import com.google.common.base.Verify;
import net.dv8tion.jda.core.entities.Message;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandParser extends DiscordBot {

  private static final Pattern IS_COMMAND =
      Pattern.compile(
          String.format("(%s)([a-zA-Z]+)(.*)", Joiner.on('|').join(Utils.COMMAND_MARKERS)));

  private static final Joiner WORD_SEPARATOR = Joiner.on(", ");

  private final Map<String, Method> commands = new HashMap<>();

  private final Map<String, DiscordBot> bots = new HashMap<>();

  public void registerCommands(DiscordBot bot) {
    for (Method method : bot.getClass().getDeclaredMethods()) {
      if (getDescription(method) != null) {
        registerCommand(bot, method);
      }
    }
  }

  /**
   * Adds a new `command` to the registry. Upon encountering it, the command bot will run
   * `bot`.`method`(arguments).
   */
  public void registerCommand(DiscordBot bot, Method method) {
    String command = method.getName();
    int modifiers = method.getModifiers();

    Verify.verify(Modifier.isPublic(modifiers), "%s is not public!", command);
    Verify.verify(!Modifier.isStatic(modifiers), "%s is static!", command);
    Verify.verify(!commands.containsKey(command), "Duplicate command %s!", command);
    Verify.verify(method.getParameterTypes()[0].equals(Message.class));

    commands.put(command, method);
    bots.put(command, bot);
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

    // Split the command line into "[command] [arg_1] [arg_2] ... [arg_n]".
    String commandName = commandMatcher.group(2);
    String[] commandArgument =
        Arrays.stream(commandMatcher.group(3).split("\\W+"))
            .filter(x -> !x.isEmpty())
            .toArray(String[]::new);

    Method commandRunner = commands.get(commandName);
    if (commandRunner == null || commandRunner.getParameterCount() - 1 != commandArgument.length) {
      reply(message, "Could not resolve command \"%s\"!", commandName).now();
      help(message);
      return true;
    }
    DiscordBot bot = Verify.verifyNotNull(bots.get(commandName));

    Class[] type = commandRunner.getParameterTypes();
    Object[] arguments = new Object[type.length];
    arguments[0] = message;
    for (int i = 0; i < commandArgument.length; i++) {
      arguments[i + 1] = Utils.convert(commandArgument[i], type[i + 1]);
    }
    try {
      commandRunner.invoke(bot, arguments);
      // message.delete().reason("Command executed").complete(); TODO: reintroduce this.
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException("Impossible due to explicit method collection", e);
    }
    return true;
  }

  @BotCommand("prints list of available commands")
  public void help(Message message) {
    ActionBuilder action = reply(message);
    for (Map.Entry<String, Method> entry : commands.entrySet()) {
      Method method = entry.getValue();
      action.say("\n%s(%s): %s", entry.getKey(), getSignature(method), getDescription(method));
    }
    action.now();
  }

  private static String getDescription(Method method) {
    BotCommand description = method.getAnnotation(BotCommand.class);
    return description != null ? description.value() : null;
  }

  private static String getSignature(Method method) {
    return WORD_SEPARATOR.join(
        Arrays.stream(method.getParameterTypes()).skip(1).map(clazz -> clazz.getName()).iterator());
  }
}
