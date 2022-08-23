package org.sbrubbles.conditio;

import org.sbrubbles.conditio.policies.Policies;

/**
 * Thrown when no working handler for a given condition was found. This may happen either when no available handler
 * could {@linkplain Handler#test(Object) handle} the condition, or when the ones that could opted to
 * {@linkplain Handler.Context#skip() skip} instead.
 *
 * @see Handler
 * @see Handler.Context
 */
public class HandlerNotFoundException extends RuntimeException {
  private final Handler.Context<?> context;

  /**
   * Creates a new instance.
   *
   * @param context the {@link Scope#signal(Condition, Policies, Restart[]) signal} context that found no handler.
   */
  public HandlerNotFoundException(Handler.Context<?> context) {
    super("No handler found for " + context);

    this.context = context;
  }

  /**
   * The context that could not be handled.
   *
   * @return the context that could not be handled.
   */
  public Handler.Context<?> getContext() {
    return context;
  }
}
