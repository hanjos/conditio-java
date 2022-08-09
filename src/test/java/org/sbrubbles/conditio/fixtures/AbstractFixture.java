package org.sbrubbles.conditio.fixtures;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Handler;
import org.sbrubbles.conditio.Restart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class AbstractFixture {
  private final List<String> handlerTrace;
  private final List<String> restartTrace;

  public AbstractFixture() {
    handlerTrace = new ArrayList<>();
    restartTrace = new ArrayList<>();
  }

  public <C extends Condition> BiFunction<C, Handler.Operations, Handler.Decision> traceHandler(
    final String prefix, final BiFunction<C, Handler.Operations, Handler.Decision> body) {
    return (c, ops) -> {
      handlerTrace.add(prefix + ": " + c.getClass().getSimpleName());

      return body.apply(c, ops);
    };
  }

  public <O extends Restart.Option, T> Function<O, T> traceRestart(final String prefix, final Function<O, T> body) {
    return o -> {
      restartTrace.add(prefix + ": " + o.getClass().getSimpleName());

      return body.apply(o);
    };
  }

  public List<String> getHandlerTrace() {
    return Collections.unmodifiableList(handlerTrace);
  }

  public List<String> getRestartTrace() {
    return Collections.unmodifiableList(restartTrace);
  }
}
