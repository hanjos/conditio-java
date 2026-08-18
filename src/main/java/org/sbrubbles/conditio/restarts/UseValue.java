package org.sbrubbles.conditio.restarts;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Restart;
import org.sbrubbles.conditio.Restarts;

import java.util.Objects;

/**
 * A restart option holding the value to be returned.
 * <p>
 * It is a data class, and so implements {@code equals} and {@code hashCode}.
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

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof UseValue)) return false;
    UseValue<?> useValue = (UseValue<?>) o;
    return Objects.equals(getValue(), useValue.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getValue());
  }
}
