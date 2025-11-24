package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Audited
@Entity(name = "licence_schedule_extension_request")
public class LicenceScheduleExtensionRequest {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetails;

  @ManyToOne
  @JoinColumn(name = "phase_id")
  private LicenceSchedulePhase licenceSchedulePhase;

  @ManyToOne
  @JoinColumn(name = "term_id")
  private LicenceScheduleTerm licenceScheduleTerm;

  @Embedded
  @AttributeOverride(name = "days", column = @Column(name = "extension_duration_days"))
  @AttributeOverride(name = "months", column = @Column(name = "extension_duration_months"))
  @AttributeOverride(name = "years", column = @Column(name = "extension_duration_years"))
  private ThreeFieldDuration extensionDuration;

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

  public ScheduleWorkProgrammeApplicationDetail getScheduleWorkProgrammeApplicationDetails() {
    return scheduleWorkProgrammeApplicationDetails;
  }

  public void setScheduleWorkProgrammeApplicationDetails(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    this.scheduleWorkProgrammeApplicationDetails = scheduleWorkProgrammeApplicationDetail;
  }

  public LicenceSchedulePhase getLicenceSchedulePhase() {
    return licenceSchedulePhase;
  }

  public void setLicenceSchedulePhase(LicenceSchedulePhase licenceSchedulePhase) {
    this.licenceSchedulePhase = licenceSchedulePhase;
  }

  public LicenceScheduleTerm getLicenceScheduleTerm() {
    return licenceScheduleTerm;
  }

  public void setLicenceScheduleTerm(LicenceScheduleTerm licenceScheduleTerm) {
    this.licenceScheduleTerm = licenceScheduleTerm;
  }
}