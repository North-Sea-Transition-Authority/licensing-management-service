package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.LicencePositionValidationService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.position.ChronologicalPositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionNameService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionPageView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeViewResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateViewResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.PositionChangeUrlContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilter;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Service
public class LicencePositionStagedPayloadViewService {

  private final LicencePositionService licencePositionService;
  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;
  private final LicencePositionStagedPayloadFilterService licencePositionStagedPayloadFilterService;
  private final ChronologicalPositionService chronologicalPositionService;
  private final LicencePositionNameService licencePositionNameService;
  private final LicencePositionValidationService licencePositionValidationService;
  private final LicenceStatusService licenceStatusService;

  public LicencePositionStagedPayloadViewService(
      LicencePositionService licencePositionService,
      LicencePositionStagedPayloadService licencePositionStagedPayloadService,
      LicencePositionStagedPayloadFilterService licencePositionStagedPayloadFilterService,
      ChronologicalPositionService chronologicalPositionService,
      LicencePositionNameService licencePositionNameService,
      LicencePositionValidationService licencePositionValidationService,
      LicenceStatusService licenceStatusService
  ) {
    this.licencePositionService = licencePositionService;
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
    this.licencePositionStagedPayloadFilterService = licencePositionStagedPayloadFilterService;
    this.chronologicalPositionService = chronologicalPositionService;
    this.licencePositionNameService = licencePositionNameService;
    this.licencePositionValidationService = licencePositionValidationService;
    this.licenceStatusService = licenceStatusService;
  }

  /**
   * The correction page view for a position on the correction timeline, whether executed or staged by the correction,
   * with the timeline limited to the positions the filter matches, the position itself and any position with a
   * validation error. The position's change views are never filtered, so every change on the position being corrected
   * can be edited. Validation always covers every position.
   */
  public LicencePositionPageView getCorrectionPositionPageView(
      CorrectionPosition correctionPosition,
      LicenceTimelineFilter filter
  ) {
    var licenceCorrection = correctionPosition.correction();
    var licence = licenceCorrection.getLicence();
    var correctionPositions = getCorrectionPositions(licenceCorrection, licence);

    var selectedPosition = switch (correctionPosition) {
      case CorrectionPosition.Executed executed -> executedSelectedPosition(executed, correctionPositions);
      case CorrectionPosition.Staged staged -> stagedSelectedPosition(staged);
    };

    return getCorrectionPageView(licenceCorrection, licence, correctionPositions, selectedPosition, filter);
  }

  private static SelectedPosition executedSelectedPosition(
      CorrectionPosition.Executed executed,
      CorrectionTimelinePositions correctionPositions
  ) {
    var licencePosition = executed.licencePosition();
    var positionRemoved = correctionPositions.removedPositionIds().contains(licencePosition.getId());

    var positionStagedPayload = LicencePositionStagedPayloadService.stagedPayloadsOfType(
            correctionPositions.positionStagedPayloads(),
            LicencePositionStagedPayloadChangeType.UPDATE_POSITION
        ).stream()
        .filter(stagedPayload -> stagedPayload.getTargetLicencePosition().getId()
            .equals(licencePosition.getId()))
        .findFirst()
        .orElse(null);

    var urlContext = positionRemoved
        ? null
        : PositionChangeUrlContext.forExecutedPosition(
            executed.correction(),
            licencePosition,
            positionStagedPayload
        );

    var actions = positionRemoved
        ? LicencePositionPageView.Actions.none()
        : new LicencePositionPageView.Actions(addChangeUrl(executed));

    return new SelectedPosition(
        licencePosition.getId(),
        licencePosition.getFormattedPositionDate(),
        licencePosition.getLicenceTransaction().getRegulatorReference(),
        urlContext,
        actions,
        false
    );
  }

  private static SelectedPosition stagedSelectedPosition(CorrectionPosition.Staged staged) {
    var positionStagedPayload = staged.licencePositionStagedPayload();
    var payload = (CreateLicencePositionPayload) positionStagedPayload.getPayload();

    return new SelectedPosition(
        staged.positionId(),
        DateUtil.formatLongDate(payload.effectiveDate()),
        payload.correctionReference(),
        PositionChangeUrlContext.forAddedPosition(staged.correction(), positionStagedPayload),
        new LicencePositionPageView.Actions(addChangeUrl(staged)),
        true
    );
  }

