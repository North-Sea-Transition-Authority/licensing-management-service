package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.operator.CropOperatorService;
import uk.co.fivium.gisframework.operator.CropResultDto;
import uk.co.fivium.grpc.gis.IntersectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSurrenderOutcome;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;

@ExtendWith(MockitoExtension.class)
class PartialSurrenderSubareaServiceTest {

  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LicenceTestUtil.builder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build())
      .build();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final String CHANGE_ID = "change-id";
  private static final UUID BLOCK_FEATURE_ID = UUID.randomUUID();
  private static final UUID SURRENDERED_PART_ID = UUID.randomUUID();

  private static final Feature INSIDE_SUBAREA_FEATURE = FeatureTestUtil.subareaFeature(UUID.randomUUID(), "30/1a");
  private static final Feature OUTSIDE_SUBAREA_FEATURE = FeatureTestUtil.subareaFeature(UUID.randomUUID(), "30/1b");
  private static final Feature STRADDLING_SUBAREA_FEATURE = FeatureTestUtil.subareaFeature(UUID.randomUUID(), "30/1c");
  private static final Feature CROPPED_SUBAREA_FEATURE = FeatureTestUtil.subareaFeature(UUID.randomUUID(), "30/1c");
  private static final Feature RETAINED_FEATURE = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 1);

  private static final SubareaDetails INSIDE_SUBAREA = new SubareaDetails(INSIDE_SUBAREA_FEATURE.getId(), "Subarea A", "A");
  private static final SubareaDetails OUTSIDE_SUBAREA = new SubareaDetails(OUTSIDE_SUBAREA_FEATURE.getId(), "Subarea B", "B");
  private static final SubareaDetails STRADDLING_SUBAREA =
      new SubareaDetails(STRADDLING_SUBAREA_FEATURE.getId(), "Subarea C", "C");
  private static final SubareaDetails UNSCRIBED_SUBAREA = new SubareaDetails(null, "Unscribed", "U");

  @Mock
  private LicencePositionSpatialService licencePositionSpatialService;

  @Mock
  private FeatureService featureService;

  @Mock
  private CropOperatorService cropOperatorService;

  @InjectMocks
  private PartialSurrenderSubareaService partialSurrenderSubareaService;

  @ParameterizedTest
  @MethodSource("surrendersKeepingNoPart")
  void processSubareas_whenNoPartIsKept_thenNoSubareasRecorded(
      SurrenderDetails surrenderDetails,
      List<UUID> retainedFeatureIds
  ) {
    var result = partialSurrenderSubareaService.processSubareas(
        LICENCE_CORRECTION, POSITION_ID, CHANGE_ID, BLOCK_FEATURE_ID, surrenderDetails, retainedFeatureIds, null);

    assertThat(result).isEqualTo(surrenderDetails.withSubareas(Map.of()));
    verifyNoInteractions(licencePositionSpatialService, featureService, cropOperatorService);
  }

  private static Stream<Arguments> surrendersKeepingNoPart() {
    return Stream.of(
        Arguments.of(
            new SurrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, UUID.randomUUID(), List.of())
                .withSubareas(Map.of(RETAINED_FEATURE.getId(), List.of(SubareaSurrenderOutcome.kept(INSIDE_SUBAREA)))),
            List.of(RETAINED_FEATURE.getId())),
        Arguments.of(
            new SurrenderDetails(BlockSurrenderType.FULL_SURRENDER, null, List.of(BLOCK_FEATURE_ID)),
            List.of()),
        Arguments.of(partialSurrenderDetails(), List.of())
    );
  }

  @Test
  void processSubareas_whenPartialSurrender_thenEachSubareaIsKeptCroppedOrRelinquishedByWhereItLiesOnThePart() {
    var surrenderDetails = partialSurrenderDetails();
    givenSubareasGoingIntoTheSurrender(INSIDE_SUBAREA, OUTSIDE_SUBAREA, STRADDLING_SUBAREA, UNSCRIBED_SUBAREA);
    var retainedFeatureIds = List.of(RETAINED_FEATURE.getId());
    var subareaFeatureIds = Set.of(
        INSIDE_SUBAREA_FEATURE.getId(), OUTSIDE_SUBAREA_FEATURE.getId(), STRADDLING_SUBAREA_FEATURE.getId());
    var subareaFeatures = List.of(INSIDE_SUBAREA_FEATURE, OUTSIDE_SUBAREA_FEATURE, STRADDLING_SUBAREA_FEATURE);
    when(featureService.getFeaturesByIds(retainedFeatureIds)).thenReturn(List.of(RETAINED_FEATURE));
    when(featureService.getFeaturesByIds(subareaFeatureIds)).thenReturn(subareaFeatures);
    when(cropOperatorService.cropFeaturesToBoundary(RETAINED_FEATURE, subareaFeatures)).thenReturn(List.of(
        new CropResultDto(INSIDE_SUBAREA_FEATURE.getId(), IntersectionStatus.FULLY_INSIDE, null),
        new CropResultDto(OUTSIDE_SUBAREA_FEATURE.getId(), IntersectionStatus.FULLY_OUTSIDE, null),
        new CropResultDto(STRADDLING_SUBAREA_FEATURE.getId(), IntersectionStatus.CROPPED, CROPPED_SUBAREA_FEATURE.getId())
    ));

    var result = partialSurrenderSubareaService.processSubareas(
        LICENCE_CORRECTION, POSITION_ID, CHANGE_ID, BLOCK_FEATURE_ID, surrenderDetails, retainedFeatureIds, null);

    var croppedSubarea = new SubareaDetails(CROPPED_SUBAREA_FEATURE.getId(), "Subarea C", "C");
    var expected = surrenderDetails.withSubareas(Map.of(RETAINED_FEATURE.getId(), List.of(
        SubareaSurrenderOutcome.kept(INSIDE_SUBAREA),
        SubareaSurrenderOutcome.relinquished(OUTSIDE_SUBAREA),
        SubareaSurrenderOutcome.cropped(STRADDLING_SUBAREA, croppedSubarea),
        SubareaSurrenderOutcome.kept(UNSCRIBED_SUBAREA))));
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void processSubareas_whenPartialSurrenderKeepsSeveralParts_thenEachPartRecordsAnOutcomePerSubarea() {
    var surrenderDetails = partialSurrenderDetails();
    givenSubareasGoingIntoTheSurrender(INSIDE_SUBAREA, OUTSIDE_SUBAREA, STRADDLING_SUBAREA);
    var otherRetainedFeature = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 1);
    var otherCroppedSubareaFeature = FeatureTestUtil.subareaFeature(UUID.randomUUID(), "30/1c");
    var retainedFeatureIds = List.of(RETAINED_FEATURE.getId(), otherRetainedFeature.getId());
    var subareaFeatureIds = Set.of(
        INSIDE_SUBAREA_FEATURE.getId(), OUTSIDE_SUBAREA_FEATURE.getId(), STRADDLING_SUBAREA_FEATURE.getId());
    var subareaFeatures = List.of(INSIDE_SUBAREA_FEATURE, OUTSIDE_SUBAREA_FEATURE, STRADDLING_SUBAREA_FEATURE);
    when(featureService.getFeaturesByIds(retainedFeatureIds)).thenReturn(List.of(RETAINED_FEATURE, otherRetainedFeature));
    when(featureService.getFeaturesByIds(subareaFeatureIds)).thenReturn(subareaFeatures);
    when(cropOperatorService.cropFeaturesToBoundary(RETAINED_FEATURE, subareaFeatures)).thenReturn(List.of(
        new CropResultDto(INSIDE_SUBAREA_FEATURE.getId(), IntersectionStatus.FULLY_INSIDE, null),
        new CropResultDto(OUTSIDE_SUBAREA_FEATURE.getId(), IntersectionStatus.FULLY_OUTSIDE, null),
        new CropResultDto(STRADDLING_SUBAREA_FEATURE.getId(), IntersectionStatus.CROPPED, CROPPED_SUBAREA_FEATURE.getId())
    ));
    when(cropOperatorService.cropFeaturesToBoundary(otherRetainedFeature, subareaFeatures)).thenReturn(List.of(
        new CropResultDto(INSIDE_SUBAREA_FEATURE.getId(), IntersectionStatus.FULLY_OUTSIDE, null),
        new CropResultDto(OUTSIDE_SUBAREA_FEATURE.getId(), IntersectionStatus.FULLY_OUTSIDE, null),
        new CropResultDto(
            STRADDLING_SUBAREA_FEATURE.getId(), IntersectionStatus.CROPPED, otherCroppedSubareaFeature.getId())
    ));

    var result = partialSurrenderSubareaService.processSubareas(
        LICENCE_CORRECTION, POSITION_ID, CHANGE_ID, BLOCK_FEATURE_ID, surrenderDetails, retainedFeatureIds, null);

    var croppedSubarea = new SubareaDetails(CROPPED_SUBAREA_FEATURE.getId(), "Subarea C", "C");
    var otherCroppedSubarea = new SubareaDetails(otherCroppedSubareaFeature.getId(), "Subarea C", "C");
    var expected = surrenderDetails.withSubareas(Map.of(
        RETAINED_FEATURE.getId(), List.of(
            SubareaSurrenderOutcome.kept(INSIDE_SUBAREA),
            SubareaSurrenderOutcome.relinquished(OUTSIDE_SUBAREA),
            SubareaSurrenderOutcome.cropped(STRADDLING_SUBAREA, croppedSubarea)),
        otherRetainedFeature.getId(), List.of(
            SubareaSurrenderOutcome.relinquished(INSIDE_SUBAREA),
            SubareaSurrenderOutcome.relinquished(OUTSIDE_SUBAREA),
            SubareaSurrenderOutcome.cropped(STRADDLING_SUBAREA, otherCroppedSubarea))));
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void processSubareas_whenPartialSurrenderAndNoSubareaHasAShape_thenSubareasAreKeptOnTheFirstPartWithoutCropping() {
    var surrenderDetails = partialSurrenderDetails();
    givenSubareasGoingIntoTheSurrender(UNSCRIBED_SUBAREA);

    var result = partialSurrenderSubareaService.processSubareas(
        LICENCE_CORRECTION,
        POSITION_ID,
        CHANGE_ID,
        BLOCK_FEATURE_ID,
        surrenderDetails,
        List.of(RETAINED_FEATURE.getId(), UUID.randomUUID()),
        null);

    var expected = surrenderDetails.withSubareas(
        Map.of(RETAINED_FEATURE.getId(), List.of(SubareaSurrenderOutcome.kept(UNSCRIBED_SUBAREA))));
    assertThat(result).isEqualTo(expected);
    verifyNoInteractions(featureService, cropOperatorService);
  }

  @Test
  void processSubareas_whenIncomingSubareasMatchPrevious_thenPreviousOutcomesReusedWithoutCropping() {
    var surrenderDetails = partialSurrenderDetails();
    givenSubareasGoingIntoTheSurrender(INSIDE_SUBAREA, STRADDLING_SUBAREA, UNSCRIBED_SUBAREA);
    var croppedSubarea = new SubareaDetails(CROPPED_SUBAREA_FEATURE.getId(), "Subarea C", "C");
    var previousDetails = partialSurrenderDetails().withSubareas(Map.of(RETAINED_FEATURE.getId(), List.of(
        SubareaSurrenderOutcome.kept(INSIDE_SUBAREA),
        SubareaSurrenderOutcome.cropped(STRADDLING_SUBAREA, croppedSubarea),
        SubareaSurrenderOutcome.kept(UNSCRIBED_SUBAREA))));

    var result = partialSurrenderSubareaService.processSubareas(
        LICENCE_CORRECTION,
        POSITION_ID,
        CHANGE_ID,
        BLOCK_FEATURE_ID,
        surrenderDetails,
        List.of(RETAINED_FEATURE.getId()),
        previousDetails);

    assertThat(result).isEqualTo(surrenderDetails.withSubareas(previousDetails.retainedFeatureIdToSubareas()));
    verifyNoInteractions(featureService, cropOperatorService);
  }

  @Test
  void processSubareas_whenIncomingSubareasDifferFromPrevious_thenSubareasCroppedAfresh() {
    var surrenderDetails = partialSurrenderDetails();
    givenSubareasGoingIntoTheSurrender(INSIDE_SUBAREA);
    var previousDetails = partialSurrenderDetails().withSubareas(
        Map.of(RETAINED_FEATURE.getId(), List.of(SubareaSurrenderOutcome.kept(OUTSIDE_SUBAREA))));
    var retainedFeatureIds = List.of(RETAINED_FEATURE.getId());
    when(featureService.getFeaturesByIds(retainedFeatureIds)).thenReturn(List.of(RETAINED_FEATURE));
    when(featureService.getFeaturesByIds(Set.of(INSIDE_SUBAREA_FEATURE.getId())))
        .thenReturn(List.of(INSIDE_SUBAREA_FEATURE));
    when(cropOperatorService.cropFeaturesToBoundary(RETAINED_FEATURE, List.of(INSIDE_SUBAREA_FEATURE)))
        .thenReturn(List.of(new CropResultDto(INSIDE_SUBAREA_FEATURE.getId(), IntersectionStatus.FULLY_INSIDE, null)));

    var result = partialSurrenderSubareaService.processSubareas(
        LICENCE_CORRECTION,
        POSITION_ID,
        CHANGE_ID,
        BLOCK_FEATURE_ID,
        surrenderDetails,
        retainedFeatureIds,
        previousDetails);

    var expected = surrenderDetails.withSubareas(
        Map.of(RETAINED_FEATURE.getId(), List.of(SubareaSurrenderOutcome.kept(INSIDE_SUBAREA))));
    assertThat(result).isEqualTo(expected);
  }

  private void givenSubareasGoingIntoTheSurrender(SubareaDetails... subareas) {
    when(licencePositionSpatialService
        .getSubareasGoingIntoChange(LICENCE_CORRECTION, POSITION_ID, BLOCK_FEATURE_ID, CHANGE_ID))
        .thenReturn(List.of(subareas));
  }

  private static SurrenderDetails partialSurrenderDetails() {
    return new SurrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, UUID.randomUUID(), List.of(SURRENDERED_PART_ID));
  }
}
