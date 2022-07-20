package org.sbrubbles.conditio.fixtures;

import org.sbrubbles.conditio.Condition;

public class PleaseSignalSomethingElse extends Condition {
  // XXX not my proudest code, but for test purposes, screw it :)
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return 1;
  }
}
