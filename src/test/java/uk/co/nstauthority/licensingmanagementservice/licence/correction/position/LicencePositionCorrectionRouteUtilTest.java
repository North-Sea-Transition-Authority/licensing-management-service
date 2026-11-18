package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;

class LicencePositionCorrectionRouteUtilTest {

  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .build();
  private static final String POSITION_PAGE_URL =
      "/licence-corrections/%s/positions/%s".formatted(CORRECTION_ID, POSITION_ID);

  @Test
  void getPositionPageUrl() {
    var result = LicencePositionCorrectionRouteUtil.getPositionPageUrl(LICENCE_CORRECTION, POSITION_ID);

    assertThat(result).isEqualTo(POSITION_PAGE_URL);
  }

  @Test
  void getPositionPageUrl_whenAddedPosition_thenAddedPositionPage() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withTargetLicencePosition(null)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(POSITION_ID.toString())
            .build())
        .build();

    var result = LicencePositionCorrectionRouteUtil.getPositionPageUrl(positionCorrection);

    assertThat(result).isEqualTo(POSITION_PAGE_URL);
  }

  @Test
  void getPositionPageUrl_whenUpdatedPosition_thenTargetPositionPage() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withTargetLicencePosition(LicencePositionTestUtil.newBuilder().withId(POSITION_ID).build())
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().build())
        .build();

    var result = LicencePositionCorrectionRouteUtil.getPositionPageUrl(positionCorrection);

    assertThat(result).isEqualTo(POSITION_PAGE_URL);
  }

  @Test
  void getPositionPageUrl_whenRemovedPosition_thenThrows() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .build();

    assertThatThrownBy(() -> LicencePositionCorrectionRouteUtil.getPositionPageUrl(positionCorrection))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Licence position correction %s removes a position so cannot have changes made against it"
            .formatted(positionCorrection.getId())
        );
  }
}
