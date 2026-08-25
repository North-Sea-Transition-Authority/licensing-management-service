package uk.co.fivium.gisframework.command;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
interface OperatorCommandRepository extends ListCrudRepository<OperatorCommand, UUID> {

  @Query("SELECT MAX(command.commandOrder) FROM OperatorCommand command WHERE command.commandJourney = :commandJourney")
  Optional<Integer> findMaxCommandOrderByCommandJourney(CommandJourney commandJourney);

  Optional<OperatorCommand> findFirstByCommandJourneyAndStatusOrderByCommandOrderDesc(CommandJourney commandJourney,
                                                                                      CommandStatus status);

  Optional<OperatorCommand> findFirstByCommandJourneyAndStatusOrderByCommandOrderAsc(CommandJourney commandJourney,
                                                                                     CommandStatus status);

  List<OperatorCommand> findAllByCommandJourneyAndStatus(CommandJourney commandJourney, CommandStatus status);

  List<OperatorCommand> findAllByCommandJourney(CommandJourney commandJourney);
}
