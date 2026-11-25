package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.AdministratorChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity.EquityChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee.LicenseeChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator.StorageOperatorChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.SubareaChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.VisibleLicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;

@ExtendWith(MockitoExtension.class)
class ChangeActionServiceTest {

  private static final String CHANGE_ID = UUID.randomUUID().toString();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder().build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder().build();
  private static final LicencePositionStagedPayload POSITION_STAGED_PAYLOAD = LicencePositionStagedPayloadTestUtil.newBuilder().build();
  private static final LicenceOperation HIDDEN_OPERATION = LicenceOperation.newBlockEndOperation().withEndedFeatureIds(List.of(UUID.randomUUID())).build();
  private static final SubareaOperation SUBAREA_OPERATION = LicenceOperation.newSubAreaOperation().withBlockFeatureId(UUID.randomUUID()).build();
  private static final AdministratorOperation ADMINISTRATOR_OPERATION = LicenceOperation.newAdministratorChange().withOperator(1).build();
  private static final LicencePositionChangeType STAGED_CHANGE = LicencePositionChangeType.removeChange().withChangeId(CHANGE_ID).build();

  @Mock
  private LicencePositionStagedPayloadService licencePositionStagedPayloadService;

  @Mock
  private SubareaChangeActionHandler subareaHandler;

  private ChangeActionService changeActionService;

  @BeforeEach
  void setUp() {
    changeActionService = new ChangeActionService(
        licencePositionStagedPayloadService,
        List.of(
            mock(AdministratorChangeActionHandler.class),
            mock(EquityChangeActionHandler.class),
            mock(LicenseeChangeActionHandler.class),
            mock(PartialSurrenderChangeActionHandler.class),
            mock(StorageOperatorChangeActionHandler.class),
            subareaHandler
        )
    );
  }

  @Test
  void getOperationOfLiveChange_whenFirstOperationIsHidden_thenReturnsFirstVisibleOperation() {
    var liveChange = liveChangeWith(HIDDEN_OPERATION, SUBAREA_OPERATION, ADMINISTRATOR_OPERATION);

    var result = changeActionService.getOperationOfLiveChange(liveChange);

    assertThat(result).isEqualTo(SUBAREA_OPERATION);
  }

