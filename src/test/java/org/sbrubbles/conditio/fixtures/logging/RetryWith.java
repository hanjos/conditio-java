package org.sbrubbles.conditio.fixtures.logging;

import org.sbrubbles.conditio.Restart;

public class RetryWith implements Restart.Option {
  private final String text;

  public RetryWith(String text) {
    this.text = text;
  }

  public String getText() {
    return text;
  }

  @Override
  public String toString() {
    return "RetryWith(" + text + ")";
  }
}
