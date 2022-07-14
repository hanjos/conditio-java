package org.sbrubbles.conditio.fixtures;

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
import java.util.Objects;
import java.util.stream.Collectors;

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
          .on(UseValue.class, r -> { traceRestart("UseValue"); return r.getValue(); })
          .on(RetryWith.class, r -> { traceRestart("RetryWith"); return parseLogEntry(r.getText()); });

        Object signal = getSignal() != null ? getSignal() : new MalformedLogEntry(text);
        return (Entry) scope.signal(signal);
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
      if (isAnalyzeLog()) {
        scope.handle(MalformedLogEntry.class, condition -> {
          traceHandler("analyzeLog");
          return getRestartOptionToUse();
        });
      }

      scope.handle(OneOffSignal.class, condition -> {
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

      scope.handle(OneOffSignal.class, condition -> {
        traceHandler("logAnalyzer");
        return new UseValue(((OneOffSignal) condition.getSignal()).getValue());
      });

      List<AnalyzedEntry> logs = new ArrayList<>();
      for (String filename : logfiles) {
        logs.addAll(analyzeLog(filename));
      }

      return logs;
    }
  }

  // data for test purposes
  private boolean analyzeLog;
  private boolean logAnalyzer;

  private Object signal;
  private Restart.Option restartOptionToUse;

  private final List<String> handlerTrace;
  private final List<String> restartTrace;

  public LoggingFixture() {
    analyzeLog = false;
    logAnalyzer = false;

    signal = null;
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

  public Object getSignal() {
    return signal;
  }

  public void setSignal(Object signal) {
    this.signal = signal;
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

  public static class MalformedLogEntry {
    String text;

    public MalformedLogEntry(String text) {
      this.text = text;
    }

    public String getText() {
      return text;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      MalformedLogEntry that = (MalformedLogEntry) o;
      return Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
      return Objects.hash(text);
    }

    @Override
    public String toString() {
      return "MalformedLogEntry(" + text + ")";
    }
  }

  public static class Entry {
    String text;

    public Entry(String text) {
      this.text = text;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Entry entry = (Entry) o;
      return Objects.equals(text, entry.text);
    }

    public String getText() {
      return text;
    }

    @Override
    public int hashCode() {
      return Objects.hash(text);
    }

    @Override
    public String toString() {
      return "ENTRY: " + text;
    }
  }

  public static class AnalyzedEntry {
    Entry entry;
    String filename;

    public AnalyzedEntry(Entry entry, String filename) {
      this.entry = entry;
      this.filename = filename;
    }

    public Entry getEntry() {
      return entry;
    }

    public String getFilename() {
      return filename;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      AnalyzedEntry that = (AnalyzedEntry) o;
      return Objects.equals(entry, that.entry) && Objects.equals(filename, that.filename);
    }

    @Override
    public int hashCode() {
      return Objects.hash(entry, filename);
    }

    @Override
    public String toString() {
      return "ANALYZED(" + filename + "): " + entry;
    }
  }

  public static class OneOffSignal {
    private final Entry value;

    public OneOffSignal(Entry value) {
      this.value = value;
    }

    public Entry getValue() {
      return value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      OneOffSignal that = (OneOffSignal) o;
      return Objects.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getValue());
    }

    @Override
    public String toString() {
      return "OneOffSignal(" + value + ")";
    }
  }
}