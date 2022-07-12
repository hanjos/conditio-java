package org.sbrubbles.conditio.fixtures;

public class RetryWith {
  String text;

  public RetryWith(String text) {
    this.text = text;
  }

  @Override
  public String toString() {
    return "ReparseWith(" + text + ")";
  }
}
