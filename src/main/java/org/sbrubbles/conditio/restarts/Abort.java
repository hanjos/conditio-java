package org.sbrubbles.conditio.restarts;

import org.sbrubbles.conditio.Restart;

/**
 * A restart which indicates the wish to abort execution and unwind the stack. It does so by throwing
 * {@link AbortException}.
 * <p>
 * Exceptions can always be thrown from within handlers or restarts. Using this restart expresses the intent
 * to offer aborting as a possibility, and should be followed by a {@code try-catch} clause at the desired level to
 * stop unwinding the stack.
 * <p>
 * Example
 * <pre>
 *   try(Scope a = Scopes.create()) {
 *     // decides to give up on the whole operation
 *     a.handle(SomeCondition.class, (c, ops) -&gt; ops.restart(Restarts.abort()))
 *
 *     try(Scope b = Scopes.create()) {
 *       try(Scope c = Scopes.create()) {
 *         // signals something which may result in the interruption of c as a whole
 *         Object result = c.signal(new SomeCondition(), Restarts.abort());
 *
 *         // (execution won't reach here)
 *       }
 *
 *       // (execution won't reach here)
 *     } catch(AbortException e) {
 *       // stops the stack unwinding in b
 *     }
 *
 *     // (carries on in scope a)...
 *   }
 * </pre>
 * <p>
 * This class works both as a {@link org.sbrubbles.conditio.Restart.Option} and a {@link Restart}. It stores no state,
 * which means that any one instance is equal to any other. So, for convenience, {@link Restarts} provides a pre-built
 * instance.
 */
public class Abort<R> implements Restart.Option, Restart<R> {
  /**
   * Creates a new instance. Developers are encouraged to just reuse {@link Restarts#abort()} instead.
   *
   * @see Restarts
   */
  public Abort() { }

  /**
   * Checks if the given option is an instance of this class.
   *
   * @param option a restart option.
   * @return if {@code option} is an instance of this class.
   */
  @Override
  public boolean test(Option option) {
    return option instanceof Abort;
  }

  /**
   * Throws an exception.
   *
   * @param option a restart option.
   * @return nothing, really, since an exception will be thrown.
   * @throws AbortException to end execution in a scope and unwind the stack.
   */
  @Override
  public R apply(Option option) throws AbortException {
    throw new AbortException();
  }

  /**
   * If the given object is an Abort.
   */
  @Override
  public boolean equals(Object o) {
    return (this == o) ||
      (o != null && this.getClass() == o.getClass());
  }

  /**
   * Some constant number.
   */
  @Override
  public int hashCode() {
    return -2;
  }
}
