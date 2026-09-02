package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@Profile("enable-lms2")
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType(LicenceType.CARBON_STORAGE)
public class LicencePositionTransferEquityController {

  private static final String PAGE_TITLE = "Add equity transfer";
  private static final String SUMMARY_PAGE_TITLE = "Transfer equity";

  private final LicencePositionTransferEquityFormValidator licencePositionTransferEquityFormValidator;
  private final TransferEquityWithdrawFormValidator transferEquityWithdrawFormValidator;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionService licencePositionService;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final TransferEquityCorrectionService transferEquityCorrectionService;

  public LicencePositionTransferEquityController(
      LicencePositionTransferEquityFormValidator licencePositionTransferEquityFormValidator,
      TransferEquityWithdrawFormValidator transferEquityWithdrawFormValidator,
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionService licencePositionService,
      OrganisationUnitQueryService organisationUnitQueryService,
      TransferEquityCorrectionService transferEquityCorrectionService
  ) {
    this.licencePositionTransferEquityFormValidator = licencePositionTransferEquityFormValidator;
    this.transferEquityWithdrawFormValidator = transferEquityWithdrawFormValidator;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionService = licencePositionService;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.transferEquityCorrectionService = transferEquityCorrectionService;
  }

  @GetMapping("/position/{licencePositionId}/transfer-equity")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView renderForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    return transferEquityModelAndView(
        correction,
        new LicencePositionTransferEquityForm(),
        executedAddTransferBackLinkUrl(correction, correctionId, licencePositionId));
  }

  @PostMapping("/position/{licencePositionId}/transfer-equity")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView submitForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") LicencePositionTransferEquityForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var equityHoldings = transferEquityCorrectionService.getEquityHoldingsForCorrection(correction, licencePositionId);
    if (licencePositionTransferEquityFormValidator.hasErrors(form, bindingResult, equityHoldings)) {
      return transferEquityModelAndView(
          correction, form, executedAddTransferBackLinkUrl(correction, correctionId, licencePositionId));
    }

    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    transferEquityCorrectionService.addTransferEquityForExecutedPosition(correction, licencePosition, form);

    var operations = transferEquityCorrectionService
        .getCommittedTransferEquityOperationsForExecutedPosition(correction, licencePosition);
    var index = operations.size() - 1;
    var holdings = transferEquityCorrectionService.getEquityHoldingsForCorrection(correction, licencePositionId);

    if (transferorHoldsNoEquity(holdings, operations.get(index))) {
      return ReverseRouter.redirect(on(this.getClass())
          .renderWithdrawForExecutedPosition(correctionId, licencePositionId, index, null));
    }

    generateSuccessBanner(redirectAttributes);
    return ReverseRouter.redirect(on(this.getClass())
        .renderSummaryForExecutedPosition(correctionId, licencePositionId, null));
  }

  @GetMapping("/position/{licencePositionId}/transfer-equity/withdraw")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView renderWithdrawForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestParam int index,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var operations = transferEquityCorrectionService
        .getCommittedTransferEquityOperationsForExecutedPosition(correction, licencePosition);

    if (isOutOfRange(operations, index)) {
      return ReverseRouter.redirect(on(this.getClass())
          .renderSummaryForExecutedPosition(correctionId, licencePositionId, null));
    }

    var operation = operations.get(index);
    return withdrawModelAndView(
        correction,
        operation,
        withdrawForm(operation),
        executedWithdrawUrl(correctionId, licencePositionId, index),
        executedSummaryUrl(correctionId, licencePositionId));
  }

  @PostMapping("/position/{licencePositionId}/transfer-equity/withdraw")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView submitWithdrawForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestParam int index,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") TransferEquityWithdrawForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var operations = transferEquityCorrectionService
        .getCommittedTransferEquityOperationsForExecutedPosition(correction, licencePosition);

    if (isOutOfRange(operations, index)) {
      return ReverseRouter.redirect(on(this.getClass())
          .renderSummaryForExecutedPosition(correctionId, licencePositionId, null));
    }

    if (transferEquityWithdrawFormValidator.hasErrors(form, bindingResult)) {
      return withdrawModelAndView(
          correction,
          operations.get(index),
          form,
          executedWithdrawUrl(correctionId, licencePositionId, index),
          executedSummaryUrl(correctionId, licencePositionId));
    }

    transferEquityCorrectionService.setTransferEquityRetentionForExecutedPosition(
        correction, licencePosition, index, retainsBeneficialInterest(form));

    generateSuccessBanner(redirectAttributes);
    return ReverseRouter.redirect(on(this.getClass())
        .renderSummaryForExecutedPosition(correctionId, licencePositionId, null));
  }

  @GetMapping("/position/{licencePositionId}/transfer-equity/summary")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView renderSummaryForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var operations = transferEquityCorrectionService
        .getCommittedTransferEquityOperationsForExecutedPosition(correction, licencePosition);

    var removeUrls = IntStream.range(0, operations.size())
        .mapToObj(index -> ReverseRouter.route(on(this.getClass())
            .removeForExecutedPosition(correctionId, licencePositionId, index, null)))
        .toList();

    var withdrawUrls = IntStream.range(0, operations.size())
        .mapToObj(index -> executedWithdrawUrl(correctionId, licencePositionId, index))
        .toList();

    var holdings = transferEquityCorrectionService.getEquityHoldingsForCorrection(correction, licencePositionId);

    return transferEquitySummaryModelAndView(
        correction,
        operations,
        ReverseRouter.route(on(this.getClass()).renderForExecutedPosition(correctionId, licencePositionId, null)),
        executedPositionUrl(correctionId, licencePositionId),
        removeUrls,
        withdrawUrls,
        holdings);
  }

  @PostMapping("/position/{licencePositionId}/transfer-equity/remove")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView removeForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestParam int index,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    transferEquityCorrectionService.removeTransferEquityForExecutedPosition(correction, licencePosition, index);

    return ReverseRouter.redirect(on(this.getClass())
        .renderSummaryForExecutedPosition(correctionId, licencePositionId, null));
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/transfer-equity")
  public ModelAndView renderForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    return transferEquityModelAndView(
        correction,
        new LicencePositionTransferEquityForm(),
        addedAddTransferBackLinkUrl(correction, correctionId, licencePositionCorrectionId));
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/transfer-equity")
  public ModelAndView submitForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") LicencePositionTransferEquityForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);
    var equityHoldings = transferEquityCorrectionService.getEquityHoldingsForAddedPosition(correction, positionCorrection);
    if (licencePositionTransferEquityFormValidator.hasErrors(form, bindingResult, equityHoldings)) {
      return transferEquityModelAndView(
          correction, form, addedAddTransferBackLinkUrl(correction, correctionId, licencePositionCorrectionId));
    }
    transferEquityCorrectionService.addTransferEquity(positionCorrection, form);

    var operations = transferEquityCorrectionService.getCommittedTransferEquityOperations(positionCorrection);
    var index = operations.size() - 1;
    var holdings = transferEquityCorrectionService.getEquityHoldingsForAddedPosition(correction, positionCorrection);

    if (transferorHoldsNoEquity(holdings, operations.get(index))) {
      return ReverseRouter.redirect(on(this.getClass())
          .renderWithdrawForAddedPosition(correctionId, licencePositionCorrectionId, index, null));
    }

    generateSuccessBanner(redirectAttributes);
    return ReverseRouter.redirect(on(this.getClass())
        .renderSummaryForAddedPosition(correctionId, licencePositionCorrectionId, null));
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/transfer-equity/withdraw")
  public ModelAndView renderWithdrawForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestParam int index,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);
    var operations = transferEquityCorrectionService.getCommittedTransferEquityOperations(positionCorrection);

    if (isOutOfRange(operations, index)) {
      return ReverseRouter.redirect(on(this.getClass())
          .renderSummaryForAddedPosition(correctionId, licencePositionCorrectionId, null));
    }

    var operation = operations.get(index);
    return withdrawModelAndView(
        correction,
        operation,
        withdrawForm(operation),
        addedWithdrawUrl(correctionId, licencePositionCorrectionId, index),
        addedSummaryUrl(correctionId, licencePositionCorrectionId));
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/transfer-equity/withdraw")
  public ModelAndView submitWithdrawForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestParam int index,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") TransferEquityWithdrawForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);
    var operations = transferEquityCorrectionService.getCommittedTransferEquityOperations(positionCorrection);

    if (isOutOfRange(operations, index)) {
      return ReverseRouter.redirect(on(this.getClass())
          .renderSummaryForAddedPosition(correctionId, licencePositionCorrectionId, null));
    }

    if (transferEquityWithdrawFormValidator.hasErrors(form, bindingResult)) {
      return withdrawModelAndView(
          correction,
          operations.get(index),
          form,
          addedWithdrawUrl(correctionId, licencePositionCorrectionId, index),
          addedSummaryUrl(correctionId, licencePositionCorrectionId));
    }

    transferEquityCorrectionService.setTransferEquityRetention(positionCorrection, index, retainsBeneficialInterest(form));

    generateSuccessBanner(redirectAttributes);
    return ReverseRouter.redirect(on(this.getClass())
        .renderSummaryForAddedPosition(correctionId, licencePositionCorrectionId, null));
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/transfer-equity/summary")
  public ModelAndView renderSummaryForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);
    var operations = transferEquityCorrectionService.getCommittedTransferEquityOperations(positionCorrection);

    var removeUrls = IntStream.range(0, operations.size())
        .mapToObj(index -> ReverseRouter.route(on(this.getClass())
            .removeForAddedPosition(correctionId, licencePositionCorrectionId, index, null)))
        .toList();

    var withdrawUrls = IntStream.range(0, operations.size())
        .mapToObj(index -> addedWithdrawUrl(correctionId, licencePositionCorrectionId, index))
        .toList();

    var holdings = transferEquityCorrectionService.getEquityHoldingsForAddedPosition(correction, positionCorrection);

    return transferEquitySummaryModelAndView(
        correction,
        operations,
        ReverseRouter.route(on(this.getClass()).renderForAddedPosition(correctionId, licencePositionCorrectionId, null)),
        addedPositionUrl(correctionId, licencePositionCorrectionId),
        removeUrls,
        withdrawUrls,
        holdings);
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/transfer-equity/remove")
  public ModelAndView removeForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestParam int index,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);
    transferEquityCorrectionService.removeTransferEquity(positionCorrection, index);

    return ReverseRouter.redirect(on(this.getClass())
        .renderSummaryForAddedPosition(correctionId, licencePositionCorrectionId, null));
  }

  private void generateSuccessBanner(RedirectAttributes redirectAttributes) {
    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Equity transfer updated")
        .applyTo(redirectAttributes);
  }

  private boolean isOutOfRange(List<TransferEquityOperation> operations, int index) {
    return index < 0 || index >= operations.size();
  }

  private boolean transferorHoldsNoEquity(
      Map<Integer, BigDecimal> holdings,
      TransferEquityOperation operation
  ) {
    return holdings.getOrDefault(operation.transferFrom(), BigDecimal.ZERO).compareTo(BigDecimal.ZERO) <= 0;
  }

  private boolean retainsBeneficialInterest(TransferEquityWithdrawForm form) {
    return TransferEquityWithdrawalDecision.valueOf(form.getWithdrawalDecision()).retainsBeneficialInterest();
  }

  private TransferEquityWithdrawForm withdrawForm(TransferEquityOperation operation) {
    var form = new TransferEquityWithdrawForm();
    var retainBeneficialInterest = operation.retainBeneficialInterest();
    if (retainBeneficialInterest != null) {
      form.setWithdrawalDecision(retainBeneficialInterest
          ? TransferEquityWithdrawalDecision.RETAIN.name()
          : TransferEquityWithdrawalDecision.WITHDRAW.name());
    }
    return form;
  }

  private ModelAndView transferEquityModelAndView(
      LicenceCorrection correction,
      LicencePositionTransferEquityForm form,
      String backLinkUrl
  ) {
    return new ModelAndView("lms/licence/correction/transferEquity")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("form", form)
        .addObject("licenseeOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("preselectedTransferFrom",
            organisationUnitQueryService.getOrganisationUnitSelectOption(form.getTransferFrom()))
        .addObject("preselectedTransferTo",
            organisationUnitQueryService.getOrganisationUnitSelectOption(form.getTransferTo()))
        .addObject("backLinkUrl", backLinkUrl);
  }

  private ModelAndView withdrawModelAndView(
      LicenceCorrection correction,
      TransferEquityOperation operation,
      TransferEquityWithdrawForm form,
      String submitUrl,
      String backLinkUrl
  ) {
    var fromOrganisationName = transferEquityCorrectionService.getTransferEquityViews(List.of(operation))
        .getFirst()
        .transferFromOrganisationName();

    return new ModelAndView("lms/licence/correction/transferEquityWithdraw")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("form", form)
        .addObject("organisationName", fromOrganisationName)
        .addObject("withdrawalOptions", TransferEquityWithdrawalDecision.getOptions())
        .addObject("submitUrl", submitUrl)
        .addObject("backLinkUrl", backLinkUrl);
  }

  private ModelAndView transferEquitySummaryModelAndView(
      LicenceCorrection correction,
      List<TransferEquityOperation> operations,
      String addTransferUrl,
      String positionUrl,
      List<String> removeUrls,
      List<String> withdrawUrls,
      Map<Integer, BigDecimal> holdings
  ) {
    var withdrawApplicable = operations.stream()
        .map(operation -> transferorHoldsNoEquity(holdings, operation))
        .toList();

    return new ModelAndView("lms/licence/correction/transferEquitySummary")
        .addObject("pageTitle", SUMMARY_PAGE_TITLE)
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("transferEquityViews", transferEquityCorrectionService.getTransferEquityViews(operations))
        .addObject("addTransferUrl", addTransferUrl)
        .addObject("backLinkUrl", positionUrl)
        .addObject("saveAndContinueUrl", positionUrl)
        .addObject("removeUrls", removeUrls)
        .addObject("withdrawUrls", withdrawUrls)
        .addObject("withdrawApplicable", withdrawApplicable);
  }

  private String executedAddTransferBackLinkUrl(
      LicenceCorrection correction,
      UUID correctionId,
      UUID licencePositionId
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var hasExistingTransfers = !transferEquityCorrectionService
        .getCommittedTransferEquityOperationsForExecutedPosition(correction, licencePosition).isEmpty();
    return hasExistingTransfers
        ? executedSummaryUrl(correctionId, licencePositionId)
        : executedPositionUrl(correctionId, licencePositionId);
  }

  private String addedAddTransferBackLinkUrl(
      LicenceCorrection correction,
      UUID correctionId,
      UUID licencePositionCorrectionId
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);
    var hasExistingTransfers = !transferEquityCorrectionService
        .getCommittedTransferEquityOperations(positionCorrection).isEmpty();
    return hasExistingTransfers
        ? addedSummaryUrl(correctionId, licencePositionCorrectionId)
        : addedChangeChooserUrl(correctionId, licencePositionCorrectionId);
  }

  private String executedPositionUrl(UUID correctionId, UUID licencePositionId) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correctionId, licencePositionId, null));
  }

  private String executedSummaryUrl(UUID correctionId, UUID licencePositionId) {
    return ReverseRouter.route(on(this.getClass())
        .renderSummaryForExecutedPosition(correctionId, licencePositionId, null));
  }

  private String executedWithdrawUrl(UUID correctionId, UUID licencePositionId, int index) {
    return ReverseRouter.route(on(this.getClass())
        .renderWithdrawForExecutedPosition(correctionId, licencePositionId, index, null));
  }

  private String addedPositionUrl(UUID correctionId, UUID licencePositionCorrectionId) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderAddedPosition(correctionId, licencePositionCorrectionId, null));
  }

  private String addedSummaryUrl(UUID correctionId, UUID licencePositionCorrectionId) {
    return ReverseRouter.route(on(this.getClass())
        .renderSummaryForAddedPosition(correctionId, licencePositionCorrectionId, null));
  }

  private String addedWithdrawUrl(UUID correctionId, UUID licencePositionCorrectionId, int index) {
    return ReverseRouter.route(on(this.getClass())
        .renderWithdrawForAddedPosition(correctionId, licencePositionCorrectionId, index, null));
  }

  private String addedChangeChooserUrl(UUID correctionId, UUID licencePositionCorrectionId) {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForAddedPosition(correctionId, licencePositionCorrectionId, null));
  }
}