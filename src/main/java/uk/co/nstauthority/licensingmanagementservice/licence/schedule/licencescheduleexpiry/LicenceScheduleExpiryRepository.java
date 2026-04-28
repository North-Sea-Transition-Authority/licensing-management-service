package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicateThisOnUpdate;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Repository
public interface LicenceScheduleExpiryRepository
    extends JpaRepository<LicenceScheduleExpiry, UUID>, DuplicationSource<LicenceScheduleDetail> {

  @DuplicateThisOnUpdate
  Optional<LicenceScheduleExpiry> findByLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail);

}
