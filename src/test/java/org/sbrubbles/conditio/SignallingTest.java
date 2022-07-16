package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sbrubbles.conditio.fixtures.*;
import org.sbrubbles.conditio.restarts.RetryWith;
import org.sbrubbles.conditio.restarts.UseValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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
  public void handleBadLog(HandlerOption handlerOption, Restart.Option restartOption, List<AnalyzedEntry> expected) throws Exception {
    switch (handlerOption) {
      case LOG_ANALYZER:
        fixture.setLogAnalyzer(true);
        break;
      case ANALYZE_LOG:
        fixture.setAnalyzeLog(true);
        break;
    }

    fixture.setRestartOptionToUse(restartOption);

    List<AnalyzedEntry> actual = fixture.logAnalyzer(BAD_LOG);
    String handlerMethodName = handlerOption.getMethodName();
    String restartName = restartOption.getClass().getSimpleName();

    assertEquals(expected, actual);
    assertLinesMatch( // the handler gets called once for each bad line in the log; in bad.txt, there's two
      Arrays.asList(handlerMethodName, handlerMethodName),
      fixture.getHandlerTrace());
    assertLinesMatch(
      Arrays.asList(restartName, restartName),
      fixture.getRestartTrace());
  }

  @Test
  public void readBadLogWithNoHandlingAtAll() throws Exception {
    try {
      fixture.logAnalyzer(BAD_LOG);
      fail();
    } catch (HandlerNotFoundException e) {
      Condition actual = e.getCondition();
      assertNotNull(actual);
      assertEquals(MalformedLogEntry.class, actual.getClass());
      assertEquals(badLine(2).getText(), ((MalformedLogEntry) actual).getText());

      assertLinesMatch(Collections.emptyList(), fixture.getHandlerTrace());
      assertLinesMatch(Collections.emptyList(), fixture.getRestartTrace());
    }
  }

  @Test
  public void readBadLogWithNoHandlerFound() throws Exception {
    final String VALUE = "oops";

    fixture.setLogAnalyzer(true);
    fixture.setConditionProvider((s, str) -> new BasicCondition(s, VALUE)); // won't match MalformedLogEntry
    fixture.setRestartOptionToUse(new UseValue(USE_VALUE_ENTRY)); // should never be called

    try {
      fixture.logAnalyzer(BAD_LOG);
      fail();
    } catch (HandlerNotFoundException e) {
      Condition actual = e.getCondition();
      assertNotNull(actual);
      assertEquals(BasicCondition.class, actual.getClass());
      assertEquals(VALUE, ((BasicCondition) actual).getValue());

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

      assertLinesMatch(Arrays.asList("logAnalyzer"), fixture.getHandlerTrace());
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

      assertLinesMatch(Arrays.asList("logAnalyzer"), fixture.getHandlerTrace());
      assertLinesMatch(Collections.emptyList(), fixture.getRestartTrace());
    }
  }

  @Test
  public void skipHandlingACondition() throws Exception {
    try (Scope scope = Scope.create()) {
      final Entry SIGNAL_ENTRY = new Entry("OMG");

      fixture.setConditionProvider((s, str) -> new OneOff(s, SIGNAL_ENTRY));

      List<AnalyzedEntry> actual = fixture.logAnalyzer(BAD_LOG);

      assertEquals(
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(SIGNAL_ENTRY, BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG),
          new AnalyzedEntry(SIGNAL_ENTRY, BAD_LOG)),
        actual);
      assertLinesMatch(
        Arrays.asList("analyzeLog", "logAnalyzer", "analyzeLog", "logAnalyzer"),
        fixture.getHandlerTrace());
      assertLinesMatch(
        Arrays.asList("UseValue", "UseValue"),
        fixture.getRestartTrace());
    }
  }

  // helpers
  static Entry goodLine(int line) {
    return new Entry(String.format("%04d OK", line));
  }

  static Entry badLine(int line) {
    return new Entry(String.format("%04d FAIL", line));
  }

  enum HandlerOption {
    LOG_ANALYZER("logAnalyzer"), ANALYZE_LOG("analyzeLog");

    private final String methodName;

    HandlerOption(String methodName) {
      this.methodName = methodName;
    }

    public String getMethodName() {
      return methodName;
    }
  }

  static Stream<Arguments> handleBadLogProvider() {
    return Stream.of(
      arguments(
        HandlerOption.LOG_ANALYZER,
        new UseValue(USE_VALUE_ENTRY),
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG),
          new AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG))),
      arguments(
        HandlerOption.ANALYZE_LOG,
        new UseValue(USE_VALUE_ENTRY),
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG),
          new AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG))),
      arguments(
        HandlerOption.LOG_ANALYZER,
        new RetryWith(FIXED_TEXT),
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(FIXED_ENTRY, BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG),
          new AnalyzedEntry(FIXED_ENTRY, BAD_LOG))),
      arguments(
        HandlerOption.ANALYZE_LOG,
        new RetryWith(FIXED_TEXT),
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(FIXED_ENTRY, BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG),
          new AnalyzedEntry(FIXED_ENTRY, BAD_LOG))),
      arguments(
        HandlerOption.LOG_ANALYZER,
        new SkipEntry(),
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG))),
      arguments(
        HandlerOption.ANALYZE_LOG,
        new SkipEntry(),
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG)))
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
