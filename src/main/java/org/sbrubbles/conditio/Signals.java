package org.sbrubbles.conditio;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Some utility methods for signals.
 * <p>
 * This class acts as a namespace, and isn't meant to be inherited or instantiated.
 */
public final class Signals {
  private Signals() { }

  /**
   * Creates a predicate which checks if a signal holds a condition compatible with the given type.
   *
   * @param type the expected type for the condition. May not be null.
   * @return a predicate which checks if signals have conditions compatible with the given type.
   * @throws NullPointerException if the given type is null.
   */
  public static <C extends Condition> Predicate<Signal<C, ?>> conditionType(final Class<? extends Condition> type) {
    Objects.requireNonNull(type, "type");

    return s -> s != null && type.isInstance(s.getCondition());
  }
}
