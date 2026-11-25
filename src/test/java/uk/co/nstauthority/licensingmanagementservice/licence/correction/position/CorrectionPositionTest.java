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
  void of_whenPositionStagedPayloadAddsAPosition_thenStagedPosition() {
    var addStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(CORRECTION)
        .withChangeType(LicencePositionStagedPayloadChangeType.ADD_POSITION)
        .build();

    var result = CorrectionPosition.of(addStagedPayload);

    assertThat(result).isEqualTo(new CorrectionPosition.Staged(addStagedPayload));
  }

  @Test
  void of_whenPositionStagedPayloadUpdatesAPosition_thenTheExecutedPositionItUpdates() {
    var updateStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(CORRECTION)
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().build())
        .build();

    var result = CorrectionPosition.of(updateStagedPayload);

    assertThat(result).isEqualTo(new CorrectionPosition.Executed(CORRECTION, POSITION));
  }

  @Test
  void of_whenPositionStagedPayloadRemovesAPosition_thenThrows() {
    var removeStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.REMOVE_POSITION)
        .build();

    assertThatThrownBy(() -> CorrectionPosition.of(removeStagedPayload))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Licence position staged payload %s removes a position so cannot have changes made against it"
            .formatted(removeStagedPayload.getId()));
  }

  @Test
  void getPathVariables_whenExecutedPosition_thenTheCorrectionAndPositionIds() {
    var result = new CorrectionPosition.Executed(CORRECTION, POSITION).getPathVariables();

    assertThat(result).isEqualTo(Map.of("correctionId", CORRECTION_ID, "licencePositionId", POSITION_ID));
  }

  @Test
  void getPathVariables_whenStagedPosition_thenTheCorrectionAndStagedPositionIds() {
    var addStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(CORRECTION)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(POSITION_ID.toString())
            .build())
        .build();

    var result = new CorrectionPosition.Staged(addStagedPayload).getPathVariables();

    assertThat(result).isEqualTo(Map.of("correctionId", CORRECTION_ID, "licencePositionId", POSITION_ID));
  }
}
