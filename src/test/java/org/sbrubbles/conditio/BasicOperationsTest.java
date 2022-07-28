package org.sbrubbles.conditio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.fixtures.PleaseSignalSomethingElse;
import org.sbrubbles.conditio.fixtures.SonOfBasicCondition;
import org.sbrubbles.conditio.fixtures.logging.MalformedLogEntry;
import org.sbrubbles.conditio.fixtures.logging.UseValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

public class BasicOperationsTest {
  @Test
  public void signalRemovesTheRestartsAfterwards() {
    final Restart USE_VALUE = Restart.on(UseValue.class, UseValue::getValue);
    final String TEST_STR = "test";
    final Restart.Option u = new UseValue(TEST_STR);

    try (Scope a = Scope.create()) {
      // no restart before the handler...
      assertFalse(toStream(a.getAllRestarts()).anyMatch(r -> r.test(u)), "before handle");

      a.handle(MalformedLogEntry.class, (c, ops) -> {
        // now there's something!
        assertTrue(toStream(ops.getScope().getAllRestarts()).anyMatch(r -> r.test(u)), "inside handle");

        return ops.restart(u);
      });

      // no restart after either
      assertFalse(toStream(a.getAllRestarts()).anyMatch(r -> r.test(u)), "after handle");

      try (Scope b = Scope.create()) {
        // no restart before signal...
        assertFalse(toStream(b.getAllRestarts()).anyMatch(r -> r.test(u)), "before signal");

        assertEquals(TEST_STR, b.signal(new MalformedLogEntry(""), USE_VALUE));

        // no restart after either...
        assertFalse(toStream(b.getAllRestarts()).anyMatch(r -> r.test(u)), "after signal");
      }
    }
  }

  @Test
  public void callRemovesTheRestartsAfterwards() {
    final Restart USE_VALUE = Restart.on(UseValue.class, UseValue::getValue);
    final String TEST_STR = "test";
    final Restart.Option u = new UseValue(TEST_STR);

    try (Scope a = Scope.create()) {
      assertFalse(toStream(a.getAllRestarts()).anyMatch(r -> r.test(u)), "before handle");

      a.handle(MalformedLogEntry.class, (c, ops) -> {
        assertTrue(toStream(ops.getScope().getAllRestarts()).anyMatch(r -> r.test(u)), "inside handle");

        return ops.restart(u);
      });

      assertEquals(TEST_STR, a.call(
        () -> {
          try (Scope b = Scope.create()) {
            assertTrue(toStream(b.getAllRestarts()).anyMatch(r -> r.test(u)), "inside call");

            return b.signal(new MalformedLogEntry(""));
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

    try (Scope a = Scope.create()) {
      a.handle(BasicCondition.class, (c, ops) -> {
        trace.add("a");
        return ops.use(EXPECTED_RESULT);
      });

      try (Scope b = Scope.create()) {
        b.handle(BasicCondition.class, (c, ops) -> {
          trace.add("b");
          return ops.skip();
        });

        try (Scope c = Scope.create()) {
          Object actual = c.signal(builder.apply(EXPECTED_RESULT));

          assertEquals(EXPECTED_RESULT, actual);
          assertLinesMatch(
            Arrays.asList("b", "a"),
            trace);
        }
      }
    }
  }

  @Test
  public void use() throws Exception {
    final String EXPECTED_RESULT = "<result>";
    final List<String> trace = new ArrayList<>();

    try (Scope a = Scope.create()) {
      a.handle(BasicCondition.class,
        (c, ops) -> {
          trace.add("a");
          return ops.use(EXPECTED_RESULT);
        });

      try (Scope b = Scope.create()) {
        Object actual = b.signal(new BasicCondition(""));

        assertEquals(EXPECTED_RESULT, actual);
        assertLinesMatch(Arrays.asList("a"), trace);
      }
    }
  }

  @Test
  public void resignal() {
    final String FIXED_RESULT = "<result>";
    final List<String> trace = new ArrayList<>();

    try (Scope a = Scope.create()) {
      a.handle(PleaseSignalSomethingElse.class,
        (c, ops) -> {
          trace.add("a");
          try (Scope s = Scope.create()) {
            return ops.use(s.signal(new BasicCondition(null)));
          }
        });

      try (Scope b = Scope.create()) {
        b.handle(BasicCondition.class,
          (c, ops) -> {
            trace.add("b");
            return ops.use(FIXED_RESULT);
          });

        try (Scope c = Scope.create()) {
          Object actual = c.signal(new PleaseSignalSomethingElse());

          assertEquals(FIXED_RESULT, actual);
          assertLinesMatch(
            Arrays.asList("a", "b"),
            trace);
        }
      }
    }

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
