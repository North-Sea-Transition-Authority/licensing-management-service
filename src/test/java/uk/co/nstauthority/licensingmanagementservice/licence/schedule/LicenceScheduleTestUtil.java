package uk.co.nstauthority.licensingmanagementservice.licence.schedule;

import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

public class LicenceScheduleTestUtil {

  private LicenceScheduleTestUtil() {}

  public static LicenceSchedule createLicenceSchedule(Licence licence) {
    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setId(UUID.randomUUID());
    licenceSchedule.setLicence(licence);
    return licenceSchedule;
  }

  public static LicenceScheduleDetail createLicenceScheduleDetail(UUID id, LicenceSchedule licenceSchedule) {
    var licenceScheduleDetail = new LicenceScheduleDetail();
    licenceScheduleDetail.setId(id);
    licenceScheduleDetail.setLicenceSchedule(licenceSchedule);
    return licenceScheduleDetail;
  }

  public static LicenceScheduleDetail createLicenceScheduleDetail(LicenceSchedule licenceSchedule) {
    return createLicenceScheduleDetail(UUID.randomUUID(), licenceSchedule);
  }

}
