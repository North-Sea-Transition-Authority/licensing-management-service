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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.EquitySetterOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
@CorrectionLicenceIsType(LicenceType.CARBON_STORAGE)
public class LicencePositionSetEquityController {

  private final LicencePositionSetEquityFormValidator licencePositionSetEquityFormValidator;
  private final SetEquityCorrectionService setEquityCorrectionService;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  LicencePositionSetEquityController(
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
  public ModelAndView renderForPosition(CorrectionPosition correctionPosition) {
    return setEquityModelAndView(
        correctionPosition.correction(),
        new LicencePositionSetEquityForm(),
        addChangeUrl(correctionPosition)
    );
  }

  @PostMapping("/position/{licencePositionId}/set-equity")
  @LicencePositionIsNotRemovedInCorrection
  ModelAndView submitForPosition(
      CorrectionPosition correctionPosition,
      @ModelAttribute("form") LicencePositionSetEquityForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var operations = new ArrayList<>(setEquityCorrectionService.getCommittedSetEquityOperations(correctionPosition));

    var organisationIds = operations.stream().map(SetEquityOperation::transferTo).toList();
    if (licencePositionSetEquityFormValidator.hasErrors(form, bindingResult, organisationIds)) {
      return setEquityModelAndView(correctionPosition.correction(), form, addChangeUrl(correctionPosition));
    }

    operations.add(toOperation(form));
    setEquityCorrectionService.commitSetEquity(correctionPosition, operations);

    NotificationBanner.newSuccessBannerWithHeader("Equity set updated", redirectAttributes);

    return ReverseRouter.redirect(on(this.getClass()).renderSummaryForPosition(correctionPosition));
  }

  @GetMapping("/position/{licencePositionId}/set-equity/summary")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView renderSummaryForPosition(CorrectionPosition correctionPosition) {
    var operations = setEquityCorrectionService.getCommittedSetEquityOperations(correctionPosition);

    var removeUrls = operations.stream()
        .map(operation -> ReverseRouter.route(on(this.getClass())
            .removeForPosition(correctionPosition, operation.transferTo())))
        .toList();

    return setEquitySummaryModelAndView(
        correctionPosition.correction(),
        operations,
        ReverseRouter.route(on(this.getClass()).renderForPosition(correctionPosition)),
        LicencePositionCorrectionRouteUtil.getPositionPageUrl(correctionPosition),
        ReverseRouter.route(on(this.getClass()).submitSummaryForPosition(correctionPosition)),
        removeUrls
    );
  }

  @PostMapping("/position/{licencePositionId}/set-equity/summary")
  @LicencePositionIsNotRemovedInCorrection
  ModelAndView submitSummaryForPosition(CorrectionPosition correctionPosition) {
    return LicencePositionCorrectionRouteUtil.redirectToPositionPage(correctionPosition);
  }

  @PostMapping("/position/{licencePositionId}/set-equity/remove")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView removeForPosition(
      CorrectionPosition correctionPosition,
      @RequestParam Integer transferTo
  ) {
    var operations = setEquityCorrectionService.getCommittedSetEquityOperations(correctionPosition)
        .stream()
        .filter(operation -> !operation.transferTo().equals(transferTo))
        .toList();

    setEquityCorrectionService.commitSetEquity(correctionPosition, operations);

    return ReverseRouter.redirect(on(this.getClass()).renderSummaryForPosition(correctionPosition));
  }

  private SetEquityOperation toOperation(LicencePositionSetEquityForm form) {
    return LicenceOperation.newSetEquityOperation()
        .withTransferTo(Integer.parseInt(form.getTransferTo()))
        .withEquity(form.getEquity().getAsBigDecimal().orElseThrow())
        .build();
  }

  private ModelAndView setEquityModelAndView(
      LicenceCorrection correction,
      LicencePositionSetEquityForm form,
      String backLinkUrl
  ) {
    return new ModelAndView("lms/licence/correction/setEquity")
        .addObject("pageTitle", "Add equity")
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("form", form)
        .addObject("licenseeOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("preselectedTransferTo", organisationUnitQueryService.getOrganisationUnitSelectOption(form.getTransferTo()))
        .addObject("backLinkUrl", backLinkUrl);
  }

  private ModelAndView setEquitySummaryModelAndView(
      LicenceCorrection correction,
      List<? extends EquitySetterOperation> operations,
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
        .addObject("pageTitle", "Add licence equity")
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("setEquityViews", views)
        .addObject("totalEquity", totalEquity)
        .addObject("addOrganisationUrl", addOrganisationUrl)
        .addObject("backLinkUrl", backLinkUrl)
        .addObject("saveAndContinueUrl", saveAndContinueUrl)
        .addObject("removeUrls", removeUrls);
  }

  private String addChangeUrl(CorrectionPosition correctionPosition) {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class).renderForPosition(correctionPosition));
  }

}
