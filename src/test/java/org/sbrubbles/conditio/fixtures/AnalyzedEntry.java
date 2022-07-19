package org.sbrubbles.conditio.fixtures;

import java.util.Objects;

public class AnalyzedEntry {
  final Entry entry;
  final String filename;

  public AnalyzedEntry(Entry entry, String filename) {
    this.entry = entry;
    this.filename = filename;
  }

  public Entry getEntry() {
    return entry;
  }

  public String getFilename() {
    return filename;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AnalyzedEntry that = (AnalyzedEntry) o;
    return Objects.equals(entry, that.entry) && Objects.equals(filename, that.filename);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entry, filename);
  }

  @Override
  public String toString() {
    return "ANALYZED(" + filename + "): " + entry;
  }
}
