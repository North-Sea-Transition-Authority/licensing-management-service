package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator;

import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.ChronologicalPositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.OrganisationChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeOperationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStorageOperatorChangeUtil;

@Service
public class StorageOperatorChangeService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final ChronologicalPositionService chronologicalPositionService;
  private final OrganisationChangeService organisationChangeService;

  StorageOperatorChangeService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      OrganisationChangeService organisationChangeService,
      ChronologicalPositionService chronologicalPositionService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.organisationChangeService = organisationChangeService;
    this.chronologicalPositionService = chronologicalPositionService;
  }

  @Transactional
  public void correctExistingStorageOperatorChange(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      Integer storageOperatorId,
      String originalChangeId
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition);

    var payload = positionCorrection.getPayload();

    var changes = LicencePositionChangeOperationUtil.upsertUpdateChange(
        payload.changes(),
        StorageOperatorOperation.class,
        originalChangeId,
        LicencePositionStorageOperatorChangeUtil.storageOperatorOperation(storageOperatorId)
    );

    positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionCorrectionService.save(positionCorrection);
  }

  @Transactional
  public void removeExistingStorageOperatorChange(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      String originalChangeId
  ) {
    licencePositionCorrectionService.findUpdatePositionCorrection(licenceCorrection, licencePosition)
        .ifPresent(this::removeStagedStorageOperatorChange);

    licencePositionCorrectionService
        .stageRemovalOfExecutedChange(licenceCorrection, licencePosition, originalChangeId);
  }

  @Transactional
  public void undoStorageOperatorChange(
      LicenceCorrection licenceCorrection,
      String changeId
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(licenceCorrection, changeId);

    var changeToUndo = licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, changeId);

    var isStorageOperatorChange = licencePositionCorrectionService.resolveStagedChangeOperations(changeToUndo).stream()
        .anyMatch(StorageOperatorOperation.class::isInstance);

    if (!isStorageOperatorChange) {
      throw new IllegalStateException(
          "Change with id %s is not an storage operator change"
              .formatted(changeId));
    }

    licencePositionCorrectionService.dropStagedChange(positionCorrection, changeId);
  }

  public String getJoiningOperatorForReinstating(
      LicenceCorrection licenceCorrection,
      LicencePositionCorrection positionCorrectionToUndo
  ) {
    var licencePositionId = positionCorrectionToUndo.getPositionId();

    var remainingPositionCorrections = licencePositionCorrectionService.getPositionCorrections(licenceCorrection)
        .stream()
        .filter(correction -> !correction.getId().equals(positionCorrectionToUndo.getId()))
        .toList();

    var targetPositions = chronologicalPositionService.getCorrectedChronologicalPositions(
        licenceCorrection,
        remainingPositionCorrections,
        Set.of(licencePositionId)
    );

    var uncorrectedChangeContext = organisationChangeService
        .getOrganisationChangeContext(licenceCorrection, licencePositionId, StorageOperatorOperation.class, targetPositions);

    return uncorrectedChangeContext.currentName();
  }

  private void removeStagedStorageOperatorChange(LicencePositionCorrection positionCorrection) {
    var payload = positionCorrection.getPayload();
    var changes = LicencePositionStorageOperatorChangeUtil.removeStorageOperatorChange(payload.changes());

    positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionCorrectionService.save(positionCorrection);
  }

  public StorageOperatorChangeForm populateCorrectingStorageOperatorForm(
      OrganisationChangeContext changeContext
  ) {
    var form = new StorageOperatorChangeForm();
    if (changeContext.currentId() != null) {
      form.getId().setInputValue(String.valueOf(changeContext.currentId()));
    }
    return form;
  }
}
