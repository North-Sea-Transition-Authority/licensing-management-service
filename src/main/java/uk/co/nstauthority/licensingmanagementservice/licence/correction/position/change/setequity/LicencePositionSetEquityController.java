package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.math.BigDecimal;
import java.util.ArrayList;
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
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/positions/{positionCorrectionId}/set-equity")
@Profile("enable-lms2")
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType(LicenceType.CARBON_STORAGE)
public class LicencePositionSetEquityController {

  private static final String ADD_PAGE_TITLE = "Add equity";
  private static final String SUMMARY_PAGE_TITLE = "Add licence equity";

  private final LicencePositionSetEquityFormValidator licencePositionSetEquityFormValidator;
  private final LicencePositionCorrectionService licencePositionCorrectionService;

  public LicencePositionSetEquityController(
      LicencePositionSetEquityFormValidator licencePositionSetEquityFormValidator,
      LicencePositionCorrectionService licencePositionCorrectionService
  ) {
    this.licencePositionSetEquityFormValidator = licencePositionSetEquityFormValidator;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
  }

  @GetMapping
  public ModelAndView showLicencePositionSetEquity(
      @PathVariable UUID correctionId,
      @PathVariable UUID positionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    return setEquityModelAndView(correctionId, positionCorrectionId, correction, new LicencePositionSetEquityForm());
  }

  @PostMapping
  public ModelAndView updateLicencePositionSetEquity(
      @PathVariable UUID correctionId,
      @PathVariable UUID positionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") LicencePositionSetEquityForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(positionCorrectionId, correction);
    var operations = new ArrayList<>(
        licencePositionCorrectionService.getCommittedSetEquityOperations(positionCorrection));

    if (licencePositionSetEquityFormValidator.hasErrors(form, bindingResult, operations)) {
      return setEquityModelAndView(correctionId, positionCorrectionId, correction, form);
    }

    var transferTo = Integer.valueOf(form.getTransferTo());

    operations.add(LicenceOperation.newSetEquityOperation()
        .withTransferTo(transferTo)
        .withEquity(form.getEquity().getAsBigDecimal().orElseThrow())
        .build());

    licencePositionCorrectionService.commitSetEquity(positionCorrection, operations);

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Equity set updated")
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(this.getClass())
        .showSetEquitySummary(correctionId, positionCorrectionId, null));
  }

  @GetMapping("/summary")
  public ModelAndView showSetEquitySummary(
      @PathVariable UUID correctionId,
      @PathVariable UUID positionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(positionCorrectionId, correction);

    var operations = licencePositionCorrectionService.getCommittedSetEquityOperations(positionCorrection);
    var views = licencePositionCorrectionService.getSetEquityViews(operations);

    var totalEquity = views.stream()
        .map(SetEquityRow::equity)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    var removeUrls = operations.stream()
        .map(operation -> ReverseRouter.route(on(this.getClass())
            .removeSetEquity(correctionId, positionCorrectionId, operation.transferTo(), null)))
        .toList();

    return new ModelAndView("lms/licence/correction/setEquitySummary")
        .addObject("pageTitle", SUMMARY_PAGE_TITLE)
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("setEquityViews", views)
        .addObject("totalEquity", totalEquity)
        .addObject("addOrganisationUrl",
            ReverseRouter.route(on(this.getClass())
                .showLicencePositionSetEquity(correctionId, positionCorrectionId, null)))
        .addObject("backLinkUrl", addedPositionUrl(correctionId, positionCorrectionId))
        .addObject("saveAndContinueUrl", ReverseRouter.route(on(this.getClass())
            .saveSetEquitySummary(correctionId, positionCorrectionId, null)))
        .addObject("removeUrls", removeUrls);
  }

  @PostMapping("/summary")
  public ModelAndView saveSetEquitySummary(
      @PathVariable UUID correctionId,
      @PathVariable UUID positionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    licencePositionCorrectionService.getPositionCorrectionForCorrection(positionCorrectionId, correction);

    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderAddedPosition(correctionId, positionCorrectionId, null));
  }

  @PostMapping("/remove")
  public ModelAndView removeSetEquity(
      @PathVariable UUID correctionId,
      @PathVariable UUID positionCorrectionId,
      @RequestParam Integer transferTo,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(positionCorrectionId, correction);

    var operations = licencePositionCorrectionService.getCommittedSetEquityOperations(positionCorrection).stream()
        .filter(operation -> !operation.transferTo().equals(transferTo))
        .toList();

    licencePositionCorrectionService.commitSetEquity(positionCorrection, operations);

    return ReverseRouter.redirect(on(this.getClass())
        .showSetEquitySummary(correctionId, positionCorrectionId, null));
  }

  private ModelAndView setEquityModelAndView(
      UUID correctionId,
      UUID positionCorrectionId,
      LicenceCorrection correction,
      LicencePositionSetEquityForm form
  ) {
    return new ModelAndView("lms/licence/correction/setEquity")
        .addObject("pageTitle", ADD_PAGE_TITLE)
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("form", form)
        .addObject("licenseeOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("preselectedTransferTo",
            licencePositionCorrectionService.getPreselectedTransferTo(form.getTransferTo()))
        .addObject("backLinkUrl",
            ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForAddedPosition(correctionId, positionCorrectionId, null)));
  }

  private String addedPositionUrl(UUID correctionId, UUID positionCorrectionId) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderAddedPosition(correctionId, positionCorrectionId, null));
  }
}