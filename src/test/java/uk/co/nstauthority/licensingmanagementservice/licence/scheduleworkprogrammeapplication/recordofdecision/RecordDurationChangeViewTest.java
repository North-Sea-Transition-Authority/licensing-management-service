package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RecordDurationChangeViewTest {

  @ParameterizedTest
  @CsvSource({
      "true, phase",
      "false, term"
  })
  void endedMessage(boolean isPhase, String expectedNoun) {
    var view = new RecordDurationChangeView(
        "id", "displayName", isPhase, "currentEndDate", "currentDuration", null, false, false, true);

    assertThat(view.endedMessage())
        .isEqualTo("This %s has already ended and cannot be changed".formatted(expectedNoun));
  }
}
