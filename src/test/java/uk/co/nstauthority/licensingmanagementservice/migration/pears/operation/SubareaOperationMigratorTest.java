package uk.co.nstauthority.licensingmanagementservice.migration.pears.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
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
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaCreateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaEndOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceHistory;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsPosition;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsOperation;

@ExtendWith(MockitoExtension.class)
class SubareaOperationMigratorTest {

  private static final LocalDate POSITION_DATE = LocalDate.of(2009, Month.MAY, 15);
  private static final long TRANSACTION_ID = 15188;
  private static final long OPERATION_ID = 198088;
  private static final int OPERATION_SEQUENCE = 1;

  private static final int ENDED_BLOCK_SI_ID = 668000;
  private static final int SUCCESSOR_BLOCK_SI_ID = 739000;
  private static final int SECOND_SUCCESSOR_BLOCK_SI_ID = 739100;

  private static final int ENDED_SUBAREA_SI_ID = 739026;
  private static final int CREATED_SUBAREA_SI_ID = 739277;
  private static final int CARRIED_SUBAREA_SI_ID = 739273;

  private static final UUID ENDED_BLOCK_FEATURE_ID = UUID.randomUUID();
  private static final UUID SUCCESSOR_BLOCK_FEATURE_ID = UUID.randomUUID();
  private static final UUID SECOND_SUCCESSOR_BLOCK_FEATURE_ID = UUID.randomUUID();

  private static final UUID ENDED_SUBAREA_FEATURE_ID = UUID.randomUUID();
  private static final UUID CREATED_SUBAREA_FEATURE_ID = UUID.randomUUID();
  private static final UUID CARRIED_SUBAREA_FEATURE_ID = UUID.randomUUID();

  private static final Map<Integer, UUID> FEATURE_IDS_BY_SI_ID = Map.of(
      ENDED_BLOCK_SI_ID, ENDED_BLOCK_FEATURE_ID,
      SUCCESSOR_BLOCK_SI_ID, SUCCESSOR_BLOCK_FEATURE_ID,
      SECOND_SUCCESSOR_BLOCK_SI_ID, SECOND_SUCCESSOR_BLOCK_FEATURE_ID,
      ENDED_SUBAREA_SI_ID, ENDED_SUBAREA_FEATURE_ID,
      CREATED_SUBAREA_SI_ID, CREATED_SUBAREA_FEATURE_ID,
      CARRIED_SUBAREA_SI_ID, CARRIED_SUBAREA_FEATURE_ID
  );

  @Mock
  private FeatureService featureService;

  private SubareaOperationMigrator subareaOperationMigrator;

  private MigrationNotes notes;

  @BeforeEach
  void setUp() {
    subareaOperationMigrator = new SubareaOperationMigrator(new PearsFeatureResolver(featureService));
    notes = new MigrationNotes();
  }

  @Test
  void migrate_whenABlockChangeOnlyCarriesSubareasAcross_thenNoSubareaChangeIsMade() {
    var history = createHistoryFrom(createBlockChangeFrom(createCarriedEntry(1)));

    var changes = subareaOperationMigrator.migrate(history, notes);

    assertThat(changes).isEmpty();
    verifyNoInteractions(featureService);
  }

