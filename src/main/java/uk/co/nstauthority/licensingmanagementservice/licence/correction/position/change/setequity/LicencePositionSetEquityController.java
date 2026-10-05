package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionCorrectionBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType(LicenceType.CARBON_STORAGE)
public class LicencePositionSetEquityController {

  private static final String ADD_PAGE_TITLE = "Add equity";
  private static final String SUMMARY_PAGE_TITLE = "Add licence equity";

  private final LicencePositionSetEquityFormValidator licencePositionSetEquityFormValidator;
  private final SetEquityCorrectionService setEquityCorrectionService;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  public LicencePositionSetEquityController(
      LicencePositionSetEquityFormValidator licencePositionSetEquityFormValidator,
      SetEquityCorrectionService setEquityCorrectionService,
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.licencePositionSetEquityFormValidator = licencePositionSetEquityFormValidator;
    this.setEquityCorrectionService = setEquityCorrectionService;
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  @GetMapping("/position/{licencePositionId}/set-equity")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    return setEquityModelAndView(
        correction,
        new LicencePositionSetEquityForm(),
        executedChangeUrl(correction, licencePosition));
  }

  @PostMapping("/position/{licencePositionId}/set-equity")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView submitForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      @ModelAttribute("form") LicencePositionSetEquityForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var operations = new ArrayList<>(
        setEquityCorrectionService.getCommittedSetEquityOperationsForExecutedPosition(correction, licencePosition));

    if (licencePositionSetEquityFormValidator.hasErrors(form, bindingResult, operations)) {
      return setEquityModelAndView(correction, form, executedChangeUrl(correction, licencePosition));
    }

    operations.add(toOperation(form));
    setEquityCorrectionService.commitSetEquityForExecutedPosition(correction, licencePosition, operations);

    NotificationBanner.newSuccessBannerWithHeader("Equity set updated", redirectAttributes);
    return ReverseRouter.redirect(on(this.getClass())
        .renderSummaryForExecutedPosition(correction, licencePosition));
  }

  @GetMapping("/position/{licencePositionId}/set-equity/summary")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderSummaryForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    var operations = setEquityCorrectionService.getCommittedSetEquityOperationsForExecutedPosition(correction, licencePosition);

    var removeUrls = operations.stream()
        .map(operation -> ReverseRouter.route(on(this.getClass())
            .removeForExecutedPosition(correction, licencePosition, operation.transferTo())))
        .toList();

    return setEquitySummaryModelAndView(
        correction,
        operations,
        ReverseRouter.route(on(this.getClass()).renderForExecutedPosition(correction, licencePosition)),
        executedPositionUrl(correction, licencePosition),
        ReverseRouter.route(on(this.getClass()).submitSummaryForExecutedPosition(correction, licencePosition)),
        removeUrls);
  }

  @PostMapping("/position/{licencePositionId}/set-equity/summary")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView submitSummaryForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition));
  }

  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  @PostMapping("/position/{licencePositionId}/set-equity/remove")
  public ModelAndView removeForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      @RequestParam Integer transferTo
  ) {
    var operations = withoutOrganisation(
        setEquityCorrectionService.getCommittedSetEquityOperationsForExecutedPosition(correction, licencePosition), transferTo
    );

    setEquityCorrectionService.commitSetEquityForExecutedPosition(correction, licencePosition, operations);
    return ReverseRouter.redirect(on(this.getClass())
        .renderSummaryForExecutedPosition(correction, licencePosition));
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/set-equity")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView renderForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    return setEquityModelAndView(
        correction,
        new LicencePositionSetEquityForm(),
        addedChangeChooserUrl(correction, licencePositionCorrection));
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/set-equity")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView submitForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @ModelAttribute("form") LicencePositionSetEquityForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var operations = new ArrayList<>(setEquityCorrectionService.getCommittedSetEquityOperations(licencePositionCorrection));

    if (licencePositionSetEquityFormValidator.hasErrors(form, bindingResult, operations)) {
      return setEquityModelAndView(correction, form, addedChangeChooserUrl(correction, licencePositionCorrection));
    }

    operations.add(toOperation(form));
    setEquityCorrectionService.commitSetEquity(licencePositionCorrection, operations);

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Equity set updated")
        .applyTo(redirectAttributes);
    return ReverseRouter.redirect(on(this.getClass())
        .renderSummaryForAddedPosition(correction, licencePositionCorrection));
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/set-equity/summary")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView renderSummaryForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    var operations = setEquityCorrectionService.getCommittedSetEquityOperations(licencePositionCorrection);

    var removeUrls = operations.stream()
        .map(operation -> ReverseRouter.route(on(this.getClass())
            .removeForAddedPosition(correction, licencePositionCorrection, operation.transferTo())))
        .toList();

    return setEquitySummaryModelAndView(
        correction,
        operations,
        ReverseRouter.route(on(this.getClass()).renderForAddedPosition(correction, licencePositionCorrection)),
        addedPositionUrl(correction, licencePositionCorrection),
        ReverseRouter.route(on(this.getClass())
            .submitSummaryForAddedPosition(correction, licencePositionCorrection)),
        removeUrls);
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/set-equity/summary")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView submitSummaryForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderAddedPosition(correction, licencePositionCorrection));
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/set-equity/remove")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView removeForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @RequestParam Integer transferTo
  ) {
    var operations = withoutOrganisation(setEquityCorrectionService
        .getCommittedSetEquityOperations(licencePositionCorrection), transferTo);

    setEquityCorrectionService.commitSetEquity(licencePositionCorrection, operations);
    return ReverseRouter.redirect(on(this.getClass())
        .renderSummaryForAddedPosition(correction, licencePositionCorrection));
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
    var views = setEquityCorrectionService.getSetEquityViews(operations);
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

  private String executedPositionUrl(LicenceCorrection correction, LicencePosition licencePosition) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition));
  }

  private String addedPositionUrl(LicenceCorrection correction, LicencePositionCorrection licencePositionCorrection) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderAddedPosition(correction, licencePositionCorrection));
  }

  private String executedChangeUrl(LicenceCorrection correction, LicencePosition licencePosition) {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForExecutedPosition(correction, licencePosition));
  }

  private String addedChangeChooserUrl(LicenceCorrection correction, LicencePositionCorrection licencePositionCorrection) {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForAddedPosition(correction, licencePositionCorrection));
  }

}