package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
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
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.LicencePositionPayload;

@Service
public class LicenseeChangeService {

  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;
  private final ChronologicalPositionService chronologicalPositionService;
  private final LicencePositionNameService licencePositionNameService;

  LicenseeChangeService(LicencePositionStagedPayloadService licencePositionStagedPayloadService,
                        ChronologicalPositionService chronologicalPositionService,
                        LicencePositionNameService licencePositionNameService) {
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
    this.chronologicalPositionService = chronologicalPositionService;
    this.licencePositionNameService = licencePositionNameService;
  }

  @Transactional
  public void addLicenseeChange(
      CorrectionPosition correctionPosition,
      List<Integer> joiningLicenseeIds,
      List<Integer> withdrawingLicenseeIds
  ) {
    switch (correctionPosition) {
      case CorrectionPosition.Executed executed -> addLicenseeChangeForExistingLicencePosition(
          executed.licencePosition(),
          executed.correction(),
          joiningLicenseeIds,
          withdrawingLicenseeIds
      );
      case CorrectionPosition.Staged staged -> addLicenseeChangeForAddedLicencePosition(
          staged.licencePositionStagedPayload(),
          joiningLicenseeIds,
          withdrawingLicenseeIds
      );
    }
  }

  private void addLicenseeChangeForAddedLicencePosition(
      LicencePositionStagedPayload licencePositionStagedPayload,
      List<Integer> joiningLicenseeIds,
      List<Integer> withdrawingLicenseeIds
  ) {
    stageLicenseeChange(licencePositionStagedPayload, joiningLicenseeIds, withdrawingLicenseeIds);
  }

  private void addLicenseeChangeForExistingLicencePosition(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      List<Integer> joiningLicenseeIds,
      List<Integer> withdrawingLicenseeIds
  ) {
    var positionStagedPayload = licencePositionStagedPayloadService
        .getOrBuildUpdatePositionStagedPayload(licenceCorrection, licencePosition);

    stageLicenseeChange(positionStagedPayload, joiningLicenseeIds, withdrawingLicenseeIds);
  }

  @Transactional
  public void correctExistingLicenseeChange(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      List<Integer> joiningLicenseeIds,
      List<Integer> withdrawingLicenseeIds,
      String originalChangeId
  ) {
    var positionStagedPayload = licencePositionStagedPayloadService
        .getOrBuildUpdatePositionStagedPayload(licenceCorrection, licencePosition);

    var payload = positionStagedPayload.getPayload();

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

    positionStagedPayload.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionStagedPayloadService.save(positionStagedPayload);
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
      LicencePositionStagedPayload positionStagedPayloadToUndo,
      UUID licencePositionId
  ) {
    var correctedChangeContext = getLicenseeChangeContext(licenceCorrection, licencePositionId);

    var remainingPositionStagedPayloads = licencePositionStagedPayloadService.getPositionStagedPayloads(licenceCorrection)
        .stream()
        .filter(positionStagedPayload -> !positionStagedPayload.getId().equals(positionStagedPayloadToUndo.getId()))
        .toList();

    var targetPositions = chronologicalPositionService.getCorrectedChronologicalPositions(
        licenceCorrection,
        remainingPositionStagedPayloads,
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
      LicencePositionStagedPayload positionStagedPayload,
      List<Integer> joiningLicenseeIds,
      List<Integer> withdrawingLicenseeIds
  ) {
    var payload = positionStagedPayload.getPayload();

    var changes = LicencePositionLicenseeChangeUtil.upsertAddLicenseeChange(
        payload.changes(),
        joiningLicenseeIds,
        withdrawingLicenseeIds
    );

    positionStagedPayload.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionStagedPayloadService.save(positionStagedPayload);
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
