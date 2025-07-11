package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LicenceStartDateRepository extends JpaRepository<LicenceStartDate, UUID> {
}
