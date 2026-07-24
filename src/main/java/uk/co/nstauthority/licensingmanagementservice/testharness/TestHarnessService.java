package uk.co.nstauthority.licensingmanagementservice.testharness;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionService;

@Service
public class TestHarnessService {

  private static final int BP_EXPLORATION_ALPHA_LTD_ID = 304;
  private static final int SHELL_PLC_ID = 9205;

  private final LicenceTransactionService licenceTransactionService;
  private final LicencePositionService licencePositionService;
  private final LicencePositionTestHarnessService licencePositionTestHarnessService;
  private final LicencePositionChangeService licencePositionChangeService;
  private final Clock clock;

  public TestHarnessService(
      LicenceTransactionService licenceTransactionService,
      LicencePositionService licencePositionService,
      LicencePositionTestHarnessService licencePositionTestHarnessService,
      LicencePositionChangeService licencePositionChangeService,
      Clock clock
  ) {
    this.licenceTransactionService = licenceTransactionService;
    this.licencePositionService = licencePositionService;
    this.licencePositionTestHarnessService = licencePositionTestHarnessService;
    this.licencePositionChangeService = licencePositionChangeService;
    this.clock = clock;
  }

  @Transactional
  public void generateLicencePositions(Licence licence, Licence secondaryLicence) {
    // clear any existing positions and changes
    licencePositionTestHarnessService.clearPositionsForLicence(licence);
    licencePositionTestHarnessService.clearPositionsForLicence(secondaryLicence);

    var now = LocalDate.now(clock);
    generateSameDateLicencePositions(licence, now);
    generateSameTransactionLicencePositions(licence, now);
    generateSameTransactionDifferentLicencePositions(licence, secondaryLicence, now);

    if (Boolean.TRUE.equals(licence.getType().isProduction())) {
      generateAdministratorPositionChange(licence);
    }

    if (Boolean.TRUE.equals(secondaryLicence.getType().isProduction())) {
      generateInitialAdministrator(secondaryLicence);
    }
  }

  private void generateAdministratorPositionChange(Licence licence) {
    var executedChronologicalLicencePositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);

    var firstPosition = executedChronologicalLicencePositions.getFirst();
    var nonFinalPosition = executedChronologicalLicencePositions.get(executedChronologicalLicencePositions.size() - 3);

    createAdministratorChange(firstPosition, SHELL_PLC_ID);
    createAdministratorChange(nonFinalPosition, BP_EXPLORATION_ALPHA_LTD_ID);
  }

  private void generateInitialAdministrator(Licence licence) {
    var executedChronologicalLicencePositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);

    var firstPosition = executedChronologicalLicencePositions.getFirst();

    createAdministratorChange(firstPosition, SHELL_PLC_ID);
  }

  private void createAdministratorChange(LicencePosition licencePosition, int operatorId) {
    var administratorChange = LicenceOperation.newAdministratorChange()
        .withOperator(operatorId)
        .build();

    licencePositionChangeService.createLicencePositionChange(
        licencePosition,
        List.of(administratorChange),
        1L,
        LicencePositionChangeStatus.CONSENTED
    );
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
