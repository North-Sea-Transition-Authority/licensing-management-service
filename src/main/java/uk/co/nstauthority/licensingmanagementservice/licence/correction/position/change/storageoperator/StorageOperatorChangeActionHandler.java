package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeAction;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;

@Component
public class StorageOperatorChangeActionHandler implements ChangeActionHandler {

  private static final String TEMPLATE_NAME = "lms/licence/correction/change/removeStorageOperatorChange";

  private final OrganisationChangeService organisationChangeService;
  private final StorageOperatorChangeService storageOperatorChangeService;

  StorageOperatorChangeActionHandler(
      OrganisationChangeService organisationChangeService,
      StorageOperatorChangeService storageOperatorChangeService
  ) {
    this.organisationChangeService = organisationChangeService;
    this.storageOperatorChangeService = storageOperatorChangeService;
  }

  @Override
  public ModelAndView executedChangeConfirmation(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LicencePositionChange liveChange
  ) {
    var storageOperatorChangeContext = organisationChangeService
        .getOrganisationChangeContext(licenceCorrection, licencePosition.getId(), StorageOperatorOperation.class);

    return new ModelAndView(TEMPLATE_NAME)
        .addObject("withdrawingStorageOperatorName", storageOperatorChangeContext.previousName())
        .addObject("joiningStorageOperatorName", storageOperatorChangeContext.currentName());
  }

  @Override
  public ModelAndView stagedChangeConfirmation(
      LicenceCorrection licenceCorrection,
      LicencePositionCorrection positionCorrection,
      String changeId,
      ChangeAction action
  ) {
    var storageOperatorChangeContext = organisationChangeService.getOrganisationChangeContext(
        licenceCorrection,
        positionCorrection.getPositionId(),
        StorageOperatorOperation.class
    );

    var joiningStorageOperatorName = action == ChangeAction.REINSTATE
        ? storageOperatorChangeService.getJoiningOperatorForReinstating(licenceCorrection, positionCorrection)
        : storageOperatorChangeContext.currentName();

    return new ModelAndView(TEMPLATE_NAME)
        .addObject("withdrawingStorageOperatorName", storageOperatorChangeContext.previousName())
        .addObject("joiningStorageOperatorName", joiningStorageOperatorName);
  }
}
