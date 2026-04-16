package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleEventStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@Repository
public interface LicenceScheduleRateRepository
    extends JpaRepository<LicenceScheduleRate, UUID>, DuplicationSource<LicenceScheduleDetail> {

  List<LicenceScheduleRate> findAllByLicenceScheduleDetailAndStartDateBetweenAndStatus(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate startDate,
      LocalDate endDate,
      LicenceScheduleEventStatus status
  );

  List<LicenceScheduleRate> findAllByLicenceScheduleTermAndRateDefinitionOptionAndStatus(
      LicenceScheduleTerm licenceScheduleTerm,
      RateDefinitionOption rateDefinitionOption,
      LicenceScheduleEventStatus status
  );

  List<LicenceScheduleRate> findAllByLicenceSchedulePhaseAndRateDefinitionOptionAndStatus(
      LicenceSchedulePhase licenceSchedulePhase,
      RateDefinitionOption rateDefinitionOption,
      LicenceScheduleEventStatus status
  );

  List<LicenceScheduleRate> findAllByLicenceScheduleDetailAndStartDateAfterAndStatus(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate date,
      LicenceScheduleEventStatus status
  );
}
