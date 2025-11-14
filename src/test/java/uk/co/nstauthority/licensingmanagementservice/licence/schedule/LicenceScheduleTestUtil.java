package uk.co.nstauthority.licensingmanagementservice.licence.schedule;

import java.time.Instant;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;

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

  public static Builder licenceScheduleDetailBuilder(LicenceSchedule licenceSchedule) {
    return new Builder(licenceSchedule);
  }

  public static class Builder {

    private final LicenceSchedule licenceSchedule;
    private UUID id;
    private LicenceScheduleDetailStatus licenceScheduleDetailStatus;
    private Instant createdInstant;

    public Builder(LicenceSchedule licenceSchedule) {
      this.licenceSchedule = licenceSchedule;
    }

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withStatus(LicenceScheduleDetailStatus licenceScheduleDetailStatus) {
      this.licenceScheduleDetailStatus = licenceScheduleDetailStatus;
      return this;
    }

    public Builder withCreatedInstant(Instant createdInstant) {
      this.createdInstant = createdInstant;
      return this;
    }

    public LicenceScheduleDetail build() {
      var licenceScheduleDetail = new LicenceScheduleDetail();
      licenceScheduleDetail.setLicenceSchedule(licenceSchedule);
      licenceScheduleDetail.setId(id);
      licenceScheduleDetail.setStatus(licenceScheduleDetailStatus);
      licenceScheduleDetail.setCreatedInstant(createdInstant);
      return licenceScheduleDetail;
    }
  }
}
