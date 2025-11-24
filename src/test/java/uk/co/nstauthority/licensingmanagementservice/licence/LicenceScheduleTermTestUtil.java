package uk.co.nstauthority.licensingmanagementservice.licence;

import java.time.LocalDate;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleEventStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

public class LicenceScheduleTermTestUtil {

  private LicenceScheduleTermTestUtil() {}

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private UUID id;
    private LicenceScheduleDetail licenceScheduleDetail;
    private TermType termType;
    private ThreeFieldDuration termDuration;
    private LocalDate startDate;
    private LocalDate endDate;
    private LicenceScheduleEventStatus status;

    private Builder() {}

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail) {
      this.licenceScheduleDetail = licenceScheduleDetail;
      return this;
    }

    public Builder withTermType(TermType termType) {
      this.termType = termType;
      return this;
    }

    public Builder withTermDuration(ThreeFieldDuration termDuration) {
      this.termDuration = termDuration;
      return this;
    }

    public Builder withStartDate(LocalDate startDate) {
      this.startDate = startDate;
      return this;
    }

    public Builder withEndDate(LocalDate endDate) {
      this.endDate = endDate;
      return this;
    }

    public Builder withStatus(LicenceScheduleEventStatus status) {
      this.status = status;
      return this;
    }

    public LicenceScheduleTerm build() {
      var licenceScheduleTerm = new LicenceScheduleTerm();
      licenceScheduleTerm.setId(id);
      licenceScheduleTerm.setLicenceScheduleDetail(licenceScheduleDetail);
      licenceScheduleTerm.setTermType(termType);
      licenceScheduleTerm.setTermDuration(termDuration);
      licenceScheduleTerm.setStartDate(startDate);
      licenceScheduleTerm.setEndDate(endDate);
      licenceScheduleTerm.setStatus(status);

      return licenceScheduleTerm;
    }
  }
}