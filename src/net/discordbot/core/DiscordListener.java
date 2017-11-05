package net.discordbot.core;

import com.google.common.base.Joiner;
import net.discordbot.bots.MusicBot;
import net.discordbot.bots.ReactBot;
import net.dv8tion.jda.client.events.call.voice.CallVoiceJoinEvent;
import net.dv8tion.jda.client.events.call.voice.CallVoiceLeaveEvent;
import net.dv8tion.jda.core.entities.Message;
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
import java.util.Properties;
import java.util.stream.Stream;

public final class DiscordListener extends ListenerAdapter {

  private final CommandParser cmdParser = new CommandParser();

  private final MusicBot musicBot = new MusicBot();

  private final ReactBot reactBot = new ReactBot();

  private final List<DiscordBot> bots = new ArrayList<>();

  private final Properties data;

  public DiscordListener(Properties data) {
    addBot(cmdParser);
    addBot(musicBot);
    addBot(reactBot);
    this.data = data;
  }

  public DiscordListener addBot(DiscordBot bot) {
    cmdParser.registerCommands(bot);
    bots.add(bot);
    return this;
  }

  @Override
  public void onMessageReceived(MessageReceivedEvent event) {
    parseMessage(event.getMessage());
  }

  @Override
  public void onMessageUpdate(MessageUpdateEvent event) {
    parseMessage(event.getMessage());
  }

  private void parseMessage(Message message) {
    if (!cmdParser.parseCommand(message)) {
      reactBot.react(message);
    }
  }

  @Override
  public void onMessageReactionAdd(MessageReactionAddEvent event) {
    reactBot.registerReaction(event.getMessageIdLong(), event.getReaction(), 1);
  }

  @Override
  public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
    reactBot.registerReaction(event.getMessageIdLong(), event.getReaction(), -1);
  }

  @Override
  public void onReady(ReadyEvent event) {
    for (DiscordBot bot : bots) {
      bot.prepare(event.getJDA(), data);
    }
    cmdParser.log("I have respawned.").now();
  }

  @Override
  public void onCallVoiceJoin(CallVoiceJoinEvent event) {
    musicBot.refreshState();
  }

  @Override
  public void onCallVoiceLeave(CallVoiceLeaveEvent event) {
    musicBot.refreshState();
  }

  @Override
  public void onException(ExceptionEvent event) {
    Stream<StackTraceElement> stackTrace = Arrays.stream(event.getCause().getStackTrace());
    cmdParser.log("Encountered exception while parsing a message!")
        .say(Joiner.on('\n').join(stackTrace.map(StackTraceElement::toString).toArray(String[]::new)))
        .now();
  }

}
