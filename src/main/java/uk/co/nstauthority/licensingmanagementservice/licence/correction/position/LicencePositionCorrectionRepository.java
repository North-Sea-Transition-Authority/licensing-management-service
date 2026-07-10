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
public interface LicencePositionCorrectionRepository
    extends JpaRepository<LicencePositionCorrection, UUID>, NotDuplicationSource {

  List<LicencePositionCorrection> findByLicenceCorrectionAndChangeType(
      LicenceCorrection licenceCorrection,
      LicencePositionCorrectionChangeType changeType
  );

  Optional<LicencePositionCorrection> findByIdAndLicenceCorrection(
      UUID id,
      LicenceCorrection licenceCorrection
  );

  boolean existsByLicenceCorrectionAndTargetLicencePositionAndChangeType(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LicencePositionCorrectionChangeType changeType
  );
}