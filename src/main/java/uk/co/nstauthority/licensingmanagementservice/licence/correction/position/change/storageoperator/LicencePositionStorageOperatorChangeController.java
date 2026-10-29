package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionCorrectionBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
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
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.storageoperator.StorageOperatorChangeContext;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType({LicenceType.CARBON_STORAGE})
public class LicencePositionStorageOperatorChangeController {

  private final LicencePositionViewService licencePositionViewService;
  private final StorageOperatorChangeFormValidator storageOperatorChangeFormValidator;
  private final StorageOperatorChangeService storageOperatorChangeService;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  public LicencePositionStorageOperatorChangeController(LicencePositionViewService licencePositionViewService,
                                                        StorageOperatorChangeFormValidator storageOperatorChangeFormValidator,
                                                        StorageOperatorChangeService storageOperatorChangeService,
                                                        OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.licencePositionViewService = licencePositionViewService;
    this.storageOperatorChangeFormValidator = storageOperatorChangeFormValidator;
    this.storageOperatorChangeService = storageOperatorChangeService;
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  @GetMapping("/position/{licencePositionId}/add-storage-operator-change")
  @LicencePositionHasNoLiveChangeOfType(value = StorageOperatorOperation.class)
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    return renderForExistingPosition(correction, licencePosition);
  }

  @PostMapping("/position/{licencePositionId}/add-storage-operator-change")
  @LicencePositionHasNoLiveChangeOfType(value = StorageOperatorOperation.class)
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView submitForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      @ModelAttribute("form") StorageOperatorChangeForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var modelAndView = getValidatedModelAndViewOrNull(
        correction,
        licencePosition,
        form,
        bindingResult
    );
    if (modelAndView != null) {
      return modelAndView;
    }

    storageOperatorChangeService.addStorageOperatorChangeForExistingLicencePosition(
        licencePosition,
        correction,
        Integer.parseInt(form.getOperatorId().getInputValue())
    );

    NotificationBanner.newSuccessBannerWithHeader("Storage operator change added", redirectAttributes);
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition, null));
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/add-storage-operator-change")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView renderForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    var storageOperatorChangeContext = getAddedPositionStorageOperatorChangeContext(correction, licencePositionCorrection);

    var form = storageOperatorChangeService.populateAddedStorageOperatorForm(
        storageOperatorChangeContext,
        licencePositionCorrection
    );

    return getStorageOperatorChangeModelAndView(
        form,
        addedCancelUrl(correction, licencePositionCorrection),
        storageOperatorChangeContext.previousOperatorName()
    );
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/add-storage-operator-change")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView submitForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @ModelAttribute("form") StorageOperatorChangeForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var storageOperatorChangeContext = getAddedPositionStorageOperatorChangeContext(correction, licencePositionCorrection);

    if (storageOperatorChangeFormValidator.hasErrors(
        form,
        bindingResult,
        storageOperatorChangeContext.previousOperatorId()
    )) {
      return getStorageOperatorChangeModelAndView(
          form,
          addedCancelUrl(correction, licencePositionCorrection),
          storageOperatorChangeContext.previousOperatorName()
      );
    }

    storageOperatorChangeService.addStorageOperatorChangeForAddedLicencePosition(
        licencePositionCorrection,
        Integer.parseInt(form.getOperatorId().getInputValue())
    );

    NotificationBanner.newSuccessBannerWithHeader("Storage operator change added", redirectAttributes);
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderAddedPosition(correction, licencePositionCorrection, null));
  }

  private ModelAndView renderForExistingPosition(LicenceCorrection correction, LicencePosition position) {
    var storageOperatorChangeContext = licencePositionViewService.getStorageOperatorChangeContext(
        correction,
        position.getId()
    );

    var form = storageOperatorChangeService.populateExistingStorageOperatorForm(
        storageOperatorChangeContext,
        correction,
        position
    );
    return getStorageOperatorChangeModelAndView(
        form,
        executedCancelUrl(correction, position),
        storageOperatorChangeContext.previousOperatorName()
    );
  }

  private ModelAndView getStorageOperatorChangeModelAndView(
      StorageOperatorChangeForm form,
      String cancelUrl,
      String previousOperatorName
  ) {
    return new ModelAndView("lms/licence/correction/change/storageOperatorChange")
        .addObject("pageTitle", "Change storage operator")
        .addObject("previousStorageOperatorName", previousOperatorName)
        .addObject("form", form)
        .addObject("cancelUrl", cancelUrl)
        .addObject("organisationUnitsUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class)
                .searchOrganisationUnits(null)))
        .addObject("preselectedStorageOperator", preselectedStorageOperator(form));
  }

  private String executedCancelUrl(LicenceCorrection correction, LicencePosition licencePosition) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition, null));
  }

  private String addedCancelUrl(LicenceCorrection correction, LicencePositionCorrection licencePositionCorrection) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderAddedPosition(correction, licencePositionCorrection, null));
  }

  private Map<String, String> preselectedStorageOperator(StorageOperatorChangeForm form) {
    var inputValue = form.getOperatorId().getInputValue();
    return organisationUnitQueryService.getOrganisationUnitSelectOption(inputValue);
  }

  private StorageOperatorChangeContext getAddedPositionStorageOperatorChangeContext(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    var payload = (CreateLicencePositionPayload) licencePositionCorrection.getPayload();
    return licencePositionViewService.getStorageOperatorChangeContext(correction, UUID.fromString(payload.licencePositionId()));
  }

  /**
   * Returns the licensee change form's {@link ModelAndView} with validation errors if {@code form} is invalid, or
   * {@code null} if it is valid and the caller should continue. Defaults a {@code null} withdrawing organisation list
   * on {@code form} to empty before validating.
   */
  @Nullable
  private ModelAndView getValidatedModelAndViewOrNull(
      LicenceCorrection correction,
      LicencePosition position,
      StorageOperatorChangeForm form,
      BindingResult bindingResult
  ) {
    var storageOperatorChangeContext = licencePositionViewService.getStorageOperatorChangeContext(
        correction,
        position.getId()
    );

    if (storageOperatorChangeFormValidator.hasErrors(form, bindingResult, storageOperatorChangeContext.previousOperatorId())) {
      return getStorageOperatorChangeModelAndView(
          form,
          executedCancelUrl(correction, position),
          storageOperatorChangeContext.previousOperatorName()
      );
    } else {
      return null;
    }
  }
}
