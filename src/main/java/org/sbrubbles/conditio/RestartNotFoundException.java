package org.sbrubbles.conditio;

/**
 * Thrown when no working restart for a given restart option was found.
 *
 * @see Restart
 * @see Restart.Option
 */
public class RestartNotFoundException extends RuntimeException {
  private final Restart.Option restartOption;

  /**
   * Creates a new instance.
   *
   * @param restartOption the option that could not be handled.
   */
  public RestartNotFoundException(Restart.Option restartOption) {
    super("No restart found for option " + restartOption);

    this.restartOption = restartOption;
  }

  /**
   * The option that could not be handled.
   *
   * @return the option that could not be handled.
   */
  public Restart.Option getRestartOption() {
    return restartOption;
  }
}
