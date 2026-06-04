package uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@IdClass(WorkAreaItemViewCompositeKey.class)
@Table(name = "work_area_item_views")
public class WorkAreaItemView {

  @Id
  private UUID itemId;

  @Id
  @Enumerated(EnumType.STRING)
  private WorkAreaDataItemType itemType;

  @Id
  private Long userId;

  public WorkAreaItemView() {
  }

  public WorkAreaItemView(UUID itemId, WorkAreaDataItemType itemType, Long userId) {
    this.itemId = itemId;
    this.itemType = itemType;
    this.userId = userId;
  }

  public UUID getItemId() {
    return itemId;
  }

  public WorkAreaDataItemType getItemType() {
    return itemType;
  }

  public Long getUserId() {
    return userId;
  }
}
