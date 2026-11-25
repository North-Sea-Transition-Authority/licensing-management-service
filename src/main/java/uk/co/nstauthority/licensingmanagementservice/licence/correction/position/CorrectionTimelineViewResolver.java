package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTimelineView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.IllegalUtilClassInstantiationException;

final class CorrectionTimelineViewResolver {

  private static final Comparator<ChronologicalPosition> TIMELINE_ORDER_COMPARATOR =
      Comparator.comparing(ChronologicalPosition::date).thenComparingInt(ChronologicalPosition::order).reversed();

  private CorrectionTimelineViewResolver() {
    throw new IllegalUtilClassInstantiationException(this.getClass());
  }

  static List<LicencePositionTimelineView> getTimelineViews(
      LicenceCorrection licenceCorrection,
      CorrectionTimelinePositions correctionPositions,
      Set<UUID> movedAwayPositionIds,
      Set<UUID> invalidPositionIds,
      Set<UUID> shownPositionIds
  ) {
    var correctionTimelinePositions = correctionPositions.correctionTimelinePositions();
    var executedPositionsById = correctionPositions.executedPositions().stream()
        .collect(Collectors.toMap(LicencePosition::getId, Function.identity()));
    var addedStagedPayloadsByPositionId = LicencePositionStagedPayloadService.stagedPayloadsOfType(
            correctionPositions.positionStagedPayloads(),
            LicencePositionStagedPayloadChangeType.ADD_POSITION
        ).stream()
        .collect(Collectors.toMap(CorrectionTimelineViewResolver::addedPositionId, Function.identity()));

    // Counted across every position so filtering never hides the action to correct the order of same-date positions.
    var sameDateCount = correctionTimelinePositions.stream()
        .filter(position -> !correctionPositions.removedPositionIds().contains(position.id()))
        .collect(Collectors.groupingBy(ChronologicalPosition::date, Collectors.counting()));

    return correctionTimelinePositions.stream()
        .filter(position -> shownPositionIds.contains(position.id()))
        .sorted(TIMELINE_ORDER_COMPARATOR)
        .map(position -> {
          var hasSameDatePositions = sameDateCount.getOrDefault(position.date(), 0L) > 1;
          var hasError = invalidPositionIds.contains(position.id());
          var addedStagedPayload = addedStagedPayloadsByPositionId.get(position.id());

          if (addedStagedPayload != null) {
            return getAddedPositionTimelineView(
                licenceCorrection,
                position,
                addedStagedPayload,
                hasError,
                hasSameDatePositions
            );
          }

          return getExecutedPositionTimelineView(
              licenceCorrection,
              position,
              executedPositionsById.get(position.id()),
              correctionPositions,
              movedAwayPositionIds.contains(position.id()),
              hasError,
              hasSameDatePositions
          );
        })
        .toList();
  }

  static UUID addedPositionId(LicencePositionStagedPayload addedStagedPayload) {
    return UUID.fromString(((CreateLicencePositionPayload) addedStagedPayload.getPayload()).licencePositionId());
  }

  private static LicencePositionTimelineView getExecutedPositionTimelineView(
      LicenceCorrection licenceCorrection,
      ChronologicalPosition position,
      LicencePosition licencePosition,
      CorrectionTimelinePositions correctionPositions,
      boolean movedAway,
      boolean hasError,
      boolean hasSameDatePositions
  ) {
    var removed = correctionPositions.removedPositionIds().contains(position.id());
    var correctedPayload = correctionPositions.updatedPositionPayloadsByTargetId().get(position.id());

    var timelineViewBuilder = LicencePositionTimelineView.builder()
        .withPositionId(position.id())
        .withUrl(LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(licenceCorrection, position.id()))
        .withRegulatorReference(position.reference())
        .withFormattedPositionDate(position.positionName())
        .withCorrectedInThisCorrection(hasPendingCorrection(correctedPayload) || movedAway)
        .withRemovedInThisCorrection(removed)
        .withHasError(hasError);

    if (removed) {
      timelineViewBuilder.withReinstateUrl(ReverseRouter.route(on(ReinstateLicencePositionStagedPayloadController.class)
          .renderReinstatePosition(licenceCorrection, licencePosition)));
    } else {
      timelineViewBuilder.withRemoveUrl(ReverseRouter.route(on(RemoveExecutedLicencePositionStagedPayloadController.class)
          .renderRemovePosition(licenceCorrection, licencePosition)));
      timelineViewBuilder.withCorrectDateUrl(ReverseRouter.route(on(CorrectPositionDateController.class)
          .renderCorrectLicencePositionStagedPayloadDate(licenceCorrection, licencePosition)));
      if (hasSameDatePositions) {
        timelineViewBuilder.withCorrectOrderUrl(getCorrectOrderPositionUrl(licenceCorrection, position.id()));
      }
    }

    return timelineViewBuilder.build();
  }

  private static LicencePositionTimelineView getAddedPositionTimelineView(
      LicenceCorrection licenceCorrection,
      ChronologicalPosition position,
      LicencePositionStagedPayload addedStagedPayload,
      boolean hasError,
      boolean hasSameDatePositions
  ) {
    var timelineViewBuilder = LicencePositionTimelineView.builder()
        .withPositionId(position.id())
        .withUrl(LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(licenceCorrection, position.id()))
        .withRegulatorReference(position.reference())
        .withFormattedPositionDate(position.positionName())
        .withAddedInThisCorrection(true)
        .withHasError(hasError)
        .withUndoUrl(ReverseRouter.route(on(UndoLicencePositionStagedPayloadController.class)
            .renderUndoPosition(licenceCorrection, addedStagedPayload)));

    if (hasSameDatePositions) {
      timelineViewBuilder.withCorrectOrderUrl(getCorrectOrderPositionUrl(licenceCorrection, position.id()));
    }

    return timelineViewBuilder.build();
  }

  private static String getCorrectOrderPositionUrl(LicenceCorrection licenceCorrection, UUID positionId) {
    return ReverseRouter.route(on(LicencePositionStagedPayloadOrderChangeController.class)
        .renderCorrectionLicencePositionOrder(licenceCorrection, positionId));
  }

  private static boolean hasPendingCorrection(@Nullable UpdateLicencePositionPayload correctedPayload) {
    return correctedPayload != null
        && (!correctedPayload.changes().isEmpty()
        || correctedPayload.effectiveDate() != null
        || correctedPayload.effectiveDateOrder() != null);
  }
}
