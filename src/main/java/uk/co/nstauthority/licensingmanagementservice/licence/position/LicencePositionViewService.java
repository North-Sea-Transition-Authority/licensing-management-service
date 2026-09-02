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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.LicencePositionValidationService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeViewResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateViewResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.PositionChangeUrlContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
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
  private final LicencePositionValidationService licencePositionValidationService;
  private final FeatureService featureService;

  public LicencePositionViewService(
      LicencePositionService licencePositionService,
      LicencePositionChangeService licencePositionChangeService,
      LicencePositionCorrectionService licencePositionCorrectionService,
      OrganisationUnitQueryService organisationUnitQueryService,
      LicencePositionValidationService licencePositionValidationService,
      FeatureService featureService
  ) {
    this.licencePositionService = licencePositionService;
    this.licencePositionChangeService = licencePositionChangeService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.licencePositionValidationService = licencePositionValidationService;
    this.featureService = featureService;
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
    var resolvedStates = LicencePositionStateResolver.resolve(chronologicalPositions);
    var currentState = resolvedStates.currentState(licencePositionId);
    var previousState = resolvedStates.previousState(licencePositionId);
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
    var resolvedStates = LicencePositionStateResolver.resolve(liveChronologicalPositions);
    var organisationNames = resolveOrganisationNames(liveChronologicalPositions);
    var featureNames = resolveFeatureNames(liveChronologicalPositions);

    return LicencePositionPageView.readOnly(
        getReadOnlyTimelineView(executedChronologicalLicencePositions),
        licencePosition.getFormattedPositionDate(),
        licencePosition.getLicenceTransaction().getRegulatorReference(),
        LicencePositionChangeViewResolver.getChangeViews(
            licencePosition.getId(),
            liveChronologicalPositions,
            resolvedStates,
            organisationNames,
            featureNames,
            null
        ),
        LicencePositionStateViewResolver.getStateView(
            licencePosition.getId(),
            resolvedStates,
            organisationNames
        ),
        licencePosition.getId(),
        licence.getType()
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

    // Removed position is kept in allChronologicalPositions (for rendering) it needs to be excluded from validation
    var currentPositionRemoved = removedPositionIds.contains(licencePosition.getId());
    var excludedFromState = currentPositionRemoved ? Set.of(licencePosition.getId()) : Set.<UUID>of();
    var resolvedStates = LicencePositionStateResolver.resolve(allChronologicalPositions, excludedFromState);
    var organisationNames = resolveOrganisationNames(allChronologicalPositions);
    var featureNames = resolveFeatureNames(allChronologicalPositions);

    var stagedPositionCorrectionId = updatedCorrections.stream()
        .filter(positionCorrection -> positionCorrection.getTargetLicencePosition().getId()
            .equals(licencePosition.getId()))
        .map(LicencePositionCorrection::getId)
        .findFirst()
        .orElse(null);

    var urlContext = currentPositionRemoved
        ? null
        : PositionChangeUrlContext.forExecutedPosition(
            licenceCorrection.getId(),
            licencePosition.getId(),
            stagedPositionCorrectionId);

    var changeViews = LicencePositionChangeViewResolver.getChangeViews(
        licencePosition.getId(),
        allChronologicalPositions,
        resolvedStates,
        organisationNames,
        featureNames,
        urlContext
    );

    var actions = currentPositionRemoved
        ? LicencePositionPageView.Actions.none()
        : new LicencePositionPageView.Actions(ReverseRouter.route(on(LicencePositionAddChangeController.class)
          .renderForExecutedPosition(licenceCorrection.getId(), licencePosition.getId(), null)));

    var validationPositions = currentPositionRemoved
        ? allChronologicalPositions.stream()
            .filter(position -> !position.id().equals(licencePosition.getId()))
            .toList()
        : allChronologicalPositions;

    var validationErrors = licencePositionValidationService.validate(
        validationPositions,
        resolvedStates,
        licencePosition.getLicence().getType() == LicenceType.CARBON_STORAGE
    );
    var errorSummaryItems = PositionValidationError.toErrorSummaryItems(validationErrors);
    var invalidPositionIds = invalidPositionIds(validationErrors);

    return LicencePositionPageView.fromExecutedPosition(
        getCorrectionTimelineView(
            executedChronologicalLicencePositions,
            licenceCorrection,
            removedPositionIds,
            updatedPositionPayloadsByTargetId,
            addedPositionCorrections,
            invalidPositionIds
        ),
        licencePosition.getFormattedPositionDate(),
        licencePosition.getLicenceTransaction().getRegulatorReference(),
        changeViews,
        LicencePositionStateViewResolver.getStateView(
            licencePosition.getId(),
            resolvedStates,
            organisationNames
        ),
        licencePosition.getId(),
        actions,
        licence.getType(),
        errorSummaryItems
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
    var resolvedStates = LicencePositionStateResolver.resolve(allChronologicalPositions);
    var organisationNames = resolveOrganisationNames(allChronologicalPositions);
    var featureNames = resolveFeatureNames(allChronologicalPositions);

    var changeViews = LicencePositionChangeViewResolver.getChangeViews(
        addedPositionId,
        allChronologicalPositions,
        resolvedStates,
        organisationNames,
        featureNames,
        PositionChangeUrlContext.forAddedPosition(licenceCorrection.getId(), positionCorrection.getId())
    );

    var addChangeUrl = ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForAddedPosition(licenceCorrection.getId(), positionCorrection.getId(), null));

    var actions = new LicencePositionPageView.Actions(addChangeUrl);

    var validationErrors = licencePositionValidationService.validate(
        allChronologicalPositions,
        resolvedStates,
        licenceCorrection.getLicence().getType() == LicenceType.CARBON_STORAGE
    );
    var errorSummaryItems = PositionValidationError.toErrorSummaryItems(validationErrors);
    var invalidPositionIds = invalidPositionIds(validationErrors);

    return LicencePositionPageView.fromAddedPosition(
        getCorrectionTimelineView(
            executedChronologicalLicencePositions,
            licenceCorrection,
            removedPositionIds,
            updatedPositionPayloadsByTargetId,
            addedPositionCorrections,
            invalidPositionIds
        ),
        DateUtil.formatLongDate(payload.effectiveDate()),
        payload.correctionReference(),
        changeViews,
        LicencePositionStateViewResolver.getStateView(addedPositionId, resolvedStates, organisationNames),
        addedPositionId,
        actions,
        licenceCorrection.getLicence().getType(),
        errorSummaryItems
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

  public List<ChronologicalPosition> getCorrectedChronologicalPositions(
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

  private static Set<UUID> invalidPositionIds(List<PositionValidationError> validationErrors) {
    return validationErrors.stream()
        .map(PositionValidationError::positionId)
        .collect(Collectors.toSet());
  }

  private Map<Integer, String> resolveOrganisationNames(List<ChronologicalPosition> chronologicalPositions) {
    var organisationIds = resolveIds(chronologicalPositions, LicencePositionViewService::organisationIds);

    if (organisationIds.isEmpty()) {
      return Collections.emptyMap();
    }

    return organisationUnitQueryService.getOrganisationUnitNamesByIds(organisationIds);
  }

  private Map<UUID, String> resolveFeatureNames(List<ChronologicalPosition> chronologicalPositions) {
    var featureIds = resolveIds(chronologicalPositions, LicencePositionViewService::featureIds);

    if (featureIds.isEmpty()) {
      return Collections.emptyMap();
    }

    return featureService.getFeaturesByIds(featureIds)
        .stream()
        .collect(Collectors.toMap(Feature::getId, Feature::getFeatureName));
  }

  private static <I> List<I> resolveIds(
      List<ChronologicalPosition> chronologicalPositions,
      Function<LicenceOperation, List<I>> idExtractor
  ) {
    return chronologicalPositions.stream()
        .flatMap(chronologicalPosition -> chronologicalPosition.changes().stream())
        .flatMap(change -> change.operations().stream())
        .map(idExtractor)
        .flatMap(List::stream)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }

  private static List<UUID> featureIds(LicenceOperation operation) {
    return switch (operation) {
      case PartialSurrenderOperation partialSurrender -> partialSurrender.featureIds();
      case SubareaOperation subarea -> List.of(subarea.featureId());
      case AdministratorOperation ignored -> List.of();
      case SetEquityOperation ignored -> List.of();
      case TransferEquityOperation ignored -> List.of();
    };
  }

  private static List<Integer> organisationIds(LicenceOperation operation) {
    return switch (operation) {
      case AdministratorOperation administratorOperation -> List.of(administratorOperation.operatorId());
      case SetEquityOperation setEquityOperation -> List.of(setEquityOperation.transferTo());
      case TransferEquityOperation transfer -> List.of(transfer.transferFrom(), transfer.transferTo());
      case PartialSurrenderOperation ignored -> List.of();
      case SubareaOperation ignored -> List.of();
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
    var changesById = new HashMap<String, PositionChange>();

    PositionChange.fromLicencePositionChanges(liveChanges).forEach(positionChange ->
        changesById.put(positionChange.changeId(), positionChange)
    );

    PositionChange.fromCorrectionChanges(correctionChanges).forEach(positionChange -> {
      var existingChange = changesById.get(positionChange.changeId());
      if (existingChange == null) {
        changesById.put(positionChange.changeId(), positionChange);
      } else {
        changesById.put(positionChange.changeId(), mergeCorrectionOntoExisting(existingChange, positionChange));
      }
    });

    return changesById.values().stream()
        .sorted(Comparator.comparing(PositionChange::changeOrder, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(PositionChange::changeId))
        .toList();
  }

  private static PositionChange mergeCorrectionOntoExisting(PositionChange existing, PositionChange correction) {
    return switch (correction.changeType()) {
      case LicencePositionChangeType.REMOVE_CHANGE -> new PositionChange(
          existing.changeId(), existing.changeOrder(), LicencePositionChangeType.REMOVE_CHANGE, existing.operations());
      case LicencePositionChangeType.UPDATE_CHANGE_ORDER -> new PositionChange(
          existing.changeId(), correction.changeOrder(), existing.changeType(), existing.operations());
      default -> new PositionChange(
          existing.changeId(), existing.changeOrder(), correction.changeType(), correction.operations());
    };
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
      List<LicencePositionCorrection> addedCorrections,
      Set<UUID> invalidPositionIds
  ) {

    var sameDateCount = sameDateCountByEffectiveDate(
        licencePositions, removedPositionIds, correctedPayloadsByPositionId, addedCorrections);

    var livePositions =
        getLivePositionEntries(
            licencePositions,
            licenceCorrection,
            removedPositionIds,
            correctedPayloadsByPositionId,
            sameDateCount,
            invalidPositionIds
        );
    var addedPositions = getAddedPositionEntries(licenceCorrection, addedCorrections, sameDateCount, invalidPositionIds);

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
      Map<LocalDate, Long> sameDateCount,
      Set<UUID> invalidPositionIds
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
              .withFormattedPositionDate(DateUtil.formatLongDateWithOrder(effectiveDate, effectiveDateOrder))
              .withCorrectedInThisCorrection(hasPendingCorrection(correctedPayload))
              .withRemovedInThisCorrection(removed)
              .withHasError(invalidPositionIds.contains(licencePosition.getId()));

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
      Map<LocalDate, Long> sameDateCount,
      Set<UUID> invalidPositionIds
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
              .withFormattedPositionDate(DateUtil.formatLongDateWithOrder(effectiveDate, payload.effectiveDateOrder()))
              .withAddedInThisCorrection(true)
              .withHasError(invalidPositionIds.contains(addedPositionId))
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
        .renderLicencePosition(licencePosition.getLicence(), licencePosition.getId(), null));
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

  private String getCorrectOrderPositionUrl(LicenceCorrection correction, LicencePosition position) {
    return getCorrectOrderPositionUrl(correction, position.getId());
  }

  private String getCorrectOrderPositionUrl(LicenceCorrection correction, UUID positionId) {
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

  private static boolean hasPendingCorrection(@Nullable UpdateLicencePositionPayload correctedPayload) {
    return correctedPayload != null
        && (!correctedPayload.changes().isEmpty()
        || correctedPayload.effectiveDate() != null
        || correctedPayload.effectiveDateOrder() != null);
  }

  private LicencePositionTimelineView.Builder baseTimelineViewBuilder(LicencePosition position, String url) {
    return LicencePositionTimelineView.builder()
        .withPositionId(position.getId())
        .withUrl(url)
        .withRegulatorReference(position.getLicenceTransaction().getRegulatorReference())
        .withFormattedPositionDate(DateUtil.formatLongDateWithOrder(position.getPositionDate(), position.getPositionDateOrder()));
  }

  private static String nameOrEmpty(Map<Integer, String> administratorNames, @Nullable Integer administratorId) {
    return administratorId == null ? "" : administratorNames.getOrDefault(administratorId, "");
  }
}