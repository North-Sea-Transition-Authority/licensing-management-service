package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.Layer;
import uk.co.fivium.gisframework.relationship.FeatureRelationshipService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderTypeForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderTypeFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderSelectAreasForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderSelectAreasFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PartialSurrenderOperationRule;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeOperationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureAttribute;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;

@Service
public class PartialSurrenderValidator {

  private final PartialSurrenderDetailsFormValidator partialSurrenderDetailsFormValidator;
  private final BlockSurrenderTypeFormValidator blockSurrenderTypeFormValidator;
  private final PartialSurrenderSelectAreasFormValidator partialSurrenderSelectAreasFormValidator;
  private final CommandJourneyService commandJourneyService;
  private final FeatureService featureService;
  private final FeatureRelationshipService featureRelationshipService;
  private final LicencePositionChangeService licencePositionChangeService;

  PartialSurrenderValidator(
      PartialSurrenderDetailsFormValidator partialSurrenderDetailsFormValidator,
      BlockSurrenderTypeFormValidator blockSurrenderTypeFormValidator,
      PartialSurrenderSelectAreasFormValidator partialSurrenderSelectAreasFormValidator,
      CommandJourneyService commandJourneyService,
      FeatureService featureService,
      FeatureRelationshipService featureRelationshipService,
      LicencePositionChangeService licencePositionChangeService
  ) {
    this.partialSurrenderDetailsFormValidator = partialSurrenderDetailsFormValidator;
    this.blockSurrenderTypeFormValidator = blockSurrenderTypeFormValidator;
    this.partialSurrenderSelectAreasFormValidator = partialSurrenderSelectAreasFormValidator;
    this.commandJourneyService = commandJourneyService;
    this.featureService = featureService;
    this.featureRelationshipService = featureRelationshipService;
    this.licencePositionChangeService = licencePositionChangeService;
  }

  /**
   * Validates a partial surrender using all associated form validators and an additional overlap check. Each step is
   * only validated once the steps before it have no errors, so only the errors of the earliest failing step are
   * returned.
   *
   * @param surrender            The partial surrender operation being validated.
   * @param stateGoingIntoChange The state before the operation.
   * @param positionDate         The date of the position.
   * @param changeId             The id of the change holding the surrender. Blocks created by the live version of
   *                             that change are not treated as overlaps.
   * @return List of error messages.
   */
  public List<String> validate(
      PartialSurrenderOperation surrender,
      LicencePositionState stateGoingIntoChange,
      LocalDate positionDate,
      @Nullable String changeId
  ) {
    if (surrender == null) {
      throw new IllegalStateException("Partial surrender must not be null");
    }
    var blockFeaturesById = getBlockFeaturesById(stateGoingIntoChange);

    var form = PartialSurrenderDetailsForm.from(surrender);
    var bindingResult = new BeanPropertyBindingResult(form, "form");
    if (partialSurrenderDetailsFormValidator.hasErrors(form, bindingResult, blockFeaturesById.values(), Set.of())) {
      return bindingResult.getAllErrors().stream()
          .map(DefaultMessageSourceResolvable::getDefaultMessage)
          .toList();
    }

    var surrenderTypeErrors = validateSurrenderTypes(surrender, blockFeaturesById);
    if (!surrenderTypeErrors.isEmpty()) {
      return surrenderTypeErrors;
    }

    var activeFeaturesByBlockId = getActiveFeaturesOfPartiallySurrenderedBlocks(surrender);
    var areaSelectionErrors = validateAreaSelections(surrender, blockFeaturesById, activeFeaturesByBlockId);
    if (!areaSelectionErrors.isEmpty()) {
      return areaSelectionErrors;
    }

    var recordedAreasKeptByBlockId = getRecordedAreasKept(surrender);
    if (activeFeaturesByBlockId.isEmpty() && recordedAreasKeptByBlockId.isEmpty()) {
      return List.of();
    }

    var otherActiveBlockPolygons =
        featureRelationshipService.build2dPolygons(getOtherActiveBlocks(surrender, positionDate, changeId));

    return Stream.of(
        validateSelectedAreasKept(
            surrender,
            blockFeaturesById,
            activeFeaturesByBlockId,
            otherActiveBlockPolygons
        ),
        validateRecordedAreasKept(
            blockFeaturesById,
            recordedAreasKeptByBlockId,
            otherActiveBlockPolygons
        )
    )
        .flatMap(List::stream)
        .toList();
  }

  /**
   * The licence blocks active on the position date, other than the surrender's own and those its live change created.
   */
  public List<Feature> getOtherActiveBlocks(
      PartialSurrenderOperation surrender,
      LocalDate positionDate,
      @Nullable String changeId
  ) {
    var featureIdsCreatedByLiveSurrender = getFeatureIdsCreatedByLiveSurrender(changeId);
    return featureService.findAllByAttribute(FeatureAttribute.LAYER.name(), Layer.BLOCKS.name()).stream()
        .filter(block -> isActiveOn(block, positionDate))
        .filter(block -> !surrender.surrenderedFeatureIds().contains(block.getId()))
        .filter(block -> !featureIdsCreatedByLiveSurrender.contains(block.getId()))
        .toList();
  }

  private List<String> validateSurrenderTypes(PartialSurrenderOperation surrender, Map<UUID, Feature> blockFeaturesById) {
    var errors = surrender.surrenderedFeatureIds().stream()
        .flatMap(blockFeatureId -> {
          var form = BlockSurrenderTypeForm.from(surrender, blockFeatureId);
          var bindingResult = new BeanPropertyBindingResult(form, "form");
          blockSurrenderTypeFormValidator.hasErrors(form, bindingResult);

          return bindingResult.getAllErrors().stream()
              .map(DefaultMessageSourceResolvable::getDefaultMessage)
              .map(message -> blockError(blockFeaturesById.get(blockFeatureId), message));
        })
        .toList();

    if (errors.isEmpty() && !surrender.hasPartialSurrender()) {
      return List.of(PartialSurrenderOperationRule.NO_PARTIAL_SURRENDER.getMessage());
    }

    return errors;
  }

