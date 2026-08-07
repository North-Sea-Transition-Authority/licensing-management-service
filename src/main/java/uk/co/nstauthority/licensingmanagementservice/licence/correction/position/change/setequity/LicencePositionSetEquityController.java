package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@Profile("enable-lms2")
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType(LicenceType.CARBON_STORAGE)
public class LicencePositionSetEquityController {

  private static final String ADD_PAGE_TITLE = "Add equity";
  private static final String SUMMARY_PAGE_TITLE = "Add licence equity";

  private final LicencePositionSetEquityFormValidator licencePositionSetEquityFormValidator;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionService licencePositionService;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  public LicencePositionSetEquityController(
      LicencePositionSetEquityFormValidator licencePositionSetEquityFormValidator,
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionService licencePositionService, OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.licencePositionSetEquityFormValidator = licencePositionSetEquityFormValidator;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionService = licencePositionService;
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  @GetMapping("/position/{licencePositionId}/set-equity")
  public ModelAndView renderForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    return setEquityModelAndView(
        correction,
        new LicencePositionSetEquityForm(),
        executedChangeUrl(correctionId, licencePositionId));
  }

  @PostMapping("/position/{licencePositionId}/set-equity")
  public ModelAndView submitForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") LicencePositionSetEquityForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var operations = new ArrayList<>(
        licencePositionCorrectionService.getCommittedSetEquityOperationsForExecutedPosition(correction, licencePosition));

    if (licencePositionSetEquityFormValidator.hasErrors(form, bindingResult, operations)) {
      return setEquityModelAndView(correction, form, executedChangeUrl(correctionId, licencePositionId));
    }

    operations.add(toOperation(form));
    licencePositionCorrectionService.commitSetEquityForExecutedPosition(correction, licencePosition, operations);

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Equity set updated")
        .applyTo(redirectAttributes);
    return ReverseRouter.redirect(on(this.getClass())
        .renderSummaryForExecutedPosition(correctionId, licencePositionId, null));
  }

  @GetMapping("/position/{licencePositionId}/set-equity/summary")
  public ModelAndView renderSummaryForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var operations = licencePositionCorrectionService
        .getCommittedSetEquityOperationsForExecutedPosition(correction, licencePosition);

    var removeUrls = operations.stream()
        .map(operation -> ReverseRouter.route(on(this.getClass())
            .removeForExecutedPosition(correctionId, licencePositionId, operation.transferTo(), null)))
        .toList();

    return setEquitySummaryModelAndView(
        correction,
        operations,
        ReverseRouter.route(on(this.getClass()).renderForExecutedPosition(correctionId, licencePositionId, null)),
        executedPositionUrl(correctionId, licencePositionId),
        ReverseRouter.route(on(this.getClass()).submitSummaryForExecutedPosition(correctionId, licencePositionId, null)),
        removeUrls);
  }

  @PostMapping("/position/{licencePositionId}/set-equity/summary")
  public ModelAndView submitSummaryForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correctionId, licencePositionId, null));
  }

  @PostMapping("/position/{licencePositionId}/set-equity/remove")
  public ModelAndView removeForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestParam Integer transferTo,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var operations = withoutOrganisation(
        licencePositionCorrectionService.getCommittedSetEquityOperationsForExecutedPosition(correction, licencePosition),
        transferTo);

    licencePositionCorrectionService.commitSetEquityForExecutedPosition(correction, licencePosition, operations);
    return ReverseRouter.redirect(on(this.getClass())
        .renderSummaryForExecutedPosition(correctionId, licencePositionId, null));
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/set-equity")
  public ModelAndView renderForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    return setEquityModelAndView(
        correction,
        new LicencePositionSetEquityForm(),
        addedChangeChooserUrl(correctionId, licencePositionCorrectionId));
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/set-equity")
  public ModelAndView submitForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") LicencePositionSetEquityForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);
    var operations = new ArrayList<>(
        licencePositionCorrectionService.getCommittedSetEquityOperations(positionCorrection));

    if (licencePositionSetEquityFormValidator.hasErrors(form, bindingResult, operations)) {
      return setEquityModelAndView(correction, form, addedChangeChooserUrl(correctionId, licencePositionCorrectionId));
    }

    operations.add(toOperation(form));
    licencePositionCorrectionService.commitSetEquity(positionCorrection, operations);

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Equity set updated")
        .applyTo(redirectAttributes);
    return ReverseRouter.redirect(on(this.getClass())
        .renderSummaryForAddedPosition(correctionId, licencePositionCorrectionId, null));
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/set-equity/summary")
  public ModelAndView renderSummaryForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);
    var operations = licencePositionCorrectionService.getCommittedSetEquityOperations(positionCorrection);

    var removeUrls = operations.stream()
        .map(operation -> ReverseRouter.route(on(this.getClass())
            .removeForAddedPosition(correctionId, licencePositionCorrectionId, operation.transferTo(), null)))
        .toList();

    return setEquitySummaryModelAndView(
        correction,
        operations,
        ReverseRouter.route(on(this.getClass()).renderForAddedPosition(correctionId, licencePositionCorrectionId, null)),
        addedPositionUrl(correctionId, licencePositionCorrectionId),
        ReverseRouter.route(on(this.getClass())
            .submitSummaryForAddedPosition(correctionId, licencePositionCorrectionId, null)),
        removeUrls);
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/set-equity/summary")
  public ModelAndView submitSummaryForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderAddedPosition(correctionId, licencePositionCorrectionId, null));
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/set-equity/remove")
  public ModelAndView removeForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestParam Integer transferTo,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);
    var operations = withoutOrganisation(
        licencePositionCorrectionService.getCommittedSetEquityOperations(positionCorrection), transferTo);

    licencePositionCorrectionService.commitSetEquity(positionCorrection, operations);
    return ReverseRouter.redirect(on(this.getClass())
        .renderSummaryForAddedPosition(correctionId, licencePositionCorrectionId, null));
  }

  private SetEquityOperation toOperation(LicencePositionSetEquityForm form) {
    return LicenceOperation.newSetEquityOperation()
        .withTransferTo(Integer.parseInt(form.getTransferTo()))
        .withEquity(form.getEquity().getAsBigDecimal().orElseThrow())
        .build();
  }

  private List<SetEquityOperation> withoutOrganisation(
      List<SetEquityOperation> operations,
      Integer transferTo
  ) {
    return operations.stream()
        .filter(operation -> !operation.transferTo().equals(transferTo))
        .toList();
  }

  private ModelAndView setEquityModelAndView(
      LicenceCorrection correction,
      LicencePositionSetEquityForm form,
      String backLinkUrl
  ) {
    return new ModelAndView("lms/licence/correction/setEquity")
        .addObject("pageTitle", ADD_PAGE_TITLE)
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("form", form)
        .addObject("licenseeOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("preselectedTransferTo", organisationUnitQueryService.getOrganisationUnitSelectOption(form.getTransferTo()))
        .addObject("backLinkUrl", backLinkUrl);
  }

  private ModelAndView setEquitySummaryModelAndView(
      LicenceCorrection correction,
      List<SetEquityOperation> operations,
      String addOrganisationUrl,
      String backLinkUrl,
      String saveAndContinueUrl,
      List<String> removeUrls
  ) {
    var views = licencePositionCorrectionService.getSetEquityViews(operations);
    var totalEquity = views.stream()
        .map(SetEquityRow::equity)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new ModelAndView("lms/licence/correction/setEquitySummary")
        .addObject("pageTitle", SUMMARY_PAGE_TITLE)
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("setEquityViews", views)
        .addObject("totalEquity", totalEquity)
        .addObject("addOrganisationUrl", addOrganisationUrl)
        .addObject("backLinkUrl", backLinkUrl)
        .addObject("saveAndContinueUrl", saveAndContinueUrl)
        .addObject("removeUrls", removeUrls);
  }

  private String executedPositionUrl(UUID correctionId, UUID licencePositionId) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correctionId, licencePositionId, null));
  }

  private String addedPositionUrl(UUID correctionId, UUID licencePositionCorrectionId) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderAddedPosition(correctionId, licencePositionCorrectionId, null));
  }

  private String executedChangeUrl(UUID correctionId, UUID licencePositionId) {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForExecutedPosition(correctionId, licencePositionId, null));
  }

  private String addedChangeChooserUrl(UUID correctionId, UUID licencePositionCorrectionId) {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForAddedPosition(correctionId, licencePositionCorrectionId, null));
  }

}