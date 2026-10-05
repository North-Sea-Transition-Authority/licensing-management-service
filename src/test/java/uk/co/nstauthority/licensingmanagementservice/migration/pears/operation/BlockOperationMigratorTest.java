package uk.co.nstauthority.licensingmanagementservice.migration.pears.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.BlockCreateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.BlockEndOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.BlockRedefinitionOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSurrenderOutcome;
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

  private static final int SUBAREA_SI_ID = 739101;
  private static final int SUCCESSOR_SUBAREA_SI_ID = 739102;

  private static final UUID SUBAREA_FEATURE_ID = UUID.randomUUID();
  private static final UUID SUCCESSOR_SUBAREA_FEATURE_ID = UUID.randomUUID();

  private static final Map<Integer, UUID> FEATURE_IDS_BY_SI_ID = Map.of(
      REPLACED_SI_ID, REPLACED_FEATURE_ID,
      SUCCESSOR_SI_ID, SUCCESSOR_FEATURE_ID,
      SUBAREA_SI_ID, SUBAREA_FEATURE_ID,
      SUCCESSOR_SUBAREA_SI_ID, SUCCESSOR_SUBAREA_FEATURE_ID
  );

  @Mock
  private FeatureService featureService;

  private BlockOperationMigrator blockOperationMigrator;

  private MigrationNotes notes;

  @BeforeEach
  void setUp() {
    blockOperationMigrator = new BlockOperationMigrator(new PearsFeatureResolver(featureService));
    notes = new MigrationNotes();
  }

  @Test
  void migrate_whenThePositionHoldsNoBlockOperations_thenNothingIsCarriedAcross() {
    var history = createHistoryFrom(createPositionFrom(
        FIRST_TRANSACTION_ID,
        new PearsOperation.Omitted(createHeaderFrom(1000, 1), "PED_CONSORTIUM_LIST_CREATE")
    ));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertThat(changes).isEmpty();
    verifyNoInteractions(featureService);
  }

  @Test
  void migrate_whenBlocksAreCreated_thenTheyAreCarriedAcrossAsABlockCreation() {
    var history = createHistoryFrom(createPositionFrom(
        FIRST_TRANSACTION_ID,
        createBlockCreateFrom(1000, 1, createEntryFrom(CREATED_SI_ID, "100"))
    ));
    when(featureService.findAllByLegacyIdIn(Set.of(CREATED_SI_ID)))
        .thenReturn(List.of(createFeatureFrom(CREATED_SI_ID, CREATED_FEATURE_ID)));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(MigratedChange.from(
        new PearsPositionKey(FIRST_TRANSACTION_ID),
        new PearsOperationKey(1, 1000),
        List.of(new BlockCreateOperation(List.of(CREATED_FEATURE_ID), Map.of()))
    )));
  }

  @Test
  void migrate_whenACreatedBlockHasNoFeature_thenNothingIsCarriedAcrossAndItIsReported() {
    var history = createHistoryFrom(createPositionFrom(
        FIRST_TRANSACTION_ID,
        createBlockCreateFrom(1000, 1, createEntryFrom(CREATED_SI_ID, "100"))
    ));
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
    var history = createHistoryFrom(createPositionFrom(
        FIRST_TRANSACTION_ID,
        createBlockChangeFrom(1000, 1, createEntryFrom(REPLACED_SI_ID, "100", SUCCESSOR_SI_ID, "60"))
    ));
    when(featureService.findAllByLegacyIdIn(Set.of(REPLACED_SI_ID, SUCCESSOR_SI_ID)))
        .thenReturn(List.of(
            createFeatureFrom(REPLACED_SI_ID, REPLACED_FEATURE_ID),
            createFeatureFrom(SUCCESSOR_SI_ID, SUCCESSOR_FEATURE_ID)
        ));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(MigratedChange.from(
        new PearsPositionKey(FIRST_TRANSACTION_ID),
        new PearsOperationKey(1, 1000),
        List.of(new PartialSurrenderOperation(
            POSITION_DATE,
            List.of(REPLACED_FEATURE_ID),
            Map.of(REPLACED_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
                BlockSurrenderType.PARTIAL_SURRENDER,
                null,
                List.of(),
                List.of(SUCCESSOR_FEATURE_ID)
            ))
        ))
    )));
  }

  @Test
  void migrate_whenABlockChangeGivesUpNoArea_thenItIsCarriedAcrossAsARedefinition() {
    var history = createHistoryFrom(createPositionFrom(
        FIRST_TRANSACTION_ID,
        createBlockChangeFrom(1000, 1, createEntryFrom(REPLACED_SI_ID, "100", SUCCESSOR_SI_ID, "100"))
    ));
    when(featureService.findAllByLegacyIdIn(Set.of(REPLACED_SI_ID, SUCCESSOR_SI_ID)))
        .thenReturn(List.of(
            createFeatureFrom(REPLACED_SI_ID, REPLACED_FEATURE_ID),
            createFeatureFrom(SUCCESSOR_SI_ID, SUCCESSOR_FEATURE_ID)
        ));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(MigratedChange.from(
        new PearsPositionKey(FIRST_TRANSACTION_ID),
        new PearsOperationKey(1, 1000),
        List.of(new BlockRedefinitionOperation(List.of(REPLACED_FEATURE_ID), List.of(SUCCESSOR_FEATURE_ID), List.of(), Map.of()))
    )));
    assertThat(notes.noteCountsByReason())
        .containsEntry("block change that gave up no area, carried across as a redefinition", 1);
  }

  @Test
  void migrate_whenABlockIsCreatedWithSubareas_thenTheSubareasAreHeldAgainstTheBlockTheySitOn() {
    var create = new PearsOperation.BlockCreate(
        createHeaderFrom(1000, 1),
        List.of(createEntryFrom(CREATED_SI_ID, "100")),
        List.of(createSubareaEntryFrom(
            PearsOperation.EntryType.SET,
            null,
            null,
            createSubareaFrom(SUBAREA_SI_ID, CREATED_SI_ID)
        ))
    );
    var history = createHistoryFrom(createPositionFrom(FIRST_TRANSACTION_ID, create));
    when(featureService.findAllByLegacyIdIn(Set.of(CREATED_SI_ID, SUBAREA_SI_ID)))
        .thenReturn(List.of(
            createFeatureFrom(CREATED_SI_ID, CREATED_FEATURE_ID),
            createFeatureFrom(SUBAREA_SI_ID, SUBAREA_FEATURE_ID)
        ));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(MigratedChange.from(
        new PearsPositionKey(FIRST_TRANSACTION_ID),
        new PearsOperationKey(1, 1000),
        List.of(new BlockCreateOperation(
            List.of(CREATED_FEATURE_ID),
            Map.of(CREATED_FEATURE_ID, List.of(new SubareaDetails(SUBAREA_FEATURE_ID, "Subarea", "SA")))
        ))
    )));
  }

  @Test
  void migrate_whenASurrenderCarriesASubareaOntoTheBlockKept_thenTheSubareaIsCroppedToThePartKept() {
    var change = new PearsOperation.BlockChange(
        createHeaderFrom(1000, 1),
        List.of(createEntryFrom(1, REPLACED_SI_ID, "100", SUCCESSOR_SI_ID, "60")),
        List.of(createSubareaEntryFrom(
            PearsOperation.EntryType.TRANSFER,
            1,
            createSubareaFrom(SUBAREA_SI_ID, REPLACED_SI_ID),
            createSubareaFrom(SUCCESSOR_SUBAREA_SI_ID, SUCCESSOR_SI_ID)
        ))
    );
    var history = createHistoryFrom(createPositionFrom(FIRST_TRANSACTION_ID, change));
    when(featureService.findAllByLegacyIdIn(
        Set.of(REPLACED_SI_ID, SUCCESSOR_SI_ID, SUBAREA_SI_ID, SUCCESSOR_SUBAREA_SI_ID)
    )).thenReturn(List.of(
        createFeatureFrom(REPLACED_SI_ID, REPLACED_FEATURE_ID),
        createFeatureFrom(SUCCESSOR_SI_ID, SUCCESSOR_FEATURE_ID),
        createFeatureFrom(SUBAREA_SI_ID, SUBAREA_FEATURE_ID),
        createFeatureFrom(SUCCESSOR_SUBAREA_SI_ID, SUCCESSOR_SUBAREA_FEATURE_ID)
    ));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(MigratedChange.from(
        new PearsPositionKey(FIRST_TRANSACTION_ID),
        new PearsOperationKey(1, 1000),
        List.of(new PartialSurrenderOperation(
            POSITION_DATE,
            List.of(REPLACED_FEATURE_ID),
            Map.of(REPLACED_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
                BlockSurrenderType.PARTIAL_SURRENDER,
                null,
                List.of(),
                List.of(SUCCESSOR_FEATURE_ID),
                Map.of(SUCCESSOR_FEATURE_ID, List.of(SubareaSurrenderOutcome.cropped(
                    new SubareaDetails(SUBAREA_FEATURE_ID, "Subarea", "SA"),
                    new SubareaDetails(SUCCESSOR_SUBAREA_FEATURE_ID, "Subarea", "SA")
                )))
            ))
        ))
    )));
  }

  @Test
  void migrate_whenARedefinitionCarriesASubareaOntoItsSuccessor_thenTheSubareaIsHeldAgainstTheSuccessor() {
    var change = new PearsOperation.BlockChange(
        createHeaderFrom(1000, 1),
        List.of(createEntryFrom(1, REPLACED_SI_ID, "100", SUCCESSOR_SI_ID, "100")),
        List.of(createSubareaEntryFrom(
            PearsOperation.EntryType.TRANSFER,
            1,
            createSubareaFrom(SUBAREA_SI_ID, REPLACED_SI_ID),
            createSubareaFrom(SUCCESSOR_SUBAREA_SI_ID, SUCCESSOR_SI_ID)
        ))
    );
    var history = createHistoryFrom(createPositionFrom(FIRST_TRANSACTION_ID, change));
    when(featureService.findAllByLegacyIdIn(
        Set.of(REPLACED_SI_ID, SUCCESSOR_SI_ID, SUBAREA_SI_ID, SUCCESSOR_SUBAREA_SI_ID)
    )).thenReturn(List.of(
        createFeatureFrom(REPLACED_SI_ID, REPLACED_FEATURE_ID),
        createFeatureFrom(SUCCESSOR_SI_ID, SUCCESSOR_FEATURE_ID),
        createFeatureFrom(SUBAREA_SI_ID, SUBAREA_FEATURE_ID),
        createFeatureFrom(SUCCESSOR_SUBAREA_SI_ID, SUCCESSOR_SUBAREA_FEATURE_ID)
    ));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(MigratedChange.from(
        new PearsPositionKey(FIRST_TRANSACTION_ID),
        new PearsOperationKey(1, 1000),
        List.of(new BlockRedefinitionOperation(
            List.of(REPLACED_FEATURE_ID),
            List.of(SUCCESSOR_FEATURE_ID),
            List.of(new SubareaDetails(SUBAREA_FEATURE_ID, "Subarea", "SA")),
            Map.of(SUCCESSOR_FEATURE_ID, List.of(new SubareaDetails(SUCCESSOR_SUBAREA_FEATURE_ID, "Subarea", "SA")))
        ))
    )));
  }

  @Test
  void migrate_whenASupersededBlockHasNoFeature_thenNothingIsCarriedAcrossAndItIsReported() {
    var history = createHistoryFrom(createPositionFrom(
        FIRST_TRANSACTION_ID,
        createBlockChangeFrom(1000, 1, createEntryFrom(REPLACED_SI_ID, "100", SUCCESSOR_SI_ID, "60"))
    ));
    when(featureService.findAllByLegacyIdIn(Set.of(REPLACED_SI_ID, SUCCESSOR_SI_ID)))
        .thenReturn(List.of(createFeatureFrom(SUCCESSOR_SI_ID, SUCCESSOR_FEATURE_ID)));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertThat(changes).isEmpty();
    assertThat(notes.unmappedCountsByReason())
        .containsEntry("block change whose superseded block could not be resolved", 1);
  }

  @Test
  void migrate_whenOnePositionHoldsSeveralSurrenders_thenTheyFoldIntoOneSurrenderAtTheFirstOfThem() {
    var history = createHistoryFrom(createPositionFrom(
        FIRST_TRANSACTION_ID,
        createBlockChangeFrom(1000, 1, createEntryFrom(REPLACED_SI_ID, "100", SUCCESSOR_SI_ID, "60")),
        createBlockChangeFrom(1001, 2, createEntryFrom(SECOND_REPLACED_SI_ID, "100", SECOND_SUCCESSOR_SI_ID, "60"))
    ));
    when(featureService.findAllByLegacyIdIn(
        Set.of(REPLACED_SI_ID, SUCCESSOR_SI_ID, SECOND_REPLACED_SI_ID, SECOND_SUCCESSOR_SI_ID)
    )).thenReturn(List.of(
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
                    BlockSurrenderType.PARTIAL_SURRENDER,
                    null,
                    List.of(),
                    List.of(SUCCESSOR_FEATURE_ID)
                ),
                SECOND_REPLACED_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
                    BlockSurrenderType.PARTIAL_SURRENDER,
                    null,
                    List.of(),
                    List.of(SECOND_SUCCESSOR_FEATURE_ID)
                )
            )
        ))
    )));
  }

  @Test
  void migrate_whenAPositionCreatesAndSurrendersBlocks_thenBothAreCarriedAcrossInPearsOrder() {
    var history = createHistoryFrom(createPositionFrom(
        FIRST_TRANSACTION_ID,
        createBlockCreateFrom(1000, 1, createEntryFrom(CREATED_SI_ID, "100")),
        createBlockChangeFrom(1001, 2, createEntryFrom(REPLACED_SI_ID, "100", SUCCESSOR_SI_ID, "60"))
    ));
    when(featureService.findAllByLegacyIdIn(Set.of(CREATED_SI_ID, REPLACED_SI_ID, SUCCESSOR_SI_ID)))
        .thenReturn(List.of(
            createFeatureFrom(CREATED_SI_ID, CREATED_FEATURE_ID),
            createFeatureFrom(REPLACED_SI_ID, REPLACED_FEATURE_ID),
            createFeatureFrom(SUCCESSOR_SI_ID, SUCCESSOR_FEATURE_ID)
        ));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(
        MigratedChange.from(
            new PearsPositionKey(FIRST_TRANSACTION_ID),
            new PearsOperationKey(1, 1000),
            List.of(new BlockCreateOperation(List.of(CREATED_FEATURE_ID), Map.of()))
        ),
        MigratedChange.from(
            new PearsPositionKey(FIRST_TRANSACTION_ID),
            new PearsOperationKey(2, 1001),
            List.of(new PartialSurrenderOperation(
                POSITION_DATE,
                List.of(REPLACED_FEATURE_ID),
                Map.of(REPLACED_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
                    BlockSurrenderType.PARTIAL_SURRENDER,
                    null,
                    List.of(),
                    List.of(SUCCESSOR_FEATURE_ID)
                ))
            ))
        )
    ));
  }

  @Test
  void migrate_whenABlockEnds_thenTheBlockIsEnded() {
    var history = createHistoryFrom(createPositionFrom(
        FIRST_TRANSACTION_ID,
        createBlockEndFrom(1000, 1, createEntryFrom(REPLACED_SI_ID, "100", null, null))
    ));
    when(featureService.findAllByLegacyIdIn(Set.of(REPLACED_SI_ID)))
        .thenReturn(List.of(createFeatureFrom(REPLACED_SI_ID, REPLACED_FEATURE_ID)));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(MigratedChange.from(
        new PearsPositionKey(FIRST_TRANSACTION_ID),
        new PearsOperationKey(1, 1000),
        List.of(new BlockEndOperation(List.of(REPLACED_FEATURE_ID), List.of()))
    )));
  }

  @Test
  void migrate_whenABlockEndCannotBeResolved_thenNothingIsCarriedAcrossAndItIsReported() {
    var history = createHistoryFrom(createPositionFrom(
        FIRST_TRANSACTION_ID,
        createBlockEndFrom(1000, 1, createEntryFrom(REPLACED_SI_ID, "100", null, null))
    ));
    when(featureService.findAllByLegacyIdIn(Set.of(REPLACED_SI_ID))).thenReturn(List.of());

    var changes = blockOperationMigrator.migrate(history, notes);

    assertThat(changes).isEmpty();
    assertThat(notes.unmappedCountsByReason())
        .containsEntry("block end whose blocks could not be resolved", 1);
  }

  @Test
  void migrate_whenABlockEndsWithSubareas_thenTheSubareasAreEndedWithIt() {
    var end = new PearsOperation.BlockEnd(
        createHeaderFrom(1000, 1),
        List.of(createEntryFrom(REPLACED_SI_ID, "100", null, null)),
        List.of(createSubareaEntryFrom(
            PearsOperation.EntryType.REMOVE,
            null,
            createSubareaFrom(SUBAREA_SI_ID, REPLACED_SI_ID),
            null
        ))
    );
    var history = createHistoryFrom(createPositionFrom(FIRST_TRANSACTION_ID, end));
    when(featureService.findAllByLegacyIdIn(Set.of(REPLACED_SI_ID, SUBAREA_SI_ID)))
        .thenReturn(List.of(
            createFeatureFrom(REPLACED_SI_ID, REPLACED_FEATURE_ID),
            createFeatureFrom(SUBAREA_SI_ID, SUBAREA_FEATURE_ID)
        ));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(MigratedChange.from(
        new PearsPositionKey(FIRST_TRANSACTION_ID),
        new PearsOperationKey(1, 1000),
        List.of(new BlockEndOperation(
            List.of(REPLACED_FEATURE_ID),
            List.of(new SubareaDetails(SUBAREA_FEATURE_ID, "Subarea", "SA"))
        ))
    )));
  }

  @Test
  void migrate_whenACreatedBlocksSubareaSitsOnAnUnresolvedBlock_thenItIsLeftOffAndReported() {
    var create = new PearsOperation.BlockCreate(
        createHeaderFrom(1000, 1),
        List.of(createEntryFrom(CREATED_SI_ID, "100")),
        List.of(createSubareaEntryFrom(
            PearsOperation.EntryType.SET,
            null,
            null,
            createSubareaFrom(SUBAREA_SI_ID, REPLACED_SI_ID)
        ))
    );
    var history = createHistoryFrom(createPositionFrom(FIRST_TRANSACTION_ID, create));
    when(featureService.findAllByLegacyIdIn(Set.of(CREATED_SI_ID, SUBAREA_SI_ID)))
        .thenReturn(List.of(
            createFeatureFrom(CREATED_SI_ID, CREATED_FEATURE_ID),
            createFeatureFrom(SUBAREA_SI_ID, SUBAREA_FEATURE_ID)
        ));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(MigratedChange.from(
        new PearsPositionKey(FIRST_TRANSACTION_ID),
        new PearsOperationKey(1, 1000),
        List.of(new BlockCreateOperation(List.of(CREATED_FEATURE_ID), Map.of()))
    )));
    assertThat(notes.unmappedCountsByReason())
        .isEqualTo(Map.of("subarea a block arrived with sits on a block that could not be resolved", 1));
  }

  @Test
  void migrate_whenACreatedBlocksSubareaHasNoFeature_thenItIsHeldWithoutOneAndReported() {
    var create = new PearsOperation.BlockCreate(
        createHeaderFrom(1000, 1),
        List.of(createEntryFrom(CREATED_SI_ID, "100")),
        List.of(createSubareaEntryFrom(
            PearsOperation.EntryType.SET,
            null,
            null,
            createSubareaFrom(SUBAREA_SI_ID, CREATED_SI_ID)
        ))
    );
    var history = createHistoryFrom(createPositionFrom(FIRST_TRANSACTION_ID, create));
    when(featureService.findAllByLegacyIdIn(Set.of(CREATED_SI_ID, SUBAREA_SI_ID)))
        .thenReturn(List.of(createFeatureFrom(CREATED_SI_ID, CREATED_FEATURE_ID)));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(MigratedChange.from(
        new PearsPositionKey(FIRST_TRANSACTION_ID),
        new PearsOperationKey(1, 1000),
        List.of(new BlockCreateOperation(
            List.of(CREATED_FEATURE_ID),
            Map.of(CREATED_FEATURE_ID, List.of(new SubareaDetails(null, "Subarea", "SA")))
        ))
    )));
    assertThat(notes.unmappedCountsByReason())
        .isEqualTo(Map.of("subarea a block arrived with could not be resolved", 1));
  }

  @Test
  void migrate_whenABlockIsSplitIntoSuccessorsLeavingItShort_thenItIsOneSurrenderRetainingBothSuccessors() {
    var history = createHistoryFrom(createPositionFrom(
        FIRST_TRANSACTION_ID,
        createBlockChangeFrom(
            1000,
            1,
            createEntryFrom(REPLACED_SI_ID, "100", SUCCESSOR_SI_ID, "60"),
            createEntryFrom(REPLACED_SI_ID, "100", SECOND_SUCCESSOR_SI_ID, "20")
        )
    ));
    when(featureService.findAllByLegacyIdIn(Set.of(REPLACED_SI_ID, SUCCESSOR_SI_ID, SECOND_SUCCESSOR_SI_ID)))
        .thenReturn(List.of(
            createFeatureFrom(REPLACED_SI_ID, REPLACED_FEATURE_ID),
            createFeatureFrom(SUCCESSOR_SI_ID, SUCCESSOR_FEATURE_ID),
            createFeatureFrom(SECOND_SUCCESSOR_SI_ID, SECOND_SUCCESSOR_FEATURE_ID)
        ));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(MigratedChange.from(
        new PearsPositionKey(FIRST_TRANSACTION_ID),
        new PearsOperationKey(1, 1000),
        List.of(new PartialSurrenderOperation(
            POSITION_DATE,
            List.of(REPLACED_FEATURE_ID),
            Map.of(REPLACED_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
                BlockSurrenderType.PARTIAL_SURRENDER,
                null,
                List.of(),
                List.of(SUCCESSOR_FEATURE_ID, SECOND_SUCCESSOR_FEATURE_ID)
            ))
        ))
    )));
  }

  @Test
  void migrate_whenABlockIsSplitIntoSuccessorsCoveringIt_thenItIsOneRedefinitionOntoBothSuccessors() {
    var history = createHistoryFrom(createPositionFrom(
        FIRST_TRANSACTION_ID,
        createBlockChangeFrom(
            1000,
            1,
            createEntryFrom(REPLACED_SI_ID, "100", SUCCESSOR_SI_ID, "60"),
            createEntryFrom(REPLACED_SI_ID, "100", SECOND_SUCCESSOR_SI_ID, "40")
        )
    ));
    when(featureService.findAllByLegacyIdIn(Set.of(REPLACED_SI_ID, SUCCESSOR_SI_ID, SECOND_SUCCESSOR_SI_ID)))
        .thenReturn(List.of(
            createFeatureFrom(REPLACED_SI_ID, REPLACED_FEATURE_ID),
            createFeatureFrom(SUCCESSOR_SI_ID, SUCCESSOR_FEATURE_ID),
            createFeatureFrom(SECOND_SUCCESSOR_SI_ID, SECOND_SUCCESSOR_FEATURE_ID)
        ));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(MigratedChange.from(
        new PearsPositionKey(FIRST_TRANSACTION_ID),
        new PearsOperationKey(1, 1000),
        List.of(new BlockRedefinitionOperation(
            List.of(REPLACED_FEATURE_ID),
            List.of(SUCCESSOR_FEATURE_ID, SECOND_SUCCESSOR_FEATURE_ID),
            List.of(),
            Map.of()
        ))
    )));
    assertThat(notes.noteCountsByReason())
        .isEqualTo(Map.of("block change that gave up no area, carried across as a redefinition", 1));
  }

  @Test
  void migrate_whenASurrendersSuccessorCannotBeResolved_thenItsCarriedSubareasAreReported() {
    var change = new PearsOperation.BlockChange(
        createHeaderFrom(1000, 1),
        List.of(createEntryFrom(1, REPLACED_SI_ID, "100", SUCCESSOR_SI_ID, "60")),
        List.of(createSubareaEntryFrom(
            PearsOperation.EntryType.TRANSFER,
            1,
            createSubareaFrom(SUBAREA_SI_ID, REPLACED_SI_ID),
            createSubareaFrom(SUCCESSOR_SUBAREA_SI_ID, SUCCESSOR_SI_ID)
        ))
    );
    var history = createHistoryFrom(createPositionFrom(FIRST_TRANSACTION_ID, change));
    when(featureService.findAllByLegacyIdIn(
        Set.of(REPLACED_SI_ID, SUCCESSOR_SI_ID, SUBAREA_SI_ID, SUCCESSOR_SUBAREA_SI_ID)
    )).thenReturn(List.of(
        createFeatureFrom(REPLACED_SI_ID, REPLACED_FEATURE_ID),
        createFeatureFrom(SUBAREA_SI_ID, SUBAREA_FEATURE_ID),
        createFeatureFrom(SUCCESSOR_SUBAREA_SI_ID, SUCCESSOR_SUBAREA_FEATURE_ID)
    ));

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(MigratedChange.from(
        new PearsPositionKey(FIRST_TRANSACTION_ID),
        new PearsOperationKey(1, 1000),
        List.of(new PartialSurrenderOperation(
            POSITION_DATE,
            List.of(REPLACED_FEATURE_ID),
            Map.of(REPLACED_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
                BlockSurrenderType.PARTIAL_SURRENDER,
                null,
                List.of(),
                List.of()
            ))
        ))
    )));
    assertThat(notes.unmappedCountsByReason())
        .isEqualTo(Map.of(
            "block the operation left behind could not be resolved", 1,
            "subareas a block change carried onto a successor that could not be resolved", 1
        ));
  }

  static Stream<Arguments> surrenderedSubareas() {
    return Stream.of(
        arguments(
            "carried as it was",
            createSubareaFrom(SUBAREA_SI_ID, REPLACED_SI_ID),
            createSubareaFrom(SUBAREA_SI_ID, SUCCESSOR_SI_ID),
            SubareaSurrenderOutcome.kept(new SubareaDetails(SUBAREA_FEATURE_ID, "Subarea", "SA"))
        ),
        arguments(
            "with no version ended",
            null,
            createSubareaFrom(SUCCESSOR_SUBAREA_SI_ID, SUCCESSOR_SI_ID),
            SubareaSurrenderOutcome.kept(new SubareaDetails(SUCCESSOR_SUBAREA_FEATURE_ID, "Subarea", "SA"))
        ),
        arguments(
            "with no version left behind",
            createSubareaFrom(SUBAREA_SI_ID, REPLACED_SI_ID),
            null,
            SubareaSurrenderOutcome.relinquished(new SubareaDetails(SUBAREA_FEATURE_ID, "Subarea", "SA"))
        )
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("surrenderedSubareas")
  void migrate_whenASurrenderCarriesASubareaOntoTheBlockKept_thenTheOutcomeIsWhatPearsDidToIt(
      String description,
      PearsOperation.Subarea input,
      PearsOperation.Subarea output,
      SubareaSurrenderOutcome expectedOutcome
  ) {
    var change = new PearsOperation.BlockChange(
        createHeaderFrom(1000, 1),
        List.of(createEntryFrom(1, REPLACED_SI_ID, "100", SUCCESSOR_SI_ID, "60")),
        List.of(createSubareaEntryFrom(PearsOperation.EntryType.TRANSFER, 1, input, output))
    );
    var history = createHistoryFrom(createPositionFrom(FIRST_TRANSACTION_ID, change));
    var siIds = new HashSet<>(Set.of(REPLACED_SI_ID, SUCCESSOR_SI_ID));
    Stream.of(input, output)
        .filter(Objects::nonNull)
        .forEach(subarea -> siIds.add(subarea.siId()));
    when(featureService.findAllByLegacyIdIn(siIds)).thenReturn(siIds.stream()
        .map(siId -> createFeatureFrom(siId, FEATURE_IDS_BY_SI_ID.get(siId)))
        .toList());

    var changes = blockOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(MigratedChange.from(
        new PearsPositionKey(FIRST_TRANSACTION_ID),
        new PearsOperationKey(1, 1000),
        List.of(new PartialSurrenderOperation(
            POSITION_DATE,
            List.of(REPLACED_FEATURE_ID),
            Map.of(REPLACED_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
                BlockSurrenderType.PARTIAL_SURRENDER,
                null,
                List.of(),
                List.of(SUCCESSOR_FEATURE_ID),
                Map.of(SUCCESSOR_FEATURE_ID, List.of(expectedOutcome))
            ))
        ))
    )));
  }

  private static void assertChangesMatch(
      List<MigratedChange> changes,
      List<MigratedChange> expected
  ) {
    assertThat(changes)
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes(".*\\.id")
        .isEqualTo(expected);
  }

  private static Feature createFeatureFrom(
      int siId,
      UUID featureId
  ) {
    return FeatureTestUtil.builder()
        .withId(featureId)
        .withLegacyId(siId)
        .build();
  }

  private static PearsLicenceHistory createHistoryFrom(PearsPosition... positions) {
    return new PearsLicenceHistory("P", 8, List.of(positions));
  }

  private static PearsPosition createPositionFrom(
      long transactionId,
      PearsOperation... operations
  ) {
    return new PearsPosition(transactionId, "XPT/1", POSITION_DATE, 1, 1, List.of(operations));
  }

  private static PearsOperation.BlockCreate createBlockCreateFrom(
      long operationId,
      int operationSequence,
      PearsOperation.BlockEntry... entries
  ) {
    return new PearsOperation.BlockCreate(createHeaderFrom(operationId, operationSequence), List.of(entries), List.of());
  }

  private static PearsOperation.BlockChange createBlockChangeFrom(
      long operationId,
      int operationSequence,
      PearsOperation.BlockEntry... entries
  ) {
    return new PearsOperation.BlockChange(createHeaderFrom(operationId, operationSequence), List.of(entries), List.of());
  }

  private static PearsOperation.BlockEnd createBlockEndFrom(
      long operationId,
      int operationSequence,
      PearsOperation.BlockEntry... entries
  ) {
    return new PearsOperation.BlockEnd(createHeaderFrom(operationId, operationSequence), List.of(entries), List.of());
  }

  private static PearsOperation.Header createHeaderFrom(
      long operationId,
      int operationSequence
  ) {
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

  private static PearsOperation.BlockEntry createEntryFrom(
      int outputSiId,
      String outputArea
  ) {
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
        null,
        createBlockFrom(inputSiId, inputArea),
        createBlockFrom(outputSiId, outputArea)
    );
  }

  private static PearsOperation.BlockEntry createEntryFrom(
      int seq,
      int inputSiId,
      String inputArea,
      int outputSiId,
      String outputArea
  ) {
    return new PearsOperation.BlockEntry(
        PearsOperation.EntryType.TRANSFER,
        seq,
        createBlockFrom(inputSiId, inputArea),
        createBlockFrom(outputSiId, outputArea)
    );
  }

  private static PearsOperation.SubareaEntry createSubareaEntryFrom(
      PearsOperation.EntryType type,
      Integer blockEntrySeq,
      PearsOperation.Subarea input,
      PearsOperation.Subarea output
  ) {
    return new PearsOperation.SubareaEntry(type, blockEntrySeq, "SA", input, output);
  }

  private static PearsOperation.Subarea createSubareaFrom(
      int siId,
      int blockSiId
  ) {
    return new PearsOperation.Subarea(siId, "Subarea", blockSiId, null);
  }

  private static PearsOperation.Block createBlockFrom(
      Integer siId,
      String areaKm2
  ) {
    if (siId == null) {
      return null;
    }
    return new PearsOperation.Block(
        siId,
        null,
        null,
        null,
        null,
        areaKm2 == null ? null : new BigDecimal(areaKm2)
    );
  }
}
