package net.discordbot.common;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.IMentionable;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.requests.RestAction;

import javax.annotation.CheckReturnValue;
import java.io.File;
import java.util.Properties;

/**
 * Implements a thin wrapper around a JDA bot for convenience purposes.
 */
public abstract class DiscordBot {

  private TextChannel mainChannel;

  private TextChannel logChannel;

  /** Create a message for the given channel. */
  @CheckReturnValue
  protected ActionBuilder message(MessageChannel channel) {
    return new ActionBuilder(channel);
  }

  /** Create a message for the main channel. */
  @CheckReturnValue
  protected ActionBuilder message() {
    return message(mainChannel);
  }

  /** Create a message for the log channel. */
  @CheckReturnValue
  protected ActionBuilder log() {
    return message(logChannel);
  }

  /** Create a reply for the given `message`. */
  @CheckReturnValue
  protected ActionBuilder reply(Message message) {
    return message(message.getChannel()).mention(message.getAuthor()).say(" ");
  }

  @CheckReturnValue
  protected ActionBuilder message(MessageChannel channel, String format, Object... args) {
    return message(channel).say(format, args);
  }

  @CheckReturnValue
  protected ActionBuilder message(String format, Object... args) {
    return message().say(format, args);
  }

  @CheckReturnValue
  public ActionBuilder log(String format, Object... args) {
    return log().say(format, args);
  }

  @CheckReturnValue
  protected ActionBuilder reply(Message message, String format, Object... args) {
    return reply(message).say(format, args);
  }

  public void prepare(JDA jda, Properties data) {
    mainChannel = jda.getTextChannelById(data.getProperty("main_channel"));
    logChannel = jda.getTextChannelById(data.getProperty("log_channel"));
  }

  public static final class ActionBuilder {

    private final MessageBuilder message;

    private final MessageChannel channel;

    private File file;

    private ActionBuilder(MessageChannel channel) {
      message = new MessageBuilder();
      this.channel = channel;
    }

    @CheckReturnValue
    public ActionBuilder addFile(File file) {
      this.file = file;
      return this;
    }

    @CheckReturnValue
    public ActionBuilder say(String format, Object... args) {
      message.appendFormat(format, args);
      return this;
    }

    @CheckReturnValue
    public ActionBuilder mention(IMentionable mention) {
      message.append(mention);
      return this;
    }

    @CheckReturnValue
    private RestAction<Message> execute() {
      Message message = this.message.build();
      return file != null ? channel.sendFile(file, message) : channel.sendMessage(message);
    }

    public void now() {
      execute().complete();
    }

    public void soon() {
      execute().queue();
    }
  }
}
