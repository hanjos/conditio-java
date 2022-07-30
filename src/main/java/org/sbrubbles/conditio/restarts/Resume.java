package org.sbrubbles.conditio.restarts;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Restart;
import org.sbrubbles.conditio.Scope;

/**
 * A restart option to indicate that execution is to proceed, "without" returning a value.
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
 * This class stores no state. It provides a pre-instanced option and restart for convenience.
 */
public class Resume implements Restart.Option {
  /**
   * A pre-built instance of this class. Since this class stores no state,
   * {@linkplain org.sbrubbles.conditio.Handler.Operations#restart(Restart.Option) restart calls} can just reuse this
   * constant instead of creating a new object.
   */
  public static final Resume OPTION = new Resume();

  /**
   * A pre-built restart for this option, which just returns {@code null}. Since the idea is to ignore the return value,
   * {@link Scope#signal(Condition, Restart...) signal}s can just reuse this constant instead of creating a new object.
   */
  public static final Restart RESTART = Restart.on(Resume.class, r -> null);

  /**
   * Creates a new instance.
   */
  public Resume() { /**/ }
}