  private static String addChangeUrl(CorrectionPosition correctionPosition) {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class).renderForPosition(correctionPosition));
  }

  private LicencePositionPageView getCorrectionPageView(
      LicenceCorrection licenceCorrection,
      Licence licence,
      CorrectionTimelinePositions correctionPositions,
      SelectedPosition selectedPosition,
      LicenceTimelineFilter filter
  ) {
    var removedPositionIds = correctionPositions.removedPositionIds();
    var correctionTimelinePositions = correctionPositions.correctionTimelinePositions();
    var shownPositions =
        withoutRemovedPositions(correctionTimelinePositions, removedPositionIds, Set.of(selectedPosition.id()));
    var resolvedStates = LicencePositionStateResolver.resolve(shownPositions, removedPositionIds);
    var featuresById = licencePositionNameService.resolveFeatures(shownPositions);
    var nameContext = licencePositionNameService.getOrganisationNameContext(correctionTimelinePositions);
    var positionDate = ChronologicalPosition.effectivePositionDate(shownPositions, selectedPosition.id());

    var changeViews = LicencePositionChangeViewResolver.getChangeViews(
        selectedPosition.id(),
        shownPositions,
        resolvedStates,
        nameContext.getNamesForDate(positionDate),
        featuresById,
        selectedPosition.urlContext()
    );

    var validationErrors = licencePositionValidationService.validate(
        withoutRemovedPositions(correctionTimelinePositions, removedPositionIds, Set.of()),
        resolvedStates,
        correctionPositions.positionStagedPayloads(),
        licence.getType(),
        licenceStatusService.getCurrentStatus(licence)
    );
    var invalidPositionIds = PositionValidationError.toPositionIds(validationErrors);
    var errorSummaryItems = PositionValidationError.toErrorSummaryItems(validationErrors);

    var timelineViews = CorrectionTimelineViewResolver.getTimelineViews(
        licenceCorrection,
        correctionPositions,
        movedAwayPositionIds(shownPositions),
        invalidPositionIds,
        licencePositionStagedPayloadFilterService.getShownCorrectionPositionIds(
            correctionTimelinePositions,
            removedPositionIds,
            filter,
            selectedPosition.id(),
            invalidPositionIds
        )
    );

    var stateView = LicencePositionStateViewResolver.getStateView(
        selectedPosition.id(),
        licence.getType(),
        resolvedStates,
        nameContext,
        featuresById,
        positionDate
    );

    var filterOptions =
        licencePositionStagedPayloadFilterService.getCorrectionFilterOptions(correctionTimelinePositions, nameContext);

    if (selectedPosition.added()) {
      return LicencePositionPageView.fromAddedPosition(
          timelineViews,
          selectedPosition.date(),
          selectedPosition.reference(),
          changeViews,
          stateView,
          selectedPosition.id(),
          selectedPosition.actions(),
          licence.getType(),
          errorSummaryItems,
          filterOptions,
          !filter.isEmpty()
      );
    }

    return LicencePositionPageView.fromExecutedPosition(
        timelineViews,
        selectedPosition.date(),
        selectedPosition.reference(),
        changeViews,
        stateView,
        selectedPosition.id(),
        selectedPosition.actions(),
        licence.getType(),
        errorSummaryItems,
        filterOptions,
        !filter.isEmpty()
    );
  }

  private CorrectionTimelinePositions getCorrectionPositions(LicenceCorrection licenceCorrection, Licence licence) {
    var executedPositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);
    var positionStagedPayloads = licencePositionStagedPayloadService.getPositionStagedPayloads(licenceCorrection);
    var removedPositionIds = LicencePositionStagedPayloadService.getRemovedPositionIds(positionStagedPayloads);
    var updatedStagedPayloads = LicencePositionStagedPayloadService.stagedPayloadsOfType(
        positionStagedPayloads,
        LicencePositionStagedPayloadChangeType.UPDATE_POSITION
    );

    return new CorrectionTimelinePositions(
        executedPositions,
        positionStagedPayloads,
        removedPositionIds,
        LicencePositionStagedPayloadService.updatedPositionPayloadsByTargetId(updatedStagedPayloads),
        chronologicalPositionService.getCorrectedChronologicalPositions(
            executedPositions,
            positionStagedPayloads,
            removedPositionIds
        )
    );
  }

  private static List<ChronologicalPosition> withoutRemovedPositions(
      List<ChronologicalPosition> chronologicalPositions,
      Set<UUID> removedPositionIds,
      Set<UUID> retainedRemovedPositionIds
  ) {
    return chronologicalPositions.stream()
        .filter(position -> retainedRemovedPositionIds.contains(position.id())
            || !removedPositionIds.contains(position.id()))
        .toList();
  }

  private static Set<UUID> movedAwayPositionIds(List<ChronologicalPosition> chronologicalPositions) {
    return chronologicalPositions.stream()
        .filter(ChronologicalPosition::hasMovedAwayChange)
        .map(ChronologicalPosition::id)
        .collect(Collectors.toSet());
  }

  private record SelectedPosition(
      UUID id,
      String date,
      String reference,
      @Nullable PositionChangeUrlContext urlContext,
      LicencePositionPageView.Actions actions,
      boolean added
  ) {
  }
}
