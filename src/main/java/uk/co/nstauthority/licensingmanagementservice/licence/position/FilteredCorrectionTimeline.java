package uk.co.nstauthority.licensingmanagementservice.licence.position;

/**
 * Where a filtered correction timeline opens: the latest position the filter matches, or a view saying that none do.
 */
public sealed interface FilteredCorrectionTimeline {

  record LatestMatchingPosition(String url) implements FilteredCorrectionTimeline {
  }

  record NoMatchingPositions(LicencePositionPageView pageView) implements FilteredCorrectionTimeline {
  }
}
