package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.fixtures.PleaseSignalSomethingElse;
import org.sbrubbles.conditio.fixtures.SonOfBasicCondition;
import org.sbrubbles.conditio.policies.Policies;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.sbrubbles.conditio.handlers.Contexts.conditionType;

public class HandlerTest {
  private Handler h;

  @BeforeEach
  public void setUp() {
    h = new HandlerImpl(conditionType(BasicCondition.class), this::body);
  }

  @Test
  public void nullParametersAreNotAllowed() {
    assertThrows(NullPointerException.class, () -> new HandlerImpl(null, this::body), "missing conditionType");
    assertThrows(NullPointerException.class, () -> new HandlerImpl(conditionType(BasicCondition.class), null), "missing body");
    assertThrows(NullPointerException.class, () -> new HandlerImpl(null, null), "missing both");
  }

  @ParameterizedTest
  @MethodSource("testProvider")
  public void test(Condition condition, boolean expected) {
    try (Scope scope = Scopes.create()) {
      final Handler.Context<Condition> ctx = (condition != null) ?
        new HandlerContextImpl<>(condition, new Policies<>(), scope) :
        null;

      assertEquals(expected, h.test(ctx));
    }
  }

  @ParameterizedTest
  @MethodSource("applyProvider")
  public void apply(Condition condition, String expected) {
    try (Scope scope = Scopes.create()) {
      final Handler.Context<Condition> ctx = (condition != null) ?
        new HandlerContextImpl<>(condition, new Policies<>(), scope) :
        null;

      assertEquals(expected, h.apply(ctx).get());
    }
  }

  @Test
  public void getters() {
    try (Scope a = Scopes.create()) {
      Condition c = new BasicCondition("OMGWTFBBQ");
      final Handler.Context<Condition> ctx = new HandlerContextImpl<>(c, new Policies<>(), a);

      assertEquals(c, ctx.getCondition());
      assertEquals(a, ctx.getScope());
    }
  }

  private Handler.Decision body(Handler.Context<BasicCondition> ctx) {
    if (ctx == null) {
      return new Handler.Decision(null);
    }

    BasicCondition c = ctx.getCondition();

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
