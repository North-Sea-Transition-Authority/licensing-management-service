package uk.co.nstauthority.licensingmanagementservice.testharness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSurrenderOutcome;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicenceCleardownService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionService;
import uk.co.nstauthority.licensingmanagementservice.testharness.LicencePositionFeatureTestHarnessService.SeededFeatures;
import uk.co.nstauthority.licensingmanagementservice.testharness.LicencePositionFeatureTestHarnessService.SplitBlock;

@Service
@Profile("test-harness")
class TestHarnessService {

  private static final int BP_EXPLORATION_ALPHA_LTD_ID = 304;
  private static final int FLAMSTONE_OIL_AND_GAS_LIMITED = 376;
  private static final int SHELL_PLC_ID = 9205;
  private static final int PPRS_TRAINING_ORG = 12845;
  private static final int FIRST_CHANGE_ORDER = 1;
  // each subarea a spatial change creates has its beneficial interest set in its own change, following that change
  private static final int FIRST_SUBAREA_EQUITY_CHANGE_ORDER = FIRST_CHANGE_ORDER + 1;

  private static final int FULLY_SURRENDERED_BLOCK = 0;
  private static final int PARTIALLY_SURRENDERED_BLOCK = 1;
  private static final int SUBAREA_SPLIT_BLOCK = 2;
  private static final int STRATA_SPLIT_BLOCK = 3;

  // early in the timeline, so the block holds subareas at different depths for most of it
  private static final int STRATA_SPLIT_POSITION = 2;
  // midway through the timeline, so a later position can change a subarea the split created
  private static final int SUBAREA_SPLIT_POSITION = 5;
  private static final long STRATA_SPLIT_DEPTH = -3000;

  private static final BigDecimal SHELL_INITIAL_EQUITY = BigDecimal.valueOf(60);
  private static final BigDecimal BP_INITIAL_EQUITY = BigDecimal.valueOf(40);
  private static final BigDecimal ZERO_EQUITY = BigDecimal.ZERO;
  private static final BigDecimal SHELL_TO_BP_TRANSFER = BigDecimal.valueOf(10);
  private static final BigDecimal SHELL_TO_PPRS_TRANSFER = BigDecimal.valueOf(15);
  private static final BigDecimal WHOLE_INTEREST = BigDecimal.valueOf(100);

  // the organisations wholly holding a block's seeded subareas, one for each subarea in order
  private static final List<Integer> SUBAREA_HOLDER_IDS = List.of(
      SHELL_PLC_ID,
      BP_EXPLORATION_ALPHA_LTD_ID,
      FLAMSTONE_OIL_AND_GAS_LIMITED
  );

  private final LicenceTransactionService licenceTransactionService;
  private final LicencePositionService licencePositionService;
  private final LicenceCleardownService licenceCleardownService;
  private final LicencePositionChangeService licencePositionChangeService;
  private final LicencePositionFeatureTestHarnessService licencePositionFeatureTestHarnessService;
  private final CommandJourneyService commandJourneyService;
  private final Clock clock;

  TestHarnessService(
      LicenceTransactionService licenceTransactionService,
      LicencePositionService licencePositionService,
      LicenceCleardownService licenceCleardownService,
      LicencePositionChangeService licencePositionChangeService,
      LicencePositionFeatureTestHarnessService licencePositionFeatureTestHarnessService,
      CommandJourneyService commandJourneyService,
      Clock clock
  ) {
    this.licenceTransactionService = licenceTransactionService;
    this.licencePositionService = licencePositionService;
    this.licenceCleardownService = licenceCleardownService;
    this.licencePositionChangeService = licencePositionChangeService;
    this.licencePositionFeatureTestHarnessService = licencePositionFeatureTestHarnessService;
    this.commandJourneyService = commandJourneyService;
    this.clock = clock;
  }

