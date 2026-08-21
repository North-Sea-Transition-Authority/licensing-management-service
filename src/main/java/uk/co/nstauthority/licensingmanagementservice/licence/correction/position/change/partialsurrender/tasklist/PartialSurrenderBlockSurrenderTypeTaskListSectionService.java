package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderTypeController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@Service
public class PartialSurrenderBlockSurrenderTypeTaskListSectionService
    implements TaskListSectionService<PartialSurrenderTaskListContext> {

  static final String SURRENDERED_BLOCKS = "Surrendered blocks";
  static final int SECTION_ORDER = 20;

  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  public PartialSurrenderBlockSurrenderTypeTaskListSectionService(
      PartialSurrenderCorrectionService partialSurrenderCorrectionService
  ) {
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
  }

  @Override
  public Optional<TaskListSection> getSection(PartialSurrenderTaskListContext context, ServiceUserDetail user) {
    return switch (context) {
      case PartialSurrenderTaskListContext.Staged(var positionCorrection) -> getSection(
          partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection).orElse(null),
          partialSurrenderCorrectionService.getSurrenderableBlockFeatures(positionCorrection),
          featureId -> blockSurrenderTypeUrl(positionCorrection, featureId));
      case PartialSurrenderTaskListContext.LiveChange(var correction, var licencePosition, var changeId) -> getSection(
          partialSurrenderCorrectionService
              .getSurrenderUnderCorrectionOrThrow(correction, licencePosition, changeId),
          partialSurrenderCorrectionService.getSurrenderableBlockFeatures(licencePosition),
          featureId -> correctingChangeBlockSurrenderTypeUrl(correction, licencePosition, changeId, featureId));
    };
  }

  private Optional<TaskListSection> getSection(
      @Nullable PartialSurrenderOperation operation,
      List<Feature> surrenderableBlockFeatures,
      Function<UUID, String> blockSurrenderTypeUrl
  ) {
    if (operation == null || operation.featureIds().isEmpty()) {
      return Optional.empty();
    }

    var surrenderedIds = new HashSet<>(operation.featureIds());
    var items = surrenderableBlockFeatures.stream()
        .filter(feature -> surrenderedIds.contains(feature.getId()))
        .sorted(LicenceBlockFeatureUtil.BLOCK_ORDER)
        .map(feature -> new TaskListItem(
            "Block %s".formatted(feature.getFeatureName()),
            TaskListLabel.notStartedOrComplete(isBlockSurrenderComplete(operation, feature.getId())),
            blockSurrenderTypeUrl.apply(feature.getId())))
        .toList();

    return items.isEmpty()
        ? Optional.empty()
        : Optional.of(new TaskListSection(SURRENDERED_BLOCKS, SECTION_ORDER, items));
  }

  private boolean isBlockSurrenderComplete(PartialSurrenderOperation operation, UUID featureId) {
    return operation.blockSurrenderTypeByFeatureId().get(featureId) == BlockSurrenderType.FULL_SURRENDER;
  }

  private String blockSurrenderTypeUrl(LicencePositionCorrection positionCorrection, UUID featureId) {
    return ReverseRouter.route(on(BlockSurrenderTypeController.class).renderSurrenderTypeForm(
        positionCorrection.getLicenceCorrection().getId(),
        positionCorrection.getId(),
        featureId,
        null
    ));
  }

  private String correctingChangeBlockSurrenderTypeUrl(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      String changeId,
      UUID featureId
  ) {
    return ReverseRouter.route(on(BlockSurrenderTypeController.class).renderSurrenderTypeFormForCorrectingChange(
        correction.getId(),
        licencePosition.getId(),
        changeId,
        featureId,
        null
    ));
  }
}
