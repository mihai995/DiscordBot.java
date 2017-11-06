package net.discordbot.common;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageReaction;

/** Acts on message activity in Discord. */
public interface TextListener {

  /** Parses a message and returns true if there is a hit and a response was issued. */
  default boolean parseMessage(Message message) {
    return false;
  }

  /** Parses a reaction to a message. */
  default void parseReaction(MessageReaction reaction, int factor) {}
}
