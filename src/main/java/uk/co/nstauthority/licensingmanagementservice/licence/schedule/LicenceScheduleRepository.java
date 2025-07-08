package uk.co.nstauthority.licensingmanagementservice.licence.schedule;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Repository
public interface LicenceScheduleRepository extends JpaRepository<LicenceSchedule, UUID> {

  boolean existsByLicence(Licence licence);

}
