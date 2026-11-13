package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionDisplay;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@ExtendWith(MockitoExtension.class)
class PartialSurrenderDetailsSummarySectionServiceTest {

  private static final LocalDate SURRENDER_DATE = LocalDate.of(2026, Month.JUNE, 5);
  private static final PartialSurrenderOperation SURRENDER = LicenceOperation.newPartialSurrenderOperation()
      .withSurrenderedFeatureIds(List.of(UUID.randomUUID()))
      .build();

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @InjectMocks
  private PartialSurrenderDetailsSummarySectionService partialSurrenderDetailsSummarySectionService;

  @Test
  void getSummarySection_whenStaged_thenSectionShowsLicenceAndSurrenderDate() {
    var licence = LicenceTestUtil.builder().withLicenceReference("P/1").build();
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(licence).build();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .build();

    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(SURRENDER_DATE);

    var result = partialSurrenderDetailsSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.Staged(positionCorrection, SURRENDER, LicencePositionState.EMPTY),
        null
    );

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expectedSection("P/1"));
  }

  @Test
  void getSummarySection_whenCorrectingALiveChange_thenSectionShowsLicenceAndPositionDate() {
    var licence = LicenceTestUtil.builder().withLicenceReference("P/1").build();
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(licence).build();
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var changeId = UUID.randomUUID().toString();

    when(licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition))
        .thenReturn(SURRENDER_DATE);

    var result = partialSurrenderDetailsSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.LiveChange(
            correction,
            licencePosition,
            changeId,
            SURRENDER,
            LicencePositionState.EMPTY
        ),
        null
    );

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expectedSection("P/1"));
  }

  private SummarySection expectedSection(String licenceReference) {
    return new SummarySection(
        PartialSurrenderDetailsSummarySectionService.SECTION_ORDER,
        List.of(SummaryItem.withCard(
            PartialSurrenderDetailsSummarySectionService.SURRENDER_DETAILS,
            SummaryCard.simpleSummaryCard(SummaryDataView.newBuilder()
                .addStringValue("Licence", licenceReference)
                .addStringValue("Surrender date", DateUtil.formatLongDate(SURRENDER_DATE))
                .build()
            ))
        ),
        SummarySectionDisplay.INLINE
    );
  }
}
