package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryColumn;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryRow;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionDisplay;

@ExtendWith(MockitoExtension.class)
class PartialSurrenderLicenceAreaSummarySectionServiceTest {

  private static final Feature FIRST_BLOCK = blockFeature(1, "100000000");
  private static final Feature SECOND_BLOCK = blockFeature(2, "200000000");
  private static final Feature RETAINED_PART = blockFeature(3, "50000000");

  private static final int SRS_WKID = CoordinateSystemUtils.getWkid(FIRST_BLOCK.getCoordinateSystem());
  private static final LicencePositionState STATE_BEFORE = new LicencePositionState(
      null,
      null,
      Set.of(),
      Map.of(),
      Map.of(FIRST_BLOCK.getId(), Set.of(), SECOND_BLOCK.getId(), Set.of()),
      Map.of()
  );

  @Mock
  private FeatureService featureService;

  @InjectMocks
  private PartialSurrenderLicenceAreaSummarySectionService partialSurrenderLicenceAreaSummarySectionService;

  @Test
  void getSummarySection_whenNoBlocksGoingIntoTheChange_thenEmpty() {
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(SECOND_BLOCK.getId()))
        .build();
    var context = new PartialSurrenderSummaryContext.Staged(
        LicencePositionCorrectionTestUtil.newBuilder().build(),
        surrender,
        LicencePositionState.EMPTY
    );

    when(featureService.getFeaturesByIds(Set.of())).thenReturn(List.of());

    var result = partialSurrenderLicenceAreaSummarySectionService.getSummarySection(context, null);

    assertThat(result).isEmpty();
  }

  @Test
  void getSummarySection_whenSurrenderTakesEffect_thenBeforeAndAfterColumns() {
    var correction = correction("P1");
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(SECOND_BLOCK.getId()))
        .withSurrenderDetails(Map.of(SECOND_BLOCK.getId(), new SurrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER,
            UUID.randomUUID(),
            List.of(UUID.randomUUID()),
            List.of(RETAINED_PART.getId())
        )))
        .build();
    var context = new PartialSurrenderSummaryContext.Staged(
        LicencePositionCorrectionTestUtil.newBuilder().withLicenceCorrection(correction).build(),
        surrender,
        STATE_BEFORE
    );

    when(featureService.getFeaturesByIds(Set.of(FIRST_BLOCK.getId(), SECOND_BLOCK.getId())))
        .thenReturn(List.of(SECOND_BLOCK, FIRST_BLOCK));
    when(featureService.getFeaturesByIds(Set.of(FIRST_BLOCK.getId(), RETAINED_PART.getId())))
        .thenReturn(List.of(RETAINED_PART, FIRST_BLOCK));

    var result = partialSurrenderLicenceAreaSummarySectionService.getSummarySection(context, null);

    var expected = expectedSection(List.of(
        new SummaryColumn(
            PartialSurrenderBlockSummarySectionService.BEFORE,
            map(FIRST_BLOCK, SECOND_BLOCK),
            List.of(areaCard("Licence P1", "300.00\u00A0km²"))
        ),
        new SummaryColumn(
            PartialSurrenderBlockSummarySectionService.AFTER,
            map(FIRST_BLOCK, RETAINED_PART),
            List.of(areaCard("Licence P1", "150.00\u00A0km²"))
        )
    ));

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenEveryBlockFullySurrendered_thenAfterColumnHasNoMap() {
    var correction = correction("P3");
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_BLOCK.getId(), SECOND_BLOCK.getId()))
        .withSurrenderDetails(Map.of(
            FIRST_BLOCK.getId(), new SurrenderDetails(BlockSurrenderType.FULL_SURRENDER, null, List.of()),
            SECOND_BLOCK.getId(), new SurrenderDetails(BlockSurrenderType.FULL_SURRENDER, null, List.of())
        ))
        .build();
    var context = new PartialSurrenderSummaryContext.Staged(
        LicencePositionCorrectionTestUtil.newBuilder().withLicenceCorrection(correction).build(),
        surrender,
        STATE_BEFORE
    );

    when(featureService.getFeaturesByIds(Set.of(FIRST_BLOCK.getId(), SECOND_BLOCK.getId())))
        .thenReturn(List.of(FIRST_BLOCK, SECOND_BLOCK));
    when(featureService.getFeaturesByIds(Set.of())).thenReturn(List.of());

    var result = partialSurrenderLicenceAreaSummarySectionService.getSummarySection(context, null);

    var expected = expectedSection(List.of(
        new SummaryColumn(
            PartialSurrenderBlockSummarySectionService.BEFORE,
            map(FIRST_BLOCK, SECOND_BLOCK),
            List.of(areaCard("Licence P3", "300.00\u00A0km²"))
        ),
        new SummaryColumn(
            PartialSurrenderBlockSummarySectionService.AFTER,
            null,
            List.of(areaCard("Licence P3", "0.00\u00A0km²"))
        )
    ));

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenSurrenderDoesNotTakeEffectYet_thenOnlyBeforeColumn() {
    var correction = correction("P2");
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(SECOND_BLOCK.getId()))
        .build();
    var context = new PartialSurrenderSummaryContext.LiveChange(
        correction,
        LicencePositionTestUtil.newBuilder().build(),
        UUID.randomUUID().toString(),
        surrender,
        STATE_BEFORE
    );

    when(featureService.getFeaturesByIds(Set.of(FIRST_BLOCK.getId(), SECOND_BLOCK.getId())))
        .thenReturn(List.of(FIRST_BLOCK, SECOND_BLOCK));

    var result = partialSurrenderLicenceAreaSummarySectionService.getSummarySection(context, null);

    var expected = expectedSection(List.of(new SummaryColumn(
        PartialSurrenderBlockSummarySectionService.BEFORE,
        map(FIRST_BLOCK, SECOND_BLOCK),
        List.of(areaCard("Licence P2", "300.00\u00A0km²"))
    )));

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  private static Feature blockFeature(int blockNumber, String squareMetres) {
    var block = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", blockNumber);
    block.setFeatureArea(new BigDecimal(squareMetres));
    return block;
  }

  private LicenceCorrection correction(String licenceReference) {
    return LicenceCorrectionTestUtil.newBuilder()
        .withLicence(LicenceTestUtil.builder().withLicenceReference(licenceReference).build())
        .build();
  }

  private SummarySection expectedSection(List<SummaryColumn> columns) {
    return new SummarySection(
        PartialSurrenderLicenceAreaSummarySectionService.SECTION_ORDER,
        List.of(SummaryItem.withRows(
            PartialSurrenderLicenceAreaSummarySectionService.LICENCE_AREA,
            List.of(new SummaryRow(columns))
        )),
        SummarySectionDisplay.INLINE
    );
  }

  private SummaryCard areaCard(String heading, String area) {
    return SummaryCard.simpleSummaryCardWithHeading(
        heading,
        SummaryDataView.newStringKeyValue(PartialSurrenderBlockSummarySectionService.AREA, area)
    );
  }

  private SummaryMapView map(Feature... blocks) {
    return new SummaryMapView(Stream.of(blocks).map(Feature::getId).toList(), SRS_WKID);
  }
}
