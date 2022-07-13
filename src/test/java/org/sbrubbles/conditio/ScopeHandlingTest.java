package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sbrubbles.conditio.fixtures.LoggingFixture;
import org.sbrubbles.conditio.fixtures.LoggingFixture.AnalyzedEntry;
import org.sbrubbles.conditio.fixtures.LoggingFixture.Entry;
import org.sbrubbles.conditio.fixtures.LoggingFixture.MalformedLogEntry;
import org.sbrubbles.conditio.fixtures.RetryWith;
import org.sbrubbles.conditio.fixtures.SkipEntry;
import org.sbrubbles.conditio.fixtures.UseValue;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class ScopeHandlingTest {
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
    // no signal should be emitted
    List<AnalyzedEntry> expected = Arrays.asList(
      new AnalyzedEntry(goodLine(1), GOOD_LOG),
      new AnalyzedEntry(goodLine(2), GOOD_LOG),
      new AnalyzedEntry(goodLine(3), GOOD_LOG),
      new AnalyzedEntry(goodLine(4), GOOD_LOG));

    assertEquals(expected, fixture.logAnalyzer(GOOD_LOG));
  }

  @ParameterizedTest
  @MethodSource("handleBadLogProvider")
  public void handleBadLog(MalformedLogEntry signal, Restart.Option restartOption, List<AnalyzedEntry> expected) throws Exception {
    fixture.setLogAnalyzer(true);
    fixture.setSignal(signal);
    fixture.setRestartOptionToUse(restartOption);

    assertEquals(expected, fixture.logAnalyzer(BAD_LOG));

    // handle in analyzeLog instead of logAnalyzer...
    fixture.setLogAnalyzer(false);
    fixture.setAnalyzeLog(true);

    // ...still works
    assertEquals(expected, fixture.logAnalyzer(BAD_LOG));
  }

  @Test
  public void readBadLogWithNoHandlingAtAll() throws Exception {
    try {
      fixture.logAnalyzer(BAD_LOG);
      fail();
    } catch (HandlerNotFoundException e) {
      assertEquals(
        new MalformedLogEntry(badLine(2).getText()),
        e.getSignal());
    }
  }

  @Test
  public void readBadLogWithNoHandlerFound() throws Exception {
    fixture.setLogAnalyzer(true);
    fixture.setSignal("oops"); // won't match MalformedLogEntry
    fixture.setRestartOptionToUse(new UseValue(USE_VALUE_ENTRY)); // should never be called

    try {
      fixture.logAnalyzer(BAD_LOG);
      fail();
    } catch (HandlerNotFoundException e) {
      assertEquals("oops", e.getSignal());
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
      assertEquals(
        UNKNOWN_RESTART_OPTION,
        e.getRestartOption());
    }
  }

  // helpers
  static Entry goodLine(int line) {
    return new Entry(String.format("%04d OK", line));
  }

  static Entry badLine(int line) {
    return new Entry(String.format("%04d FAIL", line));
  }

  static Stream<Arguments> handleBadLogProvider() {
    return Stream.of(
      arguments(
        null,
        new UseValue(USE_VALUE_ENTRY),
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG),
          new AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG))),
      arguments(
        null,
        new RetryWith(FIXED_TEXT),
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(FIXED_ENTRY, BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG),
          new AnalyzedEntry(FIXED_ENTRY, BAD_LOG))),
      arguments(
        null,
        new SkipEntry(),
        Arrays.asList(
          new AnalyzedEntry(goodLine(1), BAD_LOG),
          new AnalyzedEntry(goodLine(3), BAD_LOG)))
    );
  }

  static class UnknownRestartOption implements Restart.Option {
    private Object value;

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
