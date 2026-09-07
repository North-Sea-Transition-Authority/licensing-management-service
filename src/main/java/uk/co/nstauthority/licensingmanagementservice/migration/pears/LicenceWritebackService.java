package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import jakarta.transaction.Transactional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionService;

/**
 * Rebuilds a licence's positions in this application out of the live positions PEARS holds for it.
 *
 * <p>The positions are replayed through the same services the application uses when it makes a
 * position itself, so a migrated licence is indistinguishable from one built here: PEARS supplies
 * the position's date and its transaction's regulator reference, and the order a position holds
 * within its date is the order the replay arrives at, rather than a number copied across.
 */
@Service
@ConditionalOnPearsDataSource
class LicenceWritebackService {

  private final LicenceTransactionService licenceTransactionService;
  private final LicencePositionService licencePositionService;
  private final LicencePositionRepository licencePositionRepository;
  private final LicenceCorrectionRepository licenceCorrectionRepository;
  private final LicencePositionChangeRepository licencePositionChangeRepository;
  private final LicencePositionCorrectionRepository licencePositionCorrectionRepository;
  private final LicenceTransactionRepository licenceTransactionRepository;
  private final PearsLicenceService pearsLicenceService;

  LicenceWritebackService(
      LicenceTransactionService licenceTransactionService,
      LicencePositionService licencePositionService,
      LicencePositionRepository licencePositionRepository,
      LicenceCorrectionRepository licenceCorrectionRepository,
      LicencePositionChangeRepository licencePositionChangeRepository,
      LicencePositionCorrectionRepository licencePositionCorrectionRepository,
      LicenceTransactionRepository licenceTransactionRepository,
      PearsLicenceService pearsLicenceService
  ) {
    this.licenceTransactionService = licenceTransactionService;
    this.licencePositionService = licencePositionService;
    this.licencePositionRepository = licencePositionRepository;
    this.licenceCorrectionRepository = licenceCorrectionRepository;
    this.licencePositionChangeRepository = licencePositionChangeRepository;
    this.licencePositionCorrectionRepository = licencePositionCorrectionRepository;
    this.licenceTransactionRepository = licenceTransactionRepository;
    this.pearsLicenceService = pearsLicenceService;
  }

  @Transactional
  LicenceWritebackResult overwriteLicencePositionsFromPears(Licence licence) {
    var livePositions = pearsLicenceService.livePositions(
        licence.getPrefix(),
        Integer.parseInt(licence.getLicenceNumber())
    );

    if (livePositions.positions().isEmpty()) {
      return new LicenceWritebackResult("No positions found in PEARS for licence %s".formatted(licence.getLicenceReference()));
    }

    // every position the licence holds goes, executed or not, so a rebuilt licence is left holding
    // nothing but what PEARS supplies
    var licencePositions = licencePositionRepository.findByLicence(licence);
    var licencePositionChanges = licencePositionChangeRepository.findByLicencePositionIn(licencePositions);
    var licenceCorrections = licenceCorrectionRepository.findAllByLicence(licence);
    var licencePositionCorrections = licencePositionCorrectionRepository.findAllByLicenceCorrectionIn(licenceCorrections);

    var licenceTransactions = licencePositions.stream().map(LicencePosition::getLicenceTransaction).distinct().toList();
    var licencePositionIds = licencePositions.stream().map(LicencePosition::getId).collect(Collectors.toSet());

    // the licence's own positions are on their way out, so it takes a position of another licence holding
    // one of these transactions to keep it alive
    var heldTransactionIds = licencePositionRepository.findByLicenceTransactionIn(licenceTransactions)
        .stream()
        .filter(licencePosition -> !licencePositionIds.contains(licencePosition.getId()))
        .map(licencePosition -> licencePosition.getLicenceTransaction().getId())
        .collect(Collectors.toSet());

    var unheldLicenceTransactions = licenceTransactions.stream()
        .filter(licenceTransaction -> !heldTransactionIds.contains(licenceTransaction.getId()))
        .toList();

    licencePositionChangeRepository.deleteAll(licencePositionChanges);
    licencePositionCorrectionRepository.deleteAll(licencePositionCorrections);
    licenceCorrectionRepository.deleteAll(licenceCorrections);
    licencePositionRepository.deleteAll(licencePositions);
    licenceTransactionRepository.deleteAll(unheldLicenceTransactions);

    for (var livePosition : livePositions.positions()) {
      var licenceTransaction = licenceTransactionService.createLicenceTransaction(livePosition.regulatorReference());
      licencePositionService.createLicencePosition(
          licence,
          licenceTransaction,
          livePosition.positionDate()
      );
    }

    return new LicenceWritebackResult("Saved %d positions for licence %s".formatted(
        livePositions.positions().size(),
        livePositions.licenceReference()
    ));
  }
}
