package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.Tag;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.TagColour;
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
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCardAction;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryColumn;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryRow;
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

  private static final Tag FULL_SURRENDER_TAG = new Tag("Full surrender", TagColour.RED);
  private static final Tag PARTIAL_SURRENDER_TAG = new Tag("Partial surrender", TagColour.BLUE);
  private static final Tag RELINQUISHED_TAG = new Tag("Relinquished", TagColour.RED);
  private static final Tag CROPPED_TAG = new Tag("Cropped", TagColour.BLUE);
  private static final Tag UNCHANGED_TAG = new Tag("Unchanged", TagColour.GREY);

  @Mock
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private FeatureService featureService;

  @InjectMocks
  private PartialSurrenderBlockSummarySectionService partialSurrenderBlockSummarySectionService;

  @Test
  void getSummarySection_whenBlockSurrenderTypeNotYetSelected_thenItemPerBlockWithOnlyBeforeColumn() {
    var positionCorrection = positionCorrection();
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_BLOCK.getId(), SECOND_BLOCK.getId()))
        .build();

    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatures(staged))
        .thenReturn(List.of(SECOND_BLOCK, FIRST_BLOCK));

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        stagedContext(positionCorrection, staged, LicencePositionState.EMPTY),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSummarySectionService.SECTION_ORDER,
        List.of(
            SummaryItem.withRows("Block SHAPE 1", List.of(new SummaryRow(List.of(new SummaryColumn(
                PartialSurrenderBlockSummarySectionService.BEFORE,
                map(FIRST_BLOCK.getId()),
                List.of(blockBeforeCard("Block SHAPE 1", "184.12\u00A0km²")
                    .withAction(changeAction(surrenderTypeUrl(positionCorrection, FIRST_BLOCK))))
            ))))),
            SummaryItem.withRows("Block SHAPE 2", List.of(new SummaryRow(List.of(new SummaryColumn(
                PartialSurrenderBlockSummarySectionService.BEFORE,
                map(SECOND_BLOCK.getId()),
                List.of(blockBeforeCard("Block SHAPE 2", "200.00\u00A0km²")
                    .withAction(changeAction(surrenderTypeUrl(positionCorrection, SECOND_BLOCK))))
            )))))
        )
    );

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenBlockFullySurrendered_thenSubareasGoingIntoTheChangeRelinquished() {
    var positionCorrection = positionCorrection();
    var staged = operation(Map.of(
        FIRST_BLOCK.getId(), surrenderDetails(BlockSurrenderType.FULL_SURRENDER, List.of())
    ));

    var subareaFeature = feature("Bravo", "20000000");
    var subarea = subarea(subareaFeature, "B");
    var legacySubarea = new SubareaDetails(null, "Alpha", "A");
    var stateBefore = state(
        FIRST_BLOCK,
        Set.of(subarea, legacySubarea),
        Map.of(subarea, Map.of(1, new BigDecimal("60"), 2, new BigDecimal("40")))
    );

    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatures(staged)).thenReturn(List.of(FIRST_BLOCK));
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(argThat(ids -> Set.copyOf(ids).equals(Set.of(1, 2)))))
        .thenReturn(Map.of(1, "zeta ltd"));
    when(featureService.getFeaturesByIds(Set.of(subareaFeature.getId()))).thenReturn(List.of(subareaFeature));

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        stagedContext(positionCorrection, staged, stateBefore),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSummarySectionService.SECTION_ORDER,
        List.of(SummaryItem.withRows("Block SHAPE 1", List.of(
            new SummaryRow(List.of(
                new SummaryColumn(
                    PartialSurrenderBlockSummarySectionService.BEFORE,
                    map(FIRST_BLOCK.getId()),
                    List.of(blockBeforeCard("Block SHAPE 1", "184.12\u00A0km²"))
                ),
                new SummaryColumn(
                    PartialSurrenderBlockSummarySectionService.AFTER,
                    null,
                    List.of(blockAfterCard(
                        "Block SHAPE 1",
                        SummaryDataView.newStringKeyValue(PartialSurrenderBlockSummarySectionService.AREA, "0.00\u00A0km²"),
                        surrenderTypeUrl(positionCorrection, FIRST_BLOCK)
                    ))
                )
            )),
            subareaRowWithExplanation(
                "Subarea Alpha",
                RELINQUISHED_TAG,
                null,
                SummaryDataView.newBuilder().build(),
                PartialSurrenderBlockSummarySectionService.RELINQUISHED_WITH_BLOCK
            ),
            subareaRowWithExplanation(
                "Subarea Bravo",
                RELINQUISHED_TAG,
                map(subareaFeature.getId()),
                SummaryDataView.newBuilder()
                    .addStringValue(PartialSurrenderBlockSummarySectionService.AREA, "20.00\u00A0km²")
                    .addStringValue(
                        PartialSurrenderBlockSummarySectionService.LICENSEES,
                        List.of("Not available (40%)", "zeta ltd (60%)")
                    )
                    .build(),
                PartialSurrenderBlockSummarySectionService.RELINQUISHED_WITH_BLOCK
            )
        )).withTag(FULL_SURRENDER_TAG))
    );

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenBlockPartiallySurrendered_thenRowPerSubareaWithLicenseesOnlyBefore() {
    var positionCorrection = positionCorrection();

    var croppedSubareaFeature = feature("Alpha", "40000000");
    var croppedOntoRetainedPart = feature("Alpha 2", "15000000");
    var croppedOntoOtherRetainedPart = feature("Alpha 1", "10000000");
    var unchangedSubareaFeature = feature("Bravo", "30000000");
    var relinquishedSubareaFeature = feature("Charlie", "20000000");

    var croppedSubarea = subarea(croppedSubareaFeature, "A");
    var firstCroppedPart = subarea(croppedOntoOtherRetainedPart, "A1");
    var secondCroppedPart = subarea(croppedOntoRetainedPart, "A2");
    var unchangedSubarea = subarea(unchangedSubareaFeature, "B");
    var relinquishedSubarea = subarea(relinquishedSubareaFeature, "C");
    var legacySubarea = new SubareaDetails(null, null, "D");

    var retainedFeatureIds = List.of(RETAINED_PART.getId(), OTHER_RETAINED_PART.getId());
    var blockSurrender = surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, List.of(SURRENDERED_PART.getId()))
        .withRetainedFeatureIds(retainedFeatureIds)
        .withSubareas(Map.of(
            RETAINED_PART.getId(), List.of(
                SubareaSurrenderOutcome.cropped(croppedSubarea, secondCroppedPart),
                SubareaSurrenderOutcome.relinquished(unchangedSubarea),
                SubareaSurrenderOutcome.relinquished(relinquishedSubarea),
                SubareaSurrenderOutcome.kept(legacySubarea)
            ),
            OTHER_RETAINED_PART.getId(), List.of(
                SubareaSurrenderOutcome.cropped(croppedSubarea, firstCroppedPart),
                SubareaSurrenderOutcome.kept(unchangedSubarea),
                SubareaSurrenderOutcome.relinquished(relinquishedSubarea)
            )
        ));
    var staged = operation(Map.of(SECOND_BLOCK.getId(), blockSurrender));
    var stateBefore = state(
        SECOND_BLOCK,
        Set.of(croppedSubarea, unchangedSubarea, relinquishedSubarea, legacySubarea),
        Map.of(croppedSubarea, Map.of(1, new BigDecimal("100")))
    );

    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatures(staged)).thenReturn(List.of(SECOND_BLOCK));
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(1))).thenReturn(Map.of(1, "Org"));
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
        stagedContext(positionCorrection, staged, stateBefore),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSummarySectionService.SECTION_ORDER,
        List.of(SummaryItem.withRows("Block SHAPE 2", List.of(
            new SummaryRow(List.of(
                new SummaryColumn(
                    PartialSurrenderBlockSummarySectionService.BEFORE,
                    map(SECOND_BLOCK.getId()),
                    List.of(blockBeforeCard("Block SHAPE 2", "200.00\u00A0km²"))
                ),
                new SummaryColumn(
                    PartialSurrenderBlockSummarySectionService.AFTER,
                    map(retainedFeatureIds),
                    List.of(blockAfterCard(
                        "Block SHAPE 2",
                        SummaryDataView.newStringKeyValue(PartialSurrenderBlockSummarySectionService.AREA, "142.07\u00A0km²"),
                        defineAreaUrl(positionCorrection, SECOND_BLOCK)
                    ))
                )
            )),
            subareaRowWithExplanation(
                "Subarea Charlie",
                RELINQUISHED_TAG,
                map(relinquishedSubareaFeature.getId()),
                area("20.00\u00A0km²"),
                PartialSurrenderBlockSummarySectionService.RELINQUISHED_OUTSIDE_AREA_KEPT
            ),
            new SummaryRow("Subarea Alpha", CROPPED_TAG, List.of(
                new SummaryColumn(
                    PartialSurrenderBlockSummarySectionService.BEFORE,
                    map(croppedSubareaFeature.getId()),
                    List.of(subareaCard("Subarea Alpha", SummaryDataView.newBuilder()
                        .addStringValue(PartialSurrenderBlockSummarySectionService.AREA, "40.00\u00A0km²")
                        .addStringValue(PartialSurrenderBlockSummarySectionService.LICENSEES, List.of("Org (100%)"))
                        .build()))
                ),
                new SummaryColumn(
                    PartialSurrenderBlockSummarySectionService.AFTER,
                    map(List.of(croppedOntoOtherRetainedPart.getId(), croppedOntoRetainedPart.getId())),
                    List.of(
                        subareaCard("Subarea Alpha 1", area("10.00\u00A0km²")),
                        subareaCard("Subarea Alpha 2", area("15.00\u00A0km²"))
                    )
                )
            )),
            subareaRowWithExplanation(
                "Subarea Bravo",
                UNCHANGED_TAG,
                map(unchangedSubareaFeature.getId()),
                area("30.00\u00A0km²"),
                PartialSurrenderBlockSummarySectionService.KEPT_UNCHANGED
            ),
            subareaRowWithExplanation(
                "Subarea D",
                UNCHANGED_TAG,
                null,
                SummaryDataView.newBuilder().build(),
                PartialSurrenderBlockSummarySectionService.KEPT_UNCHANGED
            )
        )).withTag(PARTIAL_SURRENDER_TAG))
    );

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenAreasToSurrenderNotYetSelected_thenAfterBlockCardEmpty() {
    var positionCorrection = positionCorrection();
    var staged = operation(Map.of(
        SECOND_BLOCK.getId(), surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, List.of())
    ));

    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatures(staged)).thenReturn(List.of(SECOND_BLOCK));

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        stagedContext(positionCorrection, staged, LicencePositionState.EMPTY),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSummarySectionService.SECTION_ORDER,
        List.of(SummaryItem.withRows("Block SHAPE 2", List.of(new SummaryRow(List.of(
            new SummaryColumn(
                PartialSurrenderBlockSummarySectionService.BEFORE,
                map(SECOND_BLOCK.getId()),
                List.of(blockBeforeCard("Block SHAPE 2", "200.00\u00A0km²"))
            ),
            new SummaryColumn(
                PartialSurrenderBlockSummarySectionService.AFTER,
                null,
                List.of(blockAfterCard(
                    "Block SHAPE 2",
                    SummaryDataView.newBuilder().build(),
                    defineAreaUrl(positionCorrection, SECOND_BLOCK)
                ))
            )
        )))).withTag(PARTIAL_SURRENDER_TAG))
    );

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenCorrectingALiveChange_thenChangeLinksToCorrectingSurrenderType() {
    var correction = LicenceCorrectionTestUtil.newBuilder().build();
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var changeId = UUID.randomUUID().toString();
    var surrenderUnderCorrection = operation(Map.of(
        FIRST_BLOCK.getId(), surrenderDetails(BlockSurrenderType.FULL_SURRENDER, List.of())
    ));

    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatures(surrenderUnderCorrection))
        .thenReturn(List.of(FIRST_BLOCK));

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.LiveChange(
            correction,
            licencePosition,
            changeId,
            surrenderUnderCorrection,
            LicencePositionState.EMPTY
        ),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSummarySectionService.SECTION_ORDER,
        List.of(SummaryItem.withRows("Block SHAPE 1", List.of(new SummaryRow(List.of(
            new SummaryColumn(
                PartialSurrenderBlockSummarySectionService.BEFORE,
                map(FIRST_BLOCK.getId()),
                List.of(blockBeforeCard("Block SHAPE 1", "184.12\u00A0km²"))
            ),
            new SummaryColumn(
                PartialSurrenderBlockSummarySectionService.AFTER,
                null,
                List.of(blockAfterCard(
                    "Block SHAPE 1",
                    SummaryDataView.newStringKeyValue(PartialSurrenderBlockSummarySectionService.AREA, "0.00\u00A0km²"),
                    correctingSurrenderTypeUrl(correction, licencePosition, changeId, FIRST_BLOCK)
                ))
            )
        )))).withTag(FULL_SURRENDER_TAG))
    );

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenViewingALiveChangeOutsideACorrection_thenNoChangeLinks() {
    var surrender = operation(Map.of(
        FIRST_BLOCK.getId(), surrenderDetails(BlockSurrenderType.FULL_SURRENDER, List.of())
    ));

    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatures(surrender)).thenReturn(List.of(FIRST_BLOCK));

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.LiveChange(
            null,
            LicencePositionTestUtil.newBuilder().build(),
            UUID.randomUUID().toString(),
            surrender,
            LicencePositionState.EMPTY
        ),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSummarySectionService.SECTION_ORDER,
        List.of(SummaryItem.withRows("Block SHAPE 1", List.of(new SummaryRow(List.of(
            new SummaryColumn(
                PartialSurrenderBlockSummarySectionService.BEFORE,
                map(FIRST_BLOCK.getId()),
                List.of(blockBeforeCard("Block SHAPE 1", "184.12\u00A0km²"))
            ),
            new SummaryColumn(
                PartialSurrenderBlockSummarySectionService.AFTER,
                null,
                List.of(SummaryCard.simpleSummaryCardWithHeading(
                    "Block SHAPE 1",
                    SummaryDataView.newStringKeyValue(PartialSurrenderBlockSummarySectionService.AREA, "0.00\u00A0km²")
                ))
            )
        )))).withTag(FULL_SURRENDER_TAG))
    );

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenASurrenderedBlockIsNotFound_thenThrows() {
    var positionCorrection = positionCorrection();
    var staged = operation(Map.of(
        FIRST_BLOCK.getId(), surrenderDetails(BlockSurrenderType.FULL_SURRENDER, List.of())
    ));
    var context = stagedContext(positionCorrection, staged, LicencePositionState.EMPTY);

    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatures(staged)).thenReturn(List.of());

    assertThatThrownBy(() -> partialSurrenderBlockSummarySectionService.getSummarySection(context, null))
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

  private LicencePositionState state(
      Feature block,
      Set<SubareaDetails> subareas,
      Map<SubareaDetails, Map<Integer, BigDecimal>> subareaEquity
  ) {
    return new LicencePositionState(
        null,
        null,
        Set.of(),
        Map.of(),
        Map.of(block.getId(), subareas),
        Map.of(block.getId(), subareaEquity)
    );
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

  private PartialSurrenderSummaryContext stagedContext(
      LicencePositionCorrection positionCorrection,
      PartialSurrenderOperation surrender,
      LicencePositionState stateBefore
  ) {
    return new PartialSurrenderSummaryContext.Staged(positionCorrection, surrender, stateBefore);
  }

  private SummaryCard blockBeforeCard(String heading, String area) {
    return SummaryCard.simpleSummaryCardWithHeading(heading, area(area));
  }

  private SummaryCard blockAfterCard(String heading, SummaryDataView details, String changeUrl) {
    return SummaryCard.simpleSummaryCardWithHeading(heading, details).withAction(changeAction(changeUrl));
  }

  private SummaryCard subareaCard(String heading, SummaryDataView details) {
    return SummaryCard.simpleSummaryCardWithHeading(heading, details);
  }

  private SummaryRow subareaRowWithExplanation(
      String heading,
      Tag tag,
      @Nullable SummaryMapView map,
      SummaryDataView details,
      String explanation
  ) {
    return new SummaryRow(heading, tag, List.of(
        new SummaryColumn(PartialSurrenderBlockSummarySectionService.BEFORE, map, List.of(subareaCard(heading, details))),
        new SummaryColumn(PartialSurrenderBlockSummarySectionService.AFTER, null, List.of(), explanation)
    ));
  }

  private SummaryDataView area(String area) {
    return SummaryDataView.newStringKeyValue(PartialSurrenderBlockSummarySectionService.AREA, area);
  }

  private SummaryCardAction changeAction(String changeUrl) {
    return new SummaryCardAction(PartialSurrenderBlockSummarySectionService.CHANGE, changeUrl);
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
