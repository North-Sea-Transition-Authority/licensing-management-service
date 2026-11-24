package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeAction;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;

@Component
public class AdministratorChangeActionHandler implements ChangeActionHandler {

  private static final String TEMPLATE_NAME = "lms/licence/correction/change/removeAdministratorChange";

  private final OrganisationChangeService organisationChangeService;

  AdministratorChangeActionHandler(OrganisationChangeService organisationChangeService) {
    this.organisationChangeService = organisationChangeService;
  }

  @Override
  public ModelAndView executedChangeConfirmation(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LicencePositionChange liveChange
  ) {
    var administratorChangeContext = organisationChangeService
        .getOrganisationChangeContext(licenceCorrection, licencePosition.getId(), AdministratorOperation.class);

    return new ModelAndView(TEMPLATE_NAME)
        .addObject("withdrawingAdministratorName", administratorChangeContext.previousName())
        .addObject("joiningAdministratorName", administratorChangeContext.currentName());
  }

  @Override
  public ModelAndView stagedChangeConfirmation(
      LicenceCorrection licenceCorrection,
      LicencePositionCorrection positionCorrection,
      String changeId,
      ChangeAction action
  ) {
    var administratorChangeContext = organisationChangeService.getOrganisationChangeContext(
        licenceCorrection,
        positionCorrection.getPositionId(),
        AdministratorOperation.class
    );

    var joiningAdministratorName = action == ChangeAction.REINSTATE
        ? organisationChangeService.getJoiningOrganisationForReinstating(
            licenceCorrection,
            positionCorrection,
            AdministratorOperation.class
        )
        : administratorChangeContext.currentName();

    return new ModelAndView(TEMPLATE_NAME)
        .addObject("withdrawingAdministratorName", administratorChangeContext.previousName())
        .addObject("joiningAdministratorName", joiningAdministratorName);
  }
}
