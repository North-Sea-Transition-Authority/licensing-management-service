package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LicenceContinuationApplicationDetailRepository
    extends JpaRepository<LicenceContinuationApplicationDetail, UUID> {
}