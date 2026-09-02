package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionMoveDirection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOrder;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;

@ExtendWith(MockitoExtension.class)
class CorrectChangeOrderServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final UUID CHANGE_A = UUID.randomUUID();
  private static final UUID CHANGE_B = UUID.randomUUID();
  private static final UUID CHANGE_C = UUID.randomUUID();

  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LICENCE).build();

  @Mock
  private LicencePositionViewService licencePositionViewService;

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private LicencePositionService licencePositionService;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @InjectMocks
  private CorrectChangeOrderService correctChangeOrderService;

  @Test
  void getOrderableChanges_mapsLabelsToOrderableChangesPreservingOrder() {
    var labels = new LinkedHashMap<UUID, String>();
    labels.put(CHANGE_A, "Licence administrator change");
    labels.put(CHANGE_B, "Set equity change");
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID)).thenReturn(labels);

    var orderableChanges = correctChangeOrderService.getOrderableChanges(CORRECTION, POSITION_ID);

    assertThat(orderableChanges)
        .containsExactly(
            new OrderableChange(CHANGE_A, "Licence administrator change"),
            new OrderableChange(CHANGE_B, "Set equity change"));
  }

  @Test
  void correctChangeOrder_whenMovedChangeNotOnPosition_throws() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_B));

    assertThatThrownBy(() -> correctChangeOrderService.correctChangeOrder(
        CORRECTION, POSITION_ID, CHANGE_A, CHANGE_B, PositionMoveDirection.BEFORE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Cannot move change %s as it is not on licence position %s".formatted(CHANGE_A, POSITION_ID));
  }

  @Test
  void correctChangeOrder_whenTargetChangeNotOnPosition_throws() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_A));

    assertThatThrownBy(() -> correctChangeOrderService.correctChangeOrder(
        CORRECTION, POSITION_ID, CHANGE_A, CHANGE_B, PositionMoveDirection.BEFORE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "Cannot move change %s relative to change %s as they are not on the same licence position %s"
                .formatted(CHANGE_A, CHANGE_B, POSITION_ID));
  }

  @Test
  void correctChangeOrder_onExecutedPosition_writesUpdateChangeOrdersOnlyForMovedChanges() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_A, CHANGE_B, CHANGE_C));

    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    when(licencePositionCorrectionService.findFirstAddedPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(Optional.empty());
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionChangeService.findByLicencePositionId(POSITION_ID)).thenReturn(List.of(
        liveChange(CHANGE_A, 1),
        liveChange(CHANGE_B, 2),
        liveChange(CHANGE_C, 3)));
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(CORRECTION, position))
        .thenReturn(updatePositionCorrection(List.of()));

    correctChangeOrderService.correctChangeOrder(
        CORRECTION, POSITION_ID, CHANGE_B, CHANGE_C, PositionMoveDirection.AFTER);

    var captor = ArgumentCaptor.forClass(LicencePositionCorrection.class);
    verify(licencePositionCorrectionService).save(captor.capture());

    assertThat(captor.getValue().getPayload().changes())
        .containsExactly(
            new UpdateChangeOrder(CHANGE_C.toString(), 2),
            new UpdateChangeOrder(CHANGE_B.toString(), 3));
  }

  @Test
  void correctChangeOrder_whenMovedBackToItsExecutedOrder_discardsThePositionCorrection() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_B, CHANGE_A));

    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    when(licencePositionCorrectionService.findFirstAddedPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(Optional.empty());
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionChangeService.findByLicencePositionId(POSITION_ID)).thenReturn(List.of(
        liveChange(CHANGE_A, 1),
        liveChange(CHANGE_B, 2)));

    var positionCorrection = updatePositionCorrection(List.of(
        new UpdateChangeOrder(CHANGE_B.toString(), 1),
        new UpdateChangeOrder(CHANGE_A.toString(), 2)));
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(CORRECTION, position))
        .thenReturn(positionCorrection);

    correctChangeOrderService.correctChangeOrder(
        CORRECTION, POSITION_ID, CHANGE_A, CHANGE_B, PositionMoveDirection.BEFORE);

    verify(licencePositionCorrectionService).delete(positionCorrection);
    verify(licencePositionCorrectionService, never()).save(any());
  }

  @Test
  void correctChangeOrder_onAddedPosition_rewritesAddChangeOrders() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_A, CHANGE_B));

    var addedCorrection = addedPositionCorrection(List.of(
        new AddChange(CHANGE_A.toString(), 1, List.of()),
        new AddChange(CHANGE_B.toString(), 2, List.of())));
    when(licencePositionCorrectionService.findFirstAddedPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(Optional.of(addedCorrection));

    correctChangeOrderService.correctChangeOrder(
        CORRECTION, POSITION_ID, CHANGE_A, CHANGE_B, PositionMoveDirection.AFTER);

    var captor = ArgumentCaptor.forClass(LicencePositionCorrection.class);
    verify(licencePositionCorrectionService).save(captor.capture());

    assertThat(captor.getValue().getPayload().changes())
        .containsExactly(
            new AddChange(CHANGE_A.toString(), 2, List.of()),
            new AddChange(CHANGE_B.toString(), 1, List.of()));
  }

  @Test
  void correctChangeOrder_whenTwoSubareaChangesOnDifferentBlocks_reordersThemIndependently() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_A, CHANGE_B));

    var subareaChangeA = new AddChange(CHANGE_A.toString(), 1, List.of(subareaAddOperation(UUID.randomUUID())));
    var subareaChangeB = new AddChange(CHANGE_B.toString(), 2, List.of(subareaAddOperation(UUID.randomUUID())));
    var addedCorrection = addedPositionCorrection(List.of(subareaChangeA, subareaChangeB));
    when(licencePositionCorrectionService.findFirstAddedPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(Optional.of(addedCorrection));

    correctChangeOrderService.correctChangeOrder(
        CORRECTION, POSITION_ID, CHANGE_A, CHANGE_B, PositionMoveDirection.AFTER);

    var captor = ArgumentCaptor.forClass(LicencePositionCorrection.class);
    verify(licencePositionCorrectionService).save(captor.capture());

    assertThat(captor.getValue().getPayload().changes())
        .containsExactly(
            new AddChange(CHANGE_A.toString(), 2, subareaChangeA.operations()),
            new AddChange(CHANGE_B.toString(), 1, subareaChangeB.operations()));
  }

  private static LinkedHashMap<UUID, String> orderableLabels(UUID... changeIds) {
    var labels = new LinkedHashMap<UUID, String>();
    for (var changeId : changeIds) {
      labels.put(changeId, "Change " + changeId);
    }
    return labels;
  }

  private static LicencePositionChangeOperation subareaAddOperation(UUID featureId) {
    var operation = new SubareaOperation(featureId);
    return LicencePositionChangeOperation.newLicencePositionAddOperation()
        .withOperationId(operation.id())
        .withOperation(operation)
        .build();
  }

  private static LicencePositionChange liveChange(UUID id, int order) {
    return LicencePositionChangeTestUtil.newBuilder().withId(id).withChangeOrder(order).build();
  }

  private static LicencePositionCorrection updatePositionCorrection(List<LicencePositionChangeType> changes) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withCorrectionReference("CORRECTION-REF")
            .withChanges(changes)
            .build())
        .build();
  }

  private static LicencePositionCorrection addedPositionCorrection(List<LicencePositionChangeType> changes) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(POSITION_ID.toString())
            .withChanges(changes)
            .build())
        .build();
  }
}
