package net.discordbot.bots;

import com.google.common.base.Verify;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.*;
import net.discordbot.common.BasicCommand;
import net.discordbot.common.DiscordBot;
import net.discordbot.common.TextListener;
import net.discordbot.core.Config;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageReaction;
import org.ahocorasick.trie.Trie;

import java.io.File;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public final class ReactBot extends DiscordBot implements TextListener {

  private static final int PAST_REACTION_CACHE_SIZE = 100;

  private final Cache<Long, Reaction> reactionCache =
      CacheBuilder.newBuilder().maximumSize(PAST_REACTION_CACHE_SIZE).build();

  private ImmutableMap<String, Integer>  emoteScores;

  private ImmutableMap<Long, String> memeifiers;

  private ImmutableMultimap<String, Reaction> reactions;

  private Trie textMatcher;

  private File memeFolder;

  @Override
  public void prepare(JDA jda, Config cfg) {
    super.prepare(jda, cfg);
    memeifiers = cfg.getMemeifiers();
    memeFolder = cfg.getMemeFolder();
    loadMemes();
    emoteScores = cfg.getReactionScores();
  }

  @Override
  public void parseReaction(MessageReaction reaction, int factor) {
    Reaction react = reactionCache.getIfPresent(reaction.getMessageIdLong());
    if (react != null) {
      react.recordReaction(reaction.getEmote().getEmote(), factor);
    }
    // TODO: record how people receive the emitted reaction to tune future reactions.
  }

  @Override
  public boolean parseMessage(Message message) {
    return postMeme(message.getChannel(), message.getContent(), false);
  }

  private void loadMemes() {
    ImmutableCollection<String> owners = memeifiers.values();
    File[] memeFolders = memeFolder.listFiles((parent, dir) -> owners.contains(dir));
    Verify.verify(
        memeFolders != null && memeFolders.length == memeifiers.size(),
        "There are some meme subfolders missing from the meme folder!");

    ImmutableMultimap.Builder<String, Reaction> reactionBuilder = ImmutableMultimap.builder();
    for (File memeFolder : memeFolders) {
      for (File meme : memeFolder.listFiles()) {
        reactionBuilder.put(getMemeKeyword(meme.getName()), new Reaction(meme));
      }
    }
    // TODO: add additional reactions
    reactions = reactionBuilder.build();

    Trie.TrieBuilder matcherBuilder = Trie.builder().ignoreCase();
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
    postMeme(msg.getChannel(), text, true);
  }

  private boolean postMeme(MessageChannel channel, String text, boolean bypassFilter) {
    String lowercaseText = text.toLowerCase();
    Optional<Reaction> meme = getExactMeme(text).or(() -> getPartialMeme(lowercaseText));
    meme.ifPresent(x -> x.post(channel, bypassFilter));
    return meme.isPresent();
  }

  private Optional<Reaction> getExactMeme(String text) {
    return randomElement(reactions.get(text));
  }

  private Optional<Reaction> getPartialMeme(String text) {
    return randomElement(textMatcher.parseText(text)).flatMap(x -> getExactMeme(x.getKeyword()));
  }

  private static <T> Optional<T> randomElement(Collection<T> coll) {
    return coll.stream().skip((int) (Math.random() * coll.size())).findFirst();
  }

  private final class Reaction {

    private final AtomicInteger rating = new AtomicInteger(0);

    private final File reactFile;

    Reaction(File reactFile) {
      this.reactFile = reactFile;
    }

    private void post(MessageChannel channel, boolean bypassFilter) {
      double threshold = 1 / (1 + Math.exp(-rating.get() / memeifiers.size()));
      if (bypassFilter || Math.random() <= threshold) {
        long reactID = message(channel).addFile(reactFile).now().getIdLong();
        reactionCache.put(reactID, this);
      }
    }

    private void recordReaction(Emote emote, int factor) {
      if (emote != null) {
        factor *= emoteScores.getOrDefault(emote.getName(), 0);
        rating.addAndGet(factor);
      }
    }

    private void updateRating(int factor) {
      rating.addAndGet(factor);
    }
  }
}
