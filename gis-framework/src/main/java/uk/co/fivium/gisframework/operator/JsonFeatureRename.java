package uk.co.fivium.gisframework.operator;

import java.util.UUID;

public record JsonFeatureRename(
    UUID featureId,
    String featureName,
    String name
) {
  public JsonFeatureRename {
    featureName = featureName == null ? null : featureName.trim();
    name = name == null ? null : name.trim();
  }
}
