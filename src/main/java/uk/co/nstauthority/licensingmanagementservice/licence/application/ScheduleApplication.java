package uk.co.nstauthority.licensingmanagementservice.licence.application;

import jakarta.persistence.CascadeType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Audited
@MappedSuperclass
public abstract class ScheduleApplication extends Application {

  @OneToOne(mappedBy = "application", cascade = CascadeType.ALL)
  private ApplicationSchedule schedule;

  public LicenceSchedule getLicenceSchedule() {
    return schedule.getLicenceSchedule();
  }

  /** Also sets the application's licence, which must always be the schedule's licence. */
  public void setLicenceSchedule(LicenceSchedule licenceSchedule) {
    getOrCreateSchedule().setLicenceSchedule(licenceSchedule);
    setLicence(licenceSchedule.getLicence());
  }

  public LicenceScheduleDetail getSubmittedLicenceScheduleDetail() {
    return schedule.getSubmittedLicenceScheduleDetail();
  }

  public void setSubmittedLicenceScheduleDetail(LicenceScheduleDetail submittedLicenceScheduleDetail) {
    getOrCreateSchedule().setSubmittedLicenceScheduleDetail(submittedLicenceScheduleDetail);
  }

  private ApplicationSchedule getOrCreateSchedule() {
    if (schedule == null) {
      schedule = new ApplicationSchedule(this);
    }
    return schedule;
  }
}
