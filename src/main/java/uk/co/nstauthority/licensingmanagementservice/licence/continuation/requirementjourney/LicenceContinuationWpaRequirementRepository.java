package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@Repository
public interface LicenceContinuationWpaRequirementRepository
    extends JpaRepository<LicenceContinuationWpaRequirementRequest, UUID>, NotDuplicationSource {

  @EntityGraph(attributePaths = "licenceContinuationApplicationDetail")
  Optional<LicenceContinuationWpaRequirementRequest> findByLicenceContinuationApplicationDetail(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  );
}
