package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationNamePeriods;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.AdministratorChangeForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.explorationoperator.ExplorationOperatorChangeForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator.StorageOperatorChangeForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.ExplorationOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenseeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.ChronologicalPositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionNameService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.OrganisationNameContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChangeTestUtil;

@ExtendWith(MockitoExtension.class)
class OrganisationChangeServiceTest {

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private ChronologicalPositionService chronologicalPositionService;

  @Mock
  private LicencePositionNameService licencePositionNameService;

  @InjectMocks
  private OrganisationChangeService organisationChangeService;

  @Captor
  private ArgumentCaptor<LicencePositionCorrection> licencePositionCorrectionCaptor;

  private static final Integer ORGANISATION_ID = 116;
  private static final Integer UPDATED_ORGANISATION_ID = 999;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final LicencePosition LICENCE_POSITION = LicencePositionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .withId(POSITION_ID)
      .build();
  private static final String ADMIN_NAME = "Administrator org";
  private static final String STORAGE_OPERATOR_NAME = "Storage operator org";
  private static final String EXPLORATION_OPERATOR_NAME = "Exploration operator org";
  private static final OrganisationNameContext ORGANISATION_NAME_CONTEXT = new OrganisationNameContext(Map.of(
      1, new OrganisationNamePeriods(ADMIN_NAME, List.of()),
      2, new OrganisationNamePeriods(STORAGE_OPERATOR_NAME, List.of()),
      3, new OrganisationNamePeriods(EXPLORATION_OPERATOR_NAME, List.of())
  ));

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("addOrganisationChange_whenNoChangeAlreadyExists")
  void addOrganisationChange_whenStagedPositionAndNoPreviousOrganisationChange_appendsOrganisationChange(
      Class<? extends LicenceOperation> operationType,
      LicenceOperation operation
  ) {
    var correction = addPositionCorrectionFor(List.of());

    organisationChangeService.addOrganisationChange(
        new CorrectionPosition.Staged(correction),
        operationType,
        operation
    );

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (CreateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();

    assertThat(payload.licencePositionId()).isEqualTo(POSITION_ID.toString());
    assertThat(payload.changes()).hasSize(1);

    var addOperation = payload.changes().getFirst().operations().getFirst();
    assertThat(addOperation.operation()).isInstanceOf(operationType).isEqualTo(operation);
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("addOrganisationChange_whenChangeAlreadyExists")
  void addOrganisationChange_whenStagedPositionAndOrganisationChangeAlreadyExists_rewritesAddChange(
      Class<? extends LicenceOperation> operationType,
      LicenceOperation previousOperation,
      LicenceOperation currentOperation
  ) {
    var correction = addPositionCorrectionFor(List.of(organisationAddChange(previousOperation)));

    organisationChangeService.addOrganisationChange(
        new CorrectionPosition.Staged(correction),
        operationType,
        currentOperation
    );

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (CreateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes()).hasSize(1);
    var change = payload.changes().getFirst();
    assertThat(change).isInstanceOf(AddChange.class);
    assertThat(change.operations().getFirst().operation()).isEqualTo(currentOperation);
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("addOrganisationChange_whenNoChangeAlreadyExists")
  void addOrganisationChange_whenExecutedPositionAndNoExistingUpdateCorrection_createsNewCorrection(
      Class<? extends LicenceOperation> operationType,
      LicenceOperation operation
  ) {
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());

    organisationChangeService.addOrganisationChange(
        new CorrectionPosition.Executed(LICENCE_CORRECTION, LICENCE_POSITION),
        operationType,
        operation
    );

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var saved = licencePositionCorrectionCaptor.getValue();

    assertThat(saved.getLicenceCorrection()).isEqualTo(LICENCE_CORRECTION);
    assertThat(saved.getChangeType()).isEqualTo(LicencePositionCorrectionChangeType.UPDATE_POSITION);
    assertThat(saved.getTargetLicencePosition()).isEqualTo(LICENCE_POSITION);

    var payload = (UpdateLicencePositionPayload) saved.getPayload();
    assertThat(payload.changes()).hasSize(1);
    assertThat(payload.changes().getFirst().operations().getFirst().operation()).isEqualTo(operation);
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("addOrganisationChange_whenNoChangeAlreadyExists")
  void addOrganisationChange_whenExecutedPositionAndExistingUpdateCorrection_appendsOrganisationChange(
      Class<? extends LicenceOperation> operationType,
      LicenceOperation operation
  ) {
    var existing = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withChanges(List.of()).build())
        .build();

    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(existing));

    organisationChangeService.addOrganisationChange(
        new CorrectionPosition.Executed(LICENCE_CORRECTION, LICENCE_POSITION),
        operationType,
        operation
    );

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes()).hasSize(1);
    assertThat(payload.changes().getFirst().operations().getFirst().operation()).isEqualTo(operation);
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("addOrganisationChange_whenChangeAlreadyExists")
  void addOrganisationChange_whenExecutedPositionAndOrganisationChangeAlreadyExists_rewritesAddChange(
      Class<? extends LicenceOperation> operationType,
      LicenceOperation previousOperation,
      LicenceOperation currentOperation
  ) {
    var existing = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(
            LicencePositionPayload.newUpdateLicencePositionPayload()
                .withChanges(List.of(organisationAddChange(previousOperation)))
                .build())
        .build();

    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(existing));

    organisationChangeService.addOrganisationChange(
        new CorrectionPosition.Executed(LICENCE_CORRECTION, LICENCE_POSITION),
        operationType,
        currentOperation
    );

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes()).hasSize(1);
    var change = payload.changes().getFirst();
    assertThat(change).isInstanceOf(AddChange.class);
    assertThat(change.operations().getFirst().operation()).isEqualTo(currentOperation);
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("populateOrganisationForm")
  void populateOrganisationForm_whenStagedPosition_organisationChangeExists_returnsPopulatedForm(
      Class<? extends LicenceOperation> operationType,
      LicenceOperation operation,
      OrganisationChangeForm newForm,
      OrganisationChangeForm expectedForm
  ) {
    var changes = List.<LicencePositionChangeType>of(
        AddChange.buildOperationsChange(List.of(operation), 1)
    );
    var payload = CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(changes).build();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().withPayload(payload).build();
    assertThat(organisationChangeService.populateOrganisationForm(
        ORGANISATION_ID,
        new CorrectionPosition.Staged(positionCorrection),
        newForm
    )).usingRecursiveComparison().isEqualTo(expectedForm);
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("populateOrganisationForm_returnsEmpty")
  void populateOrganisationForm_whenStagedPosition_organisationChangeDoesNotExists_returnsEmptyForm(
      OrganisationChangeForm newForm,
      OrganisationChangeForm expectedForm
  ) {
    var payload = CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(List.of()).build();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().withPayload(payload).build();
    assertThat(organisationChangeService.populateOrganisationForm(
        ORGANISATION_ID,
        new CorrectionPosition.Staged(positionCorrection),
        newForm
    )).usingRecursiveComparison().isEqualTo(expectedForm);
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("populateOrganisationForm_returnsEmpty")
  void populateOrganisationForm_whenStagedPosition_currentIdIsNull_returnsEmptyForm(
      OrganisationChangeForm newForm,
      OrganisationChangeForm expectedForm
  ) {
    var payload = CreateLicencePositionPayloadTestUtil.newBuilder().build();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().withPayload(payload).build();
    assertThat(organisationChangeService.populateOrganisationForm(
        null,
        new CorrectionPosition.Staged(positionCorrection),
        newForm
    )).usingRecursiveComparison().isEqualTo(expectedForm);
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("populateOrganisationForm")
  void populateOrganisationForm_whenExecutedPosition_hasPendingOrganisationChange_returnsPopulatedForm(
      Class<? extends LicenceOperation> operationType,
      LicenceOperation operation,
      OrganisationChangeForm newForm,
      OrganisationChangeForm expectedForm
  ) {
    when(licencePositionCorrectionService.hasPendingChange(LICENCE_POSITION, LICENCE_CORRECTION, operationType))
        .thenReturn(true);
    assertThat(organisationChangeService.populateOrganisationForm(
        ORGANISATION_ID,
        new CorrectionPosition.Executed(LICENCE_CORRECTION, LICENCE_POSITION),
        newForm
    )).usingRecursiveComparison().isEqualTo(expectedForm);
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("populateOrganisationForm_returnsEmpty")
  void populateOrganisationForm_whenExecutedPosition_hasNoPendingOrganisationChange_returnsEmptyForm(
      OrganisationChangeForm newForm,
      OrganisationChangeForm expectedForm
  ) {
    when(licencePositionCorrectionService.hasPendingChange(LICENCE_POSITION, LICENCE_CORRECTION, newForm.operationType()))
        .thenReturn(false);
    assertThat(organisationChangeService.populateOrganisationForm(
        ORGANISATION_ID,
        new CorrectionPosition.Executed(LICENCE_CORRECTION, LICENCE_POSITION),
        newForm
    )).usingRecursiveComparison().isEqualTo(expectedForm);
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("populateOrganisationForm_returnsEmpty")
  void populateOrganisationForm_whenExecutedPosition_currentIdIsNull_returnsEmptyForm(
      OrganisationChangeForm newForm,
      OrganisationChangeForm expectedForm
  ) {
    assertThat(organisationChangeService.populateOrganisationForm(
        null,
        new CorrectionPosition.Executed(LICENCE_CORRECTION, LICENCE_POSITION),
        newForm
    )).usingRecursiveComparison().isEqualTo(expectedForm);
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("getOrganisationChangeContext")
  void getOrganisationChangeContext_emptyContext(
      Class<? extends LicenceOperation> operationType
  ) {
    var chronologicalPosition = ChronologicalPositionTestUtil.newBuilder().withChanges(List.of()).build();
    when(chronologicalPositionService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, POSITION_ID))
        .thenReturn(List.of(chronologicalPosition));
    when(licencePositionNameService.getOrganisationNameContext(List.of(chronologicalPosition)))
        .thenReturn(ORGANISATION_NAME_CONTEXT);

    var result = organisationChangeService.getOrganisationChangeContext(LICENCE_CORRECTION, POSITION_ID, operationType);

    assertThat(result.currentId()).isNull();
    assertThat(result.previousId()).isNull();
    assertThat(result.currentName()).isEmpty();
    assertThat(result.previousName()).isEmpty();
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("getOrganisationChangeContext")
  void getOrganisationChangeContext_givenNoChronologicalPositions_resolvesNamesWithAllPositions(
      Class<? extends LicenceOperation> operationType,
      Integer organisationNumber,
      String organisationName
  ) {
    var adminPositionChange = PositionChangeTestUtil.newBuilder().withAdministratorOperation(1).build();
    var storagePositionChange = PositionChangeTestUtil.newBuilder().withStorageOperatorOperation(2).build();
    var explorationPositionChange = PositionChangeTestUtil.newBuilder().withExplorationOperation(3).build();
    var chronologicalPosition = ChronologicalPositionTestUtil.newBuilder()
        .withChanges(List.of(adminPositionChange, storagePositionChange, explorationPositionChange))
        .withId(POSITION_ID)
        .build();

    when(chronologicalPositionService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, POSITION_ID))
        .thenReturn(List.of(chronologicalPosition));
    when(licencePositionNameService.getOrganisationNameContext(List.of(chronologicalPosition)))
        .thenReturn(ORGANISATION_NAME_CONTEXT);

    var result = organisationChangeService.getOrganisationChangeContext(LICENCE_CORRECTION, POSITION_ID, operationType);

    assertThat(result.currentId()).isEqualTo(organisationNumber);
    assertThat(result.previousId()).isNull();
    assertThat(result.currentName()).isEqualTo(organisationName);
    assertThat(result.previousName()).isEmpty();
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("getOrganisationChangeContext")
  void getOrganisationChangeContext_givenChronologicalPositions_resolvesNamesWithGivenPositions(
      Class<? extends LicenceOperation> operationType,
      Integer organisationNumber,
      String organisationName
  ) {
    var adminPositionChange = PositionChangeTestUtil.newBuilder().withAdministratorOperation(1).build();
    var storagePositionChange = PositionChangeTestUtil.newBuilder().withStorageOperatorOperation(2).build();
    var explorationPositionChange = PositionChangeTestUtil.newBuilder().withExplorationOperation(3).build();
    var chronologicalPosition = ChronologicalPositionTestUtil.newBuilder()
        .withChanges(List.of(adminPositionChange, storagePositionChange, explorationPositionChange))
        .withId(POSITION_ID)
        .build();

    when(licencePositionNameService.getOrganisationNameContext(List.of(chronologicalPosition)))
        .thenReturn(ORGANISATION_NAME_CONTEXT);

    var result = organisationChangeService.getOrganisationChangeContext(
        LICENCE_CORRECTION,
        POSITION_ID,
        operationType,
        List.of(chronologicalPosition)
    );

    assertThat(result.currentId()).isEqualTo(organisationNumber);
    assertThat(result.previousId()).isNull();
    assertThat(result.currentName()).isEqualTo(organisationName);
    assertThat(result.previousName()).isEmpty();
  }

  @Test
  void getOrganisationChangeContext_throwsIfUnsupportedType() throws IllegalArgumentException {
    assertThatThrownBy(() -> organisationChangeService.getOrganisationChangeContext(
        LICENCE_CORRECTION,
        POSITION_ID,
        LicenseeOperation.class
    ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unsupported operation type for retrieving organisation id: " + LicenseeOperation.class);
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("addOrganisationChange_whenNoChangeAlreadyExists")
  void correctOrganisationChange_whenCorrectionHasNoChangeOfType_appendsUpdateChange(
      Class<? extends LicenceOperation> operationType,
      LicenceOperation operation
  ) {
    var liveChangeId = UUID.randomUUID().toString();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withChanges(List.of()).build())
        .build();
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(positionCorrection);

    organisationChangeService.correctOrganisationChange(
        LICENCE_POSITION,
        LICENCE_CORRECTION,
        operationType,
        liveChangeId,
        operation
    );

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes()).containsExactly(UpdateChangeOperations.buildUpdateChange(liveChangeId, operation));
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("addOrganisationChange_whenChangeAlreadyExists")
  void correctOrganisationChange_whenCorrectionAlreadyCorrectsChangeOfType_rewritesUpdateChange(
      Class<? extends LicenceOperation> operationType,
      LicenceOperation previousOperation,
      LicenceOperation currentOperation
  ) {
    var liveChangeId = UUID.randomUUID().toString();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withChanges(List.of(UpdateChangeOperations.buildUpdateChange(liveChangeId, previousOperation)))
            .build())
        .build();
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(positionCorrection);

    organisationChangeService.correctOrganisationChange(
        LICENCE_POSITION,
        LICENCE_CORRECTION,
        operationType,
        liveChangeId,
        currentOperation
    );

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();
    assertThat(payload.changes())
        .containsExactly(UpdateChangeOperations.buildUpdateChange(liveChangeId, currentOperation));
  }

  private static Stream<Arguments> getOrganisationChangeContext() {
    return Stream.of(
        Arguments.argumentSet("Licence Administrator", AdministratorOperation.class, 1, ADMIN_NAME),
        Arguments.argumentSet("Storage Operator", StorageOperatorOperation.class, 2, STORAGE_OPERATOR_NAME),
        Arguments.argumentSet("Exploration Operator", ExplorationOperatorOperation.class, 3, EXPLORATION_OPERATOR_NAME)
    );
  }
  private static Stream<Arguments> addOrganisationChange_whenNoChangeAlreadyExists() {
    return Stream.of(
        Arguments.argumentSet("Storage Operator",
            StorageOperatorOperation.class,
            storageOperatorOperation(ORGANISATION_ID)),
        Arguments.argumentSet("Exploration Operator",
            ExplorationOperatorOperation.class,
            explorationOperatorOperation(ORGANISATION_ID)),
        Arguments.argumentSet("Licence Administrator",
            AdministratorOperation.class,
            administratorOperation(ORGANISATION_ID))
    );
  }

  private static Stream<Arguments> addOrganisationChange_whenChangeAlreadyExists() {
    return Stream.of(
        Arguments.argumentSet("Storage Operator",
            StorageOperatorOperation.class,
            storageOperatorOperation(ORGANISATION_ID),
            storageOperatorOperation(UPDATED_ORGANISATION_ID)),
        Arguments.argumentSet("Exploration Operator",
            ExplorationOperatorOperation.class,
            explorationOperatorOperation(ORGANISATION_ID),
            explorationOperatorOperation(UPDATED_ORGANISATION_ID)),
        Arguments.argumentSet("Licence Administrator",
            AdministratorOperation.class,
            administratorOperation(ORGANISATION_ID),
            administratorOperation(UPDATED_ORGANISATION_ID))
    );
  }

  private static Stream<Arguments> populateOrganisationForm() {
    return Stream.of(
        Arguments.argumentSet("Storage Operator",
            StorageOperatorOperation.class,
            storageOperatorOperation(ORGANISATION_ID),
            new StorageOperatorChangeForm(),
            new StorageOperatorChangeForm(ORGANISATION_ID.toString())),
        Arguments.argumentSet("Exploration Operator",
            ExplorationOperatorOperation.class,
            explorationOperatorOperation(ORGANISATION_ID),
            new ExplorationOperatorChangeForm(),
            new ExplorationOperatorChangeForm(ORGANISATION_ID.toString())),
        Arguments.argumentSet("Administrator Operator",
            AdministratorOperation.class,
            administratorOperation(ORGANISATION_ID),
            new AdministratorChangeForm(),
            new AdministratorChangeForm(ORGANISATION_ID.toString()))
    );
  }

  private static Stream<Arguments> populateOrganisationForm_returnsEmpty() {
    return Stream.of(
        Arguments.argumentSet("Storage Operator",
            new StorageOperatorChangeForm(),
            new StorageOperatorChangeForm()),
        Arguments.argumentSet("Exploration Operator",
            new ExplorationOperatorChangeForm(),
            new ExplorationOperatorChangeForm()),
        Arguments.argumentSet("Administrator Operator",
            new AdministratorChangeForm(),
            new AdministratorChangeForm())
    );
  }

  private LicencePositionCorrection addPositionCorrectionFor(List<LicencePositionChangeType> changes) {
    var payload = LicencePositionPayload.newCreateLicencePositionPayload()
        .withLicencePositionId(POSITION_ID.toString())
        .withChanges(changes)
        .build();
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withTargetLicencePosition(null)
        .withPayload(payload)
        .build();
  }

  private LicencePositionChangeType organisationAddChange(LicenceOperation licenceOperation) {
    var operation = LicencePositionChangeOperation.newLicencePositionAddOperation()
        .withOperationId(licenceOperation.id())
        .withOperation(licenceOperation)
        .build();
    return LicencePositionChangeType.addChange()
        .withChangeId(UUID.randomUUID().toString())
        .withChangeOrder(1)
        .withOperations(List.of(operation))
        .build();
  }

  private static StorageOperatorOperation storageOperatorOperation(Integer storageOperatorId) {
    return LicenceOperation.newStorageOperatorOperation()
        .withOperator(storageOperatorId)
        .build();
  }

  private static ExplorationOperatorOperation explorationOperatorOperation(Integer explorationOperatorId) {
    return LicenceOperation.newExplorationOperatorOperation()
        .withOperator(explorationOperatorId)
        .build();
  }
  private static AdministratorOperation administratorOperation(Integer administratorId) {
    return LicenceOperation.newAdministratorChange()
        .withOperator(administratorId)
        .build();
  }
}