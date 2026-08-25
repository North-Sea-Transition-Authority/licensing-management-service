package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class UpdateChangeOrderTest {

  @Test
  void builder() {
    var updateChangeOrder = LicencePositionChangeType.updateChangeOrder()
        .withChangeId("123")
        .withChangeOrder(2)
        .build();

    assertThat(updateChangeOrder.changeId()).isEqualTo("123");
    assertThat(updateChangeOrder.changeOrder()).isEqualTo(2);
    assertThat(updateChangeOrder.type()).isEqualTo(LicencePositionChangeType.UPDATE_CHANGE_ORDER);
  }

  @Test
  void operationsOf_returnsEmpty() {
    var updateChangeOrder = LicencePositionChangeType.updateChangeOrder()
        .withChangeId("123")
        .withChangeOrder(2)
        .build();

    assertThat(LicencePositionChangeType.operationsOf(updateChangeOrder)).isEqualTo(List.of());
  }
}