package uk.co.nstauthority.licensingmanagementservice.teams;

public interface TeamScopeReference {
  String getId();

  String getType();

  static TeamScopeReference from(String id, String type) {
    return new TeamScopeReference() {
      @Override
      public String getId() {
        return id;
      }

      @Override
      public String getType() {
        return type;
      }
    };
  }
}
