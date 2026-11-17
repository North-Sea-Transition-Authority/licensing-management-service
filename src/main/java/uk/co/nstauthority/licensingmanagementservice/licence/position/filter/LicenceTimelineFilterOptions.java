package uk.co.nstauthority.licensingmanagementservice.licence.position.filter;

import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.OrganisationNameContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;

/**
 * What a licence timeline can be filtered by: only the change types and organisations that appear on it.
 *
 * @param changeTypeOptions the change types, keyed by type with the display name as the value
 * @param organisationOptions the organisations, keyed by organisation unit id with the current name as the value,
 *                            ordered by name
 */
public record LicenceTimelineFilterOptions(
    Map<String, String> changeTypeOptions,
    Map<String, String> organisationOptions
) {

  public static LicenceTimelineFilterOptions none() {
    return new LicenceTimelineFilterOptions(Map.of(), Map.of());
  }

  /**
   * Offers every organisation on the timeline by the name it goes by today, since the filter spans every position
   * rather than the one being shown.
   */
  public static LicenceTimelineFilterOptions from(
      List<ChronologicalPosition> chronologicalPositions,
      OrganisationNameContext nameContext
  ) {
    var organisationOptions = ChronologicalPosition.organisationUnitIds(chronologicalPositions).stream()
        .map(organisationId -> Map.entry(
            String.valueOf(organisationId),
            nameContext.getNameForDate(organisationId, null, NOT_AVAILABLE)
        ))
        .sorted(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER))
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            (first, second) -> first,
            LinkedHashMap::new
        ));

    return new LicenceTimelineFilterOptions(
        LicenceTimelineChangeTypeFilter.getAvailableChangeTypeOptions(chronologicalPositions),
        organisationOptions
    );
  }
}
