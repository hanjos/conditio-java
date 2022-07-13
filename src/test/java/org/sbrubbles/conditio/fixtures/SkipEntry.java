package org.sbrubbles.conditio.fixtures;

import org.sbrubbles.conditio.Restart;

public class SkipEntry implements Restart.Option {
  public SkipEntry() { }

  @Override
  public String toString() {
    return "SkipEntry()";
  }
}
