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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
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
    var addedCorrectionsByPositionId = LicencePositionCorrectionService.correctionsOfType(
            correctionPositions.positionCorrections(),
            LicencePositionCorrectionChangeType.ADD_POSITION
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
          var addedCorrection = addedCorrectionsByPositionId.get(position.id());

          if (addedCorrection != null) {
            return getAddedPositionTimelineView(
                licenceCorrection,
                position,
                addedCorrection,
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

  static UUID addedPositionId(LicencePositionCorrection addedCorrection) {
    return UUID.fromString(((CreateLicencePositionPayload) addedCorrection.getPayload()).licencePositionId());
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
        .withUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(licenceCorrection, licencePosition, null)))
        .withRegulatorReference(position.reference())
        .withFormattedPositionDate(position.positionName())
        .withCorrectedInThisCorrection(hasPendingCorrection(correctedPayload) || movedAway)
        .withRemovedInThisCorrection(removed)
        .withHasError(hasError);

    if (removed) {
      timelineViewBuilder.withReinstateUrl(ReverseRouter.route(on(ReinstateLicencePositionCorrectionController.class)
          .renderReinstatePosition(licenceCorrection, licencePosition)));
    } else {
      timelineViewBuilder.withRemoveUrl(ReverseRouter.route(on(RemoveExecutedLicencePositionCorrectionController.class)
          .renderRemovePosition(licenceCorrection, licencePosition)));
      timelineViewBuilder.withCorrectDateUrl(ReverseRouter.route(on(CorrectPositionDateController.class)
          .renderCorrectLicencePositionCorrectionDate(licenceCorrection, licencePosition)));
      if (hasSameDatePositions) {
        timelineViewBuilder.withCorrectOrderUrl(getCorrectOrderPositionUrl(licenceCorrection, position.id()));
      }
    }

    return timelineViewBuilder.build();
  }

  private static LicencePositionTimelineView getAddedPositionTimelineView(
      LicenceCorrection licenceCorrection,
      ChronologicalPosition position,
      LicencePositionCorrection addedCorrection,
      boolean hasError,
      boolean hasSameDatePositions
  ) {
    var timelineViewBuilder = LicencePositionTimelineView.builder()
        .withPositionId(position.id())
        .withUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderAddedPosition(licenceCorrection, addedCorrection, null)))
        .withRegulatorReference(position.reference())
        .withFormattedPositionDate(position.positionName())
        .withAddedInThisCorrection(true)
        .withHasError(hasError)
        .withUndoUrl(ReverseRouter.route(on(UndoLicencePositionCorrectionController.class)
            .renderUndoPosition(licenceCorrection, addedCorrection)));

    if (hasSameDatePositions) {
      timelineViewBuilder.withCorrectOrderUrl(getCorrectOrderPositionUrl(licenceCorrection, position.id()));
    }

    return timelineViewBuilder.build();
  }

  private static String getCorrectOrderPositionUrl(LicenceCorrection licenceCorrection, UUID positionId) {
    return ReverseRouter.route(on(LicencePositionCorrectionOrderChangeController.class)
        .renderCorrectionLicencePositionOrder(licenceCorrection, positionId));
  }

  private static boolean hasPendingCorrection(@Nullable UpdateLicencePositionPayload correctedPayload) {
    return correctedPayload != null
        && (!correctedPayload.changes().isEmpty()
        || correctedPayload.effectiveDate() != null
        || correctedPayload.effectiveDateOrder() != null);
  }
}
