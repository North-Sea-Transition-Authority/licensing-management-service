package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

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
@Entity(name = "licence_schedule_extension_request")
public class LicenceScheduleExtensionRequest {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetails;

  @Embedded
  @AttributeOverride(name = "days", column = @Column(name = "extension_duration_days"))
  @AttributeOverride(name = "months", column = @Column(name = "extension_duration_months"))
  @AttributeOverride(name = "years", column = @Column(name = "extension_duration_years"))
  private ThreeFieldDuration extensionDuration;

  private String explanation;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public ThreeFieldDuration getExtensionDuration() {
    return extensionDuration;
  }

  public void setExtensionDuration(ThreeFieldDuration extensionDuration) {
    this.extensionDuration = extensionDuration;
  }

  public String getExplanation() {
    return explanation;
  }

  public void setExplanation(String comments) {
    this.explanation = comments;
  }

  public ScheduleWorkProgrammeApplicationDetail getScheduleWorkProgrammeApplicationDetails() {
    return scheduleWorkProgrammeApplicationDetails;
  }

  public void setScheduleWorkProgrammeApplicationDetails(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    this.scheduleWorkProgrammeApplicationDetails = scheduleWorkProgrammeApplicationDetail;
  }
}