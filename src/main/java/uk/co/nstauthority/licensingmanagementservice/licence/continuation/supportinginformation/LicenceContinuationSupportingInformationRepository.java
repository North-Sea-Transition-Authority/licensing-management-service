package uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@Repository
public interface LicenceContinuationSupportingInformationRepository
    extends JpaRepository<LicenceContinuationSupportingInformation, UUID>, NotDuplicationSource {

  Optional<LicenceContinuationSupportingInformation> findByLicenceContinuationApplicationDetail(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  );
}
