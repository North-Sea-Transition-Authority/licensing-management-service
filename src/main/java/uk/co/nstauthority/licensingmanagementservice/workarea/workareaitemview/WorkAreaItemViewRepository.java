package uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface WorkAreaItemViewRepository
    extends ListCrudRepository<WorkAreaItemView, WorkAreaItemViewCompositeKey>, NotDuplicationSource {

  Optional<WorkAreaItemView> findByUserIdAndItemIdAndItemType(Long userId, UUID itemId, WorkAreaDataItemType itemType);

  void deleteAllByItemIdAndItemType(UUID itemId, WorkAreaDataItemType itemType);

  List<WorkAreaItemView> findAllByItemType(WorkAreaDataItemType itemType);

  List<WorkAreaItemView> findAllByItemTypeInAndUserId(List<WorkAreaDataItemType> itemTypes, Long userId);
}
