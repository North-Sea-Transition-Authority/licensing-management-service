package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes;

public record UpdateChangeOrder(
    String changeId,
    Integer changeOrder
) implements LicencePositionChangeType {

  @Override
  public String type() {
    return UPDATE_CHANGE_ORDER;
  }

  public static class Builder {

    private String changeId;
    private Integer changeOrder;

    public Builder withChangeId(String changeId) {
      this.changeId = changeId;
      return this;
    }

    public Builder withChangeOrder(Integer changeOrder) {
      this.changeOrder = changeOrder;
      return this;
    }

    public UpdateChangeOrder build() {
      return new UpdateChangeOrder(changeId, changeOrder);
    }
  }
}