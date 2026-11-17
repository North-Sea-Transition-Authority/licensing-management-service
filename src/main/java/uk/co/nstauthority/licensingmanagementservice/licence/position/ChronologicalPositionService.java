package uk.co.nstauthority.licensingmanagementservice.licence.position;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;

@Service
public class ChronologicalPositionService {

  private static final Comparator<ChronologicalPosition> CHRONOLOGICAL_POSITION_COMPARATOR =
      Comparator.comparing(ChronologicalPosition::date).thenComparingInt(ChronologicalPosition::order);

  private final LicencePositionService licencePositionService;
  private final LicencePositionChangeService licencePositionChangeService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;

  public ChronologicalPositionService(
      LicencePositionService licencePositionService,
      LicencePositionChangeService licencePositionChangeService,
      LicencePositionCorrectionService licencePositionCorrectionService
  ) {
    this.licencePositionService = licencePositionService;
    this.licencePositionChangeService = licencePositionChangeService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
  }

  public List<ChronologicalPosition> getLiveChronologicalPositions(Licence licence) {
    return getLiveChronologicalPositions(licencePositionService.getExecutedChronologicalLicencePositions(licence));
  }

  public List<ChronologicalPosition> getLiveChronologicalPositions(List<LicencePosition> executedChronologicalLicencePositions) {
    return getLiveChronologicalPositions(
        executedChronologicalLicencePositions,
        getLiveChangesByPositionId(executedChronologicalLicencePositions)
    );
  }

