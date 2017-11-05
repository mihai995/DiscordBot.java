package net.discordbot.core;

import com.google.common.base.Verify;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public final class Utils {

  private Utils() {}

  /**
   * Defines all markers that indicate the beginning of a command. All regex symbols need to be
   * escaped.
   */
  public static final String[] COMMAND_MARKERS = {"sudo +", "!","`","\\\\"};

  /** The location of the resources file. */
  private static final String RESOURCE_PATH = "resources/data.txt";

  private static final String[] EXPECTED_RESOURCES = {"token", "main_channel", "log_channel"};

  /** Returns a Properties object with the data in the resource file. */
  public static Properties getData() throws IOException {
    Properties data = new Properties();
    data.load(new FileInputStream(RESOURCE_PATH));

    // Make sure all expected resources are present.
    for (String resource : EXPECTED_RESOURCES) {
      Verify.verifyNotNull(data.getProperty(resource), "Missing resource \"%s\"!", resource);
    }
    return data;
  }
}
