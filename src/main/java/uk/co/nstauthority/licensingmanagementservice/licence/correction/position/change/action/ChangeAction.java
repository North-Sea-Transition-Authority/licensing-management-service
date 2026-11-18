package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action;

import org.apache.commons.lang3.StringUtils;
import uk.co.nstauthority.licensingmanagementservice.fds.button.Button;

/**
 * What a correction can do to a single change on a position: remove a live change, undo a change it has staged, or
 * reinstate a live change it has staged the removal of.
 */
public enum ChangeAction {

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
}
