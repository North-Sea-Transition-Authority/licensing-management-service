package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderTypeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderDefineAreaController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSurrenderOutcome;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCardAction;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;

@ExtendWith(MockitoExtension.class)
class PartialSurrenderBlockSummarySectionServiceTest {

  private static final Feature FIRST_BLOCK = blockFeature(1, "184123456");
  private static final Feature SECOND_BLOCK = blockFeature(2, "200000000");

  private static final Feature RETAINED_PART = feature("SHAPE 2A", "92065000");
  private static final Feature OTHER_RETAINED_PART = feature("SHAPE 2C", "50000000");
  private static final Feature SURRENDERED_PART = feature("SHAPE 2B", "57935000");

  private static final int SRS_WKID = CoordinateSystemUtils.getWkid(SECOND_BLOCK.getCoordinateSystem());
  private static final UUID COMMAND_JOURNEY_ID = UUID.randomUUID();

  @Mock
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @Mock
  private LicencePositionSpatialService licencePositionSpatialService;

  @Mock
  private FeatureService featureService;

  @InjectMocks
  private PartialSurrenderBlockSummarySectionService partialSurrenderBlockSummarySectionService;

  @Test
  void getSummarySection_whenNoCommittedSurrender_thenEmpty() {
    var positionCorrection = positionCorrection();
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.empty());

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.Staged(positionCorrection),
        null
    );

    assertThat(result).isEmpty();
  }

  @Test
  void getSummarySection_whenBlocksSurrendered_thenItemPerBlockOrderedByBlock() {
    var positionCorrection = positionCorrection();
    var partialBlockSurrender = surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, List.of());
    var staged = operation(Map.of(
        FIRST_BLOCK.getId(), surrenderDetails(BlockSurrenderType.FULL_SURRENDER, List.of()),
        SECOND_BLOCK.getId(), partialBlockSurrender
    ));

    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(staged));
    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatures(staged))
        .thenReturn(List.of(SECOND_BLOCK, FIRST_BLOCK));
    when(partialSurrenderCorrectionService.getRetainedFeatureIds(partialBlockSurrender)).thenReturn(List.of());

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.Staged(positionCorrection),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSummarySectionService.SECTION_ORDER,
        List.of(
            new SummaryItem("Block SHAPE 1", List.of(blockCard(
                "Block SHAPE 1",
                SummaryDataView.newBuilder()
                    .addStringValue(PartialSurrenderBlockSummarySectionService.TYPE_OF_CHANGE, "Full surrender")
                    .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_BEFORE, "184.12 km²")
                    .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_AFTER, "0.00 km²")
                    .addMapValue(PartialSurrenderBlockSummarySectionService.UNLABELLED_MAP, map(FIRST_BLOCK.getId()))
                    .build(),
                surrenderTypeUrl(positionCorrection, FIRST_BLOCK)
            ))),
            new SummaryItem("Block SHAPE 2", List.of(blockCard(
                "Block SHAPE 2",
                SummaryDataView.newBuilder()
                    .addStringValue(PartialSurrenderBlockSummarySectionService.TYPE_OF_CHANGE, "Partial surrender")
                    .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_BEFORE, "200.00 km²")
                    .addMapValue(PartialSurrenderBlockSummarySectionService.BEFORE, map(SECOND_BLOCK.getId()))
                    .build(),
                defineAreaUrl(positionCorrection, SECOND_BLOCK)
            )))
        )
    );

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenBlockPartiallySurrendered_thenAreaAndMapsOfRetainedParts() {
    var positionCorrection = positionCorrection();
    var blockSurrender = surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, List.of(SURRENDERED_PART.getId()));
    var staged = operation(Map.of(SECOND_BLOCK.getId(), blockSurrender));
    var retainedFeatureIds = List.of(RETAINED_PART.getId(), OTHER_RETAINED_PART.getId());

    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(staged));
    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatures(staged)).thenReturn(List.of(SECOND_BLOCK));
    when(partialSurrenderCorrectionService.getRetainedFeatureIds(blockSurrender)).thenReturn(retainedFeatureIds);
    when(featureService.getFeaturesByIds(retainedFeatureIds)).thenReturn(List.of(RETAINED_PART, OTHER_RETAINED_PART));

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.Staged(positionCorrection),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSummarySectionService.SECTION_ORDER,
        List.of(new SummaryItem("Block SHAPE 2", List.of(blockCard(
            "Block SHAPE 2",
            SummaryDataView.newBuilder()
                .addStringValue(PartialSurrenderBlockSummarySectionService.TYPE_OF_CHANGE, "Partial surrender")
                .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_BEFORE, "200.00 km²")
                .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_AFTER, "142.07 km²")
                .addMapValue(PartialSurrenderBlockSummarySectionService.BEFORE, map(SECOND_BLOCK.getId()))
                .addMapValue(PartialSurrenderBlockSummarySectionService.AFTER, map(retainedFeatureIds))
                .build(),
            defineAreaUrl(positionCorrection, SECOND_BLOCK)
        ))))
    );

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenBlockSurrenderTypeNotYetSelected_thenOnlySurrenderTypeWithNoValue() {
    var positionCorrection = positionCorrection();
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_BLOCK.getId()))
        .withSurrenderDetails(Map.of())
        .build();

    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(staged));
    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatures(staged)).thenReturn(List.of(FIRST_BLOCK));

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.Staged(positionCorrection),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSummarySectionService.SECTION_ORDER,
        List.of(new SummaryItem("Block SHAPE 1", List.of(blockCard(
            "Block SHAPE 1",
            SummaryDataView.newBuilder()
                .addStringValue(PartialSurrenderBlockSummarySectionService.TYPE_OF_CHANGE, null)
                .build(),
            surrenderTypeUrl(positionCorrection, FIRST_BLOCK)
        ))))
    );

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenBlockPartiallySurrendered_thenSubareaCardsByOutcome() {
    var positionCorrection = positionCorrection();

    var croppedSubareaFeature = feature("SUBAREA A", "40000000");
    var croppedOntoRetainedPart = feature("SUBAREA A_1", "15000000");
    var croppedOntoOtherRetainedPart = feature("SUBAREA A_2", "10000000");
    var unchangedSubareaFeature = feature("SUBAREA B", "30000000");
    var relinquishedSubareaFeature = feature("SUBAREA C", "20000000");

    var croppedSubarea = subarea(croppedSubareaFeature, "A");
    var unchangedSubarea = subarea(unchangedSubareaFeature, "B");
    var relinquishedSubarea = subarea(relinquishedSubareaFeature, "C");
    var legacySubarea = new SubareaDetails(null, null, "D");

    var blockSurrender = surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, List.of(SURRENDERED_PART.getId()))
        .withSubareas(Map.of(
            RETAINED_PART.getId(), List.of(
                SubareaSurrenderOutcome.cropped(croppedSubarea, subarea(croppedOntoRetainedPart, "A")),
                SubareaSurrenderOutcome.relinquished(unchangedSubarea),
                SubareaSurrenderOutcome.relinquished(relinquishedSubarea),
                SubareaSurrenderOutcome.kept(legacySubarea)
            ),
            OTHER_RETAINED_PART.getId(), List.of(
                SubareaSurrenderOutcome.cropped(croppedSubarea, subarea(croppedOntoOtherRetainedPart, "A")),
                SubareaSurrenderOutcome.kept(unchangedSubarea),
                SubareaSurrenderOutcome.relinquished(relinquishedSubarea)
            )
        ));
    var staged = operation(Map.of(SECOND_BLOCK.getId(), blockSurrender));
    var retainedFeatureIds = List.of(RETAINED_PART.getId(), OTHER_RETAINED_PART.getId());

    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(staged));
    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatures(staged)).thenReturn(List.of(SECOND_BLOCK));
    when(partialSurrenderCorrectionService.getRetainedFeatureIds(blockSurrender)).thenReturn(retainedFeatureIds);
    when(featureService.getFeaturesByIds(retainedFeatureIds)).thenReturn(List.of(RETAINED_PART, OTHER_RETAINED_PART));
    when(featureService.getFeaturesByIds(Set.of(
        croppedSubareaFeature.getId(),
        croppedOntoRetainedPart.getId(),
        croppedOntoOtherRetainedPart.getId(),
        unchangedSubareaFeature.getId(),
        relinquishedSubareaFeature.getId()
    ))).thenReturn(List.of(
        croppedSubareaFeature,
        croppedOntoRetainedPart,
        croppedOntoOtherRetainedPart,
        unchangedSubareaFeature,
        relinquishedSubareaFeature
    ));

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.Staged(positionCorrection),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSummarySectionService.SECTION_ORDER,
        List.of(new SummaryItem("Block SHAPE 2", List.of(
            blockCard(
                "Block SHAPE 2",
                SummaryDataView.newBuilder()
                    .addStringValue(PartialSurrenderBlockSummarySectionService.TYPE_OF_CHANGE, "Partial surrender")
                    .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_BEFORE, "200.00 km²")
                    .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_AFTER, "142.07 km²")
                    .addMapValue(PartialSurrenderBlockSummarySectionService.BEFORE, map(SECOND_BLOCK.getId()))
                    .addMapValue(PartialSurrenderBlockSummarySectionService.AFTER, map(retainedFeatureIds))
                    .build(),
                defineAreaUrl(positionCorrection, SECOND_BLOCK)
            ),
            SummaryCard.simpleSummaryCardWithHeading("SUBAREA C", SummaryDataView.newBuilder()
                .addStringValue(PartialSurrenderBlockSummarySectionService.OUTCOME, "Relinquished")
                .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_BEFORE, "20.00 km²")
                .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_AFTER, "0.00 km²")
                .addMapValue(
                    PartialSurrenderBlockSummarySectionService.UNLABELLED_MAP,
                    map(relinquishedSubareaFeature.getId())
                )
                .build()),
            SummaryCard.simpleSummaryCardWithHeading("SUBAREA A", SummaryDataView.newBuilder()
                .addStringValue(PartialSurrenderBlockSummarySectionService.OUTCOME, "Cropped")
                .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_BEFORE, "40.00 km²")
                .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_AFTER, "25.00 km²")
                .addMapValue(PartialSurrenderBlockSummarySectionService.BEFORE, map(croppedSubareaFeature.getId()))
                .addMapValue(PartialSurrenderBlockSummarySectionService.AFTER, map(List.of(
                    croppedOntoRetainedPart.getId(),
                    croppedOntoOtherRetainedPart.getId()
                )))
                .build()),
            SummaryCard.simpleSummaryCardWithHeading("SUBAREA B", SummaryDataView.newBuilder()
                .addStringValue(PartialSurrenderBlockSummarySectionService.OUTCOME, "Unchanged")
                .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_BEFORE, "30.00 km²")
                .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_AFTER, "30.00 km²")
                .addMapValue(
                    PartialSurrenderBlockSummarySectionService.UNLABELLED_MAP,
                    map(unchangedSubareaFeature.getId())
                )
                .build()),
            SummaryCard.simpleSummaryCardWithHeading("Subarea D", SummaryDataView.newBuilder()
                .addStringValue(PartialSurrenderBlockSummarySectionService.OUTCOME, "Unchanged")
                .build())
        )))
    );

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenCorrectingALiveChange_thenChangeLinksToCorrectingSurrenderTypeAndSubareasRelinquished() {
    var correction = LicenceCorrectionTestUtil.newBuilder().build();
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var changeId = UUID.randomUUID().toString();

    var subareaFeature = feature("SUBAREA A", "20000000");
    var subarea = subarea(subareaFeature, "A");
    var surrenderUnderCorrection = operation(Map.of(
        FIRST_BLOCK.getId(), surrenderDetails(BlockSurrenderType.FULL_SURRENDER, List.of())
    ));

    when(partialSurrenderCorrectionService.getSurrenderUnderCorrectionOrThrow(correction, licencePosition, changeId))
        .thenReturn(surrenderUnderCorrection);
    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatures(surrenderUnderCorrection))
        .thenReturn(List.of(FIRST_BLOCK));
    when(licencePositionSpatialService.getSubareasGoingIntoChange(
        correction,
        licencePosition.getId(),
        FIRST_BLOCK.getId(),
        changeId
    )).thenReturn(List.of(subarea));
    when(featureService.getFeaturesByIds(Set.of(subareaFeature.getId()))).thenReturn(List.of(subareaFeature));

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.LiveChange(correction, licencePosition, changeId),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSummarySectionService.SECTION_ORDER,
        List.of(new SummaryItem("Block SHAPE 1", List.of(
            blockCard(
                "Block SHAPE 1",
                SummaryDataView.newBuilder()
                    .addStringValue(PartialSurrenderBlockSummarySectionService.TYPE_OF_CHANGE, "Full surrender")
                    .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_BEFORE, "184.12 km²")
                    .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_AFTER, "0.00 km²")
                    .addMapValue(PartialSurrenderBlockSummarySectionService.UNLABELLED_MAP, map(FIRST_BLOCK.getId()))
                    .build(),
                correctingSurrenderTypeUrl(correction, licencePosition, changeId, FIRST_BLOCK)
            ),
            SummaryCard.simpleSummaryCardWithHeading("SUBAREA A", SummaryDataView.newBuilder()
                .addStringValue(PartialSurrenderBlockSummarySectionService.OUTCOME, "Relinquished")
                .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_BEFORE, "20.00 km²")
                .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_AFTER, "0.00 km²")
                .addMapValue(PartialSurrenderBlockSummarySectionService.UNLABELLED_MAP, map(subareaFeature.getId()))
                .build())
        )))
    );

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenStagedBlockFullySurrendered_thenSubareasGoingIntoTheChangeRelinquished() {
    var positionCorrection = positionCorrection();
    var changeId = UUID.randomUUID().toString();
    var staged = operation(Map.of(
        FIRST_BLOCK.getId(), surrenderDetails(BlockSurrenderType.FULL_SURRENDER, List.of())
    ));

    var legacySubarea = new SubareaDetails(null, "Legacy subarea", "A");

    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(staged));
    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatures(staged)).thenReturn(List.of(FIRST_BLOCK));
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderChangeId(positionCorrection))
        .thenReturn(Optional.of(changeId));
    when(licencePositionSpatialService.getSubareasGoingIntoChange(positionCorrection, FIRST_BLOCK.getId(), changeId))
        .thenReturn(List.of(legacySubarea));

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.Staged(positionCorrection),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSummarySectionService.SECTION_ORDER,
        List.of(new SummaryItem("Block SHAPE 1", List.of(
            blockCard(
                "Block SHAPE 1",
                SummaryDataView.newBuilder()
                    .addStringValue(PartialSurrenderBlockSummarySectionService.TYPE_OF_CHANGE, "Full surrender")
                    .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_BEFORE, "184.12 km²")
                    .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_AFTER, "0.00 km²")
                    .addMapValue(PartialSurrenderBlockSummarySectionService.UNLABELLED_MAP, map(FIRST_BLOCK.getId()))
                    .build(),
                surrenderTypeUrl(positionCorrection, FIRST_BLOCK)
            ),
            SummaryCard.simpleSummaryCardWithHeading("Legacy subarea", SummaryDataView.newBuilder()
                .addStringValue(PartialSurrenderBlockSummarySectionService.OUTCOME, "Relinquished")
                .build())
        )))
    );

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenSubareasShareAnOutcome_thenOrderedByShortName() {
    var positionCorrection = positionCorrection();
    var changeId = UUID.randomUUID().toString();
    var staged = operation(Map.of(
        FIRST_BLOCK.getId(), surrenderDetails(BlockSurrenderType.FULL_SURRENDER, List.of())
    ));

    var secondSubarea = new SubareaDetails(null, "Second subarea", "B");
    var firstSubarea = new SubareaDetails(null, "First subarea", "A");

    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(staged));
    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatures(staged)).thenReturn(List.of(FIRST_BLOCK));
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderChangeId(positionCorrection))
        .thenReturn(Optional.of(changeId));
    when(licencePositionSpatialService.getSubareasGoingIntoChange(positionCorrection, FIRST_BLOCK.getId(), changeId))
        .thenReturn(List.of(secondSubarea, firstSubarea));

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.Staged(positionCorrection),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSummarySectionService.SECTION_ORDER,
        List.of(new SummaryItem("Block SHAPE 1", List.of(
            blockCard(
                "Block SHAPE 1",
                SummaryDataView.newBuilder()
                    .addStringValue(PartialSurrenderBlockSummarySectionService.TYPE_OF_CHANGE, "Full surrender")
                    .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_BEFORE, "184.12 km²")
                    .addStringValue(PartialSurrenderBlockSummarySectionService.AREA_AFTER, "0.00 km²")
                    .addMapValue(PartialSurrenderBlockSummarySectionService.UNLABELLED_MAP, map(FIRST_BLOCK.getId()))
                    .build(),
                surrenderTypeUrl(positionCorrection, FIRST_BLOCK)
            ),
            SummaryCard.simpleSummaryCardWithHeading("First subarea", SummaryDataView.newBuilder()
                .addStringValue(PartialSurrenderBlockSummarySectionService.OUTCOME, "Relinquished")
                .build()),
            SummaryCard.simpleSummaryCardWithHeading("Second subarea", SummaryDataView.newBuilder()
                .addStringValue(PartialSurrenderBlockSummarySectionService.OUTCOME, "Relinquished")
                .build())
        )))
    );

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenASurrenderedBlockIsNotFound_thenThrows() {
    var positionCorrection = positionCorrection();
    var staged = operation(Map.of(
        FIRST_BLOCK.getId(), surrenderDetails(BlockSurrenderType.FULL_SURRENDER, List.of())
    ));

    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(staged));
    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatures(staged)).thenReturn(List.of());

    assertThatThrownBy(() -> partialSurrenderBlockSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.Staged(positionCorrection),
        null
    ))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Surrendered feature %s not found for correction %s"
            .formatted(FIRST_BLOCK.getId(), positionCorrection.getId()));
  }

  private static Feature blockFeature(int blockNumber, String squareMetres) {
    var block = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", blockNumber);
    block.setFeatureArea(new BigDecimal(squareMetres));
    return block;
  }

  private static Feature feature(String featureName, String squareMetres) {
    return FeatureTestUtil.builder()
        .withFeatureName(featureName)
        .withFeatureArea(new BigDecimal(squareMetres))
        .build();
  }

  private SubareaDetails subarea(Feature subareaFeature, String shortName) {
    return new SubareaDetails(subareaFeature.getId(), subareaFeature.getFeatureName(), shortName);
  }

  private SurrenderDetails surrenderDetails(BlockSurrenderType type, List<UUID> surrenderedFeatureIds) {
    return new SurrenderDetails(type, COMMAND_JOURNEY_ID, surrenderedFeatureIds);
  }

  private PartialSurrenderOperation operation(Map<UUID, SurrenderDetails> surrenderDetailsByFeatureId) {
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(surrenderDetailsByFeatureId.keySet())
        .withSurrenderDetails(surrenderDetailsByFeatureId)
        .build();
  }

  private LicencePositionCorrection positionCorrection() {
    return LicencePositionCorrectionTestUtil.newBuilder().build();
  }

  private SummaryCard blockCard(
      String heading,
      SummaryDataView details,
      String changeUrl
  ) {
    return SummaryCard.simpleSummaryCardWithHeading(heading, details)
        .withAction(new SummaryCardAction(PartialSurrenderBlockSummarySectionService.CHANGE, changeUrl));
  }

  private SummaryMapView map(UUID featureId) {
    return map(List.of(featureId));
  }

  private SummaryMapView map(List<UUID> featureIds) {
    return new SummaryMapView(featureIds, SRS_WKID);
  }

  private String defineAreaUrl(
      LicencePositionCorrection positionCorrection,
      Feature block
  ) {
    return ReverseRouter.route(on(PartialSurrenderDefineAreaController.class).renderDefineArea(
        positionCorrection.getLicenceCorrection(),
        positionCorrection,
        block.getId()
    ));
  }

  private String surrenderTypeUrl(
      LicencePositionCorrection positionCorrection,
      Feature block
  ) {
    return ReverseRouter.route(on(BlockSurrenderTypeController.class).renderSurrenderTypeForm(
        positionCorrection.getLicenceCorrection(),
        positionCorrection,
        block.getId()
    ));
  }

  private String correctingSurrenderTypeUrl(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      String changeId,
      Feature block
  ) {
    return ReverseRouter.route(on(BlockSurrenderTypeController.class).renderSurrenderTypeFormForCorrectingChange(
        correction,
        licencePosition,
        new LicencePositionChange(UUID.fromString(changeId)),
        block.getId()
    ));
  }
}
