package uk.co.nstauthority.licensingmanagementservice.teams;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface TeamRepository extends ListCrudRepository<Team, UUID>, NotDuplicationSource {
  List<Team> findByTeamType(TeamType teamType);

  Optional<Team> findByTeamTypeAndScopeTypeAndScopeId(TeamType teamType, String scopeType, String scopeId);

  List<Team> findByTeamTypeAndScopeTypeAndScopeIdIn(TeamType teamType, String scopeType, Collection<String> scopeId);
}
