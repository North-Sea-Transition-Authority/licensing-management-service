package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceFormService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.AddPositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.form.ChangeFormHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.form.ChangeFormTarget;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenseeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.VisibleLicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.licensee.LicenseeChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionLicenseeChangeUtil;
import uk.co.nstauthority.licensingmanagementservice.util.ListUtil;

@Component
class LicenseeChangeFormHandler implements ChangeFormHandler<LicenseeChangeForm> {

  static final String TEMPLATE_NAME = "lms/licence/correction/change/licenseeChange";
  static final String PAGE_TITLE = "Change licensees";

  private final LicenceFormService licenceFormService;
  private final LicenseeChangeFormValidator licenseeChangeFormValidator;
  private final LicenseeChangeService licenseeChangeService;

  LicenseeChangeFormHandler(
      LicenceFormService licenceFormService,
      LicenseeChangeFormValidator licenseeChangeFormValidator,
      LicenseeChangeService licenseeChangeService
  ) {
    this.licenceFormService = licenceFormService;
    this.licenseeChangeFormValidator = licenseeChangeFormValidator;
    this.licenseeChangeService = licenseeChangeService;
  }

  @Override
  public AddPositionChangeType changeType() {
    return AddPositionChangeType.LICENSEE;
  }

  @Override
  public Class<? extends VisibleLicenceOperation> operationType() {
    return LicenseeOperation.class;
  }

  @Override
  public String displayName() {
    return "Licensee change";
  }

  @Override
  public LicenseeChangeForm newForm() {
    return new LicenseeChangeForm();
  }

  @Override
  public ModelAndView formPage(ChangeFormTarget target) {
    var context = changeContext(target);
    return formPage(LicencePositionLicenseeChangeUtil.populateLicenseeForm(context), context);
  }

  @Override
  public Optional<ModelAndView> formPageIfInvalid(
      ChangeFormTarget target,
      LicenseeChangeForm form,
      BindingResult bindingResult
  ) {
    var context = changeContext(target);

    if (form.getWithdrawingOrganisationIds() == null) {
      form.setWithdrawingOrganisationIds(List.of());
    }

    if (licenseeChangeFormValidator.hasErrors(form, bindingResult, context.previousLicenseeIds())) {
      return Optional.of(formPage(form, context));
    }

    return Optional.empty();
  }

  @Override
  public void save(ChangeFormTarget target, LicenseeChangeForm form) {
    var joiningIds = ListUtil.toIntegers(form.getJoiningOrganisationIds());
    var withdrawingIds = ListUtil.toIntegers(form.getWithdrawingOrganisationIds());

    switch (target) {
      case ChangeFormTarget.NewChange(var correctionPosition) -> licenseeChangeService
          .addLicenseeChange(correctionPosition, joiningIds, withdrawingIds);
      case ChangeFormTarget.LiveChange(var correction, var position, var change) -> licenseeChangeService
          .correctExistingLicenseeChange(
              position,
              correction,
              joiningIds,
              withdrawingIds,
              change.getId().toString()
          );
    }
  }

  private LicenseeChangeContext changeContext(ChangeFormTarget target) {
    return licenseeChangeService.getLicenseeChangeContext(target.correction(), target.positionId());
  }

  private ModelAndView formPage(LicenseeChangeForm form, LicenseeChangeContext context) {
    var previousLicenseeNames = context.previousLicenseeNames();

    return new ModelAndView(TEMPLATE_NAME)
        .addObject("previousLicenseeNames", previousLicenseeNames)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("preselectedJoiningOrgUnits",
            licenceFormService.getPreselectedOrganisationUnits(form.getJoiningOrganisationIds()))
        .addObject("preselectedWithdrawingOrgUnits",
            licenceFormService.getPreselectedOrganisationUnits(form.getWithdrawingOrganisationIds()))
        .addObject("joiningOrganisationUnitSearchEndpoint", joiningSearchSelectorEndpoint(previousLicenseeNames))
        .addObject("withdrawingOrganisationUnitSearchEndpoint", withdrawingSearchSelectorEndpoint(previousLicenseeNames));
  }

  private static String joiningSearchSelectorEndpoint(List<String> previousLicenseeNames) {
    if (previousLicenseeNames.isEmpty()) {
      return SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null));
    }

    return SearchSelectorService.routeWithConstraints(on(OrganisationUnitRestController.class)
        .searchOrganisationUnitsWithConstraints(null, null, previousLicenseeNames));
  }

  private static String withdrawingSearchSelectorEndpoint(List<String> previousLicenseeNames) {
    if (previousLicenseeNames.isEmpty()) {
      return "";
    }

    return SearchSelectorService.routeWithConstraints(on(OrganisationUnitRestController.class)
        .searchOrganisationUnitsWithConstraints(null, previousLicenseeNames, null));
  }
}