  @Test
  void getOperationOfLiveChange_whenNoOperationIsVisible_thenNotFound() {
    var liveChange = liveChangeWith(HIDDEN_OPERATION);

    assertThatThrownBy(() -> changeActionService.getOperationOfLiveChange(liveChange))
        .isInstanceOfSatisfying(ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void getOperationOfLiveChange_whenFirstVisibleOperationHasNoHandler_thenNotFound() {
    var liveChange = liveChangeWith(
        LicenceOperation.newExplorationOperatorOperation().withOperator(1).build(),
        ADMINISTRATOR_OPERATION
    );

    assertThatThrownBy(() -> changeActionService.getOperationOfLiveChange(liveChange))
        .isInstanceOfSatisfying(ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void getOperationOfStagedChange() {
    givenStagedChangeWith(HIDDEN_OPERATION, SUBAREA_OPERATION);

    var result = changeActionService.getOperationOfStagedChange(POSITION_STAGED_PAYLOAD, CHANGE_ID);

    assertThat(result).isEqualTo(SUBAREA_OPERATION);
  }

  @ParameterizedTest
  @MethodSource("operationsAndTheirHandlerTypes")
  void getHandler_whenOperationNamesHandlerBean_thenReturnsIt(
      VisibleLicenceOperation operation,
      Class<? extends ChangeActionHandler> expectedHandlerType
  ) {
    var result = changeActionService.getHandler(operation);

    assertThat(result).isInstanceOf(expectedHandlerType);
  }

  @Test
  void getHandler_whenNamedHandlerIsNotABean_thenThrows() {
    var serviceWithoutHandlers = new ChangeActionService(licencePositionStagedPayloadService, List.of());

    assertThatThrownBy(() -> serviceWithoutHandlers.getHandler(SUBAREA_OPERATION))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SubareaChangeActionHandler");
  }

  @Test
  void removeLiveChange_whenOperationIsAtMostOnePerPosition_thenDropsItsStagedChangesBeforeStagingRemoval() {
    var liveChange = liveChangeWith(ADMINISTRATOR_OPERATION);

    changeActionService.removeLiveChange(CORRECTION, POSITION, liveChange);

    var inOrder = inOrder(licencePositionStagedPayloadService);
    inOrder.verify(licencePositionStagedPayloadService)
        .dropStagedChangesOf(CORRECTION, POSITION, AdministratorOperation.class);
    inOrder.verify(licencePositionStagedPayloadService).stageRemovalOfExecutedChange(CORRECTION, POSITION, CHANGE_ID);
  }

  @Test
  void removeLiveChange_whenOperationCanRepeatOnPosition_thenOnlyStagesRemoval() {
    var liveChange = liveChangeWith(SUBAREA_OPERATION);

    changeActionService.removeLiveChange(CORRECTION, POSITION, liveChange);

    verify(licencePositionStagedPayloadService, never()).dropStagedChangesOf(any(), any(), any());
    verify(licencePositionStagedPayloadService).stageRemovalOfExecutedChange(CORRECTION, POSITION, CHANGE_ID);
  }

  @Test
  void undoStagedChange() {
    givenStagedChangeWith(SUBAREA_OPERATION);

    changeActionService.undoStagedChange(POSITION_STAGED_PAYLOAD, CHANGE_ID);

    var inOrder = inOrder(subareaHandler, licencePositionStagedPayloadService);
    inOrder.verify(subareaHandler).beforeDroppingStagedChange(POSITION_STAGED_PAYLOAD, STAGED_CHANGE);
    inOrder.verify(licencePositionStagedPayloadService).dropStagedChange(POSITION_STAGED_PAYLOAD, CHANGE_ID);
  }

  private static Stream<Arguments> operationsAndTheirHandlerTypes() {
    return Stream.of(
        Arguments.of(ADMINISTRATOR_OPERATION, AdministratorChangeActionHandler.class),
        Arguments.of(LicenceOperation.newLicenseeOperation().build(), LicenseeChangeActionHandler.class),
        Arguments.of(
            LicenceOperation.newPartialSurrenderOperation().withSurrenderedFeatureIds(List.of(UUID.randomUUID())).build(),
            PartialSurrenderChangeActionHandler.class
        ),
        Arguments.of(
            LicenceOperation.newSetEquityOperation().withTransferTo(1).withEquity(BigDecimal.TEN).build(),
            EquityChangeActionHandler.class
        ),
        Arguments.of(
            LicenceOperation.newStorageOperatorOperation().withOperator(1).build(),
            StorageOperatorChangeActionHandler.class
        ),
        Arguments.of(SUBAREA_OPERATION, SubareaChangeActionHandler.class),
        Arguments.of(
            LicenceOperation.newSubareaSetEquityOperation()
                .withBlockFeatureId(UUID.randomUUID())
                .withSubarea(new SubareaDetails(UUID.randomUUID(), "Subarea", "A"))
                .withTransferTo(1)
                .withEquity(BigDecimal.TEN)
                .build(),
            EquityChangeActionHandler.class
        ),
        Arguments.of(
            LicenceOperation.newTransferEquityOperation()
                .withTransferFrom(1)
                .withTransferTo(2)
                .withEquity(BigDecimal.TEN)
                .build(),
            EquityChangeActionHandler.class
        )
    );
  }

  private static LicencePositionChange liveChangeWith(LicenceOperation... operations) {
    return LicencePositionChangeTestUtil.newBuilder()
        .withId(UUID.fromString(CHANGE_ID))
        .withOperations(List.of(operations))
        .build();
  }

  private void givenStagedChangeWith(LicenceOperation... operations) {
    when(licencePositionStagedPayloadService.getStagedChangeOrThrow(POSITION_STAGED_PAYLOAD, CHANGE_ID)).thenReturn(STAGED_CHANGE);
    when(licencePositionStagedPayloadService.resolveStagedChangeOperations(STAGED_CHANGE)).thenReturn(List.of(operations));
  }
}
