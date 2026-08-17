package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@ExtendWith(MockitoExtension.class)
class PartialSurrenderDetailsSummarySectionServiceTest {

  private static final LocalDate SURRENDER_DATE = LocalDate.of(2026, Month.JUNE, 5);

  @Mock
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @InjectMocks
  private PartialSurrenderDetailsSummarySectionService partialSurrenderDetailsSummarySectionService;

  @Test
  void getSummarySection_whenNoCommittedSurrender_thenEmpty() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.empty());

    var result = partialSurrenderDetailsSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext(positionCorrection),
        null
    );

    assertThat(result).isEmpty();
  }

  @Test
  void getSummarySection_whenCommittedSurrender_thenSectionDescribesSurrenderedBlocks() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    var surrenderedFeatureId = UUID.randomUUID();
    var notSurrenderedFeatureId = UUID.randomUUID();

    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(surrenderedFeatureId))
        .build();
    var surrenderedFeature = FeatureTestUtil.builder()
        .withId(surrenderedFeatureId)
        .withFeatureName("21/30")
        .build();
    var notSurrenderedFeature = FeatureTestUtil.builder()
        .withId(notSurrenderedFeatureId)
        .withFeatureName("21/31")
        .build();

    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(surrender));
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(SURRENDER_DATE);
    when(partialSurrenderCorrectionService.getSurrenderableBlockFeatures(positionCorrection))
        .thenReturn(List.of(surrenderedFeature, notSurrenderedFeature));

    var result = partialSurrenderDetailsSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext(positionCorrection),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderDetailsSummarySectionService.SECTION_ORDER,
        List.of(SummaryItem.withCard(
            PartialSurrenderDetailsSummarySectionService.SURRENDER_DETAILS,
            SummaryCard.simpleSummaryCard(SummaryDataView.newBuilder()
                .addStringValue("Date of surrender", DateUtil.formatLongDate(SURRENDER_DATE))
                .addStringValue("Blocks surrendered", List.of("Block 21/30"))
                .build()
            ))
        )
    );

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenSurrenderedBlockNoLongerSurrenderable_thenThrows() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    var surrenderedFeatureId = UUID.randomUUID();

    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(surrenderedFeatureId))
        .build();
    var otherFeature = FeatureTestUtil.builder()
        .withId(UUID.randomUUID())
        .withFeatureName("21/31")
        .build();

    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(surrender));
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(SURRENDER_DATE);
    when(partialSurrenderCorrectionService.getSurrenderableBlockFeatures(positionCorrection))
        .thenReturn(List.of(otherFeature));

    assertThatThrownBy(() -> partialSurrenderDetailsSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext(positionCorrection),
        null
    ))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(surrenderedFeatureId.toString());
  }
}
