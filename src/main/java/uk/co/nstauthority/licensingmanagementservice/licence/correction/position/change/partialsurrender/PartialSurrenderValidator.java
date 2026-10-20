package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

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
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureAttribute;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;

@Service
public class PartialSurrenderValidator {

  static final String NO_PARTIAL_SURRENDER_ERROR = "Select partial surrender for at least one block";
  static final String OVERLAP_ERROR = "Block after surrender must not overlap licence block %s";

  private final PartialSurrenderDetailsFormValidator partialSurrenderDetailsFormValidator;
  private final BlockSurrenderTypeFormValidator blockSurrenderTypeFormValidator;
  private final PartialSurrenderSelectAreasFormValidator partialSurrenderSelectAreasFormValidator;
  private final CommandJourneyService commandJourneyService;
  private final FeatureService featureService;
  private final FeatureRelationshipService featureRelationshipService;

  PartialSurrenderValidator(
      PartialSurrenderDetailsFormValidator partialSurrenderDetailsFormValidator,
      BlockSurrenderTypeFormValidator blockSurrenderTypeFormValidator,
      PartialSurrenderSelectAreasFormValidator partialSurrenderSelectAreasFormValidator,
      CommandJourneyService commandJourneyService,
      FeatureService featureService,
      FeatureRelationshipService featureRelationshipService
  ) {
    this.partialSurrenderDetailsFormValidator = partialSurrenderDetailsFormValidator;
    this.blockSurrenderTypeFormValidator = blockSurrenderTypeFormValidator;
    this.partialSurrenderSelectAreasFormValidator = partialSurrenderSelectAreasFormValidator;
    this.commandJourneyService = commandJourneyService;
    this.featureService = featureService;
    this.featureRelationshipService = featureRelationshipService;
  }

  /**
   * Validates a partial surrender using all associated form validators and an additional overlap check.
   *
   * @param surrender            The partial surrender operation being validated.
   * @param stateGoingIntoChange The state before the operation.
   * @param positionDate         The date of the position.
   * @return List of error messages.
   */
  public List<String> validate(
      PartialSurrenderOperation surrender,
      LicencePositionState stateGoingIntoChange,
      LocalDate positionDate
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

    var activeFeaturesByBlockId = getActiveFeaturesOfPartiallySurrenderedBlocks(surrender);

    var selectionErrors = Stream.concat(
        validateSurrenderTypes(surrender, blockFeaturesById).stream(),
        validateAreaSelections(surrender, blockFeaturesById, activeFeaturesByBlockId).stream()
    ).toList();
    if (!selectionErrors.isEmpty()) {
      return selectionErrors;
    }

    return validateBlocksDontOverlapWithOtherActiveBlocks(surrender, blockFeaturesById, activeFeaturesByBlockId, positionDate);
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
      return List.of(NO_PARTIAL_SURRENDER_ERROR);
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
          var form = PartialSurrenderSelectAreasForm.from(surrender.featureIdToSurrenderDetails().get(entry.getKey()));
          var bindingResult = new BeanPropertyBindingResult(form, "form");
          partialSurrenderSelectAreasFormValidator.hasErrors(form, bindingResult, entry.getValue(), blockFeature);

          return bindingResult.getAllErrors().stream()
              .map(DefaultMessageSourceResolvable::getDefaultMessage)
              .map(message -> blockError(blockFeature, message));
        })
        .toList();
  }

  private List<String> validateBlocksDontOverlapWithOtherActiveBlocks(
      PartialSurrenderOperation surrender,
      Map<UUID, Feature> blockFeaturesById,
      Map<UUID, List<Feature>> activeFeaturesByBlockId,
      LocalDate positionDate
  ) {
    if (activeFeaturesByBlockId.isEmpty()) {
      return List.of();
    }

    var otherActiveBlocksByCoordinateSystem = featureService
        .findAllByAttribute(FeatureAttribute.LAYER.name(), Layer.BLOCKS.name())
        .stream()
        .filter(block -> isActiveOn(block, positionDate))
        .filter(block -> !surrender.surrenderedFeatureIds().contains(block.getId()))
        .collect(Collectors.groupingBy(Feature::getCoordinateSystem));

    return activeFeaturesByBlockId.entrySet().stream()
        .flatMap(entry -> {
          var surrenderedAreaIds = surrender.featureIdToSurrenderDetails().get(entry.getKey()).surrenderedFeatureIds();
          return entry.getValue().stream()
              .filter(area -> !surrenderedAreaIds.contains(area.getId()))
              .flatMap(retainedArea -> featureRelationshipService.get2dOverlappingFeatures(
                  retainedArea,
                  otherActiveBlocksByCoordinateSystem.getOrDefault(retainedArea.getCoordinateSystem(), List.of())
              ).stream())
              .distinct()
              .map(overlappingBlock -> blockError(
                  blockFeaturesById.get(entry.getKey()),
                  OVERLAP_ERROR.formatted(overlappingBlock.getFeatureName())
              ));
        })
        .toList();
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
