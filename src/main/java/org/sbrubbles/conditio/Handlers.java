package org.sbrubbles.conditio;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Some utility methods for handlers.
 * <p>
 * This class acts as a namespace, and isn't meant to be inherited or instantiated.
 */
public final class Handlers {
  private Handlers() { }

  /**
   * Creates and returns a new handler, with a default implementation. Ensures statically that the given parameters
   * are type-compatible.
   *
   * @param predicate the type of condition this handler expects.
   * @param body      a (bi)function which receives a {@linkplain Signal signal} and the available operations, and
   *                  returns the result.
   * @param <C>       a subtype of {@code Condition}.
   * @param <S>       a subtype of {@code C}, so that {@code body} is still compatible with {@code C} but may
   *                  accept subtypes other than {@code S}.
   * @throws NullPointerException if any of the arguments are null.
   */
  public static <C extends Condition, S extends C> Handler on(Predicate<Signal<S, ?>> predicate, BiFunction<Signal<C, ?>, Handler.Operations, Handler.Decision> body) {
    return new HandlerImpl(predicate, body);
  }

  /**
   * A handler body that invokes the restart matching the given option.
   *
   * @param option the restart option used to select a restart.
   * @return a handler body that invokes the restart matching the given option.
   */
  public static <C extends Condition> BiFunction<Signal<C, ?>, Handler.Operations, Handler.Decision> restart(Restart.Option option) {
    return (s, ops) -> ops.restart(option);
  }

  /**
   * A handler body that skips handling.
   *
   * @return a handler body that skips handling.
   */
  public static <C extends Condition> BiFunction<Signal<C, ?>, Handler.Operations, Handler.Decision> skip() {
    return (s, ops) -> ops.skip();
  }

  /**
   * A handler body that aborts execution.
   *
   * @return a handler body that aborts execution.
   */
  public static <C extends Condition> BiFunction<Signal<C, ?>, Handler.Operations, Handler.Decision> abort() {
    return (s, ops) -> ops.abort();
  }
}

class HandlerImpl implements Handler {
  private final Predicate<Signal<? extends Condition, ?>> predicate;
  private final BiFunction<Signal<? extends Condition, ?>, Operations, Decision> body;

  @SuppressWarnings("unchecked")
  <C extends Condition, S extends C> HandlerImpl(Predicate<Signal<S, ?>> predicate, BiFunction<Signal<C, ?>, Operations, Decision> body) {
    this.predicate = (Predicate) Objects.requireNonNull(predicate, "conditionType");
    this.body = (BiFunction) Objects.requireNonNull(body, "body");
  }

  @Override
  public boolean test(Signal<? extends Condition, ?> signal) {
    return getPredicate().test(signal);
  }

  @Override
  public Decision apply(Signal<?, ?> s, Operations ops) {
    return getBody().apply(s, ops);
  }

  public Predicate<Signal<? extends Condition, ?>> getPredicate() {
    return predicate;
  }

  public BiFunction<Signal<? extends Condition, ?>, Operations, Decision> getBody() {
    return body;
  }
}
