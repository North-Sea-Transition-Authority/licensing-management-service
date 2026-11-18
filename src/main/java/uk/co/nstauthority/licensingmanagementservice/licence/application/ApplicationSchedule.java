package uk.co.nstauthority.licensingmanagementservice.licence.application;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import java.util.UUID;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Audited
@Entity(name = "schedule_applications")
public class ApplicationSchedule {

  @Id
  private UUID id;

  @MapsId
  @OneToOne
  @JoinColumn(name = "id")
  private Application application;

  @ManyToOne
  @JoinColumn(name = "licence_schedule_id")
  private LicenceSchedule licenceSchedule;

  @ManyToOne
  @JoinColumn(name = "submitted_licence_schedule_detail_id")
  private LicenceScheduleDetail submittedLicenceScheduleDetail;

  protected ApplicationSchedule() {
  }

  ApplicationSchedule(Application application) {
    this.application = application;
  }

  public UUID getId() {
    return id;
  }

  public Application getApplication() {
    return application;
  }

  public LicenceSchedule getLicenceSchedule() {
    return licenceSchedule;
  }

  void setLicenceSchedule(LicenceSchedule licenceSchedule) {
    this.licenceSchedule = licenceSchedule;
  }

  public LicenceScheduleDetail getSubmittedLicenceScheduleDetail() {
    return submittedLicenceScheduleDetail;
  }

  void setSubmittedLicenceScheduleDetail(LicenceScheduleDetail submittedLicenceScheduleDetail) {
    this.submittedLicenceScheduleDetail = submittedLicenceScheduleDetail;
  }
}
