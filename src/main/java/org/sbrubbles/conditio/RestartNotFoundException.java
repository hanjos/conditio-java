package org.sbrubbles.conditio;

public class RestartNotFoundException extends RuntimeException {
  private Object restartOption;

  public RestartNotFoundException(Object restartOption) {
    super("No restart found for option " + restartOption);

    this.restartOption = restartOption;
  }

  public Object getRestartOption() {
    return restartOption;
  }
}
