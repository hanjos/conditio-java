package org.sbrubbles.conditio.handlers;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Context;

import java.util.function.Predicate;

/**
 * Some utility methods for contexts.
 * <p>
 * This class acts as a namespace, and isn't meant to be inherited or instantiated.
 */
public class Contexts {
  private Contexts() { }

  /**
   * Creates a predicate which checks if a context holds a condition compatible with the given type.
   *
   * @param type the expected type for the condition.
   * @return a predicate which checks if contexts have conditions compatible with the given type.
   */
  public static <C extends Condition> Predicate<Context<C>> conditionType(final Class<? extends Condition> type) {
    return ctx -> ctx != null && type != null && type.isInstance(ctx.getCondition());
  }
}
