package uk.co.nstauthority.licensingmanagementservice.teams;

import java.util.UUID;

public class TeamTestUtil {

  public TeamTestUtil()  {
    throw new IllegalStateException("Cannot instantiate static util class");
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private UUID id = UUID.randomUUID();
    private String name = "Test example team";
    private TeamType teamType = TeamType.ORGANISATION;
    private String scopeType = "OU";
    private String scopeId = "200";

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withTeamType(TeamType teamType) {
      this.teamType = teamType;
      return this;
    }

    public Builder withScopeType(String scopeType) {
      this.scopeType = scopeType;
      return this;
    }

    public Builder withScopeId(String scopeId) {
      this.scopeId = scopeId;
      return this;
    }

    public Team build() {
      var team = new Team(id);
      team.setName(name);
      team.setTeamType(teamType);
      team.setScopeType(scopeType);
      team.setScopeId(scopeId);
      return team;
    }
  }
}
