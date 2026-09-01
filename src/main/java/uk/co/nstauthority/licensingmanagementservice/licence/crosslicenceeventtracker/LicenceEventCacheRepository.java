package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface LicenceEventCacheRepository extends JpaRepository<LicenceEventCache, UUID>, NotDuplicationSource {
}
