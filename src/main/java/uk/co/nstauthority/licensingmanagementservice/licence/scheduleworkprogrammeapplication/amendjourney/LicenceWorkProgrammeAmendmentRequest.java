package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Audited
@Entity(name = "licence_work_programme_amendment_request")
public class LicenceWorkProgrammeAmendmentRequest {

  @Id
  @UuidGenerator
  private UUID id;

  private UUID workProgrammeActivityId;

  @ManyToOne
  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetails;

  @Embedded
  @AttributeOverride(name = "days", column = @Column(name = "work_programme_extension_duration_days"))
  @AttributeOverride(name = "months", column = @Column(name = "work_programme_extension_duration_months"))
  @AttributeOverride(name = "years", column = @Column(name = "work_programme_extension_duration_years"))
  private ThreeFieldDuration workProgrammeExtensionDuration;

  private String workProgrammeAmendmentInformation;

  private boolean durationExtensionRequired;
  private boolean additionalInfoRequired;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public ThreeFieldDuration getWorkProgrammeExtensionDuration() {
    return workProgrammeExtensionDuration;
  }

  public void setWorkProgrammeExtensionDuration(ThreeFieldDuration extensionDuration) {
    this.workProgrammeExtensionDuration = extensionDuration;
  }

  public String getWorkProgrammeAmendmentInformation() {
    return workProgrammeAmendmentInformation;
  }

  public void setWorkProgrammeAmendmentInformation(String comments) {
    this.workProgrammeAmendmentInformation = comments;
  }

  public ScheduleWorkProgrammeApplicationDetail getScheduleWorkProgrammeApplicationDetails() {
    return scheduleWorkProgrammeApplicationDetails;
  }

  public void setScheduleWorkProgrammeApplicationDetails(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetails) {
    this.scheduleWorkProgrammeApplicationDetails = scheduleWorkProgrammeApplicationDetails;
  }

  public UUID getWorkProgrammeActivityId() {
    return workProgrammeActivityId;
  }

  public void setWorkProgrammeActivityId(UUID workProgrammeActivityId) {
    this.workProgrammeActivityId = workProgrammeActivityId;
  }

  public boolean isDurationExtensionRequired() {
    return durationExtensionRequired;
  }

  public void setDurationExtensionRequired(boolean durationExtensionRequired) {
    this.durationExtensionRequired = durationExtensionRequired;
  }

  public boolean isAdditionalInfoRequired() {
    return additionalInfoRequired;
  }

  public void setAdditionalInfoRequired(boolean additionalInfoRequired) {
    this.additionalInfoRequired = additionalInfoRequired;
  }
}