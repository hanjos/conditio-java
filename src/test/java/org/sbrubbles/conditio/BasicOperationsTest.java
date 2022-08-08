package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.fixtures.BasicNotice;
import org.sbrubbles.conditio.fixtures.PleaseSignalSomethingElse;
import org.sbrubbles.conditio.fixtures.SonOfBasicCondition;
import org.sbrubbles.conditio.fixtures.logging.Entry;
import org.sbrubbles.conditio.fixtures.logging.MalformedLogEntry;
import org.sbrubbles.conditio.fixtures.logging.UseValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

public class BasicOperationsTest {
  @Test
  public void signalRemovesTheRestartsAfterwards() {
    final Restart<Entry> USE_VALUE = Restart.on(UseValue.class, r -> (Entry) r.getValue());
    final Entry TEST_VALUE = new Entry("test");
    final Restart.Option u = new UseValue(TEST_VALUE);

    try (Scope a = Scopes.create()) {
      // no restart before the handler...
      assertFalse(toStream(a.getAllRestarts()).anyMatch(r -> r.test(u)), "before handle");

      a.handle(MalformedLogEntry.class, (c, t, ops) -> {
        // now there's something!
        assertTrue(toStream(ops.getScope().getAllRestarts()).anyMatch(r -> r.test(u)), "inside handle");

        return ops.restart(u);
      });

      // no restart after either
      assertFalse(toStream(a.getAllRestarts()).anyMatch(r -> r.test(u)), "after handle");

      try (Scope b = Scopes.create()) {
        // no restart before signal...
        assertFalse(toStream(b.getAllRestarts()).anyMatch(r -> r.test(u)), "before signal");

        assertEquals(TEST_VALUE, b.signal(new MalformedLogEntry(""), Entry.class, USE_VALUE));

        // no restart after either...
        assertFalse(toStream(b.getAllRestarts()).anyMatch(r -> r.test(u)), "after signal");
      }
    }
  }

  @Test
  public void callRemovesTheRestartsAfterwards() {
    final Restart<Entry> USE_VALUE = Restart.on(UseValue.class, r -> (Entry) r.getValue());
    final Entry TEST_VALUE = new Entry("test");
    final Restart.Option u = new UseValue(TEST_VALUE);

    try (Scope a = Scopes.create()) {
      assertFalse(toStream(a.getAllRestarts()).anyMatch(r -> r.test(u)), "before handle");

      a.handle(MalformedLogEntry.class, (c, t, ops) -> {
        assertTrue(toStream(ops.getScope().getAllRestarts()).anyMatch(r -> r.test(u)), "inside handle");

        return ops.restart(u);
      });

      assertEquals(TEST_VALUE, a.call(
        () -> {
          try (Scope b = Scopes.create()) {
            assertTrue(toStream(b.getAllRestarts()).anyMatch(r -> r.test(u)), "inside call");

            return b.signal(new MalformedLogEntry(""), Entry.class);
          }
        },
        USE_VALUE));

      assertFalse(toStream(a.getAllRestarts()).anyMatch(r -> r.test(u)), "after handle");
    }
  }

  @ParameterizedTest
  @MethodSource("skipHandlingProvider")
  public void skip(Function<String, Condition> builder) {
    final String EXPECTED_RESULT = "<result>";
    final List<String> trace = new ArrayList<>();

    try (Scope a = Scopes.create()) {
      a.handle(BasicCondition.class, (c, t, ops) -> {
        trace.add("a");
        return ops.use(EXPECTED_RESULT);
      });

      try (Scope b = Scopes.create()) {
        b.handle(BasicCondition.class, (c, t, ops) -> {
          trace.add("b");
          return ops.skip();
        });

        try (Scope c = Scopes.create()) {
          String actual = c.signal(builder.apply(EXPECTED_RESULT), String.class);

          assertEquals(EXPECTED_RESULT, actual);
          assertLinesMatch(
            Arrays.asList("b", "a"),
            trace);
        }
      }
    }
  }

  @Test
  public void use() {
    final String EXPECTED_RESULT = "<result>";
    final List<String> trace = new ArrayList<>();

    try (Scope a = Scopes.create()) {
      a.handle(BasicCondition.class,
        (c, t, ops) -> {
          trace.add("a");
          return ops.use(EXPECTED_RESULT);
        });

      try (Scope b = Scopes.create()) {
        Object actual = b.signal(new BasicCondition(""), Object.class);

        assertEquals(EXPECTED_RESULT, actual);
        assertLinesMatch(Collections.singletonList("a"), trace);
      }
    }
  }

  @Test
  public void resignal() {
    final String FIXED_RESULT = "<result>";
    final List<String> trace = new ArrayList<>();

    try (Scope a = Scopes.create()) {
      a.handle(PleaseSignalSomethingElse.class,
        (c, t, ops) -> {
          trace.add("a");
          try (Scope s = Scopes.create()) {
            return ops.use(s.signal(new BasicCondition(null), String.class));
          }
        });

      try (Scope b = Scopes.create()) {
        b.handle(BasicCondition.class,
          (c, t, ops) -> {
            trace.add("b");
            return ops.use(FIXED_RESULT);
          });

        try (Scope c = Scopes.create()) {
          Object actual = c.signal(new PleaseSignalSomethingElse(), Object.class);

          assertEquals(FIXED_RESULT, actual);
          assertLinesMatch(
            Arrays.asList("a", "b"),
            trace);
        }
      }
    }
  }

  @Test
  public void signallingAConditionWithNoHandlersErrorsOut() {
    BasicCondition condition = new BasicCondition("test");

    try (Scope a = Scopes.create()) {
      a.signal(condition);

      fail();
    } catch (HandlerNotFoundException e) {
      assertEquals(condition, e.getCondition());
    }
  }

  @Test
  public void signallingANoticeWithNoHandlersJustNopesOut() {
    BasicNotice condition = new BasicNotice("test");

    try (Scope a = Scopes.create()) {
      a.signal(condition);
    }

    // nothing happens, and the returned result is meaningless, so nothing to assert
  }

  static <T> Stream<T> toStream(Iterable<T> iterable) {
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  static Stream<Function<String, Condition>> skipHandlingProvider() {
    return Stream.of(
      BasicCondition::new,
      SonOfBasicCondition::new
    );
  }
}