  @Transactional
  public void generateLicencePositions(Licence licence, Licence secondaryLicence) {
    licenceCleardownService.clear(licence);
    licenceCleardownService.clear(secondaryLicence);
    licencePositionFeatureTestHarnessService.clearFeatures(licence);
    licencePositionFeatureTestHarnessService.clearFeatures(secondaryLicence);

    var now = LocalDate.now(clock);
    generateSameDateLicencePositions(licence, now);
    generateSameTransactionLicencePositions(licence, now);
    generateSameTransactionDifferentLicencePositions(licence, secondaryLicence, now);
    var positions = licencePositionService.getExecutedChronologicalLicencePositions(licence);

    if (licence.getType().isProduction()) {
      generateAdministratorPositionChanges(positions);
    } else if (LicenceType.CARBON_STORAGE.equals(licence.getType())) {
      generateCarbonStorageBeneficialInterestPositionChanges(positions);
      generateStorageOperatorPositionChanges(positions);
    }
    generateLicenseePositionChanges(positions);

    if (secondaryLicence.getType().isProduction()) {
      generateInitialAdministrator(secondaryLicence);
    } else if (LicenceType.CARBON_STORAGE.equals(secondaryLicence.getType())) {
      generateInitialStorageOperator(secondaryLicence);
    }

    var seededFeatures = licencePositionFeatureTestHarnessService.createAndLinkFeatures(licence);
    generateSeededSubareaBeneficialInterests(licence, seededFeatures);
    var secondarySeededFeatures = licencePositionFeatureTestHarnessService.createAndLinkFeatures(secondaryLicence);
    generateSeededSubareaBeneficialInterests(secondaryLicence, secondarySeededFeatures);

    // the spatial changes need blocks and subareas to change, so are seeded once the features above exist
    if (licence.getType().isProduction()) {
      generateStrataSplitPositionChange(positions.get(STRATA_SPLIT_POSITION), seededFeatures);
      generateSubareaSplitPositionChange(positions.get(SUBAREA_SPLIT_POSITION), seededFeatures);
      var retainedHalf = createPartialSurrenderChange(penultimate(positions), seededFeatures);
      generateSubareaMergePositionChange(positions.getLast(), retainedHalf);
    }
  }

  /**
   * Every seeded subarea is wholly held by one organisation, a different one for each subarea on a block. The change
   * follows the block create that seeds the subareas.
   */
  private void generateSeededSubareaBeneficialInterests(
      Licence licence,
      SeededFeatures seededFeatures
  ) {
    var operations = seededFeatures.blocks().stream()
        .flatMap(block -> seededFeatures.subareasOf(block).stream()
            .map(subarea -> wholeInterestOperation(block, subarea, holderOf(seededFeatures, block, subarea))))
        .toList();

    if (operations.isEmpty()) {
      return;
    }

    var firstPosition = licencePositionService.getExecutedChronologicalLicencePositions(licence).getFirst();
    createSubareaEquityChanges(
        firstPosition,
        operations,
        licencePositionFeatureTestHarnessService.nextChangeOrder(firstPosition)
    );
  }

  private static int holderOf(
      SeededFeatures seededFeatures,
      Feature block,
      Feature seededSubarea
  ) {
    return SUBAREA_HOLDER_IDS.get(seededFeatures.subareasOf(block).indexOf(seededSubarea));
  }

  private void generateStrataSplitPositionChange(
      LicencePosition position,
      SeededFeatures seededFeatures
  ) {
    var block = seededFeatures.blocks().get(STRATA_SPLIT_BLOCK);
    var subarea = seededFeatures.subareaOf(block);

    var strata = licencePositionFeatureTestHarnessService.strataSplitSubarea(
        subarea,
        block,
        STRATA_SPLIT_DEPTH,
        position.getPositionDate()
    );

    createSubareaChange(position, block, List.of(subarea), strata, holderOf(seededFeatures, block, subarea));
  }

