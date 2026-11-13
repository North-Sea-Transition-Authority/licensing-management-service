package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import uk.co.nstauthority.licensingmanagementservice.fds.tag.TagColour;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum PartialSurrenderSubareaOutcome implements Displayable {
  RELINQUISHED("Relinquished", 10, TagColour.RED),
  CROPPED("Cropped", 20, TagColour.BLUE),
  UNCHANGED("Unchanged", 30, TagColour.GREY);

  private final String displayName;
  private final int displayOrder;
  private final TagColour tagColour;

  PartialSurrenderSubareaOutcome(String displayName, int displayOrder, TagColour tagColour) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
    this.tagColour = tagColour;
  }

  public TagColour getTagColour() {
    return tagColour;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }
}
