package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

class LicenseeChangeViewTest {

  @Test
  void type() {
    var currentView = new LicenseeChangeView(
        List.of("withdrawing1, withdrawing2"),
        List.of("joining1, joining2"),
        "add",
        ChangeViewUrls.none()
    );
    assertThat(currentView.type()).isEqualTo(LicenceOperation.LICENSEE);
  }
  @Test
  void merge() {
    var currentView = new LicenseeChangeView(
        List.of("withdrawing1, withdrawing2"),
        List.of("joining1, joining2"),
        "add",
        ChangeViewUrls.none()
    );
    var otherView = new LicenseeChangeView(
        List.of("withdrawing3, withdrawing4"),
        List.of("joining3, joining4"),
        "add",
        ChangeViewUrls.none()
    );
    var expectedView = new LicenseeChangeView(
        List.of("withdrawing1, withdrawing2", "withdrawing3, withdrawing4"),
        List.of("joining1, joining2", "joining3, joining4"),
        "add",
        ChangeViewUrls.none()
    );
    assertThat(currentView.merge(otherView)).isEqualTo(expectedView);
  }
}