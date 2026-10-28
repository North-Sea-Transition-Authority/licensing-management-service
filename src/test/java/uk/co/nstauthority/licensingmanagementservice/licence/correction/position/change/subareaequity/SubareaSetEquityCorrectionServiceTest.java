package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subareaequity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;

@ExtendWith(MockitoExtension.class)
class SubareaSetEquityCorrectionServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final LicencePosition LICENCE_POSITION = LicencePositionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final UUID BLOCK_FEATURE_ID = UUID.randomUUID();
  private static final SubareaDetails SUBAREA_A = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
  private static final SubareaDetails SUBAREA_B = new SubareaDetails(UUID.randomUUID(), "Subarea B", "B");
  private static final SubareaOption SUBAREA_A_OPTION = new SubareaOption(
      FeatureTestUtil.builder().withId(BLOCK_FEATURE_ID).build(),
      SUBAREA_A
  );

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @InjectMocks
  private SubareaSetEquityCorrectionService subareaSetEquityCorrectionService;

  @Captor
  private ArgumentCaptor<Predicate<SubareaSetEquityOperation>> operationToReplaceCaptor;

  @Test
  void getCommittedSubareaSetEquityOperations_whenSeveralSubareas_thenOnlyThoseForTheSubarea() {
    var subareaAOperation = subareaSetEquityOperation(SUBAREA_A, 1, 40);
    var subareaBOperation = subareaSetEquityOperation(SUBAREA_B, 2, 60);
    var positionCorrection = positionCorrectionWith(subareaAOperation, subareaBOperation);

    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), SubareaSetEquityOperation.class))
        .thenReturn(List.of(subareaAOperation, subareaBOperation));

    var result = subareaSetEquityCorrectionService
        .getCommittedSubareaSetEquityOperations(positionCorrection, SUBAREA_A_OPTION);

    assertThat(result).containsExactly(subareaAOperation);
  }

  @Test
  void getCommittedSubareaSetEquityOperationsForExecutedPosition_whenUpdateCorrectionExists_thenOperationsForTheSubarea() {
    var subareaAOperation = subareaSetEquityOperation(SUBAREA_A, 1, 40);
    var positionCorrection = positionCorrectionWith(subareaAOperation);

    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionCorrection));
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), SubareaSetEquityOperation.class))
        .thenReturn(List.of(subareaAOperation));

    var result = subareaSetEquityCorrectionService.getCommittedSubareaSetEquityOperationsForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, SUBAREA_A_OPTION);

    assertThat(result).containsExactly(subareaAOperation);
  }

  @Test
  void getCommittedSubareaSetEquityOperationsForExecutedPosition_whenNoUpdateCorrection_thenEmpty() {
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());

    var result = subareaSetEquityCorrectionService.getCommittedSubareaSetEquityOperationsForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, SUBAREA_A_OPTION);

    assertThat(result).isEmpty();
  }

  @Test
  void commitSubareaSetEquity_replacesOnlyTheAddChangeForTheSubarea() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    var operations = List.of(subareaSetEquityOperation(SUBAREA_A, 1, 100));

    subareaSetEquityCorrectionService.commitSubareaSetEquity(positionCorrection, SUBAREA_A_OPTION, operations);

    verify(licencePositionCorrectionService).replaceAddChangeFor(
        eq(positionCorrection),
        eq(SubareaSetEquityOperation.class),
        operationToReplaceCaptor.capture(),
        eq(operations));
    assertThat(operationToReplaceCaptor.getValue())
        .accepts(subareaSetEquityOperation(SUBAREA_A, 2, 50))
        .rejects(subareaSetEquityOperation(SUBAREA_B, 1, 100));
  }

  @Test
  void commitSubareaSetEquity_whenOperationHasOutdatedSubareaDetails_thenCommittedWithCurrentDetails() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    var outdatedSubarea = new SubareaDetails(SUBAREA_A.featureId(), "Old subarea name", SUBAREA_A.shortName());
    var operations = List.of(subareaSetEquityOperation(outdatedSubarea, 1, 100));

    subareaSetEquityCorrectionService.commitSubareaSetEquity(positionCorrection, SUBAREA_A_OPTION, operations);

    verify(licencePositionCorrectionService).replaceAddChangeFor(
        eq(positionCorrection),
        eq(SubareaSetEquityOperation.class),
        any(),
        eq(List.of(subareaSetEquityOperation(SUBAREA_A, 1, 100))));
  }

  @Test
  void commitSubareaSetEquityForExecutedPosition_replacesOnlyTheAddChangeForTheSubareaOnTheUpdateCorrection() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .build();
    var operations = List.of(subareaSetEquityOperation(SUBAREA_A, 1, 100));

    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(positionCorrection);

    subareaSetEquityCorrectionService.commitSubareaSetEquityForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, SUBAREA_A_OPTION, operations);

    verify(licencePositionCorrectionService).replaceAddChangeForExecutedPosition(
        eq(LICENCE_CORRECTION),
        eq(LICENCE_POSITION),
        eq(positionCorrection),
        eq(SubareaSetEquityOperation.class),
        operationToReplaceCaptor.capture(),
        eq(operations));
    assertThat(operationToReplaceCaptor.getValue())
        .accepts(subareaSetEquityOperation(SUBAREA_A, 2, 50))
        .rejects(subareaSetEquityOperation(SUBAREA_B, 1, 100));
  }

  private static SubareaSetEquityOperation subareaSetEquityOperation(SubareaDetails subarea, int transferTo, int equity) {
    return new SubareaSetEquityOperation(BLOCK_FEATURE_ID, subarea, transferTo, BigDecimal.valueOf(equity));
  }

  private static LicencePositionCorrection positionCorrectionWith(SubareaSetEquityOperation... operations) {
    var change = AddChange.buildOperationsChange(List.of(operations), 1);
    var payload = new UpdateLicencePositionPayload(null, null, "TEST-REF", List.of(change));
    return LicencePositionCorrectionTestUtil.newBuilder().withPayload(payload).build();
  }
}