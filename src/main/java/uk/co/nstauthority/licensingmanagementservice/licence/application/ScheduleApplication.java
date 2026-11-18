package uk.co.nstauthority.licensingmanagementservice.licence.application;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Audited
@MappedSuperclass
public abstract class ScheduleApplication extends Application {

  @ManyToOne
  @JoinColumn(name = "licence_schedule_id")
  private LicenceSchedule licenceSchedule;

  @ManyToOne
  @JoinColumn(name = "submitted_licence_schedule_detail_id")
  private LicenceScheduleDetail submittedLicenceScheduleDetail;

  @Override
  public Licence getLicence() {
    return getLicenceSchedule().getLicence();
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
}
