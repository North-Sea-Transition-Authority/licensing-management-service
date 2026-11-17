package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

class ExplorationOperatorChangeViewTest {

  @Test
  void type() {
    var changeView = new ExplorationOperatorChangeView(null, "", "", "", null);
    assertThat(changeView.type()).isEqualTo(LicenceOperation.EXPLORATION_OPERATOR);
  }
}