package org.sbrubbles.conditio.restarts;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Restart;
import org.sbrubbles.conditio.Scope;

/**
 * A restart which indicates that execution may proceed "without" returning a value.
 * <p>
 * This is meant for situations where the result of {@link Scope#signal(Condition, Restart...) Scope.signal} isn't
 * used, and the handler means only to acknowledge the condition (and maybe do a side effect or two), like
 * <pre>
 *   try(Scope scope = Scopes.create()) {
 *     scope.handle(Progress.class, (c, ops) -&gt; {
 *       // do something
 *       showProgressToUser(c.getValue());
 *
 *       // condition acknowledged; carry on
 *       return ops.restart(Resume.INSTANCE);
 *     });
 *
 *     // note that result of signal() is ignored and thrown away
 *     scope.signal(new Progress(0.6), Resume.INSTANCE);
 *
 *     // ...
 *   }
 * </pre>
 * <p>
 * This class works both as a {@link Restart.Option} and a {@link Restart}. It stores no state, which means that any
 * one instance is equal to any other. So, for convenience, this class provides a pre-built instance.
 */
public class Resume implements Restart.Option, Restart {
  /**
   * A pre-built instance of this class.
   */
  public static final Resume INSTANCE = new Resume();

  /**
   * Creates a new instance.
   * <p>
   * This class stores no state, so developers can just reuse {@link #INSTANCE} instead of creating a new object.
   */
  public Resume() { /**/ }

  /**
   * Checks if the given option is an instance of this class.
   *
   * @param option a restart option.
   * @return if {@code option} is an instance of {@code Resume}.
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
   * @throws ClassCastException if {@code option} does not pass {@link #test(Option) test}.
   */
  @Override
  public Object apply(Option option) {
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

  @Override
  public int hashCode() {
    return -1;
  }
}
