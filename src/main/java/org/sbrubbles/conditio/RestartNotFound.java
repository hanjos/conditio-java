package org.sbrubbles.conditio;

/**
 * A condition signalled when no working restart for a given restart option was found.
 * <p>
 * The root scope has a default handler for this condition, which throws {@link RestartNotFoundException}. But
 * signalling a condition enables programmers to adopt different strategies.
 *
 * @see Restart
 * @see Restart.Option
 */
public class RestartNotFound implements Condition {
  private final Restart.Option option;

  /**
   * Creates a new instance.
   *
   * @param option the option that could not be handled.
   */
  public RestartNotFound(Restart.Option option) {
    this.option = option;
  }

  /**
   * The option that could not be handled.
   *
   * @return the option that could not be handled.
   */
  public Restart.Option getOption() {
    return option;
  }
}
