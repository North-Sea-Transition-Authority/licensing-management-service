package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectPositionDateController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.ReinstateLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.RemoveExecutedLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.UndoLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.LicencePositionAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionAdministratorChangeUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.AdministratorChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateViewService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Service
public class LicencePositionViewService {

  private static final Comparator<TimelineEntry> TIMELINE_ORDER_COMPARATOR =
      Comparator.comparing(TimelineEntry::date).thenComparingInt(TimelineEntry::order).reversed();

  private static final Comparator<ChronologicalPosition> CHRONOLOGICAL_POSITION_COMPARATOR =
      Comparator.comparing(ChronologicalPosition::date).thenComparingInt(ChronologicalPosition::order);

  private final LicencePositionService licencePositionService;
  private final LicencePositionChangeService licencePositionChangeService;
  private final LicencePositionChangeViewService licencePositionChangeViewService;
  private final LicencePositionStateViewService licencePositionStateViewService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  public LicencePositionViewService(
      LicencePositionService licencePositionService,
      LicencePositionChangeService licencePositionChangeService,
      LicencePositionChangeViewService licencePositionChangeViewService,
      LicencePositionStateViewService licencePositionStateViewService,
      LicencePositionCorrectionService licencePositionCorrectionService,
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.licencePositionService = licencePositionService;
    this.licencePositionChangeService = licencePositionChangeService;
    this.licencePositionChangeViewService = licencePositionChangeViewService;
    this.licencePositionStateViewService = licencePositionStateViewService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  /**
   * Resolves the current and previous administrator ids for a position, along with their display names. Names are
   * resolved here so consumers do not need to look them up; an empty string is used when the id is null or the name
   * cannot be resolved.
   */
  public AdministratorChangeContext getAdministratorChangeContext(
      LicenceCorrection licenceCorrection,
      UUID licencePositionId
  ) {
    var chronologicalPositions = getCorrectedChronologicalPositions(licenceCorrection, licencePositionId);
    var currentAdministratorId =
        LicencePositionAdministratorChangeUtil.resolveCurrentAdministratorId(licencePositionId, chronologicalPositions);
    var previousAdministratorId =
        LicencePositionAdministratorChangeUtil.resolvePreviousAdministratorId(licencePositionId, chronologicalPositions);
    var administratorNames = resolveAdministratorNames(chronologicalPositions);

    return new AdministratorChangeContext(
        currentAdministratorId,
        previousAdministratorId,
        nameOrEmpty(administratorNames, currentAdministratorId),
        nameOrEmpty(administratorNames, previousAdministratorId)
    );
  }

  public LicencePositionPageView getPositionPageView(LicencePosition licencePosition) {
    var licence = licencePosition.getLicence();
    var executedChronologicalLicencePositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);
    var liveChronologicalPositions = getLiveChronologicalPositions(executedChronologicalLicencePositions);
    var administratorNames = resolveAdministratorNames(liveChronologicalPositions);

    return LicencePositionPageView.readOnly(
        getReadOnlyTimelineView(executedChronologicalLicencePositions),
        licencePosition.getFormattedPositionDate(),
        licencePosition.getLicenceTransaction().getRegulatorReference(),
        licencePositionChangeViewService.getChangeViews(licencePosition.getId(), liveChronologicalPositions, administratorNames),
        licencePositionStateViewService.getStateView(licencePosition.getId(), liveChronologicalPositions, administratorNames),
        licencePosition.getId()
    );
  }

  public LicencePositionPageView getCorrectionPositionPageView(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    var licence = licencePosition.getLicence();
    var executedChronologicalLicencePositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);
    var positionCorrections = licencePositionCorrectionService.getPositionCorrections(licenceCorrection);
    var removedPositionIds = removedPositionIds(positionCorrections);
    var addedPositionCorrections = correctionsOfType(positionCorrections, LicencePositionCorrectionChangeType.ADD_POSITION);
    var updatedCorrections = correctionsOfType(positionCorrections, LicencePositionCorrectionChangeType.UPDATE_POSITION);
    var updatedPositionPayloadsByTargetId = updatedPositionPayloadsByTargetId(updatedCorrections);
    var allChronologicalPositions = getCorrectedChronologicalPositions(
        executedChronologicalLicencePositions,
        removedPositionIds,
        updatedCorrections,
        addedPositionCorrections,
        licencePosition.getId()
    );
    var administratorNames = resolveAdministratorNames(allChronologicalPositions);

