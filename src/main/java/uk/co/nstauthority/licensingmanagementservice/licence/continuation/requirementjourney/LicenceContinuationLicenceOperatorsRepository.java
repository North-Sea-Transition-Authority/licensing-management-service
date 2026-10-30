package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicateThisOnUpdate;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@Repository
public interface LicenceContinuationLicenceOperatorsRepository 
    extends JpaRepository<LicenceContinuationLicenceOperatorsRequest, UUID>,
        DuplicationSource<LicenceContinuationApplicationDetail> {

  @DuplicateThisOnUpdate
  Optional<LicenceContinuationLicenceOperatorsRequest> findByLicenceContinuationApplicationDetail(
      LicenceContinuationApplicationDetail detail
  );
  
}