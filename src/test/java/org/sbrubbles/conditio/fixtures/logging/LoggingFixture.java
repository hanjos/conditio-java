package org.sbrubbles.conditio.fixtures.logging;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Restart;
import org.sbrubbles.conditio.Scope;
import org.sbrubbles.conditio.Scopes;
import org.sbrubbles.conditio.fixtures.AbstractFixture;
import org.sbrubbles.conditio.handlers.Handlers;
import org.sbrubbles.conditio.policies.HandlerNotFoundPolicy;
import org.sbrubbles.conditio.policies.Policies;
import org.sbrubbles.conditio.policies.ReturnTypePolicy;
import org.sbrubbles.conditio.restarts.UseValue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A baseline test bed, inspired by Practical Common Lisp's
 * <a href="https://gigamonkeys.com/book/beyond-exception-handling-conditions-and-restarts.html">logging example</a>.
 * <p>
 * It's a pretty decent usage example, if somewhat distorted by the instrumentation and shunts added for the tests.
 */
@SuppressWarnings("unchecked")
public class LoggingFixture extends AbstractFixture {
  private static final Entry SKIP_ENTRY_MARKER = new Entry(null);

  public boolean isWellFormed(String entry) {
    return entry != null && !entry.contains("FAIL");
  }

  public Entry parseLogEntry(String text) {
    try (Scope scope = Scopes.create()) {
      if (isWellFormed(text)) {
        return new Entry(text);
      } else {
        // useful for tracing
        final Restart<Entry> USE_VALUE = Restart.on(UseValue.class,
          traceRestart("UseValue", UseValue<Entry>::getValue));
        final Restart<Entry> RETRY_WITH = Restart.on(RetryWith.class,
          traceRestart("RetryWith", r -> parseLogEntry(r.getText())));

        return scope.signal(
          getConditionProvider().apply(text),
          new Policies<>(HandlerNotFoundPolicy.error(), ReturnTypePolicy.expects(Entry.class)),
          USE_VALUE,
          RETRY_WITH);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public List<Entry> parseLogFile(InputStream in) {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(in));
         Scope scope = Scopes.create()) {
      List<String> lines = br.lines().collect(Collectors.toList());
      List<Entry> entries = new ArrayList<>();

      // skips malformed entries by returning a predefined "Entry", which can be checked and skipped
      final Restart<?> SKIP_ENTRY = Restart.on(SkipEntry.class,
        traceRestart("SkipEntry", r -> SKIP_ENTRY_MARKER));

      for (String line : lines) {
        // provides an extra restart for the handlers
        Entry entry = scope.call(() -> parseLogEntry(line), SKIP_ENTRY);

        // if entry is not SKIP_ENTRY_MARKER, then it actually came from parseLogEntry; add it
        if (!SKIP_ENTRY_MARKER.equals(entry)) {
          entries.add(entry);
        }
      }

      return entries;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public List<AnalyzedEntry> analyzeLog(String filename) {
    try (Scope scope = Scopes.create()) {
      // this flag's here to test handling in different scopes, but it's actually a pretty good demonstration: since
      // this isn't a language construct, handlers can be established dynamically
      if (isAnalyzeLog()) {
        scope.handle(MalformedLogEntry.class,
          traceHandler("analyzeLog", Handlers.restart(getRestartOptionToUse())));
      }

      InputStream in = LoggingFixture.class.getResourceAsStream(filename);
      List<Entry> entries = parseLogFile(in);

      List<AnalyzedEntry> analyzed = new ArrayList<>();
      for (Entry entry : entries) {
        analyzed.add(new AnalyzedEntry(entry, filename));
      }

      return analyzed;
    }
  }

  public List<AnalyzedEntry> logAnalyzer(String... logfiles) {
    try (Scope scope = Scopes.create()) {
      if (isLogAnalyzer()) {
        scope.handle(MalformedLogEntry.class,
          traceHandler("logAnalyzer", Handlers.restart(getRestartOptionToUse())));
      }

      List<AnalyzedEntry> logs = new ArrayList<>();
      for (String filename : logfiles) {
        logs.addAll(analyzeLog(filename));
      }

      return logs;
    }
  }

  // test machinery
  private boolean analyzeLog;
  private boolean logAnalyzer;

  private Function<String, Condition> conditionProvider;
  private Restart.Option restartOptionToUse;

  public LoggingFixture() {
    analyzeLog = false;
    logAnalyzer = false;

    conditionProvider = MalformedLogEntry::new;
    restartOptionToUse = null;
  }

  public boolean isAnalyzeLog() {
    return analyzeLog;
  }

  public void setAnalyzeLog(boolean analyzeLog) {
    this.analyzeLog = analyzeLog;
  }

  public boolean isLogAnalyzer() {
    return logAnalyzer;
  }

  public void setLogAnalyzer(boolean logAnalyzer) {
    this.logAnalyzer = logAnalyzer;
  }

  public Function<String, Condition> getConditionProvider() {
    return conditionProvider;
  }

  public void setConditionProvider(Function<String, Condition> provider) {
    this.conditionProvider = provider;
  }

  public Restart.Option getRestartOptionToUse() {
    return restartOptionToUse;
  }

  public void setRestartOptionToUse(Restart.Option restartOptionToUse) {
    this.restartOptionToUse = restartOptionToUse;
  }
}