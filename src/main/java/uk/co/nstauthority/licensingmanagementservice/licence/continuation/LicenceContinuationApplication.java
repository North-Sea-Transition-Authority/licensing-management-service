package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Audited
@Entity(name = "licence_continuation_applications")
public class LicenceContinuationApplication implements LicenceApplication {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "licence_schedule_id")
  private LicenceSchedule licenceSchedule;

  @ManyToOne
  @JoinColumn(name = "submitted_licence_schedule_detail_id")
  private LicenceScheduleDetail submittedLicenceScheduleDetail;

  @Column
  private String applicationReference;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public LicenceSchedule getLicenceSchedule() {
    return licenceSchedule;
  }

  public void setLicenceSchedule(LicenceSchedule licenceSchedule) {
    this.licenceSchedule = licenceSchedule;
  }

  public LicenceScheduleDetail getSubmittedLicenceScheduleDetail() {
    return submittedLicenceScheduleDetail;
  }

  public void setSubmittedLicenceScheduleDetail(LicenceScheduleDetail submittedLicenceScheduleDetail) {
    this.submittedLicenceScheduleDetail = submittedLicenceScheduleDetail;
  }

  public String getApplicationReference() {
    return applicationReference;
  }

  public void setApplicationReference(String applicationReference) {
    this.applicationReference = applicationReference;
  }

  @Override
  public ApplicationType getApplicationType() {
    return ApplicationType.CONTINUATION_APPLICATION;
  }
}