  private void generateSubareaSplitPositionChange(
      LicencePosition position,
      SeededFeatures seededFeatures
  ) {
    var block = seededFeatures.blocks().get(SUBAREA_SPLIT_BLOCK);
    var subarea = seededFeatures.subareaOf(block);

    var subareaHalves = licencePositionFeatureTestHarnessService.splitSubareaInHalf(
        subarea,
        block,
        position.getPositionDate()
    );

    createSubareaChange(position, block, List.of(subarea), subareaHalves, holderOf(seededFeatures, block, subarea));
  }

  /**
   * The subareas the partial surrender leaves on the half of the block kept are merged on the final position, so the
   * merge is a subarea change made to a surrender's output.
   */
  private void generateSubareaMergePositionChange(
      LicencePosition finalPosition,
      RetainedHalf retainedHalf
  ) {
    var mergedSubarea = licencePositionFeatureTestHarnessService.mergeSubareasOnRetainedHalf(
        retainedHalf.subareas(),
        retainedHalf.block(),
        finalPosition.getPositionDate()
    );

    createSubareaChange(
        finalPosition,
        retainedHalf.half(),
        retainedHalf.subareas(),
        List.of(mergedSubarea),
        retainedHalf.holderOfFirstSubarea()
    );
  }

  /**
   * The subareas the change outputs are wholly held by the organisation that held the subareas they came from.
   */
  private void createSubareaChange(
      LicencePosition licencePosition,
      Feature block,
      List<Feature> replacedSubareas,
      List<Feature> outputSubareas,
      int holderId
  ) {
    LicenceOperation subareaOperation = LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(block.getId())
        .withReplacedSubareas(replacedSubareas.stream().map(SeededFeatures::toSubareaDetails).toList())
        .withOutputSubareas(outputSubareas.stream().map(SeededFeatures::toSubareaDetails).toList())
        .build();

    createConsentedChange(licencePosition, List.of(subareaOperation), FIRST_CHANGE_ORDER);

    var equityOperations = outputSubareas.stream()
        .map(subarea -> wholeInterestOperation(block, subarea, holderId))
        .toList();

    createSubareaEquityChanges(licencePosition, equityOperations, FIRST_SUBAREA_EQUITY_CHANGE_ORDER);
  }

  /**
   * A change holding the beneficial interests of several subareas is shown under the first subarea's label alone, so
   * each subarea's beneficial interest is set in a change of its own.
   */
  private void createSubareaEquityChanges(
      LicencePosition licencePosition,
      List<LicenceOperation> equityOperations,
      int firstChangeOrder
  ) {
    IntStream.range(0, equityOperations.size())
        .forEach(index -> createConsentedChange(
            licencePosition,
            List.of(equityOperations.get(index)),
            firstChangeOrder + index
        ));
  }

  private LicenceOperation wholeInterestOperation(
      Feature block,
      Feature subarea,
      int organisationUnitId
  ) {
    return LicenceOperation.newSubareaSetEquityOperation()
        .withBlockFeatureId(block.getId())
        .withSubarea(SeededFeatures.toSubareaDetails(subarea))
        .withTransferTo(organisationUnitId)
        .withEquity(WHOLE_INTEREST)
        .build();
  }

