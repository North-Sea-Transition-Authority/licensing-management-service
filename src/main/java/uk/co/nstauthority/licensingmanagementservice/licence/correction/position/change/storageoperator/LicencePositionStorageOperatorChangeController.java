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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.OrganisationChangeContext;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
@CorrectionLicenceIsType({LicenceType.CARBON_STORAGE})
public class LicencePositionStorageOperatorChangeController {

  private static final String OPERATOR_TYPE = "storage operator";

  private final StorageOperatorChangeService storageOperatorChangeService;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final OrganisationChangeService organisationChangeService;
  private final OrganisationChangeFormValidator organisationChangeFormValidator;

  public LicencePositionStorageOperatorChangeController(
      StorageOperatorChangeService storageOperatorChangeService,
      OrganisationUnitQueryService organisationUnitQueryService,
      OrganisationChangeService organisationChangeService,
      OrganisationChangeFormValidator organisationChangeFormValidator
  ) {
    this.storageOperatorChangeService = storageOperatorChangeService;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.organisationChangeService = organisationChangeService;
    this.organisationChangeFormValidator = organisationChangeFormValidator;
  }

  @GetMapping("/position/{licencePositionId}/add-storage-operator-change")
  @LicencePositionHasNoLiveChangeOfType(value = StorageOperatorOperation.class)
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    var storageOperatorChangeContext = organisationChangeService.getOrganisationChangeContext(
        correction,
        licencePosition.getId(),
        StorageOperatorOperation.class
    );

    var form = organisationChangeService.populateExistingOrganisationForm(
        storageOperatorChangeContext.currentId(),
        correction,
        licencePosition,
        new StorageOperatorChangeForm()
    );

