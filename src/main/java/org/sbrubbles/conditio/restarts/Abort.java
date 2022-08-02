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
 *     a.handle(SomeCondition.class, (c, ops) -&gt; ops.restart(Abort.INSTANCE))
 *
 *     try(Scope b = Scopes.create()) {
 *       try(Scope c = Scopes.create()) {
 *         // signals something which may result in the interruption of c as a whole
 *         Object result = c.signal(new SomeCondition(), Abort.INSTANCE);
 *
 *         // c...
 *       }
 *
 *       // b...
 *     } catch(AbortException e) {
 *       // stops the stack unwinding in b
 *     }
 *
 *     // a...
 *   }
 * </pre>
 * <p>
 * This class works both as a {@link Restart.Option} and a {@link Restart}. It stores no state, which means that any
 * one instance is equal to any other. So, for convenience, this class provides a pre-built instance.
 */
public class Abort implements Restart.Option, Restart {
  /**
   * A pre-built instance of this class.
   */
  public static final Abort INSTANCE = new Abort();

  /**
   * Creates a new instance.
   * <p>
   * This class stores no state, so developers can just reuse {@link #INSTANCE} instead of creating a new object.
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
  public Object apply(Option option) throws AbortException {
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
