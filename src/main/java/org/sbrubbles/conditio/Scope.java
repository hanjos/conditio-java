package org.sbrubbles.conditio;

public class Scope implements AutoCloseable {
  private static Scope current = null;

  public static Scope create() {
    Scope newScope = new Scope(current);
    current = newScope;

    return current;
  }

  private final Scope parent;

  private Scope(Scope parent) {
    this.parent = parent;
  }

  public Scope getParent() {
    return parent;
  }

  public boolean isRoot() {
    return getParent() == null;
  }

  @Override
  public void close() {
    current = getParent();
  }
}
