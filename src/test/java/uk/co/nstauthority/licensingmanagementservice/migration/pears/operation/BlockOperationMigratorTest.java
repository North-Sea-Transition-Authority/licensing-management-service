package uk.co.nstauthority.licensingmanagementservice.migration.pears.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.BlockCreateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.BlockEndOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.BlockRedefinitionOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceHistory;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsPosition;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsOperation;

@ExtendWith(MockitoExtension.class)
class BlockOperationMigratorTest {

  private static final LocalDate POSITION_DATE = LocalDate.of(1964, Month.SEPTEMBER, 18);
  private static final long FIRST_TRANSACTION_ID = 15188;

  private static final int CREATED_SI_ID = 739001;
  private static final int REPLACED_SI_ID = 739002;
  private static final int SUCCESSOR_SI_ID = 739003;
  private static final int SECOND_REPLACED_SI_ID = 739004;
  private static final int SECOND_SUCCESSOR_SI_ID = 739005;

  private static final UUID CREATED_FEATURE_ID = UUID.randomUUID();
  private static final UUID REPLACED_FEATURE_ID = UUID.randomUUID();
  private static final UUID SUCCESSOR_FEATURE_ID = UUID.randomUUID();
  private static final UUID SECOND_REPLACED_FEATURE_ID = UUID.randomUUID();
  private static final UUID SECOND_SUCCESSOR_FEATURE_ID = UUID.randomUUID();

  @Mock
  private FeatureService featureService;

  private BlockOperationMigrator blockOperationMigrator;

  private MigrationNotes notes;

  @BeforeEach
  void setUp() {
    blockOperationMigrator = new BlockOperationMigrator(featureService);
    notes = new MigrationNotes();
  }

  @Test
  void migrate_whenThePositionHoldsNoBlockOperations_thenNothingIsCarriedAcross() {
    var history = createHistoryFrom(createPositionFrom(FIRST_TRANSACTION_ID,
        new PearsOperation.Omitted(createHeaderFrom(1000, 1), "PED_CONSORTIUM_LIST_CREATE")
    ));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertThat(changes).isEmpty();
    verifyNoInteractions(featureService);
  }

