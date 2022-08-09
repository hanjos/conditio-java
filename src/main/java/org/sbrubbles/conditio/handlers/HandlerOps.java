package org.sbrubbles.conditio.handlers;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Handler;
import org.sbrubbles.conditio.Restart;

import java.util.function.BiFunction;

/**
 * Some general use handler operations, ready for consumption.
 * <p>
 * This class acts as a namespace, and isn't meant to be inherited or instantiated.
 */
public final class HandlerOps {
  private HandlerOps() { }

  public static BiFunction<Condition, Handler.Operations, Handler.Decision> restart(Restart.Option option) {
    return (c, ops) -> ops.restart(option);
  }

  public static BiFunction<Condition, Handler.Operations, Handler.Decision> skip() {
    return (c, ops) -> ops.skip();
  }

  public static BiFunction<Condition, Handler.Operations, Handler.Decision> use(Object value) {
    return (c, ops) -> ops.use(value);
  }
}
