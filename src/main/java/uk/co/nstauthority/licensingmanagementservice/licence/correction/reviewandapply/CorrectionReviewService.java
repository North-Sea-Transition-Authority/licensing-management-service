package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateResolver;

@Service
public class CorrectionReviewService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionViewService licencePositionViewService;

  public CorrectionReviewService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionViewService licencePositionViewService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionViewService = licencePositionViewService;
  }

  public List<ReviewPositionView> getReviewPositions(LicenceCorrection licenceCorrection) {
    var positionCorrections = licencePositionCorrectionService.getPositionCorrections(licenceCorrection);
    var detailsByPositionId = positionDetailsByPositionId(positionCorrections);
    var removedPositionIds = detailsByPositionId.values().stream()
        .filter(details -> details.changeType() == LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .map(PositionDetails::positionId)
        .collect(Collectors.toSet());

    var correctedPositions =
        licencePositionViewService.getCorrectedChronologicalPositions(licenceCorrection, removedPositionIds);
    var executedPositions =
        licencePositionViewService.getLiveChronologicalPositions(licenceCorrection.getLicence());

    var bothTimelines = Stream.concat(correctedPositions.stream(), executedPositions.stream()).toList();
    var organisationNames = licencePositionViewService.resolveOrganisationNames(bothTimelines);
    var featureNames = licencePositionViewService.resolveFeatureNames(bothTimelines);

    var correctedContext = new ReviewPositionContext(
        correctedPositions,
        LicencePositionStateResolver.resolve(correctedPositions, removedPositionIds),
        organisationNames,
        featureNames
    );
    var executedContext = new ReviewPositionContext(
        executedPositions,
        LicencePositionStateResolver.resolve(executedPositions),
        organisationNames,
        featureNames
    );
    var changeEdits = ChangeEdits.from(positionCorrections, executedContext);

    return correctedPositions.stream()
        .filter(position -> detailsByPositionId.containsKey(position.id()))
        .map(position -> ReviewPositionView.from(
            position, detailsByPositionId.get(position.id()), correctedContext, changeEdits))
        .toList();
  }

  private Map<UUID, PositionDetails> positionDetailsByPositionId(
      List<LicencePositionCorrection> positionCorrections
  ) {
    return positionCorrections.stream()
        .map(CorrectionReviewService::toPositionDetails)
        .collect(Collectors.toMap(PositionDetails::positionId, Function.identity()));
  }

  private static PositionDetails toPositionDetails(LicencePositionCorrection correction) {
    return switch (correction.getChangeType()) {
      case ADD_POSITION -> {
        var payload = (CreateLicencePositionPayload) correction.getPayload();
        yield new PositionDetails(
            UUID.fromString(payload.licencePositionId()),
            correction.getChangeType(),
            payload.correctionReference()
        );
      }
      case UPDATE_POSITION -> {
        var payload = (UpdateLicencePositionPayload) correction.getPayload();
        yield new PositionDetails(
            correction.getTargetLicencePosition().getId(),
            correction.getChangeType(),
            payload.correctionReference() != null ? payload.correctionReference() : executedReference(correction)
        );
      }
      case REMOVE_POSITION -> new PositionDetails(
          correction.getTargetLicencePosition().getId(),
          correction.getChangeType(),
          executedReference(correction)
      );
    };
  }

  private static String executedReference(LicencePositionCorrection correction) {
    return correction.getTargetLicencePosition().getLicenceTransaction().getRegulatorReference();
  }

  record PositionDetails(
      UUID positionId,
      LicencePositionCorrectionChangeType changeType,
      String correctionReference
  ) {
  }
}