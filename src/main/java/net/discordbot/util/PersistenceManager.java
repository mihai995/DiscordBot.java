package net.discordbot.util;

import java.io.*;

public final class PersistenceManager<T extends Serializable> {

  private static final long STORE_COOLDOWN_MILLISECONDS = 120000;

  private T object;

  private final File file;

  public PersistenceManager(File file, T defaultInstance) {
    this.file = file;
    if (!file.exists()) {
      store(defaultInstance);
    }
    load();
    new PeriodicStoreTask(STORE_COOLDOWN_MILLISECONDS).start();
  }

  public synchronized void load() {
    try {
      ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
      object = (T) in.readObject();
      in.close();
    } catch (IOException | ClassNotFoundException e) {
      throw new IllegalStateException("Encountered exception while loading", e);
    }
  }

  public void store() {
    store(object);
  }

  private synchronized void store(T object) {
    try {
      ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
      out.writeObject(object);
      out.close();
    } catch (IOException e) {
      throw new IllegalStateException("Encountered exception while storing", e);
    }
  }

  /** Wrapper around the object that is meant to prevent reads during load / store. */
  public synchronized T get() {
    return object;
  }

  /** Periodically updates */
  private final class PeriodicStoreTask extends Thread {

    private final long timeoutMilis;

    private PeriodicStoreTask(long timeout) {
      this.timeoutMilis = timeout;
    }

    @Override
    public void run() {
      while(true) {
        store();
        try {
          sleep(timeoutMilis);
        } catch (InterruptedException e) {
          // This is loose requirement. Silently fail.
        }
      }
    }
  }
}
