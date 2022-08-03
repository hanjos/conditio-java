package org.sbrubbles.conditio.conditions;

import org.sbrubbles.conditio.Scope;

import java.io.PrintStream;

/**
 * A {@linkplain Notice notice} that prints a warning message if no handler is found.
 */
public class Warning extends Notice {
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
