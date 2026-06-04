package uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview;

import jakarta.transaction.Transactional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ClearDownWorkAreaLogService {

  private final WorkAreaItemViewRepository workAreaItemViewRepository;

  public ClearDownWorkAreaLogService(WorkAreaItemViewRepository workAreaItemViewRepository) {
    this.workAreaItemViewRepository = workAreaItemViewRepository;
  }

  @Transactional
  public void clearDownAllViewsFor(UUID itemId, WorkAreaDataItemType itemType) {
    workAreaItemViewRepository.deleteAllByItemIdAndItemType(itemId, itemType);
  }
}
