package uk.co.fivium.gisframework.feature;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import uk.co.fivium.gisframework.command.CommandJourney;
import uk.co.fivium.gisframework.command.CommandJourneyTestUtil;
import uk.co.fivium.gisframework.command.OperatorCommand;
import uk.co.fivium.gisframework.command.OperatorCommandTestUtil;
import uk.co.fivium.grpc.gis.CoordinateSystem;

public class FeatureTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private UUID id = UUID.randomUUID();
    private Integer legacyId = 1;
    private String featureName = "Test Feature";
    private CoordinateSystem coordinateSystem = CoordinateSystem.ED50;
    private BigDecimal featureArea = BigDecimal.valueOf(100.0);
    private Feature parentFeature = null;
    private Map<String, String> attributes = Map.of();
    private LocalDate startDate = LocalDate.of(2020, 1, 1);
    private LocalDate endDate = LocalDate.of(2021, 1, 1);
    private CommandJourney commandJourney = CommandJourneyTestUtil.newBuilder().build();
    private OperatorCommand createdByCommand = OperatorCommandTestUtil.newBuilder().build();
    private Boolean active = true;

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withLegacyId(Integer legacyId) {
      this.legacyId = legacyId;
      return this;
    }

    public Builder withFeatureName(String featureName) {
      this.featureName = featureName;
      return this;
    }

    public Builder withCoordinateSystem(CoordinateSystem coordinateSystem) {
      this.coordinateSystem = coordinateSystem;
      return this;
    }

    public Builder withFeatureArea(BigDecimal featureArea) {
      this.featureArea = featureArea;
      return this;
    }

    public Builder withParentFeature(Feature parentFeature) {
      this.parentFeature = parentFeature;
      return this;
    }

    public Builder withAttributes(Map<String, String> attributes) {
      this.attributes = attributes;
      return this;
    }

    public Builder withStartDate(LocalDate startDate) {
      this.startDate = startDate;
      return this;
    }

    public Builder withEndDate(LocalDate endDate) {
      this.endDate = endDate;
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

    public Builder withActive(Boolean isActive) {
      this.active = isActive;
      return this;
    }

    public Feature build() {
      var feature = new Feature(id);
      feature.setLegacyId(legacyId);
      feature.setFeatureName(featureName);
      feature.setCoordinateSystem(coordinateSystem);
      feature.setFeatureArea(featureArea);
      feature.setParentFeature(parentFeature);
      feature.setAttributes(attributes);
      feature.setStartDate(startDate);
      feature.setEndDate(endDate);
      feature.setCommandJourney(commandJourney);
      feature.setCreatedByCommand(createdByCommand);
      feature.setActive(active);
      return feature;
    }
  }
}


