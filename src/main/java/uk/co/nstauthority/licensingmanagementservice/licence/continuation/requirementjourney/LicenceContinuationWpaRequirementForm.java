package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

public class LicenceContinuationWpaRequirementForm {
  private Boolean workProgrammeActivitiesCompletionStatus;
  private String actionsToCompleteWorkProgrammeActivities;
  private String furtherInformation;

  public Boolean getWorkProgrammeActivitiesCompletionStatus() {
    return workProgrammeActivitiesCompletionStatus;
  }

  public void setWorkProgrammeActivitiesCompletionStatus(Boolean workProgrammeActivitiesCompletionStatus) {
    this.workProgrammeActivitiesCompletionStatus = workProgrammeActivitiesCompletionStatus;
  }

  public String getActionsToCompleteWorkProgrammeActivities() {
    return actionsToCompleteWorkProgrammeActivities;
  }

  public void setActionsToCompleteWorkProgrammeActivities(String actionsToCompleteWorkProgrammeActivities) {
    this.actionsToCompleteWorkProgrammeActivities = actionsToCompleteWorkProgrammeActivities;
  }

  public String getFurtherInformation() {
    return furtherInformation;
  }

  public void setFurtherInformation(String furtherInformation) {
    this.furtherInformation = furtherInformation;
  }
}
