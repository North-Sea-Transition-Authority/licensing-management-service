package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeViewResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateViewResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.PositionChangeUrlContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilter;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilterOptions;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Service
public class LicencePositionViewService {

  private static final Comparator<LicencePosition> TIMELINE_ORDER_COMPARATOR =
      Comparator.comparing(LicencePosition::getPositionDate)
          .thenComparingInt(LicencePosition::getPositionDateOrder)
          .reversed();

  private final LicencePositionService licencePositionService;
  private final ChronologicalPositionService chronologicalPositionService;
  private final LicencePositionNameService licencePositionNameService;

  public LicencePositionViewService(
      LicencePositionService licencePositionService,
      ChronologicalPositionService chronologicalPositionService,
      LicencePositionNameService licencePositionNameService
  ) {
    this.licencePositionService = licencePositionService;
    this.chronologicalPositionService = chronologicalPositionService;
    this.licencePositionNameService = licencePositionNameService;
  }

  public LicencePositionPageView getPositionPageView(LicencePosition licencePosition) {
    return getPositionPageView(licencePosition, LicenceTimelineFilter.empty());
  }

  /**
   * The read-only page view for a position, with the timeline limited to the positions the filter matches and the
   * position's change views limited to the filter's change types. An empty filter applies no filter.
   */
  public LicencePositionPageView getPositionPageView(LicencePosition licencePosition, LicenceTimelineFilter filter) {
    var executedChronologicalLicencePositions =
        licencePositionService.getExecutedChronologicalLicencePositions(licencePosition.getLicence());
    var liveChronologicalPositions =
        chronologicalPositionService.getLiveChronologicalPositions(executedChronologicalLicencePositions);

    return getPositionPageView(
        licencePosition,
        executedChronologicalLicencePositions,
        liveChronologicalPositions,
        LicencePositionStateResolver.resolve(liveChronologicalPositions),
        filter
    );
  }

  /**
   * The read-only page view for the latest position on the licence the filter matches, or the latest position when
   * the filter is empty.
   */
  public LicencePositionPageView getLatestPositionPageView(Licence licence, LicenceTimelineFilter filter) {
    var executedChronologicalLicencePositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);

    if (executedChronologicalLicencePositions.isEmpty()) {
      return LicencePositionPageView.empty();
    }

    var liveChronologicalPositions =
        chronologicalPositionService.getLiveChronologicalPositions(executedChronologicalLicencePositions);
    var resolvedStates = LicencePositionStateResolver.resolve(liveChronologicalPositions);

    if (filter.isEmpty()) {
      return getPositionPageView(
          executedChronologicalLicencePositions.getLast(),
          executedChronologicalLicencePositions,
          liveChronologicalPositions,
          resolvedStates,
          filter
      );
    }

    var latestMatchingPositionId = liveChronologicalPositions.reversed().stream()
        .filter(position -> filter.matches(position, resolvedStates))
        .map(ChronologicalPosition::id)
        .findFirst();

    return latestMatchingPositionId
        .flatMap(positionId -> executedChronologicalLicencePositions.stream()
            .filter(position -> position.getId().equals(positionId))
            .findFirst())
        .map(position -> getPositionPageView(
            position,
            executedChronologicalLicencePositions,
            liveChronologicalPositions,
            resolvedStates,
            filter
        ))
        .orElseGet(() -> LicencePositionPageView.noMatchingPositions(
            licence.getType(),
            LicenceTimelineFilterOptions.from(
                liveChronologicalPositions,
                licencePositionNameService.getOrganisationNameContext(liveChronologicalPositions)
            )
        ));
  }

  public LicenceTimelineFilterOptions getFilterOptions(Licence licence) {
    var liveChronologicalPositions = chronologicalPositionService.getLiveChronologicalPositions(licence);

    return LicenceTimelineFilterOptions.from(
        liveChronologicalPositions,
        licencePositionNameService.getOrganisationNameContext(liveChronologicalPositions)
    );
  }

  private LicencePositionPageView getPositionPageView(
      LicencePosition licencePosition,
      List<LicencePosition> executedChronologicalLicencePositions,
      List<ChronologicalPosition> liveChronologicalPositions,
      ResolvedStates resolvedStates,
      LicenceTimelineFilter filter
  ) {
    var licence = licencePosition.getLicence();
    var featuresById = licencePositionNameService.resolveFeatures(liveChronologicalPositions);
    var nameContext = licencePositionNameService.getOrganisationNameContext(liveChronologicalPositions);
    var positionDate = ChronologicalPosition.effectivePositionDate(liveChronologicalPositions, licencePosition.getId());

    var matchingPositionIds = liveChronologicalPositions.stream()
        .filter(position -> filter.matches(position, resolvedStates))
        .map(ChronologicalPosition::id)
        .collect(Collectors.toSet());

    // Only the change type filter narrows the change views. A change view can merge several operations naming
    // different organisations, so a matching position shows every one of its changes to the organisation filter.
    var changeTypes = filter.changeTypes();
    var changeViews = LicencePositionChangeViewResolver.getChangeViews(
            licencePosition.getId(),
            liveChronologicalPositions,
            resolvedStates,
            nameContext.getNamesForDate(positionDate),
            featuresById,
            PositionChangeUrlContext.forTimelinePosition(licencePosition)
        )
        .stream()
        .filter(changeView -> changeTypes.isEmpty() || changeTypes.contains(changeView.type()))
        .toList();

    return LicencePositionPageView.readOnly(
        getReadOnlyTimelineView(executedChronologicalLicencePositions, matchingPositionIds),
        licencePosition.getFormattedPositionDate(),
        licencePosition.getLicenceTransaction().getRegulatorReference(),
        changeViews,
        LicencePositionStateViewResolver.getStateView(
            licencePosition.getId(),
            licence.getType(),
            resolvedStates,
            nameContext,
            featuresById,
            positionDate
        ),
        licencePosition.getId(),
        licence.getType(),
        LicenceTimelineFilterOptions.from(liveChronologicalPositions, nameContext),
        !filter.isEmpty()
    );
  }

  private List<LicencePositionTimelineView> getReadOnlyTimelineView(
      List<LicencePosition> chronologicalLicencePositions,
      Set<UUID> matchingPositionIds
  ) {
    return chronologicalLicencePositions.stream()
        .filter(position -> position.getStatus() == LicencePositionStatus.EXECUTED)
        .filter(position -> matchingPositionIds.contains(position.getId()))
        .sorted(TIMELINE_ORDER_COMPARATOR)
        .map(licencePosition -> LicencePositionTimelineView.builder()
            .withPositionId(licencePosition.getId())
            .withUrl(ReverseRouter.route(on(LicencePositionController.class)
                .renderLicencePosition(licencePosition.getLicence(), licencePosition, null, null)))
            .withRegulatorReference(licencePosition.getLicenceTransaction().getRegulatorReference())
            .withFormattedPositionDate(DateUtil.formatLongDateWithOrder(
                licencePosition.getPositionDate(),
                licencePosition.getPositionDateOrder()
            ))
            .build())
        .toList();
  }
}
