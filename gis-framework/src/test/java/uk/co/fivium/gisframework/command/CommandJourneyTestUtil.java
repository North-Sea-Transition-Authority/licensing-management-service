package uk.co.fivium.gisframework.command;

import java.util.UUID;

public class CommandJourneyTestUtil {

  private CommandJourneyTestUtil() {
    throw new IllegalStateException("Utility class should not be instantiated");
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private UUID id = UUID.randomUUID();

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public CommandJourney build() {
      var commandJourney = new CommandJourney();
      commandJourney.setId(id);
      return commandJourney;
    }
  }
}