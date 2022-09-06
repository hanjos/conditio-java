package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.fixtures.PleaseSignalSomethingElse;
import org.sbrubbles.conditio.fixtures.SonOfBasicCondition;
import org.sbrubbles.conditio.fixtures.logging.Entry;
import org.sbrubbles.conditio.fixtures.logging.MalformedLogEntry;
import org.sbrubbles.conditio.policies.HandlerNotFoundPolicy;
import org.sbrubbles.conditio.policies.Policies;
import org.sbrubbles.conditio.policies.ReturnTypePolicy;
import org.sbrubbles.conditio.restarts.Resume;
import org.sbrubbles.conditio.restarts.UseValue;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
public class BasicOperationsTest {
  @Test
  public void signalRemovesTheRestartsAfterwards() {
    final Restart<Entry> USE_VALUE = Restarts.on(UseValue.class, r -> (Entry) r.getValue());
    final Entry TEST_VALUE = new Entry("test");
    final Restart.Option u = new UseValue<>(TEST_VALUE);

    try (Scope a = Scopes.create()) {
      // no restart before the handler...
      assertOptionsDontMatch(Collections.singletonList(u), a.getAllRestarts(), "before handle");

      a.handle(MalformedLogEntry.class, (s, ops) -> {
        // now there's something!
        assertOptionsMatch(Collections.singletonList(u), s.getScope().getAllRestarts(), "inside handle");

        return ops.restart(u);
      });

      // no restart after either
      assertOptionsDontMatch(Collections.singletonList(u), a.getAllRestarts(), "after handle");

      try (Scope b = Scopes.create()) {
        // no restart before signal...
        assertOptionsDontMatch(Collections.singletonList(u), b.getAllRestarts(), "before signal");

        assertEquals(
          TEST_VALUE,
          b.signal(new MalformedLogEntry(""),
            new Policies<>(HandlerNotFoundPolicy.error(), ReturnTypePolicy.expects(Entry.class)),
            USE_VALUE));

        // no restart after either...
        assertOptionsDontMatch(Collections.singletonList(u), b.getAllRestarts(), "after signal");
      }
    }
  }

  @Test
  public void callRemovesTheRestartsAfterwards() {
    final Entry TEST_VALUE = new Entry("test");
    final Restart.Option u = new UseValue<>(TEST_VALUE);
    final Restart.Option r = new Resume<>();

    try (Scope a = Scopes.create()) {
      assertOptionsDontMatch(Arrays.asList(u, r), a.getAllRestarts(), "before handle");

      a.handle(MalformedLogEntry.class, (s, ops) -> {
        assertOptionsMatch(Arrays.asList(u, r), s.getScope().getAllRestarts(), "inside handle");

        return ops.restart(u);
      });

      assertEquals(TEST_VALUE,
        a.call(
          () -> {
            try (Scope b = Scopes.create()) {
              assertOptionsMatch(Arrays.asList(u, r), b.getAllRestarts(), "inside call");

              return b.signal(
                new MalformedLogEntry(""),
                new Policies<>(HandlerNotFoundPolicy.error(), ReturnTypePolicy.expects(Entry.class))); // the use value comes from call
            }
          },
          Restarts.useValue(),
          Restarts.resume()));

      assertOptionsDontMatch(Arrays.asList(u, r), a.getAllRestarts(), "after handle");
    }
  }

  @ParameterizedTest
  @MethodSource("skipHandlingProvider")
  public void skip(Function<String, Condition> builder) {
    final String EXPECTED_RESULT = "<result>";
    final List<String> trail = new ArrayList<>();
    final Condition condition = builder.apply(EXPECTED_RESULT);

    try (Scope a = Scopes.create()) {
      a.handle(BasicCondition.class, trace(trail, "a",
        Handlers.restart(Restarts.use(EXPECTED_RESULT))));

      try (Scope b = Scopes.create()) {
        b.handle(BasicCondition.class, trace(trail, "b",
          Handlers.skip()));

        try (Scope c = Scopes.create()) {
          String actual = c.raise(condition, String.class);

          assertEquals(EXPECTED_RESULT, actual);
          assertLinesMatch(
            Arrays.asList("b", "a"),
            trail);
        }
      }
    }
  }

  @Test
  public void resignal() {
    final String FIXED_RESULT = "<result>";
    final List<String> trail = new ArrayList<>();

    try (Scope a = Scopes.create()) {
      a.handle(PleaseSignalSomethingElse.class,
        trace(trail, "a", (s, ops) -> {
          try (Scope scope = Scopes.create()) {
            return ops.restart(Restarts.use(scope.raise(new BasicCondition(null), s.getPolicies().getExpectedType())));
          }
        }));

      try (Scope b = Scopes.create()) {
        b.handle(BasicCondition.class, trace(trail, "b",
          Handlers.restart(Restarts.use(FIXED_RESULT))));

        try (Scope c = Scopes.create()) {
          Object actual = c.raise(new PleaseSignalSomethingElse(), Object.class);

          assertEquals(FIXED_RESULT, actual);
          assertLinesMatch(
            Arrays.asList("a", "b"),
            trail);
        }
      }
    }
  }

  @Test
  public void signallingAConditionWithNoHandlersAndAErrorPolicyErrorsOut() {
    BasicCondition condition = new BasicCondition("test");

    try (Scope a = Scopes.create()) {
      a.signal(condition, new Policies<>(HandlerNotFoundPolicy.error(), ReturnTypePolicy.ignore()));

      fail();
    } catch (HandlerNotFoundException e) {
      assertEquals(condition, e.getSignal().getCondition());
    }
  }

