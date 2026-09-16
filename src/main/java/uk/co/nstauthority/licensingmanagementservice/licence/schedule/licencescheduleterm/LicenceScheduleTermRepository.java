package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicateThisOnUpdate;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;

@Repository
public interface LicenceScheduleTermRepository
    extends JpaRepository<LicenceScheduleTerm, UUID>, DuplicationSource<LicenceScheduleDetail> {

  @DuplicateThisOnUpdate
  List<LicenceScheduleTerm> findAllByLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail);

  List<LicenceScheduleTerm> findAllByLicenceScheduleDetailIn(
      Collection<LicenceScheduleDetail> licenceScheduleDetails
  );

  List<LicenceScheduleTerm> findAllByEndDateBetweenAndLicenceScheduleDetail_Status(
      LocalDate earliestEndDate,
      LocalDate latestEndDate,
      LicenceScheduleDetailStatus licenceScheduleDetailStatus
  );

  Optional<LicenceScheduleTerm> findByLicenceScheduleDetailAndTermType(
      LicenceScheduleDetail licenceScheduleDetail,
      TermType termType
  );
}
