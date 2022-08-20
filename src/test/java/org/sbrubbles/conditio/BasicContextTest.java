package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.fixtures.PleaseSignalSomethingElse;
import org.sbrubbles.conditio.fixtures.SonOfBasicCondition;
import org.sbrubbles.conditio.fixtures.logging.Entry;
import org.sbrubbles.conditio.fixtures.logging.MalformedLogEntry;
import org.sbrubbles.conditio.handlers.Handlers;
import org.sbrubbles.conditio.policies.HandlerNotFoundPolicy;
import org.sbrubbles.conditio.policies.Policies;
import org.sbrubbles.conditio.policies.ReturnTypePolicy;
import org.sbrubbles.conditio.restarts.Restarts;
import org.sbrubbles.conditio.restarts.Resume;
import org.sbrubbles.conditio.restarts.UseValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
public class BasicContextTest {
  @Test
  public void signalRemovesTheRestartsAfterwards() {
    final Restart<Entry> USE_VALUE = Restart.on(UseValue.class, r -> (Entry) r.getValue());
    final Entry TEST_VALUE = new Entry("test");
    final Restart.Option u = new UseValue<>(TEST_VALUE);

    try (Scope a = Scopes.create()) {
      // no restart before the handler...
      assertOptionsDontMatch(Collections.singletonList(u), a.getAllRestarts(), "before handle");

      a.handle(MalformedLogEntry.class, ctx -> {
        // now there's something!
        assertOptionsMatch(Collections.singletonList(u), ctx.getScope().getAllRestarts(), "inside handle");

        return ctx.restart(u);
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

      a.handle(MalformedLogEntry.class, ctx -> {
        assertOptionsMatch(Arrays.asList(u, r), ctx.getScope().getAllRestarts(), "inside handle");

        return ctx.restart(u);
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
        trace(trail, "a", ctx -> {
          try (Scope s = Scopes.create()) {
            return ctx.restart(Restarts.use(s.raise(new BasicCondition(null), ctx.getPolicies().getExpectedType())));
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
      a.signal(condition, new Policies<>(HandlerNotFoundPolicy.error(), null));

      fail();
    } catch (HandlerNotFoundException e) {
      assertEquals(condition, e.getCondition());
    }
  }

  @Test
  public void signallingWithNoHandlersAndAnIgnorePolicyNopesOut() {
    try (Scope a = Scopes.create()) {
      a.signal(new BasicCondition("test"), new Policies<>(HandlerNotFoundPolicy.ignore(), null));
    }

    // nothing happens, and the returned result is meaningless, so nothing to assert
  }

  @Test
  public void notifyIsTheSameAsSignallingWithAnIgnorePolicyAndResume() {
    List<String> trail = new ArrayList<>();

    try (Scope a = Scopes.create()) {
      a.handle(BasicCondition.class, ctx -> {
        trail.add(ctx.getCondition().getValue());

        assertOptionsMatch(
          Collections.singletonList(Restarts.resume()),
          ctx.getScope().getAllRestarts());

        return ctx.skip(); // no handling provided
      });

      try (Scope b = Scopes.create()) {
        b.notify(new BasicCondition("notify"));
        b.signal(new BasicCondition("signal"), new Policies<>(HandlerNotFoundPolicy.ignore(), null), Restarts.resume());
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
      a.handle(BasicCondition.class, ctx -> {
        trail.add(ctx.getCondition().getValue());

        assertOptionsMatch(
          Collections.singletonList(u),
          ctx.getScope().getAllRestarts());

        return ctx.restart(Restarts.use(TEST_VALUE));
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

  static void assertOptionsDontMatch(List<Restart.Option> options, Iterable<Restart<?>> iterable, String message) {
    assertFalse(matches(options, iterable), message);
  }

  static Stream<Function<String, Condition>> skipHandlingProvider() {
    return Stream.of(
      BasicCondition::new,
      SonOfBasicCondition::new
    );
  }

  static <C extends Condition> Function<Handler.Context<C>, Handler.Decision> trace(List<String> trail, String message, Function<Handler.Context<C>, Handler.Decision> body) {
    return ctx -> {
      trail.add(message);

      return body.apply(ctx);
    };
  }
}