  @Test
  void migrate_whenBlocksAreCreated_thenTheyAreCarriedAcrossAsABlockCreation() {
    var history = createHistoryFrom(createPositionFrom(
        FIRST_TRANSACTION_ID, createBlockCreateFrom(1000, 1, createEntryFrom(CREATED_SI_ID, "100"))));
    when(featureService.findAllByLegacyIdIn(Set.of(CREATED_SI_ID)))
        .thenReturn(List.of(createFeatureFrom(CREATED_SI_ID, CREATED_FEATURE_ID)));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(MigratedChange.from(
        new PearsPositionKey(FIRST_TRANSACTION_ID),
        new PearsOperationKey(1, 1000),
        List.of(new BlockCreateOperation(List.of(CREATED_FEATURE_ID), Map.of())))));
  }

  @Test
  void migrate_whenACreatedBlockHasNoFeature_thenNothingIsCarriedAcrossAndItIsReported() {
    var history = createHistoryFrom(createPositionFrom(
        FIRST_TRANSACTION_ID, createBlockCreateFrom(1000, 1, createEntryFrom(CREATED_SI_ID, "100"))));
    when(featureService.findAllByLegacyIdIn(Set.of(CREATED_SI_ID))).thenReturn(List.of());

    var changes = blockOperationMigrator.migrate(history, notes);

    assertThat(changes).isEmpty();
    assertThat(notes.unmappedCountsByReason())
        .containsEntry("block creation whose blocks could not be resolved", 1);
  }

  @Test
  void migrate_whenABlockOperationNamesNoBlocks_thenNothingIsCarriedAcrossAndItIsReported() {
    var history = createHistoryFrom(createPositionFrom(FIRST_TRANSACTION_ID, createBlockCreateFrom(1000, 1)));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertThat(changes).isEmpty();
    assertThat(notes.unmappedCountsByReason())
        .containsEntry("block operation that named no blocks", 1);
  }

  @Test
  void migrate_whenABlockChangeGivesUpArea_thenItIsCarriedAcrossAsAPartialSurrender() {
    var history = createHistoryFrom(createPositionFrom(FIRST_TRANSACTION_ID,
        createBlockChangeFrom(1000, 1, createEntryFrom(REPLACED_SI_ID, "100", SUCCESSOR_SI_ID, "60"))
    ));
    when(featureService.findAllByLegacyIdIn(Set.of(REPLACED_SI_ID, SUCCESSOR_SI_ID)))
        .thenReturn(List.of(
            createFeatureFrom(REPLACED_SI_ID, REPLACED_FEATURE_ID),
            createFeatureFrom(SUCCESSOR_SI_ID, SUCCESSOR_FEATURE_ID))
        );

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(MigratedChange.from(
        new PearsPositionKey(FIRST_TRANSACTION_ID),
        new PearsOperationKey(1, 1000),
        List.of(new PartialSurrenderOperation(
            POSITION_DATE,
            List.of(REPLACED_FEATURE_ID),
            Map.of(REPLACED_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
                BlockSurrenderType.PARTIAL_SURRENDER, null, List.of(), List.of(SUCCESSOR_FEATURE_ID)))))
        )
    ));
  }

  @Test
  void migrate_whenABlockChangeGivesUpNoArea_thenItIsCarriedAcrossAsARedefinition() {
    var history = createHistoryFrom(createPositionFrom(FIRST_TRANSACTION_ID,
        createBlockChangeFrom(1000, 1, createEntryFrom(REPLACED_SI_ID, "100", SUCCESSOR_SI_ID, "100"))));
    when(featureService.findAllByLegacyIdIn(Set.of(REPLACED_SI_ID, SUCCESSOR_SI_ID)))
        .thenReturn(List.of(
            createFeatureFrom(REPLACED_SI_ID, REPLACED_FEATURE_ID),
            createFeatureFrom(SUCCESSOR_SI_ID, SUCCESSOR_FEATURE_ID))
        );

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(MigratedChange.from(
        new PearsPositionKey(FIRST_TRANSACTION_ID),
        new PearsOperationKey(1, 1000),
        List.of(new BlockRedefinitionOperation(List.of(REPLACED_FEATURE_ID), List.of(SUCCESSOR_FEATURE_ID), List.of(), Map.of())))));
    assertThat(notes.noteCountsByReason())
        .containsEntry("block change that gave up no area, carried across as a redefinition", 1);
  }

  @Test
  void migrate_whenASupersededBlockHasNoFeature_thenNothingIsCarriedAcrossAndItIsReported() {
    var history = createHistoryFrom(createPositionFrom(FIRST_TRANSACTION_ID,
        createBlockChangeFrom(1000, 1, createEntryFrom(REPLACED_SI_ID, "100", SUCCESSOR_SI_ID, "60"))));
    when(featureService.findAllByLegacyIdIn(Set.of(REPLACED_SI_ID, SUCCESSOR_SI_ID)))
        .thenReturn(List.of(createFeatureFrom(SUCCESSOR_SI_ID, SUCCESSOR_FEATURE_ID)));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertThat(changes).isEmpty();
    assertThat(notes.unmappedCountsByReason())
        .containsEntry("block change whose superseded block could not be resolved", 1);
  }

  @Test
  void migrate_whenOnePositionHoldsSeveralSurrenders_thenTheyFoldIntoOneSurrenderAtTheFirstOfThem() {
    var history = createHistoryFrom(createPositionFrom(FIRST_TRANSACTION_ID,
        createBlockChangeFrom(1000, 1, createEntryFrom(REPLACED_SI_ID, "100", SUCCESSOR_SI_ID, "60")),
        createBlockChangeFrom(1001, 2, createEntryFrom(SECOND_REPLACED_SI_ID, "100", SECOND_SUCCESSOR_SI_ID, "60"))));
    when(featureService.findAllByLegacyIdIn(
        Set.of(REPLACED_SI_ID, SUCCESSOR_SI_ID, SECOND_REPLACED_SI_ID, SECOND_SUCCESSOR_SI_ID))
    ).thenReturn(List.of(
        createFeatureFrom(REPLACED_SI_ID, REPLACED_FEATURE_ID),
        createFeatureFrom(SUCCESSOR_SI_ID, SUCCESSOR_FEATURE_ID),
        createFeatureFrom(SECOND_REPLACED_SI_ID, SECOND_REPLACED_FEATURE_ID),
        createFeatureFrom(SECOND_SUCCESSOR_SI_ID, SECOND_SUCCESSOR_FEATURE_ID)
    ));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(MigratedChange.from(
        new PearsPositionKey(FIRST_TRANSACTION_ID),
        new PearsOperationKey(1, 1000),
        List.of(new PartialSurrenderOperation(
            POSITION_DATE,
            List.of(REPLACED_FEATURE_ID, SECOND_REPLACED_FEATURE_ID),
            Map.of(
                REPLACED_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
                    BlockSurrenderType.PARTIAL_SURRENDER, null, List.of(), List.of(SUCCESSOR_FEATURE_ID)),
                SECOND_REPLACED_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
                    BlockSurrenderType.PARTIAL_SURRENDER, null, List.of(), List.of(SECOND_SUCCESSOR_FEATURE_ID))))))));
  }

  @Test
  void migrate_whenAPositionCreatesAndSurrendersBlocks_thenBothAreCarriedAcrossInPearsOrder() {
    var history = createHistoryFrom(createPositionFrom(FIRST_TRANSACTION_ID,
        createBlockCreateFrom(1000, 1, createEntryFrom(CREATED_SI_ID, "100")),
        createBlockChangeFrom(1001, 2, createEntryFrom(REPLACED_SI_ID, "100", SUCCESSOR_SI_ID, "60"))));
    when(featureService.findAllByLegacyIdIn(Set.of(CREATED_SI_ID, REPLACED_SI_ID, SUCCESSOR_SI_ID)))
        .thenReturn(List.of(
            createFeatureFrom(CREATED_SI_ID, CREATED_FEATURE_ID),
            createFeatureFrom(REPLACED_SI_ID, REPLACED_FEATURE_ID),
            createFeatureFrom(SUCCESSOR_SI_ID, SUCCESSOR_FEATURE_ID))
        );

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(
        MigratedChange.from(
            new PearsPositionKey(FIRST_TRANSACTION_ID),
            new PearsOperationKey(1, 1000),
            List.of(new BlockCreateOperation(List.of(CREATED_FEATURE_ID), Map.of()))),
        MigratedChange.from(
            new PearsPositionKey(FIRST_TRANSACTION_ID),
            new PearsOperationKey(2, 1001),
            List.of(new PartialSurrenderOperation(
                POSITION_DATE,
                List.of(REPLACED_FEATURE_ID),
                Map.of(REPLACED_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
                    BlockSurrenderType.PARTIAL_SURRENDER, null, List.of(), List.of(SUCCESSOR_FEATURE_ID))))))));
  }

  @Test
  void migrate_whenABlockEnds_thenTheBlockIsEnded() {
    var history = createHistoryFrom(createPositionFrom(FIRST_TRANSACTION_ID,
        createBlockEndFrom(1000, 1, createEntryFrom(REPLACED_SI_ID, "100", null, null))));
    when(featureService.findAllByLegacyIdIn(Set.of(REPLACED_SI_ID)))
        .thenReturn(List.of(createFeatureFrom(REPLACED_SI_ID, REPLACED_FEATURE_ID)));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(MigratedChange.from(
        new PearsPositionKey(FIRST_TRANSACTION_ID),
        new PearsOperationKey(1, 1000),
        List.of(new BlockEndOperation(List.of(REPLACED_FEATURE_ID), List.of())))));
  }

  @Test
  void migrate_whenABlockEndCannotBeResolved_thenNothingIsCarriedAcrossAndItIsReported() {
    var history = createHistoryFrom(createPositionFrom(FIRST_TRANSACTION_ID,
        createBlockEndFrom(1000, 1, createEntryFrom(REPLACED_SI_ID, "100", null, null))));
    when(featureService.findAllByLegacyIdIn(Set.of(REPLACED_SI_ID))).thenReturn(List.of());

    var changes = blockOperationMigrator.migrate(history, notes);

    assertThat(changes).isEmpty();
    assertThat(notes.unmappedCountsByReason())
        .containsEntry("block end whose blocks could not be resolved", 1);
  }

  private static void assertChangesMatch(List<MigratedChange> changes, List<MigratedChange> expected) {
    assertThat(changes)
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes(".*\\.id")
        .isEqualTo(expected);
  }

  private static Feature createFeatureFrom(int siId, UUID featureId) {
    return FeatureTestUtil.builder()
        .withId(featureId)
        .withLegacyId(siId)
        .build();
  }

  private static PearsLicenceHistory createHistoryFrom(PearsPosition... positions) {
    return new PearsLicenceHistory("P", 8, List.of(positions));
  }

  private static PearsPosition createPositionFrom(long transactionId, PearsOperation... operations) {
    return new PearsPosition(transactionId, "XPT/1", POSITION_DATE, 1, 1, List.of(operations));
  }

  private static PearsOperation.BlockCreate createBlockCreateFrom(
      long operationId,
      int operationSequence,
      PearsOperation.BlockEntry... entries
  ) {
    return new PearsOperation.BlockCreate(createHeaderFrom(operationId, operationSequence), List.of(entries));
  }

  private static PearsOperation.BlockChange createBlockChangeFrom(
      long operationId,
      int operationSequence,
      PearsOperation.BlockEntry... entries
  ) {
    return new PearsOperation.BlockChange(createHeaderFrom(operationId, operationSequence), List.of(entries));
  }

  private static PearsOperation.BlockEnd createBlockEndFrom(
      long operationId,
      int operationSequence,
      PearsOperation.BlockEntry... entries
  ) {
    return new PearsOperation.BlockEnd(createHeaderFrom(operationId, operationSequence), List.of(entries));
  }

  private static PearsOperation.Header createHeaderFrom(long operationId, int operationSequence) {
    return new PearsOperation.Header(
        FIRST_TRANSACTION_ID,
        "XPT/1",
        POSITION_DATE,
        1,
        operationId,
        operationSequence,
        PearsOperation.OperationStatus.LIVE,
        null,
        "P"
    );
  }

  private static PearsOperation.BlockEntry createEntryFrom(int outputSiId, String outputArea) {
    return createEntryFrom(null, null, outputSiId, outputArea);
  }

  private static PearsOperation.BlockEntry createEntryFrom(
      Integer inputSiId,
      String inputArea,
      Integer outputSiId,
      String outputArea
  ) {
    return new PearsOperation.BlockEntry(
        PearsOperation.EntryType.TRANSFER,
        createBlockFrom(inputSiId, inputArea),
        createBlockFrom(outputSiId, outputArea)
    );
  }

  private static PearsOperation.Block createBlockFrom(Integer siId, String areaKm2) {
    if (siId == null) {
      return null;
    }
    return new PearsOperation.Block(
        siId, null, null, null, null, areaKm2 == null ? null : new BigDecimal(areaKm2));
  }
}
