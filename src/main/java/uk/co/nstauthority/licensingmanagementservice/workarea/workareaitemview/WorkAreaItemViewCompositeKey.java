package uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkAreaItemViewCompositeKey that = (WorkAreaItemViewCompositeKey) o;
    return Objects.equals(itemId, that.itemId) && itemType == that.itemType && Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(itemId, itemType, userId);
  }
}
