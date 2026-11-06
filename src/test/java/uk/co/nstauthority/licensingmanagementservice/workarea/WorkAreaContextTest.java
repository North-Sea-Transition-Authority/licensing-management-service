package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemView;

class WorkAreaContextTest {

  private static final UUID ITEM_ID = UUID.randomUUID();
  private static final long WUA_ID = 1L;

  @Test
  void getOrganisationGroupIdsForLicence_returnsGroupsOfTheLicenseesThatHaveOne() {
    var licence = new Licence();
    licence.setId(1);
    var context = WorkAreaContextTestUtil.builder()
        .withLicenseesByLicence(Map.of(licence, List.of(
            new OrganisationUnit(1, "Org 1"),
            new OrganisationUnit(2, "Org 2"),
            new OrganisationUnit(3, "Org 3")
        )))
        .withOrganisationGroupIdsByUnitId(Map.of(1, 10, 2, 20, 4, 40))
        .build();

    var result = context.getOrganisationGroupIdsForLicence(licence);

    assertThat(result).isEqualTo(Map.of(1, 10, 2, 20));
  }

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