  private List<String> validateAreaSelections(
      PartialSurrenderOperation surrender,
      Map<UUID, Feature> blockFeaturesById,
      Map<UUID, List<Feature>> activeFeaturesByBlockId
  ) {
    return activeFeaturesByBlockId.entrySet().stream()
        .flatMap(entry -> {
          var blockFeature = blockFeaturesById.get(entry.getKey());
          var surrenderDetails = surrender.featureIdToSurrenderDetails().get(entry.getKey());
          var form = PartialSurrenderSelectAreasForm.from(surrenderDetails);
          var bindingResult = new BeanPropertyBindingResult(form, "form");
          var hasErrors = partialSurrenderSelectAreasFormValidator.hasSelectionErrors(
              form,
              bindingResult,
              entry.getValue(),
              blockFeature
          );

          var areaIds = entry.getValue().stream().map(Feature::getId).toList();
          if (!hasErrors && !surrenderDetails.isSelectionUpToDate(areaIds)) {
            return Stream.of(blockError(blockFeature, PartialSurrenderOperationRule.AREAS_CHANGED.getMessage()));
          }

          return bindingResult.getAllErrors().stream()
              .map(DefaultMessageSourceResolvable::getDefaultMessage)
              .map(message -> blockError(blockFeature, message));
        })
        .toList();
  }

  private List<String> validateSelectedAreasKept(
      PartialSurrenderOperation surrender,
      Map<UUID, Feature> blockFeaturesById,
      Map<UUID, List<Feature>> activeFeaturesByBlockId,
      Map<Feature, Map<UUID, String>> otherActiveBlockPolygons
  ) {
    return activeFeaturesByBlockId.entrySet().stream()
        .flatMap(entry -> {
          var form = PartialSurrenderSelectAreasForm.from(surrender.featureIdToSurrenderDetails().get(entry.getKey()));
          var bindingResult = new BeanPropertyBindingResult(form, "form");
          partialSurrenderSelectAreasFormValidator.hasOverlapErrors(
              form,
              bindingResult,
              entry.getValue(),
              otherActiveBlockPolygons
          );

          return bindingResult.getAllErrors().stream()
              .map(DefaultMessageSourceResolvable::getDefaultMessage)
              .map(message -> blockError(blockFeaturesById.get(entry.getKey()), message));
        })
        .toList();
  }

  private List<String> validateRecordedAreasKept(
      Map<UUID, Feature> blockFeaturesById,
      Map<UUID, List<Feature>> recordedAreasKeptByBlockId,
      Map<Feature, Map<UUID, String>> otherActiveBlockPolygons
  ) {
    return recordedAreasKeptByBlockId.entrySet().stream()
        .flatMap(entry -> partialSurrenderSelectAreasFormValidator.getOverlapErrors(
                entry.getValue(),
                otherActiveBlockPolygons
            )
            .stream()
            .map(message -> blockError(blockFeaturesById.get(entry.getKey()), message)))
        .toList();
  }

  private Map<UUID, List<Feature>> getRecordedAreasKept(PartialSurrenderOperation surrender) {
    return surrender.featureIdToSurrenderDetails().entrySet().stream()
        .filter(entry -> entry.getValue().isPartialSurrender())
        .filter(entry -> !entry.getValue().hasCommandJourney())
        .map(entry -> Map.entry(entry.getKey(), featureService.getFeaturesByIds(entry.getValue().retainedFeatureIds())))
        .filter(entry -> !entry.getValue().isEmpty())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Set<UUID> getFeatureIdsCreatedByLiveSurrender(@Nullable String changeId) {
    if (changeId == null) {
      return Set.of();
    }
    return licencePositionChangeService.findById(UUID.fromString(changeId))
        .flatMap(change -> LicencePositionChangeOperationUtil.findOperation(change, PartialSurrenderOperation.class))
        .map(PartialSurrenderOperation::createdFeatureIds)
        .orElse(Set.of());
  }

  private Map<UUID, Feature> getBlockFeaturesById(LicencePositionState stateGoingIntoChange) {
    return featureService.getFeaturesByIds(stateGoingIntoChange.blockFeatureIds()).stream()
        .filter(LicenceBlockFeatureUtil::isLicenceBlock)
        .collect(Collectors.toMap(Feature::getId, Function.identity()));
  }

  private Map<UUID, List<Feature>> getActiveFeaturesOfPartiallySurrenderedBlocks(PartialSurrenderOperation surrender) {
    return surrender.featureIdToSurrenderDetails().entrySet().stream()
        .filter(entry -> entry.getValue().isPartialSurrender())
        .filter(entry -> entry.getValue().hasCommandJourney())
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> commandJourneyService.getActiveFeatures(entry.getValue().commandJourneyIdOrThrow())
        ));
  }

  /**
   * A block is active from its start date to its end date inclusive. A block with no end date has not ended.
   */
  private static boolean isActiveOn(Feature block, LocalDate date) {
    return block.getStartDate() != null
        && !block.getStartDate().isAfter(date)
        && (block.getEndDate() == null || !block.getEndDate().isBefore(date));
  }

  private static String blockError(Feature blockFeature, String message) {
    return "Block %s: %s".formatted(blockFeature.getFeatureName(), message);
  }
}