  /**
   * The legal entities involved in each of the given licences at its latest executed position, keyed by licence id.
   * Positions only record what changed, so the state is folded from the licence's full executed history. A licence
   * with no executed positions is not included.
   */
  public Map<Integer, Set<Integer>> getInvolvedLegalEntityIdsByLicenceId(Collection<Licence> licences) {
    if (licences.isEmpty()) {
      return Map.of();
    }

    var executedPositions = licencePositionService.getExecutedChronologicalLicencePositions(licences);
    var liveChangesByPositionId = getLiveChangesByPositionId(executedPositions);

    // States are resolved per licence, as resolving positions from different licences together would fold their
    // changes into one another
    return executedPositions.stream()
        .collect(Collectors.groupingBy(position -> position.getLicence().getId()))
        .entrySet()
        .stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> {
              var chronologicalPositions = getLiveChronologicalPositions(entry.getValue(), liveChangesByPositionId);
              return LicencePositionStateResolver.resolve(chronologicalPositions)
                  .currentState(chronologicalPositions.getLast().id())
                  .legalEntityOrganisationUnitIds();
            }
        ));
  }

  public List<ChronologicalPosition> getCorrectedChronologicalPositions(
      LicenceCorrection licenceCorrection,
      UUID currentLicencePositionId
  ) {
    return getCorrectedChronologicalPositions(licenceCorrection, Set.of(currentLicencePositionId));
  }

  public List<ChronologicalPosition> getCorrectedChronologicalPositions(
      LicenceCorrection licenceCorrection,
      Set<UUID> retainedRemovedPositionIds
  ) {
    return getCorrectedChronologicalPositions(
        licenceCorrection,
        licencePositionCorrectionService.getPositionCorrections(licenceCorrection),
        retainedRemovedPositionIds
    );
  }

  public List<ChronologicalPosition> getCorrectedChronologicalPositions(
      LicenceCorrection licenceCorrection,
      List<LicencePositionCorrection> positionCorrections,
      Set<UUID> retainedRemovedPositionIds
  ) {
    return getCorrectedChronologicalPositions(
        licencePositionService.getExecutedChronologicalLicencePositions(licenceCorrection.getLicence()),
        positionCorrections,
        retainedRemovedPositionIds
    );
  }

  public List<ChronologicalPosition> getCorrectedChronologicalPositions(
      List<LicencePosition> executedChronologicalLicencePositions,
      List<LicencePositionCorrection> positionCorrections,
      Set<UUID> retainedRemovedPositionIds
  ) {
    var removedPositionIds = LicencePositionCorrectionService.getRemovedPositionIds(positionCorrections);
    var updatedCorrections = LicencePositionCorrectionService.correctionsOfType(
        positionCorrections,
        LicencePositionCorrectionChangeType.UPDATE_POSITION
    );
    var addedCorrections = LicencePositionCorrectionService.correctionsOfType(
        positionCorrections,
        LicencePositionCorrectionChangeType.ADD_POSITION
    );
    var liveChangesByPositionId = getLiveChangesByPositionId(executedChronologicalLicencePositions);

    var correctedPayloadsByPositionId = LicencePositionCorrectionService.updatedPositionPayloadsByTargetId(updatedCorrections);
    var updatedChangePositionIds = LicencePositionCorrectionService.getUpdatedChangePositionIds(
        Stream.concat(updatedCorrections.stream(), addedCorrections.stream()).toList()
    );

    var chronologicalPositions = new ArrayList<ChronologicalPosition>();

    // Positions removed in this correction are excluded from the state/change recalculation so their operations no
    // longer contribute, except for those in retainedRemovedPositionIds which are kept so their own pages still render.
    executedChronologicalLicencePositions.stream()
        .filter(position -> retainedRemovedPositionIds.contains(position.getId())
            || !removedPositionIds.contains(position.getId()))
        .forEach(position -> {
          var correctedPayload = correctedPayloadsByPositionId.get(position.getId());
          var changes = PositionChange.foldChanges(
                  liveChangesByPositionId.getOrDefault(position.getId(), List.of()),
                  correctedPayload != null ? correctedPayload.changes() : List.of()
              )
              .stream()
              .map(change -> LicencePositionCorrectionService.isMovedAway(
                  change.changeId(),
                  position.getId(),
                  updatedChangePositionIds
              ) ? change.asMovedAway() : change)
              .toList();
          chronologicalPositions.add(ChronologicalPosition.fromLicencePosition(
              position,
              effectiveReference(position, correctedPayloadsByPositionId),
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

  private static LocalDate effectiveDate(
      LicencePosition position,
      Map<UUID, UpdateLicencePositionPayload> correctedPayloadsByPositionId
  ) {
    var correctedPayload = correctedPayloadsByPositionId.get(position.getId());
    return correctedPayload != null && correctedPayload.effectiveDate() != null
        ? correctedPayload.effectiveDate() : position.getPositionDate();
  }

  private static int effectiveDateOrder(
      LicencePosition position,
      Map<UUID, UpdateLicencePositionPayload> correctedPayloadsByPositionId
  ) {
    var correctedPayload = correctedPayloadsByPositionId.get(position.getId());
    return correctedPayload != null && correctedPayload.effectiveDateOrder() != null
        ? correctedPayload.effectiveDateOrder() : position.getPositionDateOrder();
  }

  private List<ChronologicalPosition> getLiveChronologicalPositions(
      List<LicencePosition> executedChronologicalLicencePositions,
      Map<UUID, List<LicencePositionChange>> liveChangesByPositionId
  ) {
    return executedChronologicalLicencePositions.stream()
        .map(licencePosition -> ChronologicalPosition.fromLicencePosition(
            licencePosition,
            executedReference(licencePosition),
            licencePosition.getPositionDate(),
            licencePosition.getPositionDateOrder(),
            PositionChange.foldChanges(liveChangesByPositionId.getOrDefault(licencePosition.getId(), List.of()), List.of()))
        ).sorted(CHRONOLOGICAL_POSITION_COMPARATOR)
        .toList();
  }

  private Map<UUID, List<LicencePositionChange>> getLiveChangesByPositionId(List<LicencePosition> licencePositions) {
    return licencePositionChangeService.findByLicencePositionIn(licencePositions).stream()
        .collect(Collectors.groupingBy(change -> change.getLicencePosition().getId()));
  }

  private static String effectiveReference(
      LicencePosition position,
      Map<UUID, UpdateLicencePositionPayload> correctedPayloadsByPositionId
  ) {
    var correctedPayload = correctedPayloadsByPositionId.get(position.getId());
    return correctedPayload != null && correctedPayload.correctionReference() != null
        ? correctedPayload.correctionReference() : executedReference(position);
  }

  private static String executedReference(LicencePosition position) {
    return position.getLicenceTransaction().getRegulatorReference();
  }
}
