package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;

@Service
public class PartialSurrenderCorrectionService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionService licencePositionService;
  private final FeatureService featureService;

  public PartialSurrenderCorrectionService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionService licencePositionService,
      FeatureService featureService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionService = licencePositionService;
    this.featureService = featureService;
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

  @Transactional
  public void adjustPartialSurrenderBlocks(LicencePositionCorrection licencePositionCorrection) {
    var committedPartialSurrender = getCommittedPartialSurrender(licencePositionCorrection);
    if (committedPartialSurrender.isEmpty()) {
      return;
    }

    var surrenderableIds = getSurrenderableBlockFeatures(licencePositionCorrection).stream()
        .map(Feature::getId)
        .collect(Collectors.toSet());

    var retainedIds = committedPartialSurrender.get().featureIds().stream()
        .filter(surrenderableIds::contains)
        .toList();

    if (retainedIds.size() == committedPartialSurrender.get().featureIds().size()) {
      return;
    }

    var retainedBlockSurrenderTypes = committedPartialSurrender.get().blockSurrenderTypeByFeatureId().entrySet().stream()
        .filter(entry -> surrenderableIds.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    var operations = retainedIds.isEmpty()
        ? List.<PartialSurrenderOperation>of()
        : List.of(LicenceOperation.newPartialSurrenderOperation()
            .withSurrenderDate(committedPartialSurrender.get().surrenderDate())
            .withFeatureIds(retainedIds)
            .withBlockSurrenderTypeByFeatureId(retainedBlockSurrenderTypes)
            .build());

    licencePositionCorrectionService.replaceAddChangeFor(
        licencePositionCorrection,
        PartialSurrenderOperation.class,
        operations
    );
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

  public Feature getSurrenderedBlockFeatureOrThrow(
      LicencePositionCorrection licencePositionCorrection,
      UUID featureId
  ) {
    var operation = getCommittedPartialSurrenderOrThrow(licencePositionCorrection);
    if (!operation.featureIds().contains(featureId)) {
      throw new LmsEntityNotFoundException(
          "Block %s is not staged for surrender on position correction %s"
              .formatted(featureId, licencePositionCorrection.getId())
      );
    }

    return featureService.getFeatureOrThrow(featureId);
  }

  @Transactional
  public void setBlockSurrenderType(
      LicencePositionCorrection licencePositionCorrection,
      UUID featureId,
      BlockSurrenderType blockSurrenderType
  ) {
    var operation = getCommittedPartialSurrenderOrThrow(licencePositionCorrection);

    var blockSurrenderTypeByFeatureId = new HashMap<>(operation.blockSurrenderTypeByFeatureId());
    blockSurrenderTypeByFeatureId.put(featureId, blockSurrenderType);

    var updatedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(operation.surrenderDate())
        .withFeatureIds(operation.featureIds())
        .withBlockSurrenderTypeByFeatureId(blockSurrenderTypeByFeatureId)
        .build();

    applyPartialSurrender(licencePositionCorrection, updatedOperation);
  }

  private LicencePositionCorrection applyPartialSurrender(
      LicencePositionCorrection licencePositionCorrection,
      PartialSurrenderOperation operation
  ) {
    return licencePositionCorrectionService.replaceAddChangeFor(
        licencePositionCorrection, PartialSurrenderOperation.class, List.of(operation));
  }
}
