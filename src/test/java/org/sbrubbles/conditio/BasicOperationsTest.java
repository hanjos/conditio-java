package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.fixtures.PleaseSignalSomethingElse;
import org.sbrubbles.conditio.fixtures.SonOfBasicCondition;
import org.sbrubbles.conditio.fixtures.logging.Entry;
import org.sbrubbles.conditio.fixtures.logging.MalformedLogEntry;
import org.sbrubbles.conditio.handlers.HandlerOps;
import org.sbrubbles.conditio.policies.Policies;
import org.sbrubbles.conditio.restarts.Restarts;
import org.sbrubbles.conditio.restarts.Resume;
import org.sbrubbles.conditio.restarts.UseValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class BasicOperationsTest {
  @Test
  public void signalRemovesTheRestartsAfterwards() {
    final Restart<Entry> USE_VALUE = Restart.on(UseValue.class, r -> (Entry) r.getValue());
    final Entry TEST_VALUE = new Entry("test");
    final Restart.Option u = new UseValue<>(TEST_VALUE);

    try (Scope a = Scopes.create()) {
      // no restart before the handler...
      assertOptionsMismatch(Collections.singletonList(u), a.getAllRestarts(), "before handle");

      a.handle(MalformedLogEntry.class, (c, ops) -> {
        // now there's something!
        assertOptionsMatch(Collections.singletonList(u), ops.getScope().getAllRestarts(), "inside handle");

        return ops.restart(u);
      });

      // no restart after either
      assertOptionsMismatch(Collections.singletonList(u), a.getAllRestarts(), "after handle");

      try (Scope b = Scopes.create()) {
        // no restart before signal...
        assertOptionsMismatch(Collections.singletonList(u), b.getAllRestarts(), "before signal");

        assertEquals(TEST_VALUE, b.signal(new MalformedLogEntry(""), Policies.error(), USE_VALUE));

        // no restart after either...
        assertOptionsMismatch(Collections.singletonList(u), b.getAllRestarts(), "after signal");
      }
    }
  }

  @Test
  public void callRemovesTheRestartsAfterwards() {
    final Entry TEST_VALUE = new Entry("test");
    final Restart.Option u = new UseValue<>(TEST_VALUE);
    final Restart.Option r = new Resume<>();

    try (Scope a = Scopes.create()) {
      assertOptionsMismatch(Arrays.asList(u, r), a.getAllRestarts(), "before handle");

      a.handle(MalformedLogEntry.class, (c, ops) -> {
        assertOptionsMatch(Arrays.asList(u, r), ops.getScope().getAllRestarts(), "inside handle");

        return ops.restart(u);
      });

      assertEquals(TEST_VALUE,
        a.call(
          () -> {
            try (Scope b = Scopes.create()) {
              assertOptionsMatch(Arrays.asList(u, r), b.getAllRestarts(), "inside call");

              return b.signal(new MalformedLogEntry(""), Policies.error()); // the use value comes from call
            }
          },
          Restarts.useValue(),
          Restarts.resume()));

      assertOptionsMismatch(Arrays.asList(u, r), a.getAllRestarts(), "after handle");
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
        HandlerOps.restart(Restarts.use(EXPECTED_RESULT))));

      try (Scope b = Scopes.create()) {
        b.handle(BasicCondition.class, trace(trail, "b",
          HandlerOps.skip()));

        try (Scope c = Scopes.create()) {
          String actual = c.raise(condition);

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
        trace(trail, "a", (c, ops) -> {
          try (Scope s = Scopes.create()) {
            return ops.restart(Restarts.use(s.raise(new BasicCondition(null))));
          }
        }));

      try (Scope b = Scopes.create()) {
        b.handle(BasicCondition.class, trace(trail, "b",
          HandlerOps.restart(Restarts.use(FIXED_RESULT))));

        try (Scope c = Scopes.create()) {
          Object actual = c.raise(new PleaseSignalSomethingElse());

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
      a.signal(condition, Policies.error());

      fail();
    } catch (HandlerNotFoundException e) {
      assertEquals(condition, e.getCondition());
    }
  }

  @Test
  public void signallingWithNoHandlersAndAnIgnorePolicyNopesOut() {
    try (Scope a = Scopes.create()) {
      a.signal(new BasicCondition("test"), Policies.ignore());
    }

    // nothing happens, and the returned result is meaningless, so nothing to assert
  }

  @Test
  public void notifyIsTheSameAsSignallingWithAnIgnorePolicyAndResume() {
    List<String> trail = new ArrayList<>();

    try (Scope a = Scopes.create()) {
      a.handle(BasicCondition.class, (c, ops) -> {
        trail.add(c.getValue());

        assertOptionsMatch(
          Collections.singletonList(Restarts.resume()),
          ops.getScope().getAllRestarts());

        return ops.skip(); // no handling provided
      });

      try (Scope b = Scopes.create()) {
        b.notify(new BasicCondition("notify"));
        b.signal(new BasicCondition("signal"), Policies.ignore(), Restarts.resume());
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
      a.handle(BasicCondition.class, (c, ops) -> {
        trail.add(c.getValue());

        assertOptionsMatch(
          Collections.singletonList(u),
          ops.getScope().getAllRestarts());

        return ops.restart(Restarts.use(TEST_VALUE));
      });

      try (Scope b = Scopes.create()) {
        assertEquals(TEST_VALUE, b.raise(new BasicCondition("raise")));
      }
    }

    assertLinesMatch(Collections.singletonList("raise"), trail);
  }

  @Test
  public void raiseUsesAnErrorPolicy() {
    BasicCondition condition = new BasicCondition("raise");

    try (Scope a = Scopes.create()) {
      a.raise(condition);

      fail();
    } catch (HandlerNotFoundException e) {
      assertEquals(condition, e.getCondition());
    }
  }


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

  static void assertOptionsMismatch(List<Restart.Option> options, Iterable<Restart<?>> iterable) {
    assertFalse(matches(options, iterable));
  }

  static void assertOptionsMismatch(List<Restart.Option> options, Iterable<Restart<?>> iterable, String message) {
    assertFalse(matches(options, iterable), message);
  }

  static Stream<Function<String, Condition>> skipHandlingProvider() {
    return Stream.of(
      BasicCondition::new,
      SonOfBasicCondition::new
    );
  }

  static BiFunction<Condition, Handler.Operations, Handler.Decision> trace(List<String> trail, String message, BiFunction<Condition, Handler.Operations, Handler.Decision> body) {
    return (c, ops) -> {
      trail.add(message);

      return body.apply(c, ops);
    };
  }
}
