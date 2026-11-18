package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator;

import java.util.UUID;
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
    return confirmation(licenceCorrection, licencePosition.getId());
  }

  @Override
  public ModelAndView stagedChangeConfirmation(
      LicenceCorrection licenceCorrection,
      LicencePositionCorrection positionCorrection,
      String changeId,
      ChangeAction action
  ) {
    return confirmation(licenceCorrection, positionCorrection.getPositionId());
  }

  private ModelAndView confirmation(LicenceCorrection licenceCorrection, UUID licencePositionId) {
    var administratorChangeContext = organisationChangeService
        .getOrganisationChangeContext(licenceCorrection, licencePositionId, AdministratorOperation.class);

    return new ModelAndView(TEMPLATE_NAME)
        .addObject("withdrawingAdministratorName", administratorChangeContext.previousName())
        .addObject("joiningAdministratorName", administratorChangeContext.currentName());
  }
}
