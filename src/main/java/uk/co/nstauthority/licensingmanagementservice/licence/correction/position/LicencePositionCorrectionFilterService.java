package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.position.ChronologicalPositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionNameService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionPageView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.OrganisationNameContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilter;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilterOptions;

@Service
public class LicencePositionCorrectionFilterService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final ChronologicalPositionService chronologicalPositionService;
  private final LicencePositionNameService licencePositionNameService;

  public LicencePositionCorrectionFilterService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      ChronologicalPositionService chronologicalPositionService,
      LicencePositionNameService licencePositionNameService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.chronologicalPositionService = chronologicalPositionService;
    this.licencePositionNameService = licencePositionNameService;
  }

  public LicenceTimelineFilterOptions getCorrectionFilterOptions(LicenceCorrection licenceCorrection) {
    var positionCorrections = licencePositionCorrectionService.getPositionCorrections(licenceCorrection);
    var correctionTimelinePositions = chronologicalPositionService.getCorrectedChronologicalPositions(
        licenceCorrection,
        positionCorrections,
        LicencePositionCorrectionService.getRemovedPositionIds(positionCorrections)
    );

    return getCorrectionFilterOptions(
        correctionTimelinePositions,
        licencePositionNameService.getOrganisationNameContext(correctionTimelinePositions)
    );
  }

  /**
   * Where the correction timeline opens under the filter: the latest position the filter matches, whether executed or
   * added in the correction, or a view saying none match.
   */
  public FilteredCorrectionTimeline getFilteredCorrectionTimeline(
      LicenceCorrection licenceCorrection,
      LicenceTimelineFilter filter
  ) {
    var positionCorrections = licencePositionCorrectionService.getPositionCorrections(licenceCorrection);
    var removedPositionIds = LicencePositionCorrectionService.getRemovedPositionIds(positionCorrections);
    var correctionTimelinePositions =
        chronologicalPositionService.getCorrectedChronologicalPositions(
            licenceCorrection,
            positionCorrections,
            removedPositionIds
        );
    var matchingPositionIds =
        getFilteredCorrectionPositionIds(correctionTimelinePositions, removedPositionIds, filter);

    return correctionTimelinePositions.reversed().stream()
        .map(ChronologicalPosition::id)
        .filter(matchingPositionIds::contains)
        .findFirst()
        .<FilteredCorrectionTimeline>map(positionId -> new FilteredCorrectionTimeline.LatestMatchingPosition(
            LicencePositionCorrectionRouteUtil.getPositionPageUrl(licenceCorrection, positionId)
        ))
        .orElseGet(() -> new FilteredCorrectionTimeline.NoMatchingPositions(
            LicencePositionPageView.noMatchingPositions(
                licenceCorrection.getLicence().getType(),
                getCorrectionFilterOptions(
                    correctionTimelinePositions,
                    licencePositionNameService.getOrganisationNameContext(correctionTimelinePositions)
                )
            )
        ));
  }

  /**
   * Offers only the change types and organisations of changes that still apply once the correction is made, so a
   * change the correction removes or moves away is never offered.
   */
  LicenceTimelineFilterOptions getCorrectionFilterOptions(
      List<ChronologicalPosition> correctionTimelinePositions,
      OrganisationNameContext nameContext
  ) {
    return LicenceTimelineFilterOptions.from(withoutRemovedChanges(correctionTimelinePositions), nameContext);
  }

  /**
   * The positions shown on the correction timeline: those the filter matches, along with the selected position so its
   * correction actions stay reachable, and any position with a validation error so every link in the error summary
   * leads to a position on the page.
   */
  Set<UUID> getShownCorrectionPositionIds(
      List<ChronologicalPosition> correctionTimelinePositions,
      Set<UUID> removedPositionIds,
      LicenceTimelineFilter filter,
      UUID selectedPositionId,
      Set<UUID> invalidPositionIds
  ) {
    var shownPositionIds =
        new HashSet<>(getFilteredCorrectionPositionIds(correctionTimelinePositions, removedPositionIds, filter));
    shownPositionIds.add(selectedPositionId);
    shownPositionIds.addAll(invalidPositionIds);
    return shownPositionIds;
  }

  /**
   * The positions on the correction timeline the filter matches. A position removed in this correction still shows on
   * the timeline, so it matches on the changes it was executed with. A change this correction removes or moves to
   * another position no longer applies to the position, so it is never matched on. Removed positions are left out of
   * the state, so the administrator a removed administrator change replaces comes from the corrected positions before
   * it.
   */
  private static Set<UUID> getFilteredCorrectionPositionIds(
      List<ChronologicalPosition> correctionTimelinePositions,
      Set<UUID> removedPositionIds,
      LicenceTimelineFilter filter
  ) {
    if (filter.isEmpty()) {
      return correctionTimelinePositions.stream()
          .map(ChronologicalPosition::id)
          .collect(Collectors.toSet());
    }

    var matchablePositions = withoutRemovedChanges(correctionTimelinePositions);
    var resolvedStates = LicencePositionStateResolver.resolve(matchablePositions, removedPositionIds);

    return matchablePositions.stream()
        .filter(position -> filter.matches(position, resolvedStates))
        .map(ChronologicalPosition::id)
        .collect(Collectors.toSet());
  }

  private static List<ChronologicalPosition> withoutRemovedChanges(List<ChronologicalPosition> chronologicalPositions) {
    return chronologicalPositions.stream()
        .map(ChronologicalPosition::withoutRemovedChanges)
        .toList();
  }
}
