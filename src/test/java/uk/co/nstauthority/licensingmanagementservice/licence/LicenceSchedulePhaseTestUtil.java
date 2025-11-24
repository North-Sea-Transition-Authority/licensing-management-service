package uk.co.nstauthority.licensingmanagementservice.licence;

import java.time.LocalDate;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleEventStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

public class LicenceSchedulePhaseTestUtil {

  private LicenceSchedulePhaseTestUtil() {}

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private UUID id;
    private LicenceScheduleTerm licenceScheduleTerm;
    private LicenceScheduleDetail licenceScheduleDetail;
    private PhaseType phaseType;
    private ThreeFieldDuration phaseDuration;
    private LocalDate startDate;
    private LocalDate endDate;
    private String comments;
    private LicenceScheduleEventStatus status;

    private Builder() {}

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withLicenceScheduleTerm(LicenceScheduleTerm licenceScheduleTerm) {
      this.licenceScheduleTerm = licenceScheduleTerm;
      return this;
    }

    public Builder withLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail) {
      this.licenceScheduleDetail = licenceScheduleDetail;
      return this;
    }

    public Builder withPhaseType(PhaseType phaseType) {
      this.phaseType = phaseType;
      return this;
    }

    public Builder withPhaseDuration(ThreeFieldDuration phaseDuration) {
      this.phaseDuration = phaseDuration;
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

    public Builder withComments(String comments) {
      this.comments = comments;
      return this;
    }

    public Builder withStatus(LicenceScheduleEventStatus status) {
      this.status = status;
      return this;
    }

    public LicenceSchedulePhase build() {
      var licenceSchedulePhase = new LicenceSchedulePhase();

      licenceSchedulePhase.setId(id);
      licenceSchedulePhase.setLicenceScheduleTerm(licenceScheduleTerm);
      licenceSchedulePhase.setLicenceScheduleDetail(licenceScheduleDetail);
      licenceSchedulePhase.setPhaseType(phaseType);
      licenceSchedulePhase.setPhaseDuration(phaseDuration);
      licenceSchedulePhase.setStartDate(startDate);
      licenceSchedulePhase.setEndDate(endDate);
      licenceSchedulePhase.setComments(comments);
      licenceSchedulePhase.setStatus(status);

      return licenceSchedulePhase;
    }
  }
}