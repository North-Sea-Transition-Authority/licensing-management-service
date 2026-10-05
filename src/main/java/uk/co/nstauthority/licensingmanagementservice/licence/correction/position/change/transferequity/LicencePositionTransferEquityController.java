package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
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
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType(LicenceType.CARBON_STORAGE)
public class LicencePositionTransferEquityController {

  private static final String PAGE_TITLE = "Add equity transfer";
  private static final String SUMMARY_PAGE_TITLE = "Transfer equity";

  private final LicencePositionTransferEquityFormValidator licencePositionTransferEquityFormValidator;
  private final TransferEquityWithdrawFormValidator transferEquityWithdrawFormValidator;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final TransferEquityCorrectionService transferEquityCorrectionService;

  public LicencePositionTransferEquityController(
      LicencePositionTransferEquityFormValidator licencePositionTransferEquityFormValidator,
      TransferEquityWithdrawFormValidator transferEquityWithdrawFormValidator,
      OrganisationUnitQueryService organisationUnitQueryService,
      TransferEquityCorrectionService transferEquityCorrectionService
  ) {
    this.licencePositionTransferEquityFormValidator = licencePositionTransferEquityFormValidator;
    this.transferEquityWithdrawFormValidator = transferEquityWithdrawFormValidator;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.transferEquityCorrectionService = transferEquityCorrectionService;
  }

