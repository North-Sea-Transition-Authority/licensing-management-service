package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@Audited
@Entity(name = "licence_continuation_wpa_requirement_request")
public class LicenceContinuationWpaRequirementRequest {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "licence_continuation_application_detail_id")
  private LicenceContinuationApplicationDetail licenceContinuationApplicationDetail;

  private Boolean workProgrammeActivitiesCompletionStatus;

  private String actionsToCompleteWorkProgrammeActivities;

  private String furtherInformation;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

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

  public LicenceContinuationApplicationDetail getLicenceContinuationApplicationDetail() {
    return licenceContinuationApplicationDetail;
  }

  public void setLicenceContinuationApplicationDetail(LicenceContinuationApplicationDetail licenceContinuationApplicationDetail) {
    this.licenceContinuationApplicationDetail = licenceContinuationApplicationDetail;
  }
}
