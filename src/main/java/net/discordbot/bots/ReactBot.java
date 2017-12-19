package net.discordbot.bots;

import com.google.common.base.Verify;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.*;
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
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReactBot extends DiscordBot implements TextListener {

  private static final int PAST_REACTION_CACHE_SIZE = 100;

  /** The default chance of posting a meme with reaction score of 0. */
  private static final double DEFAULT_CHANCE = 0.9;

  /** The smallest chance a meme can have to be posted. */
  private static final double MIN_CHANCE = 0.01;

  private static final Pattern MEME_FILE_PATTERN =
      Pattern.compile("([a-z_']+)[0-9]*\\.(jpg|png|gif)");

  private static final Pattern COMMA_SPLIT = Pattern.compile(", *");

  /** Format for react file lines. */
  private static final Pattern REACT_LINE_PATTERN =
      Pattern.compile(String.format("(%s): ([a-z ,]*)", MEME_FILE_PATTERN.pattern()));

  /** Cache containing all recent reactions posted by the RaectBot. */
  private final Cache<Long, Reaction> reactionCache =
      CacheBuilder.newBuilder().maximumSize(PAST_REACTION_CACHE_SIZE).build();

  /** Reactions used for feedback to reactions. */
  private ImmutableMap<String, Integer> emoteReactionScores;

  private ImmutableMap<Long, String> memeifiers;

  private final Multimap<String, Reaction> reactions = HashMultimap.create();

  private final Map<String, Reaction> reactionMemes = new HashMap<>();

  private PersistenceManager<ConcurrentHashMap<File, AtomicInteger>> reactionWeights;

  private Trie textMatcher;

  private File memeFolder;

  private static String getMemeKeyword(String name) {
    Matcher matcher = MEME_FILE_PATTERN.matcher(name);
    Verify.verify(matcher.matches(), "Meme file name %s is malformed", name);
    return matcher.group(1).replace('_', ' ');
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
    try {
      loadMemes();
    } catch (IOException e) {
      throw new IllegalStateException("Encountered unexpected error while loading memes", e);
    }
    emoteReactionScores = cfg.getReactionScores();
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

  private void loadMemes() throws IOException {
    HashSet<File> reactFiles = new HashSet<>();
    Files.walk(memeFolder.toPath(), 2).map(Path::toFile).filter(File::isFile).forEach(
        meme -> {
          if (meme.getName().equals("react.txt")) {
            reactFiles.add(meme);
          } else {
            addMemeFile(meme);
          }
        }
    );
    for (File reactFile : reactFiles) {
      Files.lines(reactFile.toPath()).forEach(this::addMemeReaction);
    }
    Trie.TrieBuilder matcherBuilder = Trie.builder().ignoreCase();
    reactions.keySet().forEach(matcherBuilder::addKeyword);
    textMatcher = matcherBuilder.build();
  }

  private void addMemeFile(File meme) {
    Reaction reaction = new Reaction(meme);
    reactionMemes.put(meme.getName(), reaction);
    reactions.put(getMemeKeyword(meme.getName()), reaction);
  }

  private boolean addMemeReaction(String line) {
    Matcher matcher = REACT_LINE_PATTERN.matcher(line);
    if (!matcher.matches()) {
      return false;
    }
    String memeFile = matcher.group(1);
    Reaction reaction =
        Verify.verifyNotNull(
            reactionMemes.get(memeFile), "Meme file %s not found", memeFile);
    for (String reactionPattern : COMMA_SPLIT.split(matcher.group(4))) {
      reactions.put(reactionPattern, reaction);
    }
    return true;
  }

  @BasicCommand("adds a new meme to the collection")
  public void memeify(Message msg, String memeName, String aliases) {
    String author = memeifiers.get(msg.getAuthor().getIdLong());
    if (author == null) {
      reply(msg, "you are not a meme master!").now();
      return;
    }
    File meme = validateMemeFile(new File(memeFolder, author), memeName);
    if (meme == null || !meme.isFile()) {
      reply(msg, "your memeify request is malformed").now();
      return;
    }
    addMemeReaction(String.format("%s: %s", meme.getName(), aliases));
  }

  private File validateMemeFile(File memeFolder, String memeName) {
    if (!memeFolder.isDirectory() || !memeName.endsWith(":")) {
      return null;
    }
    memeName = memeName.substring(0, memeName.length() - 1);
    // TODO: implement this
    return null;
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
        factor *= emoteReactionScores.getOrDefault(emote.getName(), 0);
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
