package uk.co.fivium.gisframework.command;

import java.util.Set;
import java.util.UUID;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;

public class FeatureJourneyStateTestUtil {

  private FeatureJourneyStateTestUtil() {
    throw new IllegalStateException("Utility class should not be instantiated");
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private UUID id = UUID.randomUUID();
    private Feature feature = FeatureTestUtil.newBuilder().build();
    private CommandJourney commandJourney = CommandJourneyTestUtil.newBuilder().build();
    private OperatorCommand createdByCommand = null;
    private boolean active = true;
    private Set<UUID> inputFeatureIds = null;

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withFeature(Feature feature) {
      this.feature = feature;
      return this;
    }

    public Builder withCommandJourney(CommandJourney commandJourney) {
      this.commandJourney = commandJourney;
      return this;
    }

    public Builder withCreatedByCommand(OperatorCommand createdByCommand) {
      this.createdByCommand = createdByCommand;
      return this;
    }

    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public Builder withInputFeatureIds(Set<UUID> inputFeatureIds) {
      this.inputFeatureIds = inputFeatureIds;
      return this;
    }

    public FeatureJourneyState build() {
      var featureJourneyState = new FeatureJourneyState();
      featureJourneyState.setId(id);
      featureJourneyState.setFeature(feature);
      featureJourneyState.setCommandJourney(commandJourney);
      featureJourneyState.setCreatedByCommand(createdByCommand);
      featureJourneyState.setActive(active);
      featureJourneyState.setInputFeatureIds(inputFeatureIds);
      return featureJourneyState;
    }
  }
}
