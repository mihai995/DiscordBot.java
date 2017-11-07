package net.discordbot.core;

import com.google.auto.value.AutoValue;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.ini4j.Ini;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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
    Ini.Section others = getValidatedSection(cfg, "OTHERS", "meme_folder");

    ImmutableMap.Builder<Long, String> memeifierList = ImmutableMap.builder();
    memeifiers.forEach((name, id) -> memeifierList.put(Long.parseLong(id), name));

    String desiredName = others.get("meme_folder");
    File[] memeFolderOptions =
        Verify.verifyNotNull(
            file.getParentFile().listFiles((dir, name) -> name.equals(desiredName)));
    File memeFolder = Iterables.getOnlyElement(Arrays.asList(memeFolderOptions));

    return new net.discordbot.core.AutoValue_Config(
        credentials.get("bot_name"),
        credentials.get("token"),
        Long.parseLong(channels.get("main")),
        Long.parseLong(channels.get("log")),
        memeFolder,
        memeifierList.build());
  }

  private static Ini.Section getValidatedSection(Ini cfg, String sectionName, String... args) {
    Ini.Section section = cfg.get(sectionName);
    Verify.verifyNotNull(section, "Section \"%s\" is missing from the config file!");
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

  public abstract ImmutableMap<Long, String> getMemeifiers();
}
