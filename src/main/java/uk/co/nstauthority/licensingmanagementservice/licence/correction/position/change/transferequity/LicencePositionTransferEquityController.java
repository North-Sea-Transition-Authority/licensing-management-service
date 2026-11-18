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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
@CorrectionLicenceIsType(LicenceType.CARBON_STORAGE)
public class LicencePositionTransferEquityController {

  private static final String PAGE_TITLE = "Add equity transfer";
  private static final String SUMMARY_PAGE_TITLE = "Transfer equity";

  private final LicencePositionTransferEquityFormValidator licencePositionTransferEquityFormValidator;
  private final TransferEquityWithdrawFormValidator transferEquityWithdrawFormValidator;
  private final TransferEquityCorrectionService transferEquityCorrectionService;

  public LicencePositionTransferEquityController(
      LicencePositionTransferEquityFormValidator licencePositionTransferEquityFormValidator,
      TransferEquityWithdrawFormValidator transferEquityWithdrawFormValidator,
      TransferEquityCorrectionService transferEquityCorrectionService
  ) {
    this.licencePositionTransferEquityFormValidator = licencePositionTransferEquityFormValidator;
    this.transferEquityWithdrawFormValidator = transferEquityWithdrawFormValidator;
    this.transferEquityCorrectionService = transferEquityCorrectionService;
  }

  @GetMapping("/position/{licencePositionId}/transfer-equity")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView renderForPosition(CorrectionPosition correctionPosition) {
    return transferEquityModelAndView(
        correctionPosition.correction(),
        new LicencePositionTransferEquityForm(),
        transferEquityCorrectionService.getEquityHoldings(correctionPosition),
        addTransferBackLinkUrl(correctionPosition)
    );
  }

  @PostMapping("/position/{licencePositionId}/transfer-equity")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView submitForPosition(
      CorrectionPosition correctionPosition,
      @ModelAttribute("form") LicencePositionTransferEquityForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var equityHoldings = transferEquityCorrectionService.getEquityHoldings(correctionPosition);
    if (licencePositionTransferEquityFormValidator.hasErrors(form, bindingResult, equityHoldings)) {
      return transferEquityModelAndView(
          correctionPosition.correction(),
          form,
          equityHoldings,
          addTransferBackLinkUrl(correctionPosition)
      );
    }

    transferEquityCorrectionService.addTransferEquity(correctionPosition, form);

    var operations = transferEquityCorrectionService.getCommittedTransferEquityOperations(correctionPosition);
    var index = operations.size() - 1;
    var holdings = transferEquityCorrectionService.getEquityHoldings(correctionPosition);

    if (transferorHoldsNoEquity(holdings, operations.get(index))) {
      return ReverseRouter.redirect(on(this.getClass()).renderWithdrawForPosition(correctionPosition, index));
    }

    generateSuccessBanner(redirectAttributes);
    return ReverseRouter.redirect(on(this.getClass()).renderSummaryForPosition(correctionPosition));
  }

  @GetMapping("/position/{licencePositionId}/transfer-equity/withdraw")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView renderWithdrawForPosition(
      CorrectionPosition correctionPosition,
      @RequestParam int index
  ) {
    var operations = transferEquityCorrectionService.getCommittedTransferEquityOperations(correctionPosition);

    if (isOutOfRange(operations, index)) {
      return ReverseRouter.redirect(on(this.getClass()).renderSummaryForPosition(correctionPosition));
    }

    var operation = operations.get(index);
    return withdrawModelAndView(
        correctionPosition.correction(),
        operation,
        withdrawForm(operation),
        withdrawUrl(correctionPosition, index),
        summaryUrl(correctionPosition)
    );
  }

  @PostMapping("/position/{licencePositionId}/transfer-equity/withdraw")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView submitWithdrawForPosition(
      CorrectionPosition correctionPosition,
      @RequestParam int index,
      @ModelAttribute("form") TransferEquityWithdrawForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var operations = transferEquityCorrectionService.getCommittedTransferEquityOperations(correctionPosition);

    if (isOutOfRange(operations, index)) {
      return ReverseRouter.redirect(on(this.getClass()).renderSummaryForPosition(correctionPosition));
    }

    if (transferEquityWithdrawFormValidator.hasErrors(form, bindingResult)) {
      return withdrawModelAndView(
          correctionPosition.correction(),
          operations.get(index),
          form,
          withdrawUrl(correctionPosition, index),
          summaryUrl(correctionPosition)
      );
    }

    transferEquityCorrectionService.setTransferEquityRetention(
        correctionPosition,
        index,
        retainsBeneficialInterest(form)
    );

    generateSuccessBanner(redirectAttributes);
    return ReverseRouter.redirect(on(this.getClass()).renderSummaryForPosition(correctionPosition));
  }

  @GetMapping("/position/{licencePositionId}/transfer-equity/summary")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView renderSummaryForPosition(CorrectionPosition correctionPosition) {
    var operations = transferEquityCorrectionService.getCommittedTransferEquityOperations(correctionPosition);

    var removeUrls = IntStream.range(0, operations.size())
        .mapToObj(index -> ReverseRouter.route(on(this.getClass()).removeForPosition(correctionPosition, index)))
        .toList();

    var withdrawUrls = IntStream.range(0, operations.size())
        .mapToObj(index -> withdrawUrl(correctionPosition, index))
        .toList();

    var holdings = transferEquityCorrectionService.getEquityHoldings(correctionPosition);

    return transferEquitySummaryModelAndView(
        correctionPosition.correction(),
        operations,
        ReverseRouter.route(on(this.getClass()).renderForPosition(correctionPosition)),
        LicencePositionCorrectionRouteUtil.getPositionPageUrl(correctionPosition),
        removeUrls,
        withdrawUrls,
        holdings
    );
  }

  @PostMapping("/position/{licencePositionId}/transfer-equity/remove")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView removeForPosition(
      CorrectionPosition correctionPosition,
      @RequestParam int index
  ) {
    transferEquityCorrectionService.removeTransferEquity(correctionPosition, index);

    return ReverseRouter.redirect(on(this.getClass()).renderSummaryForPosition(correctionPosition));
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
      Map<Integer, BigDecimal> equityHoldings,
      String backLinkUrl
  ) {
    return new ModelAndView("lms/licence/correction/transferEquity")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("form", form)
        .addObject("licenseeOptions", transferEquityCorrectionService.getLicenseeOptions(equityHoldings))
        .addObject("transferToOptions", transferEquityCorrectionService.getTransferToOptions(equityHoldings))
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

  private String addTransferBackLinkUrl(CorrectionPosition correctionPosition) {
    var hasExistingTransfers = !transferEquityCorrectionService
        .getCommittedTransferEquityOperations(correctionPosition).isEmpty();
    return hasExistingTransfers
        ? summaryUrl(correctionPosition)
        : ReverseRouter.route(on(LicencePositionAddChangeController.class).renderForPosition(correctionPosition));
  }

  private String summaryUrl(CorrectionPosition correctionPosition) {
    return ReverseRouter.route(on(this.getClass()).renderSummaryForPosition(correctionPosition));
  }

  private String withdrawUrl(CorrectionPosition correctionPosition, int index) {
    return ReverseRouter.route(on(this.getClass()).renderWithdrawForPosition(correctionPosition, index));
  }
}
