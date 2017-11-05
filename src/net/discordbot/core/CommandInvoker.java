package net.discordbot.core;

import com.google.common.base.Joiner;
import com.google.common.base.Verify;
import net.dv8tion.jda.core.entities.Message;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Defines and runs DiscordBot commands. */
public final class CommandInvoker {

  /**
   * Maps classes to their converter from a string.
   */
  private static final Map<Class, Function<String, Object>> CONVERTER = new HashMap<>();

  private final List<Function<String, Object>> types = new ArrayList<>();

  private final DiscordBot bot;

  private final Method method;

  private final String name;

  public CommandInvoker(DiscordBot bot, Method method, String name) {
    this.bot = bot;
    this.method = method;
    this.name = name;
    int modifiers = method.getModifiers();

    // Ensure method has @BotCommand annotation.
    Verify.verifyNotNull(
        method.getAnnotation(BotCommand.class),
        "%d must be annotated with @BotCommand!",
        method);

    // Ensure method is public and non-static.
    Verify.verify(Modifier.isPublic(modifiers), "%s is not public!", method);
    Verify.verify(!Modifier.isStatic(modifiers), "%s is static!", method);

    // Ensure first parameter is a message and remaining ones are permitted.
    Class[] parameters = Verify.verifyNotNull(method.getParameterTypes());
    Verify.verify(
        parameters.length > 0 && parameters[0].equals(Message.class),
        "First argument of %s should be a Message type.",
        method);
    Arrays.stream(parameters).skip(1).forEach(
        clazz -> types.add(
            Verify.verifyNotNull(
                CONVERTER.get(clazz),
                "Parameters of type %s are not allowed in DiscordBot commands.",
                 clazz.getName())));
  }

  /** Returns true if `method` represents a DiscordBot command. */
  public static boolean isCommand(Method method) {
    return method.getAnnotation(BotCommand.class) != null;
  }

  /**
   * Runs command on the given string argument (broken into words). Returns true if successful.
   */
  public boolean run(Message message, String argument) {
    ArrayList<Object> params = new ArrayList<>();
    params.add(message);

    if (!fill(params, argument)) {
      return false;
    }
    try {
      method.invoke(bot, params.toArray());
      // message.delete().reason("Command executed").complete(); TODO: reintroduce this.
    } catch (IllegalAccessException | InvocationTargetException e) {
      // Invocation failed. Mark failure and proceed.
      e.printStackTrace(System.err);
      return false;
    }
    return true;
  }

  /**
   * Extracts arguments out of `argument` and converts them into parameters. Returns true if
   * successful.
   */
  private boolean fill(ArrayList<Object> params, String argument) {
    if (argument.isEmpty() || types.isEmpty()) {
      // Can successfully fill only if there is nothing to fill.
      return argument.isEmpty() && types.isEmpty();
    }
    String[] args = argument.split("\\W+", types.size());
    if (args.length != types.size()) {
      // `argument` does not have enough words.
      return false;
    }
    for (int i = 0; i < types.size(); i++) {
      try {
        params.add(types.get(i).apply(args[i]));
      } catch (Throwable e) {
        e.printStackTrace(System.err);
        return false;
      }
    }
    return true;
  }

  public String getDescription() {
    return String.format(
        "%s(%s): %s",
        name,
        Joiner.on(", ").join(
            Arrays.stream(method.getParameterTypes()).skip(1).map(Class::getName).iterator()),
        method.getAnnotation(BotCommand.class).value());
  }

  static {
    CONVERTER.put(String.class, x -> x);
    CONVERTER.put(Integer.class, Integer::decode);
    CONVERTER.put(Double.class, Double::parseDouble);
  }
}
