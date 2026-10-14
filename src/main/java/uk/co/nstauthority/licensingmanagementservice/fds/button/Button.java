package uk.co.nstauthority.licensingmanagementservice.fds.button;

public record Button(String buttonText, String buttonClass) {

  public static Button primaryButton(String tagText) {
    return new Button(tagText, "govuk-button");
  }

  public static Button warningButton(String tagText) {
    return new Button(tagText, "govuk-button govuk-button--warning");
  }

}
