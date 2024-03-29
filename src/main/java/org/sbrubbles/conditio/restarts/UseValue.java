package org.sbrubbles.conditio.restarts;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Restart;
import org.sbrubbles.conditio.Restarts;

/**
 * A restart option holding the value to be returned.
 *
 * @param <R> the type of the value to return.
 * @see Restarts
 * @see org.sbrubbles.conditio.Scope#raise(Condition, Class, Restart[])
 */
public class UseValue<R> implements Restart.Option {
  private final R value;

  /**
   * Creates a new instance.
   *
   * @param value the value to return.
   */
  public UseValue(R value) {
    this.value = value;
  }

  /**
   * The value to be returned in {@code signal}.
   *
   * @return the value to be returned in {@code signal}.
   */
  public R getValue() {
    return value;
  }
}
