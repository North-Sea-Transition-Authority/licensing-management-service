package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenseeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.ChronologicalPositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionNameService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.licensee.LicenseeChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.licensee.UndoLicenseeChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeOperationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionLicenseeChangeUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicenseeChangeView;

@Service
public class LicenseeChangeService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final ChronologicalPositionService chronologicalPositionService;
  private final LicencePositionNameService licencePositionNameService;

  LicenseeChangeService(LicencePositionCorrectionService licencePositionCorrectionService,
                        ChronologicalPositionService chronologicalPositionService,
                        LicencePositionNameService licencePositionNameService) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.chronologicalPositionService = chronologicalPositionService;
    this.licencePositionNameService = licencePositionNameService;
  }

  @Transactional
  public void addLicenseeChangeForAddedLicencePosition(
      LicencePositionCorrection licencePositionCorrection,
      List<Integer> joiningLicenseeIds,
      List<Integer> withdrawingLicenseeIds
  ) {
    stageLicenseeChange(licencePositionCorrection, joiningLicenseeIds, withdrawingLicenseeIds);
  }

  @Transactional
  public void addLicenseeChangeForExistingLicencePosition(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      List<Integer> joiningLicenseeIds,
      List<Integer> withdrawingLicenseeIds
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition);

    stageLicenseeChange(positionCorrection, joiningLicenseeIds, withdrawingLicenseeIds);
  }

  @Transactional
  public void correctExistingLicenseeChange(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      List<Integer> joiningLicenseeIds,
      List<Integer> withdrawingLicenseeIds,
      String originalChangeId
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition);

    var payload = positionCorrection.getPayload();

    var licenseeOperation = LicenceOperation.newLicenseeOperation()
        .withLicenseesToAdd(joiningLicenseeIds)
        .withLicenseesToRemove(withdrawingLicenseeIds)
        .build();

    var changes = LicencePositionChangeOperationUtil.upsertUpdateChange(
        payload.changes(),
        LicenseeOperation.class,
        originalChangeId,
        licenseeOperation
    );

    positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionCorrectionService.save(positionCorrection);
  }

  @Transactional
  public void removeExistingLicenseeChange(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      String originalChangeId
  ) {
    licencePositionCorrectionService.findUpdatePositionCorrection(licenceCorrection, licencePosition)
        .ifPresent(this::removeStagedLicenseeChange);

    licencePositionCorrectionService
        .stageRemovalOfExecutedChange(licenceCorrection, licencePosition, originalChangeId);
  }

  private void removeStagedLicenseeChange(LicencePositionCorrection positionCorrection) {
    var payload = positionCorrection.getPayload();
    var changes = LicencePositionLicenseeChangeUtil.removeLicenseeChange(payload.changes());

    positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionCorrectionService.save(positionCorrection);
  }

  @Transactional
  public void undoLicenseeChange(
      LicencePositionCorrection positionCorrection,
      String changeId
  ) {
    var changeToUndo = licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, changeId);

    var isLicenseeChange = licencePositionCorrectionService.resolveStagedChangeOperations(changeToUndo).stream()
        .anyMatch(LicenseeOperation.class::isInstance);

    if (!isLicenseeChange) {
      throw new IllegalStateException(
          "Change with id %s is not a licensee change".formatted(changeId));
    }

    licencePositionCorrectionService.dropStagedChange(positionCorrection, changeId);
  }

  public LicenseeChangeView getRestoredLicenseeChangeView(List<String> withdrawingNames, List<String> joiningNames) {
    if (!joiningNames.isEmpty() || !withdrawingNames.isEmpty()) {
      return new LicenseeChangeView(
          withdrawingNames,
          joiningNames,
          "",
          ChangeViewUrls.none()
      );
    } else {
      return null;
    }
  }

  public UndoLicenseeChangeContext getUndoLicenseeChangeContext(
      LicenceCorrection licenceCorrection,
      LicencePositionCorrection positionCorrectionToUndo,
      UUID licencePositionId
  ) {
    var correctedChangeContext = getLicenseeChangeContext(licenceCorrection, licencePositionId);

    var remainingPositionCorrections = licencePositionCorrectionService.getPositionCorrections(licenceCorrection)
        .stream()
        .filter(correction -> !correction.getId().equals(positionCorrectionToUndo.getId()))
        .toList();

    var targetPositions = chronologicalPositionService.getCorrectedChronologicalPositions(
        licenceCorrection,
        remainingPositionCorrections,
        Set.of(licencePositionId)
    );

    var positionExistsInTarget = targetPositions.stream()
        .anyMatch(position -> position.id().equals(licencePositionId));

    if (!positionExistsInTarget) {
      return new UndoLicenseeChangeContext(
          correctedChangeContext.currentWithdrawingLicenseeNames(),
          correctedChangeContext.currentJoiningLicenseeNames(),
          List.of(),
          List.of(),
          correctedChangeContext.previousLicenseeNames()
      );
    }

    var uncorrectedChangeContext = buildLicenseeChangeContext(targetPositions, licencePositionId);

    return new UndoLicenseeChangeContext(
        correctedChangeContext.currentWithdrawingLicenseeNames(),
        correctedChangeContext.currentJoiningLicenseeNames(),
        uncorrectedChangeContext.currentWithdrawingLicenseeNames(),
        uncorrectedChangeContext.currentJoiningLicenseeNames(),
        uncorrectedChangeContext.currentLicenseeNames()
    );
  }

  public LicenseeChangeContext getLicenseeChangeContext(
      LicenceCorrection licenceCorrection,
      UUID licencePositionId
  ) {
    var chronologicalPositions =
        chronologicalPositionService.getCorrectedChronologicalPositions(licenceCorrection, licencePositionId);
    return buildLicenseeChangeContext(chronologicalPositions, licencePositionId);
  }

  private void stageLicenseeChange(
      LicencePositionCorrection positionCorrection,
      List<Integer> joiningLicenseeIds,
      List<Integer> withdrawingLicenseeIds
  ) {
    var payload = positionCorrection.getPayload();

    var changes = LicencePositionLicenseeChangeUtil.upsertAddLicenseeChange(
        payload.changes(),
        joiningLicenseeIds,
        withdrawingLicenseeIds
    );

    positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionCorrectionService.save(positionCorrection);
  }

  private LicenseeChangeContext buildLicenseeChangeContext(
      List<ChronologicalPosition> chronologicalPositions,
      UUID licencePositionId
  ) {
    var resolvedStates = LicencePositionStateResolver.resolve(chronologicalPositions);
    var currentState = resolvedStates.currentState(licencePositionId);
    var previousState = resolvedStates.previousState(licencePositionId);
    var organisationNames = licencePositionNameService.getOrganisationNameContext(chronologicalPositions).getNamesForDate(null);

    ChronologicalPosition currentPosition = chronologicalPositions.stream()
        .filter(chronologicalPosition -> chronologicalPosition.id().equals(licencePositionId))
        .toList()
        .getFirst();

    var joiningLicenseeIds = currentPosition.joiningLicensees();
    var withdrawingLicenseeIds = currentPosition.withdrawingLicensees();

    return new LicenseeChangeContext(
        joiningLicenseeIds,
        withdrawingLicenseeIds,
        previousState.licenseeIds().stream().toList(),
        currentState.licenseeIds().stream().toList(),
        organisationNames
    );
  }
}
