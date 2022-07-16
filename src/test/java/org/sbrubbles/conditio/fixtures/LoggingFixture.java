package org.sbrubbles.conditio.fixtures;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Handler;
import org.sbrubbles.conditio.Restart;
import org.sbrubbles.conditio.Scope;
import org.sbrubbles.conditio.restarts.RetryWith;
import org.sbrubbles.conditio.restarts.UseValue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * A baseline test bed, inspired by Practical Common Lisp's
 * <a href="https://gigamonkeys.com/book/beyond-exception-handling-conditions-and-restarts.html">logging example</a>.
 * <p>
 * It's a pretty decent usage example, if somewhat distorted by the instrumentation and shunts added for the tests.
 */
public class LoggingFixture {
  private static final Entry SKIP_ENTRY = new Entry(null);

  public boolean isWellFormed(String entry) {
    return entry != null && !entry.contains("FAIL");
  }

  public Entry parseLogEntry(String text) {
    try (Scope scope = Scope.create()) {
      if (isWellFormed(text)) {
        return new Entry(text);
      } else {
        scope
          .on(UseValue.class, r -> {
            traceRestart("UseValue");
            return r.getValue();
          })
          .on(RetryWith.class, r -> {
            traceRestart("RetryWith");
            return parseLogEntry(r.getText());
          });

        // here would be where you create a condition and signal it
        Condition c = getConditionProvider().apply(scope, text);
        return (Entry) scope.signal(c);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public List<Entry> parseLogFile(InputStream in) {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(in));
         Scope scope = Scope.create()) {
      List<String> lines = br.lines().collect(Collectors.toList());
      List<Entry> entries = new ArrayList<>();

      scope.on(SkipEntry.class, r -> { traceRestart("SkipEntry"); return SKIP_ENTRY; });

      for (String line : lines) {
        Entry entry = parseLogEntry(line);

        if (!SKIP_ENTRY.equals(entry)) {
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
    try (Scope scope = Scope.create()) {
      // this flag's here to test handling in different scopes, but it's actually a pretty good demonstration: since
      // this isn't a language construct, handlers can be established dynamically
      if (isAnalyzeLog()) {
        scope.handle(MalformedLogEntry.class, condition -> {
          traceHandler("analyzeLog");
          return getRestartOptionToUse();
        });
      }

      scope.handle(OneOff.class, condition -> {
        traceHandler("analyzeLog");
        return Handler.SKIP;
      });

      InputStream in = LoggingFixture.class.getResourceAsStream(filename);
      List<Entry> entries = parseLogFile(in);

      List<AnalyzedEntry> analyzed = new ArrayList<>();
      for (Entry entry : entries) {
        analyzed.add(new AnalyzedEntry(entry, filename));
      }

      return analyzed;
    }
  }

  public List<AnalyzedEntry> logAnalyzer(String... logfiles) throws Exception {
    try (Scope scope = Scope.create()) {
      if (isLogAnalyzer()) {
        scope.handle(MalformedLogEntry.class, condition -> {
          traceHandler("logAnalyzer");
          return getRestartOptionToUse();
        });
      }

      scope.handle(OneOff.class, condition -> {
        traceHandler("logAnalyzer");
        return new UseValue(condition.getValue());
      });

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

  private BiFunction<Scope, String, Condition> conditionProvider;
  private Restart.Option restartOptionToUse;

  private final List<String> handlerTrace;
  private final List<String> restartTrace;

  public LoggingFixture() {
    analyzeLog = false;
    logAnalyzer = false;

    conditionProvider = MalformedLogEntry::new;
    restartOptionToUse = null;

    handlerTrace = new ArrayList<>();
    restartTrace = new ArrayList<>();
  }

  private void traceHandler(String trace) {
    handlerTrace.add(trace);
  }

  private void traceRestart(String trace) {
    restartTrace.add(trace);
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

  public BiFunction<Scope, String, Condition> getConditionProvider() {
    return conditionProvider;
  }

  public void setConditionProvider(BiFunction<Scope, String, Condition> provider) {
    this.conditionProvider = provider;
  }

  public Restart.Option getRestartOptionToUse() {
    return restartOptionToUse;
  }

  public void setRestartOptionToUse(Restart.Option restartOptionToUse) {
    this.restartOptionToUse = restartOptionToUse;
  }

  public List<String> getHandlerTrace() {
    return Collections.unmodifiableList(handlerTrace);
  }

  public List<String> getRestartTrace() {
    return Collections.unmodifiableList(restartTrace);
  }

}