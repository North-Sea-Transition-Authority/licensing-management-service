package uk.co.nstauthority.licensingmanagementservice.licence.continuation.externalcontributorjourney;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@Repository
public interface LicenceContinuationExternalContributorRepository
    extends JpaRepository<LicenceContinuationExternalContributorRequest, UUID>, NotDuplicationSource {

  Optional<LicenceContinuationExternalContributorRequest> findByLicenceContinuationApplicationDetail(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  );
}