  /**
   * The split block is seeded with a subarea wholly within the half surrendered, which is relinquished, one across the
   * cut, which is cropped back to the half kept, and one wholly within the half kept, which is kept as it is.
   */
  private RetainedHalf createPartialSurrenderChange(
      LicencePosition licencePosition,
      SeededFeatures seededFeatures
  ) {
    var fullySurrenderedBlock = seededFeatures.blocks().get(FULLY_SURRENDERED_BLOCK);
    var partiallySurrenderedBlock = seededFeatures.blocks().get(PARTIALLY_SURRENDERED_BLOCK);
    var surrenderDate = licencePosition.getPositionDate();

    var splitBlock = splitBlockInHalf(partiallySurrenderedBlock);

    var splitBlockSubareas = seededFeatures.subareasOf(partiallySurrenderedBlock);
    var relinquishedSubarea = splitBlockSubareas.getFirst();
    var subareaAcrossTheCut = splitBlockSubareas.get(1);
    var keptSubarea = splitBlockSubareas.getLast();

    licencePositionFeatureTestHarnessService.relinquishSubarea(relinquishedSubarea, surrenderDate);
    var croppedSubarea = licencePositionFeatureTestHarnessService.cropSubareaAcrossTheMiddle(
        subareaAcrossTheCut,
        partiallySurrenderedBlock,
        surrenderDate
    );

    var fullSurrender = new SurrenderDetails(
        BlockSurrenderType.FULL_SURRENDER,
        null,
        List.of(fullySurrenderedBlock.getId())
    );
    var partialSurrender = new SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        null,
        List.of(splitBlock.surrenderedHalf().getId()),
        List.of(splitBlock.retainedHalf().getId()),
        Map.of(
            splitBlock.retainedHalf().getId(),
            List.of(
                SubareaSurrenderOutcome.relinquished(SeededFeatures.toSubareaDetails(relinquishedSubarea)),
                SubareaSurrenderOutcome.cropped(
                    SeededFeatures.toSubareaDetails(subareaAcrossTheCut),
                    SeededFeatures.toSubareaDetails(croppedSubarea)
                ),
                SubareaSurrenderOutcome.kept(SeededFeatures.toSubareaDetails(keptSubarea))
            )
        ),
        null
    );

