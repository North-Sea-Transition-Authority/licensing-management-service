package uk.co.nstauthority.licensingmanagementservice.licence.position;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.GeospatialLicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.OrganisationNameContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureSummary;

@Service
public class LicencePositionNameService {

  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final FeatureService featureService;

  public LicencePositionNameService(
      OrganisationUnitQueryService organisationUnitQueryService,
      FeatureService featureService
  ) {
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.featureService = featureService;
  }

  /**
   * Names every organisation on the timeline, not just those left in the selected position's state, because a change
   * can name an organisation that has since given up its interest.
   */
  public OrganisationNameContext getOrganisationNameContext(List<ChronologicalPosition> chronologicalPositions) {
    var organisationIds = ChronologicalPosition.organisationUnitIds(chronologicalPositions);

    return OrganisationNameContext.from(organisationUnitQueryService.getOrganisationNameHistoriesByIds(organisationIds));
  }

  public Map<UUID, FeatureSummary> resolveFeatures(List<ChronologicalPosition> chronologicalPositions) {
    var featureIds = chronologicalPositions.stream()
        .flatMap(chronologicalPosition -> chronologicalPosition.changes().stream())
        .flatMap(change -> change.operations().stream())
        .filter(GeospatialLicenceOperation.class::isInstance)
        .map(GeospatialLicenceOperation.class::cast)
        .flatMap(LicencePositionNameService::featureIdsToResolve)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    if (featureIds.isEmpty()) {
      return Collections.emptyMap();
    }

    return featureService.getFeaturesByIds(featureIds)
        .stream()
        .collect(Collectors.toMap(Feature::getId, FeatureSummary::from));
  }

  private static Stream<UUID> featureIdsToResolve(GeospatialLicenceOperation operation) {
    if (operation instanceof PartialSurrenderOperation partialSurrender) {
      return Stream.concat(partialSurrender.featureIds().stream(), partialSurrender.outputRetainedFeatureIds().stream());
    }
    return operation.featureIds().stream();
  }
}
