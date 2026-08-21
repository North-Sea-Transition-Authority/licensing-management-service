package uk.co.nstauthority.licensingmanagementservice.testharness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionService;

@Service
@Profile("test-harness")
class TestHarnessService {

  private static final int BP_EXPLORATION_ALPHA_LTD_ID = 304;
  private static final int SHELL_PLC_ID = 9205;
  private static final int PPRS_TRAINING_ORG = 12845;

  private static final BigDecimal SHELL_INITIAL_EQUITY = BigDecimal.valueOf(60);
  private static final BigDecimal BP_INITIAL_EQUITY = BigDecimal.valueOf(40);
  private static final BigDecimal ZERO_EQUITY = BigDecimal.ZERO;
  private static final BigDecimal SHELL_TO_BP_TRANSFER = BigDecimal.valueOf(10);
  private static final BigDecimal SHELL_TO_PPRS_TRANSFER = BigDecimal.valueOf(15);

  private final LicenceTransactionService licenceTransactionService;
  private final LicencePositionService licencePositionService;
  private final LicencePositionTestHarnessService licencePositionTestHarnessService;
  private final LicencePositionChangeService licencePositionChangeService;
  private final LicencePositionFeatureTestHarnessService licencePositionFeatureTestHarnessService;
  private final Clock clock;

  TestHarnessService(
      LicenceTransactionService licenceTransactionService,
      LicencePositionService licencePositionService,
      LicencePositionTestHarnessService licencePositionTestHarnessService,
      LicencePositionChangeService licencePositionChangeService,
      LicencePositionFeatureTestHarnessService licencePositionFeatureTestHarnessService,
      Clock clock
  ) {
    this.licenceTransactionService = licenceTransactionService;
    this.licencePositionService = licencePositionService;
    this.licencePositionTestHarnessService = licencePositionTestHarnessService;
    this.licencePositionChangeService = licencePositionChangeService;
    this.licencePositionFeatureTestHarnessService = licencePositionFeatureTestHarnessService;
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

    if (licence.getType().isProduction()) {
      generateAdministratorPositionChange(licence);
    } else if (LicenceType.CARBON_STORAGE.equals(licence.getType())) {
      generateCarbonStorageBeneficialInterestPositionChanges(licence);
    }

    if (secondaryLicence.getType().isProduction()) {
      generateInitialAdministrator(secondaryLicence);
    }

    // the positions were cleared above, so each licence is given a fresh set of blocks and subareas
    licencePositionFeatureTestHarnessService.createAndLinkFeatures(licence);
    licencePositionFeatureTestHarnessService.createAndLinkFeatures(secondaryLicence);

    // a surrender needs blocks to surrender, so it is seeded once the features above exist
    if (licence.getType().isProduction()) {
      generatePartialSurrenderPositionChange(licence);
    }
  }

  private void generatePartialSurrenderPositionChange(Licence licence) {
    var executedChronologicalLicencePositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);

    var penultimatePosition =
        executedChronologicalLicencePositions.get(executedChronologicalLicencePositions.size() - 2);

    var surrenderedBlock = licencePositionService.getBlockFeatures(penultimatePosition).getFirst();

    createPartialSurrenderChange(penultimatePosition, surrenderedBlock);
  }

  private void createPartialSurrenderChange(LicencePosition licencePosition, Feature surrenderedBlock) {
    // no surrender date - the change takes the date of the position it sits on
    LicenceOperation partialSurrender = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(surrenderedBlock.getId()))
        .withBlockSurrenderTypeByFeatureId(Map.of(surrenderedBlock.getId(), BlockSurrenderType.FULL_SURRENDER))
        .build();

    licencePositionChangeService.createLicencePositionChange(
        licencePosition,
        List.of(partialSurrender),
        1,
        LicencePositionChangeStatus.CONSENTED
    );
  }

  private void generateCarbonStorageBeneficialInterestPositionChanges(Licence licence) {
    var executedChronologicalLicencePositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);

    var firstPosition = executedChronologicalLicencePositions.getFirst();
    var nonFinalPosition = executedChronologicalLicencePositions.get(executedChronologicalLicencePositions.size() - 3);

    createSetBeneficialInterestChange(firstPosition);
    createTransferBeneficialInterestChange(nonFinalPosition);
  }

  private void createSetBeneficialInterestChange(LicencePosition licencePosition) {
    var operations = List.of(
        setEquityOperation(SHELL_PLC_ID, SHELL_INITIAL_EQUITY),
        setEquityOperation(BP_EXPLORATION_ALPHA_LTD_ID, BP_INITIAL_EQUITY),
        setEquityOperation(PPRS_TRAINING_ORG, ZERO_EQUITY)
    );

    licencePositionChangeService.createLicencePositionChange(
        licencePosition,
        operations,
        1,
        LicencePositionChangeStatus.CONSENTED
    );
  }

  private void createTransferBeneficialInterestChange(LicencePosition licencePosition) {
    var operations = List.of(
        transferEquityOperation(SHELL_PLC_ID, BP_EXPLORATION_ALPHA_LTD_ID, SHELL_TO_BP_TRANSFER),
        transferEquityOperation(SHELL_PLC_ID, PPRS_TRAINING_ORG, SHELL_TO_PPRS_TRANSFER)
    );

    licencePositionChangeService.createLicencePositionChange(
        licencePosition,
        operations,
        1,
        LicencePositionChangeStatus.CONSENTED
    );
  }

  private LicenceOperation setEquityOperation(int organisationUnitId, BigDecimal equity) {
    return LicenceOperation.newSetEquityOperation()
        .withTransferTo(organisationUnitId)
        .withEquity(equity)
        .build();
  }

  private LicenceOperation transferEquityOperation(int transferFrom, int transferTo, BigDecimal equity) {
    return LicenceOperation.newTransferEquityOperation()
        .withTransferFrom(transferFrom)
        .withTransferTo(transferTo)
        .withEquity(equity)
        .build();
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
        1,
        LicencePositionChangeStatus.CONSENTED
    );
  }

  private void generateSameDateLicencePositions(Licence licence, LocalDate now) {
    var transaction1 = licenceTransactionService.createLicenceTransaction(randomRegulatorReference());
    var transaction2 = licenceTransactionService.createLicenceTransaction(randomRegulatorReference());
    var transaction3 = licenceTransactionService.createLicenceTransaction(randomRegulatorReference());
    var transaction4 = licenceTransactionService.createLicenceTransaction(randomRegulatorReference());
    var transaction5 = licenceTransactionService.createLicenceTransaction(randomRegulatorReference());

    licencePositionService.createLicencePosition(licence, transaction1, now.minusWeeks(7));
    licencePositionService.createLicencePosition(licence, transaction2, now.minusWeeks(7));
    licencePositionService.createLicencePosition(licence, transaction3, now.minusWeeks(7));
    licencePositionService.createLicencePosition(licence, transaction4, now.minusWeeks(7));
    licencePositionService.createLicencePosition(licence, transaction5, now.minusWeeks(7));
    var pairTransaction1 = licenceTransactionService.createLicenceTransaction(randomRegulatorReference());
    var pairTransaction2 = licenceTransactionService.createLicenceTransaction(randomRegulatorReference());

    licencePositionService.createLicencePosition(licence, pairTransaction1, now.minusWeeks(4));
    licencePositionService.createLicencePosition(licence, pairTransaction2, now.minusWeeks(4));
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