package uk.co.nstauthority.licensingmanagementservice.migration.pears.operation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;

@Component
class PearsFeatureResolver {

  private final FeatureService featureService;

  PearsFeatureResolver(FeatureService featureService) {
    this.featureService = featureService;
  }

  Map<Integer, UUID> resolve(
      Set<Integer> siIds,
      MigrationNotes notes
  ) {
    if (siIds.isEmpty()) {
      return Map.of();
    }

    var featureIdsBySiId = new HashMap<Integer, UUID>();
    for (Feature feature : featureService.findAllByLegacyIdIn(siIds)) {
      featureIdsBySiId.put(feature.getLegacyId(), feature.getId());
    }

    for (var siId : siIds) {
      if (!featureIdsBySiId.containsKey(siId)) {
        notes.note("PEARS block or subarea has no migrated GIS feature, so no position can hold it");
      }
    }

    return featureIdsBySiId;
  }
}
