package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.explorationoperator;

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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.administrator.LicencePositionHasNoLiveChangeOfType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.ExplorationOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.OrganisationChangeContext;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
@CorrectionLicenceIsType({LicenceType.CARBON_STORAGE})
public class LicencePositionExplorationOperatorChangeController {

  private static final String OPERATOR_TYPE = "exploration operator";

  private final OrganisationChangeService organisationChangeService;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final OrganisationChangeFormValidator organisationChangeFormValidator;

  LicencePositionExplorationOperatorChangeController(
      OrganisationChangeService organisationChangeService,
      OrganisationUnitQueryService organisationUnitQueryService,
      OrganisationChangeFormValidator organisationChangeFormValidator
  ) {
    this.organisationChangeService = organisationChangeService;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.organisationChangeFormValidator = organisationChangeFormValidator;
  }

  @GetMapping("/position/{licencePositionId}/add-exploration-operator-change")
  @LicencePositionHasNoLiveChangeOfType(value = ExplorationOperatorOperation.class)
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    var explorationOperatorChangeContext = organisationChangeService.getOrganisationChangeContext(
        correction,
        licencePosition.getId(),
        ExplorationOperatorOperation.class
    );

    var form = organisationChangeService.populateExistingOrganisationForm(
        explorationOperatorChangeContext.currentId(),
        correction,
        licencePosition,
        new ExplorationOperatorChangeForm()
    );

    return getExplorationOperatorChangeModelAndView(
        form,
        executedCancelUrl(correction, licencePosition),
        explorationOperatorChangeContext.previousName()
    );
  }

  @PostMapping("/position/{licencePositionId}/add-exploration-operator-change")
  @LicencePositionHasNoLiveChangeOfType(value = ExplorationOperatorOperation.class)
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  ModelAndView submitForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      @ModelAttribute("form") ExplorationOperatorChangeForm form,
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
        ExplorationOperatorOperation.class,
        explorationOperatorOperation(Integer.parseInt(form.getId().getInputValue()))
    );

    NotificationBanner.newSuccessBannerWithHeader("Exploration operator change added", redirectAttributes);
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition, null));
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/add-exploration-operator-change")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView renderForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    var explorationOperatorChangeContext =
        getAddedPositionExplorationOperatorChangeContext(correction, licencePositionCorrection);

    var form = organisationChangeService.populateAddedOrganisationForm(
        explorationOperatorChangeContext.currentId(),
        licencePositionCorrection,
        new ExplorationOperatorChangeForm()
    );

    return getExplorationOperatorChangeModelAndView(
        form,
        addedCancelUrl(correction, licencePositionCorrection),
        explorationOperatorChangeContext.previousName()
    );
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/add-exploration-operator-change")
  @LicencePositionCorrectionBelongsToCorrection
  ModelAndView submitForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @ModelAttribute("form") ExplorationOperatorChangeForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var explorationOperatorChangeContext =
        getAddedPositionExplorationOperatorChangeContext(correction, licencePositionCorrection);

    if (organisationChangeFormValidator.hasErrors(
        form,
        bindingResult,
        explorationOperatorChangeContext.previousId(),
        OPERATOR_TYPE
    )) {
      return getExplorationOperatorChangeModelAndView(
          form,
          addedCancelUrl(correction, licencePositionCorrection),
          explorationOperatorChangeContext.previousName()
      );
    }

    organisationChangeService.addOrganisationChangeForAddedLicencePosition(
        licencePositionCorrection,
        ExplorationOperatorOperation.class,
        explorationOperatorOperation(Integer.parseInt(form.getId().getInputValue()))
    );

    NotificationBanner.newSuccessBannerWithHeader("Exploration operator change added", redirectAttributes);
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderAddedPosition(correction, licencePositionCorrection, null));
  }

  private ModelAndView getExplorationOperatorChangeModelAndView(
      ExplorationOperatorChangeForm form,
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
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition, null));
  }

  private String addedCancelUrl(LicenceCorrection correction, LicencePositionCorrection licencePositionCorrection) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderAddedPosition(correction, licencePositionCorrection, null));
  }

  private Map<String, String> preselectedOperator(OrganisationChangeForm form) {
    var inputValue = form.getId().getInputValue();
    return organisationUnitQueryService.getOrganisationUnitSelectOption(inputValue);
  }

  private OrganisationChangeContext getAddedPositionExplorationOperatorChangeContext(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    var payload = (CreateLicencePositionPayload) licencePositionCorrection.getPayload();
    return organisationChangeService.getOrganisationChangeContext(
        correction,
        UUID.fromString(payload.licencePositionId()),
        ExplorationOperatorOperation.class
    );
  }

  /**
   * Returns the exploration operator change form's {@link ModelAndView} with validation errors if {@code form} is invalid, or
   * {@code null} if it is valid and the caller should continue.
   */
  @Nullable
  private ModelAndView getValidatedModelAndViewOrNull(
      LicenceCorrection correction,
      LicencePosition position,
      ExplorationOperatorChangeForm form,
      BindingResult bindingResult
  ) {
    var explorationOperatorChangeContext = organisationChangeService.getOrganisationChangeContext(
        correction,
        position.getId(),
        ExplorationOperatorOperation.class
    );

    if (organisationChangeFormValidator.hasErrors(
        form,
        bindingResult,
        explorationOperatorChangeContext.previousId(),
        OPERATOR_TYPE)
    ) {
      return getExplorationOperatorChangeModelAndView(
          form,
          executedCancelUrl(correction, position),
          explorationOperatorChangeContext.previousName()
      );
    } else {
      return null;
    }
  }

  private ExplorationOperatorOperation explorationOperatorOperation(Integer operatorId) {
    return LicenceOperation.newExplorationOperatorOperation()
        .withOperator(operatorId)
        .build();
  }
}