    return getStorageOperatorChangeModelAndView(
        form,
        executedCancelUrl(correction, licencePosition),
        storageOperatorChangeContext.previousName()
    );
  }

  @PostMapping("/position/{licencePositionId}/add-storage-operator-change")
  @LicencePositionHasNoLiveChangeOfType(value = StorageOperatorOperation.class)
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  ModelAndView submitForExecutedPosition(
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

    organisationChangeService.addOrganisationChangeForExistingLicencePosition(
        licencePosition,
        correction,
        StorageOperatorOperation.class,
        storageOperatorOperation(Integer.parseInt(form.getId().getInputValue()))
    );

    NotificationBanner.newSuccessBannerWithHeader("Storage operator change added", redirectAttributes);
    return LicencePositionCorrectionRouteUtil.redirectToPositionPage(correction, licencePosition.getId());
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/add-storage-operator-change")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView renderForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    var storageOperatorChangeContext = getAddedPositionStorageOperatorChangeContext(correction,
        licencePositionCorrection);

    var form = organisationChangeService.populateAddedOrganisationForm(
        storageOperatorChangeContext.currentId(),
        licencePositionCorrection,
        new StorageOperatorChangeForm()
    );

    return getStorageOperatorChangeModelAndView(
        form,
        addedCancelUrl(licencePositionCorrection),
        storageOperatorChangeContext.previousName()
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
    var storageOperatorChangeContext = getAddedPositionStorageOperatorChangeContext(correction,
        licencePositionCorrection);

    if (organisationChangeFormValidator.hasErrors(
        form,
        bindingResult,
        storageOperatorChangeContext.previousId(),
        OPERATOR_TYPE
    )) {
      return getStorageOperatorChangeModelAndView(
          form,
          addedCancelUrl(licencePositionCorrection),
          storageOperatorChangeContext.previousName()
      );
    }

    organisationChangeService.addOrganisationChangeForAddedLicencePosition(
        licencePositionCorrection,
        StorageOperatorOperation.class,
        storageOperatorOperation(Integer.parseInt(form.getId().getInputValue()))
    );

    NotificationBanner.newSuccessBannerWithHeader("Storage operator change added", redirectAttributes);
    return LicencePositionCorrectionRouteUtil.redirectToPositionPage(correction, licencePositionCorrection.getPositionId());
  }

  @GetMapping("/position/{licencePositionId}/change/{changeId}/correct-storage-operator-change")
  @LicencePositionChangeBelongsToPosition
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeIsOfType(StorageOperatorOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderForCorrectingChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange licencePositionChange
  ) {
    var storageOperatorChangeContext = organisationChangeService.getOrganisationChangeContext(
        correction,
        licencePosition.getId(),
        StorageOperatorOperation.class
    );

    var form = storageOperatorChangeService.populateCorrectingStorageOperatorForm(
        storageOperatorChangeContext
    );

    return getStorageOperatorChangeModelAndView(
        form,
        executedCancelUrl(correction, licencePosition),
        storageOperatorChangeContext.previousName()
    );
  }

  @PostMapping("/position/{licencePositionId}/change/{changeId}/correct-storage-operator-change")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(StorageOperatorOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView submitForCorrectingChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change,
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

    storageOperatorChangeService.correctExistingStorageOperatorChange(
        licencePosition,
        correction,
        Integer.parseInt(form.getId().getInputValue()),
        change.getId().toString()
    );

    NotificationBanner.newSuccessBannerWithHeader("Storage operator change corrected", redirectAttributes);
    return LicencePositionCorrectionRouteUtil.redirectToPositionPage(correction, licencePosition.getId());
  }

  private ModelAndView getStorageOperatorChangeModelAndView(
      StorageOperatorChangeForm form,
      String cancelUrl,
      String previousOperatorName
  ) {
    return new ModelAndView("lms/licence/correction/change/addOrganisationChange")
        .addObject("pageTitle", "Change %s".formatted(OPERATOR_TYPE))
        .addObject("operatorType", OPERATOR_TYPE)
        .addObject("previousOperatorName", previousOperatorName)
        .addObject("form", form)
        .addObject("cancelUrl", cancelUrl)
        .addObject("organisationUnitsUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class)
                .searchOrganisationUnits(null)))
        .addObject("preselectedOperator", preselectedOperator(form));
  }

  private String executedCancelUrl(LicenceCorrection correction, LicencePosition licencePosition) {
    return LicencePositionCorrectionRouteUtil.getPositionPageUrl(correction, licencePosition.getId());
  }

  private String addedCancelUrl(LicencePositionCorrection licencePositionCorrection) {
    return LicencePositionCorrectionRouteUtil.getPositionPageUrl(licencePositionCorrection);
  }

  private Map<String, String> preselectedOperator(OrganisationChangeForm form) {
    var inputValue = form.getId().getInputValue();
    return organisationUnitQueryService.getOrganisationUnitSelectOption(inputValue);
  }

  private OrganisationChangeContext getAddedPositionStorageOperatorChangeContext(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    var payload = (CreateLicencePositionPayload) licencePositionCorrection.getPayload();
    return organisationChangeService
        .getOrganisationChangeContext(correction, UUID.fromString(payload.licencePositionId()),
            StorageOperatorOperation.class);
  }

  /**
   * Returns the storage operator change form's {@link ModelAndView} with validation errors if {@code form} is invalid, or
   * {@code null} if it is valid and the caller should continue.
   */
  @Nullable
  private ModelAndView getValidatedModelAndViewOrNull(
      LicenceCorrection correction,
      LicencePosition position,
      StorageOperatorChangeForm form,
      BindingResult bindingResult
  ) {
    var storageOperatorChangeContext = organisationChangeService.getOrganisationChangeContext(
        correction,
        position.getId(),
        StorageOperatorOperation.class
    );

    if (organisationChangeFormValidator.hasErrors(
        form,
        bindingResult,
        storageOperatorChangeContext.previousId(),
        OPERATOR_TYPE)
    ) {
      return getStorageOperatorChangeModelAndView(
          form,
          executedCancelUrl(correction, position),
          storageOperatorChangeContext.previousName()
      );
    } else {
      return null;
    }
  }

  private StorageOperatorOperation storageOperatorOperation(Integer operatorId) {
    return LicenceOperation.newStorageOperatorOperation()
        .withOperator(operatorId)
        .build();
  }
}
