package uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview;

import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WorkAreaItemViewService {

  private final WorkAreaItemViewRepository workAreaItemViewRepository;

  public WorkAreaItemViewService(WorkAreaItemViewRepository workAreaItemViewRepository) {
    this.workAreaItemViewRepository = workAreaItemViewRepository;
  }

  public boolean hasUserViewedItem(WorkAreaItemView workAreaItemView) {
    return workAreaItemViewRepository.findByUserIdAndItemIdAndItemType(
        workAreaItemView.getUserId(),
        workAreaItemView.getItemId(),
        workAreaItemView.getItemType()
    ).isPresent();
  }

  @Transactional
  public void logWorkAreaItemView(WorkAreaItemView workAreaItemView) {
    workAreaItemViewRepository.save(workAreaItemView);
  }

  public List<WorkAreaItemView> getWorkAreaItemLogs(WorkAreaDataItemType itemType) {
    return workAreaItemViewRepository.findAllByItemType(itemType);
  }

  public List<WorkAreaItemView> getWorkAreaItemLogsForUser(List<WorkAreaDataItemType> itemTypes, Long userId) {
    return workAreaItemViewRepository.findAllByItemTypeInAndUserId(itemTypes, userId);
  }
}