  @Test
  void migrate_whenABlockChangeAddsASubarea_thenItIsCarriedAcrossAsASubareaCreation() {
    var history = createHistoryFrom(createBlockChangeFrom(createAddedEntry(1, SUCCESSOR_BLOCK_SI_ID)));
    whenFeaturesAreResolved(Set.of(CREATED_SUBAREA_SI_ID, SUCCESSOR_BLOCK_SI_ID));

    var changes = subareaOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(createChangeFrom(new SubareaCreateOperation(
        UUID.randomUUID(),
        SUCCESSOR_BLOCK_FEATURE_ID,
        List.of(new SubareaDetails(CREATED_SUBAREA_FEATURE_ID, "ALL", "A"))
    ))));
  }

  @Test
  void migrate_whenABlockChangeDropsASubarea_thenItIsCarriedAcrossAsASubareaEnding() {
    var history = createHistoryFrom(createBlockChangeFrom(createDroppedEntry(1)));
    whenFeaturesAreResolved(Set.of(ENDED_SUBAREA_SI_ID, ENDED_BLOCK_SI_ID));

    var changes = subareaOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(createChangeFrom(new SubareaEndOperation(
        UUID.randomUUID(),
        ENDED_BLOCK_FEATURE_ID,
        List.of(new SubareaDetails(ENDED_SUBAREA_FEATURE_ID, "DISCOVERY AREA", "NORTH"))
    ))));
  }

  @Test
  void migrate_whenOneBlockEntryDropsASubareaAndAddsAnother_thenTheyAreOneChangeAgainstTheSuccessorBlock() {
    var history = createHistoryFrom(createBlockChangeFrom(
        createDroppedEntry(1),
        createAddedEntry(1, SUCCESSOR_BLOCK_SI_ID)
    ));
    whenFeaturesAreResolved(Set.of(
        ENDED_SUBAREA_SI_ID,
        ENDED_BLOCK_SI_ID,
        CREATED_SUBAREA_SI_ID,
        SUCCESSOR_BLOCK_SI_ID
    ));

    var changes = subareaOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(createChangeFrom(new SubareaOperation(
        UUID.randomUUID(),
        SUCCESSOR_BLOCK_FEATURE_ID,
        List.of(new SubareaDetails(ENDED_SUBAREA_FEATURE_ID, "DISCOVERY AREA", "NORTH")),
        List.of(new SubareaDetails(CREATED_SUBAREA_FEATURE_ID, "ALL", "A"))
    ))));
  }

  @Test
  void migrate_whenABlockIsGivenTwoSuccessors_thenOnlyTheEntriesThatChangedSomethingBecomeChanges() {
    var history = createHistoryFrom(createBlockChangeFrom(
        createDroppedEntry(1),
        createAddedEntry(1, SUCCESSOR_BLOCK_SI_ID),
        createCarriedEntry(2)
    ));
    whenFeaturesAreResolved(Set.of(
        ENDED_SUBAREA_SI_ID,
        ENDED_BLOCK_SI_ID,
        CREATED_SUBAREA_SI_ID,
        SUCCESSOR_BLOCK_SI_ID
    ));

    var changes = subareaOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(createChangeFrom(new SubareaOperation(
        UUID.randomUUID(),
        SUCCESSOR_BLOCK_FEATURE_ID,
        List.of(new SubareaDetails(ENDED_SUBAREA_FEATURE_ID, "DISCOVERY AREA", "NORTH")),
        List.of(new SubareaDetails(CREATED_SUBAREA_FEATURE_ID, "ALL", "A"))
    ))));
  }

  @Test
  void migrate_whenASubareaChangeNamesSubareasOnTwoBlocks_thenOneChangeIsMadePerBlock() {
    var history = createHistoryFrom(new PearsOperation.SubareaChange(createHeader(), List.of(
        createEntryFrom(
            PearsOperation.EntryType.SET,
            null,
            "A",
            null,
            createSubareaFrom(CREATED_SUBAREA_SI_ID, "ALL", SUCCESSOR_BLOCK_SI_ID)
        ),
        createEntryFrom(
            PearsOperation.EntryType.SET,
            null,
            "A",
            null,
            createSubareaFrom(CARRIED_SUBAREA_SI_ID, "ALL", SECOND_SUCCESSOR_BLOCK_SI_ID)
        )
    )));
    whenFeaturesAreResolved(Set.of(
        CREATED_SUBAREA_SI_ID,
        SUCCESSOR_BLOCK_SI_ID,
        CARRIED_SUBAREA_SI_ID,
        SECOND_SUCCESSOR_BLOCK_SI_ID
    ));

    var changes = subareaOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(
        createChangeFrom(new SubareaCreateOperation(
            UUID.randomUUID(),
            SUCCESSOR_BLOCK_FEATURE_ID,
            List.of(new SubareaDetails(CREATED_SUBAREA_FEATURE_ID, "ALL", "A"))
        )),
        createChangeFrom(new SubareaCreateOperation(
            UUID.randomUUID(),
            SECOND_SUCCESSOR_BLOCK_FEATURE_ID,
            List.of(new SubareaDetails(CARRIED_SUBAREA_FEATURE_ID, "ALL", "A"))
        ))
    ));
  }

  static Stream<Arguments> operations() {
    return Stream.of(
        arguments(new PearsOperation.SubareaCreate(createHeader(), List.of()), true),
        arguments(new PearsOperation.SubareaChange(createHeader(), List.of()), true),
        arguments(new PearsOperation.SubareaEnd(createHeader(), List.of()), true),
        arguments(new PearsOperation.BlockCreate(createHeader(), List.of(), List.of()), false),
        arguments(new PearsOperation.BlockChange(createHeader(), List.of(), List.of()), false),
        arguments(new PearsOperation.Omitted(createHeader(), "PED_RETENTION_AREA_CHANGE"), false)
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("operations")
  void supports(
      PearsOperation operation,
      boolean expected
  ) {
    var result = subareaOperationMigrator.supports(operation);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void migrate_whenASubareaOperationNamesNoSubareas_thenNothingIsCarriedAcrossAndItIsReported() {
    var history = createHistoryFrom(new PearsOperation.SubareaEnd(createHeader(), List.of()));

    var changes = subareaOperationMigrator.migrate(history, notes);

    assertThat(changes).isEmpty();
    assertThat(notes.unmappedCountsByReason())
        .isEqualTo(Map.of("subarea operation that named no subareas", 1));
    verifyNoInteractions(featureService);
  }

  @Test
  void migrate_whenASubareaOperationsBlockCannotBeResolved_thenNothingIsCarriedAcrossAndItIsReported() {
    var history = createHistoryFrom(new PearsOperation.SubareaCreate(createHeader(), List.of(
        createEntryFrom(
            PearsOperation.EntryType.SET,
            null,
            "A",
            null,
            createSubareaFrom(CREATED_SUBAREA_SI_ID, "ALL", SUCCESSOR_BLOCK_SI_ID)
        )
    )));
    whenFeaturesAreResolved(Set.of(CREATED_SUBAREA_SI_ID, SUCCESSOR_BLOCK_SI_ID), Set.of(CREATED_SUBAREA_SI_ID));

    var changes = subareaOperationMigrator.migrate(history, notes);

    assertThat(changes).isEmpty();
    assertThat(notes.unmappedCountsByReason())
        .isEqualTo(Map.of("subarea whose block could not be resolved", 1));
    assertThat(notes.noteCountsByReason())
        .isEqualTo(Map.of("PEARS block or subarea has no migrated GIS feature, so no position can hold it", 1));
  }

  @Test
  void migrate_whenABlockChangesAddedSubareaSitsOnAnUnresolvedBlock_thenNothingIsCarriedAcrossAndItIsReported() {
    var history = createHistoryFrom(createBlockChangeFrom(createAddedEntry(1, SUCCESSOR_BLOCK_SI_ID)));
    whenFeaturesAreResolved(Set.of(CREATED_SUBAREA_SI_ID, SUCCESSOR_BLOCK_SI_ID), Set.of(CREATED_SUBAREA_SI_ID));

    var changes = subareaOperationMigrator.migrate(history, notes);

    assertThat(changes).isEmpty();
    assertThat(notes.unmappedCountsByReason())
        .isEqualTo(Map.of("subarea whose block could not be resolved", 1));
  }

  @Test
  void migrate_whenASubareaLeftBehindHasNoFeature_thenItIsStillCarriedAcrossAndReported() {
    var history = createHistoryFrom(createBlockChangeFrom(createAddedEntry(1, SUCCESSOR_BLOCK_SI_ID)));
    whenFeaturesAreResolved(Set.of(CREATED_SUBAREA_SI_ID, SUCCESSOR_BLOCK_SI_ID), Set.of(SUCCESSOR_BLOCK_SI_ID));

    var changes = subareaOperationMigrator.migrate(history, notes);

    assertChangesMatch(changes, List.of(createChangeFrom(new SubareaCreateOperation(
        UUID.randomUUID(),
        SUCCESSOR_BLOCK_FEATURE_ID,
        List.of(new SubareaDetails(null, "ALL", "A"))
    ))));
    assertThat(notes.unmappedCountsByReason())
        .isEqualTo(Map.of("subarea an operation left behind could not be resolved", 1));
  }

  private void whenFeaturesAreResolved(Set<Integer> siIds) {
    whenFeaturesAreResolved(siIds, siIds);
  }

  private void whenFeaturesAreResolved(
      Set<Integer> siIds,
      Set<Integer> resolvedSiIds
  ) {
    when(featureService.findAllByLegacyIdIn(siIds)).thenReturn(resolvedSiIds.stream()
        .map(siId -> createFeatureFrom(siId, FEATURE_IDS_BY_SI_ID.get(siId)))
        .toList());
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

  private static MigratedChange createChangeFrom(LicenceOperation operation) {
    return MigratedChange.from(
        new PearsPositionKey(TRANSACTION_ID),
        new PearsOperationKey(OPERATION_SEQUENCE, OPERATION_ID),
        List.of(operation)
    );
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

  private static PearsLicenceHistory createHistoryFrom(PearsOperation... operations) {
    return new PearsLicenceHistory("P", 1, List.of(
        new PearsPosition(TRANSACTION_ID, "XPT/1", POSITION_DATE, 1, 1, List.of(operations))
    ));
  }

  private static PearsOperation.BlockChange createBlockChangeFrom(PearsOperation.SubareaEntry... entries) {
    return new PearsOperation.BlockChange(createHeader(), List.of(), List.of(entries));
  }

  private static PearsOperation.SubareaEntry createCarriedEntry(int blockEntrySeq) {
    return createEntryFrom(
        PearsOperation.EntryType.TRANSFER,
        blockEntrySeq,
        "HYDE",
        createSubareaFrom(CARRIED_SUBAREA_SI_ID, "HYDE FIELD", ENDED_BLOCK_SI_ID),
        createSubareaFrom(CARRIED_SUBAREA_SI_ID, "HYDE FIELD", SECOND_SUCCESSOR_BLOCK_SI_ID)
    );
  }

  private static PearsOperation.SubareaEntry createDroppedEntry(int blockEntrySeq) {
    return createEntryFrom(
        PearsOperation.EntryType.REMOVE,
        blockEntrySeq,
        "NORTH",
        createSubareaFrom(ENDED_SUBAREA_SI_ID, "DISCOVERY AREA", ENDED_BLOCK_SI_ID),
        null
    );
  }

  private static PearsOperation.SubareaEntry createAddedEntry(
      int blockEntrySeq,
      int blockSiId
  ) {
    return createEntryFrom(
        PearsOperation.EntryType.SET,
        blockEntrySeq,
        "A",
        null,
        createSubareaFrom(CREATED_SUBAREA_SI_ID, "ALL", blockSiId)
    );
  }

  private static PearsOperation.SubareaEntry createEntryFrom(
      PearsOperation.EntryType type,
      Integer blockEntrySeq,
      String shortName,
      PearsOperation.Subarea input,
      PearsOperation.Subarea output
  ) {
    return new PearsOperation.SubareaEntry(type, blockEntrySeq, shortName, input, output);
  }

  private static PearsOperation.Subarea createSubareaFrom(
      int siId,
      String name,
      int blockSiId
  ) {
    return new PearsOperation.Subarea(siId, name, blockSiId, null);
  }

  private static PearsOperation.Header createHeader() {
    return new PearsOperation.Header(
        TRANSACTION_ID,
        "XPT/1",
        POSITION_DATE,
        1,
        OPERATION_ID,
        OPERATION_SEQUENCE,
        PearsOperation.OperationStatus.LIVE,
        null,
        "P"
    );
  }
}
