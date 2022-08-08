package org.sbrubbles.conditio.fixtures;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.Handler;
import org.sbrubbles.conditio.Restart;
import org.sbrubbles.conditio.util.TriFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class AbstractFixture {
  private final List<String> handlerTrace;
  private final List<String> restartTrace;

  public AbstractFixture() {
    handlerTrace = new ArrayList<>();
    restartTrace = new ArrayList<>();
  }

  public <R, C extends Condition> TriFunction<C, Class<R>, Handler.Operations<R>, Handler.Decision<R>> traceHandler(
    final String prefix, final TriFunction<C, Class<R>, Handler.Operations<R>, Handler.Decision<R>> body) {
    return (c, t, ops) -> {
      handlerTrace.add(prefix + ": " + c.getClass().getSimpleName());

      return body.apply(c, t, ops);
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
