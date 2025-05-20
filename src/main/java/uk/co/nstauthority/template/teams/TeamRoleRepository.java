package uk.co.nstauthority.template.teams;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRoleRepository extends CrudRepository<TeamRole, UUID> {
  List<TeamRole> findByWuaIdAndRole(Long wuaId, Role role);

  List<TeamRole> findByWuaIdAndTeam(Long wuaId, Team team);

  List<TeamRole> findByTeam(Team team);

  void deleteByWuaIdAndTeam(Long wuaId, Team team);

  boolean existsByTeamAndWuaId(Team team, Long wuaId);

  List<TeamRole> findAllByWuaId(long wuaId);

  List<TeamRole> findAllByWuaIdAndRoleIn(long wuaId, Collection<Role> roles);

  List<TeamRole> findAllByRole(Role role);

  List<TeamRole> findAllByTeamAndRole(Team team, Role role);
}
