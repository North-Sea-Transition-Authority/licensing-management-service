package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
public record PearsScheduleMigrationQueueEntryId(UUID runId, String licenceReference) implements Serializable {
}
