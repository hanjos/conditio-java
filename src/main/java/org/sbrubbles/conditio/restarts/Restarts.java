package org.sbrubbles.conditio.restarts;

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
}
