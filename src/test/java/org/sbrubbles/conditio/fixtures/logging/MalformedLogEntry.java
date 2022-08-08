package org.sbrubbles.conditio.fixtures.logging;

import org.sbrubbles.conditio.Condition;

import java.util.Objects;

public class MalformedLogEntry extends Condition {
  private final String text;

  public MalformedLogEntry(String text) {
    this.text = text;
  }

  public String getText() {
    return text;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MalformedLogEntry that = (MalformedLogEntry) o;
    return Objects.equals(getText(), that.getText());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getText());
  }

  @Override
  public String toString() {
    return "MalformedLogEntry(" + text + ")";
  }
}
