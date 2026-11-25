package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;

@Repository
public interface LicencePositionStagedPayloadRepository
    extends JpaRepository<LicencePositionStagedPayload, UUID>, NotDuplicationSource {

  List<LicencePositionStagedPayload> findByLicenceCorrection(LicenceCorrection licenceCorrection);

  List<LicencePositionStagedPayload> findByLicenceCorrectionAndChangeType(
      LicenceCorrection licenceCorrection,
      LicencePositionStagedPayloadChangeType changeType
  );

  boolean existsByLicenceCorrectionAndTargetLicencePositionAndChangeType(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LicencePositionStagedPayloadChangeType changeType
  );

  Optional<LicencePositionStagedPayload> findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LicencePositionStagedPayloadChangeType changeType
  );
}