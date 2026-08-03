package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionOrderChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.ReinstateLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.RemoveExecutedLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.UndoLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeViewResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateViewResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.PositionChangeUrlContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
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
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  public LicencePositionViewService(
      LicencePositionService licencePositionService,
      LicencePositionChangeService licencePositionChangeService,
      LicencePositionCorrectionService licencePositionCorrectionService,
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.licencePositionService = licencePositionService;
    this.licencePositionChangeService = licencePositionChangeService;
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
    var statesByChronologicalPositionId =
        LicencePositionStateResolver.resolveStatesByChronologicalPositionId(chronologicalPositions);
    var currentState = statesByChronologicalPositionId.getOrDefault(licencePositionId, LicencePositionState.EMPTY);
    var previousState = LicencePositionStateResolver.previousState(
        licencePositionId,
        chronologicalPositions,
        statesByChronologicalPositionId
    );
    var organisationNames = resolveOrganisationNames(chronologicalPositions);

    return new AdministratorChangeContext(
        currentState.administratorId(),
        previousState.administratorId(),
        nameOrEmpty(organisationNames, currentState.administratorId()),
        nameOrEmpty(organisationNames, previousState.administratorId())
    );
  }

  public LicencePositionPageView getPositionPageView(LicencePosition licencePosition) {
    var licence = licencePosition.getLicence();
    var executedChronologicalLicencePositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);
    var liveChronologicalPositions = getLiveChronologicalPositions(executedChronologicalLicencePositions);
    var statesByChronologicalPositionId =
        LicencePositionStateResolver.resolveStatesByChronologicalPositionId(liveChronologicalPositions);
    var organisationNames = resolveOrganisationNames(liveChronologicalPositions);

    return LicencePositionPageView.readOnly(
        getReadOnlyTimelineView(executedChronologicalLicencePositions),
        licencePosition.getFormattedPositionDate(),
        licencePosition.getLicenceTransaction().getRegulatorReference(),
        LicencePositionChangeViewResolver.getChangeViews(
            licencePosition.getId(),
            liveChronologicalPositions,
            statesByChronologicalPositionId,
            organisationNames,
            null
        ),
        LicencePositionStateViewResolver.getStateView(
            licencePosition.getId(),
            statesByChronologicalPositionId,
            organisationNames
        ),
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
    var statesByChronologicalPositionId =
        LicencePositionStateResolver.resolveStatesByChronologicalPositionId(allChronologicalPositions);
    var organisationNames = resolveOrganisationNames(allChronologicalPositions);

    var changeViews = LicencePositionChangeViewResolver.getChangeViews(
        licencePosition.getId(),
        allChronologicalPositions,
        statesByChronologicalPositionId,
        organisationNames,
        PositionChangeUrlContext.forExecutedPosition(licenceCorrection.getId(), licencePosition.getId())
    );

    var addUrl = ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForExecutedPosition(licenceCorrection.getId(), licencePosition.getId(), null));
    var actions = new LicencePositionPageView.Actions(addUrl);

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
        LicencePositionStateViewResolver.getStateView(
            licencePosition.getId(),
            statesByChronologicalPositionId,
            organisationNames
        ),
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
    var statesByChronologicalPositionId =
        LicencePositionStateResolver.resolveStatesByChronologicalPositionId(allChronologicalPositions);
    var organisationNames = resolveOrganisationNames(allChronologicalPositions);

    var changeViews = LicencePositionChangeViewResolver.getChangeViews(
        addedPositionId,
        allChronologicalPositions,
        statesByChronologicalPositionId,
        organisationNames,
        PositionChangeUrlContext.forAddedPosition(licenceCorrection.getId(), positionCorrection.getId())
    );

    var addChangeUrl = ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForAddedPosition(licenceCorrection.getId(), positionCorrection.getId(), null));

    var actions = new LicencePositionPageView.Actions(addChangeUrl);

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
        LicencePositionStateViewResolver.getStateView(addedPositionId, statesByChronologicalPositionId, organisationNames),
        addedPositionId,
        actions
    );
  }

  private List<ChronologicalPosition> getLiveChronologicalPositions(List<LicencePosition> executedChronologicalLicencePositions) {
    var liveChangesByPositionId = getLiveChangesByPositionId(executedChronologicalLicencePositions);

    return executedChronologicalLicencePositions.stream()
        .map(licencePosition -> ChronologicalPosition.fromLicencePosition(
            licencePosition,
            licencePosition.getPositionDate(),
            licencePosition.getPositionDateOrder(),
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

    var correctedPayloadsByPositionId = updatedPositionPayloadsByTargetId(updatedCorrections);

    var chronologicalPositions = new ArrayList<ChronologicalPosition>();

    // Positions removed in this correction are excluded from the state/change recalculation so their operations no
    // longer contribute, except for the position currently being viewed which is retained so its own page still renders.
    executedChronologicalLicencePositions.stream()
        .filter(position -> position.getId().equals(currentLicencePositionId)
            || !removedPositionIds.contains(position.getId()))
        .forEach(position -> {
          var correctedPayload = correctedPayloadsByPositionId.get(position.getId());
          var changes = foldChanges(
              liveChangesByPositionId.getOrDefault(position.getId(), List.of()),
              correctedPayload != null ? correctedPayload.changes() : List.of()
          );
          chronologicalPositions.add(ChronologicalPosition.fromLicencePosition(
              position,
              effectiveDate(position, correctedPayloadsByPositionId),
              effectiveDateOrder(position, correctedPayloadsByPositionId),
              changes
          ));
        });

    addedCorrections.forEach(addedPosition ->
        chronologicalPositions.add(
            ChronologicalPosition.fromPayload((CreateLicencePositionPayload) addedPosition.getPayload())));

    chronologicalPositions.sort(CHRONOLOGICAL_POSITION_COMPARATOR);
    return chronologicalPositions;
  }

  private Map<Integer, String> resolveOrganisationNames(List<ChronologicalPosition> chronologicalPositions) {
    var organisationIds = chronologicalPositions.stream()
        .flatMap(chronologicalPosition -> chronologicalPosition.changes().stream())
        .flatMap(change -> change.operations().stream())
        .map(LicencePositionViewService::organisationId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();

    if (organisationIds.isEmpty()) {
      return Collections.emptyMap();
    }

    return organisationUnitQueryService.getOrganisationUnitNamesByIds(organisationIds);
  }

  private static Integer organisationId(LicenceOperation operation) {
    return switch (operation) {
      case AdministratorOperation administratorOperation -> administratorOperation.operatorId();
      case SetEquityOperation setEquityOperation -> setEquityOperation.transferTo();
    };
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

    var sameDateCount = sameDateCountByEffectiveDate(
        licencePositions, removedPositionIds, correctedPayloadsByPositionId, addedCorrections);

    var livePositions =
        getLivePositionEntries(
            licencePositions,
            licenceCorrection,
            removedPositionIds,
            correctedPayloadsByPositionId,
            sameDateCount
        );
    var addedPositions = getAddedPositionEntries(licenceCorrection, addedCorrections, sameDateCount);

    return Stream.concat(livePositions.stream(), addedPositions.stream())
        .sorted(TIMELINE_ORDER_COMPARATOR)
        .map(TimelineEntry::view)
        .toList();
  }

  private Map<LocalDate, Long> sameDateCountByEffectiveDate(
      List<LicencePosition> licencePositions,
      Set<UUID> removedPositionIds,
      Map<UUID, UpdateLicencePositionPayload> correctedPayloadsByPositionId,
      List<LicencePositionCorrection> addedCorrections
  ) {
    var counts = licencePositions.stream()
        .filter(LicencePosition::isExecuted)
        .filter(position -> !removedPositionIds.contains(position.getId()))
        .map(position -> effectiveDate(position, correctedPayloadsByPositionId))
        .collect(Collectors.groupingBy(date -> date, Collectors.counting()));

    addedCorrections.stream()
        .map(correction -> (CreateLicencePositionPayload) correction.getPayload())
        .forEach(payload -> counts.merge(payload.effectiveDate(), 1L, Long::sum));

    return counts;
  }

  private List<TimelineEntry> getLivePositionEntries(
      List<LicencePosition> licencePositions,
      LicenceCorrection licenceCorrection,
      Set<UUID> removedPositionIds,
      Map<UUID, UpdateLicencePositionPayload> correctedPayloadsByPositionId,
      Map<LocalDate, Long> sameDateCount
  ) {
    return licencePositions.stream()
        .filter(LicencePosition::isExecuted)
        .map(licencePosition -> {
          var removed = removedPositionIds.contains(licencePosition.getId());
          var correctedPayload = correctedPayloadsByPositionId.get(licencePosition.getId());
          var effectiveDate = effectiveDate(licencePosition, correctedPayloadsByPositionId);
          var effectiveDateOrder = effectiveDateOrder(licencePosition, correctedPayloadsByPositionId);

          var correctionReference = correctedPayload != null ? correctedPayload.correctionReference() : null;

          var timelineViewBuilder = baseTimelineViewBuilder(
              licencePosition, getCorrectionPositionUrl(licenceCorrection, licencePosition))
              .withFormattedPositionDate(DateUtil.formatLongDate(effectiveDate))
              .withCorrectedInThisCorrection(correctedPayload != null)
              .withRemovedInThisCorrection(removed);

          if (correctionReference != null) {
            timelineViewBuilder.withRegulatorReference(correctionReference);
          }

          if (removed) {
            timelineViewBuilder.withReinstateUrl(getReinstatePositionUrl(licenceCorrection, licencePosition));
          } else {
            timelineViewBuilder.withRemoveUrl(getRemovePositionUrl(licenceCorrection, licencePosition));
            timelineViewBuilder.withCorrectDateUrl(getCorrectDatePositionUrl(licenceCorrection, licencePosition));
            if (sameDateCount.getOrDefault(effectiveDate, 0L) > 1) {
              timelineViewBuilder.withCorrectOrderUrl(getCorrectOrderPositionUrl(licenceCorrection, licencePosition));
            }
          }

          return new TimelineEntry(effectiveDate, effectiveDateOrder, timelineViewBuilder.build());
        })
        .toList();
  }

  private List<TimelineEntry> getAddedPositionEntries(
      LicenceCorrection licenceCorrection,
      List<LicencePositionCorrection> addedCorrections,
      Map<LocalDate, Long> sameDateCount
  ) {
    return addedCorrections
        .stream()
        .map(licencePositionCorrection -> {
          var payload = (CreateLicencePositionPayload) licencePositionCorrection.getPayload();
          var effectiveDate = payload.effectiveDate();
          var addedPositionId = UUID.fromString(payload.licencePositionId());

          var timelineViewBuilder = LicencePositionTimelineView.builder()
              .withPositionId(addedPositionId)
              .withUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
                  .renderAddedPosition(licenceCorrection.getId(), licencePositionCorrection.getId(), null)))
              .withRegulatorReference(payload.correctionReference())
              .withFormattedPositionDate(DateUtil.formatLongDate(effectiveDate))
              .withAddedInThisCorrection(true)
              .withUndoUrl(ReverseRouter.route(on(UndoLicencePositionCorrectionController.class)
                  .renderUndoPosition(licenceCorrection.getId(), licencePositionCorrection.getId(), null)));

          if (sameDateCount.getOrDefault(effectiveDate, 0L) > 1) {
            timelineViewBuilder.withCorrectOrderUrl(getCorrectOrderPositionUrl(licenceCorrection, addedPositionId));
          }

          return new TimelineEntry(effectiveDate, payload.effectiveDateOrder(), timelineViewBuilder.build());
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

  public String getCorrectOrderPositionUrl(LicenceCorrection correction, LicencePosition position) {
    return getCorrectOrderPositionUrl(correction, position.getId());
  }

  public String getCorrectOrderPositionUrl(LicenceCorrection correction, UUID positionId) {
    return ReverseRouter.route(on(LicencePositionCorrectionOrderChangeController.class)
        .renderCorrectionLicencePositionOrder(correction.getId(), positionId, null));
  }

  private LocalDate effectiveDate(
      LicencePosition position,
      Map<UUID, UpdateLicencePositionPayload> correctedPayloadsByPositionId
  ) {
    var correctedPayload = correctedPayloadsByPositionId.get(position.getId());
    return correctedPayload != null && correctedPayload.effectiveDate() != null
        ? correctedPayload.effectiveDate() : position.getPositionDate();
  }

  private int effectiveDateOrder(
      LicencePosition position,
      Map<UUID, UpdateLicencePositionPayload> correctedPayloadsByPositionId
  ) {
    var correctedPayload = correctedPayloadsByPositionId.get(position.getId());
    return correctedPayload != null && correctedPayload.effectiveDateOrder() != null
        ? correctedPayload.effectiveDateOrder() : position.getPositionDateOrder();
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