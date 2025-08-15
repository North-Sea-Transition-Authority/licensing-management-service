package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Repository
public interface LicenceStartDateRepository extends JpaRepository<LicenceStartDate, UUID> {

  Optional<LicenceStartDate> findByLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail);
}
