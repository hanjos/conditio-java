package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.fixtures.PleaseSignalSomethingElse;
import org.sbrubbles.conditio.fixtures.SonOfBasicCondition;
import org.sbrubbles.conditio.handlers.Handlers;
import org.sbrubbles.conditio.policies.Policies;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.sbrubbles.conditio.handlers.Signals.conditionType;

public class HandlerTest {
  private Handler h;

  @BeforeEach
  public void setUp() {
    h = Handlers.on(conditionType(BasicCondition.class), this::body);
  }

  @Test
  public void nullParametersAreNotAllowed() {
    assertThrows(NullPointerException.class, () -> Handlers.on(null, this::body), "missing conditionType");
    assertThrows(NullPointerException.class, () -> Handlers.on(conditionType(BasicCondition.class), null), "missing body");
    assertThrows(NullPointerException.class, () -> Handlers.on(null, null), "missing both");
  }

  @ParameterizedTest
  @MethodSource("testProvider")
  public void test(Condition condition, boolean expected) {
    try (Scope scope = Scopes.create()) {
      final Signal<Condition> s = (condition != null) ?
        new Signal<>(condition, new Policies<>(), scope) :
        null;

      assertEquals(expected, h.test(s));
    }
  }

  @ParameterizedTest
  @MethodSource("applyProvider")
  public void apply(Condition condition, String expected) {
    try (Scope scope = Scopes.create()) {
      final Signal<Condition> s = (condition != null) ?
        new Signal<>(condition, new Policies<>(), scope) :
        null;
      final Handler.Operations ops = new HandlerOperationsImpl(scope);

      assertEquals(expected, h.apply(s, ops).get());
    }
  }

  private Handler.Decision body(Signal<BasicCondition> s, Handler.Operations ops) {
    if (s == null) {
      return new Handler.Decision(null);
    }

    BasicCondition c = s.getCondition();

    if (!"FAIL".equals(c.getValue())) {
      return new Handler.Decision("OK: " + c.getValue());
    } else {
      return new Handler.Decision("FAIL!");
    }
  }

  static Stream<Arguments> testProvider() {
    return Stream.of(
      arguments(null, false),
      arguments(new BasicCondition("string"), true),
      arguments(new PleaseSignalSomethingElse(), false),
      arguments(new SonOfBasicCondition("stringsson"), true)
    );
  }

  static Stream<Arguments> applyProvider() {
    return Stream.of(
      arguments(new BasicCondition("OMGWTFBBQ"), "OK: OMGWTFBBQ"),
      arguments(new BasicCondition("FAIL"), "FAIL!"),
      arguments(new BasicCondition(null), "OK: null"),
      arguments(null, null)
    );
  }
}
