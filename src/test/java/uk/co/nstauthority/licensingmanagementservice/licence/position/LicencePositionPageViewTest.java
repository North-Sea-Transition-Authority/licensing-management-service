package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem;

class LicencePositionPageViewTest {

  @Test
  void empty_returnsPageViewWithEmptyDefaults() {
    var pageView = LicencePositionPageView.empty();

    assertThat(pageView.timelineViews()).isEmpty();
    assertThat(pageView.date()).isNull();
    assertThat(pageView.regulatorReference()).isNull();
    assertThat(pageView.changeViewByType()).isEmpty();
    assertThat(pageView.stateView()).isNull();
    assertThat(pageView.canEdit()).isFalse();
  }

  @Test
  void hasPositions_whenTimelineEmpty_returnsFalse() {
    assertThat(LicencePositionPageView.empty().hasPositions()).isFalse();
  }

  @Test
  void hasPositions_whenTimelineHasEntries_returnsTrue() {
    var pageView = new LicencePositionPageView(
        List.of(timelineView()), null, "REF-1" ,Map.of(), null, false, null, false,
        LicencePositionPageView.Actions.none(), List.of());

    assertThat(pageView.hasPositions()).isTrue();
  }

  private LicencePositionTimelineView timelineView() {
    return LicencePositionTimelineView.builder()
        .withPositionId(UUID.randomUUID())
        .withUrl("/url")
        .withRegulatorReference("REF-1")
        .withFormattedPositionDate("1 January 2026")
        .build();
  }

  @Test
  void readOnly_isNotEditableAndNotAdded() {
    var pageView = LicencePositionPageView.readOnly(
        List.of(), "1 Jan 2026", "REF-1", Map.of(), null, UUID.randomUUID());

    assertThat(pageView.canEdit()).isFalse();
    assertThat(pageView.isAddedPosition()).isFalse();
  }

  @Test
  void fromExecutedPosition_isEditableAndNotAdded() {
    var errorSummaryItems = List.of(new ErrorSummaryItem(0, "field", "message"));

    var pageView = LicencePositionPageView.fromExecutedPosition(
        List.of(), "1 Jan 2026", "REF-1", Map.of(), null, UUID.randomUUID(),
        LicencePositionPageView.Actions.none(), errorSummaryItems);

    assertThat(pageView.canEdit()).isTrue();
    assertThat(pageView.isAddedPosition()).isFalse();
    assertThat(pageView.errorSummaryItems()).isEqualTo(errorSummaryItems);
  }

  @Test
  void fromAddedPosition_isEditableAddedWithNoChangeOrStateViews() {
    var errorSummaryItems = List.of(new ErrorSummaryItem(0, "field", "message"));

    var pageView = LicencePositionPageView.fromAddedPosition(
        List.of(), "1 Jan 2026", "REF-1", Map.of(), null, UUID.randomUUID(),
        LicencePositionPageView.Actions.none(), errorSummaryItems);

    assertThat(pageView.canEdit()).isTrue();
    assertThat(pageView.isAddedPosition()).isTrue();
    assertThat(pageView.changeViewByType()).isEmpty();
    assertThat(pageView.stateView()).isNull();
    assertThat(pageView.errorSummaryItems()).isEqualTo(errorSummaryItems);
  }

  @Test
  void actions_none_hasNullUrl() {
    assertThat(LicencePositionPageView.Actions.none().addChangeUrl()).isNull();
  }
}