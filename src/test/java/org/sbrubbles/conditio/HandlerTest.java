package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.fixtures.PleaseSignalSomethingElse;
import org.sbrubbles.conditio.fixtures.SonOfBasicCondition;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class HandlerTest {
  private Handler h;

  @BeforeEach
  public void setUp() {
    h = Handlers.on(s -> s != null && s.getCondition() instanceof BasicCondition, this::body);
  }

  @Test
  public void nullParametersAreNotAllowed() {
    assertThrows(NullPointerException.class, () -> Handlers.on(null, this::body), "missing conditionType");
    assertThrows(NullPointerException.class, () -> Handlers.on(s -> s.getCondition() instanceof BasicCondition, null), "missing body");
    assertThrows(NullPointerException.class, () -> Handlers.on(null, null), "missing both");
  }

  @ParameterizedTest
  @MethodSource("testProvider")
  public void test(Condition condition, boolean expected) {
    try (Scope scope = Scopes.create()) {
      final Signal<Condition> s = (condition != null) ?
          new Signal<>(condition, Optional.empty(), scope) :
          null;

      assertEquals(expected, h.test(s));
    }
  }

  @ParameterizedTest
  @MethodSource("applyProvider")
  public void apply(Condition condition, String expected) {
    try (Scope scope = Scopes.create();
         Handler.Operations ops = new Handler.Operations(scope)) {
      final Signal<Condition> s = (condition != null) ?
          new Signal<>(condition, Optional.empty(), scope) :
          null;

      assertEquals(expected, h.apply(s, ops).get());
    }
  }

  @ParameterizedTest
  @MethodSource("closedOperationsProvider")
  public void aClosedOperationsDoesntWork(final Consumer<Handler.Operations> consumer) {
    Scope scope = Scopes.create();
    Handler.Operations ops = new Handler.Operations(scope);
    ops.close();
    scope.close();

    assertThrows(UnsupportedOperationException.class, () -> consumer.accept(ops));
  }

  @Test
  public void handlerNotFoundExceptionIsThrownByDefault() {
    Condition c = new BasicCondition("");
    try (Scope a = Scopes.create()) {
      a.signal(c, Optional.empty(), Restarts.resume());
    } catch (HandlerNotFoundException e) {
      assertEquals(c, e.getSignal().getCondition());

      return;
    }

    fail("Should've thrown earlier!");
  }

  @Test
  public void handlerNotFoundIsSignalledWhenThereIsNoHandler() {
    AtomicInteger INTERCEPTED = new AtomicInteger();

    try (Scope a = Scopes.create()) {
      a.handle(HandlerNotFound.class, (s, ops) -> {
        INTERCEPTED.set(1);

        return ops.restart(Restarts.resume());
      });

      a.signal(new BasicCondition(""), Optional.empty(), Restarts.resume());
    }

    assertEquals(1, INTERCEPTED.get(), "Should've been set by the HandlerNotFound handler!");
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

  static Stream<Consumer<Handler.Operations>> closedOperationsProvider() {
    return Stream.of(
        ops -> ops.restart(Restarts.resume()),
        Handler.Operations::skip,
        Handler.Operations::abort
    );
  }
}
