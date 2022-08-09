package org.sbrubbles.conditio.restarts;

import org.sbrubbles.conditio.Restart;
import org.sbrubbles.conditio.Scope;

/**
 * Some general use restarts and restart options, ready for consumption.
 * <p>
 * This class acts as a namespace, and isn't meant to be inherited or instantiated.
 */
public final class Restarts {
  private Restarts() { }

  private static final Abort ABORT = new Abort<>();
  private static final Resume RESUME = new Resume<>();

  /**
   * A restart, that also works as its own restart option, which {@linkplain Abort aborts execution}.
   *
   * @return a pre-built instance of {@link Abort}.
   * @see Abort
   */
  public static <R> Abort<R> abort() {
    return ABORT;
  }

  /**
   * A restart, that also works as its own restart option, which {@linkplain Resume continues execution "without"
   * returning a value}.
   *
   * @return a pre-built instance of {@link Resume}.
   * @see Resume
   */
  public static <R> Resume<R> resume() {
    return RESUME;
  }

  /**
   * A {@linkplain UseValue restart option} which indicates that {@link Scope#signal} should return the given value.
   *
   * @param value the value to be returned by {@code signal}.
   * @return a restart option.
   * @see #useValue()
   */
  public static <R> UseValue<R> use(R value) { return new UseValue<>(value); }

  /**
   * A restart which matches {@link UseValue}, extracting its value.
   *
   * @return a restart matching {@code UseValue}.
   * @see #use(Object)
   */
  public static <R> Restart<R> useValue() { return Restart.on(UseValue.class, UseValue<R>::getValue); }
}
