package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.CorrectionMarker;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;

@ExtendWith(MockitoExtension.class)
class CorrectionSummaryServiceTest {

  @Mock
  private CorrectionReviewService correctionReviewService;

  @Mock
  private CorrectedTimelineService correctedTimelineService;

  @InjectMocks
  private CorrectionSummaryService correctionSummaryService;

  @Test
  void getSummaryPositions_whenCorrectionIsComplete_thenReturnsAppliedPositions() {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .build();
    var appliedPositions = List.of(
        new ReviewPositionView(UUID.randomUUID(), "1 January 2026", "COR-1", CorrectionMarker.POSITION_REMOVED, List.of()));

    when(correctionReviewService.getAppliedPositions(correction)).thenReturn(appliedPositions);

    var result = correctionSummaryService.getSummaryPositions(correction);

    assertThat(result).isEqualTo(appliedPositions);
  }

  @Test
  void getSummaryPositions_whenCorrectionIsInProgress_thenReturnsReviewPositionsWithoutValidationErrors() {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withStatus(LicenceCorrectionStatus.IN_PROGRESS)
        .build();
    var correctedTimeline = new CorrectedTimeline(
        correction, List.of(), List.of(), new ResolvedStates(new TreeMap<>(), Map.of()));
    var reviewPositions = List.of(
        new ReviewPositionView(UUID.randomUUID(), "1 January 2026", "COR-1", CorrectionMarker.POSITION_ADDED, List.of()));

    when(correctedTimelineService.getCorrectedTimeline(correction)).thenReturn(correctedTimeline);
    when(correctionReviewService.getReviewPositions(correctedTimeline, List.of())).thenReturn(reviewPositions);

    var result = correctionSummaryService.getSummaryPositions(correction);

    assertThat(result).isEqualTo(reviewPositions);
  }
}