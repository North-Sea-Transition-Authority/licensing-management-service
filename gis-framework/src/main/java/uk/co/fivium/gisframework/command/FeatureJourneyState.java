package uk.co.fivium.gisframework.command;

import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.fivium.gisframework.feature.Feature;

/**
 * Bookkeeping for a {@link Feature}'s place in a {@link CommandJourney}: which journey it currently belongs to,
 * which {@link OperatorCommand} produced it (if any), and whether it is the current representation of that part
 * of the journey or has since been superseded by a later transformation.
 */
@Entity
@Table(name = "gis_framework_feature_journey_states")
@Audited
public class FeatureJourneyState {

  @Id
  @UuidGenerator
  private UUID id;

  @OneToOne
  @JoinColumn(name = "feature_id")
  private Feature feature;

  @ManyToOne
  @JoinColumn(name = "command_journey_id")
  private CommandJourney commandJourney;

  @ManyToOne
  @JoinColumn(name = "created_by_command_id")
  private OperatorCommand createdByCommand;

  private boolean active;

  @VisibleForTesting
  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public Feature getFeature() {
    return feature;
  }

  public void setFeature(Feature feature) {
    this.feature = feature;
  }

  public CommandJourney getCommandJourney() {
    return commandJourney;
  }

  public void setCommandJourney(CommandJourney commandJourney) {
    this.commandJourney = commandJourney;
  }

  public OperatorCommand getCreatedByCommand() {
    return createdByCommand;
  }

  public void setCreatedByCommand(OperatorCommand createdByCommand) {
    this.createdByCommand = createdByCommand;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
