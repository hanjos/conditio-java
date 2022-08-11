package org.sbrubbles.conditio.policies;

import org.sbrubbles.conditio.Condition;
import org.sbrubbles.conditio.HandlerNotFoundException;
import org.sbrubbles.conditio.Restart;
import org.sbrubbles.conditio.Scope;

/**
 * What to do if no handler is found for a given {@linkplain Scope#signal(Condition, HandlerNotFoundPolicy, Restart[]) signalling}.
 * The class {@link org.sbrubbles.conditio.policies.Policies Policies} offers some general use implementations.
 *
 * @param <T> the type to be returned by {@code signal}.
 * @see Scope#signal(Condition, HandlerNotFoundPolicy, Restart[])
 */
@FunctionalInterface
public interface HandlerNotFoundPolicy<T> {
  /**
   * Called when no handler is found.
   * <p>
   * Implementations which error out are expected to do so with a {@link HandlerNotFoundException}.
   *
   * @param scope where the handler search started.
   * @return the value to be returned by {@code signal}.
   * @throws HandlerNotFoundException may be thrown if no handler is found.
   */
  T onHandlerNotFound(Condition condition, Scope scope) throws HandlerNotFoundException;
}
