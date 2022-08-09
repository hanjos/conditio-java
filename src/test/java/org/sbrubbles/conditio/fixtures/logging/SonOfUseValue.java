package org.sbrubbles.conditio.fixtures.logging;

import org.sbrubbles.conditio.restarts.UseValue;

public class SonOfUseValue<R> extends UseValue<R> {
  public SonOfUseValue(R value) {
    super(value);
  }
}
