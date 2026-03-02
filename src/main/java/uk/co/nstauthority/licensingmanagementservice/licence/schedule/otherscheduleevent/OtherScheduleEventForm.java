package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;

public class OtherScheduleEventForm {

  private OtherScheduleEventCategory otherScheduleEventCategory;

  private String otherCategoryName;

  private String description;

  private OtherScheduleEventDateOption otherScheduleEventDateOption;

  private String licenceScheduleTermId;

  private String licenceSchedulePhaseId;

  private ThreeFieldDurationInput relativeDuration = new ThreeFieldDurationInput("relativeDuration", "relative duration");

  private String relativeEventId;

  private String comments;

  public OtherScheduleEventCategory getOtherScheduleEventCategory() {
    return otherScheduleEventCategory;
  }

  public void setOtherScheduleEventCategory(OtherScheduleEventCategory otherScheduleEventCategory) {
    this.otherScheduleEventCategory = otherScheduleEventCategory;
  }

  public String getOtherCategoryName() {
    return otherCategoryName;
  }

  public void setOtherCategoryName(String otherCategoryName) {
    this.otherCategoryName = otherCategoryName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public OtherScheduleEventDateOption getOtherScheduleEventDateOption() {
    return otherScheduleEventDateOption;
  }

  public void setOtherScheduleEventDateOption(OtherScheduleEventDateOption otherScheduleEventDateOption) {
    this.otherScheduleEventDateOption = otherScheduleEventDateOption;
  }

  public String getLicenceScheduleTermId() {
    return licenceScheduleTermId;
  }

  public void setLicenceScheduleTermId(String licenceScheduleTermId) {
    this.licenceScheduleTermId = licenceScheduleTermId;
  }

  public String getLicenceSchedulePhaseId() {
    return licenceSchedulePhaseId;
  }

  public void setLicenceSchedulePhaseId(String licenceSchedulePhaseId) {
    this.licenceSchedulePhaseId = licenceSchedulePhaseId;
  }

  public ThreeFieldDurationInput getRelativeDuration() {
    return relativeDuration;
  }

  public void setRelativeDuration(ThreeFieldDurationInput relativeDuration) {
    this.relativeDuration = relativeDuration;
  }

  public String getRelativeEventId() {
    return relativeEventId;
  }

  public void setRelativeEventId(String relativeEventId) {
    this.relativeEventId = relativeEventId;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }
}
