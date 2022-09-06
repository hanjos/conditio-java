package org.sbrubbles.conditio;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Utilities for signals.
 * <p>
 * This class acts as a namespace, and isn't meant to be inherited or instantiated.
 */
public final class Signals {
  private Signals() { }

  /**
   * Creates a predicate which checks if a signal holds a condition compatible with the given type.
   *
   * @param type the expected type, in {@link Class} form, for the condition. May not be null.
   * @param <C>  the expected type for the condition.
   * @return a predicate which checks if a signal has a condition compatible with the given type.
   * @throws NullPointerException if the given type is null.
   */
  public static <C extends Condition> Predicate<Signal<C>> conditionType(final Class<C> type) {
    Objects.requireNonNull(type, "type");

    return s -> s != null && type.isInstance(s.getCondition());
  }

  /**
   * Creates a predicate which checks if a signal expects a result compatible with the given type.
   *
   * @param type the expected type for the result. May not be null.
   * @return a predicate which checks if a signal expects a result compatible with the given type.
   * @throws NullPointerException if the given type is null.
   */
  public static <C extends Condition> Predicate<Signal<C>> returnType(final Class<?> type) {
    Objects.requireNonNull(type, "type");

    return s -> s != null && type.isAssignableFrom(s.getPolicies().getExpectedType());
  }
}
