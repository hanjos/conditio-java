package org.sbrubbles.conditio.restarts;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Restart;
import org.sbrubbles.conditio.Scope;

/**
 * Indicates for execution to proceed, "without" returning a value. This is meant for situations where the
 * result of {@link Scope#signal(Condition, Restart...) signal} isn't used, and the handler means only to
 * acknowledge the condition, like
 * <pre>
 *   try(Scope scope = Scopes.create()) {
 *     scope.handle(Progress.class, (c, ops) -&gt; {
 *       // do something
 *       showProgressToUser(c.getValue());
 *
 *       // condition acknowledged; carry on
 *       return ops.restart(Resume.OPTION);
 *     });
 *
 *     // note that result of signal() is ignored and thrown away
 *     scope.signal(new Progress(0.6), Resume.RESTART);
 *
 *     // ...
 *   }
 * </pre>
 * <p>
 * There's no useful value to "return". There's also no way to tell Java to "not return" a value here, so in this
 * case {@code signal} will return a "garbage" object.
 * <p>
 * This class stores no state; therefore, it provides a pre-instanced option and restart.
 */
public class Resume implements Restart.Option {
  /**
   * A pre-built instance, for usage in restart calls.
   */
  public static final Resume OPTION = new Resume();

  /**
   * A pre-built restart, for usage in restart setting.
   */
  public static final Restart RESTART = Restart.on(Resume.class, r -> null);
}
