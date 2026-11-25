package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.ExplorationOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.AdministratorChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.ChronologicalPositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionNameService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.OrganisationChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.explorationoperator.ExplorationOperatorChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.storageoperator.StorageOperatorChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeOperationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;

@Service
public class OrganisationChangeService {
  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;
  private final ChronologicalPositionService chronologicalPositionService;
  private final LicencePositionNameService licencePositionNameService;

  public OrganisationChangeService(
      LicencePositionStagedPayloadService licencePositionStagedPayloadService,
      ChronologicalPositionService chronologicalPositionService,
      LicencePositionNameService licencePositionNameService
  ) {
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
    this.chronologicalPositionService = chronologicalPositionService;
    this.licencePositionNameService = licencePositionNameService;
  }

  @Transactional
  public void addOrganisationChange(
      CorrectionPosition correctionPosition,
      Class<? extends LicenceOperation> operationType,
      LicenceOperation operation
  ) {
    switch (correctionPosition) {
      case CorrectionPosition.Executed executed -> addOrganisationChangeForExistingLicencePosition(
          executed.licencePosition(),
          executed.correction(),
          operationType,
          operation
      );
      case CorrectionPosition.Staged staged ->
          addOrganisationChangeForAddedLicencePosition(staged.licencePositionStagedPayload(), operationType, operation);
    }
  }

  private void addOrganisationChangeForAddedLicencePosition(
      LicencePositionStagedPayload licencePositionStagedPayload,
      Class<? extends LicenceOperation> operationType,
      LicenceOperation operation
  ) {
    var payload = (CreateLicencePositionPayload) licencePositionStagedPayload.getPayload();

    var changes = LicencePositionChangeOperationUtil.upsertAddOrganisationChange(
        payload.changes(),
        operationType,
        operation
    );

    licencePositionStagedPayload.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionStagedPayloadService.save(licencePositionStagedPayload);
  }

