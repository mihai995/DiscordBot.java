package net.discordbot.util;

import com.google.auto.value.AutoValue;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import org.ini4j.Ini;

import java.io.File;
import java.io.IOException;

@AutoValue
public abstract class Config {

  public static Config create(String configPath) {
    Ini cfg = new Ini();
    File file = new File(configPath);
    try {
      cfg.load(file);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not find config file " + configPath, e);
    }

    Ini.Section credentials = getValidatedSection(cfg, "CREDENTIALS", "bot_name", "token");
    Ini.Section channels = getValidatedSection(cfg, "CHANNELS", "main", "log");
    Ini.Section memeifiers = getValidatedSection(cfg, "MEMEIFIERS");
    Ini.Section reactions = getValidatedSection(cfg, "REACTIONS");
    Ini.Section others = getValidatedSection(cfg, "OTHERS", "meme_folder", "persistence_file");

    ImmutableMap.Builder<Long, String> memeifierList = ImmutableMap.builder();
    memeifiers.forEach((name, id) -> memeifierList.put(Long.parseLong(id), name));

    ImmutableMap.Builder<String, Integer> reactionScores = ImmutableMap.builder();
    reactions.forEach((name, score) -> reactionScores.put(name, Integer.parseInt(score)));

    return new net.discordbot.util.AutoValue_Config(
        credentials.get("bot_name"),
        credentials.get("token"),
        Long.parseLong(channels.get("main")),
        Long.parseLong(channels.get("log")),
        getFile(file.getParentFile(), others.get("meme_folder")),
        getFile(file.getParentFile(), others.get("persistence_file")),
        memeifierList.build(),
        reactionScores.build());
  }

  private static File getFile(File root, String fileName) {
    return new File(String.format("%s%s%s", root.getAbsolutePath(), File.separator, fileName));
  }

  private static Ini.Section getValidatedSection(Ini cfg, String sectionName, String... args) {
    Ini.Section section = cfg.get(sectionName);
    Verify.verifyNotNull(section, "Section \"%s\" is missing from the config file!", sectionName);
    for (String arg : args) {
      Verify.verifyNotNull(
          section.get(arg), "Section \"%s\" is missing argument \"%s\"", sectionName, arg);
    }
    return section;
  }

  public abstract String getName();

  public abstract String getToken();

  public abstract long getMainChannelID();

  public abstract long getLogChannelID();

  public abstract File getMemeFolder();

  public abstract File getPersistenceFile();

  public abstract ImmutableMap<Long, String> getMemeifiers();

  public abstract ImmutableMap<String, Integer> getReactionScores();
}
