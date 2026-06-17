package uk.co.nstauthority.licensingmanagementservice.testharness;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionService;

@Service
public class TestHarnessService {

  private final LicenceTransactionService licenceTransactionService;
  private final LicencePositionService licencePositionService;
  private final Clock clock;

  public TestHarnessService(
      LicenceTransactionService licenceTransactionService,
      LicencePositionService licencePositionService,
      Clock clock
  ) {
    this.licenceTransactionService = licenceTransactionService;
    this.licencePositionService = licencePositionService;
    this.clock = clock;
  }

  @Transactional
  public void generateLicencePositions(Licence licence, Licence secondaryLicence) {
    var now = LocalDate.now(clock);
    generateSameDateLicencePositions(licence, now);
    generateSameTransactionLicencePositions(licence, now);
    generateSameTransactionDifferentLicencePositions(licence, secondaryLicence, now);
  }

  private void generateSameDateLicencePositions(Licence licence, LocalDate now) {
    var transaction1 = licenceTransactionService.createLicenceTransaction(randomRegulatorReference());
    var transaction2 = licenceTransactionService.createLicenceTransaction(randomRegulatorReference());

    licencePositionService.createLicencePosition(licence, transaction1, now.minusWeeks(7));
    licencePositionService.createLicencePosition(licence, transaction2, now.minusWeeks(7));
  }

  private void generateSameTransactionLicencePositions(Licence licence, LocalDate now) {
    var transaction =  licenceTransactionService.createLicenceTransaction(randomRegulatorReference());

    licencePositionService.createLicencePosition(licence, transaction, now.minusWeeks(5));
    licencePositionService.createLicencePosition(licence, transaction, now.minusWeeks(3));
  }

  private void generateSameTransactionDifferentLicencePositions(Licence licence, Licence secondaryLicence, LocalDate now) {
    var transaction =  licenceTransactionService.createLicenceTransaction(randomRegulatorReference());

    licencePositionService.createLicencePosition(licence, transaction, now.minusWeeks(2));
    licencePositionService.createLicencePosition(secondaryLicence, transaction, now.minusWeeks(1));
  }

  private String randomRegulatorReference() {
    return "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }
}
