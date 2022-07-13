package org.sbrubbles.conditio.fixtures;

import org.sbrubbles.conditio.Restart;

public class RetryWith implements Restart.Option{
  String text;

  public RetryWith(String text) {
    this.text = text;
  }

  @Override
  public String toString() {
    return "ReparseWith(" + text + ")";
  }
}
