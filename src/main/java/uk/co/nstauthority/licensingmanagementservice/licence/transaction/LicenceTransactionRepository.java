package uk.co.nstauthority.licensingmanagementservice.licence.transaction;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface LicenceTransactionRepository extends JpaRepository<LicenceTransaction, UUID>, NotDuplicationSource {
}
