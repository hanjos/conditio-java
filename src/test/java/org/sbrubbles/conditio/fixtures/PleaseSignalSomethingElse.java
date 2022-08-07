package org.sbrubbles.conditio.fixtures;

import org.sbrubbles.conditio.Condition;

public class PleaseSignalSomethingElse<R> extends Condition<R> {
  public PleaseSignalSomethingElse(Class<R> resultType) {
    super(resultType);
  }
}