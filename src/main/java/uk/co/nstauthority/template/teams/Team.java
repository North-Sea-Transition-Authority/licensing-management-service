package uk.co.nstauthority.template.teams;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "teams")
@Audited
public class Team {

  @Id
  @UuidGenerator
  private UUID id;

  private String name;

  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  private TeamType teamType;

  private String scopeType;
  private String scopeId;

  public Team() {
  }

  public Team(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public TeamType getTeamType() {
    return teamType;
  }

  public void setTeamType(TeamType teamType) {
    this.teamType = teamType;
  }

  public String getScopeType() {
    return scopeType;
  }

  public void setScopeType(String scopeType) {
    this.scopeType = scopeType;
  }

  public String getScopeId() {
    return scopeId;
  }

  public void setScopeId(String scopeId) {
    this.scopeId = scopeId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Team team = (Team) o;
    return Objects.equals(id, team.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

}
