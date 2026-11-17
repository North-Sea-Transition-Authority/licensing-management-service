package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
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
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final ChronologicalPositionService chronologicalPositionService;
  private final LicencePositionNameService licencePositionNameService;

  public OrganisationChangeService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      ChronologicalPositionService chronologicalPositionService,
      LicencePositionNameService licencePositionNameService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.chronologicalPositionService = chronologicalPositionService;
    this.licencePositionNameService = licencePositionNameService;
  }

  @Transactional
  public void addOrganisationChangeForAddedLicencePosition(
      LicencePositionCorrection licencePositionCorrection,
      Class<? extends LicenceOperation> operationType,
      LicenceOperation operation
  ) {
    var payload = (CreateLicencePositionPayload) licencePositionCorrection.getPayload();

    var changes = LicencePositionChangeOperationUtil.upsertAddOrganisationChange(
        payload.changes(),
        operationType,
        operation
    );

    licencePositionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionCorrectionService.save(licencePositionCorrection);
  }

  @Transactional
  public void addOrganisationChangeForExistingLicencePosition(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      Class<? extends LicenceOperation> operationType,
      LicenceOperation operation
  ) {
    var existingPositionCorrection = licencePositionCorrectionService
        .findUpdatePositionCorrection(licenceCorrection, licencePosition);

    LicencePositionCorrection positionCorrection;

    if (existingPositionCorrection.isPresent()) {
      positionCorrection = existingPositionCorrection.get();
      var payload = (UpdateLicencePositionPayload) positionCorrection.getPayload();

      var changes = LicencePositionChangeOperationUtil.upsertAddOrganisationChange(
          payload.changes(),
          operationType,
          operation
      );

      positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    } else {
      positionCorrection = new LicencePositionCorrection();
      var payload = LicencePositionPayload.newUpdateLicencePositionPayload()
          .withCorrectionReference(licenceCorrection.getCorrectionReference())
          .withChanges(List.of(AddChange.buildOperationsChange(
              List.of(operation),
              1
          ))).build();

      positionCorrection.setLicenceCorrection(licenceCorrection);
      positionCorrection.setChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION);
      positionCorrection.setTargetLicencePosition(licencePosition);
      positionCorrection.setPayload(payload);
    }
    licencePositionCorrectionService.save(positionCorrection);
  }

  public <T extends OrganisationChangeForm> T populateExistingOrganisationForm(
      Integer currentId,
      LicenceCorrection correction,
      LicencePosition position,
      T form
  ) {
    if (currentId != null
        && licencePositionCorrectionService.hasPendingChange(position, correction, form.operationType())
    ) {
      form.getId().setInputValue(String.valueOf(currentId));
    }
    return form;
  }

  public <T extends OrganisationChangeForm> T populateAddedOrganisationForm(
      Integer currentId,
      LicencePositionCorrection positionCorrection,
      T form
  ) {
    var payload = (CreateLicencePositionPayload) positionCorrection.getPayload();
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
}
