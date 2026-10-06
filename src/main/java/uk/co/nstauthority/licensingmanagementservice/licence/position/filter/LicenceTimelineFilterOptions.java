package uk.co.nstauthority.licensingmanagementservice.licence.position.filter;

import java.util.Map;

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
}
