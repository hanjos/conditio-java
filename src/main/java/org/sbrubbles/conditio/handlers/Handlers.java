package org.sbrubbles.conditio.handlers;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Handler;
import org.sbrubbles.conditio.Restart;

import java.util.function.Function;

/**
 * Some utility methods for handlers.
 * <p>
 * This class acts as a namespace, and isn't meant to be inherited or instantiated.
 */
public final class Handlers {
  private Handlers() { }

  /**
   * A handler body that invokes the restart matching the given option.
   *
   * @param option the restart option used to select a restart.
   * @return a handler body that invokes the restart matching the given option.
   */
  public static <C extends Condition> Function<Handler.Context<C>, Handler.Decision> restart(Restart.Option option) {
    return ctx -> ctx.restart(option);
  }

  /**
   * A handler body that skips handling.
   *
   * @return a handler body that skips handling.
   */
  public static <C extends Condition> Function<Handler.Context<C>, Handler.Decision> skip() {
    return Handler.Context::skip;
  }

  /**
   * A handler body that aborts execution.
   *
   * @return a handler body that aborts execution.
   */
  public static <C extends Condition> Function<Handler.Context<C>, Handler.Decision> abort() {
    return Handler.Context::abort;
  }
}
