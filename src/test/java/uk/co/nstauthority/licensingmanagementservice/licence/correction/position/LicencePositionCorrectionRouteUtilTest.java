package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

class LicencePositionCorrectionRouteUtilTest {

  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder().build();

  @Test
  void getPositionPageUrl_addedPosition() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
        .build();

    var result = LicencePositionCorrectionRouteUtil.getPositionPageUrl(LICENCE_CORRECTION, positionCorrection);

    assertThat(result).isEqualTo(ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderAddedPosition(LICENCE_CORRECTION, positionCorrection))
    );
  }

  @Test
  void getPositionPageUrl_updatedPosition() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .build();

    var result = LicencePositionCorrectionRouteUtil.getPositionPageUrl(LICENCE_CORRECTION, positionCorrection);

    assertThat(result).isEqualTo(ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(LICENCE_CORRECTION, positionCorrection.getTargetLicencePosition()))
    );
  }

  @Test
  void getPositionPageUrl_removedPosition() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .build();

    assertThatThrownBy(() -> LicencePositionCorrectionRouteUtil.getPositionPageUrl(LICENCE_CORRECTION, positionCorrection))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Licence position correction %s removes a position so cannot have changes made against it"
            .formatted(positionCorrection.getId())
        );
  }
}