  private void addOrganisationChangeForExistingLicencePosition(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      Class<? extends LicenceOperation> operationType,
      LicenceOperation operation
  ) {
    var existingPositionStagedPayload = licencePositionStagedPayloadService
        .findUpdatePositionStagedPayload(licenceCorrection, licencePosition);

    LicencePositionStagedPayload positionStagedPayload;

    if (existingPositionStagedPayload.isPresent()) {
      positionStagedPayload = existingPositionStagedPayload.get();
      var payload = (UpdateLicencePositionPayload) positionStagedPayload.getPayload();

      var changes = LicencePositionChangeOperationUtil.upsertAddOrganisationChange(
          payload.changes(),
          operationType,
          operation
      );

      positionStagedPayload.setPayload(LicencePositionPayload.withChanges(payload, changes));
    } else {
      positionStagedPayload = new LicencePositionStagedPayload();
      var payload = LicencePositionPayload.newUpdateLicencePositionPayload()
          .withCorrectionReference(licenceCorrection.getCorrectionReference())
          .withChanges(List.of(AddChange.buildOperationsChange(
              List.of(operation),
              1
          ))).build();

      positionStagedPayload.setLicenceCorrection(licenceCorrection);
      positionStagedPayload.setChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION);
      positionStagedPayload.setTargetLicencePosition(licencePosition);
      positionStagedPayload.setPayload(payload);
    }
    licencePositionStagedPayloadService.save(positionStagedPayload);
  }

  /**
   * Stages a correction of a live change, so it gives the role to the organisation in {@code operation} instead.
   */
  @Transactional
  public void correctOrganisationChange(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      Class<? extends LicenceOperation> operationType,
      String liveChangeId,
      LicenceOperation operation
  ) {
    var positionStagedPayload = licencePositionStagedPayloadService
        .getOrBuildUpdatePositionStagedPayload(licenceCorrection, licencePosition);

    var payload = positionStagedPayload.getPayload();

    var changes = LicencePositionChangeOperationUtil.upsertUpdateChange(
        payload.changes(),
        operationType,
        liveChangeId,
        operation
    );

    positionStagedPayload.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionStagedPayloadService.save(positionStagedPayload);
  }

  /**
   * The form for a new change on the position, showing the organisation the correction has already given the role to,
   * if it has.
   */
  public <T extends OrganisationChangeForm> T populateOrganisationForm(
      Integer currentId,
      CorrectionPosition correctionPosition,
      T form
  ) {
    return switch (correctionPosition) {
      case CorrectionPosition.Executed executed ->
          populateExistingOrganisationForm(currentId, executed.correction(), executed.licencePosition(), form);
      case CorrectionPosition.Staged staged ->
          populateAddedOrganisationForm(currentId, staged.licencePositionStagedPayload(), form);
    };
  }

  private <T extends OrganisationChangeForm> T populateExistingOrganisationForm(
      Integer currentId,
      LicenceCorrection correction,
      LicencePosition position,
      T form
  ) {
    if (currentId != null
        && licencePositionStagedPayloadService.hasPendingChange(position, correction, form.operationType())
    ) {
      form.getId().setInputValue(String.valueOf(currentId));
    }
    return form;
  }

  private <T extends OrganisationChangeForm> T populateAddedOrganisationForm(
      Integer currentId,
      LicencePositionStagedPayload positionStagedPayload,
      T form
  ) {
    var payload = (CreateLicencePositionPayload) positionStagedPayload.getPayload();
    if (currentId != null
        && LicencePositionChangeOperationUtil.changeExists(payload.changes(), form.operationType())) {
      form.getId().setInputValue(String.valueOf(currentId));
    }
    return form;
  }

  public OrganisationChangeContext getOrganisationChangeContext(
      LicenceCorrection licenceCorrection,
      UUID licencePositionId,
      Class<? extends LicenceOperation> operationType
  ) {
    return getOrganisationChangeContext(licenceCorrection, licencePositionId, operationType, null);
  }

  /**
   * Builds the change context for an administrator, storage operator or exploration operator,
   * used to display joining and withdrawing operators.
   *
   * @param licenceCorrection      the current correction for the licence
   * @param licencePositionId      the id of the current position in the licence
   * @param chronologicalPositions the positions to resolve, or {@code null} in the general case to
   *                               resolve all the corrected chronological positions.
   * @throws IllegalArgumentException if the operation type is not one of the supported types
   */
  public OrganisationChangeContext getOrganisationChangeContext(
      LicenceCorrection licenceCorrection,
      UUID licencePositionId,
      Class<? extends LicenceOperation> operationType,
      @Nullable List<ChronologicalPosition> chronologicalPositions
  ) {
    var positions = chronologicalPositions != null
        ? chronologicalPositions
        : chronologicalPositionService.getCorrectedChronologicalPositions(licenceCorrection, licencePositionId);

    var resolvedStates = LicencePositionStateResolver.resolve(positions);
    var nameContext = licencePositionNameService.getOrganisationNameContext(positions);

    var currentOrganisationId = resolvedStates.currentState(licencePositionId).getOrganisationIdFor(operationType);
    var previousOrganisationId = resolvedStates.previousState(licencePositionId).getOrganisationIdFor(operationType);
    var currentName = nameContext.getNameForDate(currentOrganisationId, null, "");
    var previousName = nameContext.getNameForDate(previousOrganisationId, null, "");

    return switch (operationType) {
      case Class<?> c when c.equals(AdministratorOperation.class) -> new AdministratorChangeContext(
          currentOrganisationId, previousOrganisationId, currentName, previousName);
      case Class<?> c when c.equals(StorageOperatorOperation.class) -> new StorageOperatorChangeContext(
          currentOrganisationId, previousOrganisationId, currentName, previousName);
      case Class<?> c when c.equals(ExplorationOperatorOperation.class) -> new ExplorationOperatorChangeContext(
          currentOrganisationId, previousOrganisationId, currentName, previousName);
      default -> null;
    };
  }

  public String getJoiningOrganisationForReinstating(
      LicenceCorrection licenceCorrection,
      LicencePositionStagedPayload positionStagedPayloadToUndo,
      Class<? extends LicenceOperation> operationType
  ) {
    var licencePositionId = positionStagedPayloadToUndo.getPositionId();

    var remainingPositionStagedPayloads = licencePositionStagedPayloadService.getPositionStagedPayloads(licenceCorrection)
        .stream()
        .filter(positionStagedPayload -> !positionStagedPayload.getId().equals(positionStagedPayloadToUndo.getId()))
        .toList();

    var targetPositions = chronologicalPositionService.getCorrectedChronologicalPositions(
        licenceCorrection,
        remainingPositionStagedPayloads,
        Set.of(licencePositionId)
    );

    var uncorrectedChangeContext =
        getOrganisationChangeContext(licenceCorrection, licencePositionId, operationType, targetPositions);

    return uncorrectedChangeContext.currentName();
  }
}
