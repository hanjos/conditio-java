package org.sbrubbles.conditio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbrubbles.conditio.fixtures.LoggingFixture;
import org.sbrubbles.conditio.fixtures.RetryWith;
import org.sbrubbles.conditio.fixtures.SkipEntry;
import org.sbrubbles.conditio.fixtures.UseValue;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ScopeHandlingTest {
  private static final String FIXED_TEXT = "0000 FIXED TEXT";
  private static final LoggingFixture.Entry USE_VALUE_ENTRY = new LoggingFixture.Entry("1000 USE VALUE ENTRY");
  private static final String GOOD_LOG = "../good.txt";
  private static final String BAD_LOG = "../bad.txt";

  private LoggingFixture fixture;

  @BeforeEach
  public void buildFixture() {
    fixture = new LoggingFixture();
  }

  @Test
  public void handleGoodLog() throws Exception {
    // no signal should be emitted
    List<LoggingFixture.AnalyzedEntry> expected = Arrays.asList(
      new LoggingFixture.AnalyzedEntry(goodLine(1), GOOD_LOG),
      new LoggingFixture.AnalyzedEntry(goodLine(2), GOOD_LOG),
      new LoggingFixture.AnalyzedEntry(goodLine(3), GOOD_LOG),
      new LoggingFixture.AnalyzedEntry(goodLine(4), GOOD_LOG));

    assertEquals(expected, fixture.logAnalyzer(GOOD_LOG));
  }

  @Test
  public void handleBadLogWithUseValue() throws Exception {
    fixture.setLogAnalyzer(true);
    fixture.setRestartOptionToUse(new UseValue(USE_VALUE_ENTRY));

    List<LoggingFixture.AnalyzedEntry> expected = Arrays.asList(
      new LoggingFixture.AnalyzedEntry(goodLine(1), BAD_LOG),
      new LoggingFixture.AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG),
      new LoggingFixture.AnalyzedEntry(goodLine(3), BAD_LOG),
      new LoggingFixture.AnalyzedEntry(USE_VALUE_ENTRY, BAD_LOG));

    assertEquals(expected, fixture.logAnalyzer(BAD_LOG));

    // handle in analyzeLog instead of logAnalyzer...
    fixture.setLogAnalyzer(false);
    fixture.setAnalyzeLog(true);

    // ...still works
    assertEquals(expected, fixture.logAnalyzer(BAD_LOG));
  }

  @Test
  public void handleBadLogWithRetryWith() throws Exception {
    fixture.setLogAnalyzer(true);
    fixture.setRestartOptionToUse(new RetryWith(FIXED_TEXT));

    final LoggingFixture.Entry FIXED_ENTRY = new LoggingFixture.Entry(FIXED_TEXT);

    List<LoggingFixture.AnalyzedEntry> expected = Arrays.asList(
      new LoggingFixture.AnalyzedEntry(goodLine(1), BAD_LOG),
      new LoggingFixture.AnalyzedEntry(FIXED_ENTRY, BAD_LOG),
      new LoggingFixture.AnalyzedEntry(goodLine(3), BAD_LOG),
      new LoggingFixture.AnalyzedEntry(FIXED_ENTRY, BAD_LOG));

    assertEquals(expected, fixture.logAnalyzer(BAD_LOG));

    // handle in analyzeLog instead of logAnalyzer...
    fixture.setLogAnalyzer(false);
    fixture.setAnalyzeLog(true);

    // ...still works
    assertEquals(expected, fixture.logAnalyzer(BAD_LOG));
  }

  @Test
  public void handleBadLogWithSkipEntry() throws Exception {
    fixture.setLogAnalyzer(true);
    fixture.setRestartOptionToUse(new SkipEntry());

    List<LoggingFixture.AnalyzedEntry> expected = Arrays.asList(
      new LoggingFixture.AnalyzedEntry(goodLine(1), BAD_LOG),
      new LoggingFixture.AnalyzedEntry(goodLine(3), BAD_LOG));

    assertEquals(expected, fixture.logAnalyzer(BAD_LOG));

    // handle in analyzeLog instead of logAnalyzer...
    fixture.setLogAnalyzer(false);
    fixture.setAnalyzeLog(true);

    // ...still works
    assertEquals(expected, fixture.logAnalyzer(BAD_LOG));
  }

  public LoggingFixture.Entry goodLine(int line) {
    return new LoggingFixture.Entry(String.format("%04d OK", line));
  }
}
