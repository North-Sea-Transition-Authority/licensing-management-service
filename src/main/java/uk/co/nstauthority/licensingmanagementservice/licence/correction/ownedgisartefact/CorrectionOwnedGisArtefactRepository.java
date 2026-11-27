package uk.co.nstauthority.licensingmanagementservice.licence.correction.ownedgisartefact;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;

@Repository
public interface CorrectionOwnedGisArtefactRepository
    extends JpaRepository<CorrectionOwnedGisArtefact, UUID>, NotDuplicationSource {

  List<CorrectionOwnedGisArtefact> findByLicenceCorrectionAndArtefactType(
      LicenceCorrection licenceCorrection,
      CorrectionOwnedGisArtefactType artefactType
  );

  void deleteByLicenceCorrection(LicenceCorrection licenceCorrection);
}
