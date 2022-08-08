package org.sbrubbles.conditio.handlers;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Handler;
import org.sbrubbles.conditio.Restart;
import org.sbrubbles.conditio.util.TriFunction;

/**
 * Some general use handler operations, ready for consumption.
 * <p>
 * This class acts as a namespace, and isn't meant to be inherited or instantiated.
 */
public final class HandlerOps {
  private HandlerOps() { }

  public static <R> TriFunction<Condition, Class<R>, Handler.Operations<R>, Handler.Decision<R>> restart(Restart.Option option) {
    return (c, t, ops) -> ops.restart(option);
  }

  public static <R> TriFunction<Condition, Class<R>, Handler.Operations<R>, Handler.Decision<R>> skip() {
    return (c, t, ops) -> ops.skip();
  }

  public static <R> TriFunction<Condition, Class<R>, Handler.Operations<R>, Handler.Decision<R>> use(R value) {
    return (c, t, ops) -> ops.use(value);
  }
}
