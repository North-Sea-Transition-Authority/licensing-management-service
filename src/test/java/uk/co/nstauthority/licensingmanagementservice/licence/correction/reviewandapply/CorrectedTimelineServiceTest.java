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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.ChronologicalPositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;

@ExtendWith(MockitoExtension.class)
class CorrectedTimelineServiceTest {

  @Mock
  private LicencePositionStagedPayloadService licencePositionStagedPayloadService;

  @Mock
  private ChronologicalPositionService chronologicalPositionService;

  @InjectMocks
  private CorrectedTimelineService correctedTimelineService;

  private final LicenceCorrection correction = LicenceCorrectionTestUtil.newBuilder().build();

  @Test
  void getCorrectedTimeline_retainsRemovedPositionsButExcludesThemFromTheResolvedState() {
    var removedPosition = LicencePositionTestUtil.newBuilder().build();
    var updatedPosition = LicencePositionTestUtil.newBuilder().build();

    var removeStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(removedPosition)
        .withPayload(null)
        .build();
    var updateStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(updatedPosition)
        .build();

    var positionStagedPayloads = List.of(removeStagedPayload, updateStagedPayload);
    var removedPositionIds = Set.of(removedPosition.getId());
    var positions = List.of(
        ChronologicalPositionTestUtil.newBuilder().withId(removedPosition.getId()).build(),
        ChronologicalPositionTestUtil.newBuilder().withId(updatedPosition.getId()).build());

    when(licencePositionStagedPayloadService.getPositionStagedPayloads(correction)).thenReturn(positionStagedPayloads);
    when(chronologicalPositionService.getCorrectedChronologicalPositions(
        correction, positionStagedPayloads, removedPositionIds))
        .thenReturn(positions);

    var result = correctedTimelineService.getCorrectedTimeline(correction);

    var expected = new CorrectedTimeline(
        correction,
        positionStagedPayloads,
        positions,
        LicencePositionStateResolver.resolve(positions, removedPositionIds));

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }
}