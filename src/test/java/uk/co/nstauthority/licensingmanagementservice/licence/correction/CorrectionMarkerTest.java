package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;

class CorrectionMarkerTest {

  @ParameterizedTest
  @CsvSource({
      "ADD_POSITION, POSITION_ADDED",
      "UPDATE_POSITION, POSITION_CORRECTED",
      "REMOVE_POSITION, POSITION_REMOVED"
  })
  void forPosition(LicencePositionCorrectionChangeType changeType, CorrectionMarker expectedMarker) {
    assertThat(CorrectionMarker.forPosition(changeType)).isEqualTo(expectedMarker);
  }

  @ParameterizedTest
  @CsvSource({
      "add-change, CHANGE_ADDED",
      "update-change-operations, CHANGE_CORRECTED",
      "remove-change, CHANGE_REMOVED"
  })
  void forChange_whenChangeTypeIsTagged_returnsItsMarker(String changeType, CorrectionMarker expectedMarker) {
    assertThat(CorrectionMarker.forChange(changeType)).isEqualTo(expectedMarker);
  }

  @Test
  void forChange_whenChangeUntouchedByTheCorrection_returnsNoMarker() {
    assertThat(CorrectionMarker.forChange(null)).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {LicencePositionChangeType.UPDATE_CHANGE_ORDER, "something-else"})
  void forChange_whenChangeTypeIsNotTagged_returnsNoMarker(String changeType) {
    assertThat(CorrectionMarker.forChange(changeType)).isNull();
  }
}