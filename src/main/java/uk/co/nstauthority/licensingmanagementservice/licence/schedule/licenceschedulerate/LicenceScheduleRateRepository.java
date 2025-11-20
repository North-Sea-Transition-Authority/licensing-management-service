package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LicenceScheduleRateRepository extends JpaRepository<LicenceScheduleRate, UUID> {
}
