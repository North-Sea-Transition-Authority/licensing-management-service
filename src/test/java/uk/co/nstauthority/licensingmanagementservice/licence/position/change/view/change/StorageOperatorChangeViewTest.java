package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

class StorageOperatorChangeViewTest {

  @Test
  void type() {
    var currentView = new StorageOperatorChangeView(
        "withdrawing",
        "joining1",
        "changeId",
        "changeType",
        ChangeViewUrls.none()
    );
    assertThat(currentView.type()).isEqualTo(LicenceOperation.STORAGE_OPERATOR);
  }
}