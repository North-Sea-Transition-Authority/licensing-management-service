package uk.co.fivium.gisframework.command;

import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;

/**
 * Represents an in-progress editing session scoped to one or more {@link uk.co.fivium.gisframework.feature.Feature}s
 * that are being transformed (e.g. split). {@link OperatorCommand}s performed against the journey's features
 * are recorded against it, allowing a future undo/redo implementation to replay or reverse them.
 */
@Entity
@Table(name = "gis_framework_command_journeys")
@Audited
public class CommandJourney {

  @Id
  @UuidGenerator
  private UUID id;

  @VisibleForTesting
  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }
}
