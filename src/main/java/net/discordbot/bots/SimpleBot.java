package net.discordbot.bots;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import net.discordbot.common.BasicCommand;
import net.discordbot.common.DiscordBot;
import net.discordbot.util.Config;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;

import java.io.File;
import java.util.Random;

public class SimpleBot extends DiscordBot {

  private ImmutableMap<Integer, File> diceRoll;

  private final Random randomNumberGenerator = new Random();

  @Override
  public void prepare(JDA jda, Config cfg) {
    super.prepare(jda, cfg);
    ImmutableMap<String, File> images = cfg.getImages();
    ImmutableMap.Builder<Integer, File> diceRolls = ImmutableMap.builder();
    for (int i = 1; i <= 6; i++) {
      String fileName = String.format("die%d.png", i);
      diceRolls.put(i, Verify.verifyNotNull(images.get(fileName), "Missing " + fileName));
    }
    diceRoll = diceRolls.build();
  }

  @BasicCommand("Greets the player")
  public void hello(Message message) {
    reply(message, "hello m8!").now();
  }

  @BasicCommand("Rolls a die")
  public void roll(Message message) {
    int value = 1 + randomNumberGenerator.nextInt(6);
    reply(message, "Behold the result of the demonic die roll")
        .addFile(diceRoll.get(value))
        .soon();
  }

  @BasicCommand("Random number generator in [lo; hi)")
  public void rng(Message message, String bounds) {
    int mid = bounds.indexOf(':');
    int lo, hi;
    if (mid == -1) {
      lo = 1;
    } else {
      lo = Integer.parseInt(bounds.substring(0, mid));
      bounds = bounds.substring(mid + 1);
    }
    hi = Integer.parseInt(bounds);
    int value = randomNumberGenerator.nextInt(hi - lo) + lo;
    reply(message, "I have drawn ***%d*** out of my demonic hat", value).now();
  }
}