    var changeViews = licencePositionChangeViewService.getChangeViews(
        licencePosition.getId(), allChronologicalPositions, administratorNames);
    var adminChange = (AdministratorChangeView) changeViews.get(LicenceOperation.LICENCE_ADMINISTRATOR);
    var addUrl = ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
        .renderForExecutedPosition(licenceCorrection.getId(), licencePosition.getId(), null));

    var actions = new LicencePositionPageView.Actions(
        getAdministratorChangeUrl(licenceCorrection, licencePosition.getId(), adminChange, addUrl)
    );

    return LicencePositionPageView.fromExecutedPosition(
        getCorrectionTimelineView(
            executedChronologicalLicencePositions,
            licenceCorrection,
            removedPositionIds,
            updatedPositionPayloadsByTargetId,
            addedPositionCorrections
        ),
        licencePosition.getFormattedPositionDate(),
        licencePosition.getLicenceTransaction().getRegulatorReference(),
        changeViews,
        licencePositionStateViewService.getStateView(licencePosition.getId(), allChronologicalPositions, administratorNames),
        licencePosition.getId(),
        actions
    );
  }

  public LicencePositionPageView getCorrectionAddedPositionPageView(
      LicenceCorrection licenceCorrection,
      LicencePositionCorrection positionCorrection
  ) {
    var payload = (CreateLicencePositionPayload) positionCorrection.getPayload();
    var addedPositionId = UUID.fromString(payload.licencePositionId());
    var executedChronologicalLicencePositions =
        licencePositionService.getExecutedChronologicalLicencePositions(licenceCorrection.getLicence());
    var positionCorrections = licencePositionCorrectionService.getPositionCorrections(licenceCorrection);
    var removedPositionIds = removedPositionIds(positionCorrections);
    var addedPositionCorrections = correctionsOfType(positionCorrections, LicencePositionCorrectionChangeType.ADD_POSITION);
    var updatedCorrections = correctionsOfType(positionCorrections, LicencePositionCorrectionChangeType.UPDATE_POSITION);
    var updatedPositionPayloadsByTargetId = updatedPositionPayloadsByTargetId(updatedCorrections);
    var allChronologicalPositions = getCorrectedChronologicalPositions(
        executedChronologicalLicencePositions,
        removedPositionIds,
        updatedCorrections,
        addedPositionCorrections,
        addedPositionId
    );
    var administratorNames = resolveAdministratorNames(allChronologicalPositions);

    var changeViews = licencePositionChangeViewService.getChangeViews(
        addedPositionId, allChronologicalPositions, administratorNames);
    var adminChange = (AdministratorChangeView) changeViews.get(LicenceOperation.LICENCE_ADMINISTRATOR);
    var addUrl = ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
        .renderForAddedPosition(licenceCorrection.getId(), positionCorrection.getId(), null));

    var actions = new LicencePositionPageView.Actions(
        getAdministratorChangeUrl(licenceCorrection, addedPositionId, adminChange, addUrl)
    );

    return LicencePositionPageView.fromAddedPosition(
        getCorrectionTimelineView(
            executedChronologicalLicencePositions,
            licenceCorrection,
            removedPositionIds,
            updatedPositionPayloadsByTargetId,
            addedPositionCorrections
        ),
        DateUtil.formatLongDate(payload.effectiveDate()),
        payload.correctionReference(),
        changeViews,
        licencePositionStateViewService.getStateView(addedPositionId, allChronologicalPositions, administratorNames),
        addedPositionId,
        actions
    );
  }

  private List<ChronologicalPosition> getLiveChronologicalPositions(List<LicencePosition> executedChronologicalLicencePositions) {
    var liveChangesByPositionId = getLiveChangesByPositionId(executedChronologicalLicencePositions);

    return executedChronologicalLicencePositions.stream()
        .map(licencePosition -> ChronologicalPosition.fromLicencePosition(
            licencePosition,
            foldChanges(liveChangesByPositionId.getOrDefault(licencePosition.getId(), List.of()), List.of()))
        ).sorted(CHRONOLOGICAL_POSITION_COMPARATOR)
        .toList();
  }

  private List<ChronologicalPosition> getCorrectedChronologicalPositions(
      LicenceCorrection licenceCorrection,
      UUID currentLicencePositionId
  ) {
    var executedChronologicalLicencePositions =
        licencePositionService.getExecutedChronologicalLicencePositions(licenceCorrection.getLicence());
    var positionCorrections = licencePositionCorrectionService.getPositionCorrections(licenceCorrection);
    return getCorrectedChronologicalPositions(
        executedChronologicalLicencePositions,
        removedPositionIds(positionCorrections),
        correctionsOfType(positionCorrections, LicencePositionCorrectionChangeType.UPDATE_POSITION),
        correctionsOfType(positionCorrections, LicencePositionCorrectionChangeType.ADD_POSITION),
        currentLicencePositionId
    );
  }

  private List<ChronologicalPosition> getCorrectedChronologicalPositions(
      List<LicencePosition> executedChronologicalLicencePositions,
      Set<UUID> removedPositionIds,
      List<LicencePositionCorrection> updatedCorrections,
      List<LicencePositionCorrection> addedCorrections,
      UUID currentLicencePositionId
  ) {
    var liveChangesByPositionId = getLiveChangesByPositionId(executedChronologicalLicencePositions);

    var correctionChangesByPositionId = updatedCorrections
        .stream()
        .collect(Collectors.toMap(
            licencePositionCorrection -> licencePositionCorrection.getTargetLicencePosition().getId(),
            licencePositionCorrection -> licencePositionCorrection.getPayload().changes()
        ));

    var chronologicalPositions = new ArrayList<ChronologicalPosition>();

    // Positions removed in this correction are excluded from the state/change recalculation so their operations no
    // longer contribute, except for the position currently being viewed which is retained so its own page still renders.
    executedChronologicalLicencePositions.stream()
        .filter(position -> position.getId().equals(currentLicencePositionId)
            || !removedPositionIds.contains(position.getId()))
        .forEach(position -> {
          var changes = foldChanges(
              liveChangesByPositionId.getOrDefault(position.getId(), List.of()),
              correctionChangesByPositionId.getOrDefault(position.getId(), List.of())
          );
          chronologicalPositions.add(ChronologicalPosition.fromLicencePosition(position, changes));
        });

    addedCorrections.forEach(addedPosition ->
        chronologicalPositions.add(
            ChronologicalPosition.fromPayload((CreateLicencePositionPayload) addedPosition.getPayload())));

    chronologicalPositions.sort(CHRONOLOGICAL_POSITION_COMPARATOR);
    return chronologicalPositions;
  }

  private Map<Integer, String> resolveAdministratorNames(List<ChronologicalPosition> chronologicalPositions) {
    var administratorIds = LicencePositionAdministratorChangeUtil.administratorIdChangeByPositionId(chronologicalPositions)
        .values()
        .stream()
        .distinct()
        .toList();

    if  (administratorIds.isEmpty()) {
      return Collections.emptyMap();
    }

    return organisationUnitQueryService.getOrganisationUnitNamesByIds(administratorIds);
  }

  private static Set<UUID> removedPositionIds(List<LicencePositionCorrection> positionCorrections) {
    return positionCorrections.stream()
        .filter(correction -> correction.getChangeType() == LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .map(correction -> correction.getTargetLicencePosition().getId())
        .collect(Collectors.toSet());
  }

  private static List<LicencePositionCorrection> correctionsOfType(
      List<LicencePositionCorrection> positionCorrections,
      LicencePositionCorrectionChangeType changeType
  ) {
    return positionCorrections.stream()
        .filter(correction -> correction.getChangeType() == changeType)
        .toList();
  }

  private static Map<UUID, UpdateLicencePositionPayload> updatedPositionPayloadsByTargetId(
      List<LicencePositionCorrection> updatedCorrections
  ) {
    return updatedCorrections.stream()
        .collect(Collectors.toMap(
            positionCorrection -> positionCorrection.getTargetLicencePosition().getId(),
            positionCorrection -> (UpdateLicencePositionPayload) positionCorrection.getPayload()
        ));
  }

  private Map<UUID, List<LicencePositionChange>> getLiveChangesByPositionId(List<LicencePosition> licencePositions) {
    return licencePositionChangeService.findByLicencePositionIn(licencePositions).stream()
        .collect(Collectors.groupingBy(change -> change.getLicencePosition().getId()));
  }

  private static List<PositionChange> foldChanges(
      List<LicencePositionChange> liveChanges,
      List<LicencePositionChangeType> correctionChanges
  ) {
    //TODO - LMS2-84: remove executed operations

    var changesById = new HashMap<String, PositionChange>();

    PositionChange.fromLicencePositionChanges(liveChanges).forEach(positionChange ->
        changesById.put(positionChange.changeId(), positionChange)
    );

    PositionChange.fromCorrectionChanges(correctionChanges).forEach(positionChange -> {
      var liveChange = changesById.get(positionChange.changeId());
      if (liveChange == null) {
        changesById.put(positionChange.changeId(), positionChange);
      } else {
        changesById.put(positionChange.changeId(),
            new PositionChange(
                //TODO - will need to consider these when new change types are added
                liveChange.changeId(),
                liveChange.changeOrder(),
                positionChange.changeType(),
                positionChange.operations()
            ));
      }
    });

    return changesById.values().stream()
        .sorted(Comparator.comparing(PositionChange::changeOrder, Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
  }

  private List<LicencePositionTimelineView> getReadOnlyTimelineView(List<LicencePosition> chronologicalLicencePositions) {
    return chronologicalLicencePositions.stream()
        .filter(LicencePosition::isExecuted)
        .map(licencePosition -> new TimelineEntry(
            licencePosition.getPositionDate(),
            licencePosition.getPositionDateOrder(),
            baseTimelineViewBuilder(licencePosition, getPositionUrl(licencePosition)).build()
        ))
        .sorted(TIMELINE_ORDER_COMPARATOR)
        .map(TimelineEntry::view)
        .toList();
  }

  private List<LicencePositionTimelineView> getCorrectionTimelineView(
      List<LicencePosition> licencePositions,
      LicenceCorrection licenceCorrection,
      Set<UUID> removedPositionIds,
      Map<UUID, UpdateLicencePositionPayload> correctedPayloadsByPositionId,
      List<LicencePositionCorrection> addedCorrections
  ) {
    var livePositions =
        getLivePositionEntries(licencePositions, licenceCorrection, removedPositionIds, correctedPayloadsByPositionId);
    var addedPositions = getAddedPositionEntries(licenceCorrection, addedCorrections);

    return Stream.concat(livePositions.stream(), addedPositions.stream())
        .sorted(TIMELINE_ORDER_COMPARATOR)
        .map(TimelineEntry::view)
        .toList();
  }

  private List<TimelineEntry> getLivePositionEntries(
      List<LicencePosition> licencePositions,
      LicenceCorrection licenceCorrection,
      Set<UUID> removedPositionIds,
      Map<UUID, UpdateLicencePositionPayload> correctedPayloadsByPositionId
  ) {
    return licencePositions.stream()
        .filter(LicencePosition::isExecuted)
        .map(licencePosition -> {
          var removed = removedPositionIds.contains(licencePosition.getId());
          var correctedPayload = correctedPayloadsByPositionId.get(licencePosition.getId());
          var dateCorrected = correctedPayload != null && correctedPayload.effectiveDate() != null;
          var effectiveDate = dateCorrected
              ? correctedPayload.effectiveDate() : licencePosition.getPositionDate();
          var effectiveDateOrder = correctedPayload != null && correctedPayload.effectiveDateOrder() != null
              ? correctedPayload.effectiveDateOrder() : licencePosition.getPositionDateOrder();

          var timelineViewBuilder = baseTimelineViewBuilder(
              licencePosition, getCorrectionPositionUrl(licenceCorrection, licencePosition))
              .withFormattedPositionDate(DateUtil.formatLongDate(effectiveDate))
              .withCorrectedInThisCorrection(correctedPayload != null)
              .withRemovedInThisCorrection(removed);

          if (correctedPayload != null) {
            timelineViewBuilder.withRegulatorReference(correctedPayload.correctionReference());
          }

          if (removed) {
            timelineViewBuilder.withReinstateUrl(getReinstatePositionUrl(licenceCorrection, licencePosition));
          } else {
            timelineViewBuilder.withRemoveUrl(getRemovePositionUrl(licenceCorrection, licencePosition));
            timelineViewBuilder.withCorrectDateUrl(getCorrectDatePositionUrl(licenceCorrection, licencePosition));
          }

          return new TimelineEntry(effectiveDate, effectiveDateOrder, timelineViewBuilder.build());
        })
        .toList();
  }

  private List<TimelineEntry> getAddedPositionEntries(
      LicenceCorrection licenceCorrection,
      List<LicencePositionCorrection> addedCorrections
  ) {
    return addedCorrections
        .stream()
        .map(licencePositionCorrection -> {
          var payload = (CreateLicencePositionPayload) licencePositionCorrection.getPayload();
          var effectiveDate = payload.effectiveDate();
          return new TimelineEntry(
              effectiveDate,
              payload.effectiveDateOrder(),
              LicencePositionTimelineView.builder()
                  .withPositionId(UUID.fromString(payload.licencePositionId()))
                  .withUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
                      .renderAddedPosition(licenceCorrection.getId(), licencePositionCorrection.getId(), null)))
                  .withRegulatorReference(payload.correctionReference())
                  .withFormattedPositionDate(DateUtil.formatLongDate(effectiveDate))
                  .withAddedInThisCorrection(true)
                  .withUndoUrl(ReverseRouter.route(on(UndoLicencePositionCorrectionController.class)
                      .renderUndoPosition(licenceCorrection.getId(), licencePositionCorrection.getId(), null)))
                  .build()
          );
        })
        .toList();
  }

  private String getPositionUrl(LicencePosition licencePosition) {
    return ReverseRouter.route(on(LicencePositionController.class)
        .renderLicencePosition(licencePosition.getLicence(), licencePosition.getId()));
  }

  private String getCorrectionPositionUrl(LicenceCorrection correction, LicencePosition position) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction.getId(), position.getId(), correction));
  }

  private String getRemovePositionUrl(LicenceCorrection correction, LicencePosition position) {
    return ReverseRouter.route(on(RemoveExecutedLicencePositionCorrectionController.class)
        .renderRemovePosition(correction.getId(), position.getId(), null));
  }

  private String getReinstatePositionUrl(LicenceCorrection correction, LicencePosition position) {
    return ReverseRouter.route(on(ReinstateLicencePositionCorrectionController.class)
        .renderReinstatePosition(correction.getId(), position.getId(), null));
  }

  private String getCorrectDatePositionUrl(LicenceCorrection correction, LicencePosition position) {
    return ReverseRouter.route(on(CorrectPositionDateController.class)
        .renderCorrectLicencePositionCorrectionDate(correction.getId(), position.getId(), null));
  }

  private String getAdministratorChangeUrl(
      LicenceCorrection licenceCorrection, UUID licencePositionId, AdministratorChangeView adminChange, String addUrl
  ) {
    if (adminChange == null) {
      return addUrl;
    }
    if (adminChange.changeType() == null) {
      return ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
          .renderForCorrectingChange(licenceCorrection.getId(), licencePositionId, adminChange.changeId(), null));
    }
    // TODO LMS2-83: Modify an added or corrected admin change
    return null;
  }

  private LicencePositionTimelineView.Builder baseTimelineViewBuilder(LicencePosition position, String url) {
    return LicencePositionTimelineView.builder()
        .withPositionId(position.getId())
        .withUrl(url)
        .withRegulatorReference(position.getLicenceTransaction().getRegulatorReference())
        .withFormattedPositionDate(position.getFormattedPositionDate());
  }

  private static String nameOrEmpty(Map<Integer, String> administratorNames, @Nullable Integer administratorId) {
    return administratorId == null ? "" : administratorNames.getOrDefault(administratorId, "");
  }
}
