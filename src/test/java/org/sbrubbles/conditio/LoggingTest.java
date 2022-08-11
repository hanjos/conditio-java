package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sbrubbles.conditio.fixtures.logging.*;
import org.sbrubbles.conditio.restarts.UseValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class LoggingTest {
  private static final String FIXED_TEXT = "0000 FIXED TEXT";
  private static final Entry FIXED_ENTRY = new Entry(FIXED_TEXT);
  private static final Entry USE_VALUE_ENTRY = new Entry("1000 USE VALUE ENTRY");

  private static final String GOOD_LOG = "../../good.txt";
  private static final String BAD_LOG = "../../bad.txt";

  private LoggingFixture fixture;

  @BeforeEach
  public void setUp() {
    fixture = new LoggingFixture();
  }

  @Test
  public void readGoodLog() {
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
  public void handleBadLogWithRestarts(ExpectedHandler expectedHandler, ExpectedRestart expectedRestart, List<AnalyzedEntry> expectedEntries, Restart.Option restartOption) {
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
    String restartOptionTrace = expectedRestart.getRestartName() + ": " + restartOption.getClass().getSimpleName();

    assertEquals(expectedEntries, actual);
    assertLinesMatch( // the handler gets called once for each bad line in the log; in bad.txt, there's two
      Arrays.asList(handlerTrace, handlerTrace),
      fixture.getHandlerTrace());
    assertLinesMatch(
      Arrays.asList(restartOptionTrace, restartOptionTrace),
      fixture.getRestartTrace());
  }

  @Test
  public void readBadLogWithNoHandlingAtAll() {
    final MalformedLogEntry expectedCondition = new MalformedLogEntry("0002 FAIL");

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
  public void readBadLogWithNoHandlerFound() {
    final Condition expectedCondition = new UnknownEntryCondition();

    fixture.setLogAnalyzer(true);
    fixture.setConditionProvider(str -> expectedCondition); // won't match MalformedLogEntry
    fixture.setRestartOptionToUse(new UseValue<>(USE_VALUE_ENTRY)); // should never be called

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
  public void readBadLogWithNullRestartOption() {
    fixture.setLogAnalyzer(true);
    fixture.setRestartOptionToUse(null);

    try {
      fixture.logAnalyzer(BAD_LOG);
      fail();
    } catch (RestartNotFoundException e) {
      assertNull(e.getRestartOption());

      assertLinesMatch(Collections.singletonList("logAnalyzer: " + MalformedLogEntry.class.getSimpleName()), fixture.getHandlerTrace());
      assertLinesMatch(Collections.emptyList(), fixture.getRestartTrace());
    }
  }

  @Test
  public void readBadLogWithNoRestartFound() {
    final Restart.Option UNKNOWN_RESTART_OPTION = new UnknownRestartOption("oops");

    fixture.setLogAnalyzer(true);
    fixture.setRestartOptionToUse(UNKNOWN_RESTART_OPTION); // no restart will match

    try {
      fixture.logAnalyzer(BAD_LOG);
      fail();
    } catch (RestartNotFoundException e) {
      assertEquals(UNKNOWN_RESTART_OPTION, e.getRestartOption());

      assertLinesMatch(Collections.singletonList("logAnalyzer: " + MalformedLogEntry.class.getSimpleName()), fixture.getHandlerTrace());
      assertLinesMatch(Collections.emptyList(), fixture.getRestartTrace());
    }
  }

  // helpers
  static Entry goodLine(int line) {
    return new Entry(String.format("%04d OK", line));
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
        new UseValue<>(USE_VALUE_ENTRY)),
      arguments(
        ExpectedHandler.ANALYZE_LOG,
        ExpectedRestart.USE_VALUE,
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG),
          new AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG)),
        new UseValue<>(USE_VALUE_ENTRY)),
      arguments(
        ExpectedHandler.LOG_ANALYZER,
        ExpectedRestart.USE_VALUE,
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG),
          new AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG)),
        new SonOfUseValue<>(USE_VALUE_ENTRY)),
      arguments(
        ExpectedHandler.ANALYZE_LOG,
        ExpectedRestart.USE_VALUE,
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG),
          new AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG)),
        new SonOfUseValue<>(USE_VALUE_ENTRY)),
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
      return "UnknownRestartOption(" + value + ")";
    }
  }

  static class UnknownEntryCondition extends Condition { }
}
