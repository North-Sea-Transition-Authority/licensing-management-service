package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import uk.co.fivium.formlibrary.input.ThreeFieldDateInput;

public class WorkProgrammeActivityForm {

  private WorkProgrammeActivityCategory workProgrammeActivityCategory;

  private String otherCategoryName;

  private String description;

  private WorkProgrammeActivityCommitment workProgrammeActivityCommitment;

  private WorkProgrammeActivityDateOption workProgrammeActivityDateOption;

  private String licenceScheduleTermId;

  private String licenceSchedulePhaseId;

  private ThreeFieldDateInput dueDateInput = new ThreeFieldDateInput("dueDateInput", "Due date");

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

  public ThreeFieldDateInput getDueDateInput() {
    return dueDateInput;
  }

  public void setDueDateInput(ThreeFieldDateInput dueDateInput) {
    this.dueDateInput = dueDateInput;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }
}
