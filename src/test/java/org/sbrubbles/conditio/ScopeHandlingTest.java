package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sbrubbles.conditio.fixtures.LoggingFixture;
import org.sbrubbles.conditio.fixtures.RetryWith;
import org.sbrubbles.conditio.fixtures.SkipEntry;
import org.sbrubbles.conditio.fixtures.UseValue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class ScopeHandlingTest {
  private static final String FIXED_TEXT = "0000 FIXED TEXT";
  private static final LoggingFixture.Entry FIXED_ENTRY = new LoggingFixture.Entry(FIXED_TEXT);
  private static final LoggingFixture.Entry USE_VALUE_ENTRY = new LoggingFixture.Entry("1000 USE VALUE ENTRY");

  private static final String GOOD_LOG = "../good.txt";
  private static final String BAD_LOG = "../bad.txt";

  private LoggingFixture fixture;

  @BeforeEach
  public void buildFixture() {
    fixture = new LoggingFixture();
  }

  static LoggingFixture.Entry goodLine(int line) {
    return new LoggingFixture.Entry(String.format("%04d OK", line));
  }

  static Stream<Arguments> badLogProvider() {
    return Stream.of(
      arguments(
        new UseValue(USE_VALUE_ENTRY),
        Arrays.asList(
          new LoggingFixture.AnalyzedEntry(goodLine(1), BAD_LOG),
          new LoggingFixture.AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG),
          new LoggingFixture.AnalyzedEntry(goodLine(3), BAD_LOG),
          new LoggingFixture.AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG))),
      arguments(
        new RetryWith(FIXED_TEXT),
        Arrays.asList(
          new LoggingFixture.AnalyzedEntry(goodLine(1), BAD_LOG),
          new LoggingFixture.AnalyzedEntry(FIXED_ENTRY, BAD_LOG),
          new LoggingFixture.AnalyzedEntry(goodLine(3), BAD_LOG),
          new LoggingFixture.AnalyzedEntry(FIXED_ENTRY, BAD_LOG))),
      arguments(
        new SkipEntry(),
        Arrays.asList(
          new LoggingFixture.AnalyzedEntry(goodLine(1), BAD_LOG),
          new LoggingFixture.AnalyzedEntry(goodLine(3), BAD_LOG)))
    );
  }

  @Test
  public void readGoodLog() throws Exception {
    // no signal should be emitted
    List<LoggingFixture.AnalyzedEntry> expected = Arrays.asList(
      new LoggingFixture.AnalyzedEntry(goodLine(1), GOOD_LOG),
      new LoggingFixture.AnalyzedEntry(goodLine(2), GOOD_LOG),
      new LoggingFixture.AnalyzedEntry(goodLine(3), GOOD_LOG),
      new LoggingFixture.AnalyzedEntry(goodLine(4), GOOD_LOG));

    assertEquals(expected, fixture.logAnalyzer(GOOD_LOG));
  }

  @Test
  public void readBadLog() {
    assertThrows(RuntimeException.class, () -> fixture.logAnalyzer(BAD_LOG));
  }

  @ParameterizedTest
  @MethodSource("badLogProvider")
  public void handleBadLog(Object restartOption, List<LoggingFixture.AnalyzedEntry> expected) throws Exception {
    fixture.setLogAnalyzer(true);
    fixture.setRestartOptionToUse(restartOption);

    assertEquals(expected, fixture.logAnalyzer(BAD_LOG));

    // handle in analyzeLog instead of logAnalyzer...
    fixture.setLogAnalyzer(false);
    fixture.setAnalyzeLog(true);

    // ...still works
    assertEquals(expected, fixture.logAnalyzer(BAD_LOG));
  }
}
