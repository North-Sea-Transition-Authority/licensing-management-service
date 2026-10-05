package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

class LicencePositionCorrectionRouteUtilTest {

  private static final UUID LICENCE_CORRECTION_ID = UUID.randomUUID();

  @Test
  void getPositionPageUrl_addedPosition() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
        .build();

    var result = LicencePositionCorrectionRouteUtil.getPositionPageUrl(LICENCE_CORRECTION_ID, positionCorrection);

    assertThat(result).isEqualTo(ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderAddedPosition(LICENCE_CORRECTION_ID, positionCorrection.getId(), null))
    );
  }

  @Test
  void getPositionPageUrl_updatedPosition() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .build();

    var result = LicencePositionCorrectionRouteUtil.getPositionPageUrl(LICENCE_CORRECTION_ID, positionCorrection);

    assertThat(result).isEqualTo(ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(LICENCE_CORRECTION_ID, positionCorrection.getTargetLicencePosition().getId(), null))
    );
  }

  @Test
  void getPositionPageUrl_removedPosition() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .build();

    assertThatThrownBy(() -> LicencePositionCorrectionRouteUtil.getPositionPageUrl(LICENCE_CORRECTION_ID, positionCorrection))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Licence position correction %s removes a position so cannot have changes made against it"
            .formatted(positionCorrection.getId())
        );
  }
}