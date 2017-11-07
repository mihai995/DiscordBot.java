package net.discordbot.bots;

import com.google.common.base.Verify;
import com.google.common.collect.*;
import net.discordbot.common.BasicCommand;
import net.discordbot.common.DiscordBot;
import net.discordbot.common.TextListener;
import net.discordbot.core.Config;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageReaction;
import org.ahocorasick.trie.Trie;

import java.io.File;
import java.util.Collection;
import java.util.Optional;

public final class ReactBot extends DiscordBot implements TextListener {

  private static final int PAST_REACTION_CACHE_SIZE = 100;

  private static final long MAX_IMAGE_CACHE_SIZE = 1 << 26;

  private ImmutableMap<Long, String> memeifiers;

  private ImmutableMultimap<String, File> reactions;

  private Trie textMatcher;

  private File memeFolder;

  @Override
  public void prepare(JDA jda, Config cfg) {
    super.prepare(jda, cfg);
    memeifiers = cfg.getMemeifiers();
    memeFolder = cfg.getMemeFolder();
    loadMemes();
  }

  @Override
  public void parseReaction(MessageReaction reaction, int factor) {
    // TODO: record how people receive the emitted reaction to tune future reactions.
  }

  @Override
  public boolean parseMessage(Message message) {
    Message reaction = postMeme(message.getChannel(), message.getContent());
    // TODO: record reactions posted.
    return reaction != null;
  }

  private void loadMemes() {
    ImmutableCollection<String> owners = memeifiers.values();
    File[] memeFolders = memeFolder.listFiles((parent, dir) -> owners.contains(dir));
    Verify.verify(
        memeFolders != null && memeFolders.length == memeifiers.size(),
        "There are some meme subfolders missing from the meme folder!");

    ImmutableMultimap.Builder<String, File> reactionBuilder = ImmutableMultimap.builder();
    for (File memeFolder : memeFolders) {
      for (File meme : memeFolder.listFiles()) {
        reactionBuilder.put(getMemeKeyword(meme.getName()), meme);
      }
    }
    // TODO: add additional reactions
    reactions = reactionBuilder.build();

    Trie.TrieBuilder matcherBuilder = Trie.builder();
    for (String keyword : reactions.keySet()) {
      matcherBuilder.addKeyword(keyword);
    }
    textMatcher = matcherBuilder.build();
  }

  private static String getMemeKeyword(String name) {
    int pos = name.lastIndexOf('.');
    if (pos != -1) {
      // Assume last `.` marks a file type so remove it from the name.
      name = name.substring(0, pos);
    }
    return name.toLowerCase().replace('_', ' ');
  }

  @BasicCommand("posts a requested meme")
  public void meme(Message msg, String text) {
    postMeme(msg.getChannel(), text);
  }

  private Message postMeme(MessageChannel channel, String text) {
    Optional<File> meme = getExactMeme(text).or(() -> getPartialMeme(text));
    if (meme.isPresent()) {
      return message(channel).addFile(meme.get()).now();
    }
    return null;
  }

  private Optional<File> getExactMeme(String text) {
    return randomElement(reactions.get(text));
  }

  private Optional<File> getPartialMeme(String text) {
    return randomElement(textMatcher.parseText(text)).flatMap(x -> getExactMeme(x.getKeyword()));
  }

  private static <T> Optional<T> randomElement(Collection<T> coll) {
    return coll.stream().skip((int) (Math.random() * coll.size())).findFirst();
  }
}
