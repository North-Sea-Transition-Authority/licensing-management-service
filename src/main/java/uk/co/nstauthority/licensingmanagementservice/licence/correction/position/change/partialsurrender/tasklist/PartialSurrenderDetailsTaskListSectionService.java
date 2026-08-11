package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.LicencePositionPartialSurrenderController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderDetailsForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderDetailsFormValidator;
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

  public PartialSurrenderDetailsTaskListSectionService(
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      PartialSurrenderDetailsFormValidator partialSurrenderDetailsFormValidator
  ) {
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.partialSurrenderDetailsFormValidator = partialSurrenderDetailsFormValidator;
  }

  @Override
  public Optional<TaskListSection> getSection(PartialSurrenderTaskListContext context, ServiceUserDetail user) {
    var positionCorrection = context.positionCorrection();

    var items = List.of(new TaskListItem(
        SURRENDER_DETAILS,
        TaskListLabel.notStartedOrComplete(isSurrenderDetailsComplete(positionCorrection)),
        surrenderDetailsUrl(positionCorrection)));

    return Optional.of(new TaskListSection(SURRENDER_DETAILS, SECTION_ORDER, items));
  }

  private boolean isSurrenderDetailsComplete(LicencePositionCorrection positionCorrection) {
    var form = PartialSurrenderDetailsForm.from(
        partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection).orElse(null));
    var errors = new BeanPropertyBindingResult(form, "form");

    return !partialSurrenderDetailsFormValidator.hasErrors(
        form, errors, partialSurrenderCorrectionService.getSurrenderableBlockFeatures(positionCorrection));
  }

  private String surrenderDetailsUrl(LicencePositionCorrection positionCorrection) {
    var correctionId = positionCorrection.getLicenceCorrection().getId();

    return switch (positionCorrection.getChangeType()) {
      case ADD_POSITION -> ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
          .renderForAddedPosition(correctionId, positionCorrection.getId(), null));
      case UPDATE_POSITION -> ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
          .renderForExecutedPosition(correctionId, positionCorrection.getTargetLicencePosition().getId(), null));
      case REMOVE_POSITION -> throw new IllegalStateException(
          "Licence position correction %s removes a position so cannot carry a partial surrender"
              .formatted(positionCorrection.getId()));
    };
  }
}
