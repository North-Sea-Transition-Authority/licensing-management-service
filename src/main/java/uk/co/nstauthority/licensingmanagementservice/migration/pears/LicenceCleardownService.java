package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import jakarta.transaction.Transactional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionRepository;

/**
 * Takes a licence back to holding nothing, so it can be rebuilt from PEARS. Everything it holds
 * goes, executed or not, corrections included.
 */
@Service
@ConditionalOnPearsDataSource
class LicenceCleardownService {

  private final LicencePositionRepository licencePositionRepository;
  private final LicenceCorrectionRepository licenceCorrectionRepository;
  private final LicencePositionChangeService licencePositionChangeService;
  private final LicencePositionCorrectionRepository licencePositionCorrectionRepository;
  private final LicenceTransactionRepository licenceTransactionRepository;

  LicenceCleardownService(
      LicencePositionRepository licencePositionRepository,
      LicenceCorrectionRepository licenceCorrectionRepository,
      LicencePositionChangeService licencePositionChangeService,
      LicencePositionCorrectionRepository licencePositionCorrectionRepository,
      LicenceTransactionRepository licenceTransactionRepository
  ) {
    this.licencePositionRepository = licencePositionRepository;
    this.licenceCorrectionRepository = licenceCorrectionRepository;
    this.licencePositionChangeService = licencePositionChangeService;
    this.licencePositionCorrectionRepository = licencePositionCorrectionRepository;
    this.licenceTransactionRepository = licenceTransactionRepository;
  }

  @Transactional
  void clear(Licence licence) {
    var licencePositions = licencePositionRepository.findByLicence(licence);
    var licenceCorrections = licenceCorrectionRepository.findAllByLicence(licence);
    var licencePositionCorrections = licencePositionCorrectionRepository.findAllByLicenceCorrectionIn(licenceCorrections);

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

    licencePositionChangeService.deleteForPositions(licencePositions);
    licencePositionCorrectionRepository.deleteAll(licencePositionCorrections);
    licenceCorrectionRepository.deleteAll(licenceCorrections);
    licencePositionRepository.deleteAll(licencePositions);
    licenceTransactionRepository.deleteAll(unheldLicenceTransactions);
  }
}