  @Test
  public void signallingWithNoHandlersAndAnIgnorePolicyNopesOut() {
    try (Scope a = Scopes.create()) {
      a.signal(new BasicCondition("test"), new Policies<>(HandlerNotFoundPolicy.ignore(), ReturnTypePolicy.ignore()));
    }

    // nothing happens, and the returned result is meaningless, so nothing to assert
  }

  @Test
  public void notifyIsTheSameAsSignallingWithAnIgnorePolicyAndResume() {
    List<String> trail = new ArrayList<>();

    try (Scope a = Scopes.create()) {
      a.handle(BasicCondition.class, (s, ops) -> {
        trail.add(s.getCondition().getValue());

        assertOptionsMatch(
          Collections.singletonList(Restarts.resume()),
          s.getScope().getAllRestarts());

        return ops.skip(); // no handling provided
      });

      try (Scope b = Scopes.create()) {
        b.notify(new BasicCondition("notify"));
        b.signal(new BasicCondition("signal"), new Policies<>(HandlerNotFoundPolicy.ignore(), ReturnTypePolicy.ignore()), Restarts.resume());
      }
    }

    assertLinesMatch(Arrays.asList("notify", "signal"), trail);
  }

  @Test
  public void raiseProvidesAUseValueRestart() {
    final List<String> trail = new ArrayList<>();
    final String TEST_VALUE = "<test>";
    final UseValue<String> u = new UseValue<>(TEST_VALUE);

    try (Scope a = Scopes.create()) {
      a.handle(BasicCondition.class, (s, ops) -> {
        trail.add(s.getCondition().getValue());

        assertOptionsMatch(
          Collections.singletonList(u),
          s.getScope().getAllRestarts());

        return ops.restart(Restarts.use(TEST_VALUE));
      });

      try (Scope b = Scopes.create()) {
        assertEquals(TEST_VALUE, b.raise(new BasicCondition("raise"), String.class));
      }
    }

    assertLinesMatch(Collections.singletonList("raise"), trail);
  }

  @Test
  public void raiseUsesAnErrorPolicy() {
    BasicCondition condition = new BasicCondition("raise");

    try (Scope a = Scopes.create()) {
      a.raise(condition, Void.class);

      fail();
    } catch (HandlerNotFoundException e) {
      assertEquals(condition, e.getSignal().getCondition());
    }
  }

  @Test
  public void signalTakesRestartsAndPoliciesWithCompatibleTypes() {
    try (Scope scope = Scopes.create()) {
      scope.handle(BasicCondition.class, Handlers.restart(new BasicRestartOption()));

      final ArrayList<?> expected = new ArrayList<>();
      final List<?> actual = scope.signal(
        new BasicCondition(""),
        new Policies<>(HandlerNotFoundPolicy.ignore(), ReturnTypePolicy.expects(AbstractList.class)),
        Restarts.on(BasicRestartOption.class, args -> expected));

      assertSame(expected, actual);
    }
  }

  @Test
  public void raisesReturnTypeAndRestartsMustMatch() {
    try (Scope scope = Scopes.create()) {
      scope.handle(BasicCondition.class, Handlers.restart(new BasicRestartOption()));

      final ArrayList<?> expected = new ArrayList<>();
      final List<?> actual = scope.raise(
        new BasicCondition(""),
        AbstractList.class,
        Restarts.on(BasicRestartOption.class, args -> expected));

      assertSame(expected, actual);
    }
  }

  @ParameterizedTest
  @MethodSource("closedScopeProvider")
  public void aClosedScopeDoesntWork(Consumer<Scope> consumer) {
    Scope shouldFail = Scopes.create();
    shouldFail.close();

    assertThrows(UnsupportedOperationException.class, () -> consumer.accept(shouldFail));
  }

  static class BasicRestartOption implements Restart.Option { }

  static boolean matches(List<Restart.Option> options, Iterable<Restart<?>> iterable) {
    return options.stream().allMatch(o -> {
      for (Restart<?> r : iterable) {
        if (r.test(o)) {
          return true;
        }
      }

      return false;
    });
  }

  static void assertOptionsMatch(List<Restart.Option> options, Iterable<Restart<?>> iterable) {
    assertTrue(matches(options, iterable));
  }

  static void assertOptionsMatch(List<Restart.Option> options, Iterable<Restart<?>> iterable, String message) {
    assertTrue(matches(options, iterable), message);
  }

  static void assertOptionsDontMatch(List<Restart.Option> options, Iterable<Restart<?>> iterable, String message) {
    assertFalse(matches(options, iterable), message);
  }

  static Stream<Function<String, Condition>> skipHandlingProvider() {
    return Stream.of(
      BasicCondition::new,
      SonOfBasicCondition::new
    );
  }

  static Stream<Consumer<Scope>> closedScopeProvider() {
    return Stream.of(
      scope -> scope.getParent(),
      scope -> scope.getAllRestarts(),
      scope -> scope.getAllHandlers(),
      scope -> scope.handle(Handlers.on(Signals.conditionType(BasicCondition.class), Handlers.abort())),
      scope -> scope.handle(BasicCondition.class, Handlers.abort()),
      scope -> scope.notify(new BasicCondition("")),
      scope -> scope.call(() -> ""),
      scope -> scope.raise(new BasicCondition(""), Object.class),
      scope -> scope.signal(new BasicCondition(""), new Policies<>())
    );
  }

  static <C extends Condition> BiFunction<Signal<C>, Handler.Operations, Handler.Decision> trace(List<String> trail, String message, BiFunction<Signal<C>, Handler.Operations, Handler.Decision> body) {
    return (s, ops) -> {
      trail.add(message);

      return body.apply(s, ops);
    };
  }
}
