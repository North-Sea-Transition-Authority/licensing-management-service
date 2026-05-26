package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicateThisOnUpdate;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@Repository
public interface LicenceScheduleRateRepository
    extends JpaRepository<LicenceScheduleRate, UUID>, DuplicationSource<LicenceScheduleDetail> {

  @DuplicateThisOnUpdate
  List<LicenceScheduleRate> findAllByLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail);

  List<LicenceScheduleRate> findAllByLicenceScheduleDetailAndStartDateBetween(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate startDate,
      LocalDate endDate
  );

  List<LicenceScheduleRate> findAllByLicenceScheduleTermAndRateDefinitionOption(
      LicenceScheduleTerm licenceScheduleTerm,
      RateDefinitionOption rateDefinitionOption
  );

  List<LicenceScheduleRate> findAllByLicenceSchedulePhaseAndRateDefinitionOption(
      LicenceSchedulePhase licenceSchedulePhase,
      RateDefinitionOption rateDefinitionOption
  );

  List<LicenceScheduleRate> findAllByLicenceScheduleDetailAndStartDateAfter(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate date
  );

  Optional<LicenceScheduleRate> findByLicenceScheduleDetailAndEventReference(
      LicenceScheduleDetail licenceScheduleDetail,
      EventReference eventReference
  );
}
