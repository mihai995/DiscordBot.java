package net.discordbot.core;

import com.google.auto.value.AutoValue;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import org.ini4j.Ini;

import java.io.File;
import java.io.IOException;

@AutoValue
public abstract class Config {

  public abstract String getName();

  public abstract String getToken();

  public abstract long getMainChannelID();

   public abstract long getLogChannelID();

  public abstract ImmutableMap<Long, String> getMemeifiers();

  public static Config create(String path) {
    Ini cfg = new Ini();
    try {
      cfg.load(new File(path));
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not find config file " + path, e);
    }

    Ini.Section credentials = getValidatedSection(cfg, "CREDENTIALS", "bot_name", "token");
    Ini.Section channels = getValidatedSection(cfg, "CHANNELS", "main", "log");
    Ini.Section memeifiers = getValidatedSection(cfg, "MEMEIFIERS");

    ImmutableMap.Builder memeifierList = ImmutableMap.builder();
    memeifiers.forEach((name, id) -> memeifierList.put(Long.parseLong(id), name));

    return new AutoValue_Config(
        credentials.get("bot_name"),
        credentials.get("token"),
        Long.parseLong(channels.get("main")),
        Long.parseLong(channels.get("log")),
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
}
