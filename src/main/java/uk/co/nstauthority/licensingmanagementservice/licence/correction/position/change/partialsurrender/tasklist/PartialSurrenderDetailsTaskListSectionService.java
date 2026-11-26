package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.LicencePositionPartialSurrenderController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderDetailsForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderDetailsFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@Service
public class PartialSurrenderDetailsTaskListSectionService
    implements TaskListSectionService<PartialSurrenderTaskListContext> {

  static final String SURRENDER_DETAILS = "Surrender details";
  static final int SECTION_ORDER = 10;

  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final PartialSurrenderDetailsFormValidator partialSurrenderDetailsFormValidator;
  private final LicencePositionSpatialService licencePositionSpatialService;

  public PartialSurrenderDetailsTaskListSectionService(
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      PartialSurrenderDetailsFormValidator partialSurrenderDetailsFormValidator,
      LicencePositionSpatialService licencePositionSpatialService
  ) {
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.partialSurrenderDetailsFormValidator = partialSurrenderDetailsFormValidator;
    this.licencePositionSpatialService = licencePositionSpatialService;
  }

  @Override
  public Optional<TaskListSection> getSection(PartialSurrenderTaskListContext context, ServiceUserDetail user) {
    var items = List.of(new TaskListItem(
        SURRENDER_DETAILS,
        TaskListLabel.notStartedOrComplete(isSurrenderDetailsComplete(context)),
        surrenderDetailsUrl(context)));

    return Optional.of(new TaskListSection(SURRENDER_DETAILS, SECTION_ORDER, items));
  }

  private boolean isSurrenderDetailsComplete(PartialSurrenderTaskListContext context) {
    return switch (context) {
      case PartialSurrenderTaskListContext.Staged(var positionStagedPayload) ->
          isSurrenderDetailsComplete(positionStagedPayload);
      case PartialSurrenderTaskListContext.LiveChange ignored -> true;
    };
  }

  private boolean isSurrenderDetailsComplete(LicencePositionStagedPayload positionStagedPayload) {
    var form = PartialSurrenderDetailsForm.from(
        partialSurrenderCorrectionService.getCommittedPartialSurrender(positionStagedPayload).orElse(null));
    var errors = new BeanPropertyBindingResult(form, "form");
    var stagedChangeId = partialSurrenderCorrectionService
        .getCommittedPartialSurrenderChangeId(positionStagedPayload)
        .orElse(null);
    var blockFeatures = licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionStagedPayload, stagedChangeId);

    return !partialSurrenderDetailsFormValidator.hasErrors(form, errors, blockFeatures, Set.of());
  }

  private String surrenderDetailsUrl(PartialSurrenderTaskListContext context) {
    return switch (context) {
      case PartialSurrenderTaskListContext.Staged(var positionStagedPayload) ->
          surrenderDetailsUrl(positionStagedPayload);
      case PartialSurrenderTaskListContext.LiveChange(var correction, var licencePosition, var change) ->
          ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
              .renderForCorrectingChange(correction, licencePosition, change));
    };
  }

  private String surrenderDetailsUrl(LicencePositionStagedPayload positionStagedPayload) {
    return ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
        .renderForPosition(CorrectionPosition.of(positionStagedPayload)));
  }

}
