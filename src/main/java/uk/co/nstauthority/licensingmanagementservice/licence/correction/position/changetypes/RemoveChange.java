package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes;

public record RemoveChange(
    String changeId
) implements LicencePositionChangeType {

  @Override
  public String type() {
    return REMOVE_CHANGE;
  }

  public static class Builder {

    private String changeId;

    public Builder withChangeId(String changeId) {
      this.changeId = changeId;
      return this;
    }

    public RemoveChange build() {
      return new RemoveChange(
          changeId
      );
    }
  }
}
