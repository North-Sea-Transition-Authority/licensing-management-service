package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadTestUtil;

class CorrectedTimelineTest {

  @Test
  void positionsToApply_excludesPositionsRemovedByTheCorrection() {
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

    var removedChronologicalPosition = ChronologicalPositionTestUtil.newBuilder()
        .withId(removedPosition.getId())
        .build();
    var updatedChronologicalPosition = ChronologicalPositionTestUtil.newBuilder()
        .withId(updatedPosition.getId())
        .build();

    var correctedTimeline = correctedTimelineWith(
        LicenceCorrectionTestUtil.newBuilder().build(),
        List.of(removeStagedPayload, updateStagedPayload),
        List.of(removedChronologicalPosition, updatedChronologicalPosition));

    assertThat(correctedTimeline.positionsToApply()).containsExactly(updatedChronologicalPosition);
  }

  @Test
  void licenceType() {
    var correctedTimeline = correctedTimelineForLicenceType(LicenceType.SEAWARD_PRODUCTION);

    assertThat(correctedTimeline.licenceType()).isEqualTo(LicenceType.SEAWARD_PRODUCTION);
  }

  private CorrectedTimeline correctedTimelineForLicenceType(LicenceType licenceType) {
    var licenceCorrection = LicenceCorrectionTestUtil.newBuilder()
        .withLicence(LicenceTestUtil.builder().withLicenceType(licenceType).build())
        .build();

    return correctedTimelineWith(licenceCorrection, List.of(), List.of());
  }

  private CorrectedTimeline correctedTimelineWith(
      LicenceCorrection licenceCorrection,
      List<LicencePositionStagedPayload> positionStagedPayloads,
      List<ChronologicalPosition> displayedPositions
  ) {
    return new CorrectedTimeline(
        licenceCorrection,
        positionStagedPayloads,
        displayedPositions,
        new ResolvedStates(new TreeMap<>(), Map.of()));
  }
}