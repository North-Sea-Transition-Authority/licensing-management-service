package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;

public class WorkProgrammeActivityForm {

  private WorkProgrammeActivityCategory workProgrammeActivityCategory;

  private String otherCategoryName;

  private String description;

  private WorkProgrammeActivityCommitment workProgrammeActivityCommitment;

  private WorkProgrammeActivityDateOption workProgrammeActivityDateOption;

  private String licenceScheduleTermId;

  private String licenceSchedulePhaseId;

  private ThreeFieldDurationInput relativeDuration = new ThreeFieldDurationInput("relativeDuration", "relative duration");

  private String relativeEventId;

  private String comments;

  public WorkProgrammeActivityCategory getWorkProgrammeActivityCategory() {
    return workProgrammeActivityCategory;
  }

  public void setWorkProgrammeActivityCategory(WorkProgrammeActivityCategory workProgrammeActivityCategory) {
    this.workProgrammeActivityCategory = workProgrammeActivityCategory;
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

  public WorkProgrammeActivityCommitment getWorkProgrammeActivityCommitment() {
    return workProgrammeActivityCommitment;
  }

  public void setWorkProgrammeActivityCommitment(WorkProgrammeActivityCommitment workProgrammeActivityCommitment) {
    this.workProgrammeActivityCommitment = workProgrammeActivityCommitment;
  }

  public WorkProgrammeActivityDateOption getWorkProgrammeActivityDateOption() {
    return workProgrammeActivityDateOption;
  }

  public void setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption workProgrammeActivityDateOption) {
    this.workProgrammeActivityDateOption = workProgrammeActivityDateOption;
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
