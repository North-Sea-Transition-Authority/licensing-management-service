package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

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
        LicencePositionPageView.Actions.none());

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
    var pageView = LicencePositionPageView.fromExecutedPosition(
        List.of(), "1 Jan 2026", "REF-1", Map.of(), null, UUID.randomUUID(),
        LicencePositionPageView.Actions.none());

    assertThat(pageView.canEdit()).isTrue();
    assertThat(pageView.isAddedPosition()).isFalse();
  }

  @Test
  void fromNonExecutedPosition_isEditableAddedWithNoChangeOrStateViews() {
    var pageView = LicencePositionPageView.fromNonExecutedPosition(
        List.of(), "1 Jan 2026", "REF-1", UUID.randomUUID(),
        LicencePositionPageView.Actions.none());

    assertThat(pageView.canEdit()).isTrue();
    assertThat(pageView.isAddedPosition()).isTrue();
    assertThat(pageView.changeViewByType()).isEmpty();
    assertThat(pageView.stateView()).isNull();
  }

  @Test
  void actions_none_hasNullUrl() {
    assertThat(LicencePositionPageView.Actions.none().addAdministratorChangeUrl()).isNull();
  }
}