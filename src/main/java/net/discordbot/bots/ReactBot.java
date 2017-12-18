package net.discordbot.bots;

import com.google.common.base.Verify;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import net.discordbot.common.BasicCommand;
import net.discordbot.common.DiscordBot;
import net.discordbot.common.TextListener;
import net.discordbot.util.Config;
import net.discordbot.util.PersistenceManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageReaction;
import org.ahocorasick.trie.Trie;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReactBot extends DiscordBot implements TextListener {

  private static final int PAST_REACTION_CACHE_SIZE = 100;

  /**
   * The default chance of posting a meme with reaction score of 0.
   */
  private static final double DEFAULT_CHANCE = 0.9;

  /**
   * The smallest chance a meme can have to be posted.
   */
  private static final double MIN_CHANCE = 0.01;

  private static final Pattern MEME_FILE_PATTERN = Pattern.compile("[a-z_]+[0-9]*\\.(jpg|png|gif)");

  private static final Pattern COMMA_SPLIT = Pattern.compile(", *");

  /**
   * Format for react file lines.
   */
  private static final Pattern REACT_LINE_PATTERN =
      Pattern.compile(String.format("(%s): ([a-z ,]*)", MEME_FILE_PATTERN.pattern()));

  private final Cache<Long, Reaction> reactionCache =
      CacheBuilder.newBuilder().maximumSize(PAST_REACTION_CACHE_SIZE).build();

  private ImmutableMap<String, Integer> emoteScores;

  private ImmutableMap<Long, String> memeifiers;

  private ImmutableMultimap<String, Reaction> reactions;

  private PersistenceManager<ConcurrentHashMap<File, AtomicInteger>> reactionWeights;

  private Trie textMatcher;

  private File memeFolder;

  private static String getMemeKeyword(String name) {
    int pos = name.lastIndexOf('.');
    if (pos != -1) {
      // Assume last `.` marks a file type so remove it from the name.
      name = name.substring(0, pos);
    }
    return name.toLowerCase().replace('_', ' ');
  }

  private static <T> Optional<T> randomElement(Collection<T> coll) {
    return coll.stream().skip((int) (Math.random() * coll.size())).findFirst();
  }

  private static double weightFunction(int w) {
    return Math.max(MIN_CHANCE, DEFAULT_CHANCE * Math.exp(0.1 * w));
  }

  @Override
  public void prepare(JDA jda, Config cfg) {
    super.prepare(jda, cfg);
    memeifiers = cfg.getMemeifiers();
    memeFolder = cfg.getMemeFolder();
    loadMemes();
    emoteScores = cfg.getReactionScores();
    reactionWeights = new PersistenceManager<>(cfg.getPersistenceFile(), new ConcurrentHashMap<>());
  }

  @Override
  public void parseReaction(MessageReaction reaction, int factor) {
    Reaction react = reactionCache.getIfPresent(reaction.getMessageIdLong());
    if (react != null) {
      react.recordReaction(reaction.getEmote().getEmote(), factor);
    }
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
    HashMap<String, Reaction> reactions = new HashMap<>();
    HashSet<File> reactFiles = new HashSet<>();
    for (File memeFolder : memeFolders) {
      for (File meme : memeFolder.listFiles()) {
        if (meme.getName().equals("react.txt")) {
          reactFiles.add(meme);
        } else {
          Reaction reaction = new Reaction(meme);
          reactions.put(meme.getName(), reaction);
          reactionBuilder.put(getMemeKeyword(meme.getName()), reaction);
        }
      }
    }
    try {
      for (File reactFile : reactFiles) {
        Files.lines(reactFile.toPath()).forEach(
            line -> {
              Matcher matcher = REACT_LINE_PATTERN.matcher(line);
              if (!matcher.matches()) {
                return;
              }
              String memeFile = matcher.group(1);
              Reaction reaction =
                  Verify.verifyNotNull(
                      reactions.get(memeFile), "Meme file %s not found", memeFile);
              for (String reactionPattern : COMMA_SPLIT.split(matcher.group(3))) {
                reactionBuilder.put(reactionPattern, reaction);
              }
            });
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    this.reactions = reactionBuilder.build();

    Trie.TrieBuilder matcherBuilder = Trie.builder().ignoreCase();
    for (String keyword : reactions.keySet()) {
      matcherBuilder.addKeyword(keyword);
    }
    textMatcher = matcherBuilder.build();
  }

  @BasicCommand("adds a new meme to the collection")
  public void memeify(Message msg, String argument) {
    // TODO: implement
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

  private final class Reaction {

    private final File reactFile;

    Reaction(File reactFile) {
      this.reactFile = reactFile;
    }

    private void post(MessageChannel channel, boolean bypassFilter) {
      if (bypassFilter || Math.random() <= getWeight()) {
        long reactID = message(channel).addFile(reactFile).now().getIdLong();
        reactionCache.put(reactID, this);
      }
    }

    private void recordReaction(Emote emote, int factor) {
      if (emote != null) {
        factor *= emoteScores.getOrDefault(emote.getName(), 0);
        if (factor != 0) {
          AtomicInteger weight = new AtomicInteger(0);
          weight = Optional.ofNullable(reactionWeights.get().putIfAbsent(reactFile, weight)).orElse(weight);
          weight.getAndAdd(factor);
        }
      }
    }

    private double getWeight() {
      AtomicInteger weight = reactionWeights.get().getOrDefault(reactFile, new AtomicInteger(0));
      return weightFunction(weight.get());
    }
  }
}
