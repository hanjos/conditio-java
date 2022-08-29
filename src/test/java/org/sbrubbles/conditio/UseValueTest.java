package org.sbrubbles.conditio;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sbrubbles.conditio.fixtures.BasicCondition;
import org.sbrubbles.conditio.handlers.Handlers;
import org.sbrubbles.conditio.policies.HandlerNotFoundPolicy;
import org.sbrubbles.conditio.policies.Policies;
import org.sbrubbles.conditio.policies.ReturnTypePolicy;
import org.sbrubbles.conditio.restarts.Restarts;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SuppressWarnings("unchecked")
public class UseValueTest {
  @ParameterizedTest
  @MethodSource("valuesProvider")
  public void useValue(String result, String message) {
    final List<String> trail = new ArrayList<>();

    try (Scope a = Scopes.create()) {
      a.handle(BasicCondition.class, trace(trail, message,
        Handlers.restart(Restarts.use(result))));

      try (Scope b = Scopes.create()) {
        Object actual = b.signal(new BasicCondition(""), new Policies<>(HandlerNotFoundPolicy.error(), ReturnTypePolicy.expects(Object.class)), Restarts.useValue());

        assertEquals(result, actual);
        assertEquals(1, trail.size());
        assertEquals(message, trail.get(0));
      }
    }
  }

  static <C extends Condition> BiFunction<Signal<C>, Handler.Operations, Handler.Decision> trace(List<String> trail, String message, BiFunction<Signal<C>, Handler.Operations, Handler.Decision> body) {
    return (s, ops) -> {
      trail.add(message);

      return body.apply(s, ops);
    };
  }

  static Stream<Arguments> valuesProvider() {
    return Stream.of(
      arguments("<result>", "a"),
      arguments("<result>", ""),
      arguments("<result>", null)
    );
  }
}
