package uk.co.nstauthority.licensingmanagementservice.licence.position.change;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LicenseeChangeContextTest {
  private static final LicenseeChangeContext LICENSEE_CHANGE_CONTEXT = new LicenseeChangeContext(
      List.of(3, 4),
      List.of(2),
      List.of(1, 2),
      List.of(1, 3, 4),
      Map.of(1, "Delta", 3, "Beta", 4, "Alpha")
  );

  @Test
  void currentJoiningLicenseeNames() {
    assertThat(LICENSEE_CHANGE_CONTEXT.currentJoiningLicenseeNames()).isEqualTo(List.of("Alpha", "Beta"));
  }

  @Test
  void currentWithdrawingLicenseeNames() {
    assertThat(LICENSEE_CHANGE_CONTEXT.currentWithdrawingLicenseeNames()).isEqualTo(List.of(""));
  }

  @Test
  void previousLicenseeNames() {
    assertThat(LICENSEE_CHANGE_CONTEXT.previousLicenseeNames()).isEqualTo(List.of("", "Delta"));
  }

  @Test
  void currentLicenseeNames() {
    assertThat(LICENSEE_CHANGE_CONTEXT.currentLicenseeNames()).isEqualTo(List.of("Alpha", "Beta", "Delta"));
  }
}