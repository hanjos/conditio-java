package org.sbrubbles.conditio.fixtures;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Scope;

import java.util.Objects;

public class MalformedLogEntry extends Condition {
  private String text;

  public MalformedLogEntry(Scope scope, String text) {
    super(scope);

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
    return Objects.equals(text, that.text);
  }

  @Override
  public int hashCode() {
    return Objects.hash(text);
  }

  @Override
  public String toString() {
    return "MalformedLogEntry(" + text + ")";
  }
}
