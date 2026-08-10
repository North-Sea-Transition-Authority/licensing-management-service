package uk.co.fivium.gisframework.command;

import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
interface CommandJourneyRepository extends ListCrudRepository<CommandJourney, UUID> {
}