  @GetMapping("/position/{licencePositionId}/transfer-equity")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    return transferEquityModelAndView(
        correction,
        new LicencePositionTransferEquityForm(),
        executedAddTransferBackLinkUrl(correction, licencePosition));
  }

  @PostMapping("/position/{licencePositionId}/transfer-equity")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView submitForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      @ModelAttribute("form") LicencePositionTransferEquityForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var equityHoldings = transferEquityCorrectionService.getEquityHoldingsForCorrection(correction, licencePosition.getId());
    if (licencePositionTransferEquityFormValidator.hasErrors(form, bindingResult, equityHoldings)) {
      return transferEquityModelAndView(
          correction, form, executedAddTransferBackLinkUrl(correction, licencePosition));
    }

    transferEquityCorrectionService.addTransferEquityForExecutedPosition(correction, licencePosition, form);

    var operations = transferEquityCorrectionService
        .getCommittedTransferEquityOperationsForExecutedPosition(correction, licencePosition);
    var index = operations.size() - 1;
    var holdings = transferEquityCorrectionService.getEquityHoldingsForCorrection(correction, licencePosition.getId());

    if (transferorHoldsNoEquity(holdings, operations.get(index))) {
      return ReverseRouter.redirect(on(this.getClass())
          .renderWithdrawForExecutedPosition(correction, licencePosition, index));
    }

    generateSuccessBanner(redirectAttributes);
    return ReverseRouter.redirect(on(this.getClass())
        .renderSummaryForExecutedPosition(correction, licencePosition));
  }

  @GetMapping("/position/{licencePositionId}/transfer-equity/withdraw")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderWithdrawForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      @RequestParam int index
  ) {
    var operations = transferEquityCorrectionService
        .getCommittedTransferEquityOperationsForExecutedPosition(correction, licencePosition);

    if (isOutOfRange(operations, index)) {
      return ReverseRouter.redirect(on(this.getClass())
          .renderSummaryForExecutedPosition(correction, licencePosition));
    }

    var operation = operations.get(index);
    return withdrawModelAndView(
        correction,
        operation,
        withdrawForm(operation),
        executedWithdrawUrl(correction, licencePosition, index),
        executedSummaryUrl(correction, licencePosition));
  }

  @PostMapping("/position/{licencePositionId}/transfer-equity/withdraw")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView submitWithdrawForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      @RequestParam int index,
      @ModelAttribute("form") TransferEquityWithdrawForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var operations = transferEquityCorrectionService
        .getCommittedTransferEquityOperationsForExecutedPosition(correction, licencePosition);

    if (isOutOfRange(operations, index)) {
      return ReverseRouter.redirect(on(this.getClass())
          .renderSummaryForExecutedPosition(correction, licencePosition));
    }

    if (transferEquityWithdrawFormValidator.hasErrors(form, bindingResult)) {
      return withdrawModelAndView(
          correction,
          operations.get(index),
          form,
          executedWithdrawUrl(correction, licencePosition, index),
          executedSummaryUrl(correction, licencePosition));
    }

    transferEquityCorrectionService.setTransferEquityRetentionForExecutedPosition(
        correction, licencePosition, index, retainsBeneficialInterest(form));

    generateSuccessBanner(redirectAttributes);
    return ReverseRouter.redirect(on(this.getClass())
        .renderSummaryForExecutedPosition(correction, licencePosition));
  }

  @GetMapping("/position/{licencePositionId}/transfer-equity/summary")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderSummaryForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    var operations = transferEquityCorrectionService
        .getCommittedTransferEquityOperationsForExecutedPosition(correction, licencePosition);

    var removeUrls = IntStream.range(0, operations.size())
        .mapToObj(index -> ReverseRouter.route(on(this.getClass())
            .removeForExecutedPosition(correction, licencePosition, index)))
        .toList();

    var withdrawUrls = IntStream.range(0, operations.size())
        .mapToObj(index -> executedWithdrawUrl(correction, licencePosition, index))
        .toList();

    var holdings = transferEquityCorrectionService.getEquityHoldingsForCorrection(correction, licencePosition.getId());

    return transferEquitySummaryModelAndView(
        correction,
        operations,
        ReverseRouter.route(on(this.getClass()).renderForExecutedPosition(correction, licencePosition)),
        executedPositionUrl(correction, licencePosition),
        removeUrls,
        withdrawUrls,
        holdings);
  }

  @PostMapping("/position/{licencePositionId}/transfer-equity/remove")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView removeForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      @RequestParam int index
  ) {
    transferEquityCorrectionService.removeTransferEquityForExecutedPosition(correction, licencePosition, index);

    return ReverseRouter.redirect(on(this.getClass())
        .renderSummaryForExecutedPosition(correction, licencePosition));
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/transfer-equity")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView renderForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    return transferEquityModelAndView(
        correction,
        new LicencePositionTransferEquityForm(),
        addedAddTransferBackLinkUrl(correction, licencePositionCorrection));
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/transfer-equity")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView submitForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @ModelAttribute("form") LicencePositionTransferEquityForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var equityHoldings = transferEquityCorrectionService.getEquityHoldingsForAddedPosition(correction, licencePositionCorrection);
    if (licencePositionTransferEquityFormValidator.hasErrors(form, bindingResult, equityHoldings)) {
      return transferEquityModelAndView(
          correction, form, addedAddTransferBackLinkUrl(correction, licencePositionCorrection));
    }
    transferEquityCorrectionService.addTransferEquity(licencePositionCorrection, form);

    var operations = transferEquityCorrectionService.getCommittedTransferEquityOperations(licencePositionCorrection);
    var index = operations.size() - 1;
    var holdings = transferEquityCorrectionService.getEquityHoldingsForAddedPosition(correction, licencePositionCorrection);

    if (transferorHoldsNoEquity(holdings, operations.get(index))) {
      return ReverseRouter.redirect(on(this.getClass())
          .renderWithdrawForAddedPosition(correction, licencePositionCorrection, index));
    }

    generateSuccessBanner(redirectAttributes);
    return ReverseRouter.redirect(on(this.getClass())
        .renderSummaryForAddedPosition(correction, licencePositionCorrection));
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/transfer-equity/withdraw")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView renderWithdrawForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @RequestParam int index
  ) {
    var operations = transferEquityCorrectionService.getCommittedTransferEquityOperations(licencePositionCorrection);

    if (isOutOfRange(operations, index)) {
      return ReverseRouter.redirect(on(this.getClass())
          .renderSummaryForAddedPosition(correction, licencePositionCorrection));
    }

    var operation = operations.get(index);
    return withdrawModelAndView(
        correction,
        operation,
        withdrawForm(operation),
        addedWithdrawUrl(correction, licencePositionCorrection, index),
        addedSummaryUrl(correction, licencePositionCorrection));
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/transfer-equity/withdraw")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView submitWithdrawForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @RequestParam int index,
      @ModelAttribute("form") TransferEquityWithdrawForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var operations = transferEquityCorrectionService.getCommittedTransferEquityOperations(licencePositionCorrection);

    if (isOutOfRange(operations, index)) {
      return ReverseRouter.redirect(on(this.getClass())
          .renderSummaryForAddedPosition(correction, licencePositionCorrection));
    }

    if (transferEquityWithdrawFormValidator.hasErrors(form, bindingResult)) {
      return withdrawModelAndView(
          correction,
          operations.get(index),
          form,
          addedWithdrawUrl(correction, licencePositionCorrection, index),
          addedSummaryUrl(correction, licencePositionCorrection));
    }

    transferEquityCorrectionService.setTransferEquityRetention(licencePositionCorrection, index, retainsBeneficialInterest(form));

    generateSuccessBanner(redirectAttributes);
    return ReverseRouter.redirect(on(this.getClass())
        .renderSummaryForAddedPosition(correction, licencePositionCorrection));
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/transfer-equity/summary")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView renderSummaryForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    var operations = transferEquityCorrectionService.getCommittedTransferEquityOperations(licencePositionCorrection);

    var removeUrls = IntStream.range(0, operations.size())
        .mapToObj(index -> ReverseRouter.route(on(this.getClass())
            .removeForAddedPosition(correction, licencePositionCorrection, index)))
        .toList();

    var withdrawUrls = IntStream.range(0, operations.size())
        .mapToObj(index -> addedWithdrawUrl(correction, licencePositionCorrection, index))
        .toList();

    var holdings = transferEquityCorrectionService.getEquityHoldingsForAddedPosition(correction, licencePositionCorrection);

    return transferEquitySummaryModelAndView(
        correction,
        operations,
        ReverseRouter.route(on(this.getClass()).renderForAddedPosition(correction, licencePositionCorrection)),
        addedPositionUrl(correction, licencePositionCorrection),
        removeUrls,
        withdrawUrls,
        holdings);
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/transfer-equity/remove")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView removeForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @RequestParam int index
  ) {
    transferEquityCorrectionService.removeTransferEquity(licencePositionCorrection, index);

    return ReverseRouter.redirect(on(this.getClass())
        .renderSummaryForAddedPosition(correction, licencePositionCorrection));
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
      LicencePosition licencePosition
  ) {
    var hasExistingTransfers = !transferEquityCorrectionService
        .getCommittedTransferEquityOperationsForExecutedPosition(correction, licencePosition).isEmpty();
    return hasExistingTransfers
        ? executedSummaryUrl(correction, licencePosition)
        : executedPositionUrl(correction, licencePosition);
  }

  private String addedAddTransferBackLinkUrl(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    var hasExistingTransfers = !transferEquityCorrectionService
        .getCommittedTransferEquityOperations(licencePositionCorrection).isEmpty();
    return hasExistingTransfers
        ? addedSummaryUrl(correction, licencePositionCorrection)
        : addedChangeChooserUrl(correction, licencePositionCorrection);
  }

  private String executedPositionUrl(LicenceCorrection correction, LicencePosition licencePosition) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition));
  }

  private String executedSummaryUrl(LicenceCorrection correction, LicencePosition licencePosition) {
    return ReverseRouter.route(on(this.getClass())
        .renderSummaryForExecutedPosition(correction, licencePosition));
  }

  private String executedWithdrawUrl(LicenceCorrection correction, LicencePosition licencePosition, int index) {
    return ReverseRouter.route(on(this.getClass())
        .renderWithdrawForExecutedPosition(correction, licencePosition, index));
  }

  private String addedPositionUrl(LicenceCorrection correction, LicencePositionCorrection licencePositionCorrection) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderAddedPosition(correction, licencePositionCorrection));
  }

  private String addedSummaryUrl(LicenceCorrection correction, LicencePositionCorrection licencePositionCorrection) {
    return ReverseRouter.route(on(this.getClass())
        .renderSummaryForAddedPosition(correction, licencePositionCorrection));
  }

  private String addedWithdrawUrl(LicenceCorrection correction, LicencePositionCorrection licencePositionCorrection, int index) {
    return ReverseRouter.route(on(this.getClass())
        .renderWithdrawForAddedPosition(correction, licencePositionCorrection, index));
  }

  private String addedChangeChooserUrl(LicenceCorrection correction, LicencePositionCorrection licencePositionCorrection) {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForAddedPosition(correction, licencePositionCorrection));
  }
}