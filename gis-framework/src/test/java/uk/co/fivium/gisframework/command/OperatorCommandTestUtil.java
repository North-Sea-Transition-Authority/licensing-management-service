package uk.co.fivium.gisframework.command;

import java.util.UUID;

public class OperatorCommandTestUtil {

  private OperatorCommandTestUtil() {
    throw new IllegalStateException("Utility class should not be instantiated");
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private UUID id = UUID.randomUUID();
    private CommandJourney commandJourney = CommandJourneyTestUtil.newBuilder().build();
    private CommandStatus status = CommandStatus.ACTIVE;
    private TransformationType transformationType = TransformationType.SPLIT;
    private Integer commandOrder = 1;

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withCommandJourney(CommandJourney commandJourney) {
      this.commandJourney = commandJourney;
      return this;
    }

    public Builder withStatus(CommandStatus status) {
      this.status = status;
      return this;
    }

    public Builder withTransformationType(TransformationType transformationType) {
      this.transformationType = transformationType;
      return this;
    }

    public Builder withCommandOrder(Integer commandOrder) {
      this.commandOrder = commandOrder;
      return this;
    }

    public OperatorCommand build() {
      var operatorCommand = new OperatorCommand();
      operatorCommand.setId(id);
      operatorCommand.setCommandJourney(commandJourney);
      operatorCommand.setStatus(status);
      operatorCommand.setTransformationType(transformationType);
      operatorCommand.setCommandOrder(commandOrder);
      return operatorCommand;
    }
  }
}