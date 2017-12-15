package net.discordbot.common;

import net.dv8tion.jda.core.events.Event;

/** Acts on voice channel activity in Discord. */
public interface VoiceListener {

  void processVoiceEvent(Event event);
}
