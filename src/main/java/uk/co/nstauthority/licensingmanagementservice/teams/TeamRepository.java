package uk.co.nstauthority.licensingmanagementservice.teams;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends CrudRepository<Team, UUID> {
  List<Team> findByTeamType(TeamType teamType);

  Optional<Team> findByTeamTypeAndScopeTypeAndScopeId(TeamType teamType, String scopeType, String scopeId);

  List<Team> findByTeamTypeAndScopeTypeAndScopeIdIn(TeamType teamType, String scopeType, Collection<String> scopeId);
}
