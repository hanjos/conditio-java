package org.sbrubbles.conditio.conditions;

import org.sbrubbles.conditio.Scope;

import java.io.PrintStream;

/**
 * A {@linkplain Signal signal} that prints a warning message if no handler is found.
 *
 * @see org.sbrubbles.conditio.restarts.Resume
 */
public interface Warning extends Signal {
  /**
   * The message to print.
   *
   * @return the message to print.
   */
  String getMessage();

  /**
   * Where to print.
   *
   * @return where to print.
   */
  PrintStream getOutput();

  /**
   * Prints a message to the given print stream.
   */
  @Override
  default Void onHandlerNotFound(Scope scope) {
    getOutput().println("[WARN] " + getMessage());

    return Signal.super.onHandlerNotFound(scope);
  }
}
