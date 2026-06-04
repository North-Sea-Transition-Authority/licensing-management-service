package uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkAreaItemViewServiceTest {

  @Mock
  private WorkAreaItemViewRepository workAreaItemViewRepository;

  @InjectMocks
  private WorkAreaItemViewService workAreaItemViewService;

  @Test
  void hasUserViewedItem_whenRecordExists_returnsTrue() {
    var view = new WorkAreaItemView(UUID.randomUUID(), WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION, 1L);

    when(workAreaItemViewRepository.findByUserIdAndItemIdAndItemType(view.getUserId(), view.getItemId(), view.getItemType()))
        .thenReturn(Optional.of(view));

    assertThat(workAreaItemViewService.hasUserViewedItem(view)).isTrue();
  }

  @Test
  void hasUserViewedItem_whenNoRecord_returnsFalse() {
    var view = new WorkAreaItemView(UUID.randomUUID(), WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION, 1L);

    when(workAreaItemViewRepository.findByUserIdAndItemIdAndItemType(view.getUserId(), view.getItemId(), view.getItemType()))
        .thenReturn(Optional.empty());

    assertThat(workAreaItemViewService.hasUserViewedItem(view)).isFalse();
  }

  @Test
  void logWorkAreaItemView_savesView() {
    var view = new WorkAreaItemView(UUID.randomUUID(), WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION, 5L);

    workAreaItemViewService.logWorkAreaItemView(view);

    verify(workAreaItemViewRepository).save(view);
  }

  @Test
  void getWorkAreaItemLogs_delegatesToRepository() {
    workAreaItemViewService.getWorkAreaItemLogs(WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION);

    verify(workAreaItemViewRepository).findAllByItemType(WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION);
  }

  @Test
  void getWorkAreaItemLogsForUser_delegatesToRepository() {
    var types = List.of(WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION);

    workAreaItemViewService.getWorkAreaItemLogsForUser(types, 2L);

    verify(workAreaItemViewRepository).findAllByItemTypeInAndUserId(types, 2L);
  }
}
