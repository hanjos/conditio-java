package org.sbrubbles.conditio.handlers;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Context;
import org.sbrubbles.conditio.Handler;
import org.sbrubbles.conditio.Restart;

import java.util.function.BiFunction;

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
  public static <C extends Condition> BiFunction<Context<C>, Handler.Operations, Handler.Decision> restart(Restart.Option option) {
    return (ctx, ops) -> ops.restart(option);
  }

  /**
   * A handler body that skips handling.
   *
   * @return a handler body that skips handling.
   */
  public static <C extends Condition> BiFunction<Context<C>, Handler.Operations, Handler.Decision> skip() {
    return (ctx, ops) -> ops.skip();
  }

  /**
   * A handler body that aborts execution.
   *
   * @return a handler body that aborts execution.
   */
  public static <C extends Condition> BiFunction<Context<C>, Handler.Operations, Handler.Decision> abort() {
    return (ctx, ops) -> ops.abort();
  }
}
