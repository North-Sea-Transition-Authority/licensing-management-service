package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import java.util.List;
import java.util.Map;
import java.util.Set;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.position.ChronologicalPositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionNameService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateResolver;

@Service
public class CorrectionReviewService {

  private final ChronologicalPositionService chronologicalPositionService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionService licencePositionService;
  private final LicencePositionNameService licencePositionNameService;

  public CorrectionReviewService(
      ChronologicalPositionService chronologicalPositionService,
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionService licencePositionService,
      LicencePositionNameService licencePositionNameService
  ) {
    this.chronologicalPositionService = chronologicalPositionService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionService = licencePositionService;
    this.licencePositionNameService = licencePositionNameService;
  }

  public List<ReviewPositionView> getReviewPositions(
      CorrectedTimeline correctedTimeline,
      List<PositionValidationError> blockingErrors
  ) {
    var erroredPositionIds = blockingErrors.stream()
        .map(PositionValidationError::positionId)
        .collect(Collectors.toSet());

    var positionCorrections = correctedTimeline.positionCorrections();
    var detailsByPositionId = positionDetailsByPositionId(positionCorrections);

    var correctedPositions = correctedTimeline.displayedPositions();
    var executedPositions = chronologicalPositionService.getLiveChronologicalPositions(
        correctedTimeline.licenceCorrection().getLicence()
    );

    var bothTimelines = Stream.concat(correctedPositions.stream(), executedPositions.stream()).toList();
    var organisationNames = licencePositionNameService.getOrganisationNameContext(bothTimelines).getNamesForDate(null);
    var featuresById = licencePositionNameService.resolveFeatures(bothTimelines);

    var correctedContext = new ReviewPositionContext(
        correctedPositions,
        correctedTimeline.resolvedStates(),
        organisationNames,
        featuresById
    );
    var executedContext = new ReviewPositionContext(
        executedPositions,
        LicencePositionStateResolver.resolve(executedPositions),
        organisationNames,
        featuresById
    );
    var changeEdits = ChangeEdits.from(positionCorrections, correctedContext, executedContext);
    var movedFromPositionIds = changeEdits.movedFromPositionIds();

    return correctedPositions.stream()
        .filter(position -> detailsByPositionId.containsKey(position.id())
            || erroredPositionIds.contains(position.id())
            || movedFromPositionIds.contains(position.id()))
        .map(position -> ReviewPositionView.from(
            position, detailsByPositionId.get(position.id()), correctedContext, changeEdits))
        .toList();
  }

  public List<ReviewPositionView> getAppliedPositions(LicenceCorrection licenceCorrection) {
    var positionCorrections = licencePositionCorrectionService.getPositionCorrections(licenceCorrection);
    var detailsByPositionId = positionDetailsByPositionId(positionCorrections);
    var removedPositionIds = LicencePositionCorrectionService.getRemovedPositionIds(positionCorrections);

    var positions = Stream.concat(
        licencePositionService.getExecutedChronologicalLicencePositions(licenceCorrection.getLicence()).stream(),
        positionCorrections.stream()
            .filter(correction -> correction.getChangeType() == LicencePositionCorrectionChangeType.REMOVE_POSITION)
            .map(LicencePositionCorrection::getTargetLicencePosition)
    ).toList();

    var appliedPositions = chronologicalPositionService.getLiveChronologicalPositions(positions);
    var context = new ReviewPositionContext(
        appliedPositions,
        LicencePositionStateResolver.resolve(appliedPositions, removedPositionIds),
        licencePositionNameService.getOrganisationNameContext(appliedPositions).getNamesForDate(null),
        licencePositionNameService.resolveFeatures(appliedPositions)
    );

    var changeEdits = new ChangeEdits(Set.of(), Map.of(), Map.of());

    return appliedPositions.stream()
        .filter(position -> detailsByPositionId.containsKey(position.id()))
        .map(position -> ReviewPositionView.from(
            position, detailsByPositionId.get(position.id()), context, changeEdits))
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
            payload.correctionReference() != null ? payload.correctionReference() :
                correction.getTargetLicencePosition().getLicenceTransaction().getRegulatorReference()
        );
      }
      case REMOVE_POSITION -> new PositionDetails(
          correction.getTargetLicencePosition().getId(),
          correction.getChangeType(),
          correction.getTargetLicencePosition().getLicenceTransaction().getRegulatorReference()
      );
    };
  }

  record PositionDetails(
      UUID positionId,
      LicencePositionCorrectionChangeType changeType,
      String correctionReference
  ) {
  }
}