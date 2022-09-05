package org.sbrubbles.conditio;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.fixtures.PleaseSignalSomethingElse;
import org.sbrubbles.conditio.fixtures.SonOfBasicCondition;
import org.sbrubbles.conditio.policies.HandlerNotFoundPolicy;
import org.sbrubbles.conditio.policies.Policies;
import org.sbrubbles.conditio.policies.ReturnTypePolicy;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class SignalsTest {
  @ParameterizedTest
  @MethodSource("conditionTypeProvider")
  @SuppressWarnings("unchecked")
  public void conditionType(boolean expected, Class<? extends Condition> conditionType, Signal<?> input) {
    Predicate<Signal<Condition>> predicate = Signals.conditionType(conditionType);

    assertEquals(expected, predicate.test((Signal) input));
  }

  @Test
  public void conditionTypeOnNull() {
    assertThrows(NullPointerException.class, () -> Signals.conditionType(null));
  }

  @ParameterizedTest
  @MethodSource("returnTypeProvider")
  @SuppressWarnings("unchecked")
  public void returnType(boolean expected, Class<?> returnType, Signal<?> input) {
    Predicate<Signal<Condition>> predicate = Signals.returnType(returnType);

    assertEquals(expected, predicate.test((Signal) input));
  }

  @Test
  public void returnTypeOnNull() {
    assertThrows(NullPointerException.class, () -> Signals.returnType(null));
  }

  static Stream<Arguments> conditionTypeProvider() {
    return Stream.of(
      arguments(true, BasicCondition.class, withCondition(new BasicCondition(""))),
      arguments(true, BasicCondition.class, withCondition(new SonOfBasicCondition(""))),
      arguments(false, BasicCondition.class, withCondition(new PleaseSignalSomethingElse())),
      arguments(false, BasicCondition.class, null)
    );
  }

  static Stream<Arguments> returnTypeProvider() {
    return Stream.of(
      arguments(true, String.class, withReturnType(String.class)),
      arguments(true, Object.class, withReturnType(String.class)),
      arguments(false, String.class, withReturnType(Object.class)),
      arguments(false, String.class, null)
    );
  }

  static <C extends Condition> Signal<C> withCondition(C condition) {
    return with(condition, Object.class);
  }

  static Signal<Condition> withReturnType(Class<?> returnType) {
    return with(new BasicCondition(""), returnType);
  }

  static <C extends Condition> Signal<C> with(C condition, Class<?> returnType) {
    // XXX not best practice, but for these tests, should be good enough
    try (Scope scope = Scopes.create()) {
      Policies<?> policies = new Policies<>(
        HandlerNotFoundPolicy.error(),
        ReturnTypePolicy.expects(returnType)
      );

      return new Signal<>(condition, policies, scope);
    }
  }
}

