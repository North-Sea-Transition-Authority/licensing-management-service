package uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkAreaItemViewCompositeKeyTest {

  @Test
  void settersAndGetters_assertValues() {
    var itemId = UUID.randomUUID();
    var compositeKey = new WorkAreaItemViewCompositeKey();

    compositeKey.setItemId(itemId);
    compositeKey.setItemType(WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION);
    compositeKey.setUserId(123L);

    assertThat(compositeKey.getItemId()).isEqualTo(itemId);
    assertThat(compositeKey.getItemType()).isEqualTo(WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION);
    assertThat(compositeKey.getUserId()).isEqualTo(123L);
  }
}
