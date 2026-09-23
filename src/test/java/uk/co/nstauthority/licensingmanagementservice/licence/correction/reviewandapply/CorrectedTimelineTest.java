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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;

class CorrectedTimelineTest {

  @Test
  void positionsToApply_excludesPositionsRemovedByTheCorrection() {
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

    var removedChronologicalPosition = ChronologicalPositionTestUtil.newBuilder()
        .withId(removedPosition.getId())
        .build();
    var updatedChronologicalPosition = ChronologicalPositionTestUtil.newBuilder()
        .withId(updatedPosition.getId())
        .build();

    var correctedTimeline = correctedTimelineWith(
        LicenceCorrectionTestUtil.newBuilder().build(),
        List.of(removeCorrection, updateCorrection),
        List.of(removedChronologicalPosition, updatedChronologicalPosition));

    assertThat(correctedTimeline.positionsToApply()).containsExactly(updatedChronologicalPosition);
  }

  @Test
  void isCarbonStorage_whenLicenceTypeIsCarbonStorage_thenTrue() {
    var correctedTimeline = correctedTimelineForLicenceType(LicenceType.CARBON_STORAGE);

    assertThat(correctedTimeline.isCarbonStorage()).isTrue();
  }

  @Test
  void isCarbonStorage_whenLicenceTypeIsNotCarbonStorage_thenFalse() {
    var correctedTimeline = correctedTimelineForLicenceType(LicenceType.SEAWARD_PRODUCTION);

    assertThat(correctedTimeline.isCarbonStorage()).isFalse();
  }

  private CorrectedTimeline correctedTimelineForLicenceType(LicenceType licenceType) {
    var licenceCorrection = LicenceCorrectionTestUtil.newBuilder()
        .withLicence(LicenceTestUtil.builder().withLicenceType(licenceType).build())
        .build();

    return correctedTimelineWith(licenceCorrection, List.of(), List.of());
  }

  private CorrectedTimeline correctedTimelineWith(
      LicenceCorrection licenceCorrection,
      List<LicencePositionCorrection> positionCorrections,
      List<ChronologicalPosition> displayedPositions
  ) {
    return new CorrectedTimeline(
        licenceCorrection,
        positionCorrections,
        displayedPositions,
        new ResolvedStates(new TreeMap<>(), Map.of()));
  }
}