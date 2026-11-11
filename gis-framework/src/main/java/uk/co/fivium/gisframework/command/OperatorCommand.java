package uk.co.fivium.gisframework.command;

import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;

/**
 * Records a single operator command (e.g. a split) performed against a {@link CommandJourney}'s features. The input
 * features each output was made from are held on the output's {@link FeatureJourneyState}.
 */
@Entity
@Table(name = "gis_framework_operator_commands")
@Audited
public class OperatorCommand {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "command_journey_id")
  private CommandJourney commandJourney;

  @Enumerated(EnumType.STRING)
  private CommandStatus status;

  @Enumerated(EnumType.STRING)
  private TransformationType transformationType;

  private Integer commandOrder;

  @VisibleForTesting
  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public CommandJourney getCommandJourney() {
    return commandJourney;
  }

  public void setCommandJourney(CommandJourney commandJourney) {
    this.commandJourney = commandJourney;
  }

  public CommandStatus getStatus() {
    return status;
  }

  public void setStatus(CommandStatus status) {
    this.status = status;
  }

  public TransformationType getTransformationType() {
    return transformationType;
  }

  public void setTransformationType(TransformationType transformationType) {
    this.transformationType = transformationType;
  }

  public Integer getCommandOrder() {
    return commandOrder;
  }

  public void setCommandOrder(Integer commandOrder) {
    this.commandOrder = commandOrder;
  }
}
