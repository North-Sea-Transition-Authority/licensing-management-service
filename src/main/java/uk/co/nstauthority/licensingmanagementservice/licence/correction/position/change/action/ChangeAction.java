package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action;

import org.apache.commons.lang3.StringUtils;
import uk.co.fivium.digitalenummaterialisationlibrary.enummaterialisation.MaterialisableEnum;
import uk.co.nstauthority.licensingmanagementservice.fds.button.Button;

public enum ChangeAction implements MaterialisableEnum {

  REMOVE("remove", "removed", true),
  UNDO("undo", "undone", true),
  REINSTATE("reinstate", "reinstated", false);

  private final String verb;
  private final String pastParticiple;
  private final boolean destructive;

  ChangeAction(String verb, String pastParticiple, boolean destructive) {
    this.verb = verb;
    this.pastParticiple = pastParticiple;
    this.destructive = destructive;
  }

  public String pageTitle(String changeName) {
    return "Are you sure you want to %s this %s?".formatted(verb, changeName);
  }

  public Button primaryButton(String changeName) {
    var buttonText = "%s %s".formatted(StringUtils.capitalize(verb), changeName);
    return destructive ? Button.warningButton(buttonText) : Button.primaryButton(buttonText);
  }

  public String successBannerHeading(String changeName) {
    return "%s %s".formatted(StringUtils.capitalize(changeName), pastParticiple);
  }

  @Override
  public String getDisplayName() {
    return verb;
  }

  @Override
  public int getDisplayOrder() {
    return ordinal();
  }
}
