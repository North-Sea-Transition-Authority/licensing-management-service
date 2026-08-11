package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;

@Service
public class PartialSurrenderCorrectionService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionService licencePositionService;

  public PartialSurrenderCorrectionService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionService licencePositionService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionService = licencePositionService;
  }

  public Optional<PartialSurrenderOperation> getCommittedPartialSurrender(
      @Nullable LicencePositionCorrection licencePositionCorrection
  ) {
    if (licencePositionCorrection == null) {
      return Optional.empty();
    }

    return licencePositionCorrectionService.getAddOperationsOfType(
            licencePositionCorrection.getPayload().changes(), PartialSurrenderOperation.class)
        .stream()
        .findFirst();
  }

  public PartialSurrenderOperation getCommittedPartialSurrenderOrThrow(
      LicencePositionCorrection licencePositionCorrection
  ) {
    return getCommittedPartialSurrender(licencePositionCorrection)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "No partial surrender staged on licence position correction %s"
                .formatted(licencePositionCorrection.getId())));
  }

  @Transactional
  public LicencePositionCorrection commitPartialSurrender(
      LicencePositionCorrection licencePositionCorrection,
      PartialSurrenderOperation operation
  ) {
    return applyPartialSurrender(licencePositionCorrection, operation);
  }

  @Transactional
  public LicencePositionCorrection commitPartialSurrenderForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      PartialSurrenderOperation operation
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition);

    return applyPartialSurrender(positionCorrection, operation);
  }

  public boolean hasStagedPartialSurrender(LicencePositionCorrection licencePositionCorrection) {
    return getCommittedPartialSurrender(licencePositionCorrection).isPresent();
  }

  public List<Feature> getSurrenderableBlockFeatures(LicencePositionCorrection licencePositionCorrection) {
    return switch (licencePositionCorrection.getPayload()) {
      case CreateLicencePositionPayload create -> licencePositionService.getBlockFeaturesOnLicenceOnOrBefore(
          licencePositionCorrection.getLicenceCorrection().getLicence(),
          create.effectiveDate(),
          create.effectiveDateOrder());
      case UpdateLicencePositionPayload ignored -> licencePositionService.getBlockFeatures(
          licencePositionCorrection.getTargetLicencePosition());
    };
  }

  private LicencePositionCorrection applyPartialSurrender(
      LicencePositionCorrection licencePositionCorrection,
      PartialSurrenderOperation operation
  ) {
    return licencePositionCorrectionService.replaceAddChangeFor(
        licencePositionCorrection, PartialSurrenderOperation.class, List.of(operation));
  }
}
