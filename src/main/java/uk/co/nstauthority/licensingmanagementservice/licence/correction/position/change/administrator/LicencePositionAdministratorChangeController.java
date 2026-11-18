package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionCorrectionBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeIsOfType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.administrator.LicencePositionHasNoLiveChangeOfType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.OrganisationChangeContext;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
@CorrectionLicenceIsType({LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION})
public class LicencePositionAdministratorChangeController {

  private static final String PAGE_TITLE = "Change licence administrator";
  private static final String OPERATOR_TYPE = "licence administrator";

  private final AdministratorChangeService administratorChangeService;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final OrganisationChangeService organisationChangeService;
  private final OrganisationChangeFormValidator organisationChangeFormValidator;

  public LicencePositionAdministratorChangeController(
      AdministratorChangeService administratorChangeService,
      OrganisationUnitQueryService organisationUnitQueryService,
      OrganisationChangeService organisationChangeService,
      OrganisationChangeFormValidator organisationChangeFormValidator
  ) {
    this.administratorChangeService = administratorChangeService;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.organisationChangeService = organisationChangeService;
    this.organisationChangeFormValidator = organisationChangeFormValidator;
  }

  @GetMapping("/position/{licencePositionId}/add-administrator-change")
  @LicencePositionHasNoLiveChangeOfType(value = AdministratorOperation.class)
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    var administratorChangeContext = organisationChangeService.getOrganisationChangeContext(
        correction,
        licencePosition.getId(),
        AdministratorOperation.class
    );

    var form = organisationChangeService.populateExistingOrganisationForm(
        administratorChangeContext.currentId(),
        correction,
        licencePosition,
        new AdministratorChangeForm()
    );

