package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;

class PositionChangeUrlContextTest {

  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder().build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder().build();

  @Test
  void correctionPosition_whenExecutedPosition_thenExecutedPosition() {
    var updateCorrection = LicencePositionCorrectionTestUtil.newBuilder().withLicenceCorrection(CORRECTION).build();
    var context = PositionChangeUrlContext.forExecutedPosition(CORRECTION, POSITION, updateCorrection);

    var result = context.correctionPosition();

    assertThat(result).isEqualTo(new CorrectionPosition.Executed(CORRECTION, POSITION));
  }

  @Test
  void correctionPosition_whenAddedPosition_thenStagedPosition() {
    var addCorrection = LicencePositionCorrectionTestUtil.newBuilder().withLicenceCorrection(CORRECTION).build();
    var context = PositionChangeUrlContext.forAddedPosition(CORRECTION, addCorrection);

    var result = context.correctionPosition();

    assertThat(result).isEqualTo(new CorrectionPosition.Staged(addCorrection));
  }

  @Test
  void correctionPosition_whenOnTheLicenceTimeline_thenThrows() {
    var context = PositionChangeUrlContext.forTimelinePosition(POSITION);

    assertThatThrownBy(context::correctionPosition)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("A position on the licence timeline is not being viewed within a correction");
  }
}
