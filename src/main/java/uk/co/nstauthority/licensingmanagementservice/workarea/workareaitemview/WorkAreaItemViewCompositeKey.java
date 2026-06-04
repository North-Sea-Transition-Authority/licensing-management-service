package uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

public class WorkAreaItemViewCompositeKey implements Serializable {

  @Serial
  private static final long serialVersionUID = 1483958263294632821L;

  private UUID itemId;
  private WorkAreaDataItemType itemType;
  private Long userId;

  public WorkAreaItemViewCompositeKey() {
    // Required by JPA for @IdClass
  }

  public UUID getItemId() {
    return itemId;
  }

  public void setItemId(UUID itemId) {
    this.itemId = itemId;
  }

  public WorkAreaDataItemType getItemType() {
    return itemType;
  }

  public void setItemType(WorkAreaDataItemType itemType) {
    this.itemType = itemType;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }
}
