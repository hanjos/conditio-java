package org.sbrubbles.conditio;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.fixtures.PleaseSignalSomethingElse;
import org.sbrubbles.conditio.fixtures.SonOfBasicCondition;
import org.sbrubbles.conditio.policies.Policies;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class SignalsTest {
  @ParameterizedTest
  @MethodSource("conditionTypeProvider")
  public <C extends Condition> void conditionType(boolean expected, Class<C> conditionType, Signal<C> input) {
    Predicate<Signal<C>> predicate = Signals.conditionType(conditionType);

    assertEquals(expected, predicate.test(input));
  }

  @Test
  public void conditionTypeOnNull() {
    assertThrows(NullPointerException.class, () -> Signals.conditionType(null));
  }

  static Stream<Arguments> conditionTypeProvider() {
    return Stream.of(
      arguments(true, BasicCondition.class, withCondition(new BasicCondition(""))),
      arguments(true, BasicCondition.class, withCondition(new SonOfBasicCondition(""))),
      arguments(false, BasicCondition.class, withCondition(new PleaseSignalSomethingElse())),
      arguments(false, BasicCondition.class, null)
    );
  }

  static <C extends Condition> Signal<C> withCondition(C condition) {
    // XXX not best practice, but for these tests, should be good enough
    try (Scope scope = Scopes.create()) {
      return new Signal<>(condition, new Policies<>(), scope);
    }
  }
}

