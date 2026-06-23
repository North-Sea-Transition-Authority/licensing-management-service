package uk.co.nstauthority.licensingmanagementservice.teams;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface TeamRoleRepository extends ListCrudRepository<TeamRole, UUID>, NotDuplicationSource {
  List<TeamRole> findByWuaIdAndRole(Long wuaId, Role role);

  List<TeamRole> findByWuaIdAndTeam(Long wuaId, Team team);

  List<TeamRole> findByTeam(Team team);

  void deleteByWuaIdAndTeam(Long wuaId, Team team);

  boolean existsByTeamAndWuaId(Team team, Long wuaId);

  boolean existsByTeam(Team team);

  @EntityGraph(attributePaths = {"team"})
  List<TeamRole> findAllByWuaId(long wuaId);

  List<TeamRole> findAllByWuaIdAndRoleIn(long wuaId, Collection<Role> roles);

  List<TeamRole> findAllByRole(Role role);

  List<TeamRole> findAllByTeamAndRole(Team team, Role role);

  List<TeamRole> findAllByRoleIn(Collection<Role> roles);

  boolean existsByWuaId(long wuaId);

}
