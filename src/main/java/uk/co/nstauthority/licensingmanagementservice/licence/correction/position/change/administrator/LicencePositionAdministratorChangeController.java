package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.AdministratorChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionAdministratorChangeUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType({LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION})
public class LicencePositionAdministratorChangeController {

  private static final String PAGE_TITLE = "Change licence administrator";

  private final AdministratorChangeFormValidator administratorChangeFormValidator;
  private final AdministratorChangeService administratorChangeService;
  private final LicencePositionViewService licencePositionViewService;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  public LicencePositionAdministratorChangeController(
      AdministratorChangeFormValidator administratorChangeFormValidator,
      AdministratorChangeService administratorChangeService,
      LicencePositionViewService licencePositionViewService,
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.administratorChangeFormValidator = administratorChangeFormValidator;
    this.administratorChangeService = administratorChangeService;
    this.licencePositionViewService = licencePositionViewService;
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  @GetMapping("/position/{licencePositionId}/add-administrator-change")
  @LicencePositionHasNoLiveChangeOfType(value = AdministratorOperation.class)
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    var administratorChangeContext = licencePositionViewService.getAdministratorChangeContext(
        correction,
        licencePosition.getId()
    );

    var form = new AdministratorChangeForm();
    if (administratorChangeContext.currentAdministratorId() != null
        && administratorChangeService.hasPendingAdministratorChange(licencePosition, correction)
    ) {
      form.getAdminId().setInputValue(String.valueOf(administratorChangeContext.currentAdministratorId()));
    }

    return getAdministratorChangeModelAndView(
        form,
        executedCancelUrl(correction, licencePosition),
        administratorChangeContext.previousAdministratorName()
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
    var administratorChangeContext = licencePositionViewService.getAdministratorChangeContext(
        correction,
        licencePosition.getId()
    );

    if (administratorChangeFormValidator.hasErrors(
        form,
        bindingResult,
        administratorChangeContext.previousAdministratorId()
    )) {
      return getAdministratorChangeModelAndView(
          form,
          executedCancelUrl(correction, licencePosition),
          administratorChangeContext.previousAdministratorName()
      );
    }

    administratorChangeService.addAdministratorChangeForExistingLicencePosition(
        licencePosition,
        correction,
        Integer.parseInt(form.getAdminId().getInputValue())
    );

    NotificationBanner.newSuccessBannerWithHeader("Licence administrator change added", redirectAttributes);
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition));
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/add-administrator-change")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView renderForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    var administratorChangeContext = getAddedPositionAdministratorChangeContext(correction, licencePositionCorrection);
    var payload = (CreateLicencePositionPayload) licencePositionCorrection.getPayload();

    var form = new AdministratorChangeForm();
    if (administratorChangeContext.currentAdministratorId() != null
        && LicencePositionAdministratorChangeUtil.adminChangeExists(payload.changes())
    ) {
      form.getAdminId().setInputValue(String.valueOf(administratorChangeContext.currentAdministratorId()));
    }

    return getAdministratorChangeModelAndView(
        form,
        addedCancelUrl(correction, licencePositionCorrection),
        administratorChangeContext.previousAdministratorName()
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

    if (administratorChangeFormValidator.hasErrors(
        form,
        bindingResult,
        administratorChangeContext.previousAdministratorId()
    )) {
      return getAdministratorChangeModelAndView(
          form,
          addedCancelUrl(correction, licencePositionCorrection),
          administratorChangeContext.previousAdministratorName()
      );
    }

    administratorChangeService.addAdministratorChangeForAddedLicencePosition(
        licencePositionCorrection,
        Integer.parseInt(form.getAdminId().getInputValue())
    );

    NotificationBanner.newSuccessBannerWithHeader("Licence administrator change added", redirectAttributes);
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderAddedPosition(correction, licencePositionCorrection));
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
        licencePositionViewService.getAdministratorChangeContext(correction, licencePosition.getId());
    var form = new AdministratorChangeForm();
    if (administratorChangeContext.currentAdministratorId() != null) {
      form.getAdminId().setInputValue(String.valueOf(administratorChangeContext.currentAdministratorId()));
    }
    return getAdministratorChangeModelAndView(
        form,
        executedCancelUrl(correction, licencePosition),
        administratorChangeContext.previousAdministratorName()
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
    var administratorChangeContext =
        licencePositionViewService.getAdministratorChangeContext(correction, licencePosition.getId());

    if (administratorChangeFormValidator.hasErrors(
        form,
        bindingResult,
        administratorChangeContext.previousAdministratorId()
    )) {
      return getAdministratorChangeModelAndView(
          form,
          executedCancelUrl(correction, licencePosition),
          administratorChangeContext.previousAdministratorName()
      );
    }

    administratorChangeService.correctExistingAdministratorChange(
        licencePosition,
        correction,
        change.getId().toString(),
        Integer.parseInt(form.getAdminId().getInputValue())
    );

    NotificationBanner.newSuccessBannerWithHeader("Licence administrator change corrected", redirectAttributes);
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition));
  }

  private AdministratorChangeContext getAddedPositionAdministratorChangeContext(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    var payload = (CreateLicencePositionPayload) licencePositionCorrection.getPayload();
    return licencePositionViewService.getAdministratorChangeContext(correction, UUID.fromString(payload.licencePositionId()));
  }

  private String executedCancelUrl(LicenceCorrection correction, LicencePosition licencePosition) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition));
  }

  private String addedCancelUrl(LicenceCorrection correction, LicencePositionCorrection licencePositionCorrection) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderAddedPosition(correction, licencePositionCorrection));
  }

  private Map<String, String> preselectedAdministrator(AdministratorChangeForm form) {
    var inputValue = form.getAdminId().getInputValue();
    if (StringUtils.isBlank(inputValue)) {
      return Map.of();
    }
    try {
      var administratorId = Integer.parseInt(inputValue);
      return organisationUnitQueryService.getOrganisationUnitNameById(administratorId)
          .map(name -> Map.of(inputValue, name))
          .orElse(Map.of());
    } catch (NumberFormatException ex) {
      return Map.of();
    }
  }

  private ModelAndView getAdministratorChangeModelAndView(
      AdministratorChangeForm form,
      String cancelUrl,
      String previousAdministratorName
  ) {
    return new ModelAndView("lms/licence/correction/change/administratorChange")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("cancelUrl", cancelUrl)
        .addObject("previousLicenceAdministratorName", previousAdministratorName)
        .addObject("preselectedAdministrator", preselectedAdministrator(form))
        .addObject("organisationUnitsUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)));
  }
}
