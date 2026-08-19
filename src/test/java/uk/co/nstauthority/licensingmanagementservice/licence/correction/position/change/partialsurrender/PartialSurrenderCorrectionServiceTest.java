package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;

@ExtendWith(MockitoExtension.class)
class PartialSurrenderCorrectionServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final LicencePosition LICENCE_POSITION = LicencePositionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final UUID FIRST_FEATURE_ID = UUID.randomUUID();
  private static final UUID SECOND_FEATURE_ID = UUID.randomUUID();

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private LicencePositionService licencePositionService;

  @Mock
  private FeatureService featureService;

  @InjectMocks
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @Captor
  private ArgumentCaptor<List<PartialSurrenderOperation>> partialSurrenderOperationCaptor;

  private static LicencePositionCorrection positionCorrection() {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withTargetLicencePosition(null)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(List.of()).build())
        .build();
  }

  private static LicencePositionCorrection addedPositionCorrection() {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withTargetLicencePosition(null)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(List.of()).build())
        .build();
  }

  private static LicencePositionCorrection executedPositionCorrection() {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().withChanges(List.of()).build())
        .build();
  }

  @Test
  void getCommittedPartialSurrender_whenNoPartialSurrenderChange_returnsEmpty() {
    var positionCorrection = positionCorrection();
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of());

    assertThat(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection)).isEmpty();
  }

  @Test
  void getCommittedPartialSurrender_whenPartialSurrenderChange_returnsTheOperation() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .build();
    var positionCorrection = positionCorrection();
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of(operation));

    assertThat(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .contains(operation);
  }

  @Test
  void getCommittedPartialSurrender_whenNoPositionCorrection_returnsEmpty() {
    assertThat(partialSurrenderCorrectionService.getCommittedPartialSurrender(null)).isEmpty();
  }

  @Test
  void getCommittedPartialSurrenderOrThrow_whenStaged_returnsTheOperation() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    var positionCorrection = positionCorrection();
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of(operation));

    assertThat(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionCorrection))
        .isEqualTo(operation);
  }

  @Test
  void getCommittedPartialSurrenderOrThrow_whenNotStaged_throws() {
    var positionCorrection = positionCorrection();
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of());

    assertThatThrownBy(() ->
        partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionCorrection))
        .isInstanceOf(LmsEntityNotFoundException.class)
        .hasMessageContaining(positionCorrection.getId().toString());
  }

  @Test
  void noSurrenderedBlocksArePartial_whenEveryBlockIsFullSurrender_returnsTrue() {
    var positionCorrection = positionCorrection();
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .withBlockSurrenderTypeByFeatureId(Map.of(
            FIRST_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER,
            SECOND_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER))
        .build();
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of(operation));

    assertThat(partialSurrenderCorrectionService.allSurrenderedBlocksAreFull(positionCorrection)).isTrue();
  }

  @Test
  void noSurrenderedBlocksArePartial_whenAnyBlockIsPartialSurrender_returnsFalse() {
    var positionCorrection = positionCorrection();
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .withBlockSurrenderTypeByFeatureId(Map.of(
            FIRST_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER,
            SECOND_FEATURE_ID, BlockSurrenderType.PARTIAL_SURRENDER))
        .build();
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of(operation));

    assertThat(partialSurrenderCorrectionService.allSurrenderedBlocksAreFull(positionCorrection)).isFalse();
  }

  @Test
  void noSurrenderedBlocksArePartial_whenNoSurrenderStaged_returnsFalse() {
    var positionCorrection = positionCorrection();
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of());

    assertThat(partialSurrenderCorrectionService.allSurrenderedBlocksAreFull(positionCorrection)).isFalse();
  }

  @Test
  void commitPartialSurrender_whenFeatureIds_replacesAddChangeWithTheOperation() {
    var positionCorrection = positionCorrection();
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    when(licencePositionCorrectionService.replaceAddChangeFor(
        positionCorrection, PartialSurrenderOperation.class, List.of(operation)))
        .thenReturn(positionCorrection);

    assertThat(partialSurrenderCorrectionService.commitPartialSurrender(positionCorrection, operation))
        .isEqualTo(positionCorrection);
  }

  @Test
  void commitPartialSurrenderForExecutedPosition_replacesAddChangeOnTheResolvedPositionCorrection() {
    var positionCorrection = positionCorrection();
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(positionCorrection);
    when(licencePositionCorrectionService.replaceAddChangeFor(
        positionCorrection, PartialSurrenderOperation.class, List.of(operation)))
        .thenReturn(positionCorrection);

    assertThat(partialSurrenderCorrectionService.commitPartialSurrenderForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, operation))
        .isEqualTo(positionCorrection);
  }

  @Test
  void hasStagedPartialSurrender_whenStaged_returnsTrue() {
    var positionCorrection = positionCorrection();
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of(LicenceOperation.newPartialSurrenderOperation()
            .withFeatureIds(List.of(FIRST_FEATURE_ID))
            .build()));

    assertThat(partialSurrenderCorrectionService.hasStagedPartialSurrender(positionCorrection)).isTrue();
  }

  @Test
  void hasStagedPartialSurrender_whenNotStaged_returnsFalse() {
    var positionCorrection = positionCorrection();
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of());

    assertThat(partialSurrenderCorrectionService.hasStagedPartialSurrender(positionCorrection)).isFalse();
  }

  @Test
  void adjustPartialSurrenderBlocks_whenNoCommittedSurrender_doesNothing() {
    var positionCorrection = executedPositionCorrection();
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of());

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionCorrection);

    verify(licencePositionCorrectionService, never())
        .replaceAddChangeFor(eq(positionCorrection), eq(PartialSurrenderOperation.class), anyList());
  }

  @Test
  void adjustPartialSurrenderBlocks_whenAllBlocksStillSurrenderable_doesNothing() {
    var positionCorrection = executedPositionCorrection();
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .build();
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of(surrender));
    when(licencePositionService.getBlockFeatures(LICENCE_POSITION)).thenReturn(List.of(
        FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build(),
        FeatureTestUtil.builder().withId(SECOND_FEATURE_ID).build()
    ));

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionCorrection);

    verify(licencePositionCorrectionService, never())
        .replaceAddChangeFor(eq(positionCorrection), eq(PartialSurrenderOperation.class), anyList());
  }

  @Test
  void adjustPartialSurrenderBlocks_whenSomeBlocksNoLongerSurrenderable_retainsOnlySurrenderableBlocks() {
    var positionCorrection = executedPositionCorrection();
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .build();
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of(surrender));
    when(licencePositionService.getBlockFeatures(LICENCE_POSITION)).thenReturn(List.of(
        FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build()
    ));

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionCorrection);

    var expected = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(surrender.surrenderDate())
        .withFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    verify(licencePositionCorrectionService)
        .replaceAddChangeFor(positionCorrection, PartialSurrenderOperation.class, List.of(expected));
  }

  @Test
  void adjustPartialSurrenderBlocks_whenSomeBlocksNoLongerSurrenderable_preservesSurrenderTypesForRetainedBlocks() {
    var positionCorrection = executedPositionCorrection();
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .withBlockSurrenderTypeByFeatureId(Map.of(
            FIRST_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER,
            SECOND_FEATURE_ID, BlockSurrenderType.PARTIAL_SURRENDER
        ))
        .build();
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of(surrender));
    when(licencePositionService.getBlockFeatures(LICENCE_POSITION)).thenReturn(List.of(
        FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build()
    ));

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionCorrection);

    var expected = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(surrender.surrenderDate())
        .withFeatureIds(List.of(FIRST_FEATURE_ID))
        .withBlockSurrenderTypeByFeatureId(Map.of(FIRST_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER))
        .build();
    verify(licencePositionCorrectionService)
        .replaceAddChangeFor(positionCorrection, PartialSurrenderOperation.class, List.of(expected));
  }

  @Test
  void adjustPartialSurrenderBlocks_whenNoBlocksStillSurrenderable_removesTheSurrender() {
    var positionCorrection = executedPositionCorrection();
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .build();
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of(surrender));
    when(licencePositionService.getBlockFeatures(LICENCE_POSITION)).thenReturn(List.of(
        FeatureTestUtil.builder().withId(UUID.randomUUID()).build()
    ));

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionCorrection);

    verify(licencePositionCorrectionService)
        .replaceAddChangeFor(positionCorrection, PartialSurrenderOperation.class, List.of());
  }

  @Test
  void getSurrenderableBlockFeatures_whenAddedPosition_returnsTheBlocksHeldAsAtTheEffectiveDate() {
    var positionCorrection = addedPositionCorrection();
    var payload = (CreateLicencePositionPayload) positionCorrection.getPayload();
    var blockFeatures = List.of(FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build());
    when(licencePositionService.getBlockFeaturesOnLicenceOnOrBefore(
        LICENCE, payload.effectiveDate(), payload.effectiveDateOrder()))
        .thenReturn(blockFeatures);

    assertThat(partialSurrenderCorrectionService.getSurrenderableBlockFeatures(positionCorrection))
        .isEqualTo(blockFeatures);
  }

  @Test
  void getSurrenderableBlockFeatures_whenExecutedPosition_returnsTheBlocksHeldByThatPosition() {
    var blockFeatures = List.of(FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build());
    when(licencePositionService.getBlockFeatures(LICENCE_POSITION)).thenReturn(blockFeatures);

    assertThat(partialSurrenderCorrectionService.getSurrenderableBlockFeatures(executedPositionCorrection()))
        .isEqualTo(blockFeatures);
  }

  @Test
  void getSurrenderedBlockFeatureOrThrow_whenStaged_returnsTheFeature() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    var positionCorrection = positionCorrection();
    var feature = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of(operation));
    when(featureService.getFeatureOrThrow(FIRST_FEATURE_ID)).thenReturn(feature);

    assertThat(partialSurrenderCorrectionService.getSurrenderedBlockFeatureOrThrow(positionCorrection, FIRST_FEATURE_ID))
        .isEqualTo(feature);
  }

  @Test
  void getSurrenderedBlockFeatureOrThrow_whenNotStaged_throws() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    var positionCorrection = positionCorrection();
    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(), PartialSurrenderOperation.class))
        .thenReturn(List.of(operation));

    assertThatThrownBy(() ->
        partialSurrenderCorrectionService.getSurrenderedBlockFeatureOrThrow(positionCorrection, SECOND_FEATURE_ID))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void setBlockSurrenderType_replacesTheOperationWithTheTypeSetForTheFeaturePreservingOtherState() {
    var surrenderDate = LocalDate.of(2026, Month.AUGUST, 1);
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(surrenderDate)
        .withFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .withBlockSurrenderTypeByFeatureId(Map.of(FIRST_FEATURE_ID, BlockSurrenderType.PARTIAL_SURRENDER))
        .build();
    var positionCorrection = positionCorrection();

    when(licencePositionCorrectionService.getAddOperationsOfType(
        positionCorrection.getPayload().changes(),
        PartialSurrenderOperation.class
    )).thenReturn(List.of(existingOperation));

    partialSurrenderCorrectionService.setBlockSurrenderType(
        positionCorrection, SECOND_FEATURE_ID,
        BlockSurrenderType.FULL_SURRENDER
    );

    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(surrenderDate)
        .withFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .withBlockSurrenderTypeByFeatureId(Map.of(
            FIRST_FEATURE_ID, BlockSurrenderType.PARTIAL_SURRENDER,
            SECOND_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER
        ))
        .build();

    verify(licencePositionCorrectionService)
        .replaceAddChangeFor(eq(positionCorrection), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());

    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(expectedOperation);
  }
}
