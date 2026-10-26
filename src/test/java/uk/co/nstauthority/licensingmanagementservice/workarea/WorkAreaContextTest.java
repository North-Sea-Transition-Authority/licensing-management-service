package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemView;

class WorkAreaContextTest {

  private static final UUID ITEM_ID = UUID.randomUUID();
  private static final long WUA_ID = 1L;

  @Test
  void isNewItem_whenItemViewed_thenFalse() {
    var context = WorkAreaContextTestUtil.builder()
        .withViewedItems(List.of(new WorkAreaItemView(ITEM_ID, WorkAreaDataItemType.LICENCE_CORRECTION, WUA_ID)))
        .build();

    var result = context.isNewItem(WorkAreaDataItemType.LICENCE_CORRECTION, ITEM_ID);

    assertThat(result).isFalse();
  }

  @Test
  void isNewItem_whenItemNotViewed_thenTrue() {
    var context = WorkAreaContextTestUtil.builder()
        .withViewedItems(List.of(new WorkAreaItemView(UUID.randomUUID(), WorkAreaDataItemType.LICENCE_CORRECTION, WUA_ID)))
        .build();

    var result = context.isNewItem(WorkAreaDataItemType.LICENCE_CORRECTION, ITEM_ID);

    assertThat(result).isTrue();
  }

  @Test
  void isNewItem_whenSameIdViewedAsDifferentItemType_thenTrue() {
    var context = WorkAreaContextTestUtil.builder()
        .withViewedItems(List.of(new WorkAreaItemView(ITEM_ID, WorkAreaDataItemType.DRAFT_LICENCE_SCHEDULE, WUA_ID)))
        .build();

    var result = context.isNewItem(WorkAreaDataItemType.LICENCE_CORRECTION, ITEM_ID);

    assertThat(result).isTrue();
  }
}
