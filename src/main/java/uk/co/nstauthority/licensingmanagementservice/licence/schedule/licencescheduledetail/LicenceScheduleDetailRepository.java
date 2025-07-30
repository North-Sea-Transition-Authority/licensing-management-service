package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;

@Repository
public interface LicenceScheduleDetailRepository extends JpaRepository<LicenceScheduleDetail, UUID> {
  Optional<LicenceScheduleDetail> findByLicenceSchedule(LicenceSchedule licenceSchedule);

  Optional<LicenceScheduleDetail> findByLicenceSchedule_Licence(Licence licence);
}
