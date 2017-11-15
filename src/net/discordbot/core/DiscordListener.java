package net.discordbot.core;

import com.google.common.base.Joiner;
import net.discordbot.common.DiscordBot;
import net.discordbot.common.TextListener;
import net.discordbot.common.VoiceListener;
import net.discordbot.util.Config;
import net.dv8tion.jda.client.events.call.voice.CallVoiceJoinEvent;
import net.dv8tion.jda.client.events.call.voice.CallVoiceLeaveEvent;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageReaction;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ExceptionEvent;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.MessageUpdateEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public final class DiscordListener extends ListenerAdapter {

  private final CommandManager cmdManager = new CommandManager();

  private final List<DiscordBot> bots = new ArrayList<>();

  private final List<TextListener> textBots = new ArrayList<>();

  private final List<VoiceListener> voiceBots = new ArrayList<>();

  private final Config cfg;

  public DiscordListener(Config cfg) {
    addBot(cmdManager);
    this.cfg = cfg;
  }

  public DiscordListener addBot(DiscordBot bot) {
    cmdManager.registerCommands(bot);
    bots.add(bot);
    if (bot instanceof TextListener) {
      textBots.add((TextListener) bot);
    }
    if (bot instanceof VoiceListener) {
      voiceBots.add((VoiceListener) bot);
    }
    return this;
  }

  /** Handles the processing of new and old messages. */
  private void processMessage(Message message) {
    // Needs to happen sequentially.
    for (TextListener textBot : textBots) {
      if (textBot.parseMessage(message)) {
        return;
      }
    }
  }

  /** Handles the processing of message reactions. */
  private void processReaction(MessageReaction reaction, int factor) {
    textBots.parallelStream().forEach(bot -> bot.parseReaction(reaction, factor));
  }

  /** Handles the processing of voice channel events. */
  private void processVoiceEvent(Event event) {
    voiceBots.parallelStream().forEach(bot -> bot.processVoiceEvent(event));
  }

  @Override
  public void onMessageReceived(MessageReceivedEvent event) {
    processMessage(event.getMessage());
  }

  @Override
  public void onMessageUpdate(MessageUpdateEvent event) {
    processMessage(event.getMessage());
  }


  @Override
  public void onMessageReactionAdd(MessageReactionAddEvent event) {
    processReaction(event.getReaction(), 1);
  }

  @Override
  public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
    processReaction(event.getReaction(), -1);
  }

  @Override
  public void onReady(ReadyEvent event) {
    JDA jda = event.getJDA();
    bots.parallelStream().forEach(bot -> bot.prepare(jda, cfg));
    getDiscordBot().log("I have respawned.").soon();
  }

  @Override
  public void onCallVoiceJoin(CallVoiceJoinEvent event) {
    processVoiceEvent(event);
  }

  @Override
  public void onCallVoiceLeave(CallVoiceLeaveEvent event) {
    processVoiceEvent(event);
  }

  @Override
  public void onException(ExceptionEvent event) {
    Stream<StackTraceElement> stackTrace = Arrays.stream(event.getCause().getStackTrace());
    getDiscordBot().log("Encountered exception while parsing a message!")
        .say(Joiner.on('\n').join(stackTrace.map(StackTraceElement::toString).toArray(String[]::new)))
        .now();
  }

  /** Returns a DiscordBot that the listener can use to post messages on chat. */
  private DiscordBot getDiscordBot() {
    return bots.get(0);
  }
}
