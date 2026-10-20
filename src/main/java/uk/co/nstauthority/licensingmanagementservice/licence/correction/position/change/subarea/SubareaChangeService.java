package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;

@Service
public class SubareaChangeService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionSpatialService licencePositionSpatialService;
  private final FeatureService featureService;
  private final CommandJourneyService commandJourneyService;

  public SubareaChangeService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionSpatialService licencePositionSpatialService,
      FeatureService featureService,
      CommandJourneyService commandJourneyService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionSpatialService = licencePositionSpatialService;
    this.featureService = featureService;
    this.commandJourneyService = commandJourneyService;
  }

  @Transactional
  public LicencePositionCorrection commitSubareaChangeForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      UUID blockFeatureId
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition);

    replaceAddSubareaChangeForBlock(positionCorrection, blockFeatureId);
    return positionCorrection;
  }

  @Transactional
  public void commitSubareaChange(
      LicencePositionCorrection licencePositionCorrection,
      UUID blockFeatureId
  ) {
    replaceAddSubareaChangeForBlock(licencePositionCorrection, blockFeatureId);
  }

  public SubareaOperation getSubareaOperationOrThrow(
      LicencePositionCorrection licencePositionCorrection,
      UUID blockFeatureId
  ) {
    return licencePositionCorrectionService
        .getAddOperationsOfType(licencePositionCorrection.getPayload().changes(), SubareaOperation.class)
        .stream()
        .filter(operation -> blockFeatureId.equals(operation.blockFeatureId()))
        .findFirst()
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "No subarea change staged for block %s on position correction %s"
                .formatted(blockFeatureId, licencePositionCorrection.getId())));
  }

  private void replaceAddSubareaChangeForBlock(
      LicencePositionCorrection licencePositionCorrection,
      UUID blockFeatureId
  ) {
    var operation = LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(blockFeatureId)
        .withCommandJourneyId(createCommandJourneyForSubareas(licencePositionCorrection, blockFeatureId))
        .build();

    var payload = licencePositionCorrection.getPayload();

    var changes = payload.changes().stream()
        .filter(change -> !isAddSubareaChangeForBlock(change, blockFeatureId))
        .collect(Collectors.toCollection(ArrayList::new));

    changes.add(AddChange.buildOperationsChange(List.of(operation), licencePositionCorrectionService.nextChangeOrder(changes)));

    licencePositionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionCorrectionService.save(licencePositionCorrection);
  }

  private UUID createCommandJourneyForSubareas(
      LicencePositionCorrection licencePositionCorrection,
      UUID blockFeatureId
  ) {
    var subareas = licencePositionSpatialService.getSubareasGoingIntoChange(licencePositionCorrection, blockFeatureId, null);
    var subareaFeatures = featureService.getFeaturesByIds(SubareaDetails.featureIds(subareas));
    return commandJourneyService.createAndAssignCommandJourney(subareaFeatures).getId();
  }

  private boolean isAddSubareaChangeForBlock(LicencePositionChangeType change, UUID blockFeatureId) {
    return licencePositionCorrectionService.getAddOperationsOfType(List.of(change), SubareaOperation.class).stream()
        .anyMatch(op -> blockFeatureId.equals(op.blockFeatureId()));
  }
}
