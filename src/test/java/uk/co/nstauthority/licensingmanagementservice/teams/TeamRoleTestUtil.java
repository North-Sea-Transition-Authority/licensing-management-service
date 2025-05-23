package uk.co.nstauthority.licensingmanagementservice.teams;

import java.util.UUID;

public class TeamRoleTestUtil {

  public TeamRoleTestUtil() {
    throw new IllegalStateException("Cannot instantiate static util class");
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private UUID id = UUID.randomUUID();

    private Team team;
    private Role role;
    private Long wuaId = 1L;

    public UUID getId() {
      return id;
    }

    public Builder withId(UUID uuid) {
      this.id = uuid;
      return this;
    }

    public Team getTeam() {
      return team;
    }

    public Builder withTeam(Team team) {
      this.team = team;
      return this;
    }

    public Role getRole() {
      return role;
    }

    public Builder withRole(Role role) {
      this.role = role;
      return this;
    }

    public Long getWuaId() {
      return wuaId;
    }

    public Builder withWuaId(Long wuaId) {
      this.wuaId = wuaId;
      return this;
    }

    public TeamRole build() {
      var teamRole = new TeamRole(id);
      teamRole.setRole(role);
      teamRole.setTeam(team);
      teamRole.setWuaId(wuaId);
      return teamRole;
    }
  }
}
