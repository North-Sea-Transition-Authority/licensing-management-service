package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.BlockCreateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.BlockEndOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaCreateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;

@ExtendWith(MockitoExtension.class)
class LicencePositionValidationServiceTest {

  private static final LocalDate POSITION_DATE = LocalDate.of(2026, Month.JUNE, 18);
  private static final UUID BLOCK_FEATURE_ID = UUID.randomUUID();
  private static final SubareaDetails SUBAREA_A = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
  private static final SubareaDetails SUBAREA_B = new SubareaDetails(UUID.randomUUID(), "Subarea B", "B");

  @Mock
  private PartialSurrenderValidator partialSurrenderValidator;

  @Mock
  private SpatialStateValidator spatialStateValidator;

  @InjectMocks
  private LicencePositionValidationService service;

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {
      LicencePositionChangeType.ADD_CHANGE,
      LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS,
      LicencePositionChangeType.UPDATE_CHANGE_ORDER
  })
  void validate_whenStagedChangeHasPartialSurrender_thenItsErrorsReturnedForTheChange(String changeType) {
    var blockFeatureId = UUID.randomUUID();
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(blockFeatureId))
        .build();
    var surrenderChange = change(changeType, surrender);
    var position = position(
        UUID.randomUUID(),
        POSITION_DATE,
        change(null, blockCreate(blockFeatureId)),
        surrenderChange,
        licenseeChange()
    );
    var positions = List.of(position);
    var corrections = List.of(stagingCorrection(addChange(surrenderChange.changeId())));
    when(partialSurrenderValidator.validate(
        surrender,
        LicencePositionState.EMPTY.withBlock(blockFeatureId, List.of()),
        POSITION_DATE,
        surrenderChange.changeId()
    )).thenReturn(List.of("Surrender error"));

    var errors = service.validate(positions, LicencePositionStateResolver.resolve(positions), corrections, LicenceType.CARBON_STORAGE, LicenceStatusType.EXTANT);

    assertThat(errors).containsExactly(new PositionValidationError(
        position.id(),
        position.positionName(),
        surrenderChange.changeId(),
        LicenceOperation.PARTIAL_SURRENDER,
        "Surrender error"
    ));
  }

  @Test
  void validate_whenCorrectionRemovesPartialSurrender_thenRemovedSurrenderNotValidated() {
    var blockFeatureId = UUID.randomUUID();
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(blockFeatureId))
        .build();
    var removedSurrender = change(LicencePositionChangeType.REMOVE_CHANGE, surrender);
    var position = position(
        UUID.randomUUID(),
        POSITION_DATE,
        change(null, blockCreate(blockFeatureId)),
        removedSurrender
    );
    var positions = List.of(position);
    var corrections = List.of(stagingCorrection(
        LicencePositionChangeType.removeChange().withChangeId(removedSurrender.changeId()).build()));

    service.validate(positions, LicencePositionStateResolver.resolve(positions), corrections, LicenceType.CARBON_STORAGE, LicenceStatusType.EXTANT);

    verifyNoInteractions(partialSurrenderValidator);
  }

  @Test
  void validate_whenStagedSpatialChange_thenEveryNonRemovedChangeCheckedAgainstItsState() {
    var blockFeatureId = UUID.randomUUID();
    var subarea = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
    var liveBlockCreate = new PositionChange(
        UUID.randomUUID().toString(),
        1,
        null,
        List.of(blockCreate(blockFeatureId), LicenceOperation.newLicenseeOperation().withLicenseesToAdd(List.of(888)).build())
    );
    var addedSubareaCreate = change(
        LicencePositionChangeType.ADD_CHANGE,
        new SubareaCreateOperation(blockFeatureId, List.of(subarea))
    );
    var removedBlockEnd = change(
        LicencePositionChangeType.REMOVE_CHANGE,
        new BlockEndOperation(List.of(blockFeatureId), List.of())
    );
    var first = position(UUID.randomUUID(), POSITION_DATE, liveBlockCreate);
    var second = position(UUID.randomUUID(), POSITION_DATE.plusDays(1), addedSubareaCreate, removedBlockEnd);
    var positions = List.of(first, second);
    var states = LicencePositionStateResolver.resolve(positions);
    var firstContext = new PositionValidationContext(
        first, states.currentState(first.id()), states.previousState(first.id()), true, false, LicenceType.CARBON_STORAGE, LicenceStatusType.EXTANT);
    var secondContext = new PositionValidationContext(
        second, states.currentState(second.id()), states.previousState(second.id()), false, true, LicenceType.CARBON_STORAGE, LicenceStatusType.EXTANT);
    var spatialError = PositionValidationError
        .forOperation(secondContext, LicenceOperation.SUBAREA_CREATE, "Spatial error")
        .withChangeId(addedSubareaCreate.changeId());
    when(spatialStateValidator.validate(firstContext, liveBlockCreate, LicencePositionState.EMPTY))
        .thenReturn(List.of());
    var stateAfterFirstPosition = LicencePositionState.EMPTY
        .withBlock(blockFeatureId, List.of())
        .withLicenseeIds(List.of(888), List.of());
    when(spatialStateValidator.validate(secondContext, addedSubareaCreate, stateAfterFirstPosition))
        .thenReturn(List.of(spatialError));

    var corrections = List.of(stagingCorrection(addChange(addedSubareaCreate.changeId())));

    var errors = service.validate(positions, states, corrections, LicenceType.CARBON_STORAGE, LicenceStatusType.EXTANT);

    assertThat(errors).containsExactly(spatialError);
    verify(spatialStateValidator, never()).validate(
        secondContext,
        removedBlockEnd,
        stateAfterFirstPosition.withBlock(blockFeatureId, List.of(subarea))
    );
  }

  @Test
  void validate_whenOnlyAnotherSpatialChangeIsStaged_thenUntouchedSurrendersAreNotValidated() {
    var blockFeatureId = UUID.randomUUID();
    var liveSurrender = change(null, LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(blockFeatureId))
        .build());
    var stagedBlockCreate = change(LicencePositionChangeType.ADD_CHANGE, blockCreate(UUID.randomUUID()));
    var position = position(
        UUID.randomUUID(),
        POSITION_DATE,
        change(null, blockCreate(blockFeatureId)),
        liveSurrender,
        stagedBlockCreate
    );
    var positions = List.of(position);
    var corrections = List.of(stagingCorrection(addChange(stagedBlockCreate.changeId())));

    service.validate(positions, LicencePositionStateResolver.resolve(positions), corrections, LicenceType.CARBON_STORAGE, LicenceStatusType.EXTANT);

    verifyNoInteractions(partialSurrenderValidator);
  }

  @Test
  void validate_whenThePositionOfASurrenderIsMoved_thenTheSurrenderIsValidated() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(UUID.randomUUID()))
        .build();
    var position = ChronologicalPositionTestUtil.live(licencePosition, surrender);
    var positions = List.of(position);
    var corrections = List.of(LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(licencePosition)
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withEffectiveDate(POSITION_DATE).build())
        .build());

    service.validate(positions, LicencePositionStateResolver.resolve(positions), corrections, LicenceType.CARBON_STORAGE, LicenceStatusType.EXTANT);

    verify(partialSurrenderValidator).validate(
        surrender,
        LicencePositionState.EMPTY,
        position.date(),
        position.changes().getFirst().changeId()
    );
  }

  @ParameterizedTest
  @MethodSource("stagedChangeTypes")
  void validate_whenCorrectionStagesChangeWithSpatialOperation_thenSpatialStateChecked(
      Function<String, LicencePositionChangeType> stagedChange
  ) {
    var liveBlockCreate = change(null, blockCreate(UUID.randomUUID()));
    var position = position(UUID.randomUUID(), POSITION_DATE, liveBlockCreate);
    var positions = List.of(position);
    var states = LicencePositionStateResolver.resolve(positions);
    var corrections = List.of(stagingCorrection(stagedChange.apply(liveBlockCreate.changeId())));

    service.validate(positions, states, corrections, LicenceType.CARBON_STORAGE, LicenceStatusType.EXTANT);

    verify(spatialStateValidator).validate(firstPositionContext(position, states), liveBlockCreate, LicencePositionState.EMPTY);
  }



  @ParameterizedTest
  @MethodSource("positionCorrections")
  void validate_whenPositionWithSpatialOperationCorrected_thenSpatialStateChecked(
      LicencePositionCorrectionTestUtil positionCorrection
  ) {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var position = ChronologicalPositionTestUtil.live(licencePosition, blockCreate(UUID.randomUUID()));
    var positions = List.of(position);
    var states = LicencePositionStateResolver.resolve(positions);
    var corrections = List.of(positionCorrection.withTargetLicencePosition(licencePosition).build());

    service.validate(positions, states, corrections, LicenceType.CARBON_STORAGE, LicenceStatusType.EXTANT);

    verify(spatialStateValidator).validate(
        firstPositionContext(position, states),
        position.changes().getFirst(),
        LicencePositionState.EMPTY
    );
  }

  private static Stream<Arguments> positionCorrections() {
    return Stream.of(
        Arguments.of(LicencePositionCorrectionTestUtil.newBuilder()
            .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
            .withPayload(null)),
        Arguments.of(LicencePositionCorrectionTestUtil.newBuilder()
            .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
            .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withEffectiveDate(POSITION_DATE).build())),
        Arguments.of(LicencePositionCorrectionTestUtil.newBuilder()
            .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
            .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withEffectiveDateOrder(2).build()))
    );
  }

  @Test
  void validate_whenNoStagedSpatialChange_thenSpatialChangesNotValidated() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var position = ChronologicalPositionTestUtil.live(licencePosition, blockCreate(UUID.randomUUID()));
    var stagedAdministratorChange = administratorChange();
    var positions = List.of(position, position(UUID.randomUUID(), POSITION_DATE, stagedAdministratorChange));
    var corrections = List.of(LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(licencePosition)
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withCorrectionReference("NEW_REFERENCE")
            .withChanges(List.of(addChange(stagedAdministratorChange.changeId())))
            .build())
        .build());

    service.validate(positions, LicencePositionStateResolver.resolve(positions), corrections, LicenceType.CARBON_STORAGE, LicenceStatusType.EXTANT);

    verifyNoInteractions(spatialStateValidator, partialSurrenderValidator);
  }

  @Test
  void validate_whenNoPositions_returnsNoErrors() {
    assertThat(service.validate(List.of(), LicencePositionStateResolver.resolve(List.of()), List.of(), LicenceType.CARBON_STORAGE, LicenceStatusType.EXTANT)).isEmpty();
  }

  @Test
  void validate_marksOnlyTheFirstPositionAsFirst() {
    // The first position has no admin change, so the flag is true and causes an error
    var first = ChronologicalPositionTestUtil.live(LicencePositionTestUtil.newBuilder().build());
    var second = ChronologicalPositionTestUtil.live(LicencePositionTestUtil.newBuilder().build());

    var chronologicalPositions = List.of(first, second);
    var states = LicencePositionStateResolver.resolve(chronologicalPositions);

    assertThat(service.validate(chronologicalPositions, states, List.of(), LicenceType.LANDWARD_PRODUCTION, LicenceStatusType.EXTANT))
        .extracting(PositionValidationError::positionId, PositionValidationError::message)
        .containsExactly(
            tuple(first.id(), AdministratorPositionRule.FIRST_POSITION_MUST_HAVE_ADMINISTRATOR.getMessage()),
            tuple(first.id(), LicenseePositionRule.FIRST_POSITION_MUST_HAVE_LICENSEE.getMessage()),
            tuple(second.id(), LicenseePositionRule.POSITION_MUST_HAVE_LICENSEES.getMessage()));
  }

  @Test
  void validate_aggregatesErrorsAcrossPositions() {
    var first = LicencePositionTestUtil.newBuilder().build();
    var second = LicencePositionTestUtil.newBuilder().build();
    var chronologicalPositions = List.of(
        ChronologicalPositionTestUtil.live(first),
        ChronologicalPositionTestUtil.live(
            second,
            LicenceOperation.newAdministratorChange().withOperator(2).build(),
            LicenceOperation.newAdministratorChange().withOperator(3).build(),
            LicenceOperation.newLicenseeOperation().withLicenseesToAdd(List.of(2)).build(),
            LicenceOperation.newLicenseeOperation().withLicenseesToAdd(List.of(3)).build())
    );
    var states = LicencePositionStateResolver.resolve(chronologicalPositions);

    assertThat(service.validate(chronologicalPositions, states, List.of(), LicenceType.LANDWARD_PRODUCTION, LicenceStatusType.EXTANT))
        .extracting(PositionValidationError::message)
        .containsExactlyInAnyOrder(AdministratorPositionRule.FIRST_POSITION_MUST_HAVE_ADMINISTRATOR.getMessage(),
            AdministratorPositionRule.ONLY_ONE_ADMINISTRATOR_CHANGE.getMessage(),
            LicenseePositionRule.FIRST_POSITION_MUST_HAVE_LICENSEE.getMessage(),
            LicenseePositionRule.ONLY_ONE_LICENSEE_CHANGE.getMessage());
  }

  @Test
  void validate_whenPositionsShareTransactionEachWithBeneficialInterestChange_flagsEachPosition() {
    var transactionId = UUID.randomUUID();
    var first = position(transactionId, LocalDate.of(2026, Month.JUNE, 18), setEquityChange(1, "100"), licenseeChange());
    var second = position(transactionId, LocalDate.of(2026, Month.JULY, 9), setEquityChange(1, "100"));

    var positions = List.of(first, second);
    var errors = service.validate(positions, LicencePositionStateResolver.resolve(positions), List.of(), LicenceType.CARBON_STORAGE, LicenceStatusType.EXTANT);

    assertThat(errors)
        .extracting(PositionValidationError::positionId, PositionValidationError::message)
        .containsExactly(
            tuple(first.id(), EquityPositionRule.SINGLE_CHANGE_PER_TRANSACTION.getMessage()),
            tuple(second.id(), EquityPositionRule.SINGLE_CHANGE_PER_TRANSACTION.getMessage()));
  }

  @Test
  void validate_whenPositionsHaveDistinctTransactions_doesNotFlagSingleChangeError() {
    var first = position(UUID.randomUUID(), LocalDate.of(2026, Month.JUNE, 18), setEquityChange(1, "100"), licenseeChange());
    var second = position(UUID.randomUUID(), LocalDate.of(2026, Month.JULY, 9), setEquityChange(1, "100"));

    var positions = List.of(first, second);
    var errors = service.validate(positions, LicencePositionStateResolver.resolve(positions), List.of(), LicenceType.CARBON_STORAGE, LicenceStatusType.EXTANT);

    assertThat(errors).isEmpty();
  }

  @Test
  void validate_whenSinglePositionHasTwoBeneficialInterestChanges_flagsSingleChangeError() {
    var position = position(UUID.randomUUID(), LocalDate.of(2026, Month.JUNE, 18),
        setEquityChange(1, "100"), setEquityChange(1, "100"), licenseeChange());

    var positions = List.of(position);
    var errors = service.validate(positions, LicencePositionStateResolver.resolve(positions), List.of(), LicenceType.CARBON_STORAGE, LicenceStatusType.EXTANT);

    assertThat(errors)
        .extracting(PositionValidationError::positionId, PositionValidationError::message)
        .containsExactly(tuple(position.id(), EquityPositionRule.SINGLE_CHANGE_PER_TRANSACTION.getMessage()));
  }

  @Test
  void validate_whenNotCarbonStorageLicence_flagsEachBeneficialInterestChangeAsCarbonStorageOnly() {
    var transactionId = UUID.randomUUID();
    var first = position(transactionId, LocalDate.of(2026, Month.JUNE, 18),
        administratorChange(), setEquityChange(1, "100"), licenseeChange());
    var second = position(transactionId, LocalDate.of(2026, Month.JULY, 9), setEquityChange(1, "100"));

    var positions = List.of(first, second);
    var errors = service.validate(positions, LicencePositionStateResolver.resolve(positions), List.of(), LicenceType.LANDWARD_PRODUCTION, LicenceStatusType.EXTANT);

    assertThat(errors)
        .extracting(PositionValidationError::positionId, PositionValidationError::message)
        .containsExactly(
            tuple(first.id(), EquityOperationRule.CARBON_STORAGE_LICENCE_ONLY.getMessage()),
            tuple(second.id(), EquityOperationRule.CARBON_STORAGE_LICENCE_ONLY.getMessage()));
  }

  @Test
  void validate_delegatesEquityValidationToEachPosition() {
    var position = position(UUID.randomUUID(), LocalDate.of(2026, Month.JUNE, 18), setEquityChange(1, "60"), licenseeChange());

    var positions = List.of(position);
    var errors = service.validate(positions, LicencePositionStateResolver.resolve(positions), List.of(), LicenceType.CARBON_STORAGE, LicenceStatusType.EXTANT);

    assertThat(errors)
        .extracting(PositionValidationError::positionId, PositionValidationError::message)
        .containsExactly(
            tuple(position.id(), EquityPositionRule.BENEFICIAL_INTERESTS_MUST_TOTAL_ONE_HUNDRED.getMessage()));
  }

  @Test
  void validate_whenOriginatingPositionSharesTransaction_returnsEquityErrorThenSingleChangeErrorForOriginOnly() {
    var transactionId = UUID.randomUUID();
    var first = position(transactionId, LocalDate.of(2026, Month.JUNE, 18), setEquityChange(1, "60"), licenseeChange());
    var second = position(transactionId, LocalDate.of(2026, Month.JULY, 9), setEquityChange(1, "60"));

    var positions = List.of(first, second);
    var errors = service.validate(positions, LicencePositionStateResolver.resolve(positions), List.of(), LicenceType.CARBON_STORAGE, LicenceStatusType.EXTANT);

    assertThat(errors)
        .extracting(PositionValidationError::positionId, PositionValidationError::message)
        .containsExactly(
            tuple(first.id(), EquityPositionRule.BENEFICIAL_INTERESTS_MUST_TOTAL_ONE_HUNDRED.getMessage()),
            tuple(first.id(), EquityPositionRule.SINGLE_CHANGE_PER_TRANSACTION.getMessage()),
            tuple(second.id(), EquityPositionRule.SINGLE_CHANGE_PER_TRANSACTION.getMessage()));
  }

  @Test
  void validate_whenInvalidTotalCarriedAcrossPositions_flagsOnlyTheOriginatingPosition() {
    var origin = position(UUID.randomUUID(), LocalDate.of(2026, Month.JUNE, 18), setEquityChange(1, "60"), licenseeChange());
    var carriesForwardFirst = position(UUID.randomUUID(), LocalDate.of(2026, Month.JULY, 9));
    var carriesForwardSecond = position(UUID.randomUUID(), LocalDate.of(2026, Month.JULY, 16));

    var positions = List.of(origin, carriesForwardFirst, carriesForwardSecond);
    var errors = service.validate(positions, LicencePositionStateResolver.resolve(positions), List.of(), LicenceType.CARBON_STORAGE, LicenceStatusType.EXTANT);

    assertThat(errors)
        .extracting(PositionValidationError::positionId, PositionValidationError::message)
        .containsExactly(
            tuple(origin.id(), EquityPositionRule.BENEFICIAL_INTERESTS_MUST_TOTAL_ONE_HUNDRED.getMessage()));
  }

  @Test
  void validate_whenPositionsShareTransactionEachWithChangeToSameSubarea_flagsEachPosition() {
    var transactionId = UUID.randomUUID();
    var first = position(transactionId, LocalDate.of(2026, Month.JUNE, 18), subareaSetEquityChange(SUBAREA_A));
    var second = position(transactionId, LocalDate.of(2026, Month.JULY, 9), subareaSetEquityChange(SUBAREA_A));

    var positions = List.of(subareasCreatedPosition(), first, second);
    var errors = service.validate(positions, LicencePositionStateResolver.resolve(positions), List.of(), LicenceType.SEAWARD_PRODUCTION, LicenceStatusType.EXTANT);

    assertThat(errors).containsExactly(singleSubareaChangeError(first), singleSubareaChangeError(second));
  }

  @Test
  void validate_whenSinglePositionHasTwoChangesToSameSubarea_flagsPosition() {
    var position = position(UUID.randomUUID(), LocalDate.of(2026, Month.JUNE, 18),
        subareaSetEquityChange(SUBAREA_A), subareaSetEquityChange(SUBAREA_A));

    var positions = List.of(subareasCreatedPosition(), position);
    var errors = service.validate(positions, LicencePositionStateResolver.resolve(positions), List.of(), LicenceType.SEAWARD_PRODUCTION, LicenceStatusType.EXTANT);

    assertThat(errors).containsExactly(singleSubareaChangeError(position));
  }

  @Test
  void validate_whenPositionsShareTransactionWithChangesToDifferentSubareas_returnsNoErrors() {
    var transactionId = UUID.randomUUID();
    var first = position(transactionId, LocalDate.of(2026, Month.JUNE, 18), subareaSetEquityChange(SUBAREA_A));
    var second = position(transactionId, LocalDate.of(2026, Month.JULY, 9), subareaSetEquityChange(SUBAREA_B));

    var positions = List.of(subareasCreatedPosition(), first, second);
    var errors = service.validate(positions, LicencePositionStateResolver.resolve(positions), List.of(), LicenceType.SEAWARD_PRODUCTION, LicenceStatusType.EXTANT);

    assertThat(errors).isEmpty();
  }

  @Test
  void validate_whenChangesToSameSubareaOnDifferentTransactions_returnsNoErrors() {
    var first = position(UUID.randomUUID(), LocalDate.of(2026, Month.JUNE, 18), subareaSetEquityChange(SUBAREA_A));
    var second = position(UUID.randomUUID(), LocalDate.of(2026, Month.JULY, 9), subareaSetEquityChange(SUBAREA_A));

    var positions = List.of(subareasCreatedPosition(), first, second);
    var errors = service.validate(positions, LicencePositionStateResolver.resolve(positions), List.of(), LicenceType.SEAWARD_PRODUCTION, LicenceStatusType.EXTANT);

    assertThat(errors).isEmpty();
  }

  private static Stream<Arguments> stagedChangeTypes() {
    return Stream.of(
        Arguments.of((Function<String, LicencePositionChangeType>) LicencePositionValidationServiceTest::addChange),
        Arguments.of((Function<String, LicencePositionChangeType>) changeId ->
            LicencePositionChangeType.updateChangeOperations().withChangeId(changeId).withOperations(List.of()).build()),
        Arguments.of((Function<String, LicencePositionChangeType>) changeId ->
            LicencePositionChangeType.removeChange().withChangeId(changeId).build()),
        Arguments.of((Function<String, LicencePositionChangeType>) changeId ->
            LicencePositionChangeType.updateChangeOrder().withChangeId(changeId).withChangeOrder(2).build())
    );
  }

  private static ChronologicalPosition position(UUID transactionId, LocalDate date, PositionChange... changes) {
    return new ChronologicalPosition(UUID.randomUUID(), transactionId, "REGULATOR_REFERENCE", date, 1, List.of(changes));
  }

  private static PositionChange change(String changeType, LicenceOperation operation) {
    return new PositionChange(UUID.randomUUID().toString(), 1, changeType, List.of(operation));
  }

  private static LicencePositionCorrection stagingCorrection(LicencePositionChangeType stagedChange) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withChanges(List.of(stagedChange)).build())
        .build();
  }

  private static LicencePositionChangeType addChange(String changeId) {
    return LicencePositionChangeType.addChange().withChangeId(changeId).withChangeOrder(1).withOperations(List.of()).build();
  }

  private static PositionValidationContext firstPositionContext(ChronologicalPosition position, ResolvedStates states) {
    return new PositionValidationContext(
        position, states.currentState(position.id()), states.previousState(position.id()), true, true, LicenceType.CARBON_STORAGE, LicenceStatusType.EXTANT);
  }

  private static LicenceOperation blockCreate(UUID blockFeatureId) {
    return LicenceOperation.newBlockCreateOperation().withFeatureIds(List.of(blockFeatureId)).build();
  }

  private static ChronologicalPosition subareasCreatedPosition() {
    var createBlock = new PositionChange(
        UUID.randomUUID().toString(),
        1,
        null,
        List.of(new BlockCreateOperation(
            List.of(BLOCK_FEATURE_ID),
            Map.of(BLOCK_FEATURE_ID, List.of(SUBAREA_A, SUBAREA_B))
        ))
    );
    return position(UUID.randomUUID(), LocalDate.of(2026, Month.MAY, 1), administratorChange(), licenseeChange(), createBlock);
  }

  private static PositionChange administratorChange() {
    return new PositionChange(
        UUID.randomUUID().toString(),
        0,
        null,
        List.of(LicenceOperation.newAdministratorChange().withOperator(999).build())
    );
  }

  private static PositionChange licenseeChange() {
    return new PositionChange(
        UUID.randomUUID().toString(),
        0,
        null,
        List.of(LicenceOperation.newLicenseeOperation().withLicenseesToAdd(List.of(888)).build())
    );
  }

  private static PositionChange setEquityChange(int organisationId, String equity) {
    return new PositionChange(
        UUID.randomUUID().toString(),
        1,
        null,
        List.of(LicenceOperation.newSetEquityOperation()
            .withTransferTo(organisationId)
            .withEquity(new BigDecimal(equity))
            .build())
    );
  }

  private static PositionChange subareaSetEquityChange(SubareaDetails subarea) {
    return new PositionChange(
        UUID.randomUUID().toString(),
        1,
        null,
        List.of(new SubareaSetEquityOperation(BLOCK_FEATURE_ID, subarea, 1, new BigDecimal("100")))
    );
  }

  private static PositionValidationError singleSubareaChangeError(ChronologicalPosition position) {
    return new PositionValidationError(
        position.id(),
        position.positionName(),
        null,
        null,
        SubareaEquityPositionRule.SINGLE_CHANGE_PER_SUBAREA_PER_TRANSACTION.getMessage()
    );
  }
}