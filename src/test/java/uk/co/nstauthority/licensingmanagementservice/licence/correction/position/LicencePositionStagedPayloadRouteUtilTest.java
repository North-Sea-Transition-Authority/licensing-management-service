package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.UpdateLicencePositionPayloadTestUtil;

class LicencePositionStagedPayloadRouteUtilTest {

  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .build();
  private static final String POSITION_PAGE_URL =
      "/licence-corrections/%s/positions/%s".formatted(CORRECTION_ID, POSITION_ID);

  @Test
  void getPositionPageUrl() {
    var result = LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(LICENCE_CORRECTION, POSITION_ID);

    assertThat(result).isEqualTo(POSITION_PAGE_URL);
  }

  @Test
  void getPositionPageUrl_whenCorrectionPosition_thenPositionPage() {
    var correctionPosition = new CorrectionPosition.Executed(
        LICENCE_CORRECTION,
        LicencePositionTestUtil.newBuilder().withId(POSITION_ID).build()
    );

    var result = LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(correctionPosition);

    assertThat(result).isEqualTo(POSITION_PAGE_URL);
  }

  @Test
  void redirectToPositionPage() {
    var correctionPosition = new CorrectionPosition.Executed(
        LICENCE_CORRECTION,
        LicencePositionTestUtil.newBuilder().withId(POSITION_ID).build()
    );

    var result = LicencePositionStagedPayloadRouteUtil.redirectToPositionPage(correctionPosition);

    assertThat(result.getViewName()).isEqualTo("redirect:" + POSITION_PAGE_URL);
  }

  @Test
  void getPositionPageUrl_whenAddedPosition_thenAddedPositionPage() {
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.ADD_POSITION)
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withTargetLicencePosition(null)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(POSITION_ID.toString())
            .build())
        .build();

    var result = LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(positionStagedPayload);

    assertThat(result).isEqualTo(POSITION_PAGE_URL);
  }

  @Test
  void getPositionPageUrl_whenUpdatedPosition_thenTargetPositionPage() {
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withTargetLicencePosition(LicencePositionTestUtil.newBuilder().withId(POSITION_ID).build())
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().build())
        .build();

    var result = LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(positionStagedPayload);

    assertThat(result).isEqualTo(POSITION_PAGE_URL);
  }

  @Test
  void getPositionPageUrl_whenRemovedPosition_thenThrows() {
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.REMOVE_POSITION)
        .build();

    assertThatThrownBy(() -> LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(positionStagedPayload))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Licence position staged payload %s removes a position so cannot have changes made against it"
            .formatted(positionStagedPayload.getId())
        );
  }
}