    // no surrender date - the change takes the date of the position it sits on
    LicenceOperation partialSurrenderOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(fullySurrenderedBlock.getId(), partiallySurrenderedBlock.getId()))
        .withSurrenderDetails(Map.of(
            fullySurrenderedBlock.getId(), fullSurrender,
            partiallySurrenderedBlock.getId(), partialSurrender
        ))
        .build();

    createConsentedChange(licencePosition, List.of(partialSurrenderOperation), FIRST_CHANGE_ORDER);

    // the subareas left on the half kept are still wholly held by the organisations that held them before
    var croppedSubareaHolderId = holderOf(seededFeatures, partiallySurrenderedBlock, subareaAcrossTheCut);
    var equityOperations = List.of(
        wholeInterestOperation(splitBlock.retainedHalf(), croppedSubarea, croppedSubareaHolderId),
        wholeInterestOperation(
            splitBlock.retainedHalf(),
            keptSubarea,
            holderOf(seededFeatures, partiallySurrenderedBlock, keptSubarea)
        )
    );

    createSubareaEquityChanges(licencePosition, equityOperations, FIRST_SUBAREA_EQUITY_CHANGE_ORDER);

    return new RetainedHalf(
        partiallySurrenderedBlock,
        splitBlock.retainedHalf(),
        List.of(croppedSubarea, keptSubarea),
        croppedSubareaHolderId
    );
  }

  private SplitBlock splitBlockInHalf(Feature block) {
    // the journey is only the working area the split is made in: the executed change records the halves outright
    var commandJourney = commandJourneyService.createAndAssignCommandJourney(List.of(block));
    var splitBlock = licencePositionFeatureTestHarnessService.splitBlockInHalf(commandJourney, block);
    commandJourneyService.deleteAllExcludingActiveFeatures(commandJourney.getId());
    return splitBlock;
  }

  /**
   * The half of a partially surrendered block kept, with the subareas the surrender left on it and the organisation
   * wholly holding the first of them.
   */
  private record RetainedHalf(
      Feature block,
      Feature half,
      List<Feature> subareas,
      int holderOfFirstSubarea
  ) {
  }

  private void generateCarbonStorageBeneficialInterestPositionChanges(List<LicencePosition> positions) {
    createSetBeneficialInterestChange(positions.getFirst());
    createTransferBeneficialInterestChange(positions.get(positions.size() - 3));
  }

  private void createSetBeneficialInterestChange(LicencePosition licencePosition) {
    var operations = List.of(
        setEquityOperation(SHELL_PLC_ID, SHELL_INITIAL_EQUITY),
        setEquityOperation(BP_EXPLORATION_ALPHA_LTD_ID, BP_INITIAL_EQUITY),
        setEquityOperation(PPRS_TRAINING_ORG, ZERO_EQUITY)
    );

    createConsentedChange(licencePosition, operations, FIRST_CHANGE_ORDER);
  }

  private void createTransferBeneficialInterestChange(LicencePosition licencePosition) {
    var operations = List.of(
        transferEquityOperation(SHELL_PLC_ID, BP_EXPLORATION_ALPHA_LTD_ID, SHELL_TO_BP_TRANSFER),
        transferEquityOperation(SHELL_PLC_ID, PPRS_TRAINING_ORG, SHELL_TO_PPRS_TRANSFER)
    );

    createConsentedChange(licencePosition, operations, FIRST_CHANGE_ORDER);
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

  private void generateStorageOperatorPositionChanges(List<LicencePosition> positions) {
    createStorageOperatorChange(positions.getFirst(), SHELL_PLC_ID);
    createStorageOperatorChange(positions.get(positions.size() - 3), BP_EXPLORATION_ALPHA_LTD_ID);
  }

  private void generateInitialStorageOperator(Licence licence) {
    var firstPosition = licencePositionService.getExecutedChronologicalLicencePositions(licence).getFirst();

    createStorageOperatorChange(firstPosition, SHELL_PLC_ID);
  }

  private void createStorageOperatorChange(LicencePosition licencePosition, int operatorId) {
    var storageOperatorChange = LicenceOperation.newStorageOperatorOperation()
        .withOperator(operatorId)
        .build();

    createConsentedChange(licencePosition, List.of(storageOperatorChange), FIRST_CHANGE_ORDER);
  }

  private void generateLicenseePositionChanges(List<LicencePosition> positions) {
    createLicenseeChange(positions.getFirst(), List.of(BP_EXPLORATION_ALPHA_LTD_ID, FLAMSTONE_OIL_AND_GAS_LIMITED), List.of());
    createLicenseeChange(positions.get(positions.size() - 4), List.of(SHELL_PLC_ID), List.of(BP_EXPLORATION_ALPHA_LTD_ID));
  }

  private void createLicenseeChange(
      LicencePosition licencePosition,
      List<Integer> licenseesToAdd,
      List<Integer> licenseesToRemove
  ) {
    var licenseeChange = LicenceOperation.newLicenseeOperation()
        .withLicenseesToAdd(licenseesToAdd)
        .withLicenseesToRemove(licenseesToRemove)
        .build();

    createConsentedChange(licencePosition, List.of(licenseeChange), FIRST_CHANGE_ORDER);
  }

  private void generateAdministratorPositionChanges(List<LicencePosition> positions) {
    createAdministratorChange(positions.getFirst(), SHELL_PLC_ID);
    createAdministratorChange(positions.get(positions.size() - 3), BP_EXPLORATION_ALPHA_LTD_ID);
  }

  private void generateInitialAdministrator(Licence licence) {
    var firstPosition = licencePositionService.getExecutedChronologicalLicencePositions(licence).getFirst();

    createAdministratorChange(firstPosition, SHELL_PLC_ID);
  }

  private void createAdministratorChange(LicencePosition licencePosition, int operatorId) {
    var administratorChange = LicenceOperation.newAdministratorChange()
        .withOperator(operatorId)
        .build();

    createConsentedChange(licencePosition, List.of(administratorChange), FIRST_CHANGE_ORDER);
  }

  private void createConsentedChange(
      LicencePosition licencePosition,
      List<LicenceOperation> operations,
      int changeOrder
  ) {
    licencePositionChangeService.createLicencePositionChange(
        licencePosition,
        operations,
        changeOrder,
        LicencePositionChangeStatus.CONSENTED
    );
  }

  private static LicencePosition penultimate(List<LicencePosition> positions) {
    return positions.get(positions.size() - 2);
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
