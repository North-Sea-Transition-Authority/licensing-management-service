package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;

class CorrectionPositionTest {

  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .build();

  @Test
  void of_whenPositionCorrectionAddsAPosition_thenStagedPosition() {
    var addCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(CORRECTION)
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
        .build();

    var result = CorrectionPosition.of(addCorrection);

    assertThat(result).isEqualTo(new CorrectionPosition.Staged(addCorrection));
  }

  @Test
  void of_whenPositionCorrectionUpdatesAPosition_thenTheExecutedPositionItUpdates() {
    var updateCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(CORRECTION)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().build())
        .build();

    var result = CorrectionPosition.of(updateCorrection);

    assertThat(result).isEqualTo(new CorrectionPosition.Executed(CORRECTION, POSITION));
  }

  @Test
  void of_whenPositionCorrectionRemovesAPosition_thenThrows() {
    var removeCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .build();

    assertThatThrownBy(() -> CorrectionPosition.of(removeCorrection))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Licence position correction %s removes a position so cannot have changes made against it"
            .formatted(removeCorrection.getId()));
  }

  @Test
  void getPathVariables_whenExecutedPosition_thenTheCorrectionAndPositionIds() {
    var result = new CorrectionPosition.Executed(CORRECTION, POSITION).getPathVariables();

    assertThat(result).isEqualTo(Map.of("correctionId", CORRECTION_ID, "licencePositionId", POSITION_ID));
  }

  @Test
  void getPathVariables_whenStagedPosition_thenTheCorrectionAndStagedPositionIds() {
    var addCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(CORRECTION)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(POSITION_ID.toString())
            .build())
        .build();

    var result = new CorrectionPosition.Staged(addCorrection).getPathVariables();

    assertThat(result).isEqualTo(Map.of("correctionId", CORRECTION_ID, "licencePositionId", POSITION_ID));
  }
}
