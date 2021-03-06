package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sbrubbles.conditio.fixtures.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class SignallingTest {
  private static final String FIXED_TEXT = "0000 FIXED TEXT";
  private static final Entry FIXED_ENTRY = new Entry(FIXED_TEXT);
  private static final Entry USE_VALUE_ENTRY = new Entry("1000 USE VALUE ENTRY");

  private static final String GOOD_LOG = "../good.txt";
  private static final String BAD_LOG = "../bad.txt";

  private LoggingFixture fixture;

  @BeforeEach
  public void buildFixture() {
    fixture = new LoggingFixture();
  }

  @Test
  public void readGoodLog() throws Exception {
    List<AnalyzedEntry> expected = Arrays.asList(
      new AnalyzedEntry(goodLine(1), GOOD_LOG),
      new AnalyzedEntry(goodLine(2), GOOD_LOG),
      new AnalyzedEntry(goodLine(3), GOOD_LOG),
      new AnalyzedEntry(goodLine(4), GOOD_LOG));

    List<AnalyzedEntry> actual = fixture.logAnalyzer(GOOD_LOG);

    assertEquals(expected, actual);
    assertTrue(fixture.getHandlerTrace().isEmpty());
    assertTrue(fixture.getRestartTrace().isEmpty());
  }

  @ParameterizedTest
  @MethodSource("handleBadLogProvider")
  public void handleBadLogWithRestarts(ExpectedHandler expectedHandler, ExpectedRestart expectedRestart, List<AnalyzedEntry> expectedEntries, Restart.Option restartOption) throws Exception {
    switch (expectedHandler) {
      case LOG_ANALYZER:
        fixture.setLogAnalyzer(true);
        break;
      case ANALYZE_LOG:
        fixture.setAnalyzeLog(true);
        break;
    }

    fixture.setRestartOptionToUse(restartOption);

    List<AnalyzedEntry> actual = fixture.logAnalyzer(BAD_LOG);

    String handlerTrace = expectedHandler.getMethodName() + ": " + MalformedLogEntry.class.getSimpleName();
    String restartOptionTrace = expectedRestart.getRestartName();

    assertEquals(expectedEntries, actual);
    assertLinesMatch( // the handler gets called once for each bad line in the log; in bad.txt, there's two
      Arrays.asList(handlerTrace, handlerTrace),
      fixture.getHandlerTrace());
    assertLinesMatch(
      Arrays.asList(restartOptionTrace, restartOptionTrace),
      fixture.getRestartTrace());
  }

  @Test
  public void readBadLogWithNoHandlingAtAll() throws Exception {
    final MalformedLogEntry expectedCondition = new MalformedLogEntry(badLine(2).getText());

    try {
      fixture.logAnalyzer(BAD_LOG);
      fail();
    } catch (HandlerNotFoundException e) {
      assertEquals(expectedCondition, e.getCondition());

      assertLinesMatch(Collections.emptyList(), fixture.getHandlerTrace());
      assertLinesMatch(Collections.emptyList(), fixture.getRestartTrace());
    }
  }

  @Test
  public void readBadLogWithNoHandlerFound() throws Exception {
    final Condition expectedCondition = new BasicCondition("oops");

    fixture.setLogAnalyzer(true);
    fixture.setConditionProvider(str -> expectedCondition); // won't match MalformedLogEntry
    fixture.setRestartOptionToUse(new UseValue(USE_VALUE_ENTRY)); // should never be called

    try {
      fixture.logAnalyzer(BAD_LOG);
      fail();
    } catch (HandlerNotFoundException e) {
      assertEquals(expectedCondition, e.getCondition());

      assertLinesMatch(Collections.emptyList(), fixture.getHandlerTrace());
      assertLinesMatch(Collections.emptyList(), fixture.getRestartTrace());
    }
  }

  @Test
  public void readBadLogWithNullRestartOption() throws Exception {
    fixture.setLogAnalyzer(true);
    fixture.setRestartOptionToUse(null);

    try {
      fixture.logAnalyzer(BAD_LOG);
      fail();
    } catch (RestartNotFoundException e) {
      assertNull(e.getRestartOption());

      assertLinesMatch(Arrays.asList("logAnalyzer: " + MalformedLogEntry.class.getSimpleName()), fixture.getHandlerTrace());
      assertLinesMatch(Collections.emptyList(), fixture.getRestartTrace());
    }
  }

  @Test
  public void readBadLogWithNoRestartFound() throws Exception {
    final Restart.Option UNKNOWN_RESTART_OPTION = new UnknownRestartOption("oops");

    fixture.setLogAnalyzer(true);
    fixture.setRestartOptionToUse(UNKNOWN_RESTART_OPTION); // no restart will match

    try {
      fixture.logAnalyzer(BAD_LOG);
      fail();
    } catch (RestartNotFoundException e) {
      assertEquals(UNKNOWN_RESTART_OPTION, e.getRestartOption());

      assertLinesMatch(Arrays.asList("logAnalyzer: " + MalformedLogEntry.class.getSimpleName()), fixture.getHandlerTrace());
      assertLinesMatch(Collections.emptyList(), fixture.getRestartTrace());
    }
  }

  @Test
  public void readBadLogUsingNoRestarts() throws Exception {
    final Entry FIXED_ENTRY = new Entry("1111 FIXED ENTRY");
    fixture.setConditionProvider(t -> new NoRestartUsed(FIXED_ENTRY));

    List<AnalyzedEntry> actual = fixture.logAnalyzer(BAD_LOG);

    assertEquals(
      Arrays.asList(
        new AnalyzedEntry(goodLine(1), BAD_LOG),
        new AnalyzedEntry(FIXED_ENTRY, BAD_LOG),
        new AnalyzedEntry(goodLine(3), BAD_LOG),
        new AnalyzedEntry(FIXED_ENTRY, BAD_LOG)),
      actual);
    assertLinesMatch(
      Arrays.asList(
        "logAnalyzer: " + NoRestartUsed.class.getSimpleName(),
        "logAnalyzer: " + NoRestartUsed.class.getSimpleName()),
      fixture.getHandlerTrace());
    assertLinesMatch(
      Collections.emptyList(),
      fixture.getRestartTrace());
  }

  @Test
  public void readBadLogWithResignalling() throws Exception {
    fixture.setConditionProvider(t -> new PleaseSignalSomethingElse());

    List<AnalyzedEntry> actual = fixture.logAnalyzer(BAD_LOG);

    assertEquals(
      Arrays.asList(
        new AnalyzedEntry(goodLine(1), BAD_LOG),
        new AnalyzedEntry(goodLine(3), BAD_LOG)),
      actual);
    assertLinesMatch(
      Arrays.asList(
        "logAnalyzer: " + PleaseSignalSomethingElse.class.getSimpleName(),
        "analyzeLog: " + SomethingElse.class.getSimpleName(),
        "logAnalyzer: " + PleaseSignalSomethingElse.class.getSimpleName(),
        "analyzeLog: " + SomethingElse.class.getSimpleName()),
      fixture.getHandlerTrace());
    assertLinesMatch(
      Arrays.asList("SkipEntry", "SkipEntry"),
      fixture.getRestartTrace());
  }

  @ParameterizedTest
  @MethodSource("skipHandlingProvider")
  public void skipHandling(final Class<? extends Condition> conditionType, final Function<Entry, Condition> conditionBuilder) throws Exception {
    final Entry SIGNAL_ENTRY = new Entry("OMG");

    fixture.setConditionProvider(str -> conditionBuilder.apply(SIGNAL_ENTRY));

    List<AnalyzedEntry> actual = fixture.logAnalyzer(BAD_LOG);

    assertEquals(
      Arrays.asList(
        new AnalyzedEntry(goodLine(1), BAD_LOG),
        new AnalyzedEntry(SIGNAL_ENTRY, BAD_LOG),
        new AnalyzedEntry(goodLine(3), BAD_LOG),
        new AnalyzedEntry(SIGNAL_ENTRY, BAD_LOG)),
      actual);
    assertLinesMatch(
      Arrays.asList(
        "analyzeLog: " + conditionType.getSimpleName(),
        "logAnalyzer: " + conditionType.getSimpleName(),
        "analyzeLog: " + conditionType.getSimpleName(),
        "logAnalyzer: " + conditionType.getSimpleName()),
      fixture.getHandlerTrace());
    assertLinesMatch(
      Arrays.asList("UseValue", "UseValue"),
      fixture.getRestartTrace());
  }

  @Test
  public void signalRemovesTheRestartsAfterwards() {
    final Restart USE_VALUE = Restart.on(UseValue.class, UseValue::getValue);
    final String TEST_STR = "test";
    final Restart.Option u = new UseValue(TEST_STR);

    try (Scope a = Scope.create()) {
      // no restart before the handler...
      assertFalse(toStream(a.getAllRestarts()).anyMatch(r -> r.test(u)), "before handle");

      a.handle(MalformedLogEntry.class, (c, s) -> {
        // now there's something!
        assertTrue(toStream(s.getAllRestarts()).anyMatch(r -> r.test(u)), "inside handle");

        return s.restart(u);
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

      a.handle(MalformedLogEntry.class, (c, s) -> {
        assertTrue(toStream(s.getAllRestarts()).anyMatch(r -> r.test(u)), "inside handle");

        return s.restart(u);
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

  // helpers
  static <T> Stream<T> toStream(Iterable<T> iterable) {
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  static Entry goodLine(int line) {
    return new Entry(String.format("%04d OK", line));
  }

  static Entry badLine(int line) {
    return new Entry(String.format("%04d FAIL", line));
  }

  enum ExpectedHandler {
    LOG_ANALYZER("logAnalyzer"), ANALYZE_LOG("analyzeLog");

    private final String methodName;

    ExpectedHandler(String methodName) {
      this.methodName = methodName;
    }

    public String getMethodName() {
      return methodName;
    }
  }

  enum ExpectedRestart {
    USE_VALUE("UseValue"), RETRY_WITH("RetryWith"), SKIP_ENTRY("SkipEntry");

    private final String restartName;

    ExpectedRestart(String restartName) {
      this.restartName = restartName;
    }

    public String getRestartName() {
      return restartName;
    }
  }

  static Stream<Arguments> handleBadLogProvider() {
    return Stream.of(
      arguments(
        ExpectedHandler.LOG_ANALYZER,
        ExpectedRestart.USE_VALUE,
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG),
          new AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG)),
        new UseValue(USE_VALUE_ENTRY)),
      arguments(
        ExpectedHandler.ANALYZE_LOG,
        ExpectedRestart.USE_VALUE,
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG),
          new AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG)),
        new UseValue(USE_VALUE_ENTRY)),
      arguments(
        ExpectedHandler.LOG_ANALYZER,
        ExpectedRestart.USE_VALUE,
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG),
          new AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG)),
        new SonOfUseValue(USE_VALUE_ENTRY)),
      arguments(
        ExpectedHandler.ANALYZE_LOG,
        ExpectedRestart.USE_VALUE,
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG),
          new AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG)),
        new SonOfUseValue(USE_VALUE_ENTRY)),
      arguments(
        ExpectedHandler.LOG_ANALYZER,
        ExpectedRestart.RETRY_WITH,
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(FIXED_ENTRY, BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG),
          new AnalyzedEntry(FIXED_ENTRY, BAD_LOG)),
        new RetryWith(FIXED_TEXT)),
      arguments(
        ExpectedHandler.ANALYZE_LOG,
        ExpectedRestart.RETRY_WITH,
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(FIXED_ENTRY, BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG),
          new AnalyzedEntry(FIXED_ENTRY, BAD_LOG)),
        new RetryWith(FIXED_TEXT)),
      arguments(
        ExpectedHandler.LOG_ANALYZER,
        ExpectedRestart.SKIP_ENTRY,
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG)),
        new SkipEntry()),
      arguments(
        ExpectedHandler.ANALYZE_LOG,
        ExpectedRestart.SKIP_ENTRY,
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG)),
        new SkipEntry())
    );
  }

  static Stream<Arguments> skipHandlingProvider() {
    return Stream.of(
      arguments(SkipHandler.class, (Function<Entry, Condition>) SkipHandler::new),
      arguments(SonOfSkipHandler.class, (Function<Entry, Condition>) SonOfSkipHandler::new)
    );
  }

  static class UnknownRestartOption implements Restart.Option {
    private final Object value;

    public UnknownRestartOption(Object value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      UnknownRestartOption that = (UnknownRestartOption) o;
      return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public String toString() {
      return "BadRestartOption(" + value + ")";
    }
  }
}
