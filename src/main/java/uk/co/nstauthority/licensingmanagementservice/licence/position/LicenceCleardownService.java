package uk.co.nstauthority.licensingmanagementservice.licence.position;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionRepository;

/**
 * Takes a licence back to holding nothing, so it can be rebuilt. Everything it holds goes,
 * executed or not, corrections included.
 */
@Service
public class LicenceCleardownService {

  private final LicencePositionRepository licencePositionRepository;
  private final LicenceCorrectionRepository licenceCorrectionRepository;
  private final LicenceTransactionRepository licenceTransactionRepository;
  private final EntityManager entityManager;

  LicenceCleardownService(
      LicencePositionRepository licencePositionRepository,
      LicenceCorrectionRepository licenceCorrectionRepository,
      LicenceTransactionRepository licenceTransactionRepository,
      EntityManager entityManager
  ) {
    this.licencePositionRepository = licencePositionRepository;
    this.licenceCorrectionRepository = licenceCorrectionRepository;
    this.licenceTransactionRepository = licenceTransactionRepository;
    this.entityManager = entityManager;
  }

  @Transactional
  public void clear(Licence licence) {
    var licencePositions = licencePositionRepository.findByLicence(licence);
    var licenceCorrections = licenceCorrectionRepository.findAllByLicence(licence);

    var licenceTransactions = licencePositions.stream().map(LicencePosition::getLicenceTransaction).distinct().toList();
    var licencePositionIds = licencePositions.stream().map(LicencePosition::getId).collect(Collectors.toSet());

    // The licence's own positions are on their way out, so it takes a position of another licence
    // holding one of these transactions to keep it alive.
    var heldTransactionIds = licencePositionRepository.findByLicenceTransactionIn(licenceTransactions)
        .stream()
        .filter(licencePosition -> !licencePositionIds.contains(licencePosition.getId()))
        .map(licencePosition -> licencePosition.getLicenceTransaction().getId())
        .collect(Collectors.toSet());

    var unheldLicenceTransactions = licenceTransactions.stream()
        .filter(licenceTransaction -> !heldTransactionIds.contains(licenceTransaction.getId()))
        .toList();

    deleteChangesOf(licencePositions);
    deletePositionCorrectionsOf(licenceCorrections);
    licenceCorrectionRepository.deleteAll(licenceCorrections);
    licencePositionRepository.deleteAll(licencePositions);
    licenceTransactionRepository.deleteAll(unheldLicenceTransactions);
  }

  /**
   * A bulk delete never loads the rows, so operations stored in a shape that no longer
   * deserialises are cleared all the same. It is not audited.
   */
  private void deleteChangesOf(List<LicencePosition> licencePositions) {
    if (licencePositions.isEmpty()) {
      return;
    }

    entityManager.createQuery("DELETE FROM licence_position_changes c WHERE c.licencePosition IN :licencePositions")
        .setParameter("licencePositions", licencePositions)
        .executeUpdate();
  }

  /**
   * A bulk delete never loads the rows, so payloads stored in a shape that no longer
   * deserialises are cleared all the same. It is not audited.
   */
  private void deletePositionCorrectionsOf(List<LicenceCorrection> licenceCorrections) {
    if (licenceCorrections.isEmpty()) {
      return;
    }

    entityManager.createQuery("DELETE FROM licence_position_corrections c WHERE c.licenceCorrection IN :licenceCorrections")
        .setParameter("licenceCorrections", licenceCorrections)
        .executeUpdate();
  }
}
