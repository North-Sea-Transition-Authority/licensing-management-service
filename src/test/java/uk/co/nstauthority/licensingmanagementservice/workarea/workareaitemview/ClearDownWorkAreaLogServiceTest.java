package uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview;

import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClearDownWorkAreaLogServiceTest {

  @Mock
  private WorkAreaItemViewRepository workAreaItemViewRepository;

  @InjectMocks
  private ClearDownWorkAreaLogService clearDownWorkAreaLogService;

  @Test
  void clearDownAllViewsFor_deletesAllRecordsForGivenItem() {
    var itemId = UUID.randomUUID();

    clearDownWorkAreaLogService.clearDownAllViewsFor(itemId, WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION);

    verify(workAreaItemViewRepository).deleteAllByItemIdAndItemType(itemId, WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION);
  }

  @Test
  void clearDownViewFor_deletesOnlyTheGivenUsersRecordForItem() {
    var itemId = UUID.randomUUID();

    clearDownWorkAreaLogService.clearDownViewFor(42L, itemId, WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION);

    verify(workAreaItemViewRepository)
        .deleteByUserIdAndItemIdAndItemType(42L, itemId, WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION);
  }
}
