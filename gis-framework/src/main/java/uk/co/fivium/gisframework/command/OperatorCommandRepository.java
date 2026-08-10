package uk.co.fivium.gisframework.command;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
interface OperatorCommandRepository extends ListCrudRepository<OperatorCommand, UUID> {

  @Query("SELECT MAX(command.commandOrder) FROM OperatorCommand command WHERE command.commandJourney = :commandJourney")
  Optional<Integer> findMaxCommandOrderByCommandJourney(CommandJourney commandJourney);
}