    return getAdministratorChangeModelAndView(
        form,
        executedCancelUrl(correction, licencePosition),
        administratorChangeContext.previousName()
    );
  }

  @PostMapping("/position/{licencePositionId}/add-administrator-change")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionHasNoLiveChangeOfType(value = AdministratorOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView submitForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      @ModelAttribute("form") AdministratorChangeForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var administratorChangeContext = organisationChangeService.getOrganisationChangeContext(
        correction,
        licencePosition.getId(),
        AdministratorOperation.class
    );

    if (organisationChangeFormValidator.hasErrors(
        form,
        bindingResult,
        administratorChangeContext.previousId(),
        OPERATOR_TYPE
    )) {
      return getAdministratorChangeModelAndView(
          form,
          executedCancelUrl(correction, licencePosition),
          administratorChangeContext.previousName()
      );
    }

    organisationChangeService.addOrganisationChangeForExistingLicencePosition(
        licencePosition,
        correction,
        AdministratorOperation.class,
        administratorOperation(Integer.parseInt(form.getId().getInputValue()))
    );

    NotificationBanner.newSuccessBannerWithHeader("Licence administrator change added", redirectAttributes);
    return LicencePositionCorrectionRouteUtil.redirectToPositionPage(correction, licencePosition.getId());
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/add-administrator-change")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView renderForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    var administratorChangeContext = getAddedPositionAdministratorChangeContext(correction, licencePositionCorrection);

    var form = organisationChangeService.populateAddedOrganisationForm(
        administratorChangeContext.currentId(),
        licencePositionCorrection,
        new AdministratorChangeForm()
    );

    return getAdministratorChangeModelAndView(
        form,
        LicencePositionCorrectionRouteUtil.getPositionPageUrl(correction, licencePositionCorrection.getPositionId()),
        administratorChangeContext.previousName()
    );
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/add-administrator-change")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView submitForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @ModelAttribute("form") AdministratorChangeForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var administratorChangeContext = getAddedPositionAdministratorChangeContext(correction, licencePositionCorrection);

    if (organisationChangeFormValidator.hasErrors(
        form,
        bindingResult,
        administratorChangeContext.previousId(),
        OPERATOR_TYPE
    )) {
      return getAdministratorChangeModelAndView(
          form,
          LicencePositionCorrectionRouteUtil.getPositionPageUrl(correction, licencePositionCorrection.getPositionId()),
          administratorChangeContext.previousName()
      );
    }

    organisationChangeService.addOrganisationChangeForAddedLicencePosition(
        licencePositionCorrection,
        AdministratorOperation.class,
        administratorOperation(Integer.parseInt(form.getId().getInputValue()))
    );

    NotificationBanner.newSuccessBannerWithHeader("Licence administrator change added", redirectAttributes);
    return LicencePositionCorrectionRouteUtil.redirectToPositionPage(correction, licencePositionCorrection.getPositionId());
  }

  @GetMapping("/position/{licencePositionId}/change/{changeId}/correct-administrator-change")
  @LicencePositionChangeBelongsToPosition
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeIsOfType(AdministratorOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderForCorrectingChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange licencePositionChange
  ) {
    var administratorChangeContext =
        organisationChangeService.getOrganisationChangeContext(correction, licencePosition.getId(), AdministratorOperation.class);
    var form = new AdministratorChangeForm();
    if (administratorChangeContext.currentId() != null) {
      form.getId().setInputValue(String.valueOf(administratorChangeContext.currentId()));
    }
    return getAdministratorChangeModelAndView(
        form,
        executedCancelUrl(correction, licencePosition),
        administratorChangeContext.previousName()
    );
  }

  @PostMapping("/position/{licencePositionId}/change/{changeId}/correct-administrator-change")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(AdministratorOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView submitForCorrectingChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change,
      @ModelAttribute("form") AdministratorChangeForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var administratorChangeContext = organisationChangeService
        .getOrganisationChangeContext(correction, licencePosition.getId(), AdministratorOperation.class);

    if (organisationChangeFormValidator.hasErrors(
        form,
        bindingResult,
        administratorChangeContext.previousId(),
        OPERATOR_TYPE
    )) {
      return getAdministratorChangeModelAndView(
          form,
          executedCancelUrl(correction, licencePosition),
          administratorChangeContext.previousName()
      );
    }

    administratorChangeService.correctExistingAdministratorChange(
        licencePosition,
        correction,
        change.getId().toString(),
        Integer.parseInt(form.getId().getInputValue())
    );

    NotificationBanner.newSuccessBannerWithHeader("Licence administrator change corrected", redirectAttributes);
    return LicencePositionCorrectionRouteUtil.redirectToPositionPage(correction, licencePosition.getId());
  }

  private OrganisationChangeContext getAddedPositionAdministratorChangeContext(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    var payload = (CreateLicencePositionPayload) licencePositionCorrection.getPayload();
    return organisationChangeService
        .getOrganisationChangeContext(correction, UUID.fromString(payload.licencePositionId()), AdministratorOperation.class);
  }

  private String executedCancelUrl(LicenceCorrection correction, LicencePosition licencePosition) {
    return LicencePositionCorrectionRouteUtil.getPositionPageUrl(correction, licencePosition.getId());
  }

  private Map<String, String> preselectedAdministrator(AdministratorChangeForm form) {
    var inputValue = form.getId().getInputValue();
    return organisationUnitQueryService.getOrganisationUnitSelectOption(inputValue);
  }

  private ModelAndView getAdministratorChangeModelAndView(
      AdministratorChangeForm form,
      String cancelUrl,
      String previousAdministratorName
  ) {
    return new ModelAndView("lms/licence/correction/change/addOrganisationChange")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("operatorType", OPERATOR_TYPE)
        .addObject("form", form)
        .addObject("cancelUrl", cancelUrl)
        .addObject("previousOperatorName", previousAdministratorName)
        .addObject("preselectedOperator", preselectedAdministrator(form))
        .addObject("organisationUnitsUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)));
  }

  private static AdministratorOperation administratorOperation(Integer administratorId) {
    return LicenceOperation.newAdministratorChange()
        .withOperator(administratorId)
        .build();
  }
}
