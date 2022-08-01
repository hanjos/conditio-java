package org.sbrubbles.conditio.conditions;

import org.sbrubbles.conditio.Scope;

import java.io.PrintStream;

/**
 * A {@linkplain Signal signal} that prints a warning message if no handler is found.
 *
 * @see org.sbrubbles.conditio.restarts.Resume
 */
public class Warning extends Signal {
  private final String message;
  private final PrintStream output;

  public Warning(String message) {
    this(message, System.out);
  }

  public Warning(String message, PrintStream output) {
    this.message = message;
    this.output = output;
  }

  /**
   * The message to print.
   *
   * @return the message to print.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Where to print.
   *
   * @return where to print.
   */
  public PrintStream getOutput() {
    return output;
  }

  /**
   * Prints a message to the given print stream.
   */
  @Override
  public Void onHandlerNotFound(Scope scope) {
    getOutput().println("[WARN] " + getMessage());

    return super.onHandlerNotFound(scope);
  }
}
