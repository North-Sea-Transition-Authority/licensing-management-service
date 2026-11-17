package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaTransfer;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryColumn;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryRow;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionDisplay;

@ExtendWith(MockitoExtension.class)
class SubareaChangeOverviewSummarySectionServiceTest {

  private static final UUID BLOCK_FEATURE_ID = UUID.randomUUID();
  private static final Feature BLOCK = FeatureTestUtil.blockFeature(BLOCK_FEATURE_ID, "110", 8);

  @Mock
  private FeatureService featureService;

  @InjectMocks
  private SubareaChangeOverviewSummarySectionService subareaChangeOverviewSummarySectionService;

  @Test
  void getSummarySection_whenSubareasChange_thenAfterShowsUnchangedAndOutputSubareas() {
    var a = subarea("A");
    var b = subarea("B");
    var a1 = subarea("A1");
    var a2 = subarea("A2");
    var context = new SubareaChangeSummaryContext.Staged(
        BLOCK_FEATURE_ID,
        List.of(a, b),
        List.of(new SubareaTransfer(List.of(a), List.of(a1, a2))),
        LicencePositionState.EMPTY,
        "/change-subareas"
    );
    when(featureService.getFeatureOrThrow(BLOCK_FEATURE_ID)).thenReturn(BLOCK);

    var section = subareaChangeOverviewSummarySectionService.getSummarySection(context, null);

    assertThat(section).usingRecursiveComparison().isEqualTo(overview(
        new SummaryMapView(List.of(a.featureId(), b.featureId()), 4230),
        new SummaryMapView(List.of(b.featureId(), a1.featureId(), a2.featureId()), 4230)
    ));
  }

  @Test
  void getSummarySection_whenSubareasHaveNoShape_thenNoMaps() {
    var legacySubarea = new SubareaDetails(null, null, "A");
    var legacyOutput = new SubareaDetails(null, null, "A1");
    var context = new SubareaChangeSummaryContext.Staged(
        BLOCK_FEATURE_ID,
        List.of(legacySubarea),
        List.of(new SubareaTransfer(List.of(legacySubarea), List.of(legacyOutput))),
        LicencePositionState.EMPTY,
        "/change-subareas"
    );
    when(featureService.getFeatureOrThrow(BLOCK_FEATURE_ID)).thenReturn(BLOCK);

    var section = subareaChangeOverviewSummarySectionService.getSummarySection(context, null);

    assertThat(section).usingRecursiveComparison().isEqualTo(overview(null, null));
  }

  private static SubareaDetails subarea(String name) {
    return SubareaDetails.from(FeatureTestUtil.subareaFeature(UUID.randomUUID(), name));
  }

  private static Optional<SummarySection> overview(
      SummaryMapView before,
      SummaryMapView after
  ) {
    var row = new SummaryRow(List.of(
        new SummaryColumn("Before", before, List.of()),
        new SummaryColumn("After", after, List.of())
    ));
    return Optional.of(new SummarySection(
        SubareaChangeOverviewSummarySectionService.SECTION_ORDER,
        List.of(SummaryItem.withRows(null, List.of(row))),
        SummarySectionDisplay.INLINE
    ));
  }
}
