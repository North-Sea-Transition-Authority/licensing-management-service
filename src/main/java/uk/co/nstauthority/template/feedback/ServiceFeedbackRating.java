package uk.co.nstauthority.template.feedback;

import uk.co.nstauthority.template.util.enumutil.Displayable;

public enum ServiceFeedbackRating implements Displayable {
  VERY_SATISFIED(10, "Very satisfied"),
  SATISFIED(20, "Satisfied"),
  NEITHER(30, "Neither satisfied or dissatisfied"),
  DISSATISFIED(40, "Dissatisfied"),
  VERY_DISSATISFIED(50, "Very dissatisfied");

  private final int displayOrder;
  private final String displayName;

  ServiceFeedbackRating(int displayOrder, String displayName) {
    this.displayOrder = displayOrder;
    this.displayName = displayName;
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }
}
