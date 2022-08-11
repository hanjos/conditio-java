package org.sbrubbles.conditio.restarts;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Restart;
import org.sbrubbles.conditio.Scope;
import org.sbrubbles.conditio.policies.HandlerNotFoundPolicy;

/**
 * A restart which indicates that execution may proceed "without" returning a value.
 * <p>
 * This is meant for situations where the result of {@link Scope#signal(Condition, HandlerNotFoundPolicy, Restart[]) Scope.signal} isn't
 * used, and the handler means only to acknowledge the condition (and maybe do a side effect or two), like
 * <pre>
 *   try(Scope scope = Scopes.create()) {
 *     scope.handle(Progress.class, (c, t, ops) -&gt; {
 *       // do something
 *       showProgressToUser(c.getValue());
 *
 *       // condition acknowledged; carry on
 *       return ops.restart(Restarts.resume());
 *     });
 *
 *     // note that result of signal() is ignored and thrown away
 *     scope.signal(new Progress(0.6), Restarts.resume());
 *
 *     // ...
 *   }
 * </pre>
 * <p>
 * This class works both as a {@link org.sbrubbles.conditio.Restart.Option} and a {@link Restart}. It stores no state,
 * which means that any one instance is equal to any other. So, for convenience, {@link Restarts} provides a pre-built
 * instance.
 *
 * @param <R> unused, since any result is meant to be ignored.
 * @see Restarts
 */
public class Resume<R> implements Restart.Option, Restart<R> {
  /**
   * Creates a new instance. Developers can just reuse {@link Restarts#resume()} instead.
   *
   * @see Restarts
   */
  public Resume() { /**/ }

  /**
   * Checks if the given option is an instance of this class.
   *
   * @param option a restart option.
   * @return if {@code option} is an instance of this class.
   */
  @Override
  public boolean test(Option option) {
    return option instanceof Resume;
  }

  /**
   * {@code Resume}s have no meaningful value to "return"; so this method returns a "garbage" object, to be ignored.
   *
   * @param option a restart option.
   * @return a "garbage" value, to be ignored.
   * @throws ClassCastException if {@code option} does not pass {@link #test(Restart.Option) test}.
   */
  @Override
  public R apply(Option option) {
    if (!test(option)) {
      throw new ClassCastException();
    }

    return null;
  }

  /**
   * If the given object is a {@code Resume}.
   */
  @Override
  public boolean equals(Object o) {
    return (this == o) ||
      (o != null && getClass() == o.getClass());
  }

  /**
   * Some constant number.
   */
  @Override
  public int hashCode() {
    return -1;
  }
}
