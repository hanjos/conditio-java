package org.sbrubbles.conditio.fixtures;

import java.util.Objects;

public class Entry {
  final String text;

  public Entry(String text) {
    this.text = text;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Entry entry = (Entry) o;
    return Objects.equals(text, entry.text);
  }

  public String getText() {
    return text;
  }

  @Override
  public int hashCode() {
    return Objects.hash(text);
  }

  @Override
  public String toString() {
    return "ENTRY: " + text;
  }
}
