package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicateThisOnUpdate;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;

@Repository
public interface LicenceScheduleExpiryRepository
    extends JpaRepository<LicenceScheduleExpiry, UUID>, DuplicationSource<LicenceScheduleDetail> {

  @DuplicateThisOnUpdate
  Optional<LicenceScheduleExpiry> findByLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail);

  List<LicenceScheduleExpiry> findAllByExpiryDateBetweenAndLicenceScheduleDetail_Status(
      LocalDate earliestExpiryDate,
      LocalDate latestExpiryDate,
      LicenceScheduleDetailStatus licenceScheduleDetailStatus
  );
}
