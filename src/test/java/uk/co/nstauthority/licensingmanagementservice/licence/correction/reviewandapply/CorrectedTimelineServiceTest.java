package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;

@ExtendWith(MockitoExtension.class)
class CorrectedTimelineServiceTest {

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private LicencePositionViewService licencePositionViewService;

  @InjectMocks
  private CorrectedTimelineService correctedTimelineService;

  private final LicenceCorrection correction = LicenceCorrectionTestUtil.newBuilder().build();

  @Test
  void getCorrectedTimeline_retainsRemovedPositionsButExcludesThemFromTheResolvedState() {
    var removedPosition = LicencePositionTestUtil.newBuilder().build();
    var updatedPosition = LicencePositionTestUtil.newBuilder().build();

    var removeCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(removedPosition)
        .withPayload(null)
        .build();
    var updateCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(updatedPosition)
        .build();

    var positionCorrections = List.of(removeCorrection, updateCorrection);
    var removedPositionIds = Set.of(removedPosition.getId());
    var positions = List.of(
        ChronologicalPositionTestUtil.newBuilder().withId(removedPosition.getId()).build(),
        ChronologicalPositionTestUtil.newBuilder().withId(updatedPosition.getId()).build());

    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(positionCorrections);
    when(licencePositionViewService.getCorrectedChronologicalPositions(
        correction, positionCorrections, removedPositionIds))
        .thenReturn(positions);

    var result = correctedTimelineService.getCorrectedTimeline(correction);

    var expected = new CorrectedTimeline(
        correction,
        positionCorrections,
        positions,
        LicencePositionStateResolver.resolve(positions, removedPositionIds));

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }
}